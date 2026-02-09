package org.onast.example.quotaservice.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("StatusPages")

fun Application.configureStatusPages() {
    install(StatusPages) {

        exception<IllegalArgumentException> { call, cause ->
            logger.info("Bad request: {}", cause.message)
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to (cause.message ?: "bad request"))
            )
        }

        exception<BadRequestException> { call, cause ->
            logger.info("Bad request body: {}", cause.message)
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid request body"))
        }

        exception<Throwable> { call, cause ->
            logger.error("Unhandled error", cause)   // <-- stack trace will appear in IDEA
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to "internal error")
            )
        }
    }
}