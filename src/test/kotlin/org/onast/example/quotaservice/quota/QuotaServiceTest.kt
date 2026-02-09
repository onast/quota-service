package org.onast.example.quotaservice.quota

import kotlin.test.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class QuotaServiceTest {

    @Test
    fun `reject when request would exceed limit`() {
        val clock = Clock { 0L }
        val service = QuotaService(QuotaPolicy(limitUnits = 100, windowMs = 3600_000L), clock)

        assertTrue(service.consume("k", 10) is QuotaDecision.Accepted)
        assertTrue(service.consume("k", 1000) is QuotaDecision.Rejected)
    }

    @Test
    fun `resets after window`() {
        var now = 0L
        val clock = Clock { now }
        val service = QuotaService(QuotaPolicy(limitUnits = 100, windowMs = 1000L), clock)

        assertTrue(service.consume("k", 100) is QuotaDecision.Accepted)
        assertTrue(service.consume("k", 1) is QuotaDecision.Rejected)

        now = 1000L // window expires
        assertTrue(service.consume("k", 1) is QuotaDecision.Accepted)
    }

    @Test
    fun `does not exceed quota under concurrent requests`() {
        val clock = Clock { 0L }
        val limit = 1000
        val service = QuotaService(QuotaPolicy(limitUnits = limit, windowMs = 3600_000L), clock)

        val threads = 20
        val requestsPerThread = 200
        val unitsPerRequest = 1

        val pool = Executors.newFixedThreadPool(threads)
        val start = CountDownLatch(1)
        val done = CountDownLatch(threads)

        val accepted = AtomicInteger(0)
        val rejected = AtomicInteger(0)

        repeat(threads) {
            pool.submit {
                start.await()
                repeat(requestsPerThread) {
                    when (service.consume("k", unitsPerRequest)) {
                        is QuotaDecision.Accepted -> accepted.incrementAndGet()
                        is QuotaDecision.Rejected -> rejected.incrementAndGet()
                    }
                }
                done.countDown()
            }
        }

        start.countDown()
        done.await()
        pool.shutdown()

        // accepted can never exceed limit
        assertTrue(accepted.get() <= limit)
        // total = accepted + rejected should equal all attempts
        assertEquals(threads * requestsPerThread, accepted.get() + rejected.get())
    }
}