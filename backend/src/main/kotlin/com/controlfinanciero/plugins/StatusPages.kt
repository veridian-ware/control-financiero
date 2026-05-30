package com.controlfinanciero.plugins

import com.controlfinanciero.models.dto.ApiResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = cause.message))
        }
        exception<Exception> { call, cause ->
            call.application.environment.log.error("Error no manejado", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiResponse<Unit>(false, message = "Error interno del servidor")
            )
        }
    }
}
