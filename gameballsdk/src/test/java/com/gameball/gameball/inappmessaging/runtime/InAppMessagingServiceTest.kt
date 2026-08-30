package com.gameball.gameball.inappmessaging.runtime

import androidx.test.core.app.ApplicationProvider
import com.gameball.gameball.inappmessaging.artwork.ArtworkPrefetcher
import com.gameball.gameball.inappmessaging.data.CampaignCache
import com.gameball.gameball.inappmessaging.data.DisplayHistory
import com.gameball.gameball.inappmessaging.data.IamEvent
import com.gameball.gameball.inappmessaging.data.IamEventType
import com.gameball.gameball.inappmessaging.data.IamStore
import com.gameball.gameball.inappmessaging.data.MessageAnalytics
import com.gameball.gameball.inappmessaging.data.MessageSource
import com.gameball.gameball.inappmessaging.data.SyncOutcome
import com.gameball.gameball.inappmessaging.data.VariableSource
import com.gameball.gameball.inappmessaging.domain.Campaign
import com.gameball.gameball.inappmessaging.domain.MessageButton
import com.gameball.gameball.inappmessaging.model.DisplayDecision
import com.gameball.gameball.local.SharedPreferencesUtils
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InAppMessagingServiceTest {

    // --- fakes ---

    private class FakeSource : MessageSource {
        var outcome: SyncOutcome = SyncOutcome.Success("{}", com.gameball.gameball.inappmessaging.domain.SyncResult.EMPTY)
        var fetchCount = 0
        override suspend fun fetch(customerId: String): SyncOutcome {
            fetchCount++
            return outcome
        }
    }

    private class RecordingAnalytics : MessageAnalytics {
        val events = mutableListOf<IamEvent>()
        var started = 0
        var disposed = 0
        var flushes = 0
        override fun start() { started++ }
        override fun record(event: IamEvent) { events.add(event) }
        override suspend fun flush() { flushes++ }
        override fun dispose() { disposed++ }
        fun typesFor(campaignId: Int) = events.filter { it.campaignId == campaignId }.map { it.type }
    }

    private class FakePrefetcher : ArtworkPrefetcher {
        var readyAlways = true
        val warmed = mutableListOf<String>()
        var retryCalls = 0
        override suspend fun warm(urls: Set<String>) { warmed.addAll(urls) }
        override fun isReady(url: String?) = readyAlways
        override fun retryFailedIfDue(nowMillis: Long) { retryCalls++ }
        override fun reset() { warmed.clear() }
    }

    private class FakeVariables : VariableSource {
        var values = mapOf("player_name" to "Ahmed")
        var invalidations = 0
        var clears = 0
        override suspend fun values(customerId: String, needed: Set<String>) = values
        override fun invalidate() { invalidations++ }
        override fun clear() { clears++ }
    }

    private class FakePresenter : MessagePresenter {
        var canPresent = true
        var presented: Campaign? = null
        var resolved: ResolvedMessage? = null
        var callbacks: PresentationCallbacks? = null
        var dismissCount = 0
        override var isShowing = false
        override fun present(
            campaign: Campaign, resolved: ResolvedMessage, callbacks: PresentationCallbacks
        ): Boolean {
            if (!canPresent) return false
            presented = campaign
            this.resolved = resolved
            this.callbacks = callbacks
            isShowing = true
            return true
        }
        override fun dismissCurrent() {
            if (isShowing) dismissCount++
            isShowing = false
        }
    }

    private class MutableClock(var now: Long = 1_800_000_000_000L) : Clock {
        override fun nowMillis() = now
    }

    // --- fixtures ---

    private lateinit var source: FakeSource
    private lateinit var analytics: RecordingAnalytics
    private lateinit var artwork: FakePrefetcher
    private lateinit var variables: FakeVariables
    private lateinit var presenter: FakePresenter
    private lateinit var store: IamStore
    private lateinit var cache: CampaignCache
    private lateinit var history: DisplayHistory
    private val clock = MutableClock()

    private fun payload(
        campaignId: Int = 1,
        priority: Int = 0,
        trigger: String = """{ "type": "session_start" }""",
        cooldown: Int = 0,
        header: String = "Hello",
        extra: String = ""
    ) = """
        { "cooldownSeconds": $cooldown, "messages": [ {
            "campaignId": $campaignId, "messageType": 2, "priority": $priority,
            "trigger": $trigger, "content": {}, "locale": { "header": "$header" } $extra
        } ] }
    """.trimIndent()

    private fun success(raw: String) = SyncOutcome.Success(
        raw, com.gameball.gameball.inappmessaging.data.MessageParser.parse(raw)
    )

    @Before
    fun setUp() {
        SharedPreferencesUtils.init(ApplicationProvider.getApplicationContext(), Gson())
        SharedPreferencesUtils.getInstance().clearData()
        store = IamStore(SharedPreferencesUtils.getInstance())
        cache = CampaignCache(store)
        history = DisplayHistory(store)
        source = FakeSource()
        analytics = RecordingAnalytics()
        artwork = FakePrefetcher()
        variables = FakeVariables()
        presenter = FakePresenter()
    }

    private fun service(hooks: HostHooks = HostHooks()) = InAppMessagingService(
        scope = CoroutineScope(Dispatchers.Unconfined),
        clock = clock,
        source = source,
        cache = cache,
        history = history,
        artwork = artwork,
        analytics = analytics,
        variables = variables,
        presenter = presenter,
        sessionState = SessionState(clock),
        hooks = hooks
    )

    // --- sync sequencing ---

    @Test
    fun `a successful sync evaluates session start and presents`() {
        source.outcome = success(payload())
        service().start("alice")
        assertEquals(1, presenter.presented?.campaignId)
    }

    @Test
    fun `an empty successful sync replaces the cache`() {
        cache.put("alice", payload(campaignId = 99))
        source.outcome = success("""{ "messages": [] }""")
        service().start("alice")
        assertNull("the empty sync must replace, not fall back", presenter.presented)
        assertTrue(cache.get("alice")!!.campaigns.isEmpty())
    }

    @Test
    fun `a failed sync falls back to the cache`() {
        cache.put("alice", payload(campaignId = 42))
        source.outcome = SyncOutcome.Failure("503", permanent = false)
        service().start("alice")
        assertEquals(42, presenter.presented?.campaignId)
    }

    @Test
    fun `a successful sync does not consult the cache`() {
        cache.put("alice", payload(campaignId = 99))
        source.outcome = success(payload(campaignId = 7))
        service().start("alice")
        assertEquals(7, presenter.presented?.campaignId)
    }

    @Test
    fun `artwork is warmed before session start is evaluated`() {
        source.outcome = success(
            """
            { "messages": [ { "campaignId": 1, "messageType": 2,
              "trigger": { "type": "session_start" },
              "content": { "imageUrl": "https://x/a.jpg" },
              "locale": { "header": "Hi" } } ] }
            """.trimIndent()
        )
        service().start("alice")
        assertTrue(artwork.warmed.contains("https://x/a.jpg"))
        assertEquals(1, presenter.presented?.campaignId)
    }

    @Test
    fun `start is idempotent for the same customer`() {
        source.outcome = success(payload())
        val svc = service()
        svc.start("alice")
        svc.start("alice")
        assertEquals(1, source.fetchCount)
    }

    @Test
    fun `a different customer refetches and discards the previous cache`() {
        source.outcome = success(payload())
        val svc = service()
        svc.start("alice")
        history.recordImpression("alice", 1, clock.now)

        svc.start("bob")
        assertEquals(2, source.fetchCount)
        assertNull("alice's cache must be gone", cache.get("alice"))
        assertTrue(variables.clears > 0)
    }

    // --- deferral vs suppression ---

    @Test
    fun `no surface defers rather than suppressing`() {
        presenter.canPresent = false
        source.outcome = success(payload())
        val svc = service()
        svc.start("alice")
        assertNull(presenter.presented)

        presenter.canPresent = true
        svc.onSurfaceAvailable()
        assertEquals("the deferred campaign must come back", 1, presenter.presented?.campaignId)
    }

    @Test
    fun `beforeDisplay later defers and discard spends the occurrence`() {
        source.outcome = success(payload())
        val deferring = service(HostHooks(beforeDisplay = { DisplayDecision.LATER }))
        deferring.start("alice")
        assertNull(presenter.presented)
        deferring.onSurfaceAvailable()
        assertEquals("later must be retried", 1, presenter.presented?.campaignId)

        setUp()
        source.outcome = success(payload())
        val discarding = service(HostHooks(beforeDisplay = { DisplayDecision.DISCARD }))
        discarding.start("alice")
        discarding.onSurfaceAvailable()
        assertNull("discard must not be retried", presenter.presented)
    }

    /** Defect 10: the retry must ask "may this show now", not "has it ever shown". */
    @Test
    fun `retry keeps a deferred repeatable campaign that has already displayed`() {
        val repeatable = payload(
            campaignId = 5,
            trigger = """{ "type": "event", "name": "x", "repeatable": true,
                           "minIntervalSeconds": 1 }"""
        )
        source.outcome = success(repeatable)
        presenter.canPresent = false
        val svc = service()
        svc.start("alice")
        history.recordImpression("alice", 5, clock.now - 10_000L)

        svc.onEvent("x", emptyMap())
        presenter.canPresent = true
        svc.onSurfaceAvailable()
        assertEquals(5, presenter.presented?.campaignId)
    }

    @Test
    fun `retry re-checks the floor`() {
        source.outcome = success(payload(cooldown = 30))
        presenter.canPresent = false
        val svc = service()
        svc.start("alice")

        // Something else displayed while ours was waiting.
        history.recordImpression("alice", 999, clock.now)
        presenter.canPresent = true
        svc.onSurfaceAvailable()
        assertNull("inside the floor it must keep waiting", presenter.presented)
    }

    // --- impression accounting ---

    @Test
    fun `the cap is recorded at impression, not at selection`() {
        presenter.canPresent = false
        source.outcome = success(payload())
        service().start("alice")
        assertTrue(
            "a deferred message must not burn its slot",
            history.load("alice").perCampaign.isEmpty()
        )
    }

    @Test
    fun `the impression is reported and recorded when the view paints`() {
        source.outcome = success(payload())
        service().start("alice")
        assertTrue(analytics.events.isEmpty())

        presenter.callbacks!!.onShown()
        assertEquals(listOf(IamEventType.IMPRESSION), analytics.typesFor(1))
        assertEquals(1, history.load("alice").perCampaign.size)
    }

    @Test
    fun `dismissal is reported only when shown and not engaged`() {
        source.outcome = success(payload())
        service().start("alice")
        presenter.callbacks!!.onShown()
        presenter.callbacks!!.onDismissed()
        assertEquals(
            listOf(IamEventType.IMPRESSION, IamEventType.DISMISS), analytics.typesFor(1)
        )
    }

    @Test
    fun `a message dismissed before it paints reports nothing at all`() {
        source.outcome = success(payload())
        service().start("alice")
        presenter.callbacks!!.onDismissed()
        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun `a tap suppresses the dismissal report`() {
        val withAction = payload(extra = "").replace(
            """"content": {}""", """"content": { "action": { "type": "dismiss" } }"""
        )
        source.outcome = success(withAction)
        service().start("alice")
        presenter.callbacks!!.onShown()
        presenter.callbacks!!.onTapped(null)
        presenter.callbacks!!.onDismissed()
        assertEquals(
            "the dismissal after a tap is not 'shown and ignored'",
            listOf(IamEventType.IMPRESSION, IamEventType.CLICK),
            analytics.typesFor(1)
        )
    }

    @Test
    fun `an isTest campaign displays and reports nothing`() {
        val isTest = """
            { "cooldownSeconds": 0, "messages": [ { "campaignId": 1, "messageType": 2,
              "isTest": true, "trigger": { "type": "session_start" },
              "content": {}, "locale": { "header": "Hi" } } ] }
        """.trimIndent()
        source.outcome = success(isTest)
        service().start("alice")
        presenter.callbacks!!.onShown()
        presenter.callbacks!!.onDismissed()
        assertEquals(1, presenter.presented?.campaignId)
        assertTrue("isTest telemetry must never reach statistics", analytics.events.isEmpty())
    }

    // --- hooks ---

    @Test
    fun `the click is reported even when onAction returns true`() {
        val withAction = """
            { "cooldownSeconds": 0, "messages": [ { "campaignId": 1, "messageType": 2,
              "trigger": { "type": "session_start" },
              "content": { "action": { "type": "dismiss" } },
              "locale": { "header": "Hi" } } ] }
        """.trimIndent()
        source.outcome = success(withAction)
        var handled = 0
        service(HostHooks(onAction = { _, _, _ -> handled++; true })).start("alice")
        presenter.callbacks!!.onShown()
        presenter.callbacks!!.onTapped(null)

        assertEquals(1, handled)
        assertTrue(
            "a host that intercepts a tap must not erase its own click analytics",
            analytics.typesFor(1).contains(IamEventType.CLICK)
        )
    }

    @Test
    fun `the action path dismisses before performing`() {
        val withAction = """
            { "cooldownSeconds": 0, "messages": [ { "campaignId": 1, "messageType": 2,
              "trigger": { "type": "session_start" },
              "content": { "action": { "type": "navigate", "route": "orders" } },
              "locale": { "header": "Hi" } } ] }
        """.trimIndent()
        source.outcome = success(withAction)
        val order = mutableListOf<String>()
        val svc = service(HostHooks(onNavigate = { _, _ -> order.add("navigate") }))
        svc.start("alice")
        presenter.callbacks!!.onShown()
        presenter.callbacks!!.onTapped(null)

        assertEquals(1, presenter.dismissCount)
        assertEquals(listOf("navigate"), order)
        assertFalse(presenter.isShowing)
    }

    @Test
    fun `the observer sees a message even when a hook then discards it`() {
        source.outcome = success(payload())
        val seen = mutableListOf<Int>()
        service(
            HostHooks(
                beforeDisplay = { DisplayDecision.DISCARD },
                observer = { seen.add(it.campaignId) }
            )
        ).start("alice")
        assertEquals(listOf(1), seen)
        assertNull(presenter.presented)
    }

    // --- personalisation wiring ---

    @Test
    fun `the variable cache is dropped on an event but not on session start`() {
        source.outcome = success(
            payload(campaignId = 3, trigger = """{ "type": "event", "name": "x" }""")
        )
        val svc = service()
        svc.start("alice")
        assertEquals("session start must not drop it", 0, variables.invalidations)

        svc.onEvent("x", emptyMap())
        assertEquals(1, variables.invalidations)
    }

    @Test
    fun `a personalised message renders substituted copy and never a raw brace`() {
        val personalised = """
            { "cooldownSeconds": 0, "messages": [ { "campaignId": 1, "messageType": 2,
              "trigger": { "type": "session_start" }, "content": {},
              "locale": { "header": "Hi {player_name}", "message": "You have {unknown} left" } } ] }
        """.trimIndent()
        source.outcome = success(personalised)
        service().start("alice")

        assertEquals("Hi Ahmed", presenter.resolved?.header)
        assertEquals("You have  left", presenter.resolved?.body)
        assertFalse(presenter.resolved!!.header!!.contains("{"))
        assertFalse(presenter.resolved!!.body!!.contains("{"))
    }

    // --- artwork retry placement ---

    @Test
    fun `the artwork retry fires before the nothing-displayable early return`() {
        source.outcome = success("""{ "messages": [] }""")
        service().start("alice")
        assertEquals(
            "a session where everything failed is the one that has to recover",
            1, artwork.retryCalls
        )
    }

    // --- stop ---

    @Test
    fun `stop dismisses, flushes and then disposes`() {
        source.outcome = success(payload())
        val svc = service()
        svc.start("alice")
        presenter.callbacks!!.onShown()

        svc.stop()
        assertEquals(1, presenter.dismissCount)
        assertTrue("flush must happen before dispose", analytics.flushes > 0)
        assertEquals(1, analytics.disposed)
        assertTrue(variables.clears > 0)
        assertFalse(svc.isStarted)
    }

    @Test
    fun `stop then start restarts the analytics scheduler`() {
        source.outcome = success(payload())
        val svc = service()
        svc.start("alice")
        svc.stop()
        svc.start("alice")
        assertEquals("the scheduler must be armed again", 2, analytics.started)
    }

    @Test
    fun `isStarted reflects start and stop`() {
        val svc = service()
        assertFalse(svc.isStarted)
        svc.start("alice")
        assertTrue(svc.isStarted)
        svc.stop()
        assertFalse(svc.isStarted)
    }

    @Test
    fun `nothing is evaluated before start`() {
        val svc = service()
        svc.onEvent("x", emptyMap())
        svc.onSurfaceAvailable()
        assertEquals(0, source.fetchCount)
        assertNull(presenter.presented)
    }
}
