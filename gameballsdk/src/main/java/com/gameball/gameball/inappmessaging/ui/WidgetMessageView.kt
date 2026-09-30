package com.gameball.gameball.inappmessaging.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.gameball.gameball.inappmessaging.domain.Campaign
import com.gameball.gameball.inappmessaging.domain.MessageType

/**
 * The chrome around a game campaign: a scrim and a card for a modal, edge to edge for fullscreen.
 *
 * Deliberately not `showProfile`'s dialog. An in-app message is drawn by this module, in this
 * module's own chrome, and a campaign that arrives through a message trigger should be
 * indistinguishable from one drawn as artwork.
 *
 * Nothing here reads [Campaign.content]: a game carries none. The close affordance is the
 * widget's own — it posts through `GameballWidget.closeWidget()` — plus the scrim and the back
 * button, which the presenter owns.
 */
internal class WidgetMessageView(context: Context) : FrameLayout(context) {

    @VisibleForTesting
    internal val widget = GameballWidgetView(context)

    private var card: View = widget

    /**
     * Lays out the chrome for [messageType] and returns the view the widget lives in.
     *
     * Fullscreen insets its content into the safe area while the surface stays edge to edge.
     * Measured on Flutter: a real fullscreen box is about 0.46 aspect on a portrait phone, well
     * inside the ratio at which the widget starts clipping. The earlier reading that fullscreen
     * clipped came from measuring a 1.12-aspect box and was wrong.
     */
    fun bind(campaign: Campaign, onScrimTap: () -> Unit) {
        when (campaign.messageType) {
            MessageType.FULLSCREEN -> bindFullscreen()
            else -> bindModal(campaign, onScrimTap)
        }
    }

    private fun bindModal(campaign: Campaign, onScrimTap: () -> Unit) {
        setBackgroundColor(campaign.content.colors.frame ?: MessageMetrics.Shared.DEFAULT_SCRIM)

        val margin = dp(MessageMetrics.Modal.MARGIN_DP)
        val radius = dp(MessageMetrics.Modal.CORNER_RADIUS_DP).toFloat()

        val host = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                cornerRadius = radius
                setColor(Color.WHITE)
            }
            // Clipped so the widget's own corners cannot square off the card's.
            clipToOutline = true
            addView(widget, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }
        card = host

        addView(host, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER).apply {
            setMargins(margin, margin, margin, margin)
        })
        host.maxWidthDp(MessageMetrics.Modal.MAX_WIDTH_DP)

        // A tap outside the card dismisses, matching every other modal in this module. Taps on
        // the card itself must not: a wheel is played by tapping it.
        setOnClickListener { onScrimTap() }
        host.setOnClickListener { /* swallowed */ }
    }

    private fun bindFullscreen() {
        setBackgroundColor(Color.BLACK)
        card = widget
        addView(widget, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // The surface goes edge to edge; the widget sits inside the safe area, so a wheel is not
        // cut by a cutout and a close button is not under the status bar.
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            widget.updateLayoutParams<LayoutParams> {
                setMargins(insets.left, insets.top, insets.right, insets.bottom)
            }
            windowInsets
        }
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
