package com.gameball.gameball.inappmessaging.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    // --- gameReady: the widget's own word that the page it opened is on screen ---

    @Test
    fun `gameReady means the game is on screen and playable unless it says otherwise`() {
        assertEquals(
            GameReadiness.Playable,
            WidgetEvents.readinessOf("""{"type":"gameReady","metadata":{"page":"details_wheel_9645","playable":true}}""")
        )
        assertEquals(
            GameReadiness.Playable,
            WidgetEvents.readinessOf("""{"type":"gameReady","metadata":{"page":"details_wheel_9645"}}""")
        )
        assertEquals(GameReadiness.Playable, WidgetEvents.readinessOf("""{"type":"gameReady"}"""))
    }

    @Test
    fun `only an explicit playable false holds the game back, with the widget's reason`() {
        assertEquals(
            GameReadiness.Unplayable("unplayable"),
            WidgetEvents.readinessOf(
                """{"type":"gameReady","metadata":{"page":"details_tictactoe_12680","playable":false,"reason":"unplayable"}}"""
            )
        )
        assertEquals(
            GameReadiness.Unplayable("no reason given"),
            WidgetEvents.readinessOf("""{"type":"gameReady","metadata":{"playable":false}}""")
        )
        assertEquals(
            "a string is not the flag, as on the other SDKs",
            GameReadiness.Playable,
            WidgetEvents.readinessOf("""{"type":"gameReady","metadata":{"playable":"false"}}""")
        )
    }

    @Test
    fun `any other event says nothing about readiness, and never throws`() {
        assertNull(WidgetEvents.readinessOf("""{"type":"gameCompleted","metadata":{"playable":false}}"""))
        listOf(null, "", "not json", "null", "[]", "\"gameReady\"", """{"metadata":{"playable":false}}""")
            .forEach { assertNull("'$it'", WidgetEvents.readinessOf(it)) }
    }
}
