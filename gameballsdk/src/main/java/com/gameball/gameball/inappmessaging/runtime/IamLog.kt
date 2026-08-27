package com.gameball.gameball.inappmessaging.runtime

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

    /** Host apps can silence the module's logs; on by default so integrators can diagnose. */
    @Volatile
    @JvmStatic
    var enabled: Boolean = true

    fun d(message: String) { if (enabled) Log.d(TAG, message) }

    fun w(message: String) { if (enabled) Log.w(TAG, message) }

    fun e(message: String, throwable: Throwable? = null) {
        if (!enabled) return
        if (throwable != null) Log.e(TAG, message, throwable) else Log.e(TAG, message)
    }
}
