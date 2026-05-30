package com.controlfinanciero.services

import com.controlfinanciero.repositories.TransactionRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.OffsetDateTime

@Serializable
data class MercadoPagoPayment(
    val id: Long,
    val description: String? = null,
    val status: String,
    @SerialName("transaction_amount") val transactionAmount: Double,
    @SerialName("date_created") val dateCreated: String,
    @SerialName("payment_type_id") val paymentTypeId: String? = null,
    @SerialName("operation_type") val operationType: String? = null
)

@Serializable
data class MercadoPagoSearchResponse(
    val results: List<MercadoPagoPayment>,
    val paging: MercadoPagoPaging
)

@Serializable
data class MercadoPagoPaging(
    val total: Int,
    val limit: Int,
    val offset: Int
)

class MercadoPagoService(
    private val accessToken: String,
    private val transactionRepository: TransactionRepository,
    private val baseUrl: String = "https://api.mercadopago.com"
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    /**
     * Busca pagos recibidos en Mercado Pago y los importa como transacciones.
     * @param from fecha desde (ISO 8601)
     * @param to fecha hasta (ISO 8601)
     * @param defaultCategoryId categoría por defecto para asignar
     */
    suspend fun syncPayments(
        from: String? = null,
        to: String? = null,
        defaultCategoryId: Int = 1
    ): SyncResult {
        var offset = 0
        val limit = 50
        var imported = 0
        var skipped = 0
        var errors = 0

        do {
            val response: MercadoPagoSearchResponse = client.get("$baseUrl/v1/payments/search") {
                header("Authorization", "Bearer $accessToken")
                parameter("sort", "date_created")
                parameter("criteria", "desc")
                parameter("limit", limit)
                parameter("offset", offset)
                from?.let { parameter("begin_date", it) }
                to?.let { parameter("end_date", it) }
            }.body()

            for (payment in response.results) {
                if (payment.status != "approved") {
                    skipped++
                    continue
                }

                try {
                    val isIngreso = payment.operationType == "regular_payment"
                    val type = if (isIngreso) "ingreso" else "egreso"
                    // date_created viene en ISO 8601 con offset (ej: "...T10:15:30.000-03:00").
                    // OffsetDateTime soporta cualquier offset (+/-); el LocalDateTime.parse anterior
                    // fallaba con offsets negativos (Argentina = -03:00).
                    val date = OffsetDateTime.parse(payment.dateCreated).toLocalDateTime()

                    transactionRepository.createFromMercadoPago(
                        mpAmount = payment.transactionAmount,
                        mpDescription = payment.description ?: "Pago Mercado Pago #${payment.id}",
                        mpType = type,
                        mpCategoryId = defaultCategoryId,
                        mpDate = date,
                        mpExternalId = "mp_${payment.id}"
                    )
                    imported++
                } catch (e: Exception) {
                    errors++
                }
            }

            offset += limit
        } while (offset < response.paging.total)

        return SyncResult(imported, skipped, errors)
    }

    fun close() = client.close()
}

@Serializable
data class SyncResult(
    val imported: Int,
    val skipped: Int,
    val errors: Int
)
