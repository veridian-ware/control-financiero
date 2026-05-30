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
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null
)
