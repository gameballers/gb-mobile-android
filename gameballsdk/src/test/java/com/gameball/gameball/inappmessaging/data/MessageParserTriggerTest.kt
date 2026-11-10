package com.gameball.gameball.inappmessaging.data

import com.gameball.gameball.inappmessaging.domain.Campaign
import com.gameball.gameball.inappmessaging.domain.FilterLogic
import com.gameball.gameball.inappmessaging.domain.FilterOperator
import com.gameball.gameball.inappmessaging.domain.MessageSelector
import com.gameball.gameball.inappmessaging.domain.TriggerOccurrence
import com.gameball.gameball.inappmessaging.domain.TriggerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageParserTriggerTest {

    private fun parseOne(trigger: String): Campaign? = MessageParser.parse(
        """
        { "messages": [ {
            "campaignId": 1, "messageType": 2, "contentMode": "prerendered",
            "trigger": $trigger,
            "content": {}, "locale": { "header": "Hello" }
        } ] }
        """.trimIndent()
    ).campaigns.firstOrNull()

    @Test
    fun `session_start parses with no fields`() {
        val trigger = parseOne("""{ "type": "session_start" }""")!!.trigger
        assertEquals(TriggerType.SESSION_START, trigger.type)
        assertEquals(false, trigger.repeatable)
        assertNull(trigger.minIntervalSeconds)
    }

    /**
     * The live account sends repeatable and minIntervalSeconds on session_start too. Ignoring
     * them made every app-open campaign once-ever, whatever the dashboard said.
     */
    @Test
    fun `a session_start trigger carries its repeat rule`() {
        val trigger = parseOne(
            """{ "type": "session_start", "repeatable": true, "minIntervalSeconds": 3600 }"""
        )!!.trigger
        assertEquals(TriggerType.SESSION_START, trigger.type)
        assertEquals(true, trigger.repeatable)
        assertEquals(3600, trigger.minIntervalSeconds)

        val everySession = parseOne(
            """{ "type": "session_start", "repeatable": true, "minIntervalSeconds": 0 }"""
        )!!.trigger
        assertEquals(true, everySession.repeatable)
        assertNull(everySession.minIntervalSeconds)
    }

    @Test
    fun `an event trigger matches on name and carries its repeat rule`() {
        val trigger = parseOne(
            """{ "type": "event", "eventId": 1382, "name": "place_order",
                 "repeatable": true, "minIntervalSeconds": 300 }"""
        )!!.trigger
        assertEquals(TriggerType.EVENT, trigger.type)
        assertEquals("place_order", trigger.eventName)
        assertEquals(true, trigger.repeatable)
        assertEquals(300, trigger.minIntervalSeconds)
    }

    @Test
    fun `an event trigger with a null or blank name drops the campaign`() {
        assertNull(parseOne("""{ "type": "event", "eventId": 1382, "name": null }"""))
        assertNull(parseOne("""{ "type": "event", "eventId": 1382, "name": "  " }"""))
        assertNull(parseOne("""{ "type": "event", "eventId": 1382 }"""))
    }

    @Test
    fun `an unknown or missing trigger type drops the campaign`() {
        assertNull(parseOne("""{ "type": "geofence" }"""))
        assertNull(parseOne("""{ }"""))
        assertNull(parseOne("""null"""))
    }

    @Test
    fun `minIntervalSeconds of zero means every occurrence`() {
        val trigger = parseOne(
            """{ "type": "event", "name": "x", "repeatable": true, "minIntervalSeconds": 0 }"""
        )!!.trigger
        assertNull(trigger.minIntervalSeconds)
    }

    @Test
    fun `filters parse with name, operator and value`() {
        val filters = parseOne(
            """{ "type": "event", "name": "purchase", "metadataLogicalOperator": "And",
                 "metadataFilters": [ { "name": "price", "operator": "greaterThan", "value": 100 },
                                      { "name": "currency", "operator": "Is", "value": "USD" } ] }"""
        )!!.trigger.filters
        assertEquals(2, filters.size)
        assertEquals("price", filters[0].name)
        assertEquals(FilterOperator.GREATER_THAN, filters[0].operator)
        assertEquals(100L, filters[0].value)
        assertEquals(FilterOperator.EQUALS, filters[1].operator)
        assertEquals("USD", filters[1].value)
    }

    /**
     * The backend spells them Greater and Less. Dropped as unrecognised, the filter disappeared
     * and the campaign showed for any value.
     */
    @Test
    fun `Greater and Less filters are kept and gate the campaign`() {
        val trigger = parseOne(
            """{ "type": "event", "name": "purchase", "metadataLogicalOperator": "AND",
                 "metadataFilters": [ { "metadataId": 1, "name": "price", "operator": "Greater", "value": 100 },
                                      { "metadataId": 2, "name": "quantity", "operator": "Less", "value": 5 } ] }"""
        )!!.trigger
        assertEquals(
            listOf(FilterOperator.GREATER_THAN, FilterOperator.LESS_THAN),
            trigger.filters.map { it.operator }
        )

        fun matches(price: Int, quantity: Int) = MessageSelector.triggerMatches(
            trigger,
            TriggerOccurrence.Event("purchase", mapOf("price" to price, "quantity" to quantity))
        )
        assertTrue(matches(price = 150, quantity = 1))
        assertFalse("a price at or under 100 must not match", matches(price = 100, quantity = 1))
        assertFalse("a quantity of 5 or more must not match", matches(price = 150, quantity = 5))
    }

    /** A filter you cannot name is one you cannot evaluate. */
    @Test
    fun `a filter with no name drops the whole campaign`() {
        assertNull(parseOne(
            """{ "type": "event", "name": "purchase",
                 "metadataFilters": [ { "operator": "greaterThan", "value": 100 } ] }"""
        ))
    }

    /** Widening is the right response to one bad field rather than a contract mismatch. */
    @Test
    fun `a bad operator or a null value drops only that filter`() {
        val filters = parseOne(
            """{ "type": "event", "name": "purchase",
                 "metadataFilters": [ { "name": "a", "operator": "startsWith", "value": 1 },
                                      { "name": "b", "operator": "equals", "value": null },
                                      { "name": "c", "operator": "equals", "value": 3 } ] }"""
        )!!.trigger.filters
        assertEquals(listOf("c"), filters.map { it.name })
    }

    /** "Any of these conditions" in the dashboard. It used to drop the campaign outright. */
    @Test
    fun `an Or logical operator is kept, in any case, and matches on any filter`() {
        listOf("Or", "OR", "or").forEach { spelling ->
            val parsed = parseOne(
                """{ "type": "event", "name": "purchase", "metadataLogicalOperator": "$spelling",
                     "metadataFilters": [ { "name": "price", "operator": "Greater", "value": 100 },
                                          { "name": "currency", "operator": "Is", "value": "USD" } ] }"""
            )
            assertNotNull("'$spelling' must not drop the campaign", parsed)
            val trigger = parsed!!.trigger
            assertEquals(FilterLogic.ANY, trigger.filterLogic)

            fun matches(metadata: Map<String, Any?>) = MessageSelector.triggerMatches(
                trigger, TriggerOccurrence.Event("purchase", metadata)
            )
            assertTrue(matches(mapOf("price" to 150, "currency" to "EUR")))
            assertTrue(matches(mapOf("price" to 50, "currency" to "USD")))
            assertFalse(matches(mapOf("price" to 50, "currency" to "EUR")))
        }
    }

    /**
     * Between was unrecognised, so its filter was dropped; under Or that left "any of none", and
     * the campaign showed on every purchase whatever the amount.
     */
    @Test
    fun `a Between filter is kept and gates the campaign, inclusive at both ends`() {
        val trigger = parseOne(
            """{ "type": "event", "name": "purchase", "metadataLogicalOperator": "OR",
                 "metadataFilters": [ { "name": "amount", "operator": "Between", "value": "100,500" } ] }"""
        )!!.trigger
        assertEquals(listOf("amount"), trigger.filters.map { it.name })

        fun matches(amount: Any) = MessageSelector.triggerMatches(
            trigger, TriggerOccurrence.Event("purchase", mapOf("amount" to amount))
        )
        assertTrue(matches(300))
        assertTrue("the lower edge is inside", matches(100))
        assertTrue("the upper edge is inside", matches(500))
        assertFalse(matches(99))
        assertFalse(matches(501))
    }

    /** Kept, as iOS keeps it, and it narrows: a range that cannot be read is never inside. */
    @Test
    fun `a Between filter whose range cannot be read is kept and never matches`() {
        val trigger = parseOne(
            """{ "type": "event", "name": "purchase", "metadataLogicalOperator": "OR",
                 "metadataFilters": [ { "name": "amount", "operator": "Between", "value": "100" } ] }"""
        )!!.trigger
        assertEquals(1, trigger.filters.size)
        assertFalse(
            MessageSelector.triggerMatches(
                trigger, TriggerOccurrence.Event("purchase", mapOf("amount" to 100))
            )
        )
    }

    /**
     * Under Or a dropped filter only narrows, until every one is dropped: "any of none" reads as
     * no condition at all and shows the campaign on every occurrence of its event.
     */
    @Test
    fun `an Or trigger none of whose filters can be read drops the campaign`() {
        assertNull(parseOne(
            """{ "type": "event", "name": "purchase", "metadataLogicalOperator": "OR",
                 "metadataFilters": [ { "name": "a", "operator": "startsWith", "value": 1 },
                                      { "name": "b", "operator": "equals", "value": null } ] }"""
        ))
        // Nothing declared is nothing dropped: no conditions, as under And.
        assertNotNull(parseOne(
            """{ "type": "event", "name": "purchase", "metadataLogicalOperator": "OR",
                 "metadataFilters": [] }"""
        ))
    }

    @Test
    fun `an Or trigger keeps the filters it can read and matches on them alone`() {
        val trigger = parseOne(
            """{ "type": "event", "name": "purchase", "metadataLogicalOperator": "OR",
                 "metadataFilters": [ { "name": "code", "operator": "startsWith", "value": "x" },
                                      { "name": "price", "operator": "Greater", "value": 100 } ] }"""
        )!!.trigger
        assertEquals(listOf("price"), trigger.filters.map { it.name })

        fun matches(metadata: Map<String, Any?>) = MessageSelector.triggerMatches(
            trigger, TriggerOccurrence.Event("purchase", metadata)
        )
        assertTrue(matches(mapOf("price" to 150)))
        assertFalse(matches(mapOf("price" to 50, "code" to "xyz")))
    }

    /** The Or guard is Or's alone: under And an unreadable filter still drops only itself. */
    @Test
    fun `an And trigger none of whose filters can be read keeps the campaign, as before`() {
        val trigger = parseOne(
            """{ "type": "event", "name": "purchase", "metadataLogicalOperator": "AND",
                 "metadataFilters": [ { "name": "a", "operator": "startsWith", "value": 1 } ] }"""
        )!!.trigger
        assertTrue(trigger.filters.isEmpty())
    }

    @Test
    fun `an unknown logical operator still drops the campaign`() {
        assertNull(parseOne(
            """{ "type": "event", "name": "x", "metadataLogicalOperator": "Xor",
                 "metadataFilters": [ { "name": "a", "operator": "equals", "value": 1 } ] }"""
        ))
    }

    @Test
    fun `a null or absent logical operator is treated as And`() {
        val trigger = parseOne(
            """{ "type": "event", "name": "x", "metadataLogicalOperator": null,
                 "metadataFilters": [ { "name": "a", "operator": "equals", "value": 1 } ] }"""
        )!!.trigger
        assertTrue(trigger.filters.isNotEmpty())
        assertEquals(FilterLogic.ALL, trigger.filterLogic)
        assertEquals(
            FilterLogic.ALL,
            parseOne("""{ "type": "event", "name": "x" }""")!!.trigger.filterLogic
        )
    }

    @Test
    fun `absent filters give an empty list, not a drop`() {
        assertTrue(parseOne("""{ "type": "event", "name": "x" }""")!!.trigger.filters.isEmpty())
        assertTrue(parseOne(
            """{ "type": "event", "name": "x", "metadataFilters": null }"""
        )!!.trigger.filters.isEmpty())
    }
}
