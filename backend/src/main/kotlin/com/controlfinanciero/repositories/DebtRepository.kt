package com.controlfinanciero.repositories

import com.controlfinanciero.database.DatabaseFactory.dbQuery
import com.controlfinanciero.models.db.Debts
import com.controlfinanciero.models.dto.CreateDebtRequest
import com.controlfinanciero.models.dto.DebtDTO
import com.controlfinanciero.models.dto.DebtSummaryDTO
import com.controlfinanciero.models.dto.UpdateDebtRequest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class DebtRepository {

    private fun ResultRow.toDTO(today: LocalDate): DebtDTO {
        val total = this[Debts.totalInstallments]
        val paid = this[Debts.paidInstallments]
        val amount = this[Debts.installmentAmount].toDouble()
        val due = this[Debts.dueDate]

        // Una deuda "simple" (sin N) se considera saldada con un pago (paid >= 1).
        val finished = if (total != null) paid >= total else paid >= 1
        val remainingInstallments = total?.let { (it - paid).coerceAtLeast(0) }
        val remainingAmount = when {
            total != null -> (remainingInstallments ?: 0) * amount
            finished -> 0.0
            else -> amount
        }
        val pct = when {
            total != null && total > 0 -> (paid.toDouble() / total * 100).coerceIn(0.0, 100.0)
            finished -> 100.0
            else -> 0.0
        }
        val dueStatus = when {
            finished -> "ok"
            due == null -> "sin_fecha"
            else -> {
                val days = ChronoUnit.DAYS.between(today, due)
                when {
                    days < 0 -> "vencido"
                    days <= 7 -> "proximo"
                    else -> "ok"
                }
            }
        }
        return DebtDTO(
            id = this[Debts.id],
            description = this[Debts.description],
            type = this[Debts.type],
            creditor = this[Debts.creditor],
            installmentAmount = amount,
            totalInstallments = total,
            paidInstallments = paid,
            dueDate = due?.toString(),
            notes = this[Debts.notes],
            remainingInstallments = remainingInstallments,
            remainingAmount = remainingAmount,
            progressPct = pct,
            finished = finished,
            dueStatus = dueStatus
        )
    }

    /** Deudas del usuario + totales (lo que falta, compromiso mensual, vencidas/próximas). */
    suspend fun getSummary(userId: Int): DebtSummaryDTO = dbQuery {
        val today = LocalDate.now()
        val items = Debts.selectAll().where { Debts.userId eq userId }
            .orderBy(Debts.dueDate to SortOrder.ASC_NULLS_LAST, Debts.id to SortOrder.ASC)
            .map { it.toDTO(today) }
        val active = items.filter { !it.finished }
        DebtSummaryDTO(
            totalRemaining = active.sumOf { it.remainingAmount },
            totalMonthly = active.sumOf { it.installmentAmount },
            overdueCount = items.count { it.dueStatus == "vencido" },
            dueSoonCount = items.count { it.dueStatus == "proximo" },
            debts = items
        )
    }

    suspend fun create(userId: Int, req: CreateDebtRequest): DebtDTO = dbQuery {
        val now = LocalDateTime.now()
        val id = Debts.insert {
            it[Debts.userId] = userId
            it[description] = req.description.trim()
            it[type] = req.type.trim()
            it[creditor] = req.creditor?.trim()?.ifBlank { null }
            it[installmentAmount] = BigDecimal.valueOf(req.installmentAmount)
            it[totalInstallments] = req.totalInstallments
            it[paidInstallments] = req.paidInstallments.coerceAtLeast(0)
            it[dueDate] = req.dueDate?.let { d -> LocalDate.parse(d) }
            it[notes] = req.notes?.trim()?.ifBlank { null }
            it[createdAt] = now
            it[updatedAt] = now
        } get Debts.id
        Debts.selectAll().where { Debts.id eq id }.single().toDTO(LocalDate.now())
    }

    suspend fun update(userId: Int, id: Int, req: UpdateDebtRequest): DebtDTO? = dbQuery {
        val updated = Debts.update({ (Debts.id eq id) and (Debts.userId eq userId) }) { st ->
            req.description?.let { st[description] = it.trim() }
            req.type?.let { st[type] = it.trim() }
            req.creditor?.let { st[creditor] = it.trim().ifBlank { null } }
            req.installmentAmount?.let { st[installmentAmount] = BigDecimal.valueOf(it) }
            req.totalInstallments?.let { st[totalInstallments] = it }
            req.paidInstallments?.let { st[paidInstallments] = it.coerceAtLeast(0) }
            req.dueDate?.let { st[dueDate] = LocalDate.parse(it) }
            req.notes?.let { st[notes] = it.trim().ifBlank { null } }
            st[updatedAt] = LocalDateTime.now()
        }
        if (updated == 0) return@dbQuery null
        Debts.selectAll().where { (Debts.id eq id) and (Debts.userId eq userId) }.single().toDTO(LocalDate.now())
    }

    /** Marca una cuota pagada: avanza el contador y empuja el vencimiento +1 mes (si sigue abierta). */
    suspend fun pay(userId: Int, id: Int): DebtDTO? = dbQuery {
        val row = Debts.selectAll().where { (Debts.id eq id) and (Debts.userId eq userId) }
            .singleOrNull() ?: return@dbQuery null
        val total = row[Debts.totalInstallments]
        val paid = row[Debts.paidInstallments]
        val newPaid = if (total != null) (paid + 1).coerceAtMost(total) else paid + 1
        val stillOpen = total == null || newPaid < total
        val due = row[Debts.dueDate]
        Debts.update({ (Debts.id eq id) and (Debts.userId eq userId) }) {
            it[paidInstallments] = newPaid
            if (due != null && stillOpen) it[dueDate] = due.plusMonths(1)
            it[updatedAt] = LocalDateTime.now()
        }
        Debts.selectAll().where { (Debts.id eq id) and (Debts.userId eq userId) }.single().toDTO(LocalDate.now())
    }

    /** Revierte el último pago: retrocede el contador y la fecha −1 mes. */
    suspend fun unpay(userId: Int, id: Int): DebtDTO? = dbQuery {
        val row = Debts.selectAll().where { (Debts.id eq id) and (Debts.userId eq userId) }
            .singleOrNull() ?: return@dbQuery null
        val paid = row[Debts.paidInstallments]
        if (paid <= 0) return@dbQuery row.toDTO(LocalDate.now())
        val due = row[Debts.dueDate]
        Debts.update({ (Debts.id eq id) and (Debts.userId eq userId) }) {
            it[paidInstallments] = paid - 1
            if (due != null) it[dueDate] = due.minusMonths(1)
            it[updatedAt] = LocalDateTime.now()
        }
        Debts.selectAll().where { (Debts.id eq id) and (Debts.userId eq userId) }.single().toDTO(LocalDate.now())
    }

    suspend fun delete(userId: Int, id: Int): Boolean = dbQuery {
        Debts.deleteWhere { (Debts.id eq id) and (Debts.userId eq userId) } > 0
    }
}
