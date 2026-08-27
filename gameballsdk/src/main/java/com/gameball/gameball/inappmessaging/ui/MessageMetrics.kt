package com.gameball.gameball.inappmessaging.ui

import android.content.Context
import android.util.TypedValue

/**
 * Every layout constant in the module, in one place.
 *
 * The Android counterpart to Flutter's message_view_metrics.dart and iOS's
 * MessageViewAttributes.swift. Views reference these; nothing is inlined. Cross-platform
 * visual parity is deliberately scheduled after all platforms ship, and keeping the numbers
 * here is what makes that pass a diff between three files rather than a hunt through three
 * widget trees.
 *
 * None of these is configurable by a host or a campaign. A campaign controls colour, text and
 * behaviour - never geometry.
 */
internal object MessageMetrics {

    object Shared {
        /** The dimmed layer behind a modal when the campaign names no colors.frame. */
        val DEFAULT_SCRIM: Int = 0x99000000.toInt()
        const val CLOSE_GLYPH_SIZE_DP = 24f
        /** Separate from the glyph: the accessibility minimum on both platforms. */
        const val CLOSE_TOUCH_TARGET_DP = 48f
        val CLOSE_GLYPH_ON_LIGHT: Int = 0xFF111827.toInt()
        val CLOSE_GLYPH_ON_DARK: Int = 0xFFFFFFFF.toInt()
        /** Where black and white contrast equally. Not a taste value. */
        const val CLOSE_GLYPH_LUMINANCE_THRESHOLD = 0.179
        const val BUTTON_CORNER_RADIUS_DP = 8f
    }

    object Modal {
        const val MARGIN_DP = 24f
        const val MAX_WIDTH_DP = 420f
        const val CORNER_RADIUS_DP = 16f
        const val CONTENT_PADDING_START_DP = 20f
        const val CONTENT_PADDING_TOP_DP = 20f
        const val CONTENT_PADDING_END_DP = 20f
        const val CONTENT_PADDING_BOTTOM_DP = 0f
        /** Applied only when both header and body are present. */
        const val HEADER_TO_BODY_SPACING_DP = 8f
        const val BUTTONS_PADDING_START_DP = 20f
        const val BUTTONS_PADDING_TOP_DP = 20f
        const val BUTTONS_PADDING_END_DP = 20f
        const val BUTTONS_PADDING_BOTTOM_DP = 16f
        /** Between buttons and between wrapped rows. */
        const val BUTTON_SPACING_DP = 8f
        const val BUTTON_PADDING_H_DP = 20f
        const val BUTTON_PADDING_V_DP = 12f
        const val IMAGE_ONLY_BUTTONS_PADDING_H_DP = 20f
        const val IMAGE_ONLY_BUTTONS_PADDING_BOTTOM_DP = 20f
        const val CLOSE_INSET_DP = 4f
        /**
         * Artwork at or above this ratio fills the card width with no bars. A shape rather
         * than a screen fraction, which is what makes the crossover device-independent: the
         * rule this replaced slid from 1.013 on a tall phone to 1.226 on a short one, so the
         * same square image was clean on one and letterboxed on the other.
         */
        const val MIN_IMAGE_RATIO = 0.55f
        /** Height always kept for copy and buttons, so artwork cannot squeeze out the CTA. */
        const val COPY_RESERVE_DP = 120f
        const val IMAGE_ONLY_HEIGHT_FRACTION = 0.65f
        const val HEADER_TEXT_SP = 22f
        const val HEADER_LINE_SP = 28f
        const val BODY_TEXT_SP = 14f
        const val BODY_LINE_SP = 20f
        const val BUTTON_TEXT_SP = 14f
    }

    object Slideup {
        const val MARGIN_DP = 12f
        const val MAX_WIDTH_DP = 480f
        const val CORNER_RADIUS_DP = 12f
        /** It has no scrim to separate it from the app, so it needs the shadow. */
        const val ELEVATION_DP = 6f
        const val CONTENT_PADDING_H_DP = 14f
        const val CONTENT_PADDING_V_DP = 12f
        /**
         * Load-bearing, and a mechanism rather than a number: bounding the container's height
         * instead truncates where a clamp grows the banner, and the two diverge at a large
         * accessibility text scale.
         */
        const val MAX_TEXT_LINES = 3
        /** Fixed square. Sizing to the artwork's own ratio would change the banner height. */
        const val ICON_SIZE_DP = 40f
        const val ICON_CORNER_RADIUS_DP = 8f
        const val ICON_SPACING_END_DP = 12f
        const val CHEVRON_SPACING_START_DP = 8f
        const val CHEVRON_SIZE_DP = 20f
        const val COPY_TEXT_SP = 14f
        const val COPY_LINE_SP = 20f
        const val DEFAULT_AUTO_DISMISS_MS = 8_000L
    }

    object Fullscreen {
        const val CONTENT_PADDING_START_DP = 24f
        const val CONTENT_PADDING_TOP_DP = 24f
        const val CONTENT_PADDING_END_DP = 24f
        const val CONTENT_PADDING_BOTTOM_DP = 0f
        /**
         * The artwork's fixed share of the available height. A fixed share rather than the
         * slack the copy leaves is what stops it letterboxing.
         *
         * The fit within this box is fitCenter, not centerCrop: with the live 384x640 asset
         * in a 390x375.5 box, cover crops 42% of the poster away, which is the offer baked
         * into the top of a promo image being lost. centerCrop is correct only for
         * image_only, where bleeding to every edge is the point.
         */
        const val IMAGE_HEIGHT_FRACTION = 0.50f
        const val IMAGE_ONLY_BUTTONS_PADDING_H_DP = 24f
        const val IMAGE_ONLY_BUTTONS_PADDING_BOTTOM_DP = 32f
        const val HEADER_TO_BODY_SPACING_DP = 12f
        const val BUTTONS_PADDING_START_DP = 24f
        const val BUTTONS_PADDING_TOP_DP = 28f
        const val BUTTONS_PADDING_END_DP = 24f
        const val BUTTONS_PADDING_BOTTOM_DP = 24f
        const val BUTTON_SPACING_DP = 12f
        const val BUTTON_PADDING_V_DP = 16f
        const val BUTTON_TEXT_SP = 16f
        const val CLOSE_PADDING_DP = 8f
        const val HEADER_TEXT_SP = 24f
        const val HEADER_LINE_SP = 32f
        const val BODY_TEXT_SP = 16f
        const val BODY_LINE_SP = 24f
    }

    object Motion {
        const val MODAL_DURATION_MS = 200L
        const val MODAL_SCALE_FROM = 0.96f
        const val FULLSCREEN_DURATION_MS = 200L
        const val SLIDEUP_DURATION_MS = 220L
    }

    fun dp(context: Context, value: Float): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value, context.resources.displayMetrics
    ).toInt()

    fun sp(context: Context, value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, value, context.resources.displayMetrics
    )
}
