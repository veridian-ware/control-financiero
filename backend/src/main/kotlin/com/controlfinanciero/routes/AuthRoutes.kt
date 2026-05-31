package com.controlfinanciero.routes

import com.controlfinanciero.models.dto.*
import com.controlfinanciero.plugins.generateJwtToken
import com.controlfinanciero.plugins.userId
import com.controlfinanciero.repositories.CategoryRepository
import com.controlfinanciero.repositories.UserRepository
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.mindrot.jbcrypt.BCrypt

/** Rutas públicas de autenticación (sin token). */
fun Route.authRoutes(config: ApplicationConfig) {
    val users = UserRepository()
    val categories = CategoryRepository()

    route("/api/auth") {
        post("/register") {
            val req = call.receive<RegisterRequest>()
            val email = req.email.trim().lowercase()
            require(email.contains("@") && email.length >= 5) { "Email inválido" }
            require(req.password.length >= 6) { "La contraseña debe tener al menos 6 caracteres" }

            if (users.emailExists(email)) {
                return@post call.respond(
                    HttpStatusCode.Conflict,
                    ApiResponse<Unit>(false, message = "Ya existe un usuario con ese email")
                )
            }

            val hash = BCrypt.hashpw(req.password, BCrypt.gensalt())
            val user = users.create(email, hash)
            categories.seedDefaults(user.id) // categorías por defecto para el usuario nuevo

            val token = generateJwtToken(config, user.id, user.email)
            call.respond(HttpStatusCode.Created, ApiResponse(true, data = AuthResponse(token, user)))
        }

        post("/login") {
            val req = call.receive<LoginRequest>()
            val email = req.email.trim().lowercase()
            val creds = users.findCredentialsByEmail(email)
            if (creds == null || !BCrypt.checkpw(req.password, creds.passwordHash)) {
                return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Unit>(false, message = "Email o contraseña incorrectos")
                )
            }
            val user = users.getById(creds.id)!!
            val token = generateJwtToken(config, user.id, user.email)
            call.respond(ApiResponse(true, data = AuthResponse(token, user)))
        }
    }
}

/** Rutas de cuenta que requieren token (van dentro de `authenticate`). */
fun Route.accountRoutes() {
    val users = UserRepository()

    get("/api/auth/me") {
        val user = users.getById(call.userId())
            ?: return@get call.respond(
                HttpStatusCode.NotFound,
                ApiResponse<Unit>(false, message = "Usuario no encontrado")
            )
        call.respond(ApiResponse(true, data = user))
    }
}
