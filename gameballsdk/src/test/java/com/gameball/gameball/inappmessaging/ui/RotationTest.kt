package com.gameball.gameball.inappmessaging.ui

import android.app.Activity
import android.app.Application
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
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
import com.gameball.gameball.inappmessaging.domain.WidgetContent
import com.gameball.gameball.inappmessaging.runtime.ActivityTracker
import com.gameball.gameball.inappmessaging.runtime.Clock
import com.gameball.gameball.inappmessaging.runtime.PresentationCallbacks
import com.gameball.gameball.inappmessaging.runtime.ResolvedMessage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Tracker C5 on Android: rotating recreates the Activity, and a message or game drawn into it went
 * with it. The presenter still held the dead view, so it went on reporting a message on screen and
 * every later one was deferred until the app was restarted.
 *
 * Driven by a real recreation, with the tracker hearing the real lifecycle and wired to the
 * presenter as GameballInAppMessaging wires it.
 */
@RunWith(RobolectricTestRunner::class)
class RotationTest {

    private var shown = 0
    private var dismissed = 0
    private var failed = 0

    private val callbacks = object : PresentationCallbacks {
        override fun onShown() { shown++ }
        override fun onTapped(button: MessageButton?) = Unit
        override fun onDismissed() { dismissed++ }
        override fun onFailed() { failed++ }
    }

    private lateinit var tracker: ActivityTracker
    private lateinit var presenter: OverlayPresenter

    /** Off for the test that looks at the moment between the old screen and the new one. */
    private var resumeOnSurface = true

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        IamImageLoader.init(app)
        tracker = ActivityTracker(app, object : ActivityTracker.Callbacks {
            override fun onAppForegrounded() = Unit
            override fun onAppBackgrounded() = Unit
            override fun onSurfaceAvailable() {
                if (resumeOnSurface) presenter.resumeAfterHostLoss()
            }
            override fun onActivityDestroyed(activity: Activity) = presenter.onHostDestroyed(activity)
        })
        presenter = OverlayPresenter(tracker, Clock { 1_000L }) { openDetail ->
            "https://m.gameball.app/?openDetail=$openDetail"
        }
        tracker.register()
    }

    @After
    fun tearDown() = tracker.unregister()

    private fun message(type: MessageType = MessageType.MODAL) = Campaign(
        campaignId = 1, variationId = null, dispatchId = null, name = "c",
        priority = 0, messageType = type, rawMessageType = type.wire,
        expiresAtMillis = null, isTest = false,
        trigger = Trigger(TriggerType.SESSION_START),
        content = MessageContent(
            header = "Hello", body = "Body", imageUrl = null, iconUrl = null,
            layout = MessageLayout.DEFAULT, colors = MessageColors(background = 0xFFFFFFFF.toInt()),
            buttons = emptyList(), clickAction = null, showCloseButton = true,
            dismissOnScrimTap = true, slidePosition = SlidePosition.BOTTOM,
            orientation = MessageOrientation.ANY, autoDismissMillis = null,
            headerAlign = null, bodyAlign = null, extras = emptyMap()
        ),
        responseIndex = 0
    )

    private fun game(type: MessageType = MessageType.FULLSCREEN) = Campaign(
        campaignId = 0, variationId = null, dispatchId = null, name = "Spin the Wheel",
        priority = 0, messageType = type, rawMessageType = type.wire,
        expiresAtMillis = null, isTest = false,
        trigger = Trigger(TriggerType.SESSION_START),
        content = MessageContent.forGame(),
        responseIndex = 0,
        widget = WidgetContent(gameCampaignId = 9645, gameCampaignType = "wheel")
    )

    private val resolved = ResolvedMessage("Hello", "Body", emptyList())

    /** Drives a real layout pass so pre-draw listeners fire, exactly as a frame would. */
    private fun paint(activity: Activity) {
        val root = activity.window.decorView
        root.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )
        root.layout(0, 0, 1080, 1920)
        root.viewTreeObserver.dispatchOnPreDraw()
    }

    private fun messagesIn(activity: Activity): Int =
        activity.findViewById<ViewGroup>(android.R.id.content).childCount

    private fun gamesIn(activity: Activity): List<WidgetMessageView> {
        val decor = activity.window.decorView as ViewGroup
        return (0 until decor.childCount).mapNotNull { decor.getChildAt(it) as? WidgetMessageView }
    }

    @Test
    fun `a message comes back on the new screen after a rotation, counted once`() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val before = controller.get()
        assertTrue(presenter.present(message(), resolved, callbacks))
        paint(before)
        assertEquals(1, shown)

        controller.recreate()
        val after = controller.get()
        paint(after)

        assertNotSame(before, after)
        assertEquals("on the new screen", 1, messagesIn(after))
        assertTrue(presenter.isShowing)
        assertEquals("still one view of one message", 1, shown)
        assertEquals("the customer closed nothing", 0, dismissed)
    }

    /** The bug the tester hit: after a rotation nothing could open again until a restart. */
    @Test
    fun `a message brought back after a rotation still closes, and the next one can open`() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        presenter.present(message(), resolved, callbacks)
        paint(controller.get())

        controller.recreate()
        presenter.dismissCurrent()

        assertFalse(presenter.isShowing)
        assertTrue("the next message may draw", presenter.present(message(), resolved, callbacks))
    }

    @Test
    fun `while a message waits for its new screen nothing else is drawn in its place`() {
        resumeOnSurface = false
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        presenter.present(message(), resolved, callbacks)
        paint(controller.get())

        controller.recreate()

        assertTrue("still the message on screen, as far as anyone asking knows", presenter.isShowing)
        assertFalse(presenter.present(message(), resolved, callbacks))
        assertTrue(presenter.resumeAfterHostLoss())
        assertEquals(1, messagesIn(controller.get()))
    }

    @Test
    fun `a message whose screen closes for good gives way`() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        presenter.present(message(), resolved, callbacks)
        paint(controller.get())

        controller.pause().stop().destroy()

        assertFalse(presenter.isShowing)
        assertEquals("seen, then gone with its screen", 1, dismissed)
    }

    @Test
    fun `a game comes back on the new screen after a rotation, counted once`() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val before = controller.get()
        presenter.present(game(), resolved, callbacks)
        val first = gamesIn(before).single()
        first.widget.settle(ready = true)
        assertEquals(1, shown)

        controller.recreate()
        val second = gamesIn(controller.get()).single()
        second.widget.settle(ready = true)

        assertNotSame("drawn again on the new screen", first, second)
        assertEquals("one view of one game", 1, shown)
        assertTrue(presenter.isShowing)

        second.widget.closeRequested()
        assertFalse(presenter.isShowing)
        assertEquals(1, dismissed)
    }

    /** A game still loading when the screen turned: the old WebView's late word must not close the new one. */
    @Test
    fun `a game still loading when the screen rotates loads again on the new one`() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        presenter.present(game(MessageType.MODAL), resolved, callbacks)
        val first = gamesIn(controller.get()).single()

        controller.recreate()
        first.widget.settle(ready = false)

        assertEquals(0, failed)
        val second = gamesIn(controller.get()).single()
        second.widget.settle(ready = true)
        assertEquals(1, shown)
    }
}
