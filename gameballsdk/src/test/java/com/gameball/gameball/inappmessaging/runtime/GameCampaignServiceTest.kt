package com.gameball.gameball.inappmessaging.runtime

import androidx.test.core.app.ApplicationProvider
import com.gameball.gameball.inappmessaging.artwork.ArtworkPrefetcher
import com.gameball.gameball.inappmessaging.data.CampaignCache
import com.gameball.gameball.inappmessaging.data.DisplayHistory
import com.gameball.gameball.inappmessaging.data.IamEvent
import com.gameball.gameball.inappmessaging.data.IamStore
import com.gameball.gameball.inappmessaging.data.IamTime
import com.gameball.gameball.inappmessaging.data.MessageAnalytics
import com.gameball.gameball.inappmessaging.data.MessageParser
import com.gameball.gameball.inappmessaging.data.MessageSource
import com.gameball.gameball.inappmessaging.data.SyncOutcome
import com.gameball.gameball.inappmessaging.data.VariableSource
import com.gameball.gameball.inappmessaging.domain.Campaign
import com.gameball.gameball.inappmessaging.domain.CampaignKey
import com.gameball.gameball.inappmessaging.domain.MessageAction
import com.gameball.gameball.inappmessaging.domain.MessageButton
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
import org.robolectric.shadows.ShadowLog

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

    /** Holds what the service schedules until the test runs it. */
    private class ManualScheduler {
        val due = mutableListOf<() -> Unit>()

        fun schedule(delayMillis: Long, block: () -> Unit): () -> Unit {
            due.add(block)
            return { due.remove(block) }
        }

        fun runAll() {
            val now = due.toList()
            due.clear()
            now.forEach { it() }
        }
    }

    private lateinit var source: FakeSource
    private lateinit var analytics: RecordingAnalytics
    private lateinit var presenter: FakePresenter
    private lateinit var store: IamStore
    private lateinit var cache: CampaignCache
    private lateinit var history: DisplayHistory
    private val clock = MutableClock()
    private val scheduler = ManualScheduler()

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
        trigger: String = """{ "type": "event", "name": "buy", "repeatable": true }""",
        priority: Int = 0
    ) = """
        { "campaignType": 1, "campaignId": $id, "messageType": 2, "priority": $priority,
          "trigger": $trigger, "content": {}, "locale": { "header": "Hello" } }
    """.trimIndent()

    private val spin = """{ "type": "event", "name": "spin", "repeatable": true }"""

    private fun sync(vararg entries: String) = syncWithCooldown(0, *entries)

    private fun syncWithCooldown(seconds: Int, vararg entries: String) =
        """{ "cooldownSeconds": $seconds, "messages": [ ${entries.joinToString(",")} ] }"""

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
        hooks = HostHooks(),
        schedule = scheduler::schedule
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

    // --- after a play ---

    /** Away for longer than the session timeout, so coming back syncs again. */
    private fun nextSession(svc: InAppMessagingService) {
        svc.onAppBackgrounded()
        clock.now += 31_000L
        svc.onAppForegrounded()
    }

    @Test
    fun `a played game is not offered again until the next successful sync`() {
        source.outcome = success(sync(game()))
        val svc = service()
        svc.start("alice")
        svc.onEvent("spin", emptyMap())
        presenter.callbacks!!.onShown()
        presenter.callbacks!!.onGameCompleted()
        presenter.close()

        svc.onEvent("spin", emptyMap())
        assertEquals("repeatable, but just played", 1, presenter.presentCount)

        nextSession(svc)
        svc.onEvent("spin", emptyMap())
        assertEquals("the next sync decides whether it may be played again", 2, presenter.presentCount)
    }

    @Test
    fun `a game closed without a play is offered again`() {
        source.outcome = success(sync(game()))
        val svc = service()
        svc.start("alice")
        svc.onEvent("spin", emptyMap())
        presenter.callbacks!!.onShown()
        presenter.close()

        svc.onEvent("spin", emptyMap())
        assertEquals(2, presenter.presentCount)
    }

    @Test
    fun `a play holds back only the game that was played`() {
        source.outcome = success(sync(game(id = 9645), game(id = 9702, trigger = """{ "type": "event", "name": "quiz", "repeatable": true }""")))
        val svc = service()
        svc.start("alice")
        svc.onEvent("spin", emptyMap())
        presenter.callbacks!!.onShown()
        presenter.callbacks!!.onGameCompleted()
        presenter.close()

        svc.onEvent("quiz", emptyMap())
        assertEquals(9702, presenter.presented!!.widget!!.gameCampaignId)
    }

    // --- a waiting game ---

    @Test
    fun `a waiting game the latest sync no longer offers is dropped`() {
        source.outcome = success(sync(game()))
        presenter.canPresent = false
        val svc = service()
        svc.start("alice")
        svc.onEvent("spin", emptyMap())

        // The campaign ended while the game waited; the new session's sync no longer has it.
        source.outcome = success(sync())
        nextSession(svc)
        presenter.canPresent = true
        svc.onSurfaceAvailable()

        assertEquals(0, presenter.presentCount)
    }

    @Test
    fun `a waiting game is held to the expiry the latest sync gives it`() {
        source.outcome = success(sync(game()))
        presenter.canPresent = false
        val svc = service()
        svc.start("alice")
        svc.onEvent("spin", emptyMap())

        // Still offered, but with an end date that has already passed.
        source.outcome = success(sync(game(expiresAt = IamTime.toIso8601Utc(clock.now - 60_000L))))
        nextSession(svc)
        presenter.canPresent = true
        svc.onSurfaceAvailable()

        assertEquals(0, presenter.presentCount)
    }

    @Test
    fun `a waiting game past its expiresAt is dropped`() {
        source.outcome = success(sync(game(expiresAt = IamTime.toIso8601Utc(clock.now + 10_000L))))
        presenter.canPresent = false
        val svc = service()
        svc.start("alice")
        svc.onEvent("spin", emptyMap())

        clock.now += 20_000L
        presenter.canPresent = true
        svc.onSurfaceAvailable()

        assertEquals(0, presenter.presentCount)
    }

    @Test
    fun `a waiting game still offered by the latest sync shows`() {
        source.outcome = success(sync(game()))
        presenter.canPresent = false
        val svc = service()
        svc.start("alice")
        svc.onEvent("spin", emptyMap())

        presenter.canPresent = true
        svc.onSurfaceAvailable()

        assertEquals(9645, presenter.presented!!.widget!!.gameCampaignId)
    }

    /** Every game's campaignId is 0, so comparing campaignIds made every game the same one. */
    @Test
    fun `a game taking another's place in the waiting slot is logged by kind and id`() {
        IamLog.enabled = true
        try {
            source.outcome = success(
                sync(game(id = 9645), game(id = 9702, trigger = """{ "type": "event", "name": "quiz", "repeatable": true }"""))
            )
            presenter.canPresent = false
            val svc = service()
            svc.start("alice")
            svc.onEvent("spin", emptyMap())
            svc.onEvent("quiz", emptyMap())

            val lines = ShadowLog.getLogsForTag("GameballIAM").map { it.msg }
            assertTrue(lines.joinToString("\n"), lines.any { "game:9702 displaces campaign game:9645" in it })
        } finally {
            IamLog.enabled = false
        }
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

    // --- two campaigns on one trigger (tracker F1, F2) ---

    /** F2: only the winner was kept, so the game that lost the tie was never offered again. */
    @Test
    fun `a second game on the same trigger opens once the first is closed`() {
        source.outcome = success(sync(game(id = 9645), game(id = 9702)))
        val svc = service()
        svc.start("alice")
        svc.onEvent("spin", emptyMap())
        assertEquals(9645, presenter.presented!!.widget!!.gameCampaignId)

        presenter.callbacks!!.onShown()
        presenter.close()

        assertEquals("the runner-up waited behind it", 9702, presenter.presented!!.widget!!.gameCampaignId)
        assertEquals(2, presenter.presentCount)
    }

    /** F1: the message wins on priority, and the game behind it must not be lost. */
    @Test
    fun `a game on the same trigger as a higher-priority message opens once the message is closed`() {
        source.outcome = success(sync(message(id = 2215, trigger = spin, priority = 100), game(id = 9645)))
        val svc = service()
        svc.start("alice")
        svc.onEvent("spin", emptyMap())
        assertEquals(2215, presenter.presented!!.campaignId)

        presenter.callbacks!!.onShown()
        presenter.close()

        assertEquals(9645, presenter.presented?.widget?.gameCampaignId)
    }

    /** F1 on device closed the message with its OK button: a tap must not throw the game away. */
    @Test
    fun `the game behind a message still opens when the message is closed with its button`() {
        source.outcome = success(sync(message(id = 2215, trigger = spin, priority = 100), game(id = 9645)))
        val svc = service()
        svc.start("alice")
        svc.onEvent("spin", emptyMap())
        presenter.callbacks!!.onShown()

        presenter.callbacks!!.onTapped(MessageButton("ok", "OK", MessageAction.Dismiss))

        assertEquals(9645, presenter.presented?.widget?.gameCampaignId)
    }

    /** The floor counts from the winner's display, so the runner-up waits it out instead of being dropped. */
    @Test
    fun `the game behind another waits out the cooldown, then opens`() {
        source.outcome = success(syncWithCooldown(10, game(id = 9645), game(id = 9702)))
        val svc = service()
        svc.start("alice")
        svc.onEvent("spin", emptyMap())
        presenter.callbacks!!.onShown()

        clock.now += 3_000L
        presenter.close()
        assertEquals("inside the 10 s floor the first game started", 1, presenter.presentCount)

        clock.now += 7_000L
        scheduler.runAll()

        assertEquals(9702, presenter.presented!!.widget!!.gameCampaignId)
    }

    /** Deferred because the screen was busy, it was thrown away when the game on screen closed. */
    @Test
    fun `a campaign deferred behind a shown game keeps its place when the game is closed`() {
        source.outcome = success(sync(game(), message()))
        val svc = service()
        svc.start("alice")
        svc.onEvent("spin", emptyMap())
        svc.onEvent("buy", emptyMap())
        presenter.callbacks!!.onShown()

        presenter.close()

        assertEquals(2215, presenter.presented!!.campaignId)
    }

    /** Only a winner that took the screen leaves a runner-up behind; one that could not draw keeps its place. */
    @Test
    fun `a winner that could not draw keeps the waiting slot for itself`() {
        source.outcome = success(sync(game(id = 9645), game(id = 9702)))
        presenter.canPresent = false
        val svc = service()
        svc.start("alice")
        svc.onEvent("spin", emptyMap())

        presenter.canPresent = true
        svc.onSurfaceAvailable()

        assertEquals(9645, presenter.presented!!.widget!!.gameCampaignId)
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
