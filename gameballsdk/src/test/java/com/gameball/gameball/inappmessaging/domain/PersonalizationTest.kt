package com.gameball.gameball.inappmessaging.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalizationTest {

    private val values = mapOf(
        "player_name" to "Ahmed",
        "points_balance" to "1,250",
        "empty" to ""
    )

    @Test
    fun `a known token is substituted`() {
        assertEquals(
            "Thank you Ahmed - your points are on the way.",
            Personalization.substitute(
                "Thank you {player_name} - your points are on the way.", values
            )
        )
    }

    @Test
    fun `values are inserted verbatim, already formatted`() {
        assertEquals(
            "You have 1,250 points",
            Personalization.substitute("You have {points_balance} points", values)
        )
    }

    /** substitute leaves unknown tokens so a caller can still tell resolved from unresolved. */
    @Test
    fun `an unknown token is left exactly as written`() {
        assertEquals(
            "Hi {missing_token}",
            Personalization.substitute("Hi {missing_token}", values)
        )
    }

    @Test
    fun `malformed braces are untouched`() {
        listOf("{ spaced }", "{2}", "{", "}", "{}", "{-bad}", "{1abc}").forEach { text ->
            assertEquals(text, Personalization.substitute(text, values))
        }
    }

    @Test
    fun `double braces keep their outer pair`() {
        assertEquals("{Ahmed}", Personalization.substitute("{{player_name}}", values))
    }

    /** A substituted value is data, not a template. */
    @Test
    fun `substitution is one pass`() {
        val recursive = mapOf("a" to "{b}", "b" to "SHOULD NOT APPEAR")
        assertEquals("{b}", Personalization.substitute("{a}", recursive))
    }

    /** Walking matches in reverse is what keeps the later ranges valid. */
    @Test
    fun `a long replacement does not corrupt the tokens after it`() {
        val long = mapOf("a" to "X".repeat(50), "b" to "second", "c" to "third")
        assertEquals(
            "${"X".repeat(50)}|second|third",
            Personalization.substitute("{a}|{b}|{c}", long)
        )
    }

    @Test
    fun `hasToken is false for text with no brace and true only for a real token`() {
        assertFalse(Personalization.hasToken(null))
        assertFalse(Personalization.hasToken(""))
        assertFalse(Personalization.hasToken("no tokens here"))
        assertFalse(Personalization.hasToken("{ spaced }"))
        assertFalse(Personalization.hasToken("{2}"))
        assertTrue(Personalization.hasToken("Hi {player_name}"))
    }

    @Test
    fun `tokenNames collects across header, body and button labels`() {
        val names = Personalization.tokenNames(
            "Hello {player_name}",
            "You have {points_balance} points and {pending_points} pending",
            "Spend {points_balance}",
            null
        )
        assertEquals(setOf("player_name", "points_balance", "pending_points"), names)
    }

    @Test
    fun `tokenNames is empty for copy with no tokens`() {
        assertTrue(Personalization.tokenNames("plain", "also plain", null).isEmpty())
    }

    @Test
    fun `blankUnresolved removes anything still in braces`() {
        assertEquals("Hi ", Personalization.blankUnresolved("Hi {missing}"))
        assertEquals("a  b", Personalization.blankUnresolved("a {x} b"))
        assertNull(Personalization.blankUnresolved(null))
    }

    @Test
    fun `blankUnresolved leaves already-substituted text alone`() {
        val substituted = Personalization.substitute("Hi {player_name}", values)
        assertEquals("Hi Ahmed", Personalization.blankUnresolved(substituted))
    }

    @Test
    fun `blankUnresolved does not touch malformed braces`() {
        assertEquals("{ spaced }", Personalization.blankUnresolved("{ spaced }"))
        assertEquals("{2}", Personalization.blankUnresolved("{2}"))
    }

    /**
     * A token resolving to an empty string is treated as resolved, because it was: the API
     * answered, the customer just has no value. This is the case per-token defaults will fix
     * and it is knowingly left as is.
     */
    @Test
    fun `a token resolving to an empty string counts as resolved`() {
        val result = Personalization.substitute("Nice pick, {empty} - enjoy", values)
        assertEquals("Nice pick,  - enjoy", result)
        assertEquals("Nice pick,  - enjoy", Personalization.blankUnresolved(result))
    }

    @Test
    fun `the whole pipeline never lets a raw brace reach the screen`() {
        val rendered = Personalization.blankUnresolved(
            Personalization.substitute("Hi {player_name}, you have {unknown} left", values)
        )!!
        assertFalse(rendered.contains("{"))
        assertEquals("Hi Ahmed, you have  left", rendered)
    }

    /** The path where the fetch timed out and substitution never ran at all. */
    @Test
    fun `blanking alone is enough when substitution never ran`() {
        val rendered = Personalization.blankUnresolved("Hi {player_name}")!!
        assertFalse(rendered.contains("{"))
        assertEquals("Hi ", rendered)
    }
}
