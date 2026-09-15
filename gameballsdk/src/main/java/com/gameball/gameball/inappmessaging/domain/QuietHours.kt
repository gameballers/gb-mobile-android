package com.gameball.gameball.inappmessaging.domain

import com.gameball.gameball.inappmessaging.runtime.IamLog

/**
 * A global suppression window, sent at the sync response root as
 * {enabled, start, end} alongside cooldownSeconds. No campaign carries its own.
 *
 * The times are UTC — confirmed with the backend team. The strings carry no zone and the
 * obvious reading, the customer's local wall clock, is wrong: at UTC+3 the two
 * interpretations disagree for six hours of every day, in both directions.
 *
 * The window is half-open (the start minute is inside, the end minute is not) and it wraps
 * midnight, so 22:00 -> 08:00 is two ranges rather than one.
 */
internal data class QuietHours(
    val startMinute: Int,
    val endMinute: Int
) {

    fun contains(nowMillis: Long): Boolean {
        val minute = minuteOfDayUtc(nowMillis)
        return if (startMinute < endMinute) {
            minute >= startMinute && minute < endMinute
        } else {
            // wraps midnight
            minute >= startMinute || minute < endMinute
        }
    }

    companion object {

        private const val MINUTES_PER_DAY = 1440L

        /** Minute of day in UTC. Math.floorMod is API 24, hence the manual wrap. */
        fun minuteOfDayUtc(millis: Long): Int {
            var minutes = (millis / 60_000L) % MINUTES_PER_DAY
            if (minutes < 0) minutes += MINUTES_PER_DAY
            return minutes.toInt()
        }

        /** Returns null — meaning "no window" — for every unusable input, logging which. */
        fun from(enabled: Boolean?, start: String?, end: String?): QuietHours? {
            if (enabled != true) {
                IamLog.d("quiet hours absent or not enabled; no window")
                return null
            }
            val startMinute = parseMinutes(start)
            val endMinute = parseMinutes(end)
            if (startMinute == null || endMinute == null) {
                IamLog.w("quiet hours malformed (start='$start', end='$end'); no window")
                return null
            }
            if (startMinute == endMinute) {
                IamLog.w(
                    "quiet hours refused: start == end ('$start'). Zero-length and " +
                        "twenty-four-hours are indistinguishable on the wire; no window"
                )
                return null
            }
            return QuietHours(startMinute, endMinute)
        }

        /**
         * Parses "HH:mm", tolerating "HH:mm:ss". Split and toIntOrNull rather than a date
         * formatter, which would emit Arabic-Indic digits on an Arabic-locale device.
         */
        private fun parseMinutes(value: String?): Int? {
            val parts = value?.trim()?.split(":") ?: return null
            if (parts.size < 2) return null
            val hours = parts[0].toIntOrNull() ?: return null
            val minutes = parts[1].toIntOrNull() ?: return null
            if (hours !in 0..23 || minutes !in 0..59) return null
            return hours * 60 + minutes
        }
    }
}
