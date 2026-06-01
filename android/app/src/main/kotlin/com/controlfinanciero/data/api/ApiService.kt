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

    @PUT("api/auth/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): ApiResponse<User>

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

    // Recurrentes (ingresos/egresos fijos)
    @GET("api/recurring")
    suspend fun getRecurring(): ApiResponse<List<RecurringTransaction>>

    @POST("api/recurring")
    suspend fun createRecurring(@Body request: CreateRecurringRequest): ApiResponse<RecurringTransaction>

    @POST("api/recurring/run")
    suspend fun runRecurring(): ApiResponse<Int>

    @DELETE("api/recurring/{id}")
    suspend fun deleteRecurring(@Path("id") id: Int): ApiResponse<String>

    // Hogar compartido
    @GET("api/household")
    suspend fun getHousehold(): ApiResponse<Household>

    @POST("api/household")
    suspend fun createHousehold(@Body request: CreateHouseholdRequest): ApiResponse<Household>

    @POST("api/household/join")
    suspend fun joinHousehold(@Body request: JoinHouseholdRequest): ApiResponse<Household>

    @POST("api/household/leave")
    suspend fun leaveHousehold(): ApiResponse<String>
}
