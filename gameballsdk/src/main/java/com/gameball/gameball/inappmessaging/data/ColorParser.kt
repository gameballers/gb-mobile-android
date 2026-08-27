package com.gameball.gameball.inappmessaging.data

import com.gameball.gameball.inappmessaging.runtime.IamLog

/**
 * Parses every colour the wire can carry: #RRGGBB, #AARRGGBB, either without the hash, and a
 * raw packed 32-bit integer.
 *
 * The wire is ARGB and Android's Color is ARGB, so there is no channel shuffling. Alpha is
 * honoured everywhere, including on the message background — a campaign may deliberately make
 * a modal card translucent.
 *
 * Anything else logs and returns null, which means that one slot falls back to the host theme
 * while the rest of the message renders normally. A malformed colour never costs the customer
 * the message.
 */
internal object ColorParser {

    fun parse(value: Any?): Int? = when (value) {
        null -> null
        is Int -> value
        is Long -> value.toInt()
        is Number -> value.toInt()
        is String -> parseString(value)
        else -> {
            IamLog.w("ignoring malformed colour of type ${value.javaClass.simpleName}: $value")
            null
        }
    }

    private fun parseString(raw: String): Int? {
        var hex = raw.trim().removePrefix("#")
        if (hex.length == 6) hex = "FF$hex"
        if (hex.length != 8) {
            IamLog.w("ignoring malformed colour: '$raw'")
            return null
        }
        val parsed = hex.toLongOrNull(16)
        if (parsed == null) {
            IamLog.w("ignoring malformed colour: '$raw'")
            return null
        }
        return parsed.toInt()
    }
}
