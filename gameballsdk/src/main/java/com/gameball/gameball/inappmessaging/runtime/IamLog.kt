package com.gameball.gameball.inappmessaging.runtime

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log

/**
 * Developer-facing diagnostics for the in-app messaging module.
 *
 * Deliberately separate from [com.gameball.gameball.logging.GameballLogger], which posts
 * telemetry to the Gameball backend. Nothing here leaves the device.
 *
 * Filter with: adb logcat -s GameballIAM
 */
internal object IamLog {
    private const val TAG = "GameballIAM"

    /** Set by the host. Null means "follow the host app's build type". */
    @Volatile
    private var hostOverride: Boolean? = null

    @Volatile
    private var hostIsDebuggable: Boolean = false

    /**
     * Whether the module writes diagnostics to logcat.
     *
     * Follows the host app's `android:debuggable` flag by default: on while an integrator is
     * building, silent in the release build their customers install, where these lines reach
     * every end user's device log and say nothing that user or the app can act on. Assigning
     * it overrides that for the life of the process.
     */
    @JvmStatic
    var enabled: Boolean
        get() = hostOverride ?: hostIsDebuggable
        set(value) {
            hostOverride = value
        }

    /** Reads the host's build type. Called once, when messaging starts. */
    @JvmStatic
    fun resolveDefaultFrom(context: Context) {
        hostIsDebuggable =
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    fun d(message: String) { if (enabled) Log.d(TAG, message) }

    fun w(message: String) { if (enabled) Log.w(TAG, message) }

    fun e(message: String, throwable: Throwable? = null) {
        if (!enabled) return
        if (throwable != null) Log.e(TAG, message, throwable) else Log.e(TAG, message)
    }
}
