package com.gameball.gameball.inappmessaging.domain

/**
 * Which sequence an id belongs to.
 *
 * Not cosmetic. The backend's contract states that challenge ids and message campaign ids are
 * separate sequences that overlap numerically, so a bare number stops identifying a campaign
 * once both kinds share one list.
 */
internal enum class CampaignKind(val wire: String) {
    MESSAGE("message"),
    GAME("game")
}

/**
 * Identifies one campaign across both kinds.
 *
 * Every game entry arrives with `campaignId: 0` — the field is deliberately left empty rather
 * than filled with a challenge id — so a game's identity is its `gameCampaignId`.
 */
internal data class CampaignKey(val kind: CampaignKind, val id: Int) {

    /** The form written to the display history: `message:2215`, `game:9645`. */
    val storageKey: String get() = "${kind.wire}:$id"

    override fun toString(): String = storageKey

    companion object {
        fun message(id: Int) = CampaignKey(CampaignKind.MESSAGE, id)

        fun game(id: Int) = CampaignKey(CampaignKind.GAME, id)

        /**
         * Reads a storage key, and also a bare number.
         *
         * A bare number is how every history written before this release looks, and all of those
         * are message campaigns — games did not exist yet. Reading them as messages is what stops
         * an update re-showing every once-ever campaign.
         */
        fun parse(raw: String): CampaignKey? {
            raw.toIntOrNull()?.let { return message(it) }

            val separator = raw.indexOf(':')
            if (separator <= 0) return null
            val id = raw.substring(separator + 1).toIntOrNull() ?: return null
            return when (raw.substring(0, separator)) {
                CampaignKind.MESSAGE.wire -> message(id)
                CampaignKind.GAME.wire -> game(id)
                else -> null
            }
        }
    }
}
