package com.gameball.gameball.push

import android.content.Intent
import android.util.Log
import com.gameball.gameball.utils.Constants.TAG

/**
 * Runs the same detection on any intent that may carry a Gameball push, and calls [onClick] for
 * a tap or [onDismiss] for a swipe-away. Callers decide what each one does.
 */
internal fun detectPush(intent: Intent?, onClick: (GameballPush) -> Unit, onDismiss: (GameballPush) -> Unit) {
    // Reopening from Recents re-delivers the original launch intent with this flag set
    if (intent == null || (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) return
    val push = GameballPush.from(intent.extras) ?: return
    // Runs inside the host's lifecycle callbacks and a receiver, where a throw would crash the app
    try {
        if (intent.action == PushDismissReceiver.ACTION_DISMISSED) onDismiss(push) else onClick(push)
    } catch (t: Throwable) {
        Log.e(TAG, "could not handle a Gameball push", t)
    }
}
