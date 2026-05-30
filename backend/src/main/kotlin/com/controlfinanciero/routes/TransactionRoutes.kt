package com.controlfinanciero.routes

import com.controlfinanciero.models.dto.ApiResponse
import com.controlfinanciero.models.dto.CreateTransactionRequest
import com.controlfinanciero.repositories.TransactionRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDateTime

fun Route.transactionRoutes() {
    val repository = TransactionRepository()

    route("/api/transactions") {
        get {
            val type = call.queryParameters["type"]
            val categoryId = call.queryParameters["categoryId"]?.toIntOrNull()
            val from = call.queryParameters["from"]?.let { LocalDateTime.parse(it) }
            val to = call.queryParameters["to"]?.let { LocalDateTime.parse(it) }
            val limit = call.queryParameters["limit"]?.toIntOrNull() ?: 50
            val offset = call.queryParameters["offset"]?.toLongOrNull() ?: 0

            val transactions = repository.getAll(type, categoryId, from, to, limit, offset)
            call.respond(ApiResponse(true, data = transactions))
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "ID inválido"))
            val transaction = repository.getById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Transacción no encontrada"))
            call.respond(ApiResponse(true, data = transaction))
        }

        post {
            val request = call.receive<CreateTransactionRequest>()
            require(request.type in listOf("ingreso", "egreso")) { "Tipo debe ser 'ingreso' o 'egreso'" }
            require(request.amount > 0) { "El monto debe ser mayor a 0" }
            val created = repository.create(request)
            call.respond(HttpStatusCode.Created, ApiResponse(true, data = created))
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "ID inválido"))
            val deleted = repository.delete(id)
            if (deleted) call.respond(ApiResponse(true, data = "Transacción eliminada"))
            else call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Transacción no encontrada"))
        }
    }
}
