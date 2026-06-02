package com.controlfinanciero.routes

import com.controlfinanciero.models.dto.ApiResponse
import com.controlfinanciero.models.dto.CreateRecurringRequest
import com.controlfinanciero.plugins.userId
import com.controlfinanciero.repositories.RecurringTransactionRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDate

fun Route.recurringRoutes() {
    val repository = RecurringTransactionRepository()

    route("/api/recurring") {
        // Lista los fijos del usuario con sus vencimientos del mes (pendientes/pagados).
        get {
            call.respond(ApiResponse(true, data = repository.getAll(call.userId())))
        }

        post {
            val req = call.receive<CreateRecurringRequest>()
            require(req.type in listOf("ingreso", "egreso")) { "Tipo debe ser 'ingreso' o 'egreso'" }
            require(req.amount > 0) { "El monto debe ser mayor a 0" }
            require(req.frequency in RecurringTransactionRepository.FREQUENCIES) {
                "Frecuencia debe ser 'semanal', 'quincenal' o 'mensual'"
            }
            require(runCatching { LocalDate.parse(req.anchorDate) }.isSuccess) {
                "Fecha de inicio inválida (formato yyyy-MM-dd)"
            }
            val created = repository.create(call.userId(), req)
            call.respond(HttpStatusCode.Created, ApiResponse(true, data = created))
        }

        // Marca un vencimiento como pagado (crea la transacción real que cuenta en el dashboard).
        post("/occurrences/{id}/pay") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "ID inválido"))
            val ok = repository.markPaid(call.userId(), id)
            if (ok) call.respond(ApiResponse(true, data = "Vencimiento pagado"))
            else call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Vencimiento no encontrado"))
        }

        // Vuelve un vencimiento a "pendiente" (borra la transacción asociada).
        post("/occurrences/{id}/unpay") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "ID inválido"))
            val ok = repository.markPending(call.userId(), id)
            if (ok) call.respond(ApiResponse(true, data = "Vencimiento pendiente"))
            else call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Vencimiento no encontrado"))
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "ID inválido"))
            val deleted = repository.delete(call.userId(), id)
            if (deleted) call.respond(ApiResponse(true, data = "Fijo eliminado"))
            else call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Fijo no encontrado"))
        }
    }
}
