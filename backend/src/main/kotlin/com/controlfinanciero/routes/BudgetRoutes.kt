package com.controlfinanciero.routes

import com.controlfinanciero.models.dto.ApiResponse
import com.controlfinanciero.models.dto.CreateBudgetRequest
import com.controlfinanciero.models.dto.UpdateBudgetRequest
import com.controlfinanciero.plugins.userId
import com.controlfinanciero.repositories.BudgetRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.budgetRoutes() {
    val repository = BudgetRepository()

    route("/api/budgets") {
        // Presupuestos con el gastado del mes y el estado (excedido/% usado).
        get {
            call.respond(ApiResponse(true, data = repository.getAll(call.userId())))
        }

        // Crea o actualiza el presupuesto de una categoría (upsert).
        post {
            val req = call.receive<CreateBudgetRequest>()
            require(req.monthlyLimit > 0) { "El límite debe ser mayor a 0" }
            repository.upsert(call.userId(), req)
            call.respond(HttpStatusCode.Created, ApiResponse(true, data = "Presupuesto guardado"))
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "ID inválido"))
            val req = call.receive<UpdateBudgetRequest>()
            require(req.monthlyLimit > 0) { "El límite debe ser mayor a 0" }
            val ok = repository.update(call.userId(), id, req)
            if (ok) call.respond(ApiResponse(true, data = "Presupuesto actualizado"))
            else call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Presupuesto no encontrado"))
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "ID inválido"))
            val deleted = repository.delete(call.userId(), id)
            if (deleted) call.respond(ApiResponse(true, data = "Presupuesto eliminado"))
            else call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Presupuesto no encontrado"))
        }
    }
}
