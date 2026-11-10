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
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@RunWith(RobolectricTestRunner::class)
class RemoteVariableSourceTest {

    private lateinit var server: MockWebServer
    private lateinit var store: IamStore
    private lateinit var api: IamApi
    private var nowMillis = 1_800_000_000_000L
    private val clock = Clock { nowMillis }

    /** The nine keys the endpoint actually returns; four of them are personal data. */
    private val nineKeys = """
        { "variables": {
            "player_name": "Ahmed", "first_name": "Ahmed",
            "player_last_name": "El Assy", "player_display_name": "Ahmed El Assy",
            "player_unique_id": "10708564181292", "player_email": "ahmed@example.com",
            "points_balance": "1,250", "available_points": "1,250", "pending_points": "100"
        } }
    """.trimIndent()

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

    private fun source() = RemoteVariableSource(
        api, store, CoroutineScope(Dispatchers.Unconfined), clock
    )

    private fun values(src: RemoteVariableSource, needed: Set<String> = setOf("points_balance")) =
        runBlocking { src.values("alice", needed) }

    @Test
    fun `a successful fetch returns the values`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(nineKeys))
        val result = values(source())
        assertEquals("Ahmed", result["player_name"])
        assertEquals("1,250", result["points_balance"])
    }

    @Test
    fun `values are cached for sixty seconds`() {
        val src = source()
        server.enqueue(MockResponse().setResponseCode(200).setBody(nineKeys))
        values(src)
        assertEquals(1, server.requestCount)

        nowMillis += 59_000L
        values(src)
        assertEquals("still within the TTL", 1, server.requestCount)

        nowMillis += 2_000L
        server.enqueue(MockResponse().setResponseCode(200).setBody(nineKeys))
        values(src)
        assertEquals("past the TTL", 2, server.requestCount)
    }

    /**
     * invalidate drops freshness but must keep the persisted fallback: clearing it is what
     * made the fallback unreachable in Flutter and put a raw token on screen.
     */
    @Test
    fun `invalidate drops the cache but the persisted fallback survives`() {
        val src = source()
        server.enqueue(MockResponse().setResponseCode(200).setBody(nineKeys))
        values(src)

        src.invalidate()
        assertEquals("1,250", src.readPersisted("alice")["points_balance"])

        // With the endpoint now failing, the persisted copy is what renders.
        server.enqueue(MockResponse().setResponseCode(503))
        assertEquals("1,250", values(src)["points_balance"])
    }

    @Test
    fun `404, 422, 503 and a socket error all fall back rather than throwing`() {
        listOf(404, 422, 503).forEach { code ->
            SharedPreferencesUtils.getInstance().clearData()
            val src = source()
            server.enqueue(MockResponse().setResponseCode(code))
            assertTrue("HTTP $code should yield an empty map, not throw", values(src).isEmpty())
        }
        val src = source()
        server.shutdown()
        assertTrue(values(src).isEmpty())
    }

    @Test
    fun `an empty result falls back to what is already held`() {
        val src = source()
        server.enqueue(MockResponse().setResponseCode(200).setBody(nineKeys))
        values(src)
        src.invalidate()

        server.enqueue(MockResponse().setResponseCode(200).setBody("""{ "variables": {} }"""))
        assertEquals("1,250", values(src)["points_balance"])
    }

    /**
     * Only the tokens the held campaigns mention land on disk. The endpoint returns all nine
     * including four pieces of PII whether or not any campaign mentions them.
     */
    @Test
    fun `only the tokens the campaigns use are persisted`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(nineKeys))
        values(source(), needed = setOf("points_balance"))

        val persisted = store.readRaw(IamStore.Slot.VARIABLES)!!
        assertTrue(persisted.contains("points_balance"))
        assertFalse("PII must not be stored unless a campaign uses it",
            persisted.contains("player_email"))
        assertFalse(persisted.contains("player_last_name"))
        assertFalse(persisted.contains("player_display_name"))
    }

    @Test
    fun `a campaign set mentioning no tokens stores nothing at all`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(nineKeys))
        values(source(), needed = emptySet())
        assertEquals(null, store.readRaw(IamStore.Slot.VARIABLES))
    }

    @Test
    fun `clear removes the persisted values too`() {
        val src = source()
        server.enqueue(MockResponse().setResponseCode(200).setBody(nineKeys))
        values(src)
        assertTrue(src.readPersisted("alice").isNotEmpty())

        src.clear()
        assertEquals(null, store.readRaw(IamStore.Slot.VARIABLES))
        assertTrue(src.readPersisted("alice").isEmpty())
    }

    @Test
    fun `stored values are scoped to their customer`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(nineKeys))
        values(source())
        assertTrue(source().readPersisted("bob").isEmpty())
    }

    @Test
    fun `the request names the customer in the body`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(nineKeys))
        values(source())
        val request = server.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS)!!
        assertEquals("/api/v4.0/integrations/inapp-messages/variables", request.path)
        assertTrue(request.body.readUtf8().contains("\"customerId\":\"alice\""))
    }
}
