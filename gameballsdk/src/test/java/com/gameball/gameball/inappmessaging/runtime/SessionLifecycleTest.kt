package com.gameball.gameball.inappmessaging.runtime

import android.app.Activity
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionLifecycleTest {

    private var foregrounded = 0
    private var backgrounded = 0
    private var surfaceAvailable = 0
    private lateinit var tracker: ActivityTracker

    private val recorder = object : ActivityTracker.Callbacks {
        override fun onAppForegrounded() { foregrounded++ }
        override fun onAppBackgrounded() { backgrounded++ }
        override fun onSurfaceAvailable() { surfaceAvailable++ }
    }

    @Before
    fun setUp() {
        tracker = ActivityTracker(
            ApplicationProvider.getApplicationContext<Application>(), recorder
        )
    }

    private fun activity(): Activity = Robolectric.buildActivity(Activity::class.java).get()

    // --- foreground / background counting ---

    @Test
    fun `a cold start foregrounds exactly once`() {
        tracker.onActivityStarted(activity())
        assertEquals(1, foregrounded)
        assertEquals(0, backgrounded)
    }

    /** Android-only: onActivityStopped fires on every screen transition inside the host. */
    @Test
    fun `navigating between activities does not background the app`() {
        val a = activity()
        val b = activity()
        tracker.onActivityStarted(a)
        tracker.onActivityStarted(b)   // B starts before A stops
        tracker.onActivityStopped(a)

        assertEquals(1, foregrounded)
        assertEquals("a screen transition is not a session boundary", 0, backgrounded)
    }

    /** Rotation destroys and recreates the Activity in exactly this order. */
    @Test
    fun `rotation does not background the app`() {
        val before = activity()
        val after = activity()
        tracker.onActivityStarted(before)
        tracker.onActivityStarted(after)
        tracker.onActivityStopped(before)

        assertEquals(0, backgrounded)
    }

    @Test
    fun `leaving the app backgrounds it exactly once`() {
        val a = activity()
        tracker.onActivityStarted(a)
        tracker.onActivityStopped(a)
        assertEquals(1, backgrounded)
    }

    @Test
    fun `returning foregrounds it again`() {
        val a = activity()
        tracker.onActivityStarted(a)
        tracker.onActivityStopped(a)
        tracker.onActivityStarted(a)
        assertEquals(2, foregrounded)
        assertEquals(1, backgrounded)
    }

    // --- the Activity handle ---

    @Test
    fun `the current activity is tracked on resume and cleared on pause`() {
        val a = activity()
        assertNull(tracker.currentActivity)
        tracker.onActivityResumed(a)
        assertSame(a, tracker.currentActivity)
        tracker.onActivityPaused(a)
        assertNull(tracker.currentActivity)
    }

    @Test
    fun `pausing a different activity does not clear the current one`() {
        val a = activity()
        val b = activity()
        tracker.onActivityResumed(a)
        tracker.onActivityPaused(b)
        assertSame("pausing B must not blank the handle to A", a, tracker.currentActivity)
    }

    @Test
    fun `resuming an activity is a retry trigger`() {
        tracker.onActivityResumed(activity())
        assertEquals(1, surfaceAvailable)
    }

    @Test
    fun `unregister clears the handle and the counter`() {
        val a = activity()
        tracker.register()
        tracker.onActivityStarted(a)
        tracker.onActivityResumed(a)
        tracker.unregister()
        assertNull(tracker.currentActivity)
    }

    // --- the pause stamp ---

    private class MutableClock(var nowMillis: Long) : Clock {
        override fun nowMillis(): Long = nowMillis
    }

    @Test
    fun `a resume inside the timeout does not start a new session`() {
        val clock = MutableClock(0)
        val session = SessionState(clock, sessionTimeoutMillis = 30_000L)
        session.onBackgrounded()
        clock.nowMillis = 29_000L
        assertFalse(session.onForegrounded())
    }

    @Test
    fun `a resume beyond the timeout starts a new session`() {
        val clock = MutableClock(0)
        val session = SessionState(clock, sessionTimeoutMillis = 30_000L)
        session.onBackgrounded()
        clock.nowMillis = 30_001L
        assertTrue(session.onForegrounded())
    }

    /**
     * Defect 1, in its Android form. Several "backgrounded" notifications can arrive before
     * one resume; if the last stamp won, the measured absence here would be 20s and the
     * session would be missed entirely.
     */
    @Test
    fun `several backgrounded notifications before one resume still measure the full absence`() {
        val clock = MutableClock(0)
        val session = SessionState(clock, sessionTimeoutMillis = 30_000L)

        session.onBackgrounded()          // t = 0, the real departure
        clock.nowMillis = 20_000L
        session.onBackgrounded()          // a second notification, 20s later
        clock.nowMillis = 40_000L

        assertTrue("the absence is 40s, not 20s", session.onForegrounded())
    }

    @Test
    fun `foregrounding without a prior background is not a new session`() {
        val clock = MutableClock(0)
        val session = SessionState(clock, sessionTimeoutMillis = 30_000L)
        assertFalse(session.onForegrounded())
    }

    @Test
    fun `the stamp is consumed, so a second foreground does not re-fire`() {
        val clock = MutableClock(0)
        val session = SessionState(clock, sessionTimeoutMillis = 30_000L)
        session.onBackgrounded()
        clock.nowMillis = 60_000L
        assertTrue(session.onForegrounded())
        assertFalse(session.onForegrounded())
    }

    @Test
    fun `reset clears a pending stamp`() {
        val clock = MutableClock(0)
        val session = SessionState(clock, sessionTimeoutMillis = 30_000L)
        session.onBackgrounded()
        session.reset()
        clock.nowMillis = 60_000L
        assertFalse(session.onForegrounded())
    }
}
