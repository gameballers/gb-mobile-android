package com.gameball.gameball.push

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.gameball.gameball.utils.Constants.TAG
import com.google.firebase.messaging.CommonNotificationBuilder
import com.google.firebase.messaging.NotificationParams

/**
 * Shows a Gameball push the way FCM would, plus a delete intent, for apps that opt in through
 * GameballApp.handlePushIntent. Only for pushes FCM would show itself: while the app is on
 * screen, FCM hands pushes to onMessageReceived and this leaves them alone.
 */
internal object PushNotificationDisplay {
    private const val MESSAGE_ID_KEY = "google.message_id"
    private const val DEFAULT_CHANNEL_KEY = "com.google.firebase.messaging.default_notification_channel_id"
    private const val FALLBACK_CHANNEL_ID = "fcm_fallback_notification_channel"
    private const val FALLBACK_CHANNEL_LABEL = "fcm_fallback_notification_channel_label"

    // Skipping FCM's own handling skips its duplicate check, so the last few shown message IDs are kept here
    private val shownMessageIds = ArrayDeque<String>()

    /** True when the push was shown here or dropped as a duplicate; false hands it to FCM unchanged. */
    fun handle(context: Context, extras: Bundle): Boolean {
        if (isAppForeground(context)) return false
        return try {
            if (!NotificationParams.isNotification(extras)) return false
            if (!alreadyShown(extras.getString(MESSAGE_ID_KEY))) show(context, extras)
            true
        } catch (t: Throwable) {
            // FCM's internals changed: hand the push back to FCM, so only dismiss tracking is lost
            Log.w(TAG, "could not show a Gameball push, handing it to FCM", t)
            false
        }
    }

    // The same check FCM makes before showing a push itself: phone locked, or app not on screen
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
        // The app's FCM settings (default icon, color and channel) live in its manifest metadata
        @Suppress("DEPRECATION")
        val metadata = context.packageManager
            .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA).metaData ?: Bundle.EMPTY
        val channelId = channelFor(context, params.notificationChannelId, metadata)
        val info = CommonNotificationBuilder.createNotificationInfo(context, context, params, channelId, metadata)
        info.notificationBuilder.setDeleteIntent(PushDismissReceiver.pendingIntent(context, extras))
        manager.notify(info.tag, info.id, info.notificationBuilder.build())
    }

    /**
     * FCM's own order, so the push lands in the channel FCM would have used: the push's channel if
     * the app created it, then the manifest default, then FCM's fallback channel with FCM's own ID
     * and label. FCM's getOrCreateChannel does the same, but FCM marks it @VisibleForTesting.
     */
    private fun channelFor(context: Context, requested: String?, metadata: Bundle): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null // no channels before Android 8
        val manager = context.getSystemService(NotificationManager::class.java)
        listOf(requested, metadata.getString(DEFAULT_CHANNEL_KEY))
            .firstOrNull { !it.isNullOrEmpty() && manager.getNotificationChannel(it) != null }
            ?.let { return it }
        if (manager.getNotificationChannel(FALLBACK_CHANNEL_ID) == null) {
            @SuppressLint("DiscouragedApi") // FCM looks its label up the same way
            val labelId = context.resources.getIdentifier(FALLBACK_CHANNEL_LABEL, "string", context.packageName)
            val label = if (labelId != 0) context.getString(labelId) else "Misc"
            manager.createNotificationChannel(
                NotificationChannel(FALLBACK_CHANNEL_ID, label, NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        return FALLBACK_CHANNEL_ID
    }

    private fun alreadyShown(messageId: String?): Boolean = synchronized(shownMessageIds) {
        if (messageId == null) return false
        if (messageId in shownMessageIds) return true
        if (shownMessageIds.size == 10) shownMessageIds.removeFirst()
        shownMessageIds.addLast(messageId)
        false
    }
}
