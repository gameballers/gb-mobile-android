package com.gameball.gameball.inappmessaging.data

import com.google.gson.JsonParser

/**
 * Reads what an open game posts through `WidgetEvent.postEvent(...)`.
 *
 * Only the event's type is read. Its metadata names the game in the widget's own terms —
 * campaignType `spinTheWheel` where the sync says `wheel` — so it is never matched against the
 * sync: the event came from the WebView of the one game that is open, and that is the game it is
 * about.
 */
internal object WidgetEvents {

    private const val GAME_COMPLETED = "gameCompleted"

    /** True when [raw] is the widget saying the game was played to its end. Never throws. */
    fun isGameCompleted(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        // The instance method for the same reason as in MessageParser: parseString is Gson 2.8.6+.
        @Suppress("DEPRECATION")
        val root = try {
            JsonParser().parse(raw)
        } catch (t: Throwable) {
            return false
        }
        if (root == null || !root.isJsonObject) return false
        return root.asJsonObject.str("type").equals(GAME_COMPLETED, ignoreCase = true)
    }
}
