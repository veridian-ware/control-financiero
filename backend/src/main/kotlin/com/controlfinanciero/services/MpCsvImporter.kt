package com.controlfinanciero.services

import com.controlfinanciero.database.DatabaseFactory.dbQuery
import com.controlfinanciero.models.db.Categories
import com.controlfinanciero.models.db.Transactions
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime
import kotlin.math.abs

/**
 * Importa el CSV de "dinero en cuenta / settlement" exportado de Mercado Pago.
 * Columnas (sep `;`): SOURCE_ID;PAYMENT_METHOD_TYPE;TRANSACTION_TYPE;TRANSACTION_AMOUNT;
 * TRANSACTION_DATE;FEE_AMOUNT;SETTLEMENT_DATE;REAL_AMOUNT;TAXES_AMOUNT;BUSINESS_UNIT;SUB_UNIT;...
 *
 * - Monto con signo: negativo = egreso, positivo = ingreso.
 * - `onlyPurchases`: si true, solo las filas con BUSINESS_UNIT = "Mercado Pago" (compras
 *   Wallet/QR/Checkouts), descartando transferencias/retiros.
 * - Dedup por externalId = "mpcsv_<SOURCE_ID>". Categoría "Mercado Pago" (auto-creada por tipo).
 */
class MpCsvImporter {

    suspend fun import(userId: Int, csv: String, accountId: Int?, onlyPurchases: Boolean): SyncResult =
        dbQuery {
            val lines = csv.split(Regex("\\r?\\n")).filter { it.isNotBlank() }
            if (lines.size < 2) return@dbQuery SyncResult(0, 0, 0)

            val delimiter = if (lines[0].contains(';')) ';' else ','
            val header = lines[0].split(delimiter).map { it.trim().uppercase() }
            val iSource = header.indexOf("SOURCE_ID")
            val iAmount = header.indexOf("TRANSACTION_AMOUNT")
            val iDate = header.indexOf("TRANSACTION_DATE")
            val iBiz = header.indexOf("BUSINESS_UNIT")
            val iSub = header.indexOf("SUB_UNIT")
            if (iSource < 0 || iAmount < 0 || iDate < 0) {
                return@dbQuery SyncResult(0, 0, lines.size - 1) // formato no reconocido
            }

            // Categoría "Mercado Pago" por tipo (get-or-create).
            fun categoryFor(type: String): Int {
                Categories.selectAll()
                    .where {
                        (Categories.userId eq userId) and (Categories.name eq "Mercado Pago") and
                            (Categories.type eq type)
                    }
                    .firstOrNull()?.let { return it[Categories.id] }
                return Categories.insert {
                    it[Categories.userId] = userId
                    it[name] = "Mercado Pago"
                    it[Categories.type] = type
                    it[icon] = "account_balance_wallet"
                    it[color] = if (type == "egreso") "#F43F5E" else "#22C55E"
                    it[createdAt] = LocalDateTime.now()
                } get Categories.id
            }
            val egresoCat = categoryFor("egreso")
            val ingresoCat = categoryFor("ingreso")

            var imported = 0
            var skipped = 0
            var errors = 0
            for (i in 1 until lines.size) {
                try {
                    val cols = lines[i].split(delimiter)
                    fun col(idx: Int) = if (idx in cols.indices) cols[idx].trim() else ""

                    val amount = col(iAmount).toDoubleOrNull()
                    if (amount == null || amount == 0.0) {
                        if (amount == null) errors++ else skipped++
                        continue
                    }
                    val biz = if (iBiz >= 0) col(iBiz) else ""
                    if (onlyPurchases && !biz.equals("Mercado Pago", ignoreCase = true)) {
                        skipped++; continue
                    }

                    val type = if (amount < 0) "egreso" else "ingreso"
                    val date = OffsetDateTime.parse(col(iDate)).toLocalDateTime()
                    val externalId = "mpcsv_${col(iSource)}"
                    val sub = if (iSub >= 0) col(iSub) else ""
                    val description = listOf(biz, sub).filter { it.isNotBlank() }
                        .joinToString(" · ").ifBlank { "Mercado Pago" }

                    val exists = Transactions.selectAll()
                        .where { (Transactions.externalId eq externalId) and (Transactions.userId eq userId) }
                        .count() > 0
                    if (exists) { skipped++; continue }

                    Transactions.insert {
                        it[Transactions.userId] = userId
                        it[Transactions.amount] = BigDecimal.valueOf(abs(amount))
                        it[Transactions.description] = description
                        it[Transactions.type] = type
                        it[categoryId] = if (type == "egreso") egresoCat else ingresoCat
                        it[Transactions.date] = date
                        it[sourceCol] = "mercadopago_csv"
                        it[Transactions.externalId] = externalId
                        it[Transactions.accountId] = accountId
                        it[createdAt] = LocalDateTime.now()
                    }
                    imported++
                } catch (e: Exception) {
                    errors++
                }
            }
            SyncResult(imported, skipped, errors)
        }
}
