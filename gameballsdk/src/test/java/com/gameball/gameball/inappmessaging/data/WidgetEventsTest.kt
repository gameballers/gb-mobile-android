package com.gameball.gameball.inappmessaging.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetEventsTest {

    /**
     * The widget names the game in its own terms — campaignType "spinTheWheel" where the sync
     * says "wheel" — so only the event's type is read, never its metadata.
     */
    @Test
    fun `gameCompleted is a play, whatever its metadata says`() {
        assertTrue(WidgetEvents.isGameCompleted(
            """{"type":"gameCompleted","metadata":{"campaignId":"9645","campaignType":"spinTheWheel","hasWon":true}}"""
        ))
        assertTrue(WidgetEvents.isGameCompleted(
            """{"type":"gameCompleted","metadata":{"campaignId":"1","campaignType":"somethingElse"}}"""
        ))
        assertTrue(WidgetEvents.isGameCompleted("""{"type":"gameCompleted"}"""))
    }

    @Test
    fun `any other event is not a play`() {
        assertFalse(WidgetEvents.isGameCompleted("""{"type":"rewardClaimed","metadata":{}}"""))
        assertFalse(WidgetEvents.isGameCompleted("""{"metadata":{"campaignType":"spinTheWheel"}}"""))
    }

    @Test
    fun `malformed input is not a play, and never throws`() {
        listOf(null, "", "  ", "not json", "[object Object]", "null", "[]", "\"gameCompleted\"")
            .forEach { assertFalse("'$it'", WidgetEvents.isGameCompleted(it)) }
    }
}
