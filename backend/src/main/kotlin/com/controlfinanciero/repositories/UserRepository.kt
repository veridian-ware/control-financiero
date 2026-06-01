package com.controlfinanciero.repositories

import com.controlfinanciero.database.DatabaseFactory.dbQuery
import com.controlfinanciero.models.db.Users
import com.controlfinanciero.models.dto.UserDTO
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime

/** Credenciales internas (incluye el hash). No se serializa nunca hacia el cliente. */
data class UserCredentials(val id: Int, val email: String, val passwordHash: String)

class UserRepository {

    private fun ResultRow.toUserDTO() = UserDTO(
        id = this[Users.id],
        email = this[Users.email],
        name = this[Users.name],
        hasMercadoPagoToken = this[Users.mpAccessToken] != null
    )

    suspend fun emailExists(email: String): Boolean = dbQuery {
        Users.selectAll().where { Users.email eq email }.count() > 0
    }

    suspend fun create(email: String, passwordHash: String, name: String? = null): UserDTO = dbQuery {
        val id = Users.insert {
            it[Users.email] = email
            it[Users.passwordHash] = passwordHash
            it[Users.name] = name
            it[createdAt] = LocalDateTime.now()
        } get Users.id
        UserDTO(id = id, email = email, name = name, hasMercadoPagoToken = false)
    }

    suspend fun findCredentialsByEmail(email: String): UserCredentials? = dbQuery {
        Users.selectAll().where { Users.email eq email }.singleOrNull()?.let {
            UserCredentials(it[Users.id], it[Users.email], it[Users.passwordHash])
        }
    }

    suspend fun getById(id: Int): UserDTO? = dbQuery {
        Users.selectAll().where { Users.id eq id }.singleOrNull()?.toUserDTO()
    }

    suspend fun getMpToken(userId: Int): String? = dbQuery {
        Users.selectAll().where { Users.id eq userId }.singleOrNull()?.get(Users.mpAccessToken)
    }

    suspend fun setMpToken(userId: Int, token: String): Boolean = dbQuery {
        Users.update({ Users.id eq userId }) {
            it[mpAccessToken] = token
        } > 0
    }

    suspend fun updateName(userId: Int, name: String): Boolean = dbQuery {
        Users.update({ Users.id eq userId }) {
            it[Users.name] = name
        } > 0
    }
}
