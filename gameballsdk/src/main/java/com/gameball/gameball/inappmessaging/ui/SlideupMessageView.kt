package com.gameball.gameball.inappmessaging.ui

import android.content.Context
import androidx.appcompat.view.ContextThemeWrapper
import android.graphics.Outline
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.gameball.gameball.R
import com.gameball.gameball.inappmessaging.domain.MessageContent
import com.gameball.gameball.inappmessaging.domain.SlidePosition
import com.gameball.gameball.inappmessaging.runtime.PresentationCallbacks
import com.gameball.gameball.inappmessaging.runtime.ResolvedMessage
import com.google.android.material.card.MaterialCardView
import com.gameball.gameball.inappmessaging.artwork.IamImageLoader
import kotlin.math.abs

/**
 * messageType 1. A non-blocking banner at one edge.
 *
 * The only type that demands no decision from the customer: no scrim, no buttons, no close
 * glyph, and the app underneath stays fully usable. Everything about its geometry follows
 * from that - a banner that grew with its content would eventually cover the screen it exists
 * not to block.
 */
internal class SlideupMessageView(context: Context) : FrameLayout(context) {

    /**
     * The host's own context, used for campaign-absent colour fallbacks so the message keeps
     * adopting the app it lives in.
     */
    private val hostContext: Context = context

    private val root: View = LayoutInflater
        .from(ContextThemeWrapper(context, R.style.Theme_GameballIAM))
        .inflate(R.layout.gb_iam_slideup, this, true)
        .also {
            // A <merge> root, so the banner is a direct child and there is no redundant level.
            clipChildren = false
            clipToPadding = false
        }

    val banner: MaterialCardView = root.findViewById(R.id.gb_iam_slideup_card)
    private val row: View = root.findViewById(R.id.gb_iam_slideup_row)
    private val icon: ImageView = root.findViewById(R.id.gb_iam_slideup_icon)
    private val copy: TextView = root.findViewById(R.id.gb_iam_slideup_copy)
    private val chevron: ImageView = root.findViewById(R.id.gb_iam_slideup_chevron)

    private var slidePosition: SlidePosition = SlidePosition.BOTTOM
    private var callbacks: PresentationCallbacks? = null
    private var gestureDetector: GestureDetector? = null

    fun bind(
        content: MessageContent,
        resolved: ResolvedMessage,
        callbacks: PresentationCallbacks
    ) {
        this.callbacks = callbacks
        slidePosition = content.slidePosition

        applyGeometry()
        applyColours(content)
        applyCopy(content, resolved)
        applyIcon(content)
        applyChevron(content)
        applyInsets()
        applySwipe()
    }

    private fun applyGeometry() {
        val margin = MessageMetrics.dp(context, MessageMetrics.Slideup.MARGIN_DP)
        val maxWidth = MessageMetrics.dp(context, MessageMetrics.Slideup.MAX_WIDTH_DP)
        val available = resources.displayMetrics.widthPixels - margin * 2

        (banner.layoutParams as LayoutParams).apply {
            width = minOf(available, maxWidth)
            gravity = when (slidePosition) {
                SlidePosition.TOP -> android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
                SlidePosition.BOTTOM -> android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
            }
            setMargins(margin, margin, margin, margin)
        }
        banner.radius = MessageMetrics.dp(context, MessageMetrics.Slideup.CORNER_RADIUS_DP).toFloat()
        // It floats over app content with no scrim, so it needs the shadow to separate.
        banner.cardElevation =
            MessageMetrics.dp(context, MessageMetrics.Slideup.ELEVATION_DP).toFloat()
    }

    private fun applyColours(content: MessageContent) {
        banner.setCardBackgroundColor(
            ColorResolver.resolve(
                hostContext, content.colors.background,
                com.google.android.material.R.attr.colorSurface
            )
        )
        val textColor = ColorResolver.resolve(
            hostContext, content.colors.text,
            com.google.android.material.R.attr.colorOnSurface
        )
        copy.setTextColor(textColor)
        // The chevron follows colors.text, falling to onSurfaceVariant when unset.
        chevron.setColorFilter(
            content.colors.text ?: ColorResolver.themeColor(
                hostContext, com.google.android.material.R.attr.colorOnSurfaceVariant
            )
        )
    }

