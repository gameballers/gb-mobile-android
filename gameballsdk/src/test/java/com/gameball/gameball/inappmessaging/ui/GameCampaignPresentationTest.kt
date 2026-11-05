package com.gameball.gameball.inappmessaging.ui

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
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
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class GameCampaignPresentationTest {

    private lateinit var activity: Activity
    private lateinit var tracker: ActivityTracker

    private var shown = 0
    private var dismissed = 0
    private var failed = 0
    private var played = 0
    private val urlsRequested = mutableListOf<String>()

    private val callbacks = object : PresentationCallbacks {
        override fun onShown() { shown++ }
        override fun onTapped(button: MessageButton?) = Unit
        override fun onDismissed() { dismissed++ }
        override fun onFailed() { failed++ }
        override fun onGameCompleted() { played++ }
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
        shown = 0; dismissed = 0; failed = 0; played = 0; urlsRequested.clear()
    }

    private fun gameCampaign(type: MessageType = MessageType.MODAL, gameId: Int = 9645) = Campaign(
        campaignId = 0, variationId = null, dispatchId = null, name = "Spin the Wheel",
        priority = 0, messageType = type, rawMessageType = type.wire,
        expiresAtMillis = null, isTest = false,
        trigger = Trigger(TriggerType.SESSION_START),
        content = MessageContent.forGame(),
        responseIndex = 0,
        widget = WidgetContent(gameCampaignId = gameId, gameCampaignType = "wheel")
    )

    private val resolved = ResolvedMessage(null, null, emptyList())

    private fun presenter(builder: ((String) -> String)? = { openDetail ->
        urlsRequested.add(openDetail)
        "https://m.gameball.app/?openDetail=$openDetail"
    }) = OverlayPresenter(tracker, Clock { 1_000L }, builder)

    /** The whole window: a game hangs over all of it, the app bar and status bar included. */
    private fun gameRoot(): ViewGroup = activity.window.decorView as ViewGroup

    /** Every game overlay in [of], wherever it hangs. */
    private fun games(of: View = gameRoot()): List<WidgetMessageView> = when (of) {
        is WidgetMessageView -> listOf(of)
        is ViewGroup -> (0 until of.childCount).flatMap { games(of.getChildAt(it)) }
        else -> emptyList()
    }

    private fun overlay(): WidgetMessageView? = games().lastOrNull()

    private fun widgetView(): GameballWidgetView? = overlay()?.widget

    /** Drives a real layout pass, exactly as a frame would. */
    private fun paint() {
        val root = gameRoot()
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
        host.onBackPressedDispatcher.onBackPressed()

        assertEquals("nothing visible may take back from the app", 0, dismissed)
        assertTrue("the app's own back ran", host.isFinishing)
        assertEquals(1, games(host.window.decorView).size)
    }

    @Test
    fun `a game drawn after its screen went away is let go, not shown`() {
        val presenter = presenter()
        presenter.present(gameCampaign(), resolved, callbacks)
        val overlay = overlay()!!
        // Its Activity finished while it loaded, which takes the window and the overlay with it.
        (overlay.parent as ViewGroup).removeView(overlay)

        overlay.widget.settle(ready = true)

        assertEquals("nobody saw it", 0, shown)
        assertEquals(1, failed)
        assertFalse(presenter.isShowing)
    }

    // --- the widget's own word (gameReady) ---

    private val gameReady =
        """{"type":"gameReady","metadata":{"page":"details_wheel_9645","playable":true}}"""
    private val unplayable =
        """{"type":"gameReady","metadata":{"page":"details_wheel_9645","playable":false,"reason":"unplayable"}}"""

    /** Tracker C4: an Arabic scratch card with no Arabic title never passed the page guess. */
    @Test
    fun `a game the widget says is on screen is revealed`() {
        val presenter = presenter()
        presenter.present(gameCampaign(), resolved, callbacks)

        widgetView()!!.eventPosted(gameReady)

        assertEquals(1f, overlay()!!.alpha, 0f)
        assertEquals(1, shown)
        assertTrue(presenter.isShowing)
    }

    /** Tracker B3: the widget's "already played today" page must never be put over the app. */
    @Test
    fun `a game the widget says cannot be played is taken away unseen`() {
        val presenter = presenter()
        presenter.present(gameCampaign(), resolved, callbacks)

        widgetView()!!.eventPosted(unplayable)

        assertEquals("nobody saw it", 0, shown)
        assertEquals(1, failed)
        assertFalse(presenter.isShowing)
        assertNull(overlay())
    }

    @Test
    fun `any other event leaves a loading game hidden`() {
        presenter().present(gameCampaign(), resolved, callbacks)

        widgetView()!!.eventPosted("""{"type":"gameCompleted"}""")

        assertTrue(overlay()!!.alpha < 0.01f)
        assertEquals(0, shown)
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
        assertEquals(0, games().size)
    }

    @Test
    fun `a fullscreen game mounts edge to edge`() {
        assertTrue(presenter().present(gameCampaign(MessageType.FULLSCREEN), resolved, callbacks))
        assertNotNull(widgetView())
    }

    /**
     * Tracker C1: in the content root a fullscreen game covered only the area under the app's
     * action bar, with the status bar and title bar still showing above it. It hangs over the
     * whole window instead, as on the other SDKs; its content still keeps to the safe area.
     */
    @Test
    fun `a game covers the whole window, app bar included`() {
        listOf(MessageType.MODAL, MessageType.FULLSCREEN).forEach { type ->
            val presenter = presenter()
            presenter.present(gameCampaign(type), resolved, callbacks)

            assertSame("$type", activity.window.decorView, overlay()!!.parent)
            presenter.dismissCurrent()
            assertEquals("$type: and leaves it whole", 0, games().size)
        }
    }

    /**
     * Over the whole window the bars are measured on the window itself. A window that fits system
     * windows hands the views under its decor insets with the bars already taken out, which would
     * have put the game under the status bar.
     */
    @Test
    fun `a game over the whole window keeps clear of the bars even when its insets arrive consumed`() {
        val window = Insets.of(0, 96, 0, 48)
        assertEquals(window, WidgetMessageView.safeArea(dispatched = Insets.NONE, window = window))
        assertEquals(
            "edge by edge",
            Insets.of(10, 96, 0, 48),
            WidgetMessageView.safeArea(dispatched = Insets.of(10, 0, 0, 0), window = window)
        )
        assertEquals(
            "no window insets to read: what was dispatched",
            Insets.of(0, 24, 0, 0),
            WidgetMessageView.safeArea(dispatched = Insets.of(0, 24, 0, 0), window = null)
        )
    }

    // --- closing ---

    /** The page's own close, `GameballWidget.closeWidget()`, used to only tell the service. */
    @Test
    fun `the widget's own close removes the game`() {
        val presenter = presenter()
        presenter.present(gameCampaign(), resolved, callbacks)
        widgetView()!!.settle(ready = true)

        widgetView()!!.closeRequested()

        assertEquals("the game must leave the screen", 0, games().size)
        assertFalse(presenter.isShowing)
        assertEquals(1, dismissed)
    }

    @Test
    fun `a tap on the backdrop removes the game`() {
        val presenter = presenter()
        presenter.present(gameCampaign(), resolved, callbacks)
        widgetView()!!.settle(ready = true)

        overlay()!!.performClick()

        assertEquals("the game must leave the screen", 0, games().size)
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
                "$type: the top end of a left-to-right game",
                Gravity.TOP or Gravity.RIGHT,
                (close.layoutParams as FrameLayout.LayoutParams).gravity
            )
            assertTrue("$type: a 48dp target at least", close.width >= dp(48f) && close.height >= dp(48f))
            presenter.dismissCurrent()
        }
    }

    /**
     * Tracker C4: the close sits at the top end of the game, which mirrors with the game's own
     * language. Following the app's direction left it at the top right of an Arabic game whenever
     * the app itself was left to right, or had not declared RTL support at all.
     */
    @Test
    fun `the close control sits at the top end of the game's own language`() {
        mapOf("ar" to Gravity.LEFT, "en" to Gravity.RIGHT, "he" to Gravity.LEFT).forEach { (lang, side) ->
            val presenter = presenter(builder = { "https://m.gameball.app/?lang=$lang&openDetail=$it" })
            presenter.present(gameCampaign(), resolved, callbacks)

            val params = overlay()!!.closeTarget.layoutParams as FrameLayout.LayoutParams
            assertEquals(lang, Gravity.TOP or side, params.gravity)
            val inset = if (side == Gravity.LEFT) params.leftMargin else params.rightMargin
            assertTrue("$lang: kept off the edge", inset > 0)
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

            assertEquals("$type", 0, games().size)
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
        games(host.window.decorView).last().widget.settle(ready = true)

        host.onBackPressedDispatcher.onBackPressed()

        assertEquals(0, games(host.window.decorView).size)
        assertEquals(1, dismissed)
        assertFalse("the host's route must survive", host.isFinishing)
    }

    /**
     * On a host that is not a ComponentActivity, back was heard by a key listener on the overlay,
     * which fires only while the overlay itself has focus. The WebView takes focus at the first
     * touch, so from then on back went past the game and finished the host Activity.
     */
    @Test
    fun `back closes a game on a plain Activity with focus inside the game`() {
        val presenter = presenter()
        presenter.present(gameCampaign(), resolved, callbacks)
        val overlay = overlay()!!
        overlay.widget.settle(ready = true)

        // A tap on the game moves focus into it, off the overlay. Robolectric's WebView cannot take
        // focus - it hands requestFocus to a stub - so a focusable view beside it stands in.
        val keysHeard = mutableListOf<Int>()
        val page = View(activity).apply {
            isFocusableInTouchMode = true
            setOnKeyListener { _, keyCode, _ -> keysHeard.add(keyCode); false }
        }
        overlay.widget.addView(page)
        paint()
        assertTrue(page.requestFocus())
        assertFalse("focus is inside the game, not on the overlay", overlay.isFocused)

        activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        assertEquals("any other key is the game's", listOf(KeyEvent.KEYCODE_ENTER), keysHeard)
        assertTrue(presenter.isShowing)

        activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
        activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK))

        assertFalse("the host Activity must survive", activity.isFinishing)
        assertEquals("the game must leave the screen", 0, games().size)
        assertFalse(presenter.isShowing)
        assertEquals(1, dismissed)
    }

    // --- identity ---

    /**
     * Every game's campaignId is 0. Matched on that, a second game picked up the first one's
     * presentation, already marked shown, and its own impression was never recorded — so a
     * once-ever game would come back.
     */
    @Test
    fun `a different game never inherits another game's presentation`() {
        val presenter = presenter()
        presenter.present(gameCampaign(gameId = 9645), resolved, callbacks)
        widgetView()!!.settle(ready = true)
        assertEquals(1, shown)

        // Rotation takes the view away and there is no Activity to put it back into, so the
        // presentation stays behind.
        tracker.onActivityPaused(activity)
        assertFalse(presenter.rePresentAfterConfigurationChange(resolved, callbacks))
        tracker.onActivityResumed(activity)

        assertTrue(presenter.present(gameCampaign(gameId = 9702), resolved, callbacks))
        widgetView()!!.settle(ready = true)
        assertEquals("the second game is a view of its own", 2, shown)
    }

    // --- a play ---

    /** The widget's events reached the bridge, but nobody was listening. */
    @Test
    fun `a finished game is reported, and nothing else is`() {
        presenter().present(gameCampaign(), resolved, callbacks)
        val widget = widgetView()!!
        widget.settle(ready = true)

        widget.eventPosted("""{"type":"rewardClaimed","metadata":{}}""")
        assertEquals(0, played)

        widget.eventPosted(
            """{"type":"gameCompleted","metadata":{"campaignId":"9645","campaignType":"spinTheWheel","hasWon":true}}"""
        )
        assertEquals(1, played)
        assertEquals("a play does not close the game; the customer still sees the result", 1, games().size)
    }

    // --- a game that never loads ---

    @Test
    fun `a game that never becomes ready is taken away unseen and reported as failed`() {
        val presenter = presenter()
        presenter.present(gameCampaign(), resolved, callbacks)

        shadowOf(Looper.getMainLooper()).idleFor(
            GameballWidgetView.READY_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS
        )

        assertEquals(0, games().size)
        assertFalse(presenter.isShowing)
        assertEquals("the service must hear it failed, or it keeps the game waiting", 1, failed)
        assertEquals("nothing was seen, so nothing is reported", 0, shown)
        assertEquals(0, dismissed)
    }

    // --- a renderer that dies ---

    /**
     * The WebView's client, told its renderer is gone. No RenderProcessGoneDetail is built: the
     * class arrived in API 26, and constructing one here would fail this whole class on the
     * suite's default SDK. Nothing reads it.
     */
    private fun rendererGone(webView: WebView): Boolean =
        shadowOf(webView).webViewClient.onRenderProcessGone(webView, null)

    /** Android's own answer to a renderer that crashed or was killed is to take the app with it. */
    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun `a loading game whose renderer dies is taken away unseen, and the app lives on`() {
        val presenter = presenter()
        presenter.present(gameCampaign(), resolved, callbacks)
        val webView = widgetView()!!.webView

        assertTrue("unhandled, a dead renderer takes the host app down", rendererGone(webView))

        assertNull("the dead WebView is out of the hierarchy", webView.parent)
        assertTrue("and destroyed", shadowOf(webView).wasDestroyCalled())
        assertEquals(0, games().size)
        assertFalse(presenter.isShowing)
        assertEquals("the way a failed load goes", 1, failed)
        assertEquals(0, shown)
        assertEquals(0, dismissed)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun `a shown game whose renderer dies leaves the screen the same way`() {
        val presenter = presenter()
        presenter.present(gameCampaign(), resolved, callbacks)
        widgetView()!!.settle(ready = true)
        val webView = widgetView()!!.webView

        assertTrue(rendererGone(webView))

        assertNull(webView.parent)
        assertTrue(shadowOf(webView).wasDestroyCalled())
        assertEquals(0, games().size)
        assertFalse(presenter.isShowing)
        assertEquals(1, failed)
        assertEquals("its one impression stands, and nothing more is reported", 1, shown)
        assertEquals(0, dismissed)
    }

    @Test
    fun `dismissing tears the WebView down`() {
        val presenter = presenter()
        presenter.present(gameCampaign(), resolved, callbacks)
        assertEquals(1, games().size)

        // A WebView left attached keeps a renderer process and this Activity alive.
        presenter.dismissCurrent()
        assertEquals(0, games().size)
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

    @Test
    fun `the url written to the log carries neither the api key nor the session token`() {
        val full = WidgetUrl.build(
            host = "m.gameball.app/alpha", apiKey = "key-1", customerId = "alice",
            openDetail = "details_wheel_9645", language = "en", sessionToken = "token-1"
        )
        val logged = WidgetUrl.redacted(full)

        assertFalse(logged, logged.contains("key-1"))
        assertFalse(logged, logged.contains("token-1"))
        // Everything that tells an environment or id mismatch apart is still there.
        assertTrue(logged, logged.startsWith("https://m.gameball.app/alpha/"))
        assertTrue(logged, logged.contains("customerId=alice"))
        assertTrue(logged, logged.contains("openDetail=details_wheel_9645"))
    }
}
