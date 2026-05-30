package com.controlfinanciero.plugins

import com.controlfinanciero.routes.categoryRoutes
import com.controlfinanciero.routes.dashboardRoutes
import com.controlfinanciero.routes.mercadoPagoRoutes
import com.controlfinanciero.routes.transactionRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        categoryRoutes()
        transactionRoutes()
        dashboardRoutes()
        mercadoPagoRoutes(environment.config)
    }
}
