package com.controlfinanciero.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class UserDTO(
    val id: Int,
    val email: String,
    val hasMercadoPagoToken: Boolean = false
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserDTO
)

@Serializable
data class SetMpTokenRequest(
    val accessToken: String
)
