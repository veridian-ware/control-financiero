package com.controlfinanciero.repositories

import com.controlfinanciero.database.DatabaseFactory.dbQuery
import com.controlfinanciero.models.db.Accounts
import com.controlfinanciero.models.db.Transactions
import com.controlfinanciero.models.dto.AccountDTO
import com.controlfinanciero.models.dto.AccountSummaryDTO
import com.controlfinanciero.models.dto.CreateAccountRequest
import com.controlfinanciero.models.dto.UpdateAccountRequest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.time.LocalDateTime

class AccountRepository {

    companion object {
        val TYPES = setOf("efectivo", "banco", "billetera", "otro")
    }

    /** Cuentas del usuario con saldo calculado (inicial + ingresos − egresos) + patrimonio total. */
    suspend fun getSummary(userId: Int): AccountSummaryDTO = dbQuery {
        val accounts = Accounts.selectAll().where { Accounts.userId eq userId }
            .orderBy(Accounts.id, SortOrder.ASC)
            .toList()

        // Suma firmada (ingreso +, egreso −) por cuenta, de las transacciones del usuario.
        val sums = HashMap<Int, BigDecimal>()
        Transactions.selectAll()
            .where { (Transactions.userId eq userId) and (Transactions.accountId.isNotNull()) }
            .forEach {
                val accId = it[Transactions.accountId] ?: return@forEach
                val amount = it[Transactions.amount]
                val delta = if (it[Transactions.type] == "ingreso") amount else amount.negate()
                sums[accId] = (sums[accId] ?: BigDecimal.ZERO) + delta
            }

        val dtos = accounts.map { row ->
            val id = row[Accounts.id]
            val initial = row[Accounts.initialBalance]
            val balance = initial + (sums[id] ?: BigDecimal.ZERO)
            AccountDTO(
                id = id,
                name = row[Accounts.name],
                type = row[Accounts.type],
                initialBalance = initial.toDouble(),
                balance = balance.toDouble()
            )
        }
        AccountSummaryDTO(totalBalance = dtos.sumOf { it.balance }, accounts = dtos)
    }

    suspend fun create(userId: Int, req: CreateAccountRequest): AccountDTO = dbQuery {
        val id = Accounts.insert {
            it[Accounts.userId] = userId
            it[name] = req.name.trim()
            it[type] = req.type
            it[initialBalance] = BigDecimal.valueOf(req.initialBalance)
            it[createdAt] = LocalDateTime.now()
        } get Accounts.id
        // Cuenta recién creada: sin transacciones, el saldo es el saldo inicial.
        AccountDTO(id, req.name.trim(), req.type, req.initialBalance, req.initialBalance)
    }

    suspend fun update(userId: Int, id: Int, req: UpdateAccountRequest): Boolean = dbQuery {
        Accounts.update({ (Accounts.id eq id) and (Accounts.userId eq userId) }) { st ->
            req.name?.let { st[name] = it.trim() }
            req.type?.let { st[type] = it }
            req.initialBalance?.let { st[initialBalance] = BigDecimal.valueOf(it) }
        } > 0
    }

    suspend fun delete(userId: Int, id: Int): Boolean = dbQuery {
        // Desvincula las transacciones (FK) antes de borrar la cuenta.
        Transactions.update({ (Transactions.accountId eq id) and (Transactions.userId eq userId) }) {
            it[accountId] = null
        }
        Accounts.deleteWhere { (Accounts.id eq id) and (Accounts.userId eq userId) } > 0
    }
}
