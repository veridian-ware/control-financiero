package com.controlfinanciero

import com.controlfinanciero.database.DatabaseFactory
import com.controlfinanciero.plugins.*
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    DatabaseFactory.init(environment.config)
    configureSerialization()
    configureCORS()
    configureStatusPages()
    configureRouting()
}
