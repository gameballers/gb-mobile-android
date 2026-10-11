package com.gameball.gameball.push

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.core.app.OnNewIntentProvider
import com.gameball.gameball.utils.Constants

/** Detects taps on Gameball push notifications from each activity's launch intent, with no code in the host app. */
internal object PushOpenCapture {
    private val pendingTokens = mutableListOf<String>()
    private var reporter: ((String) -> Unit)? = null
    private var registered = false

    @Synchronized
    fun register(app: Application) {
        // init can run more than once, and Android would call a duplicate registration twice
        if (registered) return
        registered = true
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                // Saved state means a rotation or process restore re-delivering a tap that was already counted
                if (savedInstanceState == null) capture(activity.intent)
                // Warm taps on singleTop/singleTask activities arrive in onNewIntent instead of a new activity
                (activity as? OnNewIntentProvider)?.addOnNewIntentListener { capture(it) }
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    /** Sends captured click tokens to [report]; taps captured before the SDK's init are sent now. */
    @Synchronized
    fun setReporter(report: (String) -> Unit) {
        reporter = report
        pendingTokens.forEach(report)
        pendingTokens.clear()
    }

    private fun capture(intent: Intent?) {
        // Reopening from Recents re-delivers the original launch intent with this flag set
        if (intent == null || (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) return
        val token = try {
            val extras = intent.extras ?: return
            if (!extras.getString(Constants.IS_GB_KEY).equals("true", ignoreCase = true)) return
            extras.getString(Constants.PUSH_CLICK_TOKEN_KEY)
        } catch (e: RuntimeException) {
            // Launch intents can come from any app; unreadable extras are skipped instead of crashing the host
            return
        }
        if (!token.isNullOrBlank()) onClick(token)
    }

    @Synchronized
    private fun onClick(token: String) {
        val report = reporter
        if (report != null) report(token) else pendingTokens.add(token)
    }
}
