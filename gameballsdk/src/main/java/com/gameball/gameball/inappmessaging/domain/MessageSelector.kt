package com.gameball.gameball.inappmessaging.domain

import com.gameball.gameball.inappmessaging.runtime.IamLog

/**
 * Chooses at most one campaign for an occurrence.
 *
 * Pure by design: no I/O, no Context and no clock of its own, so every rule is testable with
 * plain data and no mocking. The service sequences; this owns all the policy.
 */
internal object MessageSelector {

    fun select(
        occurrence: TriggerOccurrence,
        campaigns: List<Campaign>,
        history: DisplayHistorySnapshot,
        nowMillis: Long,
        cooldownSeconds: Int,
        quietHours: QuietHours?,
        isArtworkReady: (Campaign) -> Boolean
    ): Campaign? {
        val eligible = campaigns.filter { campaign ->
            triggerMatches(campaign.trigger, occurrence) &&
                notExpired(campaign, nowMillis) &&
                repeatEligible(campaign, history, nowMillis) &&
                campaign.messageType.isSupported &&
                isArtworkReady(campaign)
        }
        if (eligible.isEmpty()) return null

        // Quiet hours suppress rather than defer: the pending slot is in-memory and a window
        // is hours long, so "retry when it ends" would never fire. This costs the occurrence,
        // not the campaign.
        if (quietHours != null && quietHours.contains(nowMillis)) {
            IamLog.d("inside quiet hours; suppressing ${eligible.size} eligible campaign(s)")
            return null
        }

        // The global floor, checked after eligibility and before sorting. Not per campaign.
        val lastDisplay = history.lastDisplayAtMillis
        if (lastDisplay != null && nowMillis - lastDisplay < cooldownSeconds * 1000L) {
            IamLog.d("inside the ${cooldownSeconds}s display floor; nothing selected")
            return null
        }

        val winner = eligible.sortedWith(RANKING).first()
        if (eligible.size > 1) {
            IamLog.d(
                "selected campaign ${winner.campaignId} (priority ${winner.priority}) from " +
                    "${eligible.size} eligible"
            )
        }
        return winner
    }

    /**
     * Highest priority first, then response order.
     *
     * The response-order tie-break is meaningful, not merely deterministic: the backend
     * returns campaigns in the sequence the marketer arranged in the dashboard, confirmed
     * with the backend team. Kotlin's sortedWith is stable, so a comparator on priority alone
     * would preserve input order — but the index is written in explicitly so that a future
     * refactor to "ascending campaignId" is visibly a behaviour change rather than a tidy-up.
     * Four tie-break tests in the Flutter SDK all passed under a comparator that broke this.
     */
    private val RANKING: Comparator<Campaign> =
        compareByDescending<Campaign> { it.priority }.thenBy { it.responseIndex }

    /**
     * The retry question. Deliberately not "has this ever displayed": asking the cruder
     * question threw away every repeatable campaign that happened to be waiting in the
     * pending slot.
     */
    fun mayDisplayNow(
        campaign: Campaign,
        history: DisplayHistorySnapshot,
        nowMillis: Long,
        cooldownSeconds: Int,
        quietHours: QuietHours?,
        isArtworkReady: (Campaign) -> Boolean
    ): Boolean {
        if (!notExpired(campaign, nowMillis)) return false
        if (!campaign.messageType.isSupported) return false
        if (!repeatEligible(campaign, history, nowMillis)) return false
        if (!isArtworkReady(campaign)) return false
        if (quietHours != null && quietHours.contains(nowMillis)) return false
        val lastDisplay = history.lastDisplayAtMillis
        if (lastDisplay != null && nowMillis - lastDisplay < cooldownSeconds * 1000L) return false
        return true
    }

    fun triggerMatches(trigger: Trigger, occurrence: TriggerOccurrence): Boolean =
        when (occurrence) {
            is TriggerOccurrence.SessionStart -> trigger.type == TriggerType.SESSION_START
            is TriggerOccurrence.Event ->
                trigger.type == TriggerType.EVENT &&
                    trigger.eventName == occurrence.name &&
                    FilterEvaluator.matches(trigger.filters, occurrence.metadata)
        }

    fun repeatEligible(
        campaign: Campaign,
        history: DisplayHistorySnapshot,
        nowMillis: Long
    ): Boolean {
        val record = history.perCampaign[campaign.campaignId] ?: return true
        if (!campaign.trigger.repeatable) return false
        val interval = campaign.trigger.minIntervalSeconds ?: return true
        return nowMillis - record.lastDisplayAtMillis >= interval * 1000L
    }

    /** Never display at or after the expiry instant. */
    private fun notExpired(campaign: Campaign, nowMillis: Long): Boolean =
        campaign.expiresAtMillis?.let { nowMillis < it } ?: true
}
