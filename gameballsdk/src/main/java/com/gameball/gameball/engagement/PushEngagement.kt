package com.gameball.gameball.engagement

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * What the customer did with a Gameball push, as the backend's push-engagement endpoint counts it.
 * The numbers are the backend's own (PushEngagementType in g-backend-v2) and must never change.
 */
internal enum class PushEngagementType(val value: Int) {
    /** Tapped the notification or one of its action buttons. */
    CLICKED(1),

    /** Swiped it away, or tapped a "Later" / "No thanks" button. */
    DISMISSED(2),

    /** Didn't tap, but opened the app on their own soon after receiving it. */
    INFLUENCED_OPEN(3)
}

/**
 * The body of one push-engagement report. [eventUid] is new for each event, so the backend never
 * counts one event twice; a retry of the same event must send the same body again.
 */
internal fun pushEngagementBody(
    clickToken: String,
    type: PushEngagementType,
    occurredAt: Long,
    eventUid: String = UUID.randomUUID().toString()
): Map<String, Any> = mapOf(
    "clickToken" to clickToken,
    "engagementType" to type.value,
    "eventUid" to eventUid,
    "occurredAt" to isoUtc(occurredAt)
)

// The backend reads occurredAt as UTC. SimpleDateFormat isn't thread-safe, so one per call.
private fun isoUtc(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date(millis))
