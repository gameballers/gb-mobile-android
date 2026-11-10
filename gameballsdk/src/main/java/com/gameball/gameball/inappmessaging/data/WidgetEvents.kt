package com.gameball.gameball.inappmessaging.data

import com.google.gson.JsonObject
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
    private const val GAME_READY = "gameReady"

    /** True when [raw] is the widget saying the game was played to its end. Never throws. */
    fun isGameCompleted(raw: String?): Boolean =
        parse(raw)?.str("type").equals(GAME_COMPLETED, ignoreCase = true)

    /**
     * The widget's word on the page it opened, from its `gameReady` event; null for any other
     * event. Never throws.
     *
     * Only an explicit `playable: false` holds the game back, as on the other SDKs: the page is
     * then the widget's "already played today", "not available" or error screen, which is all the
     * customer would see.
     */
    fun readinessOf(raw: String?): GameReadiness? {
        val root = parse(raw) ?: return null
        if (!root.str("type").equals(GAME_READY, ignoreCase = true)) return null
        val metadata = root.obj("metadata")
        val playable = metadata?.child("playable")
        val saysNo = playable != null && playable.isJsonPrimitive &&
            playable.asJsonPrimitive.isBoolean && !playable.asBoolean
        if (!saysNo) return GameReadiness.Playable
        return GameReadiness.Unplayable(metadata?.str("reason") ?: "no reason given")
    }

    private fun parse(raw: String?): JsonObject? {
        if (raw.isNullOrBlank()) return null
        // The instance method for the same reason as in MessageParser: parseString is Gson 2.8.6+.
        @Suppress("DEPRECATION")
        val root = try {
            JsonParser().parse(raw)
        } catch (t: Throwable) {
            return null
        }
        return if (root != null && root.isJsonObject) root.asJsonObject else null
    }
}

/** What the widget's `gameReady` event says about the page it opened. */
internal sealed class GameReadiness {
    object Playable : GameReadiness()
    data class Unplayable(val reason: String) : GameReadiness()
}
