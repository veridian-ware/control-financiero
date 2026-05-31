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
    val externalId: String? = null
)

@Serializable
data class CreateTransactionRequest(
    val amount: Double,
    val description: String,
    val type: String,
    val categoryId: Int,
    val date: String? = null // null = ahora
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
    val id: Int? = null,
    val amount: Double,
    val description: String,
    val type: String, // "ingreso" o "egreso"
    val categoryId: Int,
    val categoryName: String? = null,
    val dayOfMonth: Int, // 1..28
    val active: Boolean = true
)

@Serializable
data class CreateRecurringRequest(
    val amount: Double,
    val description: String,
    val type: String,
    val categoryId: Int,
    val dayOfMonth: Int
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
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null
)
