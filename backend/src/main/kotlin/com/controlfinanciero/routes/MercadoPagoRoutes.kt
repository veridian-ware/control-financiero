package com.controlfinanciero.routes

import com.controlfinanciero.models.dto.ApiResponse
import com.controlfinanciero.repositories.TransactionRepository
import com.controlfinanciero.services.MercadoPagoService
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.mercadoPagoRoutes(config: ApplicationConfig) {
    val accessToken = config.propertyOrNull("mercadopago.accessToken")?.getString()

    route("/api/mercadopago") {
        post("/sync") {
            if (accessToken.isNullOrBlank()) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(false, message = "Access token de Mercado Pago no configurado. Setear MERCADOPAGO_ACCESS_TOKEN")
                )
            }

            val from = call.queryParameters["from"]
            val to = call.queryParameters["to"]
            val defaultCategoryId = call.queryParameters["categoryId"]?.toIntOrNull() ?: 1

            val service = MercadoPagoService(accessToken, TransactionRepository())
            try {
                val result = service.syncPayments(from, to, defaultCategoryId)
                call.respond(ApiResponse(true, data = result, message = "Sincronización completada"))
            } finally {
                service.close()
            }
        }
    }
}
