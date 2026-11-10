package com.gameball.gameball.inappmessaging.data

import androidx.test.core.app.ApplicationProvider
import com.gameball.gameball.local.SharedPreferencesUtils
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PersistenceTest {

    private lateinit var prefs: SharedPreferencesUtils

    @Before
    fun setUp() {
        SharedPreferencesUtils.init(ApplicationProvider.getApplicationContext(), Gson())
        prefs = SharedPreferencesUtils.getInstance()
        prefs.clearData()
    }

    private fun store() = IamStore(prefs)

    private val payloadWithQuietHours = """
        {
          "cooldownSeconds": 10,
          "quietHours": { "enabled": true, "start": "22:00", "end": "08:00" },
          "messages": [ { "campaignId": 1, "messageType": 2,
            "trigger": { "type": "session_start" },
            "content": {}, "locale": { "header": "Hi" } } ]
        }
    """.trimIndent()

    /** A once-ever campaign must stay suppressed across a restart. */
    @Test
    fun `a display record survives a simulated restart`() {
        DisplayHistory(store()).recordImpression("alice", 2055, 1_000L)

        // A fresh store and history, as if the process had died and come back.
        val afterRestart = DisplayHistory(store()).load("alice")
        assertEquals(1, afterRestart.perCampaign.size)
        assertEquals(1_000L, afterRestart.perCampaign[2055]!!.lastDisplayAtMillis)
        assertEquals(1, afterRestart.perCampaign[2055]!!.count)
        assertEquals(1_000L, afterRestart.lastDisplayAtMillis)
    }

    @Test
    fun `repeat impressions increment the count and move the timestamp`() {
        val history = DisplayHistory(store())
        history.recordImpression("alice", 2055, 1_000L)
        history.recordImpression("alice", 2055, 5_000L)
        val record = history.load("alice").perCampaign[2055]!!
        assertEquals(2, record.count)
        assertEquals(5_000L, record.lastDisplayAtMillis)
    }

    @Test
    fun `lastDisplayAtMillis tracks the most recent display from any campaign`() {
        val history = DisplayHistory(store())
        history.recordImpression("alice", 1, 1_000L)
        history.recordImpression("alice", 2, 9_000L)
        assertEquals(9_000L, history.load("alice").lastDisplayAtMillis)
    }

    @Test
    fun `history is scoped per customer`() {
        DisplayHistory(store()).recordImpression("alice", 2055, 1_000L)
        assertTrue(DisplayHistory(store()).load("bob").perCampaign.isEmpty())
    }

    /** Reading another customer's blob discards it, so it cannot resurface later. */
    @Test
    fun `reading another customer's blob clears it`() {
        DisplayHistory(store()).recordImpression("alice", 2055, 1_000L)
        assertTrue(DisplayHistory(store()).load("bob").perCampaign.isEmpty())
        assertTrue(
            "alice's history should have been discarded on the mismatched read",
            DisplayHistory(store()).load("alice").perCampaign.isEmpty()
        )
    }

    /**
     * The regression test for the Flutter cache bug: rebuilding the parsed result field by
     * field silently dropped the quiet-hours window, and going offline then became a way to
     * message somebody at 3am.
     */
    @Test
    fun `the campaign cache stores the raw payload and re-parses it on read`() {
        CampaignCache(store()).put("alice", payloadWithQuietHours)

        val restored = CampaignCache(store()).get("alice")
        assertNotNull(restored)
        assertEquals(1, restored!!.campaigns.size)
        assertEquals(10, restored.cooldownSeconds)
        assertNotNull("the quiet-hours window must survive the cache", restored.quietHours)
        assertEquals(22 * 60, restored.quietHours!!.startMinute)
        assertEquals(8 * 60, restored.quietHours!!.endMinute)
    }

    @Test
    fun `the campaign cache is scoped per customer`() {
        CampaignCache(store()).put("alice", payloadWithQuietHours)
        assertNull(CampaignCache(store()).get("bob"))
    }

    @Test
    fun `an empty cache reads as null rather than an empty result`() {
        assertNull(CampaignCache(store()).get("alice"))
    }

    @Test
    fun `a corrupt blob is discarded rather than throwing`() {
        store().writeRaw(IamStore.Slot.DISPLAY_HISTORY, "{ not json")
        assertTrue(DisplayHistory(store()).load("alice").perCampaign.isEmpty())

        store().writeRaw(IamStore.Slot.CAMPAIGN_CACHE, "{ not json")
        assertNull(CampaignCache(store()).get("alice"))
    }

    @Test
    fun `a scoped blob with no customerId is discarded`() {
        store().writeRaw(IamStore.Slot.DISPLAY_HISTORY, """{ "data": { "last": 1 } }""")
        assertTrue(DisplayHistory(store()).load("alice").perCampaign.isEmpty())
    }

    /** The outbox is not customer-scoped; each entry carries its own customerId. */
    @Test
    fun `the outbox slot round-trips without scoping`() {
        store().writeRaw(IamStore.Slot.OUTBOX, """[{"eventUid":"abc"}]""")
        assertEquals("""[{"eventUid":"abc"}]""", store().readRaw(IamStore.Slot.OUTBOX))
    }

    @Test
    fun `clearAll removes every slot`() {
        val s = store()
        s.writeScoped(IamStore.Slot.CAMPAIGN_CACHE, "alice", payloadWithQuietHours)
        s.writeScoped(IamStore.Slot.DISPLAY_HISTORY, "alice", """{"last":1}""")
        s.writeRaw(IamStore.Slot.OUTBOX, "[]")
        s.writeScoped(IamStore.Slot.VARIABLES, "alice", """{"a":"b"}""")

        s.clearAll()

        IamStore.Slot.values().forEach { assertNull(it.name, s.readRaw(it)) }
    }
}
