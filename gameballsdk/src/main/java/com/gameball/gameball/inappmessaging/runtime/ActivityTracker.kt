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
