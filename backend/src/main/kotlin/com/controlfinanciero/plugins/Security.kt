package com.controlfinanciero.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.controlfinanciero.models.dto.ApiResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.config.*
import io.ktor.server.response.*
import java.util.Date

private const val CLAIM_USER_ID = "userId"
private const val CLAIM_EMAIL = "email"

const val JWT_AUTH = "auth-jwt"

fun Application.configureSecurity() {
    val config = environment.config
    val secret = config.property("jwt.secret").getString()
    val issuer = config.property("jwt.issuer").getString()
    val audience = config.property("jwt.audience").getString()
    val realmValue = config.property("jwt.realm").getString()

    install(Authentication) {
        jwt(JWT_AUTH) {
            realm = realmValue
            verifier(
                JWT.require(Algorithm.HMAC256(secret))
                    .withIssuer(issuer)
                    .withAudience(audience)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim(CLAIM_USER_ID).asInt() != null) {
                    JWTPrincipal(credential.payload)
                } else null
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Unit>(false, message = "Token ausente, inválido o expirado")
                )
            }
        }
    }
}

/** Genera un JWT firmado para el usuario dado. */
fun generateJwtToken(config: ApplicationConfig, userId: Int, email: String): String {
    val secret = config.property("jwt.secret").getString()
    val issuer = config.property("jwt.issuer").getString()
    val audience = config.property("jwt.audience").getString()
    val validityMs = config.propertyOrNull("jwt.validityMs")?.getString()?.toLong() ?: 604_800_000L
    return JWT.create()
        .withIssuer(issuer)
        .withAudience(audience)
        .withClaim(CLAIM_USER_ID, userId)
        .withClaim(CLAIM_EMAIL, email)
        .withExpiresAt(Date(System.currentTimeMillis() + validityMs))
        .sign(Algorithm.HMAC256(secret))
}

/** Extrae el userId del JWT del request autenticado. Solo válido dentro de rutas `authenticate(JWT_AUTH)`. */
fun ApplicationCall.userId(): Int =
    principal<JWTPrincipal>()!!.payload.getClaim(CLAIM_USER_ID).asInt()
