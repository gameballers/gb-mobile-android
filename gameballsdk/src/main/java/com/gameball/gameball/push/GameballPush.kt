package com.gameball.gameball.push

import android.os.Bundle
import com.gameball.gameball.utils.Constants

/**
 * A push Gameball's backend sent, read the same way wherever it shows up: a launch intent, a
 * delete intent, FCM's receive intent, or a data map an app passes in. Anything without
 * `isGB = "true"` is not one of ours and never becomes a GameballPush.
 */
internal class GameballPush private constructor(val payload: Map<String, String>) {

    /** The token the backend issued for this push; null when it carries none. */
    val clickToken: String?
        get() = payload[Constants.PUSH_CLICK_TOKEN_KEY]?.takeIf { it.isNotBlank() }

    /** The same tap seen twice, e.g. through a splash screen that passes the extras on, gives the same key. */
    val key: String?
        get() = clickToken ?: payload[MESSAGE_ID_KEY]

    companion object {
        private const val MESSAGE_ID_KEY = "google.message_id"

        fun from(data: Map<String, String>?): GameballPush? =
            data?.takeIf { it[Constants.IS_GB_KEY].equals("true", ignoreCase = true) }?.let { GameballPush(it) }

        /** Null for anything that isn't a readable Gameball push. Never throws. */
        @Suppress("DEPRECATION") // Bundle.get: FCM's extras mix the backend's strings with its own longs and ints
        fun from(extras: Bundle?): GameballPush? = try {
            from(extras?.keySet()?.mapNotNull { key -> (extras.get(key) as? String)?.let { key to it } }?.toMap())
        } catch (e: RuntimeException) {
            // Launch intents can come from any app; extras that can't be unpacked aren't ours
            null
        }
    }
}
