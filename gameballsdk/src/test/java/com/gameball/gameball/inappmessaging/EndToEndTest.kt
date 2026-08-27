package com.gameball.gameball.inappmessaging

import android.app.Activity
import android.app.Application
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import com.gameball.gameball.GameballApp
import com.gameball.gameball.inappmessaging.artwork.ArtworkPrefetcher
import com.gameball.gameball.inappmessaging.data.IamEvent
import com.gameball.gameball.inappmessaging.data.IamEventType
import com.gameball.gameball.inappmessaging.data.MessageAnalytics
import com.gameball.gameball.inappmessaging.data.MessageParser
import com.gameball.gameball.inappmessaging.data.MessageSource
import com.gameball.gameball.inappmessaging.data.SyncOutcome
import com.gameball.gameball.inappmessaging.data.VariableSource
import com.gameball.gameball.inappmessaging.model.DisplayDecision
import com.gameball.gameball.inappmessaging.model.InAppMessage
import com.gameball.gameball.local.SharedPreferencesUtils
import com.gameball.gameball.model.request.Event
import com.gameball.gameball.model.request.GameballConfig
import com.gameball.gameball.network.Callback
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Drives the whole module through the public API only, against a stubbed source.
 *
 * This is the test that catches wiring mistakes every unit test passes. In Flutter, metadata
 * filters were fully unit-tested and completely unreachable because the event hook passed only
 * the event name and never its metadata - which is exactly what the filter case below covers.
 */
@RunWith(RobolectricTestRunner::class)
class EndToEndTest {

    private class StubSource(var raw: String) : MessageSource {
        var fetches = 0
        override suspend fun fetch(customerId: String): SyncOutcome {
            fetches++
            return SyncOutcome.Success(raw, MessageParser.parse(raw))
        }
    }

    private class RecordingAnalytics : MessageAnalytics {
        val events = mutableListOf<IamEvent>()
        override fun start() = Unit
        override fun record(event: IamEvent) { events.add(event) }
        override suspend fun flush() = Unit
        override fun dispose() = Unit
        fun types() = events.map { it.type }
    }

    private class ReadyArtwork : ArtworkPrefetcher {
        override suspend fun warm(urls: Set<String>) = Unit
        override fun isReady(url: String?) = true
        override fun retryFailedIfDue(nowMillis: Long) = Unit
        override fun reset() = Unit
    }

    private class FixedVariables(private val values: Map<String, String>) : VariableSource {
        override suspend fun values(customerId: String, needed: Set<String>) = values
        override fun invalidate() = Unit
        override fun clear() = Unit
    }

    private lateinit var app: GameballApp
    private lateinit var activity: Activity
    private lateinit var controller: org.robolectric.android.controller.ActivityController<Activity>
    private lateinit var analytics: RecordingAnalytics
    private lateinit var messaging: GameballInAppMessaging

    private val noop = object : Callback<Boolean> {
        override fun onSuccess(t: Boolean?) = Unit
        override fun onError(e: Throwable?) = Unit
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        SharedPreferencesUtils.init(context, Gson())
        SharedPreferencesUtils.getInstance().clearData()
        controller = Robolectric.buildActivity(Activity::class.java)
        activity = controller.get()
        analytics = RecordingAnalytics()

        app = GameballApp.getInstance(context)
        app.init(GameballConfig.builder().apiKey("k").lang("en").build())
        messaging = GameballInAppMessaging(context)
        // 2026-08-27T12:00:00Z. Pinned because the live payload carries an enabled
        // 22:00-08:00 UTC quiet-hours window, and a wall-clock test would suppress every
        // message for ten hours of every day.
        messaging.clockOverride = com.gameball.gameball.inappmessaging.runtime.Clock {
            1_787_832_000_000L
        }
    }

    private fun start(
        raw: String,
        values: Map<String, String> = emptyMap(),
        options: InAppMessagingOptions = InAppMessagingOptions.builder().build()
    ): StubSource {
        val source = StubSource(raw)
        messaging.sourceOverride = source
        messaging.analyticsOverride = analytics
        messaging.artworkOverride = ReadyArtwork()
        messaging.variablesOverride = FixedVariables(values)
        messaging.start("alice", options, apiPrefix = null, sdkVersion = "3.3.0")
        return source
    }

    private fun contentRoot(): ViewGroup = activity.findViewById(android.R.id.content)

