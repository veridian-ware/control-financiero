package com.controlfinanciero.repositories

import com.controlfinanciero.database.DatabaseFactory.dbQuery
import com.controlfinanciero.models.db.Households
import com.controlfinanciero.models.db.Users
import com.controlfinanciero.models.dto.HouseholdDTO
import com.controlfinanciero.models.dto.HouseholdMemberDTO
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime

class HouseholdRepository {

    /** Hogar al que pertenece el usuario, con sus miembros. null si no está en ninguno. */
    suspend fun getForUser(userId: Int): HouseholdDTO? = dbQuery {
        val householdId = Users.selectAll().where { Users.id eq userId }
            .singleOrNull()?.get(Users.householdId) ?: return@dbQuery null
        loadHousehold(householdId)
    }

    /** Crea un hogar y mete al usuario adentro. Devuelve el hogar con su código de invitación. */
    suspend fun create(userId: Int, name: String): HouseholdDTO = dbQuery {
        val id = Households.insert {
            it[Households.name] = name
            it[inviteCode] = uniqueInviteCode()
            it[createdAt] = LocalDateTime.now()
        } get Households.id

        Users.update({ Users.id eq userId }) { it[householdId] = id }
        loadHousehold(id)!!
    }

    /** Une al usuario al hogar del código dado. null si el código no existe. */
    suspend fun join(userId: Int, inviteCode: String): HouseholdDTO? = dbQuery {
        val householdId = Households.selectAll().where { Households.inviteCode eq inviteCode }
            .singleOrNull()?.get(Households.id) ?: return@dbQuery null

        Users.update({ Users.id eq userId }) { it[Users.householdId] = householdId }
        loadHousehold(householdId)
    }

    /** Saca al usuario de su hogar (sus transacciones siguen siendo suyas). */
    suspend fun leave(userId: Int): Boolean = dbQuery {
        Users.update({ Users.id eq userId }) { it[householdId] = null } > 0
    }

    /**
     * Ids de usuarios cuyos datos debe ver este usuario: todos los miembros de su hogar,
     * o solo él mismo si no pertenece a ninguno. Base de la vista compartida.
     */
    suspend fun memberIds(userId: Int): List<Int> = dbQuery {
        val householdId = Users.selectAll().where { Users.id eq userId }
            .singleOrNull()?.get(Users.householdId) ?: return@dbQuery listOf(userId)
        Users.selectAll().where { Users.householdId eq householdId }.map { it[Users.id] }
    }

    private fun loadHousehold(householdId: Int): HouseholdDTO? {
        val row = Households.selectAll().where { Households.id eq householdId }.singleOrNull() ?: return null
        val members = Users.selectAll().where { Users.householdId eq householdId }
            .map { HouseholdMemberDTO(id = it[Users.id], email = it[Users.email]) }
        return HouseholdDTO(
            id = row[Households.id],
            name = row[Households.name],
            inviteCode = row[Households.inviteCode],
            members = members
        )
    }

    private fun uniqueInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // sin caracteres ambiguos
        repeat(10) {
            val code = (1..8).map { chars.random() }.joinToString("")
            val taken = Households.selectAll().where { Households.inviteCode eq code }.count() > 0
            if (!taken) return code
        }
        // Fallback prácticamente imposible de colisionar
        return (1..12).map { chars.random() }.joinToString("")
    }
}
