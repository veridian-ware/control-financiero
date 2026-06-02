package com.controlfinanciero.models.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime

object Households : Table("households") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100)
    val inviteCode = varchar("invite_code", 12).uniqueIndex() // código para que otra persona se una
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}

object Users : Table("users") {
    val id = integer("id").autoIncrement()
    val email = varchar("email", 255).uniqueIndex()
    val name = varchar("name", 100).nullable() // nombre para mostrar en el perfil
    val passwordHash = varchar("password_hash", 60) // BCrypt produce hashes de 60 chars
    val mpAccessToken = varchar("mp_access_token", 255).nullable() // token de Mercado Pago por usuario
    val householdId = integer("household_id").references(Households.id).nullable() // hogar compartido (opcional)
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
    val externalId = varchar("external_id", 100).nullable() // ID de Mercado Pago / recurrente
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}

// Plantilla de ingreso/egreso fijo (ej: alquiler quincenal, haberes mensuales). Define la
// frecuencia y la fecha del primer vencimiento; los vencimientos concretos viven en
// RecurringOccurrences y arrancan "pendiente" hasta que el usuario los marca "pagado".
object RecurringTransactions : Table("recurring_transactions") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val amount = decimal("amount", 12, 2)
    val description = varchar("description", 500)
    val type = varchar("type", 10) // "ingreso" o "egreso"
    val categoryId = integer("category_id").references(Categories.id)
    val frequency = varchar("frequency", 10) // "semanal" | "quincenal" | "mensual"
    val anchorDate = date("anchor_date") // primer vencimiento; los demás se calculan por frecuencia
    val active = bool("active").default(true)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}

// Un vencimiento concreto de un fijo. Se genera de forma idempotente (uniqueIndex
// recurring_id + due_date) y arranca "pendiente"; al marcarlo "pagado" se crea la
// Transaction real (transaction_id) que cuenta en el dashboard.
object RecurringOccurrences : Table("recurring_occurrences") {
    val id = long("id").autoIncrement()
    val recurringId = integer("recurring_id").references(RecurringTransactions.id)
    val userId = integer("user_id").references(Users.id)
    val dueDate = date("due_date")
    val amount = decimal("amount", 12, 2)
    val status = varchar("status", 10).default("pendiente") // "pendiente" | "pagado"
    val transactionId = long("transaction_id").references(Transactions.id).nullable()
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
    init { uniqueIndex(recurringId, dueDate) }
}