    private fun applyCopy(content: MessageContent, resolved: ResolvedMessage) {
        copy.text = resolved.body ?: resolved.header
        copy.textSize = MessageMetrics.Slideup.COPY_TEXT_SP
        // The clamp is a mechanism, not a number: bounding the container's height instead
        // truncates where a clamp grows the banner, and they diverge at a large text scale.
        copy.maxLines = MessageMetrics.Slideup.MAX_TEXT_LINES
        copy.gravity = content.bodyAlign.toGravity(android.view.Gravity.START)
    }

    private fun applyIcon(content: MessageContent) {
        val url = content.iconUrl
        if (url.isNullOrBlank()) {
            icon.visibility = View.GONE
            return
        }
        icon.visibility = View.VISIBLE
        val radius = MessageMetrics.dp(context, MessageMetrics.Slideup.ICON_CORNER_RADIUS_DP)
        icon.clipToOutline = true
        icon.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radius.toFloat())
            }
        }
        IamImageLoader.load(url, icon, object : com.squareup.picasso.Callback {
            override fun onSuccess() = Unit
            override fun onError(e: Exception?) {
                // Collapse to zero width rather than leaving a gap: otherwise every broken
                // image shifts the copy. A content problem degrades the message; it never
                // takes it down.
                icon.visibility = View.GONE
            }
        })
    }

    private fun applyChevron(content: MessageContent) {
        // Drawn only when the campaign set a message action, so the affordance matches the
        // behaviour. Inert - not merely unresponsive - when there is none.
        val hasAction = content.clickAction != null
        chevron.visibility = if (hasAction) View.VISIBLE else View.GONE
        if (hasAction) {
            banner.setOnClickListener { callbacks?.onTapped(null) }
        } else {
            banner.setOnClickListener(null)
        }
        // Order matters: View.setOnClickListener sets clickable to true even when handed
        // null, so an inert banner would otherwise absorb taps and ripple. The spec calls for
        // inert, not merely unresponsive.
        banner.isClickable = hasAction
        banner.isFocusable = hasAction
    }

    /**
     * Copy drawn under a cutout loses its first line, and a bottom banner overlapping the
     * gesture strip swallows the swipe that is the only way to dismiss it. Android 15 enforces
     * edge-to-edge for apps targeting it, so hosts increasingly draw behind the bars.
     */
    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            updatePadding(top = insets.top, bottom = insets.bottom)
            windowInsets
        }
        ViewCompat.requestApplyInsets(this)
    }

    /**
     * Swipe only toward its own edge - a top banner swipes up, a bottom one down. Sideways
     * would fight a horizontal scroll or ViewPager underneath.
     */
    private fun applySwipe() {
        val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent) = true

            override fun onFling(
                down: MotionEvent?, up: MotionEvent, velocityX: Float, velocityY: Float
            ): Boolean {
                if (abs(velocityY) <= abs(velocityX)) return false
                val towardOwnEdge = when (slidePosition) {
                    SlidePosition.TOP -> velocityY < 0
                    SlidePosition.BOTTOM -> velocityY > 0
                }
                if (!towardOwnEdge) return false
                callbacks?.onDismissed()
                return true
            }
        })
        gestureDetector = detector
        banner.setOnTouchListener { view, event ->
            val consumed = detector.onTouchEvent(event)
            if (!consumed && event.actionMasked == MotionEvent.ACTION_UP) view.performClick()
            consumed
        }
    }

    /**
     * The overlay spans the screen so the banner can be positioned, but it must intercept
     * nothing outside the banner's own band: taps must reach the app underneath.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean = false
}

/**
 * start/end rather than left/right, which is the difference between a layout that mirrors in
 * Arabic and one that does not.
 *
 * Expressed as a Gravity rather than View.textAlignment: Gravity.START/END are equally
 * directional and mirror the same way, and unlike textAlignment they are observable, so the
 * rule can be tested rather than only inspected.
 */
internal fun com.gameball.gameball.inappmessaging.domain.TextAlign?.toGravity(
    default: Int
): Int = when (this) {
    com.gameball.gameball.inappmessaging.domain.TextAlign.START -> android.view.Gravity.START
    com.gameball.gameball.inappmessaging.domain.TextAlign.END -> android.view.Gravity.END
    com.gameball.gameball.inappmessaging.domain.TextAlign.CENTER -> android.view.Gravity.CENTER_HORIZONTAL
    com.gameball.gameball.inappmessaging.domain.TextAlign.LEFT -> android.view.Gravity.LEFT
    com.gameball.gameball.inappmessaging.domain.TextAlign.RIGHT -> android.view.Gravity.RIGHT
    null -> default
}
