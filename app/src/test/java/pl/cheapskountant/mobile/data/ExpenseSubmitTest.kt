package pl.cheapskountant.mobile.data

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.cheapskountant.mobile.data.remote.ApiClient
import pl.cheapskountant.mobile.data.remote.ExpensePayload
import pl.cheapskountant.mobile.data.remote.RemoteCall
import pl.cheapskountant.mobile.domain.ExpenseDraft
import pl.cheapskountant.mobile.domain.ExpenseIssue
import pl.cheapskountant.mobile.domain.ExpenseValidation
import pl.cheapskountant.mobile.domain.ExpenseValidator
import pl.cheapskountant.mobile.domain.OutboxTransitions
import pl.cheapskountant.mobile.domain.SendOutcome
import pl.cheapskountant.mobile.domain.ValidExpenseLine
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

class ExpenseSubmitTest {
    private val remote = RemoteCall()

    @Test
    fun postsJsonNumbersAndOmitsBlankDescription() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(201).setBody("[]").addHeader("Content-Type", "application/json"))
        server.start()
        try {
            val lines = listOf(
                ValidExpenseLine(null, BigDecimal("12.50"), "2026-10-16", "PLN", "Jedzenie"),
                ValidExpenseLine("Warzywa", BigDecimal("1.00"), "2026-10-16", "EUR", "Jedzenie"),
            )
            val outcome = remote.execute(true) { api(server).storeExpenses(ExpensePayload.encode(lines)) }
            val recorded = server.takeRequest()
            assertEquals("Bearer example-key", recorded.getHeader("Authorization"))
            assertTrue(recorded.path!!.endsWith("api/expense"))
            val body = recorded.body.readUtf8()
            val array = Json.parseToJsonElement(body).jsonArray
            assertEquals(2, array.size)
            assertEquals("12.50", array[0].jsonObject["amount"]!!.jsonPrimitive.content)
            assertFalse(array[0].jsonObject.containsKey("description"))
            assertEquals("Warzywa", array[1].jsonObject["description"]!!.jsonPrimitive.content)
            assertTrue(outcome is SendOutcome.Success)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun spacesOnlyDescriptionIsNotPosted() {
        val result = ExpenseValidator.validate(
            listOf(ExpenseDraft("   ", "12,50", LocalDate.of(2026, 10, 16), "PLN", "Jedzenie")),
            setOf("Jedzenie"),
            Locale("pl"),
        )
        assertTrue(result is ExpenseValidation.Bad)
        assertEquals(ExpenseIssue.DescriptionSpaces, (result as ExpenseValidation.Bad).issue)
    }

    @Test
    fun encodesUpToFiveHundredLines() {
        val lines = List(500) {
            ValidExpenseLine(null, BigDecimal("1.00"), "2026-10-16", "PLN", "Jedzenie")
        }
        assertEquals(500, ExpensePayload.encode(lines).size)
    }

    @Test
    fun httpErrorAndMissingStatusAndOffline() = runTest {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setResponseCode(401).setBody("""{"reason":"not authorized","explanation":"rejected"}"""),
        )
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        server.start()
        try {
            val api = api(server)
            val body = ExpensePayload.encode(
                listOf(ValidExpenseLine(null, BigDecimal("1.00"), "2026-10-16", "PLN", "Jedzenie")),
            )
            val http = remote.execute(true) { api.storeExpenses(body) }
            assertTrue(http is SendOutcome.HttpError)
            assertEquals(401, (http as SendOutcome.HttpError).code)
            assertEquals(OutboxTransitions.ERROR, OutboxTransitions.statusAfter(http, OutboxTransitions.READY))
            val dropped = remote.execute(true) { api.storeExpenses(body) }
            assertEquals(SendOutcome.NoStatus, dropped)
            val requestCount = server.requestCount
            val offline = remote.execute(false) { api.storeExpenses(body) }
            assertEquals(SendOutcome.NoNetwork, offline)
            assertEquals(requestCount, server.requestCount)
            assertEquals(OutboxTransitions.WAITING, OutboxTransitions.statusAfter(offline, OutboxTransitions.READY))
        } finally {
            server.shutdown()
        }
    }

    private fun api(server: MockWebServer) = ApiClient.create(server.url("/").toString(), "example-key")
}
