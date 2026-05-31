package com.controlfinanciero.routes

import com.controlfinanciero.models.dto.ApiResponse
import com.controlfinanciero.plugins.userId
import com.controlfinanciero.repositories.TransactionRepository
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDateTime
import java.time.YearMonth

fun Route.dashboardRoutes() {
    val repository = TransactionRepository()

    route("/api/dashboard") {
        get {
            val now = LocalDateTime.now()
            val from = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
            val to = YearMonth.from(now).atEndOfMonth().atTime(23, 59, 59)
            val dashboard = repository.getDashboard(call.userId(), from, to)
            call.respond(ApiResponse(true, data = dashboard))
        }

        get("/monthly/{year}") {
            val year = call.parameters["year"]?.toIntOrNull() ?: LocalDateTime.now().year
            val report = repository.getMonthlyReport(call.userId(), year)
            call.respond(ApiResponse(true, data = report))
        }
    }
}
