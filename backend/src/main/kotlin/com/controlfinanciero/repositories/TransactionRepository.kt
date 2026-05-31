package com.controlfinanciero.repositories

import com.controlfinanciero.database.DatabaseFactory.dbQuery
import com.controlfinanciero.models.db.Categories
import com.controlfinanciero.models.db.Transactions
import com.controlfinanciero.models.dto.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TransactionRepository {

    private fun ResultRow.toTransactionDTO() = TransactionDTO(
        id = this[Transactions.id],
        amount = this[Transactions.amount].toDouble(),
        description = this[Transactions.description],
        type = this[Transactions.type],
        categoryId = this[Transactions.categoryId],
        categoryName = this.getOrNull(Categories.name),
        date = this[Transactions.date].format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        source = this[Transactions.sourceCol],
        externalId = this[Transactions.externalId]
    )

    suspend fun getAll(
        userIds: List<Int>,
        type: String? = null,
        categoryId: Int? = null,
        from: LocalDateTime? = null,
        to: LocalDateTime? = null,
        limit: Int = 50,
        offset: Long = 0
    ): List<TransactionDTO> = dbQuery {
        val query = Transactions.join(Categories, JoinType.LEFT, Transactions.categoryId, Categories.id)
            .selectAll()
            .where { Transactions.userId inList userIds }

        type?.let { query.andWhere { Transactions.type eq it } }
        categoryId?.let { query.andWhere { Transactions.categoryId eq it } }
        from?.let { query.andWhere { Transactions.date greaterEq it } }
        to?.let { query.andWhere { Transactions.date lessEq it } }

        query.orderBy(Transactions.date, SortOrder.DESC)
            .limit(limit)
            .offset(offset)
            .map { it.toTransactionDTO() }
    }

    suspend fun getById(userId: Int, id: Long): TransactionDTO? = dbQuery {
        Transactions.join(Categories, JoinType.LEFT, Transactions.categoryId, Categories.id)
            .selectAll().where { (Transactions.id eq id) and (Transactions.userId eq userId) }
            .singleOrNull()?.toTransactionDTO()
    }

    suspend fun create(userId: Int, request: CreateTransactionRequest): TransactionDTO = dbQuery {
        val now = LocalDateTime.now()
        val txDate = request.date?.let { LocalDateTime.parse(it) } ?: now

        val id = Transactions.insert {
            it[Transactions.userId] = userId
            it[amount] = BigDecimal.valueOf(request.amount)
            it[description] = request.description
            it[type] = request.type
            it[categoryId] = request.categoryId
            it[date] = txDate
            it[sourceCol] = "manual"
            it[createdAt] = now
        } get Transactions.id

        TransactionDTO(
            id = id,
            amount = request.amount,
            description = request.description,
            type = request.type,
            categoryId = request.categoryId,
            date = txDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            source = "manual"
        )
    }

    suspend fun createFromMercadoPago(
        userId: Int,
        mpAmount: Double,
        mpDescription: String,
        mpType: String,
        mpCategoryId: Int,
        mpDate: LocalDateTime,
        mpExternalId: String
    ): TransactionDTO = dbQuery {
        val existing = Transactions.selectAll()
            .where { (Transactions.externalId eq mpExternalId) and (Transactions.userId eq userId) }
            .singleOrNull()

        if (existing != null) return@dbQuery existing.toTransactionDTO()

        val id = Transactions.insert {
            it[Transactions.userId] = userId
            it[amount] = BigDecimal.valueOf(mpAmount)
            it[description] = mpDescription
            it[type] = mpType
            it[categoryId] = mpCategoryId
            it[date] = mpDate
            it[sourceCol] = "mercadopago"
            it[externalId] = mpExternalId
            it[createdAt] = LocalDateTime.now()
        } get Transactions.id

        TransactionDTO(
            id = id, amount = mpAmount, description = mpDescription,
            type = mpType, categoryId = mpCategoryId,
            date = mpDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            source = "mercadopago", externalId = mpExternalId
        )
    }

    suspend fun delete(userId: Int, id: Long): Boolean = dbQuery {
        Transactions.deleteWhere { (Transactions.id eq id) and (Transactions.userId eq userId) } > 0
    }

    suspend fun getDashboard(userIds: List<Int>, from: LocalDateTime, to: LocalDateTime): DashboardDTO = dbQuery {
        val transactions = Transactions
            .join(Categories, JoinType.LEFT, Transactions.categoryId, Categories.id)
            .selectAll()
            .where {
                (Transactions.userId inList userIds) and
                    (Transactions.date greaterEq from) and (Transactions.date lessEq to)
            }
            .orderBy(Transactions.date, SortOrder.DESC)
            .map { it.toTransactionDTO() }

        val totalIngresos = transactions.filter { it.type == "ingreso" }.sumOf { it.amount }
        val totalEgresos = transactions.filter { it.type == "egreso" }.sumOf { it.amount }

        val porCategoria = transactions.groupBy { it.categoryId }.map { (catId, txs) ->
            CategorySummary(
                categoryId = catId,
                categoryName = txs.first().categoryName ?: "Sin categoría",
                type = txs.first().type,
                total = txs.sumOf { it.amount },
                count = txs.size
            )
        }.sortedByDescending { it.total }

        DashboardDTO(
            totalIngresos = totalIngresos,
            totalEgresos = totalEgresos,
            balance = totalIngresos - totalEgresos,
            transaccionesPorCategoria = porCategoria,
            transaccionesRecientes = transactions.take(10)
        )
    }

    suspend fun getMonthlyReport(userIds: List<Int>, year: Int): List<MonthlyReport> = dbQuery {
        val from = LocalDateTime.of(year, 1, 1, 0, 0)
        val to = LocalDateTime.of(year, 12, 31, 23, 59, 59)

        val transactions = Transactions.selectAll()
            .where {
                (Transactions.userId inList userIds) and
                    (Transactions.date greaterEq from) and (Transactions.date lessEq to)
            }
            .map {
                Triple(
                    it[Transactions.date].monthValue,
                    it[Transactions.type],
                    it[Transactions.amount].toDouble()
                )
            }

        (1..12).map { month ->
            val monthTxs = transactions.filter { it.first == month }
            val ingresos = monthTxs.filter { it.second == "ingreso" }.sumOf { it.third }
            val egresos = monthTxs.filter { it.second == "egreso" }.sumOf { it.third }
            MonthlyReport(
                month = "$year-${month.toString().padStart(2, '0')}",
                ingresos = ingresos,
                egresos = egresos,
                balance = ingresos - egresos
            )
        }
    }
}
