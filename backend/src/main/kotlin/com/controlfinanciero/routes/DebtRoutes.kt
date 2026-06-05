package com.controlfinanciero.routes

import com.controlfinanciero.models.dto.ApiResponse
import com.controlfinanciero.models.dto.CreateDebtRequest
import com.controlfinanciero.models.dto.UpdateDebtRequest
import com.controlfinanciero.plugins.userId
import com.controlfinanciero.repositories.DebtRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.debtRoutes() {
    val repository = DebtRepository()

    route("/api/debts") {
        // Resumen: totales (restante, mensual, vencidas/próximas) + lista de deudas.
        get {
            call.respond(ApiResponse(true, data = repository.getSummary(call.userId())))
        }

        post {
            val req = call.receive<CreateDebtRequest>()
            require(req.description.isNotBlank()) { "La descripción es obligatoria" }
            require(req.installmentAmount > 0) { "El monto de la cuota debe ser mayor a 0" }
            require(req.paidInstallments >= 0) { "Las cuotas pagadas no pueden ser negativas" }
            req.totalInstallments?.let { require(it > 0) { "El total de cuotas debe ser mayor a 0" } }
            val created = repository.create(call.userId(), req)
            call.respond(HttpStatusCode.Created, ApiResponse(true, data = created))
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "ID inválido"))
            val req = call.receive<UpdateDebtRequest>()
            val updated = repository.update(call.userId(), id, req)
                ?: return@put call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Deuda no encontrada"))
            call.respond(ApiResponse(true, data = updated))
        }

        // Registra el pago de una cuota: avanza el contador (empuja el vencimiento +1 mes).
        post("/{id}/pay") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "ID inválido"))
            val updated = repository.pay(call.userId(), id)
                ?: return@post call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Deuda no encontrada"))
            call.respond(ApiResponse(true, data = updated))
        }

        // Revierte el último pago.
        post("/{id}/unpay") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "ID inválido"))
            val updated = repository.unpay(call.userId(), id)
                ?: return@post call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Deuda no encontrada"))
            call.respond(ApiResponse(true, data = updated))
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "ID inválido"))
            val deleted = repository.delete(call.userId(), id)
            if (deleted) call.respond(ApiResponse(true, data = "Deuda eliminada"))
            else call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Deuda no encontrada"))
        }
    }
}
