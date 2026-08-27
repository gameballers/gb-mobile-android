package com.gameball.gameball.inappmessaging.domain

/** What is known about one campaign's past displays. */
internal data class DisplayRecord(
    val lastDisplayAtMillis: Long,
    val count: Int
)

/**
 * An immutable read of the display history, passed into the pure selector.
 *
 * [lastDisplayAtMillis] is the most recent display from *any* campaign — the global cooldown
 * floor is not per campaign.
 */
internal data class DisplayHistorySnapshot(
    val perCampaign: Map<Int, DisplayRecord> = emptyMap(),
    val lastDisplayAtMillis: Long? = null
)
