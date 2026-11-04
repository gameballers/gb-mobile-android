package com.gameball.gameball.inappmessaging.ui

import android.app.Activity
import android.app.Application
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ApplicationProvider
import com.gameball.gameball.R
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
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class GameCampaignPresentationTest {

    private lateinit var activity: Activity
    private lateinit var tracker: ActivityTracker

    private var shown = 0
    private var dismissed = 0
    private var failed = 0
    private val urlsRequested = mutableListOf<String>()

    private val callbacks = object : PresentationCallbacks {
        override fun onShown() { shown++ }
        override fun onTapped(button: MessageButton?) = Unit
        override fun onDismissed() { dismissed++ }
        override fun onFailed() { failed++ }
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
        shown = 0; dismissed = 0; failed = 0; urlsRequested.clear()
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

    private fun overlay(): WidgetMessageView? =
        contentRoot().getChildAt(contentRoot().childCount - 1) as? WidgetMessageView

    private fun widgetView(): GameballWidgetView? = overlay()?.widget

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

    /**
     * The whole overlay, not just the WebView: hiding only the WebView left a dark backdrop and
     * an empty white card on screen for up to twelve seconds, or a black screen in fullscreen.
     */
    @Test
    fun `the whole game is invisible until it reports itself drawn`() {
        listOf(MessageType.MODAL, MessageType.FULLSCREEN).forEach { type ->
            val presenter = presenter()
            presenter.present(gameCampaign(type), resolved, callbacks)
            val overlay = overlay()!!

            assertTrue("$type: the scrim, card and surface must be hidden", overlay.alpha < 0.01f)
            assertTrue("$type: hidden, never at zero", overlay.alpha > 0f)
            assertEquals("$type: the game is not hidden a second time", 1f, overlay.widget.alpha, 0f)

            overlay.widget.settle(ready = true)
            assertEquals("$type", 1f, overlay.alpha, 0f)
            presenter.dismissCurrent()
        }
    }

    @Test
    fun `the gate never hides at zero`() {
        // The regression this pins: Flutter hid at opacity 0 and the page stopped loading — a
        // fully transparent surface is skipped in the draw path, and a WebView that is never
        // drawn has its render loop suspended. Zero is the one value that must not come back.
        assertTrue(WidgetMessageView.HIDDEN_ALPHA > 0f)
        assertTrue(WidgetMessageView.HIDDEN_ALPHA < 0.01f)
    }

    private fun touchAt(x: Float, y: Float): MotionEvent =
        MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, x, y, 0)

    /** An invisible game that swallows taps is a frozen app as far as the customer can tell. */
    @Test
    fun `a loading game lets every touch through to the app`() {
        listOf(MessageType.MODAL, MessageType.FULLSCREEN).forEach { type ->
            val presenter = presenter()
            presenter.present(gameCampaign(type), resolved, callbacks)
            val overlay = overlay()!!
            @Suppress("DEPRECATION")
            val insets = WindowInsetsCompat.Builder()
                .setSystemWindowInsets(Insets.of(0, 96, 0, 48))
                .build()
            ViewCompat.dispatchApplyWindowInsets(overlay, insets)
            paint()

            assertFalse("$type: while loading", overlay.dispatchTouchEvent(touchAt(10f, 10f)))
            assertFalse("$type: while loading", overlay.dispatchTouchEvent(touchAt(540f, 960f)))

            overlay.widget.settle(ready = true)
            assertTrue(
                "$type: once drawn, nothing reaches the app beneath",
                overlay.dispatchTouchEvent(touchAt(10f, 10f))
            )
            presenter.dismissCurrent()
        }
    }

    @Test
    fun `back belongs to the app until the game is drawn`() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        tracker.onActivityResumed(host)
        presenter().present(gameCampaign(), resolved, callbacks)
        val root = host.findViewById<ViewGroup>(android.R.id.content)

        host.onBackPressedDispatcher.onBackPressed()

        assertEquals("nothing visible may take back from the app", 0, dismissed)
        assertTrue("the app's own back ran", host.isFinishing)
        assertEquals(1, root.childCount)
    }

    @Test
    fun `a game drawn after its screen went away is let go, not shown`() {
        val presenter = presenter()
        presenter.present(gameCampaign(), resolved, callbacks)
        val overlay = overlay()!!
        // Its Activity finished while it loaded, which takes the window and the overlay with it.
        contentRoot().removeView(overlay)

        overlay.widget.settle(ready = true)

        assertEquals("nobody saw it", 0, shown)
        assertEquals(1, failed)
        assertFalse(presenter.isShowing)
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

    // --- closing ---

    /** The page's own close, `GameballWidget.closeWidget()`, used to only tell the service. */
    @Test
    fun `the widget's own close removes the game`() {
        val presenter = presenter()
        presenter.present(gameCampaign(), resolved, callbacks)
        widgetView()!!.settle(ready = true)

        widgetView()!!.closeRequested()

        assertEquals("the game must leave the screen", 0, contentRoot().childCount)
        assertFalse(presenter.isShowing)
        assertEquals(1, dismissed)
    }

    @Test
    fun `a tap on the backdrop removes the game`() {
        val presenter = presenter()
        presenter.present(gameCampaign(), resolved, callbacks)
        widgetView()!!.settle(ready = true)

        overlay()!!.performClick()

        assertEquals("the game must leave the screen", 0, contentRoot().childCount)
        assertFalse(presenter.isShowing)
        assertEquals(1, dismissed)
    }

    @Test
    fun `a close that arrives after the game is gone touches nothing`() {
        val presenter = presenter()
        presenter.present(gameCampaign(), resolved, callbacks)
        val stale = widgetView()!!
        presenter.dismissCurrent()

        // A bridge call already on its way when the WebView was torn down.
        stale.closeRequested()
        assertEquals(0, dismissed)
    }

    // --- the close control ---

    private fun dp(value: Float) = MessageMetrics.dp(activity, value)

    /** On mobile the widget page draws no close or back control for a game; this is the way out. */
    @Test
    fun `every game draws a labelled close control at its top end`() {
        listOf(MessageType.MODAL, MessageType.FULLSCREEN).forEach { type ->
            val presenter = presenter()
            presenter.present(gameCampaign(type), resolved, callbacks)
            widgetView()!!.settle(ready = true)
            paint()

            val close = overlay()!!.closeTarget
            assertEquals("$type", View.VISIBLE, close.visibility)
            assertEquals("$type", activity.getString(R.string.gb_iam_close), close.contentDescription?.toString())
            assertEquals(
                "$type: top end, so it mirrors under RTL",
                Gravity.TOP or Gravity.END,
                (close.layoutParams as FrameLayout.LayoutParams).gravity
            )
            assertTrue("$type: a 48dp target at least", close.width >= dp(48f) && close.height >= dp(48f))
            presenter.dismissCurrent()
        }
    }

    @Test
    fun `the close control closes the game`() {
        listOf(MessageType.MODAL, MessageType.FULLSCREEN).forEach { type ->
            dismissed = 0
            val presenter = presenter()
            presenter.present(gameCampaign(type), resolved, callbacks)
            widgetView()!!.settle(ready = true)

            overlay()!!.closeTarget.performClick()

            assertEquals("$type", 0, contentRoot().childCount)
            assertFalse("$type", presenter.isShowing)
            assertEquals("$type", 1, dismissed)
        }
    }

    @Test
    fun `the close control clears the status bar and a cutout`() {
        listOf(MessageType.MODAL, MessageType.FULLSCREEN).forEach { type ->
            val presenter = presenter()
            presenter.present(gameCampaign(type), resolved, callbacks)
            val overlay = overlay()!!

            // An edge-to-edge host: a 96px status bar (or cutout) on top, a nav bar below.
            @Suppress("DEPRECATION")
            val insets = WindowInsetsCompat.Builder()
                .setSystemWindowInsets(Insets.of(0, 96, 0, 48))
                .build()
            ViewCompat.dispatchApplyWindowInsets(overlay, insets)
            paint()

            val close = overlay.closeTarget
            val top = (close.parent as View).top + close.top
            assertTrue("$type: the close starts at ${top}px, under the 96px bar", top >= 96)
            presenter.dismissCurrent()
        }
    }

    @Test
    fun `back still closes a game`() {
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        tracker.onActivityResumed(host)
        val presenter = presenter()
        presenter.present(gameCampaign(), resolved, callbacks)
        val root = host.findViewById<ViewGroup>(android.R.id.content)
        (root.getChildAt(root.childCount - 1) as WidgetMessageView).widget.settle(ready = true)

        host.onBackPressedDispatcher.onBackPressed()

        assertEquals(0, root.childCount)
        assertEquals(1, dismissed)
        assertFalse("the host's route must survive", host.isFinishing)
    }

    // --- a game that never loads ---

    @Test
    fun `a game that never becomes ready is taken away unseen and reported as failed`() {
        val presenter = presenter()
        presenter.present(gameCampaign(), resolved, callbacks)

        shadowOf(Looper.getMainLooper()).idleFor(
            GameballWidgetView.READY_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS
        )

        assertEquals(0, contentRoot().childCount)
        assertFalse(presenter.isShowing)
        assertEquals("the service must hear it failed, or it keeps the game waiting", 1, failed)
        assertEquals("nothing was seen, so nothing is reported", 0, shown)
        assertEquals(0, dismissed)
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
