package com.gameball.gameball.inappmessaging.data

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * ISO-8601 UTC formatting and lenient parsing for the wire.
 *
 * Every formatter here pins [Locale.US]. On an Arabic-locale device a default-locale
 * SimpleDateFormat emits Arabic-Indic digits, which is neither valid ISO-8601 nor a valid
 * integer, and the whole analytics batch 400s.
 *
 * SimpleDateFormat is not thread-safe, hence the ThreadLocal.
 */
internal object IamTime {

    private const val EMIT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

    /**
     * The ISO 'XXX' offset pattern is API 24+, so offsets are normalised to the RFC-822 'Z'
     * form ("+0000") before parsing. Order matters: most specific first, because
     * SimpleDateFormat happily parses a prefix and would read "2026-08-27T10:30:00" as a bare
     * date under "yyyy-MM-dd".
     */
    private val PARSE_PATTERNS = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd"
    )

    private val OFFSET_WITH_COLON = Regex("""([+-]\d{2}):(\d{2})$""")

    private val emitter = ThreadLocal.withInitial {
        SimpleDateFormat(EMIT_PATTERN, Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    fun toIso8601Utc(millis: Long): String = emitter.get()!!.format(Date(millis))

    /** Returns null for anything unparseable. Never throws. */
    fun parseIso8601(value: String?): Long? {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isEmpty()) return null

        var normalised = trimmed
        if (normalised.endsWith("Z") || normalised.endsWith("z")) {
            normalised = normalised.dropLast(1) + "+0000"
        }
        normalised = OFFSET_WITH_COLON.replace(normalised) { m ->
            m.groupValues[1] + m.groupValues[2]
        }

        for (pattern in PARSE_PATTERNS) {
            val format = SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val position = ParsePosition(0)
            val parsed = format.parse(normalised, position)
            // Require the whole string to be consumed, or "2026-08-27garbage" would parse.
            if (parsed != null && position.index == normalised.length) return parsed.time
        }
        return null
    }
}
