package org.onast.example.quotaservice.plugins

import org.onast.example.quotaservice.api.ConsumeRequest
import org.onast.example.quotaservice.api.ConsumeResponse
import org.onast.example.quotaservice.quota.QuotaDecision
import org.onast.example.quotaservice.quota.QuotaPolicy
import org.onast.example.quotaservice.quota.QuotaService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory


private val quotaPolicy = QuotaPolicy(limitUnits = 100, windowMs = 60 * 60 * 1000L)

private val quotaService = QuotaService(policy = quotaPolicy)

private val logger = LoggerFactory.getLogger("Routing")

fun Application.configureRouting() {
    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        post("/quota/consume") {
            val apiKey = call.request.headers["X-Api-Key"]?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: throw IllegalArgumentException("Missing X-Api-Key header")

            val req = call.receive<ConsumeRequest>()

            logger.info("consume apiKey={}, units={}", apiKey, req.units)
            val decision = quotaService.consume(apiKey = apiKey, units = req.units)

            when (decision) {
                is QuotaDecision.Accepted -> {
                    call.response.headers.append("X-Quota-Limit", quotaPolicy.limitUnits.toString())
                    call.response.headers.append("X-Quota-Remaining", decision.remaining.toString())
                    call.response.headers.append("X-Quota-Reset-Millis", decision.resetAtMs.toString())
                    call.respond(
                        HttpStatusCode.OK,
                        ConsumeResponse(true, decision.remaining, decision.resetAtMs)
                    )
                }
                is QuotaDecision.Rejected -> {
                    call.response.headers.append("X-Quota-Limit", quotaPolicy.limitUnits.toString())
                    call.response.headers.append("X-Quota-Remaining", decision.remaining.toString())
                    call.response.headers.append("X-Quota-Reset-Millis", decision.resetAtMs.toString())
                    call.respond(
                        HttpStatusCode.TooManyRequests,
                        ConsumeResponse(false, decision.remaining, decision.resetAtMs)
                    )
                }
            }
        }
    }
}
