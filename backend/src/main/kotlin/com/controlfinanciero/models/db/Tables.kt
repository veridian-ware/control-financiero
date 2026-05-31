package com.controlfinanciero.models.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object Users : Table("users") {
    val id = integer("id").autoIncrement()
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 60) // BCrypt produce hashes de 60 chars
    val mpAccessToken = varchar("mp_access_token", 255).nullable() // token de Mercado Pago por usuario
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}

object Categories : Table("categories") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val name = varchar("name", 100)
    val type = varchar("type", 10) // "ingreso" o "egreso"
    val icon = varchar("icon", 50).nullable()
    val color = varchar("color", 7).nullable() // hex color
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}

object Transactions : Table("transactions") {
    val id = long("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val amount = decimal("amount", 12, 2)
    val description = varchar("description", 500)
    val type = varchar("type", 10) // "ingreso" o "egreso"
    val categoryId = integer("category_id").references(Categories.id)
    val date = datetime("date")
    val sourceCol = varchar("source", 50).default("manual") // propiedad renombrada: "source" choca con ColumnSet.source
    val externalId = varchar("external_id", 100).nullable() // ID de Mercado Pago
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}
