package com.controlfinanciero.data.api

import com.controlfinanciero.data.models.*
import retrofit2.http.*

interface ApiService {

    // Autenticación
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthResponse>

    @GET("api/auth/me")
    suspend fun me(): ApiResponse<User>

    @POST("api/mercadopago/token")
    suspend fun setMercadoPagoToken(@Body request: SetMpTokenRequest): ApiResponse<String>

    // Categorías
    @GET("api/categories")
    suspend fun getCategories(@Query("type") type: String? = null): ApiResponse<List<Category>>

    @POST("api/categories")
    suspend fun createCategory(@Body category: Category): ApiResponse<Category>

    @POST("api/categories/seed")
    suspend fun seedCategories(): ApiResponse<String>

    // Transacciones
    @GET("api/transactions")
    suspend fun getTransactions(
        @Query("type") type: String? = null,
        @Query("categoryId") categoryId: Int? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Long = 0
    ): ApiResponse<List<Transaction>>

    @POST("api/transactions")
    suspend fun createTransaction(@Body request: CreateTransactionRequest): ApiResponse<Transaction>

    @DELETE("api/transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: Long): ApiResponse<String>

    // Dashboard
    @GET("api/dashboard")
    suspend fun getDashboard(): ApiResponse<Dashboard>

    @GET("api/dashboard/monthly/{year}")
    suspend fun getMonthlyReport(@Path("year") year: Int): ApiResponse<List<MonthlyReport>>

    // Mercado Pago
    @POST("api/mercadopago/sync")
    suspend fun syncMercadoPago(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("categoryId") categoryId: Int? = null
    ): ApiResponse<SyncResult>
}
