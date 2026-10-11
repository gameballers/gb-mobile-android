package com.gameball.gameball.push

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Process
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.gameball.gameball.utils.Constants
import com.gameball.gameball.utils.Constants.TAG
import com.google.firebase.messaging.CommonNotificationBuilder
import com.google.firebase.messaging.NotificationParams

/** Shows Gameball pushes the way FCM would, plus a delete intent, for the host's messaging service. */
internal object PushNotificationDisplay {
    private const val RECEIVE_ACTION = "com.google.android.c2dm.intent.RECEIVE"
    private const val MESSAGE_ID_KEY = "google.message_id"
    // Skipping super skips FCM's own duplicate check, so the last few shown message ids are kept here
    private val shownMessageIds = ArrayDeque<String>()

    // True when the push was shown here or dropped as a duplicate; false hands it to FCM unchanged
    fun handle(context: Context, intent: Intent): Boolean {
        val extras = intent.extras
        if (intent.action != RECEIVE_ACTION || extras == null ||
            !extras.getString(Constants.IS_GB_KEY).equals("true", ignoreCase = true) || isAppForeground(context)
        ) return false
        return try {
            if (!NotificationParams.isNotification(extras)) return false
            if (!alreadyShown(extras.getString(MESSAGE_ID_KEY))) show(context, extras)
            true
        } catch (e: Throwable) {
            // FCM internals changed: hand the push back to FCM; only dismiss tracking is lost
            Log.w(TAG, "Couldn't show Gameball push, handing it to FCM: ${e.message}")
            false
        }
    }

    // Same check FCM makes before showing a push itself: phone locked, or app not on screen
    private fun isAppForeground(context: Context): Boolean {
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (keyguard.isKeyguardLocked) return false
        val pid = Process.myPid()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return activityManager.runningAppProcesses?.any {
            it.pid == pid && it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        } == true
    }

    @SuppressLint("MissingPermission") // guarded by areNotificationsEnabled
    private fun show(context: Context, extras: Bundle) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val params = NotificationParams(extras)
        // Same lookup FCM does for the app's default notification icon, color and channel
        @Suppress("DEPRECATION")
        val metadata = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA).metaData ?: Bundle.EMPTY
        val channelId = CommonNotificationBuilder.getOrCreateChannel(context, params.notificationChannelId, metadata)
        val info = CommonNotificationBuilder.createNotificationInfo(context, context, params, channelId, metadata)
        info.notificationBuilder.setDeleteIntent(PushDismissReceiver.pendingIntent(context, extras))
        manager.notify(info.tag, info.id, info.notificationBuilder.build())
    }

    private fun alreadyShown(messageId: String?): Boolean = synchronized(shownMessageIds) {
        if (messageId == null) return false
        if (messageId in shownMessageIds) return true
        if (shownMessageIds.size == 10) shownMessageIds.removeFirst()
        shownMessageIds.addLast(messageId)
        false
    }
}
