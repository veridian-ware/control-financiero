package com.controlfinanciero.data.models

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null
)

@Serializable
data class Category(
    val id: Int? = null,
    val name: String,
    val type: String,
    val icon: String? = null,
    val color: String? = null
)

@Serializable
data class Transaction(
    val id: Long? = null,
    val amount: Double,
    val description: String,
    val type: String,
    val categoryId: Int,
    val categoryName: String? = null,
    val date: String,
    val source: String = "manual",
    val externalId: String? = null
)

@Serializable
data class CreateTransactionRequest(
    val amount: Double,
    val description: String,
    val type: String,
    val categoryId: Int,
    val date: String? = null
)

@Serializable
data class Dashboard(
    val totalIngresos: Double,
    val totalEgresos: Double,
    val balance: Double,
    val transaccionesPorCategoria: List<CategorySummary>,
    val transaccionesRecientes: List<Transaction>
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
    val month: String,
    val ingresos: Double,
    val egresos: Double,
    val balance: Double
)

@Serializable
data class SyncResult(
    val imported: Int,
    val skipped: Int,
    val errors: Int
)

// --- Autenticación ---

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String? = null
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class User(
    val id: Int,
    val email: String,
    val name: String? = null,
    val hasMercadoPagoToken: Boolean = false
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: User
)

@Serializable
data class SetMpTokenRequest(
    val accessToken: String
)

@Serializable
data class UpdateProfileRequest(
    val name: String
)

// --- Recurrentes (ingresos/egresos fijos) ---

@Serializable
data class RecurringTransaction(
    val id: Int,
    val amount: Double,
    val description: String,
    val type: String, // "ingreso" o "egreso"
    val categoryId: Int,
    val categoryName: String? = null,
    val frequency: String, // "semanal" | "quincenal" | "mensual"
    val anchorDate: String, // yyyy-MM-dd
    val active: Boolean = true,
    val occurrences: List<RecurringOccurrence> = emptyList()
)

@Serializable
data class RecurringOccurrence(
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

// --- Hogar compartido ---

@Serializable
data class HouseholdMember(
    val id: Int,
    val email: String
)

@Serializable
data class Household(
    val id: Int,
    val name: String,
    val inviteCode: String,
    val members: List<HouseholdMember>
)

@Serializable
data class CreateHouseholdRequest(val name: String)

@Serializable
data class JoinHouseholdRequest(val inviteCode: String)

// --- Inversiones ---

@Serializable
data class Investment(
    val id: Int,
    val name: String,
    val type: String,
    val amountInvested: Double,
    val currentValue: Double,
    val gain: Double,
    val yieldPct: Double
)

@Serializable
data class InvestmentSummary(
    val totalInvested: Double,
    val totalValue: Double,
    val totalGain: Double,
    val yieldPct: Double,
    val investments: List<Investment>
)

@Serializable
data class CreateInvestmentRequest(
    val name: String,
    val type: String,
    val amountInvested: Double,
    val currentValue: Double? = null
)

@Serializable
data class UpdateInvestmentRequest(
    val name: String? = null,
    val type: String? = null,
    val amountInvested: Double? = null,
    val currentValue: Double? = null
)
