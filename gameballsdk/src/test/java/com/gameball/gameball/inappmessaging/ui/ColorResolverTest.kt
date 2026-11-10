package com.gameball.gameball.inappmessaging.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorResolverTest {

    private val dark = MessageMetrics.Shared.CLOSE_GLYPH_ON_LIGHT   // #111827, used on light
    private val light = MessageMetrics.Shared.CLOSE_GLYPH_ON_DARK   // #FFFFFF, used on dark
    private val hostOnSurface = 0xFF445566.toInt()

    private fun contrast(a: Int, b: Int): Double {
        val la = ColorResolver.relativeLuminance(a)
        val lb = ColorResolver.relativeLuminance(b)
        val hi = maxOf(la, lb)
        val lo = minOf(la, lb)
        return (hi + 0.05) / (lo + 0.05)
    }

    /** Quietly substituting something more readable is how a brand colour is lost. */
    @Test
    fun `a named close colour is used verbatim, readable or not`() {
        val unreadable = 0xFFFF0000.toInt()
        assertEquals(
            unreadable,
            ColorResolver.closeGlyphColor(unreadable, 0xFFFF0000.toInt(), hostOnSurface)
        )
    }

    @Test
    fun `a light background gets the dark glyph`() {
        assertEquals(dark, ColorResolver.closeGlyphColor(null, 0xFFFFFFFF.toInt(), hostOnSurface))
    }

    @Test
    fun `a dark background gets the light glyph`() {
        assertEquals(light, ColorResolver.closeGlyphColor(null, 0xFF111827.toInt(), hostOnSurface))
    }

    @Test
    fun `a saturated background picks the contrasting half`() {
        assertEquals(dark, ColorResolver.closeGlyphColor(null, 0xFFF5C518.toInt(), hostOnSurface))
    }

    @Test
    fun `no campaign colour at all falls through to the host theme`() {
        assertEquals(hostOnSurface, ColorResolver.closeGlyphColor(null, null, hostOnSurface))
    }

    /** The property the whole derivation exists for. */
    @Test
    fun `the derived pair clears 3 to 1 against every background`() {
        val steps = listOf(0x00, 0x33, 0x66, 0x99, 0xCC, 0xFF)
        var worst = Double.MAX_VALUE
        var worstColor = 0
        for (r in steps) for (g in steps) for (b in steps) {
            val background = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
            val glyph = ColorResolver.closeGlyphColor(null, background, hostOnSurface)
            val ratio = contrast(glyph, background)
            if (ratio < worst) { worst = ratio; worstColor = background }
        }
        assertTrue(
            "worst contrast was %.2f:1 on #%06X".format(worst, worstColor and 0xFFFFFF),
            worst >= 3.0
        )
    }

    /** The live account's palette specifically. */
    @Test
    fun `the live palette clears 3 to 1`() {
        listOf(0xFFFFFFFF, 0xFF111827, 0xFF1F2937, 0xFFF9FAFB).forEach { argb ->
            val background = argb.toInt()
            val glyph = ColorResolver.closeGlyphColor(null, background, hostOnSurface)
            assertTrue(
                "#%06X only reached %.2f:1".format(background and 0xFFFFFF, contrast(glyph, background)),
                contrast(glyph, background) >= 3.0
            )
        }
    }

    @Test
    fun `the threshold is where the two change places`() {
        // A mid grey either side of luminance 0.179.
        val justAbove = 0xFF7C7C7C.toInt()
        val justBelow = 0xFF6E6E6E.toInt()
        assertTrue(ColorResolver.relativeLuminance(justAbove) > 0.179)
        assertTrue(ColorResolver.relativeLuminance(justBelow) < 0.179)
        assertEquals(dark, ColorResolver.closeGlyphColor(null, justAbove, hostOnSurface))
        assertEquals(light, ColorResolver.closeGlyphColor(null, justBelow, hostOnSurface))
    }

    @Test
    fun `relative luminance matches the WCAG reference values`() {
        assertEquals(0.0, ColorResolver.relativeLuminance(0xFF000000.toInt()), 0.0001)
        assertEquals(1.0, ColorResolver.relativeLuminance(0xFFFFFFFF.toInt()), 0.0001)
        assertEquals(0.2126, ColorResolver.relativeLuminance(0xFFFF0000.toInt()), 0.0001)
        assertEquals(0.7152, ColorResolver.relativeLuminance(0xFF00FF00.toInt()), 0.0001)
        assertEquals(0.0722, ColorResolver.relativeLuminance(0xFF0000FF.toInt()), 0.0001)
    }
}
