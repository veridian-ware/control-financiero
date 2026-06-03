package com.controlfinanciero.routes

import com.controlfinanciero.models.dto.ApiResponse
import com.controlfinanciero.models.dto.ContributeRequest
import com.controlfinanciero.models.dto.CreateSavingsGoalRequest
import com.controlfinanciero.models.dto.UpdateSavingsGoalRequest
import com.controlfinanciero.plugins.userId
import com.controlfinanciero.repositories.SavingsGoalRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.savingsGoalRoutes() {
    val repository = SavingsGoalRepository()

    route("/api/savings-goals") {
        // Resumen: totales (ahorrado/objetivo) + lista de metas del usuario.
        get {
            call.respond(ApiResponse(true, data = repository.getSummary(call.userId())))
        }

        post {
            val req = call.receive<CreateSavingsGoalRequest>()
            require(req.name.isNotBlank()) { "El nombre es obligatorio" }
            require(req.targetAmount > 0) { "El objetivo debe ser mayor a 0" }
            require(req.initialAmount >= 0) { "El monto inicial no puede ser negativo" }
            val created = repository.create(call.userId(), req)
            call.respond(HttpStatusCode.Created, ApiResponse(true, data = created))
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "ID inválido"))
            val req = call.receive<UpdateSavingsGoalRequest>()
            val updated = repository.update(call.userId(), id, req)
                ?: return@put call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Meta no encontrada"))
            call.respond(ApiResponse(true, data = updated))
        }

        // Aportar (amount > 0) o retirar (amount < 0) sobre el ahorrado.
        post("/{id}/contribute") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "ID inválido"))
            val req = call.receive<ContributeRequest>()
            require(req.amount != 0.0) { "El aporte no puede ser 0" }
            val updated = repository.contribute(call.userId(), id, req.amount)
                ?: return@post call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Meta no encontrada"))
            call.respond(ApiResponse(true, data = updated))
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "ID inválido"))
            val deleted = repository.delete(call.userId(), id)
            if (deleted) call.respond(ApiResponse(true, data = "Meta eliminada"))
            else call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Meta no encontrada"))
        }
    }
}
