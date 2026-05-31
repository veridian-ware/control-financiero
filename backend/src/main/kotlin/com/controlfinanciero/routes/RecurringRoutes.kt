package com.controlfinanciero.routes

import com.controlfinanciero.models.dto.ApiResponse
import com.controlfinanciero.models.dto.CreateRecurringRequest
import com.controlfinanciero.plugins.userId
import com.controlfinanciero.repositories.RecurringTransactionRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.recurringRoutes() {
    val repository = RecurringTransactionRepository()

    route("/api/recurring") {
        get {
            call.respond(ApiResponse(true, data = repository.getAll(call.userId())))
        }

        post {
            val req = call.receive<CreateRecurringRequest>()
            require(req.type in listOf("ingreso", "egreso")) { "Tipo debe ser 'ingreso' o 'egreso'" }
            require(req.amount > 0) { "El monto debe ser mayor a 0" }
            require(req.dayOfMonth in 1..28) { "El día debe estar entre 1 y 28" }
            val created = repository.create(call.userId(), req)
            call.respond(HttpStatusCode.Created, ApiResponse(true, data = created))
        }

        // Genera ahora las transacciones recurrentes del mes que ya vencieron.
        post("/run") {
            val count = repository.materializeDue(call.userId())
            call.respond(ApiResponse(true, data = count, message = "$count transacción(es) generada(s)"))
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "ID inválido"))
            val deleted = repository.delete(call.userId(), id)
            if (deleted) call.respond(ApiResponse(true, data = "Recurrencia eliminada"))
            else call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Recurrencia no encontrada"))
        }
    }
}
