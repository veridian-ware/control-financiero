package com.controlfinanciero.repositories

import com.controlfinanciero.database.DatabaseFactory.dbQuery
import com.controlfinanciero.models.db.Budgets
import com.controlfinanciero.models.db.Categories
import com.controlfinanciero.models.db.Transactions
import com.controlfinanciero.models.dto.BudgetDTO
import com.controlfinanciero.models.dto.CreateBudgetRequest
import com.controlfinanciero.models.dto.UpdateBudgetRequest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth

class BudgetRepository {

    private val households = HouseholdRepository()

    /** Presupuestos del usuario con el gastado del mes (egresos de la categoría, alcance hogar). */
    suspend fun getAll(userId: Int): List<BudgetDTO> {
        val scope = households.memberIds(userId)
        val now = LocalDateTime.now()
        val from = now.toLocalDate().withDayOfMonth(1).atStartOfDay()
        val to = YearMonth.from(now).atEndOfMonth().atTime(23, 59, 59)
        return dbQuery {
            val spentByCat = HashMap<Int, BigDecimal>()
            Transactions.selectAll().where {
                (Transactions.userId inList scope) and
                    (Transactions.type eq "egreso") and
                    (Transactions.date greaterEq from) and (Transactions.date lessEq to)
            }.forEach {
                val cat = it[Transactions.categoryId]
                spentByCat[cat] = (spentByCat[cat] ?: BigDecimal.ZERO) + it[Transactions.amount]
            }

            Budgets.join(Categories, JoinType.LEFT, Budgets.categoryId, Categories.id)
                .selectAll().where { Budgets.userId eq userId }
                .orderBy(Budgets.id, SortOrder.ASC)
                .map { row ->
                    val catId = row[Budgets.categoryId]
                    val limit = row[Budgets.monthlyLimit]
                    val spent = spentByCat[catId] ?: BigDecimal.ZERO
                    val limitD = limit.toDouble()
                    val spentD = spent.toDouble()
                    BudgetDTO(
                        id = row[Budgets.id],
                        categoryId = catId,
                        categoryName = row.getOrNull(Categories.name),
                        monthlyLimit = limitD,
                        spent = spentD,
                        remaining = limitD - spentD,
                        percentUsed = if (limitD > 0) spentD / limitD * 100 else 0.0,
                        exceeded = spent > limit
                    )
                }
        }
    }

    /** Crea o actualiza el presupuesto de la categoría (uno por categoría y usuario). */
    suspend fun upsert(userId: Int, req: CreateBudgetRequest): Boolean = dbQuery {
        val now = LocalDateTime.now()
        val existing = Budgets.selectAll()
            .where { (Budgets.userId eq userId) and (Budgets.categoryId eq req.categoryId) }
            .singleOrNull()
        if (existing != null) {
            Budgets.update({ Budgets.id eq existing[Budgets.id] }) {
                it[monthlyLimit] = BigDecimal.valueOf(req.monthlyLimit)
                it[updatedAt] = now
            }
        } else {
            Budgets.insert {
                it[Budgets.userId] = userId
                it[categoryId] = req.categoryId
                it[monthlyLimit] = BigDecimal.valueOf(req.monthlyLimit)
                it[createdAt] = now
                it[updatedAt] = now
            }
        }
        true
    }

    suspend fun update(userId: Int, id: Int, req: UpdateBudgetRequest): Boolean = dbQuery {
        Budgets.update({ (Budgets.id eq id) and (Budgets.userId eq userId) }) {
            it[monthlyLimit] = BigDecimal.valueOf(req.monthlyLimit)
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun delete(userId: Int, id: Int): Boolean = dbQuery {
        Budgets.deleteWhere { (Budgets.id eq id) and (Budgets.userId eq userId) } > 0
    }
}
