package com.controlfinanciero.models.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object Categories : Table("categories") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100)
    val type = varchar("type", 10) // "ingreso" o "egreso"
    val icon = varchar("icon", 50).nullable()
    val color = varchar("color", 7).nullable() // hex color
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}

object Transactions : Table("transactions") {
    val id = long("id").autoIncrement()
    val amount = decimal("amount", 12, 2)
    val description = varchar("description", 500)
    val type = varchar("type", 10) // "ingreso" o "egreso"
    val categoryId = integer("category_id").references(Categories.id)
    val date = datetime("date")
    val source = varchar("source", 50).default("manual") // "manual", "mercadopago"
    val externalId = varchar("external_id", 100).nullable() // ID de Mercado Pago
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}
