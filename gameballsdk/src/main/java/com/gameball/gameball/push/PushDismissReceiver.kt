package com.gameball.gameball.push

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import com.gameball.gameball.GameballApp
import java.util.concurrent.atomic.AtomicInteger

/** Gets the delete intent of a Gameball push the SDK showed: the user swiped it away or tapped "Clear all". */
internal class PushDismissReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // The same entry point as taps; the action below is how detectPush tells a swipe-away from a tap.
        // If Android started the process for this, Application.onCreate (and so init) ran first.
        GameballApp.getInstance(context).handlePushClick(intent)
    }

    companion object {
        const val ACTION_DISMISSED = "com.gameball.gameball.push.DISMISSED"

        private val requestCodes = AtomicInteger(SystemClock.elapsedRealtime().toInt())

        fun pendingIntent(context: Context, extras: Bundle): PendingIntent {
            val intent = Intent(context, PushDismissReceiver::class.java)
                .setAction(ACTION_DISMISSED)
                .putExtras(extras)
            // Unique per notification, or Android hands back an older PendingIntent carrying the older push's extras
            return PendingIntent.getBroadcast(
                context, requestCodes.incrementAndGet(), intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
