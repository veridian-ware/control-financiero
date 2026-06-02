package com.controlfinanciero.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class CategoryDTO(
    val id: Int? = null,
    val name: String,
    val type: String, // "ingreso" o "egreso"
    val icon: String? = null,
    val color: String? = null
)

@Serializable
data class TransactionDTO(
    val id: Long? = null,
    val amount: Double,
    val description: String,
    val type: String,
    val categoryId: Int,
    val categoryName: String? = null,
    val date: String, // ISO 8601
    val source: String = "manual",
    val externalId: String? = null,
    val accountId: Int? = null
)

@Serializable
data class CreateTransactionRequest(
    val amount: Double,
    val description: String,
    val type: String,
    val categoryId: Int,
    val date: String? = null, // null = ahora
    val accountId: Int? = null
)

@Serializable
data class DashboardDTO(
    val totalIngresos: Double,
    val totalEgresos: Double,
    val balance: Double,
    val transaccionesPorCategoria: List<CategorySummary>,
    val transaccionesRecientes: List<TransactionDTO>
)

@Serializable
data class CategorySummary(
    val categoryId: Int,
    val categoryName: String,
    val type: String,
    val total: Double,
    val count: Int
)

@Serializable
data class MonthlyReport(
    val month: String, // "2026-05"
    val ingresos: Double,
    val egresos: Double,
    val balance: Double
)

@Serializable
data class RecurringTransactionDTO(
    val id: Int,
    val amount: Double,
    val description: String,
    val type: String, // "ingreso" o "egreso"
    val categoryId: Int,
    val categoryName: String? = null,
    val frequency: String, // "semanal" | "quincenal" | "mensual"
    val anchorDate: String, // yyyy-MM-dd
    val active: Boolean = true,
    val occurrences: List<RecurringOccurrenceDTO> = emptyList()
)

@Serializable
data class RecurringOccurrenceDTO(
    val id: Long,
    val recurringId: Int,
    val dueDate: String, // yyyy-MM-dd
    val amount: Double,
    val status: String, // "pendiente" | "pagado"
    val transactionId: Long? = null
)

@Serializable
data class CreateRecurringRequest(
    val amount: Double,
    val description: String,
    val type: String,
    val categoryId: Int,
    val frequency: String, // "semanal" | "quincenal" | "mensual"
    val anchorDate: String // yyyy-MM-dd
)

@Serializable
data class HouseholdMemberDTO(
    val id: Int,
    val email: String
)

@Serializable
data class HouseholdDTO(
    val id: Int,
    val name: String,
    val inviteCode: String,
    val members: List<HouseholdMemberDTO>
)

@Serializable
data class CreateHouseholdRequest(val name: String)

@Serializable
data class JoinHouseholdRequest(val inviteCode: String)

@Serializable
data class InvestmentDTO(
    val id: Int,
    val name: String,
    val type: String,
    val amountInvested: Double,
    val currentValue: Double,
    val gain: Double,      // current - invertido
    val yieldPct: Double   // gain / invertido * 100
)

@Serializable
data class InvestmentSummaryDTO(
    val totalInvested: Double,
    val totalValue: Double,
    val totalGain: Double,
    val yieldPct: Double,
    val investments: List<InvestmentDTO>
)

@Serializable
data class CreateInvestmentRequest(
    val name: String,
    val type: String,
    val amountInvested: Double,
    val currentValue: Double? = null // null = igual al invertido
)

@Serializable
data class UpdateInvestmentRequest(
    val name: String? = null,
    val type: String? = null,
    val amountInvested: Double? = null,
    val currentValue: Double? = null
)

@Serializable
data class AccountDTO(
    val id: Int,
    val name: String,
    val type: String,        // efectivo | banco | billetera | otro
    val initialBalance: Double,
    val balance: Double      // inicial + ingresos − egresos asociados
)

@Serializable
data class AccountSummaryDTO(
    val totalBalance: Double,
    val accounts: List<AccountDTO>
)

@Serializable
data class CreateAccountRequest(
    val name: String,
    val type: String,
    val initialBalance: Double = 0.0
)

@Serializable
data class UpdateAccountRequest(
    val name: String? = null,
    val type: String? = null,
    val initialBalance: Double? = null
)

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null
)