    private fun paint() {
        val root = contentRoot()
        root.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )
        root.layout(0, 0, 1080, 1920)
        root.viewTreeObserver.dispatchOnPreDraw()
    }

    private fun sessionStartPayload(header: String = "Hello", body: String = "Body") = """
        { "cooldownSeconds": 0, "messages": [ {
            "campaignId": 2057, "messageType": 2, "priority": 4,
            "trigger": { "type": "session_start" },
            "content": { "action": { "type": "dismiss" } },
            "locale": { "header": "$header", "message": "$body" } } ] }
    """.trimIndent()

    @Test
    fun `a session-start campaign renders and reports an impression when it paints`() {
        start(sessionStartPayload())
        activityResumed()
        paint()

        assertTrue("a message should be on screen", contentRoot().childCount > 0)
        assertEquals(listOf(IamEventType.IMPRESSION), analytics.types())
        assertEquals(2057, analytics.events.first().campaignId)
    }

    @Test
    fun `a tap reports a click and removes the message`() {
        start(sessionStartPayload())
        activityResumed()
        paint()
        val before = contentRoot().childCount

        val card = contentRoot().getChildAt(before - 1)
            .findViewById<View>(com.gameball.gameball.R.id.gb_iam_modal_card)
        assertNotNull("the modal card should be on screen", card)
        card.performClick()

        assertTrue(
            "tapping the card reports a click; the scrim would have dismissed instead",
            analytics.types().contains(IamEventType.CLICK)
        )
        assertEquals("the message is dismissed before the action runs",
            before - 1, contentRoot().childCount)
    }

    /**
     * The defect-7 catcher, end to end: a filtered campaign is only reachable if sendEvent
     * passes the metadata map and not just the event name.
     */
    @Test
    fun `an event with metadata selects a filtered campaign`() {
        val filtered = """
            { "cooldownSeconds": 0, "messages": [ {
                "campaignId": 3001, "messageType": 2,
                "trigger": { "type": "event", "name": "purchase",
                    "metadataLogicalOperator": "And",
                    "metadataFilters": [
                        { "name": "price", "operator": "greaterThan", "value": 100 } ] },
                "content": {}, "locale": { "header": "Big spender" } } ] }
        """.trimIndent()
        start(filtered)
        activityResumed()

        messaging.onEvent("purchase", mapOf("price" to 50))
        assertEquals("below the threshold, nothing shows", 0, drawnCount())

        messaging.onEvent("purchase", mapOf("price" to 150))
        paint()
        assertTrue("above it, the campaign shows", drawnCount() > 0)
        assertEquals(3001, analytics.events.first().campaignId)
    }

    @Test
    fun `a personalised campaign renders substituted copy and never a raw brace`() {
        start(
            sessionStartPayload(header = "Hi {player_name}", body = "You have {unknown} left"),
            values = mapOf("player_name" to "Ahmed")
        )
        activityResumed()
        paint()

        val text = allText(contentRoot())
        assertTrue("expected the substituted name in: $text", text.contains("Hi Ahmed"))
        assertFalse("a raw brace must never reach the screen: $text", text.contains("{"))
    }

    @Test
    fun `an isTest campaign displays and reports nothing`() {
        val isTest = """
            { "cooldownSeconds": 0, "messages": [ { "campaignId": 9, "messageType": 2,
                "isTest": true, "trigger": { "type": "session_start" },
                "content": {}, "locale": { "header": "QA only" } } ] }
        """.trimIndent()
        start(isTest)
        activityResumed()
        paint()

        assertTrue(drawnCount() > 0)
        assertTrue("test telemetry must never reach statistics", analytics.events.isEmpty())
    }

    @Test
    fun `a host hook can discard a message, and still observes it`() {
        val seen = mutableListOf<InAppMessage>()
        start(
            sessionStartPayload(),
            options = InAppMessagingOptions.builder()
                .beforeDisplay { DisplayDecision.DISCARD }
                .observer { seen.add(it) }
                .build()
        )
        activityResumed()

        assertEquals("the observer sees every selection", 1, seen.size)
        assertEquals(2057, seen.first().campaignId)
        assertEquals("but nothing is drawn", 0, drawnCount())
    }

    @Test
    fun `the live payload drives the module end to end`() {
        val live = javaClass.classLoader!!.getResourceAsStream("live_sync_payload.json")!!
            .bufferedReader().use { it.readText() }
        start(live)
        activityResumed()
        paint()

        // The live account has session-start campaigns, so something must render.
        assertTrue("the live payload should produce a message", drawnCount() > 0)
    }

    @Test
    fun `stop dismisses what is showing`() {
        start(sessionStartPayload())
        activityResumed()
        paint()
        assertTrue(drawnCount() > 0)

        messaging.stop()
        assertEquals(0, drawnCount())
        assertFalse(messaging.isStarted)
    }

    // --- helpers ---

    /**
     * The module registers its Activity tracker during start, so the Activity has to be
     * brought to the foreground afterwards for the tracker to see it - which is also the real
     * ordering in a host app that opts in from Application.onCreate.
     */
    private fun activityResumed() {
        controller = Robolectric.buildActivity(Activity::class.java).setup()
        activity = controller.get()
    }

    private fun drawnCount(): Int = contentRoot().childCount

    private fun allText(view: View): String = when (view) {
        is android.widget.TextView -> view.text?.toString().orEmpty()
        is ViewGroup -> (0 until view.childCount).joinToString(" ") { allText(view.getChildAt(it)) }
        else -> ""
    }
}
