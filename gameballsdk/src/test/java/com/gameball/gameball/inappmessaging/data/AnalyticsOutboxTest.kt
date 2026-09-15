package com.gameball.gameball.inappmessaging.data

import androidx.test.core.app.ApplicationProvider
import com.gameball.gameball.inappmessaging.runtime.Clock
import com.gameball.gameball.local.SharedPreferencesUtils
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
class AnalyticsOutboxTest {

    private lateinit var server: MockWebServer
    private lateinit var store: IamStore
    private lateinit var api: IamApi
    private val clock = Clock { 1_800_000_000_000L }

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        SharedPreferencesUtils.init(ApplicationProvider.getApplicationContext(), Gson())
        SharedPreferencesUtils.getInstance().clearData()
        store = IamStore(SharedPreferencesUtils.getInstance())
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
            .create(IamApi::class.java)
    }

    @After
    fun tearDown() = server.shutdown()

    private fun outbox() = PersistentAnalyticsOutbox(
        api, store, CoroutineScope(Dispatchers.Unconfined), clock
    )

    private fun event(
        uid: String = IamEvent.newUid(),
        customerId: String = "alice",
        campaignId: Int = 2055,
        type: IamEventType = IamEventType.IMPRESSION,
        buttonId: String? = null
    ) = IamEvent(
        eventUid = uid, customerId = customerId, dispatchId = "d-1", campaignId = campaignId,
        variationId = 20, type = type, occurredAtMillis = 1_787_826_600_123L, buttonId = buttonId
    )

    private val uuidV4 =
        Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")

    /** A non-GUID is a hard 400 that discards the entire batch, not one event. */
    @Test
    fun `eventUid is a valid v4 UUID`() {
        repeat(100) {
            val uid = IamEvent.newUid()
            assertTrue("not a v4 UUID: $uid", uuidV4.matches(uid))
        }
    }

    @Test
    fun `the uid is never regenerated on retry`() {
        val box = outbox()
        val uid = IamEvent.newUid()
        server.enqueue(MockResponse().setResponseCode(503))
        box.record(event(uid = uid))
        runBlocking { box.flush() }
        val first = server.takeRequest(5, TimeUnit.SECONDS)!!.body.readUtf8()

        server.enqueue(MockResponse().setResponseCode(202).setBody("""{"accepted":1,"rejected":0}"""))
        runBlocking { box.flush() }
        val second = server.takeRequest(5, TimeUnit.SECONDS)!!.body.readUtf8()

        assertTrue(first.contains(uid))
        assertTrue("the uid must be stable across retries", second.contains(uid))
    }

    /** Defect 4: narrowing success to exactly 200 reported every accepted event as a failure. */
    @Test
    fun `202 is accepted and clears the batch`() {
        val box = outbox()
        server.enqueue(MockResponse().setResponseCode(202).setBody("""{"accepted":1,"rejected":0}"""))
        box.record(event())
        runBlocking { box.flush() }
        assertEquals(0, box.queuedCount())
    }

    @Test
    fun `a 2xx with rejected greater than zero still clears the batch`() {
        val box = outbox()
        server.enqueue(MockResponse().setResponseCode(202).setBody("""{"accepted":0,"rejected":1}"""))
        box.record(event())
        runBlocking { box.flush() }
        assertEquals(0, box.queuedCount())
    }

    @Test
    fun `a 2xx with an unreadable body is still accepted`() {
        val box = outbox()
        server.enqueue(MockResponse().setResponseCode(202).setBody("not json at all"))
        box.record(event())
        runBlocking { box.flush() }
        assertEquals(0, box.queuedCount())
    }

    @Test
    fun `400, 401, 404 and 422 discard permanently`() {
        listOf(400, 401, 404, 422).forEach { code ->
            SharedPreferencesUtils.getInstance().clearData()
            val box = outbox()
            server.enqueue(MockResponse().setResponseCode(code))
            box.record(event())
            runBlocking { box.flush() }
            assertEquals("HTTP $code should discard", 0, box.queuedCount())
        }
    }

    /** The outbox is FIFO, so a permanently rejected batch at the head must not block it. */
    @Test
    fun `403 discards rather than blocking the queue forever`() {
        val box = outbox()
        server.enqueue(MockResponse().setResponseCode(403))
        box.record(event())
        runBlocking { box.flush() }
        assertEquals(0, box.queuedCount())
    }

    @Test
    fun `408, 429 and 503 keep the batch`() {
        listOf(408, 429, 503).forEach { code ->
            SharedPreferencesUtils.getInstance().clearData()
            val box = outbox()
            server.enqueue(MockResponse().setResponseCode(code))
            box.record(event())
            runBlocking { box.flush() }
            assertEquals("HTTP $code should retry", 1, box.queuedCount())
        }
    }

    @Test
    fun `status mapping is exhaustive`() {
        assertEquals(FlushOutcome.ACCEPTED, outcomeFor(200))
        assertEquals(FlushOutcome.ACCEPTED, outcomeFor(202))
        assertEquals(FlushOutcome.ACCEPTED, outcomeFor(299))
        assertEquals(FlushOutcome.RETRY, outcomeFor(408))
        assertEquals(FlushOutcome.RETRY, outcomeFor(429))
        assertEquals(FlushOutcome.RETRY, outcomeFor(500))
        assertEquals(FlushOutcome.RETRY, outcomeFor(503))
        assertEquals(FlushOutcome.DISCARD, outcomeFor(400))
        assertEquals(FlushOutcome.DISCARD, outcomeFor(403))
        assertEquals(FlushOutcome.DISCARD, outcomeFor(422))
    }

    /**
     * Chunking bounds the cost of a failed retry, and it bites on a large queue restored from
     * disk rather than on live recording, which flushes at ten.
     */
    @Test
    fun `a large restored queue is chunked at 50 per request`() {
        val seeded = outbox()
        repeat(120) { seeded.recordWithoutFlush(event(campaignId = it)) }

        val box = outbox()
        box.load()
        assertEquals(120, box.queuedCount())

        repeat(3) {
            server.enqueue(MockResponse().setResponseCode(202).setBody("""{"accepted":50}"""))
        }
        runBlocking { box.flush() }

        val sizes = (1..3).map {
            Regex("\"eventUid\"").findAll(server.takeRequest(5, TimeUnit.SECONDS)!!.body.readUtf8()).count()
        }
        assertEquals(listOf(50, 50, 20), sizes)
        assertEquals(0, box.queuedCount())
    }

    @Test
    fun `the ceiling drops the oldest and logs`() {
        val box = outbox()
        val firstUid = IamEvent.newUid()
        val lastUid = IamEvent.newUid()
        box.record(event(uid = firstUid))
        repeat(499) { box.record(event()) }
        box.record(event(uid = lastUid))   // the 501st

        assertEquals(500, box.queuedCount())
        val persisted = store.readRaw(IamStore.Slot.OUTBOX)!!
        assertFalse("the oldest should have been dropped", persisted.contains(firstUid))
        assertTrue("the newest should be kept", persisted.contains(lastUid))
    }

    @Test
    fun `the outbox survives a restart`() {
        val uid = IamEvent.newUid()
        outbox().record(event(uid = uid))

        // A fresh outbox over the same store, as if the process had died.
        val afterRestart = outbox()
        afterRestart.start()
        assertEquals(1, afterRestart.queuedCount())

        server.enqueue(MockResponse().setResponseCode(202).setBody("""{"accepted":1}"""))
        runBlocking { afterRestart.flush() }
        assertTrue(server.takeRequest(5, TimeUnit.SECONDS)!!.body.readUtf8().contains(uid))
    }

    /**
     * L3: on an Arabic-locale device a default-locale formatter emits Arabic-Indic digits,
     * which is not valid ISO-8601 and 400s the entire batch.
     */
    @Test
    fun `occurredAt is ASCII ISO-8601 with the device locale set to Arabic`() {
        val originalLocale = Locale.getDefault()
        val originalZone = TimeZone.getDefault()
        try {
            Locale.setDefault(Locale("ar", "EG"))
            TimeZone.setDefault(TimeZone.getTimeZone("Africa/Cairo"))
            val box = outbox()
            server.enqueue(MockResponse().setResponseCode(202).setBody("""{"accepted":1}"""))
            box.record(event())
            runBlocking { box.flush() }
            val body = server.takeRequest(5, TimeUnit.SECONDS)!!.body.readUtf8()
            val occurredAt = Regex("\"occurredAt\":\"([^\"]+)\"").find(body)!!.groupValues[1]
            assertTrue("expected ASCII digits, got $occurredAt", occurredAt.all { it.code < 128 })
            assertTrue(occurredAt.matches(Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z""")))
        } finally {
            Locale.setDefault(originalLocale)
            TimeZone.setDefault(originalZone)
        }
    }

    /** The batch body names one customerId at its root, so events must not be mixed. */
    @Test
    fun `events are grouped by customerId into separate requests`() {
        val box = outbox()
        box.record(event(customerId = "alice"))
        box.record(event(customerId = "bob"))
        server.enqueue(MockResponse().setResponseCode(202).setBody("""{"accepted":1}"""))
        server.enqueue(MockResponse().setResponseCode(202).setBody("""{"accepted":1}"""))
        runBlocking { box.flush() }

        val bodies = listOf(server.takeRequest(5, TimeUnit.SECONDS)!!.body.readUtf8(), server.takeRequest(5, TimeUnit.SECONDS)!!.body.readUtf8())
        assertTrue(bodies.any { it.contains("\"customerId\":\"alice\"") })
        assertTrue(bodies.any { it.contains("\"customerId\":\"bob\"") })
        bodies.forEach {
            assertFalse("a batch must name exactly one customer", 
                it.contains("alice") && it.contains("bob"))
        }
    }

    @Test
    fun `a button tap reports a click carrying buttonId and a surface tap does not`() {
        val box = outbox()
        server.enqueue(MockResponse().setResponseCode(202).setBody("""{"accepted":2}"""))
        box.record(event(type = IamEventType.CLICK, buttonId = "ok"))
        runBlocking { box.flush() }
        val withButton = server.takeRequest(5, TimeUnit.SECONDS)!!.body.readUtf8()
        assertTrue(withButton.contains("\"buttonId\":\"ok\""))
        assertTrue(withButton.contains("\"type\":\"click\""))

        server.enqueue(MockResponse().setResponseCode(202).setBody("""{"accepted":1}"""))
        box.record(event(type = IamEventType.CLICK))
        runBlocking { box.flush() }
        assertFalse(server.takeRequest(5, TimeUnit.SECONDS)!!.body.readUtf8().contains("buttonId"))
    }

    @Test
    fun `stop then start re-arms the scheduler`() {
        val box = outbox()
        box.start()
        box.dispose()
        box.start()
        // If dispose's flag were sticky, nothing would be scheduled and this event would only
        // ever go out on the count threshold.
        server.enqueue(MockResponse().setResponseCode(202).setBody("""{"accepted":1}"""))
        box.record(event())
        runBlocking { box.flush() }
        assertEquals(0, box.queuedCount())
    }
}
