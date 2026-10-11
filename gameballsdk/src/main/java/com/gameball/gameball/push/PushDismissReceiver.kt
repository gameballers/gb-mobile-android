package com.gameball.gameball.push

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import com.gameball.gameball.utils.Constants.TAG
import java.util.concurrent.atomic.AtomicInteger

/** Receives the delete intent of Gameball pushes the SDK shows: the user swiped one away or tapped "Clear all". */
internal class PushDismissReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // No backend endpoint for dismissals yet; the report call goes here once it exists
        Log.d(TAG, "Gameball push dismissed")
    }

    companion object {
        private val requestCodes = AtomicInteger(SystemClock.elapsedRealtime().toInt())

        fun pendingIntent(context: Context, extras: Bundle): PendingIntent {
            val intent = Intent(context, PushDismissReceiver::class.java).putExtras(extras)
            val flags = PendingIntent.FLAG_ONE_SHOT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
            // Unique per notification, or Android hands back an older PendingIntent carrying the older push's extras
            return PendingIntent.getBroadcast(context, requestCodes.incrementAndGet(), intent, flags)
        }
    }
}
