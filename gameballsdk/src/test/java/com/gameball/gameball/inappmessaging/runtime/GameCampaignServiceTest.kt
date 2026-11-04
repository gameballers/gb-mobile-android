package com.gameball.gameball.inappmessaging.runtime

import androidx.test.core.app.ApplicationProvider
import com.gameball.gameball.inappmessaging.artwork.ArtworkPrefetcher
import com.gameball.gameball.inappmessaging.data.CampaignCache
import com.gameball.gameball.inappmessaging.data.DisplayHistory
import com.gameball.gameball.inappmessaging.data.IamEvent
import com.gameball.gameball.inappmessaging.data.IamStore
import com.gameball.gameball.inappmessaging.data.MessageAnalytics
import com.gameball.gameball.inappmessaging.data.MessageParser
import com.gameball.gameball.inappmessaging.data.MessageSource
import com.gameball.gameball.inappmessaging.data.SyncOutcome
import com.gameball.gameball.inappmessaging.data.VariableSource
import com.gameball.gameball.inappmessaging.domain.Campaign
import com.gameball.gameball.inappmessaging.domain.CampaignKey
import com.gameball.gameball.inappmessaging.domain.SyncResult
import com.gameball.gameball.local.SharedPreferencesUtils
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GameCampaignServiceTest {

    private class FakeSource : MessageSource {
        var outcome: SyncOutcome = SyncOutcome.Success("{}", SyncResult.EMPTY)
        override suspend fun fetch(customerId: String): SyncOutcome = outcome
    }

    private class RecordingAnalytics : MessageAnalytics {
        val events = mutableListOf<IamEvent>()
        override fun start() = Unit
        override fun record(event: IamEvent) { events.add(event) }
        override suspend fun flush() = Unit
        override fun dispose() = Unit
    }

    private class FakePrefetcher : ArtworkPrefetcher {
        override suspend fun warm(urls: Set<String>) = Unit
        override fun isReady(url: String?) = true
        override fun retryFailedIfDue(nowMillis: Long) = Unit
        override fun reset() = Unit
    }

    private class FakeVariables : VariableSource {
        override suspend fun values(customerId: String, needed: Set<String>) = emptyMap<String, String>()
        override fun invalidate() = Unit
        override fun clear() = Unit
    }

    private class FakePresenter : MessagePresenter {
        var canPresent = true
        var presented: Campaign? = null
        var presentCount = 0
        var callbacks: PresentationCallbacks? = null
        override var isShowing = false
        override fun present(
            campaign: Campaign, resolved: ResolvedMessage, callbacks: PresentationCallbacks
        ): Boolean {
            if (!canPresent) return false
            presented = campaign
            presentCount++
            this.callbacks = callbacks
            isShowing = true
            return true
        }
        override fun dismissCurrent() { isShowing = false }

        /** What the real presenter does when a game never becomes ready: off the screen, unseen. */
        fun fail() {
            isShowing = false
            callbacks?.onFailed()
        }

        /** What the real presenter does on close, a backdrop tap or back. */
        fun close() {
            isShowing = false
            callbacks?.onDismissed()
        }
    }

    private class MutableClock(var now: Long = 1_800_000_000_000L) : Clock {
        override fun nowMillis() = now
    }

    private lateinit var source: FakeSource
    private lateinit var analytics: RecordingAnalytics
    private lateinit var presenter: FakePresenter
    private lateinit var store: IamStore
    private lateinit var cache: CampaignCache
    private lateinit var history: DisplayHistory
    private val clock = MutableClock()

    /** A game entry and, optionally, an ordinary message beside it. */
    private fun payload(withMessage: Boolean = false, gamePriority: Int = 0) = """
        { "cooldownSeconds": 0, "messages": [
          { "campaignType": 2, "campaignId": 0, "messageType": 2, "priority": $gamePriority,
            "trigger": { "type": "session_start", "repeatable": true },
            "content": null, "locale": null,
            "gameCampaignId": 9645, "gameCampaignType": "wheel" }
          ${if (withMessage) ""","{}"""".replace("\"{}\"", """
          { "campaignId": 2215, "messageType": 2, "priority": 0,
            "trigger": { "type": "session_start", "repeatable": true },
            "content": {}, "locale": { "header": "Hello" } }""") else ""}
        ] }
    """.trimIndent()

    private fun success(raw: String) = SyncOutcome.Success(raw, MessageParser.parse(raw))

    /** One game entry, shaped as the sync sends it. */
    private fun game(
        id: Int = 9645,
        trigger: String = """{ "type": "event", "name": "spin", "repeatable": true }""",
        expiresAt: String? = null
    ) = """
        { "campaignType": 2, "campaignId": 0, "messageType": 2, "priority": 0,
          "trigger": $trigger, "content": null, "locale": null,
          "expiresAt": ${expiresAt?.let { "\"$it\"" } ?: "null"},
          "gameCampaignId": $id, "gameCampaignType": "wheel" }
    """.trimIndent()

    /** One ordinary message entry. */
    private fun message(
        id: Int = 2215,
        trigger: String = """{ "type": "event", "name": "buy", "repeatable": true }"""
    ) = """
        { "campaignType": 1, "campaignId": $id, "messageType": 2, "priority": 0,
          "trigger": $trigger, "content": {}, "locale": { "header": "Hello" } }
    """.trimIndent()

    private fun sync(vararg entries: String) =
        """{ "cooldownSeconds": 0, "messages": [ ${entries.joinToString(",")} ] }"""

    @Before
    fun setUp() {
        SharedPreferencesUtils.init(ApplicationProvider.getApplicationContext(), Gson())
        SharedPreferencesUtils.getInstance().clearData()
        store = IamStore(SharedPreferencesUtils.getInstance())
        cache = CampaignCache(store)
        history = DisplayHistory(store)
        source = FakeSource()
        analytics = RecordingAnalytics()
        presenter = FakePresenter()
    }

    private fun service() = InAppMessagingService(
        scope = CoroutineScope(Dispatchers.Unconfined),
        clock = clock,
        source = source,
        cache = cache,
        history = history,
        artwork = FakePrefetcher(),
        analytics = analytics,
        variables = FakeVariables(),
        presenter = presenter,
        sessionState = SessionState(clock),
        hooks = HostHooks()
    )

    @Test
    fun `a game campaign is selected and presented like any other`() {
        source.outcome = success(payload())
        service().start("alice")

        val presented = presenter.presented
        assertNotNull(presented)
        assertTrue(presented!!.isWidget)
        assertEquals(9645, presented.widget!!.gameCampaignId)
    }

    @Test
    fun `a displayed game reports no message telemetry`() {
        // Its campaignId is always 0, so the default path would file every impression under
        // campaign zero. The game reports its own engagement from inside the widget.
        source.outcome = success(payload())
        service().start("alice")
        presenter.callbacks!!.onShown()

        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun `a displayed game still records its display history`() {
        // Suppressing telemetry must not suppress the cap, or a once-ever game reappears every
        // session. Recorded under the game's own key, never under campaignId 0.
        source.outcome = success(payload())
        service().start("alice")
        presenter.callbacks!!.onShown()

        val loaded = history.load("alice")
        assertEquals(1, loaded.perCampaign.size)
        assertNotNull(loaded.perCampaign[CampaignKey.game(9645)])
        assertNull(loaded.perCampaign[CampaignKey.message(0)])
    }

    @Test
    fun `a displayed message still reports telemetry`() {
        // The guard is for games only. Ordinary messages have shipped and must keep reporting.
        source.outcome = success(
            """{ "cooldownSeconds": 0, "messages": [
              { "campaignId": 2215, "messageType": 2,
                "trigger": { "type": "session_start" },
                "content": {}, "locale": { "header": "Hello" } } ] }"""
        )
        service().start("alice")
        presenter.callbacks!!.onShown()

        assertEquals(1, analytics.events.size)
        assertEquals(2215, analytics.events.first().campaignId)
    }

    @Test
    fun `a game is never opened from a cached set, while a cached message still shows`() {
        // A message carries its own content, so a cached one is still correct. A game carries a
        // pointer and fetches live: the campaign may have ended since it was cached, and the
        // customer would open it to an error.
        cache.put("alice", payload(withMessage = true))
        source.outcome = SyncOutcome.Failure("offline", permanent = false)
        service().start("alice")

        val presented = presenter.presented
        assertNotNull(presented)
        assertEquals(2215, presented!!.campaignId)
    }

    // --- a game that never loads ---

    @Test
    fun `a game that never loads is let go until its next trigger`() {
        source.outcome = success(sync(game()))
        val svc = service()
        svc.start("alice")
        svc.onEvent("spin", emptyMap())
        assertEquals(1, presenter.presentCount)

        presenter.fail()
        // What every Activity resume does. Kept waiting, the game loaded again on each one.
        svc.onSurfaceAvailable()
        assertEquals("a resume must not load it again", 1, presenter.presentCount)

        svc.onEvent("spin", emptyMap())
        assertEquals("its next trigger may offer it again", 2, presenter.presentCount)
    }

    @Test
    fun `a campaign waiting behind a game that never loads still shows`() {
        source.outcome = success(sync(game(), message()))
        val svc = service()
        svc.start("alice")
        svc.onEvent("spin", emptyMap())
        svc.onEvent("buy", emptyMap())
        assertTrue("the message waits while the game loads", presenter.presented!!.isWidget)

        presenter.fail()
        assertEquals(2215, presenter.presented!!.campaignId)
    }
}
