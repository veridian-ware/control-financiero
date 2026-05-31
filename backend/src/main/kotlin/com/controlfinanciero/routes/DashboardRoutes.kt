package com.controlfinanciero.routes

import com.controlfinanciero.models.dto.ApiResponse
import com.controlfinanciero.plugins.userId
import com.controlfinanciero.repositories.HouseholdRepository
import com.controlfinanciero.repositories.RecurringTransactionRepository
import com.controlfinanciero.repositories.TransactionRepository
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDateTime
import java.time.YearMonth

fun Route.dashboardRoutes() {
    val repository = TransactionRepository()
    val households = HouseholdRepository()
    val recurring = RecurringTransactionRepository()

    route("/api/dashboard") {
        get {
            // Al abrir el dashboard generamos los ingresos/egresos recurrentes del mes que ya
            // vencieron (idempotente), así el sueldo y demás aparecen sin carga manual.
            recurring.materializeDue(call.userId())

            val scope = households.memberIds(call.userId())
            val now = LocalDateTime.now()
            val from = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
            val to = YearMonth.from(now).atEndOfMonth().atTime(23, 59, 59)
            val dashboard = repository.getDashboard(scope, from, to)
            call.respond(ApiResponse(true, data = dashboard))
        }

        get("/monthly/{year}") {
            recurring.materializeDue(call.userId())
            val scope = households.memberIds(call.userId())
            val year = call.parameters["year"]?.toIntOrNull() ?: LocalDateTime.now().year
            val report = repository.getMonthlyReport(scope, year)
            call.respond(ApiResponse(true, data = report))
        }
    }
}
