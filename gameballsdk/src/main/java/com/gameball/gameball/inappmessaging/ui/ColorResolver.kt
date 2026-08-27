package com.gameball.gameball.inappmessaging.ui

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

/**
 * Colour fallbacks, and the close glyph in particular.
 *
 * Where a campaign names no colour the message falls through to the embedding app's theme, so
 * it adopts the app it lives in rather than asserting a colour the brand never chose. Nothing
 * here ever falls back to a literal except the two close-glyph constants and the scrim, both
 * of which the spec names.
 */
internal object ColorResolver {

    /** WCAG 2.1 relative luminance. */
    fun relativeLuminance(@ColorInt color: Int): Double {
        fun channel(value: Int): Double {
            val s = value / 255.0
            return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel((color shr 16) and 0xFF) +
            0.7152 * channel((color shr 8) and 0xFF) +
            0.0722 * channel(color and 0xFF)
    }

    /**
     * Three cases, in order. There is no fourth, and nothing here consults the artwork.
     *
     * Deriving rather than defaulting is not a refinement: a fixed white glyph measures
     * 1.00:1 against a white card, and a fixed dark glyph measures 1.00:1 against the #111827
     * the live slideup campaign uses. Both failures are live cases, not hypotheticals. The
     * derived pair clears WCAG's 3:1 for a non-text control against every background, worst
     * case 3.8:1 at the threshold itself.
     *
     * The one case it cannot guarantee is a glyph over full-bleed artwork, where the message
     * background is not what sits behind it. That is the campaign that should name
     * colors.closeButton, and it is the same case Braze leaves to the marketer.
     */
    @ColorInt
    fun closeGlyphColor(
        @ColorInt campaignCloseButton: Int?,
        @ColorInt background: Int?,
        @ColorInt hostOnSurface: Int
    ): Int {
        // 1. The campaign asked for exactly this. Quietly substituting something more readable
        //    is how a brand colour becomes a colour nobody chose.
        campaignCloseButton?.let { return it }

        // 2. Whichever half of the pair contrasts with the surface the glyph sits on.
        background?.let {
            return if (relativeLuminance(it) > MessageMetrics.Shared.CLOSE_GLYPH_LUMINANCE_THRESHOLD) {
                MessageMetrics.Shared.CLOSE_GLYPH_ON_LIGHT
            } else {
                MessageMetrics.Shared.CLOSE_GLYPH_ON_DARK
            }
        }

        // 3. Material already guarantees this contrasts with the surface it sits on;
        //    computing our own would second-guess a solved problem.
        return hostOnSurface
    }

    /** The campaign's colour when it named one, otherwise the host theme's. */
    @ColorInt
    fun resolve(context: Context, @ColorInt campaign: Int?, @AttrRes themeAttr: Int): Int =
        campaign ?: themeColor(context, themeAttr)

    @ColorInt
    fun themeColor(context: Context, @AttrRes attr: Int): Int {
        val value = TypedValue()
        return if (context.theme.resolveAttribute(attr, value, true)) {
            if (value.resourceId != 0) {
                androidx.core.content.ContextCompat.getColor(context, value.resourceId)
            } else {
                value.data
            }
        } else {
            // A host whose theme lacks the attribute still gets a legible message.
            0xFF000000.toInt()
        }
    }
}
