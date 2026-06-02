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

    @POST("api/transactions/import")
    suspend fun importCsv(@Body request: ImportCsvRequest): ApiResponse<ImportResult>

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

    @POST("api/recurring/occurrences/{id}/pay")
    suspend fun payOccurrence(@Path("id") id: Long): ApiResponse<String>

    @POST("api/recurring/occurrences/{id}/unpay")
    suspend fun unpayOccurrence(@Path("id") id: Long): ApiResponse<String>

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

    // Inversiones
    @GET("api/investments")
    suspend fun getInvestments(): ApiResponse<InvestmentSummary>

    @POST("api/investments")
    suspend fun createInvestment(@Body request: CreateInvestmentRequest): ApiResponse<Investment>

    @PUT("api/investments/{id}")
    suspend fun updateInvestment(@Path("id") id: Int, @Body request: UpdateInvestmentRequest): ApiResponse<Investment>

    @DELETE("api/investments/{id}")
    suspend fun deleteInvestment(@Path("id") id: Int): ApiResponse<String>

    // Cuentas
    @GET("api/accounts")
    suspend fun getAccounts(): ApiResponse<AccountSummary>

    @POST("api/accounts")
    suspend fun createAccount(@Body request: CreateAccountRequest): ApiResponse<Account>

    @PUT("api/accounts/{id}")
    suspend fun updateAccount(@Path("id") id: Int, @Body request: UpdateAccountRequest): ApiResponse<String>

    @DELETE("api/accounts/{id}")
    suspend fun deleteAccount(@Path("id") id: Int): ApiResponse<String>

    // Presupuestos
    @GET("api/budgets")
    suspend fun getBudgets(): ApiResponse<List<Budget>>

    @POST("api/budgets")
    suspend fun createBudget(@Body request: CreateBudgetRequest): ApiResponse<String>

    @PUT("api/budgets/{id}")
    suspend fun updateBudget(@Path("id") id: Int, @Body request: UpdateBudgetRequest): ApiResponse<String>

    @DELETE("api/budgets/{id}")
    suspend fun deleteBudget(@Path("id") id: Int): ApiResponse<String>
}
