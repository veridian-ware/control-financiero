package com.controlfinanciero.routes

import com.controlfinanciero.models.dto.ApiResponse
import com.controlfinanciero.models.dto.SetMpTokenRequest
import com.controlfinanciero.plugins.userId
import com.controlfinanciero.repositories.CategoryRepository
import com.controlfinanciero.repositories.TransactionRepository
import com.controlfinanciero.repositories.UserRepository
import com.controlfinanciero.services.MercadoPagoService
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.mercadoPagoRoutes(config: ApplicationConfig) {
    val users = UserRepository()
    val categories = CategoryRepository()
    val configToken = config.propertyOrNull("mercadopago.accessToken")?.getString()

    route("/api/mercadopago") {
        // Guarda/actualiza el token de Mercado Pago del usuario autenticado.
        post("/token") {
            val req = call.receive<SetMpTokenRequest>()
            require(req.accessToken.isNotBlank()) { "El token no puede estar vacío" }
            users.setMpToken(call.userId(), req.accessToken.trim())
            call.respond(ApiResponse(true, data = "Token de Mercado Pago guardado"))
        }

        post("/sync") {
            val userId = call.userId()
            val token = users.getMpToken(userId) ?: configToken
            if (token.isNullOrBlank()) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(false, message = "No hay token de Mercado Pago. Configuralo con POST /api/mercadopago/token")
                )
            }

            // Resuelve la categoría por defecto, validando que pertenezca al usuario.
            val requested = call.queryParameters["categoryId"]?.toIntOrNull()
            val defaultCategoryId = requested?.let { categories.getById(userId, it)?.id }
                ?: categories.firstCategoryId(userId)
                ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(false, message = "Creá al menos una categoría antes de sincronizar")
                )

            val from = call.queryParameters["from"]
            val to = call.queryParameters["to"]

            val service = MercadoPagoService(token, TransactionRepository())
            try {
                val result = service.syncPayments(userId, from, to, defaultCategoryId)
                call.respond(ApiResponse(true, data = result, message = "Sincronización completada"))
            } finally {
                service.close()
            }
        }
    }
}
