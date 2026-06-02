package com.controlfinanciero.routes

import com.controlfinanciero.models.dto.ApiResponse
import com.controlfinanciero.models.dto.CreateAccountRequest
import com.controlfinanciero.models.dto.UpdateAccountRequest
import com.controlfinanciero.plugins.userId
import com.controlfinanciero.repositories.AccountRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

// Cuentas/billeteras financieras del usuario (no confundir con accountRoutes() de auth).
fun Route.financialAccountRoutes() {
    val repository = AccountRepository()

    route("/api/accounts") {
        // Resumen: patrimonio total + lista de cuentas con su saldo.
        get {
            call.respond(ApiResponse(true, data = repository.getSummary(call.userId())))
        }

        post {
            val req = call.receive<CreateAccountRequest>()
            require(req.name.isNotBlank()) { "El nombre es obligatorio" }
            require(req.type in AccountRepository.TYPES) {
                "Tipo debe ser 'efectivo', 'banco', 'billetera' u 'otro'"
            }
            val created = repository.create(call.userId(), req)
            call.respond(HttpStatusCode.Created, ApiResponse(true, data = created))
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "ID inválido"))
            val req = call.receive<UpdateAccountRequest>()
            req.type?.let {
                require(it in AccountRepository.TYPES) { "Tipo inválido" }
            }
            val ok = repository.update(call.userId(), id, req)
            if (ok) call.respond(ApiResponse(true, data = "Cuenta actualizada"))
            else call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Cuenta no encontrada"))
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "ID inválido"))
            val deleted = repository.delete(call.userId(), id)
            if (deleted) call.respond(ApiResponse(true, data = "Cuenta eliminada"))
            else call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Cuenta no encontrada"))
        }
    }
}
