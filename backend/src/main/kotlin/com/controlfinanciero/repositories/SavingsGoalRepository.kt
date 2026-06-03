package com.controlfinanciero.repositories

import com.controlfinanciero.database.DatabaseFactory.dbQuery
import com.controlfinanciero.models.db.SavingsGoals
import com.controlfinanciero.models.dto.CreateSavingsGoalRequest
import com.controlfinanciero.models.dto.SavingsGoalDTO
import com.controlfinanciero.models.dto.SavingsGoalSummaryDTO
import com.controlfinanciero.models.dto.UpdateSavingsGoalRequest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class SavingsGoalRepository {

    private fun ResultRow.toDTO(): SavingsGoalDTO {
        val target = this[SavingsGoals.targetAmount].toDouble()
        val current = this[SavingsGoals.currentAmount].toDouble()
        val remaining = (target - current).coerceAtLeast(0.0)
        val pct = if (target > 0) (current / target * 100).coerceIn(0.0, 100.0) else 0.0
        return SavingsGoalDTO(
            id = this[SavingsGoals.id],
            name = this[SavingsGoals.name],
            targetAmount = target,
            currentAmount = current,
            deadline = this[SavingsGoals.deadline]?.toString(),
            remaining = remaining,
            progressPct = pct,
            reached = target > 0 && current >= target
        )
    }

    /** Metas del usuario + totales (ahorrado, objetivo). */
    suspend fun getSummary(userId: Int): SavingsGoalSummaryDTO = dbQuery {
        val items = SavingsGoals.selectAll().where { SavingsGoals.userId eq userId }
            .orderBy(SavingsGoals.id, SortOrder.ASC)
            .map { it.toDTO() }
        SavingsGoalSummaryDTO(
            totalSaved = items.sumOf { it.currentAmount },
            totalTarget = items.sumOf { it.targetAmount },
            goals = items
        )
    }

    suspend fun create(userId: Int, req: CreateSavingsGoalRequest): SavingsGoalDTO = dbQuery {
        val now = LocalDateTime.now()
        val id = SavingsGoals.insert {
            it[SavingsGoals.userId] = userId
            it[name] = req.name.trim()
            it[targetAmount] = BigDecimal.valueOf(req.targetAmount)
            it[currentAmount] = BigDecimal.valueOf(req.initialAmount.coerceAtLeast(0.0))
            it[deadline] = req.deadline?.let { d -> LocalDate.parse(d) }
            it[createdAt] = now
            it[updatedAt] = now
        } get SavingsGoals.id
        SavingsGoals.selectAll().where { SavingsGoals.id eq id }.single().toDTO()
    }

    suspend fun update(userId: Int, id: Int, req: UpdateSavingsGoalRequest): SavingsGoalDTO? = dbQuery {
        val updated = SavingsGoals.update({ (SavingsGoals.id eq id) and (SavingsGoals.userId eq userId) }) { st ->
            req.name?.let { st[name] = it.trim() }
            req.targetAmount?.let { st[targetAmount] = BigDecimal.valueOf(it) }
            req.currentAmount?.let { st[currentAmount] = BigDecimal.valueOf(it.coerceAtLeast(0.0)) }
            req.deadline?.let { st[deadline] = LocalDate.parse(it) }
            st[updatedAt] = LocalDateTime.now()
        }
        if (updated == 0) return@dbQuery null
        SavingsGoals.selectAll().where { (SavingsGoals.id eq id) and (SavingsGoals.userId eq userId) }
            .single().toDTO()
    }

    /** Suma (o resta, si amount es negativo) sobre el ahorrado; nunca baja de 0. */
    suspend fun contribute(userId: Int, id: Int, amount: Double): SavingsGoalDTO? = dbQuery {
        val row = SavingsGoals.selectAll()
            .where { (SavingsGoals.id eq id) and (SavingsGoals.userId eq userId) }
            .singleOrNull() ?: return@dbQuery null
        val newAmount = (row[SavingsGoals.currentAmount].toDouble() + amount).coerceAtLeast(0.0)
        SavingsGoals.update({ (SavingsGoals.id eq id) and (SavingsGoals.userId eq userId) }) {
            it[currentAmount] = BigDecimal.valueOf(newAmount)
            it[updatedAt] = LocalDateTime.now()
        }
        SavingsGoals.selectAll().where { (SavingsGoals.id eq id) and (SavingsGoals.userId eq userId) }
            .single().toDTO()
    }

    suspend fun delete(userId: Int, id: Int): Boolean = dbQuery {
        SavingsGoals.deleteWhere { (SavingsGoals.id eq id) and (SavingsGoals.userId eq userId) } > 0
    }
}
