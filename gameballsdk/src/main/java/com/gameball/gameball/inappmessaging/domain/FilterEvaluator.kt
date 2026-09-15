package com.gameball.gameball.inappmessaging.domain

import com.gameball.gameball.inappmessaging.runtime.IamLog

/**
 * Evaluates a campaign's metadata filters against a triggering event's properties.
 *
 * Always a conjunction: metadataLogicalOperator is either "And" or the campaign was dropped
 * at parse.
 */
internal object FilterEvaluator {

    fun matches(filters: List<MetadataFilter>, metadata: Map<String, Any?>): Boolean =
        filters.all { matches(it, metadata) }

    private fun matches(filter: MetadataFilter, metadata: Map<String, Any?>): Boolean {
        // A filter is a requirement, so absence is failure. Otherwise filters are decorative:
        // a campaign targeting "spent over $100" would reach everyone whose event happened
        // not to carry the property.
        val actual = metadata[filter.name] ?: return false

        return when (filter.operator) {
            FilterOperator.EQUALS -> isEqual(actual, filter.value)
            FilterOperator.NOT_EQUALS -> !isEqual(actual, filter.value)
            FilterOperator.CONTAINS -> contains(actual, filter.value)
            FilterOperator.GREATER_THAN -> compare(actual, filter) { it > 0 }
            FilterOperator.GREATER_OR_EQUAL -> compare(actual, filter) { it >= 0 }
            FilterOperator.LESS_THAN -> compare(actual, filter) { it < 0 }
            FilterOperator.LESS_OR_EQUAL -> compare(actual, filter) { it <= 0 }
        }
    }

    /** Numeric when both sides coerce; string comparison otherwise. */
    private fun isEqual(actual: Any, expected: Any): Boolean {
        val a = asNumber(actual)
        val b = asNumber(expected)
        if (a != null && b != null) return a == b
        return actual.toString() == expected.toString()
    }

    private fun contains(actual: Any, expected: Any): Boolean = when (actual) {
        is Iterable<*> -> actual.any { it?.toString() == expected.toString() }
        else -> actual.toString().contains(expected.toString())
    }

    /**
     * Ordering refuses non-numbers rather than falling back to string comparison, under which
     * "9" > "100" and a price filter would silently invert.
     */
    private inline fun compare(
        actual: Any,
        filter: MetadataFilter,
        predicate: (Int) -> Boolean
    ): Boolean {
        val a = asNumber(actual)
        val b = asNumber(filter.value)
        if (a == null || b == null) {
            IamLog.w(
                "filter '${filter.name}' uses ${filter.operator} but " +
                    "'$actual' or '${filter.value}' is not a number; not matched"
            )
            return false
        }
        return predicate(a.compareTo(b))
    }

    private fun asNumber(value: Any?): Double? = when (value) {
        is Number -> value.toDouble()
        is String -> value.trim().toDoubleOrNull()
        else -> null
    }
}
