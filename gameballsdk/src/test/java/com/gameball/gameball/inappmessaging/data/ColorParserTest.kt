package com.gameball.gameball.inappmessaging.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ColorParserTest {

    @Test
    fun `six hex digits are promoted to full opacity`() {
        assertEquals(0xFFFFFFFF.toInt(), ColorParser.parse("#FFFFFF"))
        assertEquals(0xFF111827.toInt(), ColorParser.parse("#111827"))
    }

    @Test
    fun `the hash is optional`() {
        assertEquals(0xFF111827.toInt(), ColorParser.parse("111827"))
        assertEquals(0x99000000.toInt(), ColorParser.parse("99000000"))
    }

    @Test
    fun `eight hex digits are read as ARGB, not RGBA`() {
        // #80FF0000 is a half-transparent red: alpha 0x80, red 0xFF.
        val parsed = ColorParser.parse("#80FF0000")!!
        assertEquals(0x80, (parsed ushr 24) and 0xFF)
        assertEquals(0xFF, (parsed ushr 16) and 0xFF)
        assertEquals(0x00, (parsed ushr 8) and 0xFF)
        assertEquals(0x00, parsed and 0xFF)
    }

    @Test
    fun `a raw packed integer is accepted as Braze encodes it`() {
        assertEquals(0xFF111827.toInt(), ColorParser.parse(0xFF111827.toInt()))
        assertEquals(0xFF111827.toInt(), ColorParser.parse(0xFF111827L))
    }

    @Test
    fun `whitespace is tolerated`() {
        assertEquals(0xFFFFFFFF.toInt(), ColorParser.parse("  #FFFFFF  "))
    }

    @Test
    fun `alpha is honoured and never clamped to opaque`() {
        assertEquals(0x99000000.toInt(), ColorParser.parse("#99000000"))
    }

    @Test
    fun `malformed values return null so that one slot falls back`() {
        assertNull(ColorParser.parse(null))
        assertNull(ColorParser.parse(""))
        assertNull(ColorParser.parse("#FFF"))
        assertNull(ColorParser.parse("#GGGGGG"))
        assertNull(ColorParser.parse("red"))
        assertNull(ColorParser.parse("rgb(1,2,3)"))
        assertNull(ColorParser.parse(true))
    }
}
