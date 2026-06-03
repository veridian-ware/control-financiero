package com.controlfinanciero.plugins

import com.controlfinanciero.routes.accountRoutes
import com.controlfinanciero.routes.authRoutes
import com.controlfinanciero.routes.budgetRoutes
import com.controlfinanciero.routes.categoryRoutes
import com.controlfinanciero.routes.dashboardRoutes
import com.controlfinanciero.routes.financialAccountRoutes
import com.controlfinanciero.routes.householdRoutes
import com.controlfinanciero.routes.investmentRoutes
import com.controlfinanciero.routes.mercadoPagoRoutes
import com.controlfinanciero.routes.recurringRoutes
import com.controlfinanciero.routes.savingsGoalRoutes
import com.controlfinanciero.routes.transactionRoutes
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        // Públicas (sin token)
        authRoutes(environment.config)

        // Protegidas por JWT
        authenticate(JWT_AUTH) {
            accountRoutes()
            categoryRoutes()
            budgetRoutes()
            transactionRoutes()
            recurringRoutes()
            dashboardRoutes()
            householdRoutes()
            financialAccountRoutes()
            investmentRoutes()
            savingsGoalRoutes()
            mercadoPagoRoutes(environment.config)
        }
    }
}
