package pl.cheapskountant.mobile.data

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.cheapskountant.mobile.data.remote.ApiClient
import pl.cheapskountant.mobile.data.remote.ReceiptParts
import pl.cheapskountant.mobile.data.remote.RemoteCall
import pl.cheapskountant.mobile.domain.OutboxTransitions
import pl.cheapskountant.mobile.domain.SendOutcome

class ReceiptSubmitTest {
    private val remote = RemoteCall()

    @Test
    fun transcriptionRepeatsImagesInOrder() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"items":[]}""").addHeader("Content-Type", "application/json"))
        server.start()
        try {
            val parts = listOf(
                ReceiptParts.image("a.jpg", "image/jpeg", byteArrayOf(1, 2)),
                ReceiptParts.image("b.png", "image/png", byteArrayOf(3)),
                ReceiptParts.image("c.webp", "image/webp", byteArrayOf(4)),
            )
            val outcome = remote.execute(true) { api(server).transcribeReceipt(parts) }
            val recorded = server.takeRequest()
            val body = recorded.body.readUtf8()
            val contentType = recorded.getHeader("Content-Type").orEmpty()
            assertTrue(contentType.startsWith("multipart/form-data"))
            assertTrue(body.indexOf("a.jpg") < body.indexOf("b.png"))
            assertTrue(body.indexOf("b.png") < body.indexOf("c.webp"))
            assertTrue(body.contains("image/jpeg"))
            assertTrue(body.contains("image/png"))
            assertTrue(body.contains("image/webp"))
            assertTrue(outcome is SendOutcome.Success)
            assertEquals(OutboxTransitions.READY, OutboxTransitions.statusAfter(outcome, OutboxTransitions.READY))
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun receiptCreatedOn201AndErrorsStayInError() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"id":"1"}""").addHeader("Content-Type", "application/json"))
        server.enqueue(MockResponse().setResponseCode(422).setBody("""{"reason":"incomplete","explanation":"bad"}"""))
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        server.start()
        try {
            val api = api(server)
            val document = JsonObject(mapOf("seller" to JsonPrimitive("Sklep")))
            val created = remote.execute(true) { api.createReceipt(document) }
            assertTrue(created is SendOutcome.Success)
            val rejected = remote.execute(true) { api.createReceipt(document) }
            assertEquals(OutboxTransitions.ERROR, OutboxTransitions.statusAfter(rejected, OutboxTransitions.READY))
            val dropped = remote.execute(true) { api.createReceipt(document) }
            assertEquals(SendOutcome.NoStatus, dropped)
            val offline = remote.execute(false) { api.createReceipt(document) }
            assertEquals(OutboxTransitions.WAITING, OutboxTransitions.statusAfter(offline, OutboxTransitions.READY))
        } finally {
            server.shutdown()
        }
    }

    private fun api(server: MockWebServer) = ApiClient.create(server.url("/").toString(), "example-key")
}
