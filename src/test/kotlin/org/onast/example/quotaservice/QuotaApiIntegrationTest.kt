package org.onast.example.quotaservice

import org.onast.example.quotaservice.plugins.*
import org.onast.example.quotaservice.quota.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class QuotaApiIntegrationTest {

    @Test
    fun `POST quota consume returns 200 and quota headers`() = testApplication {
        application {
            configureLogging()
            configureSerialization()
            configureStatusPages()

            val policy = QuotaPolicy(limitUnits = 100, windowMs = 60_000L)
            val service = QuotaService(policy, clock = Clock { 0L })

            configureRouting(service, policy)
        }

        val resp = client.post("/quota/consume") {
            header("X-Api-Key", "k1")
            contentType(ContentType.Application.Json)
            setBody("""{"units":10}""")
        }

        assertEquals(HttpStatusCode.OK, resp.status)

        // headers exist
        assertEquals("100", resp.headers["X-Quota-Limit"])
        assertNotNull(resp.headers["X-Quota-Remaining"])
        assertNotNull(resp.headers["X-Quota-Reset-Millis"])
    }

    @Test
    fun `POST quota consume returns 429 when exceeded`() = testApplication {
        application {
            configureLogging()
            configureSerialization()
            configureStatusPages()

            val policy = QuotaPolicy(limitUnits = 10, windowMs = 60_000L)
            val service = QuotaService(policy, clock = Clock { 0L })

            configureRouting(service, policy)
        }

        // consume full quota
        val ok = client.post("/quota/consume") {
            header("X-Api-Key", "k1")
            contentType(ContentType.Application.Json)
            setBody("""{"units":10}""")
        }
        assertEquals(HttpStatusCode.OK, ok.status)

        // now exceed
        val tooMuch = client.post("/quota/consume") {
            header("X-Api-Key", "k1")
            contentType(ContentType.Application.Json)
            setBody("""{"units":1}""")
        }
        assertEquals(HttpStatusCode.TooManyRequests, tooMuch.status)
    }

    @Test
    fun `missing API key returns 400`() = testApplication {
        application {
            configureLogging()
            configureSerialization()
            configureStatusPages()

            val policy = QuotaPolicy(limitUnits = 100, windowMs = 60_000L)
            val service = QuotaService(policy, clock = Clock { 0L })

            configureRouting(service, policy)
        }

        val resp = client.post("/quota/consume") {
            contentType(ContentType.Application.Json)
            setBody("""{"units":10}""")
        }

        assertEquals(HttpStatusCode.BadRequest, resp.status)
    }

    @Test
    fun `invalid JSON returns 400`() = testApplication {
        application {
            configureLogging()
            configureSerialization()
            configureStatusPages()

            val policy = QuotaPolicy(limitUnits = 100, windowMs = 60_000L)
            val service = QuotaService(policy, clock = Clock { 0L })

            configureRouting(service, policy)
        }

        val resp = client.post("/quota/consume") {
            header("X-Api-Key", "k1")
            contentType(ContentType.Application.Json)
            setBody("""{"units": }""") // invalid JSON
        }

        assertEquals(HttpStatusCode.BadRequest, resp.status)
    }
}
