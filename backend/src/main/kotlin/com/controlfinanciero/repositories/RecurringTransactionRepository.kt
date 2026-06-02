package com.controlfinanciero.repositories

import com.controlfinanciero.database.DatabaseFactory.dbQuery
import com.controlfinanciero.models.db.Categories
import com.controlfinanciero.models.db.RecurringOccurrences
import com.controlfinanciero.models.db.RecurringTransactions
import com.controlfinanciero.models.db.Transactions
import com.controlfinanciero.models.dto.CreateRecurringRequest
import com.controlfinanciero.models.dto.RecurringOccurrenceDTO
import com.controlfinanciero.models.dto.RecurringTransactionDTO
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class RecurringTransactionRepository {

    companion object {
        val FREQUENCIES = setOf("semanal", "quincenal", "mensual")
    }

    // ---- Mapeo ----

    private fun ResultRow.toTemplateDTO(occurrences: List<RecurringOccurrenceDTO>) =
        RecurringTransactionDTO(
            id = this[RecurringTransactions.id],
            amount = this[RecurringTransactions.amount].toDouble(),
            description = this[RecurringTransactions.description],
            type = this[RecurringTransactions.type],
            categoryId = this[RecurringTransactions.categoryId],
            categoryName = this.getOrNull(Categories.name),
            frequency = this[RecurringTransactions.frequency],
            anchorDate = this[RecurringTransactions.anchorDate].toString(),
            active = this[RecurringTransactions.active],
            occurrences = occurrences
        )

    private fun ResultRow.toOccurrenceDTO() = RecurringOccurrenceDTO(
        id = this[RecurringOccurrences.id],
        recurringId = this[RecurringOccurrences.recurringId],
        dueDate = this[RecurringOccurrences.dueDate].toString(),
        amount = this[RecurringOccurrences.amount].toDouble(),
        status = this[RecurringOccurrences.status],
        transactionId = this[RecurringOccurrences.transactionId]
    )

    // ---- Lectura ----

    /** Lista los fijos del usuario con sus vencimientos (hasta fin del mes en curso). */
    suspend fun getAll(userId: Int): List<RecurringTransactionDTO> {
        generateOccurrences(userId)
        return dbQuery {
            val horizon = YearMonth.now().atEndOfMonth()
            val occByTemplate = RecurringOccurrences.selectAll()
                .where { (RecurringOccurrences.userId eq userId) and (RecurringOccurrences.dueDate lessEq horizon) }
                .orderBy(RecurringOccurrences.dueDate, SortOrder.ASC)
                .map { it.toOccurrenceDTO() }
                .groupBy { it.recurringId }

            RecurringTransactions
                .join(Categories, JoinType.LEFT, RecurringTransactions.categoryId, Categories.id)
                .selectAll().where { RecurringTransactions.userId eq userId }
                .orderBy(RecurringTransactions.id, SortOrder.ASC)
                .map { it.toTemplateDTO(occByTemplate[it[RecurringTransactions.id]] ?: emptyList()) }
        }
    }

    // ---- Escritura ----

    suspend fun create(userId: Int, req: CreateRecurringRequest): RecurringTransactionDTO {
        val anchor = LocalDate.parse(req.anchorDate)
        val newId = dbQuery {
            RecurringTransactions.insert {
                it[RecurringTransactions.userId] = userId
                it[amount] = BigDecimal.valueOf(req.amount)
                it[description] = req.description
                it[type] = req.type
                it[categoryId] = req.categoryId
                it[frequency] = req.frequency
                it[anchorDate] = anchor
                it[active] = true
                it[createdAt] = LocalDateTime.now()
            } get RecurringTransactions.id
        }
        // getAll genera los vencimientos del mes y devuelve el template ya armado.
        return getAll(userId).first { it.id == newId }
    }

    suspend fun delete(userId: Int, id: Int): Boolean = dbQuery {
        // Borra los vencimientos del fijo; las transacciones ya pagadas quedan como gasto real.
        RecurringOccurrences.deleteWhere {
            (RecurringOccurrences.recurringId eq id) and (RecurringOccurrences.userId eq userId)
        }
        RecurringTransactions.deleteWhere {
            (RecurringTransactions.id eq id) and (RecurringTransactions.userId eq userId)
        } > 0
    }

    // ---- Vencimientos: pagar / volver a pendiente ----

    /** Marca un vencimiento como pagado: crea la Transaction real (idempotente por externalId). */
    suspend fun markPaid(userId: Int, occurrenceId: Long): Boolean = dbQuery {
        val row = RecurringOccurrences
            .join(RecurringTransactions, JoinType.INNER, RecurringOccurrences.recurringId, RecurringTransactions.id)
            .selectAll()
            .where { (RecurringOccurrences.id eq occurrenceId) and (RecurringOccurrences.userId eq userId) }
            .singleOrNull() ?: return@dbQuery false

        if (row[RecurringOccurrences.status] == "pagado") return@dbQuery true

        val recurringId = row[RecurringOccurrences.recurringId]
        val dueDate = row[RecurringOccurrences.dueDate]
        val externalId = "rec_${recurringId}_$dueDate"

        val existingTxId = Transactions.selectAll()
            .where { (Transactions.externalId eq externalId) and (Transactions.userId eq userId) }
            .singleOrNull()?.get(Transactions.id)

        val txId = existingTxId ?: Transactions.insert {
            it[Transactions.userId] = userId
            it[amount] = row[RecurringOccurrences.amount]
            it[description] = row[RecurringTransactions.description]
            it[type] = row[RecurringTransactions.type]
            it[categoryId] = row[RecurringTransactions.categoryId]
            it[date] = dueDate.atStartOfDay()
            it[sourceCol] = "recurrente"
            it[Transactions.externalId] = externalId
            it[createdAt] = LocalDateTime.now()
        } get Transactions.id

        RecurringOccurrences.update({ RecurringOccurrences.id eq occurrenceId }) {
            it[status] = "pagado"
            it[transactionId] = txId
        }
        true
    }

    /** Vuelve un vencimiento a "pendiente": borra la Transaction asociada si existe. */
    suspend fun markPending(userId: Int, occurrenceId: Long): Boolean = dbQuery {
        val row = RecurringOccurrences.selectAll()
            .where { (RecurringOccurrences.id eq occurrenceId) and (RecurringOccurrences.userId eq userId) }
            .singleOrNull() ?: return@dbQuery false

        val txId = row[RecurringOccurrences.transactionId]
        // Primero soltamos la referencia (FK) y recién después borramos la transacción.
        RecurringOccurrences.update({ RecurringOccurrences.id eq occurrenceId }) {
            it[status] = "pendiente"
            it[transactionId] = null
        }
        if (txId != null) {
            Transactions.deleteWhere { (Transactions.id eq txId) and (Transactions.userId eq userId) }
        }
        true
    }

    // ---- Generación de vencimientos ----

    /** Snapshot de plantilla para generar sin reabrir el cursor. */
    private data class Template(
        val id: Int, val amount: BigDecimal, val frequency: String, val anchor: LocalDate
    )

    /**
     * Genera (idempotente) los vencimientos del mes en curso para los fijos activos del usuario.
     * No crea transacciones: cada vencimiento arranca "pendiente".
     * @return cantidad de vencimientos nuevos creados.
     */
    suspend fun generateOccurrences(userId: Int): Int = dbQuery {
        val ym = YearMonth.now()
        val windowStart = ym.atDay(1)
        val windowEnd = ym.atEndOfMonth()

        val templates = RecurringTransactions.selectAll()
            .where { (RecurringTransactions.userId eq userId) and (RecurringTransactions.active eq true) }
            .map {
                Template(
                    id = it[RecurringTransactions.id],
                    amount = it[RecurringTransactions.amount],
                    frequency = it[RecurringTransactions.frequency],
                    anchor = it[RecurringTransactions.anchorDate]
                )
            }

        var created = 0
        for (t in templates) {
            for (due in dueDatesInWindow(t.anchor, t.frequency, windowStart, windowEnd)) {
                val exists = RecurringOccurrences.selectAll()
                    .where { (RecurringOccurrences.recurringId eq t.id) and (RecurringOccurrences.dueDate eq due) }
                    .count() > 0
                if (exists) continue
                RecurringOccurrences.insert {
                    it[recurringId] = t.id
                    it[RecurringOccurrences.userId] = userId
                    it[dueDate] = due
                    it[amount] = t.amount
                    it[status] = "pendiente"
                    it[createdAt] = LocalDateTime.now()
                }
                created++
            }
        }
        created
    }

    /** Fechas de vencimiento dentro de [start, end] avanzando por frecuencia desde el ancla. */
    private fun dueDatesInWindow(
        anchor: LocalDate, frequency: String, start: LocalDate, end: LocalDate
    ): List<LocalDate> {
        val step: (LocalDate) -> LocalDate = when (frequency) {
            "semanal" -> { d -> d.plusWeeks(1) }
            "quincenal" -> { d -> d.plusWeeks(2) }
            "mensual" -> { d -> d.plusMonths(1) }
            else -> return emptyList()
        }
        val result = mutableListOf<LocalDate>()
        var d = anchor
        var guard = 0
        while (!d.isAfter(end) && guard < 1000) {
            if (!d.isBefore(start)) result.add(d)
            d = step(d)
            guard++
        }
        return result
    }
}
