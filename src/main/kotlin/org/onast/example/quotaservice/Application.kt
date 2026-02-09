package org.onast.example.quotaservice

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.onast.example.quotaservice.plugins.configureLogging
import org.onast.example.quotaservice.plugins.configureRouting
import org.onast.example.quotaservice.plugins.configureSerialization
import org.onast.example.quotaservice.plugins.configureStatusPages
import org.onast.example.quotaservice.quota.QuotaPolicy
import org.onast.example.quotaservice.quota.QuotaService

fun main() {
    embeddedServer(
        Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    configureLogging()
    configureSerialization()
    configureStatusPages()

    val policy = QuotaPolicy(limitUnits = 100, windowMs = 60 * 60 * 1000L)
    val service = QuotaService(policy)
    configureRouting(service, policy)
}