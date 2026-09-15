package com.gameball.gameball.inappmessaging.domain

internal enum class TriggerType { SESSION_START, EVENT }

/**
 * There are exactly two trigger types. Purchases are not one of them: a purchase arrives as
 * an event named "purchase" with productId, price, currency and quantity folded into its
 * properties, so ordinary metadata filters work on them.
 */
internal data class Trigger(
    val type: TriggerType,
    /** Required for [TriggerType.EVENT]. Matching is on name — never on the backend's eventId. */
    val eventName: String? = null,
    val filters: List<MetadataFilter> = emptyList(),
    /** false means once ever, enforced on device. */
    val repeatable: Boolean = false,
    /** Only meaningful when [repeatable]. 0 or null means every occurrence. */
    val minIntervalSeconds: Int? = null
)

internal enum class FilterOperator {
    EQUALS, NOT_EQUALS, GREATER_THAN, GREATER_OR_EQUAL, LESS_THAN, LESS_OR_EQUAL, CONTAINS;

    companion object {
        private val BY_NAME: Map<String, FilterOperator> = mapOf(
            "equals" to EQUALS, "is" to EQUALS, "eq" to EQUALS,
            "notequals" to NOT_EQUALS, "isnot" to NOT_EQUALS, "neq" to NOT_EQUALS,
            "greaterthan" to GREATER_THAN, "gt" to GREATER_THAN,
            "greaterthanorequal" to GREATER_OR_EQUAL,
            "greaterthanorequals" to GREATER_OR_EQUAL,
            "gte" to GREATER_OR_EQUAL,
            "lessthan" to LESS_THAN, "lt" to LESS_THAN,
            "lessthanorequal" to LESS_OR_EQUAL,
            "lessthanorequals" to LESS_OR_EQUAL,
            "lte" to LESS_OR_EQUAL,
            "contains" to CONTAINS
        )

        /** Null for anything unrecognised — the caller drops that filter, not the campaign. */
        fun from(raw: String?): FilterOperator? =
            BY_NAME[raw?.trim()?.lowercase()?.replace("_", "")]
    }
}

/**
 * A requirement on the triggering event's metadata. A filter that cannot be named cannot be
 * evaluated, and treating it as "always true" would silently widen the campaign — so a
 * missing name drops the whole campaign, while a bad operator drops only the filter.
 */
internal data class MetadataFilter(
    val name: String,
    val operator: FilterOperator,
    val value: Any
)

/** What just happened, offered to the selector. */
internal sealed class TriggerOccurrence {
    object SessionStart : TriggerOccurrence()
    data class Event(
        val name: String,
        val metadata: Map<String, Any?> = emptyMap()
    ) : TriggerOccurrence()
}
