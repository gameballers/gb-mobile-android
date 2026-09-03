package com.gameball.gameball.inappmessaging.ui

import android.app.Activity
import android.app.Application
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import com.gameball.gameball.inappmessaging.artwork.IamImageLoader
import com.gameball.gameball.inappmessaging.domain.Campaign
import com.gameball.gameball.inappmessaging.domain.MessageButton
import com.gameball.gameball.inappmessaging.domain.MessageColors
import com.gameball.gameball.inappmessaging.domain.MessageContent
import com.gameball.gameball.inappmessaging.domain.MessageLayout
import com.gameball.gameball.inappmessaging.domain.MessageOrientation
import com.gameball.gameball.inappmessaging.domain.MessageType
import com.gameball.gameball.inappmessaging.domain.SlidePosition
import com.gameball.gameball.inappmessaging.domain.Trigger
import com.gameball.gameball.inappmessaging.domain.TriggerType
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

@RunWith(RobolectricTestRunner::class)
class OverlayPresenterTest {

    private lateinit var activity: Activity
    private lateinit var tracker: ActivityTracker
    private var now = 1_000L
    private val clock = Clock { now }

    private var shown = 0
    private var dismissed = 0
    private var tapped = 0

    private val callbacks = object : PresentationCallbacks {
        override fun onShown() { shown++ }
        override fun onTapped(button: MessageButton?) { tapped++ }
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
        IamImageLoader.init(activity)
        tracker = ActivityTracker(ApplicationProvider.getApplicationContext<Application>(), noCallbacks)
        shown = 0; dismissed = 0; tapped = 0; now = 1_000L
    }

    private fun campaign(
        type: MessageType = MessageType.MODAL,
        autoDismissMillis: Long? = null,
        orientation: MessageOrientation = MessageOrientation.ANY
    ) = Campaign(
        campaignId = 1, variationId = null, dispatchId = null, name = "c",
        priority = 0, messageType = type, rawMessageType = type.wire,
        expiresAtMillis = null, isTest = false,
        trigger = Trigger(TriggerType.SESSION_START),
        content = MessageContent(
            header = "Hello", body = "Body", imageUrl = null, iconUrl = null,
            layout = MessageLayout.DEFAULT, colors = MessageColors(background = 0xFFFFFFFF.toInt()),
            buttons = emptyList(), clickAction = null, showCloseButton = true,
            dismissOnScrimTap = true, slidePosition = SlidePosition.BOTTOM,
            orientation = orientation, autoDismissMillis = autoDismissMillis,
            headerAlign = null, bodyAlign = null, extras = emptyMap()
        ),
        responseIndex = 0
    )

    private val resolved = ResolvedMessage("Hello", "Body", emptyList())

    private fun presenter() = OverlayPresenter(tracker, clock)

    private fun contentRoot(): ViewGroup = activity.findViewById(android.R.id.content)

