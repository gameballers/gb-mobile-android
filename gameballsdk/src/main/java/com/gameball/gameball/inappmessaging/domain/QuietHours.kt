package com.gameball.gameball.inappmessaging.domain

import com.gameball.gameball.inappmessaging.runtime.IamLog
import java.util.TimeZone

/**
 * A global suppression window, sent at the sync response root as
 * {enabled, start, end} alongside cooldownSeconds. No campaign carries its own.
 *
 * The times are on the device's local clock, never UTC — the sync contract says so in as many
 * words. The strings carry no zone, and at UTC+3 the two readings disagree for six hours of
 * every day, in both directions.
 *
 * The window is half-open (the start minute is inside, the end minute is not) and it wraps
 * midnight, so 22:00 -> 08:00 is two ranges rather than one.
 */
internal data class QuietHours(
    val startMinute: Int,
    val endMinute: Int
) {

    /** [timeZone] is the device's own unless a test pins one. */
    fun contains(nowMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): Boolean {
        val minute = minuteOfDay(nowMillis, timeZone)
        return if (startMinute < endMinute) {
            minute >= startMinute && minute < endMinute
        } else {
            // wraps midnight
            minute >= startMinute || minute < endMinute
        }
    }

    companion object {

        private const val MINUTES_PER_DAY = 1440L

        /**
         * Minute of day on [timeZone]'s wall clock, daylight saving included: the offset is the
         * one in force at [millis]. The manual wrap keeps an instant before 1970 in range.
         */
        fun minuteOfDay(millis: Long, timeZone: TimeZone): Int {
            val local = millis + timeZone.getOffset(millis)
            var minutes = (local / 60_000L) % MINUTES_PER_DAY
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
