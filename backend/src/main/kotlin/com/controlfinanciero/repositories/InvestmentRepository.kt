package com.controlfinanciero.repositories

import com.controlfinanciero.database.DatabaseFactory.dbQuery
import com.controlfinanciero.models.db.Investments
import com.controlfinanciero.models.dto.CreateInvestmentRequest
import com.controlfinanciero.models.dto.InvestmentDTO
import com.controlfinanciero.models.dto.InvestmentSummaryDTO
import com.controlfinanciero.models.dto.UpdateInvestmentRequest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.time.LocalDateTime

class InvestmentRepository {

    private fun ResultRow.toDTO(): InvestmentDTO {
        val invested = this[Investments.amountInvested].toDouble()
        val current = this[Investments.currentValue].toDouble()
        val gain = current - invested
        return InvestmentDTO(
            id = this[Investments.id],
            name = this[Investments.name],
            type = this[Investments.type],
            amountInvested = invested,
            currentValue = current,
            gain = gain,
            yieldPct = if (invested > 0) gain / invested * 100 else 0.0
        )
    }

    /** Inversiones del usuario + totales (invertido, valor actual, ganancia, rendimiento). */
    suspend fun getSummary(userId: Int): InvestmentSummaryDTO = dbQuery {
        val items = Investments.selectAll().where { Investments.userId eq userId }
            .orderBy(Investments.id, SortOrder.ASC)
            .map { it.toDTO() }
        val totalInvested = items.sumOf { it.amountInvested }
        val totalValue = items.sumOf { it.currentValue }
        val totalGain = totalValue - totalInvested
        InvestmentSummaryDTO(
            totalInvested = totalInvested,
            totalValue = totalValue,
            totalGain = totalGain,
            yieldPct = if (totalInvested > 0) totalGain / totalInvested * 100 else 0.0,
            investments = items
        )
    }

    suspend fun create(userId: Int, req: CreateInvestmentRequest): InvestmentDTO = dbQuery {
        val now = LocalDateTime.now()
        val current = req.currentValue ?: req.amountInvested
        val id = Investments.insert {
            it[Investments.userId] = userId
            it[name] = req.name.trim()
            it[type] = req.type
            it[amountInvested] = BigDecimal.valueOf(req.amountInvested)
            it[currentValue] = BigDecimal.valueOf(current)
            it[createdAt] = now
            it[updatedAt] = now
        } get Investments.id
        Investments.selectAll().where { Investments.id eq id }.single().toDTO()
    }

    suspend fun update(userId: Int, id: Int, req: UpdateInvestmentRequest): InvestmentDTO? = dbQuery {
        val updated = Investments.update({ (Investments.id eq id) and (Investments.userId eq userId) }) { st ->
            req.name?.let { st[name] = it.trim() }
            req.type?.let { st[type] = it }
            req.amountInvested?.let { st[amountInvested] = BigDecimal.valueOf(it) }
            req.currentValue?.let { st[currentValue] = BigDecimal.valueOf(it) }
            st[updatedAt] = LocalDateTime.now()
        }
        if (updated == 0) return@dbQuery null
        Investments.selectAll().where { (Investments.id eq id) and (Investments.userId eq userId) }
            .single().toDTO()
    }

    suspend fun delete(userId: Int, id: Int): Boolean = dbQuery {
        Investments.deleteWhere { (Investments.id eq id) and (Investments.userId eq userId) } > 0
    }
}