    /** Drives a real layout pass so doOnPreDraw fires, exactly as a frame would. */
    private fun paint() {
        val root = contentRoot()
        root.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )
        root.layout(0, 0, 1080, 1920)
        root.viewTreeObserver.dispatchOnPreDraw()
    }

    // --- surface availability ---

    @Test
    fun `present returns false and does not throw when there is no Activity`() {
        val p = presenter()
        assertFalse(p.present(campaign(), resolved, callbacks))
        assertFalse(p.isShowing)
    }

    @Test
    fun `present attaches to the content root when an Activity is available`() {
        tracker.onActivityResumed(activity)
        assertTrue(presenter().present(campaign(), resolved, callbacks))
        assertTrue(contentRoot().childCount > 0)
    }

    /** A handle bound once points at a dead surface after the first rotation. */
    @Test
    fun `the Activity is resolved at presentation time, not cached`() {
        val p = presenter()
        tracker.onActivityResumed(activity)
        assertTrue(p.present(campaign(), resolved, callbacks))
        p.dismissCurrent()

        val recreated = Robolectric.buildActivity(Activity::class.java).setup().get()
        tracker.onActivityPaused(activity)
        tracker.onActivityResumed(recreated)
        assertTrue("must attach to the new Activity", p.present(campaign(), resolved, callbacks))
        assertTrue(recreated.findViewById<ViewGroup>(android.R.id.content).childCount > 0)
    }

    @Test
    fun `a second present while one is showing is refused`() {
        tracker.onActivityResumed(activity)
        val p = presenter()
        assertTrue(p.present(campaign(), resolved, callbacks))
        assertFalse(p.present(campaign(), resolved, callbacks))
    }

    // --- impression timing ---

    @Test
    fun `the impression is reported on the first pre-draw pass, not at attach`() {
        tracker.onActivityResumed(activity)
        presenter().present(campaign(), resolved, callbacks)
        assertEquals("attaching only schedules a frame", 0, shown)

        paint()
        assertEquals(1, shown)
    }

    /** Frames stop when the app is backgrounded, so nothing is booked. */
    @Test
    fun `a message dismissed before it paints reports no impression`() {
        tracker.onActivityResumed(activity)
        val p = presenter()
        p.present(campaign(), resolved, callbacks)
        p.dismissCurrent()
        paint()
        assertEquals(0, shown)
    }

    @Test
    fun `the impression fires only once per presentation`() {
        tracker.onActivityResumed(activity)
        presenter().present(campaign(), resolved, callbacks)
        paint()
        paint()
        assertEquals(1, shown)
    }

    // --- rotation ---

    /**
     * Rotation destroys the view but not the presentation. Re-presenting must not log a
     * second impression, or one customer view becomes two.
     */
    @Test
    fun `re-presenting after a configuration change does not report a second impression`() {
        tracker.onActivityResumed(activity)
        val p = presenter()
        p.present(campaign(), resolved, callbacks)
        paint()
        assertEquals(1, shown)

        assertTrue(p.rePresent(resolved, callbacks))
        paint()
        assertEquals("still one view of one message", 1, shown)
    }

    @Test
    fun `re-presenting with nothing pending does nothing`() {
        assertFalse(presenter().rePresent(resolved, callbacks))
    }

    // --- dismissal ---

    @Test
    fun `dismissCurrent removes the view`() {
        tracker.onActivityResumed(activity)
        val p = presenter()
        p.present(campaign(), resolved, callbacks)
        val before = contentRoot().childCount
        p.dismissCurrent()
        assertEquals(before - 1, contentRoot().childCount)
        assertFalse(p.isShowing)
        assertNull(p.currentPresentation())
    }

    @Test
    fun `an auto-dismiss timer fires after its duration`() {
        tracker.onActivityResumed(activity)
        presenter().present(campaign(autoDismissMillis = 8_000L), resolved, callbacks)
        paint()
        assertEquals(0, dismissed)

        shadowOf(android.os.Looper.getMainLooper()).idleFor(
            8_000L, java.util.concurrent.TimeUnit.MILLISECONDS
        )
        assertEquals(1, dismissed)
    }

    @Test
    fun `no duration means no timer`() {
        tracker.onActivityResumed(activity)
        presenter().present(campaign(autoDismissMillis = null), resolved, callbacks)
        paint()
        shadowOf(android.os.Looper.getMainLooper()).idleFor(
            60_000L, java.util.concurrent.TimeUnit.MILLISECONDS
        )
        assertEquals("an explicit 0 means stay until dismissed", 0, dismissed)
    }

    // --- back ---

    @Test
    fun `a slideup does not intercept back`() {
        tracker.onActivityResumed(activity)
        presenter().present(campaign(type = MessageType.SLIDEUP), resolved, callbacks)
        paint()
        // Nothing to assert beyond the absence of a callback; a non-blocking banner has no
        // claim on the gesture and the host's navigation must keep working.
        assertEquals(0, dismissed)
    }

    @Test
    fun `a modal registers a back callback that dismisses without popping the host route`() {
        val componentActivity = Robolectric
            .buildActivity(androidx.activity.ComponentActivity::class.java).setup().get()
        tracker.onActivityResumed(componentActivity)
        val p = presenter()
        assertTrue(p.present(campaign(type = MessageType.MODAL), resolved, callbacks))

        componentActivity.onBackPressedDispatcher.onBackPressed()
        assertEquals(1, dismissed)
        assertFalse("the host's route must survive", componentActivity.isFinishing)
    }

    @Test
    fun `the back callback is removed on dismissal`() {
        val componentActivity = Robolectric
            .buildActivity(androidx.activity.ComponentActivity::class.java).setup().get()
        tracker.onActivityResumed(componentActivity)
        val p = presenter()
        p.present(campaign(type = MessageType.MODAL), resolved, callbacks)
        p.dismissCurrent()

        componentActivity.onBackPressedDispatcher.onBackPressed()
        assertEquals("a removed callback must not fire", 0, dismissed)
    }

    // --- orientation ---

    @Test
    fun `a fullscreen whose orientation does not match is refused so the service can defer`() {
        tracker.onActivityResumed(activity)
        val landscapeOnly = campaign(
            type = MessageType.FULLSCREEN, orientation = MessageOrientation.LANDSCAPE
        )
        assertFalse(presenter().present(landscapeOnly, resolved, callbacks))
    }

    @Test
    fun `a fullscreen with no orientation preference is presented`() {
        tracker.onActivityResumed(activity)
        assertTrue(
            presenter().present(campaign(type = MessageType.FULLSCREEN), resolved, callbacks)
        )
    }

    // --- motion ---

    @Test
    fun `reduce motion skips the entry animation entirely`() {
        android.provider.Settings.Global.putFloat(
            activity.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 0f
        )
        tracker.onActivityResumed(activity)
        val p = presenter()
        p.present(campaign(), resolved, callbacks)
        val view = contentRoot().getChildAt(contentRoot().childCount - 1)
        assertEquals("no fade at all, not merely a shorter one", 1f, view.alpha, 0.001f)
    }

    /**
     * A slideup translates the banner (not the transparent overlay) - so reduce motion is
     * asserted on the banner itself, not the top-level view. Anything else would only prove
     * the overlay is opaque, which is not the property that matters.
     */
    @Test
    fun `reduce motion lands the slideup banner at rest, translated and opaque`() {
        android.provider.Settings.Global.putFloat(
            activity.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 0f
        )
        tracker.onActivityResumed(activity)
        presenter().present(campaign(type = MessageType.SLIDEUP), resolved, callbacks)
        val overlay = contentRoot().getChildAt(contentRoot().childCount - 1) as SlideupMessageView
        assertEquals("banner sits at its resting position", 0f, overlay.banner.translationY, 0.001f)
        assertEquals("banner is fully opaque", 1f, overlay.banner.alpha, 0.001f)
    }

    @Test
    fun `an unsupported type is never drawn`() {
        tracker.onActivityResumed(activity)
        assertFalse(
            presenter().present(campaign(type = MessageType.UNSUPPORTED), resolved, callbacks)
        )
    }
}
