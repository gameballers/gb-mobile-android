package com.gameball.gameball.inappmessaging.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.VisibleForTesting
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.core.view.updateLayoutParams
import com.gameball.gameball.R
import com.gameball.gameball.inappmessaging.domain.Campaign
import com.gameball.gameball.inappmessaging.domain.MessageType

/**
 * The chrome around a game campaign: a scrim and a card for a modal, edge to edge for fullscreen.
 *
 * Deliberately not `showProfile`'s dialog. An in-app message is drawn by this module, in this
 * module's own chrome, and a campaign that arrives through a message trigger should be
 * indistinguishable from one drawn as artwork.
 *
 * Nothing here reads [Campaign.content]: a game carries none. On mobile the widget page draws no
 * close or back control for a game, so the way out is drawn here — a close control over the
 * game's top-end corner — beside the scrim and the back button, which the presenter owns.
 */
internal class WidgetMessageView(context: Context) : FrameLayout(context) {

    @VisibleForTesting
    internal val widget = GameballWidgetView(context)

    /** The box the game lives in: a rounded card for a modal, the safe area for fullscreen. */
    private val host = FrameLayout(context)

    /**
     * The close control, on a 48dp target in the game's top-end corner.
     *
     * A sibling of the game rather than anything inside it, so closing can never also be a tap on
     * the game; and inside [host], so it sits in the safe area with the game — never under the
     * status bar or a cutout.
     */
    @VisibleForTesting
    internal val closeTarget = FrameLayout(context)

    /**
     * Lays out the chrome for the campaign's messageType.
     *
     * Fullscreen insets its content into the safe area while the surface stays edge to edge.
     * Measured on Flutter: a real fullscreen box is about 0.46 aspect on a portrait phone, well
     * inside the ratio at which the widget starts clipping. The earlier reading that fullscreen
     * clipped came from measuring a 1.12-aspect box and was wrong.
     */
    fun bind(campaign: Campaign, onScrimTap: () -> Unit, onClose: () -> Unit) {
        val modal = campaign.messageType != MessageType.FULLSCREEN
        val margin = if (modal) dp(MessageMetrics.Modal.MARGIN_DP) else 0

        host.addView(widget, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(
            host,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER).apply {
                setMargins(margin, margin, margin, margin)
            }
        )

        if (modal) bindModal(campaign, onScrimTap) else bindFullscreen()
        bindClose(onClose)
        applyInsets(margin)
    }

    private fun bindModal(campaign: Campaign, onScrimTap: () -> Unit) {
        setBackgroundColor(campaign.content.colors.frame ?: MessageMetrics.Shared.DEFAULT_SCRIM)

        host.background = GradientDrawable().apply {
            cornerRadius = dp(MessageMetrics.Modal.CORNER_RADIUS_DP).toFloat()
            setColor(Color.WHITE)
        }
        // Clipped so the widget's own corners cannot square off the card's.
        host.clipToOutline = true
        host.maxWidthDp(MessageMetrics.Modal.MAX_WIDTH_DP)

        // A tap outside the card dismisses, matching every other modal in this module. Taps on
        // the card itself must not: a wheel is played by tapping it.
        setOnClickListener { onScrimTap() }
        host.setOnClickListener { /* swallowed */ }
    }

    private fun bindFullscreen() {
        setBackgroundColor(Color.BLACK)
    }

    private fun bindClose(onClose: () -> Unit) {
        val disc = dp(MessageMetrics.Game.CLOSE_DISC_DP)
        val glyphPadding = (disc - dp(MessageMetrics.Shared.CLOSE_GLYPH_SIZE_DP)) / 2
        val glyph = ImageView(context).apply {
            setImageResource(R.drawable.gb_iam_ic_close)
            // The game draws whatever it likes under this corner, so the glyph brings its own
            // contrast: white on a dark disc, rather than a colour derived from a background
            // nobody here can see.
            setColorFilter(MessageMetrics.Shared.CLOSE_GLYPH_ON_DARK)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(MessageMetrics.Game.CLOSE_DISC_COLOR)
            }
            setPadding(glyphPadding, glyphPadding, glyphPadding, glyphPadding)
            // The target carries the label; the glyph is decoration.
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        closeTarget.addView(glyph, LayoutParams(disc, disc, Gravity.CENTER))
        closeTarget.contentDescription = context.getString(R.string.gb_iam_close)
        closeTarget.isClickable = true
        closeTarget.isFocusable = true
        closeTarget.setOnClickListener { onClose() }

        val target = dp(MessageMetrics.Shared.CLOSE_TOUCH_TARGET_DP)
        val inset = dp(MessageMetrics.Game.CLOSE_INSET_DP)
        // END rather than RIGHT, so it mirrors under RTL.
        host.addView(closeTarget, LayoutParams(target, target, Gravity.TOP or Gravity.END).apply {
            topMargin = inset
            marginEnd = inset
        })
    }

    /**
     * The surface goes edge to edge; the game and its close sit inside the safe area, so a wheel
     * is not cut by a cutout and the close is never under the status bar. A modal keeps its
     * margin inside that area.
     */
    private fun applyInsets(margin: Int) {
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            host.updateLayoutParams<LayoutParams> {
                setMargins(
                    margin + insets.left,
                    margin + insets.top,
                    margin + insets.right,
                    margin + insets.bottom
                )
            }
            windowInsets
        }
        // Added to a window that has already dispatched its insets, which a view attached later
        // never hears about unless it asks.
        doOnAttach { ViewCompat.requestApplyInsets(it) }
    }

    /** Reveals the widget. Called once the readiness gate reports the page drawn. */
    fun reveal() = widget.reveal()

    fun destroy() = widget.destroy()

    private fun dp(value: Float): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics
    ).toInt()

    private fun View.maxWidthDp(value: Float) {
        val max = dp(value)
        addOnLayoutChangeListener { v, left, _, right, _, _, _, _, _ ->
            if (right - left > max) {
                v.updateLayoutParams<LayoutParams> { width = max }
            }
        }
    }
}
