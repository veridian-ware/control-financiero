package com.controlfinanciero.repositories

import com.controlfinanciero.database.DatabaseFactory.dbQuery
import com.controlfinanciero.models.db.Categories
import com.controlfinanciero.models.db.RecurringTransactions
import com.controlfinanciero.models.db.Transactions
import com.controlfinanciero.models.dto.CreateRecurringRequest
import com.controlfinanciero.models.dto.RecurringTransactionDTO
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class RecurringTransactionRepository {

    private fun ResultRow.toDTO() = RecurringTransactionDTO(
        id = this[RecurringTransactions.id],
        amount = this[RecurringTransactions.amount].toDouble(),
        description = this[RecurringTransactions.description],
        type = this[RecurringTransactions.type],
        categoryId = this[RecurringTransactions.categoryId],
        categoryName = this.getOrNull(Categories.name),
        dayOfMonth = this[RecurringTransactions.dayOfMonth],
        active = this[RecurringTransactions.active]
    )

    suspend fun getAll(userId: Int): List<RecurringTransactionDTO> = dbQuery {
        RecurringTransactions
            .join(Categories, JoinType.LEFT, RecurringTransactions.categoryId, Categories.id)
            .selectAll().where { RecurringTransactions.userId eq userId }
            .orderBy(RecurringTransactions.dayOfMonth, SortOrder.ASC)
            .map { it.toDTO() }
    }

    suspend fun create(userId: Int, req: CreateRecurringRequest): RecurringTransactionDTO = dbQuery {
        val day = req.dayOfMonth.coerceIn(1, 28)
        val id = RecurringTransactions.insert {
            it[RecurringTransactions.userId] = userId
            it[amount] = BigDecimal.valueOf(req.amount)
            it[description] = req.description
            it[type] = req.type
            it[categoryId] = req.categoryId
            it[dayOfMonth] = day
            it[active] = true
            it[createdAt] = LocalDateTime.now()
        } get RecurringTransactions.id

        RecurringTransactionDTO(
            id = id, amount = req.amount, description = req.description,
            type = req.type, categoryId = req.categoryId, dayOfMonth = day, active = true
        )
    }

    suspend fun setActive(userId: Int, id: Int, active: Boolean): Boolean = dbQuery {
        RecurringTransactions.update({
            (RecurringTransactions.id eq id) and (RecurringTransactions.userId eq userId)
        }) { it[RecurringTransactions.active] = active } > 0
    }

    suspend fun delete(userId: Int, id: Int): Boolean = dbQuery {
        RecurringTransactions.deleteWhere {
            (RecurringTransactions.id eq id) and (RecurringTransactions.userId eq userId)
        } > 0
    }

    /** Plantilla materializable: snapshot para insertar sin reabrir queries sobre el cursor. */
    private data class Due(
        val id: Int, val amount: BigDecimal, val description: String,
        val type: String, val categoryId: Int, val dayOfMonth: Int
    )

    /**
     * Genera las transacciones del mes en curso para las recurrencias activas del usuario cuyo
     * día ya pasó (o es hoy). Idempotente: usa externalId = "rec_<id>_<yyyy-MM>" para no duplicar.
     * @return cantidad de transacciones creadas.
     */
    suspend fun materializeDue(userId: Int): Int = dbQuery {
        val today = LocalDate.now()
        val ym = YearMonth.from(today)
        val tag = "%04d-%02d".format(ym.year, ym.monthValue)

        val due = RecurringTransactions.selectAll()
            .where { (RecurringTransactions.userId eq userId) and (RecurringTransactions.active eq true) }
            .map {
                Due(
                    id = it[RecurringTransactions.id],
                    amount = it[RecurringTransactions.amount],
                    description = it[RecurringTransactions.description],
                    type = it[RecurringTransactions.type],
                    categoryId = it[RecurringTransactions.categoryId],
                    dayOfMonth = it[RecurringTransactions.dayOfMonth]
                )
            }

        var created = 0
        for (r in due) {
            val day = r.dayOfMonth.coerceIn(1, 28)
            if (today.dayOfMonth < day) continue // todavía no llegó el día este mes

            val externalId = "rec_${r.id}_$tag"
            val exists = Transactions.selectAll()
                .where { (Transactions.externalId eq externalId) and (Transactions.userId eq userId) }
                .count() > 0
            if (exists) continue

            Transactions.insert {
                it[Transactions.userId] = userId
                it[amount] = r.amount
                it[description] = r.description
                it[type] = r.type
                it[categoryId] = r.categoryId
                it[date] = ym.atDay(day).atStartOfDay()
                it[sourceCol] = "recurrente"
                it[Transactions.externalId] = externalId
                it[createdAt] = LocalDateTime.now()
            }
            created++
        }
        created
    }
}
