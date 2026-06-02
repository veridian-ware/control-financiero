package com.controlfinanciero.routes

import com.controlfinanciero.models.dto.ApiResponse
import com.controlfinanciero.models.dto.CreateInvestmentRequest
import com.controlfinanciero.models.dto.UpdateInvestmentRequest
import com.controlfinanciero.plugins.userId
import com.controlfinanciero.repositories.InvestmentRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.investmentRoutes() {
    val repository = InvestmentRepository()

    route("/api/investments") {
        // Resumen: totales + lista de inversiones del usuario.
        get {
            call.respond(ApiResponse(true, data = repository.getSummary(call.userId())))
        }

        post {
            val req = call.receive<CreateInvestmentRequest>()
            require(req.name.isNotBlank()) { "El nombre es obligatorio" }
            require(req.type.isNotBlank()) { "El tipo es obligatorio" }
            require(req.amountInvested >= 0) { "El monto invertido no puede ser negativo" }
            require((req.currentValue ?: 0.0) >= 0) { "El valor actual no puede ser negativo" }
            val created = repository.create(call.userId(), req)
            call.respond(HttpStatusCode.Created, ApiResponse(true, data = created))
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "ID inválido"))
            val req = call.receive<UpdateInvestmentRequest>()
            val updated = repository.update(call.userId(), id, req)
                ?: return@put call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Inversión no encontrada"))
            call.respond(ApiResponse(true, data = updated))
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "ID inválido"))
            val deleted = repository.delete(call.userId(), id)
            if (deleted) call.respond(ApiResponse(true, data = "Inversión eliminada"))
            else call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Inversión no encontrada"))
        }
    }
}
