package com.gameball.gameball.inappmessaging.runtime

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * Tracks the foreground session and the Activity the module may draw into.
 *
 * Registered on opt-in and unregistered on stop: until the host calls startInAppMessaging,
 * the SDK must register nothing at all.
 */
internal class ActivityTracker(
    private val application: Application,
    private val callbacks: Callbacks
) : Application.ActivityLifecycleCallbacks {

    internal interface Callbacks {
        fun onAppForegrounded()
        fun onAppBackgrounded()
        /** A retry trigger: an Activity became available, or the device rotated. */
        fun onSurfaceAvailable()
    }

    private var startedActivities = 0
    private var current: WeakReference<Activity>? = null
    private var registered = false

    /**
     * Always resolved fresh. A strong reference to an Activity from a process-lifetime
     * singleton is a textbook leak, and this module lives in GameballApp.
     */
    val currentActivity: Activity?
        get() = current?.get()

    fun register() {
        if (registered) return
        registered = true
        application.registerActivityLifecycleCallbacks(this)
        // Callbacks fire for future events only. If the calling Activity is already resumed - the
        // usual case when a host toggles startInAppMessaging from a toolbar - onActivityResumed
        // will not re-fire, so currentActivity would stay null until the next screen change and
        // the first campaign would be deferred as "no surface available". Peek at ActivityThread's
        // internal records to seed the current Activity if we can; the SDK self-heals from the
        // next real onActivityResumed even if the reflection fails.
        seedFromActivityThread()
    }

    /**
     * Reflection is kept narrow: one static method and two field reads on classes that have
     * carried the same shape since API 24. Failures are silent - the caller gets the same
     * behaviour it had before this seed existed. The window-attached guard excludes an
     * Activity that only reached onCreate: reflected records also list an Activity whose
     * window has not been added yet, and treating that one as current would give the
     * presenter a decor view with no attached content root.
     */
    private fun seedFromActivityThread() {
        val activity = try {
            val threadClass = Class.forName("android.app.ActivityThread")
            val thread = threadClass.getMethod("currentActivityThread").invoke(null) ?: return
            val activitiesField = threadClass.getDeclaredField("mActivities").apply {
                isAccessible = true
            }
            val activities = activitiesField.get(thread) as? Map<*, *> ?: return
            activities.values.asSequence().mapNotNull { record ->
                val cls = record?.javaClass ?: return@mapNotNull null
                val pausedField = cls.getDeclaredField("paused").apply { isAccessible = true }
                if (pausedField.getBoolean(record)) return@mapNotNull null
                val activityField = cls.getDeclaredField("activity").apply { isAccessible = true }
                activityField.get(record) as? Activity
            }.firstOrNull {
                !it.isFinishing && it.window?.decorView?.isAttachedToWindow == true
            }
        } catch (t: Throwable) {
            IamLog.d(
                "could not seed current Activity from ActivityThread; " +
                    "waiting for the next lifecycle callback (${t.javaClass.simpleName})"
            )
            null
        } ?: return
        current = WeakReference(activity)
        // startedActivities is a session-boundary counter, not a UI-presence one; leaving it at
        // zero here would make the tracker's first onActivityStopped fire onAppBackgrounded on an
        // Activity that was already foregrounded before we registered.
        startedActivities = 1
        callbacks.onSurfaceAvailable()
    }

    fun unregister() {
        if (!registered) return
        registered = false
        application.unregisterActivityLifecycleCallbacks(this)
        current = null
        startedActivities = 0
    }

    /**
     * Counting started Activities means a screen transition (start B, stop A) never dips to
     * zero, and neither does a rotation. Treating onActivityPaused/onActivityStopped as "the
     * app went to background" makes every navigation inside the host look like a new session.
     *
     * This is what androidx.lifecycle's ProcessLifecycleOwner does internally; it is five
     * lines, so the module does not take the dependency.
     */
    override fun onActivityStarted(activity: Activity) {
        if (startedActivities == 0) callbacks.onAppForegrounded()
        startedActivities++
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities--
        if (startedActivities <= 0) {
            startedActivities = 0
            callbacks.onAppBackgrounded()
        }
    }

    override fun onActivityResumed(activity: Activity) {
        current = WeakReference(activity)
        callbacks.onSurfaceAvailable()
    }

    override fun onActivityPaused(activity: Activity) {
        // Only clear it if this is still the Activity we hold: pausing a different one during
        // a transition must not blank the handle the presenter is about to use.
        if (current?.get() === activity) current = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
