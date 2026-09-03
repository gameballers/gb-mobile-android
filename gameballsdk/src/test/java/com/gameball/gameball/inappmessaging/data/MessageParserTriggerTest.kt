package com.gameball.gameball.inappmessaging.data

import com.gameball.gameball.inappmessaging.domain.Campaign
import com.gameball.gameball.inappmessaging.domain.FilterOperator
import com.gameball.gameball.inappmessaging.domain.TriggerType
import org.junit.Assert.assertEquals
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

    @Test
    fun `session_start carries its repeat rule`() {
        val trigger = parseOne(
            """{ "type": "session_start", "repeatable": true, "minIntervalSeconds": 60 }"""
        )!!.trigger
        assertEquals(TriggerType.SESSION_START, trigger.type)
        assertEquals(true, trigger.repeatable)
        assertEquals(60, trigger.minIntervalSeconds)
    }

    @Test
    fun `session_start with minIntervalSeconds of zero means every occurrence`() {
        val trigger = parseOne(
            """{ "type": "session_start", "repeatable": true, "minIntervalSeconds": 0 }"""
        )!!.trigger
        assertNull(trigger.minIntervalSeconds)
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

    @Test
    fun `a logical operator other than And drops the campaign`() {
        assertNull(parseOne(
            """{ "type": "event", "name": "x", "metadataLogicalOperator": "Or",
                 "metadataFilters": [ { "name": "a", "operator": "equals", "value": 1 } ] }"""
        ))
    }

    @Test
    fun `a null or absent logical operator is treated as And`() {
        assertTrue(parseOne(
            """{ "type": "event", "name": "x", "metadataLogicalOperator": null,
                 "metadataFilters": [ { "name": "a", "operator": "equals", "value": 1 } ] }"""
        )!!.trigger.filters.isNotEmpty())
    }

    @Test
    fun `absent filters give an empty list, not a drop`() {
        assertTrue(parseOne("""{ "type": "event", "name": "x" }""")!!.trigger.filters.isEmpty())
        assertTrue(parseOne(
            """{ "type": "event", "name": "x", "metadataFilters": null }"""
        )!!.trigger.filters.isEmpty())
    }
}
