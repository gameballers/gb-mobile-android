package com.gameball.gameball.inappmessaging.data

import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RemoteMessageSourceTest {

    private lateinit var server: MockWebServer
    private lateinit var source: RemoteMessageSource

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
            .create(IamApi::class.java)
        source = RemoteMessageSource(
            api = api,
            localeProvider = { "en" },
            appVersion = "1.0.0",
            sdkVersion = "3.3.0"
        )
    }

    @After
    fun tearDown() = server.shutdown()

    private fun fetch() = runBlocking { source.fetch("moaty-survey-3") }

    @Test
    fun `the sync body carries platform 2, the customer and the resolved locale`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"messages":[]}"""))
        fetch()
        val body = server.takeRequest().body.readUtf8()
        assertTrue("platform must be 2", body.contains("\"platform\":2"))
        assertTrue(body.contains("\"customerId\":\"moaty-survey-3\""))
        assertTrue(body.contains("\"locale\":\"en\""))
        assertTrue(body.contains("\"sdkVersion\":\"3.3.0\""))
        assertTrue(body.contains("\"appVersion\":\"1.0.0\""))
    }

    @Test
    fun `the request goes to the v4_0 sync path`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"messages":[]}"""))
        fetch()
        assertEquals(
            "/api/v4.0/integrations/inapp-messages/sync",
            server.takeRequest().path
        )
    }

    @Test
    fun `a 200 payload is parsed into campaigns and the raw body is carried for caching`() {
        val payload = """
            { "cooldownSeconds": 15, "messages": [ { "campaignId": 7, "messageType": 2,
              "trigger": { "type": "session_start" }, "content": {},
              "locale": { "header": "Hi" } } ] }
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(payload))
        val outcome = fetch() as SyncOutcome.Success
        assertEquals(1, outcome.result.campaigns.size)
        assertEquals(15, outcome.result.cooldownSeconds)
        assertTrue(outcome.rawPayload.contains("\"campaignId\""))
    }

    /** An empty successful sync REPLACES the cache; it must not read as a failure. */
    @Test
    fun `a 200 with an empty messages array is a success, not a failure`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"messages":[]}"""))
        val outcome = fetch()
        assertTrue(outcome is SyncOutcome.Success)
        assertTrue((outcome as SyncOutcome.Success).result.campaigns.isEmpty())
    }

    /** The payload was served, so this is not a network failure — the parser handles it. */
    @Test
    fun `a 200 with an unparseable body is a success carrying no campaigns`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{ not json"))
        val outcome = fetch()
        assertTrue(outcome is SyncOutcome.Success)
        assertTrue((outcome as SyncOutcome.Success).result.campaigns.isEmpty())
    }

    @Test
    fun `400 and 401 are permanent failures`() {
        server.enqueue(MockResponse().setResponseCode(400))
        assertTrue((fetch() as SyncOutcome.Failure).permanent)
        server.enqueue(MockResponse().setResponseCode(401))
        assertTrue((fetch() as SyncOutcome.Failure).permanent)
    }

    /**
     * Two very different problems behind one status: an unknown customer self-heals on the
     * next warm resume, while a bare 404 means the endpoints are not deployed here at all.
     */
    @Test
    fun `404 with a body and 404 without one are reported differently`() {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"code":"PlayerNotFound"}"""))
        val withBody = fetch() as SyncOutcome.Failure
        server.enqueue(MockResponse().setResponseCode(404))
        val withoutBody = fetch() as SyncOutcome.Failure

        assertNotEquals(withBody.reason, withoutBody.reason)
        assertFalse("an unknown customer self-heals", withBody.permanent)
        assertTrue("an undeployed endpoint does not", withoutBody.permanent)
        assertTrue(withoutBody.reason.contains("alpha-only"))
    }

    @Test
    fun `422 is a permanent failure`() {
        server.enqueue(MockResponse().setResponseCode(422).setBody("""{"code":"PlayerInactive"}"""))
        assertTrue((fetch() as SyncOutcome.Failure).permanent)
    }

    @Test
    fun `503 is a transient failure`() {
        server.enqueue(MockResponse().setResponseCode(503))
        val failure = fetch() as SyncOutcome.Failure
        assertFalse(failure.permanent)
        assertTrue(failure.reason.contains("503"))
    }

    @Test
    fun `a socket failure is a transient failure and does not throw`() {
        server.shutdown()
        val outcome = fetch()
        assertTrue(outcome is SyncOutcome.Failure)
        assertFalse((outcome as SyncOutcome.Failure).permanent)
    }
}
