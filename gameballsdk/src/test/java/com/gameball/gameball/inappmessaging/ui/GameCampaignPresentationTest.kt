package com.gameball.gameball.inappmessaging.ui

import android.app.Activity
import android.app.Application
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import com.gameball.gameball.inappmessaging.data.WidgetUrl
import com.gameball.gameball.inappmessaging.domain.Campaign
import com.gameball.gameball.inappmessaging.domain.MessageButton
import com.gameball.gameball.inappmessaging.domain.MessageContent
import com.gameball.gameball.inappmessaging.domain.MessageType
import com.gameball.gameball.inappmessaging.domain.Trigger
import com.gameball.gameball.inappmessaging.domain.TriggerType
import com.gameball.gameball.inappmessaging.domain.WidgetContent
import com.gameball.gameball.inappmessaging.runtime.ActivityTracker
import com.gameball.gameball.inappmessaging.runtime.Clock
import com.gameball.gameball.inappmessaging.runtime.PresentationCallbacks
import com.gameball.gameball.inappmessaging.runtime.ResolvedMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GameCampaignPresentationTest {

    private lateinit var activity: Activity
    private lateinit var tracker: ActivityTracker

    private var shown = 0
    private var dismissed = 0
    private val urlsRequested = mutableListOf<String>()

    private val callbacks = object : PresentationCallbacks {
        override fun onShown() { shown++ }
        override fun onTapped(button: MessageButton?) = Unit
        override fun onDismissed() { dismissed++ }
    }

    private val noCallbacks = object : ActivityTracker.Callbacks {
        override fun onAppForegrounded() = Unit
        override fun onAppBackgrounded() = Unit
        override fun onSurfaceAvailable() = Unit
    }

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        tracker = ActivityTracker(ApplicationProvider.getApplicationContext<Application>(), noCallbacks)
        // The presenter resolves the Activity at presentation time and never caches one, so the
        // tracker has to have seen a resume before anything can be drawn.
        tracker.onActivityResumed(activity)
        shown = 0; dismissed = 0; urlsRequested.clear()
    }

    private fun gameCampaign(type: MessageType = MessageType.MODAL) = Campaign(
        campaignId = 0, variationId = null, dispatchId = null, name = "Spin the Wheel",
        priority = 0, messageType = type, rawMessageType = type.wire,
        expiresAtMillis = null, isTest = false,
        trigger = Trigger(TriggerType.SESSION_START),
        content = MessageContent.forGame(),
        responseIndex = 0,
        widget = WidgetContent(gameCampaignId = 9645, gameCampaignType = "wheel")
    )

    private val resolved = ResolvedMessage(null, null, emptyList())

    private fun presenter(builder: ((String) -> String)? = { openDetail ->
        urlsRequested.add(openDetail)
        "https://m.gameball.app/?openDetail=$openDetail"
    }) = OverlayPresenter(tracker, Clock { 1_000L }, builder)

    private fun contentRoot(): ViewGroup = activity.findViewById(android.R.id.content)

    private fun widgetView(): GameballWidgetView? =
        (contentRoot().getChildAt(contentRoot().childCount - 1) as? WidgetMessageView)?.widget

    /** Drives a real layout pass, exactly as a frame would. */
    private fun paint() {
        val root = contentRoot()
        root.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )
        root.layout(0, 0, 1080, 1920)
        root.viewTreeObserver.dispatchOnPreDraw()
    }

    @Test
    fun `a game is mounted and asks for its own url`() {
        assertTrue(presenter().present(gameCampaign(), resolved, callbacks))
        assertEquals(listOf("details_wheel_9645"), urlsRequested)
        assertNotNull(widgetView())
    }

    @Test
    fun `the widget is hidden until it reports itself drawn`() {
        presenter().present(gameCampaign(), resolved, callbacks)
        val widget = widgetView()!!

        assertTrue("the gate must hide the widget while it loads", widget.alpha < 1f)
        widget.reveal()
        assertEquals(1f, widget.alpha, 0f)
    }

    @Test
    fun `the gate never hides at zero`() {
        // The regression this pins: Flutter hid at opacity 0 and the page stopped loading — a
        // fully transparent surface is skipped in the draw path, and a WebView that is never
        // drawn has its render loop suspended. Zero is the one value that must not come back.
        assertTrue(GameballWidgetView.HIDDEN_ALPHA > 0f)
        assertTrue(GameballWidgetView.HIDDEN_ALPHA < 0.01f)
    }

    @Test
    fun `painting does not count an impression for a game`() {
        // The whole point of the gate. The surface is attached and painting for seconds before
        // there is anything on it, so counting at pre-draw books a view of a loading skeleton.
        presenter().present(gameCampaign(), resolved, callbacks)
        paint()
        assertEquals(0, shown)
    }

    @Test
    fun `a game is passed over when this build cannot open widgets`() {
        // An empty card is worse than no card. A build with no url builder declines the campaign,
        // which leaves it eligible at its next trigger.
        assertFalse(presenter(builder = null).present(gameCampaign(), resolved, callbacks))
        assertEquals(0, contentRoot().childCount)
    }

    @Test
    fun `a fullscreen game mounts edge to edge`() {
        assertTrue(presenter().present(gameCampaign(MessageType.FULLSCREEN), resolved, callbacks))
        assertNotNull(widgetView())
    }

    @Test
    fun `dismissing tears the WebView down`() {
        val presenter = presenter()
        presenter.present(gameCampaign(), resolved, callbacks)
        assertEquals(1, contentRoot().childCount)

        // A WebView left attached keeps a renderer process and this Activity alive.
        presenter.dismissCurrent()
        assertEquals(0, contentRoot().childCount)
        assertFalse(presenter.isShowing)
    }
}

@RunWith(RobolectricTestRunner::class)
class WidgetUrlTest {

    private fun url(openDetail: String = "details_wheel_9645", host: String = "m.gameball.app") =
        WidgetUrl.build(
            host = host,
            apiKey = "key-1",
            customerId = "alice",
            openDetail = openDetail,
            language = "en"
        )

    @Test
    fun `the open-details text reaches the widget`() {
        assertTrue(url().contains("openDetail=details_wheel_9645"))
    }

    @Test
    fun `navigation is always hidden`() {
        // A campaign opens one game. Leaving the widget's navigation in place would turn an
        // in-app message into a second entry point to the whole profile.
        assertTrue(url().contains("hideNavigation=true"))
    }

    @Test
    fun `the host is honoured, so an alpha integration does not open production`() {
        // The trap this closes: "this campaign is not available" is indistinguishable from a
        // campaign that does not exist, and an environment mismatch produces exactly that.
        assertTrue(url(host = "alpha-m.gameball.app").startsWith("https://alpha-m.gameball.app/"))
    }

    @Test
    fun `credentials come from the SDK, never from the campaign`() {
        assertTrue(url().contains("apiKey=key-1"))
        assertTrue(url().contains("customerId=alice"))
    }

    @Test
    fun `an absent optional field is left out rather than sent empty`() {
        val bare = WidgetUrl.build(
            host = "m.gameball.app", apiKey = "k", customerId = null,
            openDetail = "details_quiz_1", language = null
        )
        assertFalse(bare.contains("lang="))
        assertFalse(bare.contains("platform="))
        assertTrue(bare.contains("customerId=&") || bare.endsWith("customerId="))
    }
}
