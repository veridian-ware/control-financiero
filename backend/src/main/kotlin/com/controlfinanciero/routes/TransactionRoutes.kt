package com.controlfinanciero.routes

import com.controlfinanciero.models.dto.ApiResponse
import com.controlfinanciero.models.dto.CreateTransactionRequest
import com.controlfinanciero.models.dto.ImportCsvRequest
import com.controlfinanciero.models.dto.ImportResult
import com.controlfinanciero.plugins.userId
import com.controlfinanciero.repositories.HouseholdRepository
import com.controlfinanciero.repositories.TransactionRepository
import com.controlfinanciero.services.MpCsvImporter
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDateTime

fun Route.transactionRoutes() {
    val repository = TransactionRepository()
    val households = HouseholdRepository()
    val csvImporter = MpCsvImporter()

    route("/api/transactions") {
        get {
            val type = call.queryParameters["type"]
            val categoryId = call.queryParameters["categoryId"]?.toIntOrNull()
            val from = call.queryParameters["from"]?.let { LocalDateTime.parse(it) }
            val to = call.queryParameters["to"]?.let { LocalDateTime.parse(it) }
            val limit = call.queryParameters["limit"]?.toIntOrNull() ?: 50
            val offset = call.queryParameters["offset"]?.toLongOrNull() ?: 0

            val scope = households.memberIds(call.userId()) // hogar compartido o solo el usuario
            val transactions = repository.getAll(scope, type, categoryId, from, to, limit, offset)
            call.respond(ApiResponse(true, data = transactions))
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "ID inválido"))
            val transaction = repository.getById(call.userId(), id)
                ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Transacción no encontrada"))
            call.respond(ApiResponse(true, data = transaction))
        }

        post {
            val request = call.receive<CreateTransactionRequest>()
            require(request.type in listOf("ingreso", "egreso")) { "Tipo debe ser 'ingreso' o 'egreso'" }
            require(request.amount > 0) { "El monto debe ser mayor a 0" }
            val created = repository.create(call.userId(), request)
            call.respond(HttpStatusCode.Created, ApiResponse(true, data = created))
        }

        post("/import") {
            val request = call.receive<ImportCsvRequest>()
            require(request.csv.isNotBlank()) { "El CSV está vacío" }
            val result = csvImporter.import(
                userId = call.userId(),
                csv = request.csv,
                accountId = request.accountId,
                onlyPurchases = request.onlyPurchases
            )
            call.respond(
                ApiResponse(true, data = ImportResult(result.imported, result.skipped, result.errors))
            )
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "ID inválido"))
            val deleted = repository.delete(call.userId(), id)
            if (deleted) call.respond(ApiResponse(true, data = "Transacción eliminada"))
            else call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Transacción no encontrada"))
        }
    }
}
