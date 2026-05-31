package com.controlfinanciero.routes

import com.controlfinanciero.models.dto.ApiResponse
import com.controlfinanciero.models.dto.CreateHouseholdRequest
import com.controlfinanciero.models.dto.JoinHouseholdRequest
import com.controlfinanciero.plugins.userId
import com.controlfinanciero.repositories.HouseholdRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.householdRoutes() {
    val households = HouseholdRepository()

    route("/api/household") {
        // Hogar actual del usuario (o success=false si no está en ninguno).
        get {
            val household = households.getForUser(call.userId())
            if (household != null) call.respond(ApiResponse(true, data = household))
            else call.respond(ApiResponse<Unit>(false, message = "No pertenecés a ningún hogar"))
        }

        post {
            val req = call.receive<CreateHouseholdRequest>()
            require(req.name.isNotBlank()) { "El nombre no puede estar vacío" }
            val created = households.create(call.userId(), req.name.trim())
            call.respond(HttpStatusCode.Created, ApiResponse(true, data = created))
        }

        // Unirse a un hogar existente con su código de invitación.
        post("/join") {
            val req = call.receive<JoinHouseholdRequest>()
            val code = req.inviteCode.trim().uppercase()
            val joined = households.join(call.userId(), code)
                ?: return@post call.respond(
                    HttpStatusCode.NotFound,
                    ApiResponse<Unit>(false, message = "Código de invitación inválido")
                )
            call.respond(ApiResponse(true, data = joined, message = "Te uniste al hogar"))
        }

        post("/leave") {
            households.leave(call.userId())
            call.respond(ApiResponse(true, data = "Saliste del hogar"))
        }
    }
}
