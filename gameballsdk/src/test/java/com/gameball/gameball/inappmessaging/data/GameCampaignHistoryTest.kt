package com.gameball.gameball.inappmessaging.data

import androidx.test.core.app.ApplicationProvider
import com.gameball.gameball.inappmessaging.domain.CampaignKey
import com.gameball.gameball.inappmessaging.domain.CampaignKind
import com.gameball.gameball.local.SharedPreferencesUtils
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GameCampaignHistoryTest {

    private lateinit var prefs: SharedPreferencesUtils

    @Before
    fun setUp() {
        SharedPreferencesUtils.init(ApplicationProvider.getApplicationContext(), Gson())
        prefs = SharedPreferencesUtils.getInstance()
        prefs.clearData()
    }

    private fun store() = IamStore(prefs)

    @Test
    fun `a game and a message sharing a number do not share cap state`() {
        val history = DisplayHistory(store())
        history.recordImpression("alice", CampaignKey.message(2215), 1_000L)

        val loaded = history.load("alice")
        assertEquals(1_000L, loaded.perCampaign[CampaignKey.message(2215)]!!.lastDisplayAtMillis)
        assertNull(loaded.perCampaign[CampaignKey.game(2215)])
    }

    @Test
    fun `a history written by the current release still decodes`() {
        // The migration that matters most. Every history written before this change keys on a
        // bare number, and all of those are message campaigns — games did not exist. Without
        // this, an update loses the history and every once-ever campaign shows again.
        //
        // The fixture is the exact shape DisplayHistory wrote in 3.4.0, not a hand-wave.
        store().writeScoped(
            IamStore.Slot.DISPLAY_HISTORY,
            "alice",
            """{"last":1000,"records":{"2215":{"at":1000,"count":1}}}"""
        )

        val loaded = DisplayHistory(store()).load("alice")
        assertEquals(1, loaded.perCampaign.size)
        assertEquals(1_000L, loaded.perCampaign[CampaignKey.message(2215)]!!.lastDisplayAtMillis)
        assertEquals(1_000L, loaded.lastDisplayAtMillis)
    }

    @Test
    fun `both kinds round-trip through storage`() {
        val history = DisplayHistory(store())
        history.recordImpression("alice", CampaignKey.message(2215), 1_000L)
        history.recordImpression("alice", CampaignKey.game(9645), 2_000L)

        val loaded = DisplayHistory(store()).load("alice")
        assertEquals(2, loaded.perCampaign.size)
        assertEquals(1_000L, loaded.perCampaign[CampaignKey.message(2215)]!!.lastDisplayAtMillis)
        assertEquals(2_000L, loaded.perCampaign[CampaignKey.game(9645)]!!.lastDisplayAtMillis)
    }

    @Test
    fun `a key that names no known kind is skipped rather than guessed at`() {
        store().writeScoped(
            IamStore.Slot.DISPLAY_HISTORY,
            "alice",
            """{"last":1000,"records":{"challenge:1":{"at":1000,"count":1},"2215":{"at":900,"count":1}}}"""
        )

        val loaded = DisplayHistory(store()).load("alice")
        assertEquals(1, loaded.perCampaign.size)
        assertEquals(900L, loaded.perCampaign[CampaignKey.message(2215)]!!.lastDisplayAtMillis)
    }
}

class CampaignKeyTest {

    @Test
    fun `the storage form names the kind`() {
        assertEquals("message:2215", CampaignKey.message(2215).storageKey)
        assertEquals("game:9645", CampaignKey.game(9645).storageKey)
    }

    @Test
    fun `a game and a message sharing a number are different keys`() {
        assertNotEquals(CampaignKey.message(2215), CampaignKey.game(2215))
    }

    @Test
    fun `a bare number reads as a message campaign`() {
        assertEquals(CampaignKey.message(2215), CampaignKey.parse("2215"))
    }

    @Test
    fun `both storage forms read back`() {
        assertEquals(CampaignKey.message(1), CampaignKey.parse("message:1"))
        assertEquals(CampaignKey.game(9645), CampaignKey.parse("game:9645"))
    }

    @Test
    fun `anything else is rejected rather than guessed at`() {
        assertNull(CampaignKey.parse("challenge:1"))
        assertNull(CampaignKey.parse("game:abc"))
        assertNull(CampaignKey.parse(""))
        assertNull(CampaignKey.parse(":1"))
        assertTrue(CampaignKind.values().size == 2)
    }
}
