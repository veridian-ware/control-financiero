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

// Cuenta/billetera del usuario (efectivo, banco, MP, Brubank…). El saldo se calcula como
// saldo inicial + ingresos − egresos de las transacciones asociadas (account_id).
object Accounts : Table("accounts") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val name = varchar("name", 100)
    val type = varchar("type", 20) // efectivo | banco | billetera | otro
    val initialBalance = decimal("initial_balance", 14, 2)
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
    val accountId = integer("account_id").references(Accounts.id).nullable() // cuenta/billetera (opcional)
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

// Inversión del usuario, carga manual (sin datos de mercado en vivo). El rendimiento se
// calcula como (current_value - amount_invested); el usuario actualiza current_value a mano.
object Investments : Table("investments") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val name = varchar("name", 200)
    val type = varchar("type", 30) // acciones | cripto | plazo_fijo | fci | dolares | otro
    val amountInvested = decimal("amount_invested", 14, 2)
    val currentValue = decimal("current_value", 14, 2)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    override val primaryKey = PrimaryKey(id)
}

// Presupuesto mensual por categoría (egreso). Un presupuesto por categoría y usuario.
// El "gastado" se calcula al vuelo desde las transacciones del mes (no se almacena).
object Budgets : Table("budgets") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val categoryId = integer("category_id").references(Categories.id)
    val monthlyLimit = decimal("monthly_limit", 14, 2)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    override val primaryKey = PrimaryKey(id)
    init { uniqueIndex(userId, categoryId) }
}

// Meta de ahorro del usuario (carga manual, standalone como Investments): monto objetivo +
// ahorrado acumulado + fecha límite opcional. El progreso se calcula al vuelo. El usuario
// "aporta" (suma) o retira (resta) sobre current_amount; no toca transacciones ni cuentas.
object SavingsGoals : Table("savings_goals") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val name = varchar("name", 200)
    val targetAmount = decimal("target_amount", 14, 2)
    val currentAmount = decimal("current_amount", 14, 2)
    val deadline = date("deadline").nullable() // fecha límite opcional
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    override val primaryKey = PrimaryKey(id)
}
