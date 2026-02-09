package org.onast.example.quotaservice.quota

import java.util.concurrent.ConcurrentHashMap

class QuotaService(
    private val policy: QuotaPolicy,
    private val clock: Clock = SystemClock
) {
    // per API key state
    private data class Bucket(var windowStartMs: Long, var used: Int)

    private val buckets = ConcurrentHashMap<String, Bucket>()

    fun consume(apiKey: String, units: Int): QuotaDecision {
        require(apiKey.isNotBlank()) { "Missing X-Api-Key" }
        require(units > 0) { "Units must be positive" }
        require(policy.limitUnits > 0) { "Policy limit must be positive" }
        require(policy.windowMs > 0) { "Policy window must be positive" }

        val now = clock.nowMs()

        var decision: QuotaDecision? = null

        buckets.compute(apiKey) { _, existing ->
            val bucket = existing ?: Bucket(windowStartMs = now, used = 0)

            // reset window if expired
            if (now - bucket.windowStartMs >= policy.windowMs) {
                bucket.windowStartMs = now
                bucket.used = 0
            }

            val resetAt = bucket.windowStartMs + policy.windowMs
            val newUsed = bucket.used + units

            decision =
                if (newUsed <= policy.limitUnits) {
                    bucket.used = newUsed
                    QuotaDecision.Accepted(
                        remaining = policy.limitUnits - bucket.used,
                        resetAtMs = resetAt
                    )
                } else {
                    QuotaDecision.Rejected(
                        remaining = policy.limitUnits - bucket.used, // unchanged
                        resetAtMs = resetAt
                    )
                }

            bucket
        }

        return decision ?: error("Unexpected state: decision not computed")
    }
}