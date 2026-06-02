package com.controlfinanciero.database

import com.controlfinanciero.models.db.Accounts
import com.controlfinanciero.models.db.Budgets
import com.controlfinanciero.models.db.Categories
import com.controlfinanciero.models.db.Households
import com.controlfinanciero.models.db.Investments
import com.controlfinanciero.models.db.RecurringOccurrences
import com.controlfinanciero.models.db.RecurringTransactions
import com.controlfinanciero.models.db.Transactions
import com.controlfinanciero.models.db.Users
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    fun init(config: ApplicationConfig) {
        val url = config.property("database.url").getString()
        val user = config.property("database.user").getString()
        val password = config.property("database.password").getString()
        val driver = config.property("database.driver").getString()

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = url
            driverClassName = driver
            username = user
            this.password = password
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        val dataSource = HikariDataSource(hikariConfig)
        Database.connect(dataSource)

        transaction {
            // Orden importa por las foreign keys: Households <- Users <- Categories <- Transactions.
            // createMissingTablesAndColumns agrega tablas/columnas nuevas a una DB ya existente
            // (ej: users.household_id, recurring_transactions).
            SchemaUtils.createMissingTablesAndColumns(
                Households, Users, Accounts, Categories, Transactions,
                RecurringTransactions, RecurringOccurrences, Investments, Budgets
            )
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
