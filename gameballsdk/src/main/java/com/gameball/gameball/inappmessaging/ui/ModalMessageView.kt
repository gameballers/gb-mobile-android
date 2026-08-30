package com.gameball.gameball.inappmessaging.ui

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.widget.NestedScrollView
import com.gameball.gameball.R
import com.gameball.gameball.inappmessaging.artwork.IamImageLoader
import com.gameball.gameball.inappmessaging.domain.MessageContent
import com.gameball.gameball.inappmessaging.domain.MessageLayout
import com.gameball.gameball.inappmessaging.runtime.PresentationCallbacks
import com.gameball.gameball.inappmessaging.runtime.ResolvedMessage
import com.google.android.material.card.MaterialCardView
import kotlin.math.min

/**
 * messageType 2. A centred card over a dimmed app.
 *
 * It blocks: the scrim fills the overlay and swallows every tap that is not on the card,
 * whether or not that tap dismisses. That is what separates it from a slideup, and it is why
 * the dismissal rules matter more here - a modal the customer cannot close is a customer who
 * cannot use the app.
 */
internal class ModalMessageView(context: Context) : FrameLayout(context) {

    private val hostContext: Context = context
    private val themed = ContextThemeWrapper(context, R.style.Theme_GameballIAM)

    private val root: View = LayoutInflater.from(themed)
        .inflate(R.layout.gb_iam_modal, this, true)

    val card: MaterialCardView = root.findViewById(R.id.gb_iam_modal_card)
    private val column: View = root.findViewById(R.id.gb_iam_modal_column)
    private val image: ImageView = root.findViewById(R.id.gb_iam_modal_image)
    private val scroll: NestedScrollView = root.findViewById(R.id.gb_iam_modal_scroll)
    private val header: TextView = root.findViewById(R.id.gb_iam_modal_header)
    private val body: TextView = root.findViewById(R.id.gb_iam_modal_body)
    private val buttons: GbFlowRow = root.findViewById(R.id.gb_iam_modal_buttons)
    private val closeTarget: View = root.findViewById(R.id.gb_iam_modal_close_target)
    private val closeGlyph: ImageView = root.findViewById(R.id.gb_iam_modal_close)

    private var callbacks: PresentationCallbacks? = null

    fun bind(
        content: MessageContent,
        resolved: ResolvedMessage,
        callbacks: PresentationCallbacks
    ) {
        this.callbacks = callbacks
        val imageOnly = content.layout == MessageLayout.IMAGE_ONLY && content.hasArtwork

        applyScrim(content)
        applyCard(content)
        applyArtwork(content, imageOnly)
        applyCopy(content, resolved, imageOnly)
        applyButtons(content, resolved, imageOnly)
        applyClose(content)
    }

    /**
     * The scrim always absorbs the tap - its job is to block the app beneath - and dismisses
     * only when closeBehaviour allows it. Under "button" a tap outside does nothing at all,
     * deliberately.
     */
    private fun applyScrim(content: MessageContent) {
        setBackgroundColor(content.colors.frame ?: MessageMetrics.Shared.DEFAULT_SCRIM)
        isClickable = true
        isFocusable = true
        setOnClickListener {
            if (content.dismissOnScrimTap) callbacks?.onDismissed()
        }
        // The card must not pass its taps up to the scrim.
        card.isClickable = true
        card.setOnClickListener {
            content.clickAction?.let { callbacks?.onTapped(null) }
        }
    }

    private fun applyCard(content: MessageContent) {
        val margin = MessageMetrics.dp(context, MessageMetrics.Modal.MARGIN_DP)
        val maxWidth = MessageMetrics.dp(context, MessageMetrics.Modal.MAX_WIDTH_DP)
        val available = resources.displayMetrics.widthPixels - margin * 2
        (card.layoutParams as LayoutParams).apply {
            width = min(available, maxWidth)
            gravity = Gravity.CENTER
            setMargins(margin, margin, margin, margin)
        }
        card.radius = MessageMetrics.dp(context, MessageMetrics.Modal.CORNER_RADIUS_DP).toFloat()
        card.setCardBackgroundColor(
            ColorResolver.resolve(
                hostContext, content.colors.background,
                com.google.android.material.R.attr.colorSurface
            )
        )
    }

    private fun applyArtwork(content: MessageContent, imageOnly: Boolean) {
        val url = content.imageUrl
        if (url.isNullOrBlank()) {
            image.visibility = View.GONE
            return
        }
        image.visibility = View.VISIBLE

        val cardWidth = (card.layoutParams as LayoutParams).width
        val screenHeight = resources.displayMetrics.heightPixels
        image.maxHeight = if (imageOnly) {
            // The image is the whole message; cover, capped at 65% of the screen.
            image.scaleType = ImageView.ScaleType.CENTER_CROP
            image.adjustViewBounds = false
            (screenHeight * MessageMetrics.Modal.IMAGE_ONLY_HEIGHT_FRACTION).toInt()
        } else {
            // contain: never cropped, never distorted.
            image.scaleType = ImageView.ScaleType.FIT_CENTER
            image.adjustViewBounds = true
            artworkHeightCap(cardWidth, screenHeight)
        }
        if (imageOnly) {
            image.layoutParams.height = (screenHeight * MessageMetrics.Modal.IMAGE_ONLY_HEIGHT_FRACTION).toInt()
        }

        IamImageLoader.load(url, image, object : com.squareup.picasso.Callback {
            override fun onSuccess() = Unit
            override fun onError(e: Exception?) {
                // Collapses to nothing; the text band closes up and the message still shows.
                image.visibility = View.GONE
            }
        })
    }

    /**
     * The smaller of two bounds:
     *   byShape - the same crossover ratio on every device, which a screen fraction could not
     *             give: the rule this replaced slid from 1.013 on a tall phone to 1.226 on a
     *             short one, so the same square image was clean on one and letterboxed on the
     *             other.
     *   byRoom  - never let artwork squeeze out the call to action.
     */
    internal fun artworkHeightCap(cardWidthPx: Int, availableHeightPx: Int): Int {
        val byShape = (cardWidthPx / MessageMetrics.Modal.MIN_IMAGE_RATIO).toInt()
        val byRoom = availableHeightPx -
            MessageMetrics.dp(context, MessageMetrics.Modal.COPY_RESERVE_DP)
        return min(byShape, byRoom)
    }

    private fun applyCopy(content: MessageContent, resolved: ResolvedMessage, imageOnly: Boolean) {
        if (imageOnly) {
            // The image carries the message; text is never drawn, even when supplied.
            scroll.visibility = View.GONE
            return
        }
        scroll.visibility = View.VISIBLE
        val headerColor = ColorResolver.resolve(
            hostContext, content.colors.header, com.google.android.material.R.attr.colorOnSurface
        )
        val bodyColor = ColorResolver.resolve(
            hostContext, content.colors.text, com.google.android.material.R.attr.colorOnSurface
        )

        header.visibility = if (resolved.header.isNullOrBlank()) View.GONE else View.VISIBLE
        header.text = resolved.header
        header.textSize = MessageMetrics.Modal.HEADER_TEXT_SP
        header.setTextColor(headerColor)
        header.gravity = content.headerAlign.toGravity(android.view.Gravity.START)

        body.visibility = if (resolved.body.isNullOrBlank()) View.GONE else View.VISIBLE
        body.text = resolved.body
        body.textSize = MessageMetrics.Modal.BODY_TEXT_SP
        body.setTextColor(bodyColor)
        body.gravity = content.bodyAlign.toGravity(android.view.Gravity.START)

        // Applied only when both are present.
        val spacing = if (header.visibility == View.VISIBLE && body.visibility == View.VISIBLE) {
            MessageMetrics.dp(context, MessageMetrics.Modal.HEADER_TO_BODY_SPACING_DP)
        } else {
            0
        }
        (body.layoutParams as android.view.ViewGroup.MarginLayoutParams).topMargin = spacing

        // wrap_content with a cap, never match_parent: the obvious "make it scrollable" makes
        // every card full height, and a short message must stay short.
        scroll.layoutParams.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
    }

    private fun applyButtons(
        content: MessageContent,
        resolved: ResolvedMessage,
        imageOnly: Boolean
    ) {
        buttons.removeAllViews()
        if (resolved.buttons.isEmpty()) {
            buttons.visibility = View.GONE
            return
        }
        buttons.visibility = View.VISIBLE
        buttons.horizontalGap = MessageMetrics.dp(context, MessageMetrics.Modal.BUTTON_SPACING_DP)
        buttons.verticalGap = buttons.horizontalGap
        // A compact right-aligned row is a dialog convention and looks lost over a poster, so
        // image_only stretches and stacks instead.
        buttons.alignEnd = !imageOnly
        buttons.stretchChildren = imageOnly

        if (imageOnly) {
            val h = MessageMetrics.dp(context, MessageMetrics.Modal.IMAGE_ONLY_BUTTONS_PADDING_H_DP)
            val b = MessageMetrics.dp(context, MessageMetrics.Modal.IMAGE_ONLY_BUTTONS_PADDING_BOTTOM_DP)
            buttons.setPadding(h, 0, h, b)
        }

        resolved.buttons.forEach { model ->
            buttons.addView(
                MessageViewSupport.button(
                    context = themed,
                    hostContext = hostContext,
                    model = model,
                    textSizeSp = MessageMetrics.Modal.BUTTON_TEXT_SP,
                    paddingH = MessageMetrics.dp(context, MessageMetrics.Modal.BUTTON_PADDING_H_DP),
                    paddingV = MessageMetrics.dp(context, MessageMetrics.Modal.BUTTON_PADDING_V_DP),
                    cornerRadius = MessageMetrics.dp(
                        context, MessageMetrics.Shared.BUTTON_CORNER_RADIUS_DP
                    ).toFloat(),
                    bold = false
                ) { tapped -> callbacks?.onTapped(tapped) }
            )
        }
    }

    private fun applyClose(content: MessageContent) {
        if (!content.showCloseButton) {
            closeTarget.visibility = View.GONE
            return
        }
        MessageViewSupport.applyCloseGlyph(
            closeTarget, closeGlyph, content.colors, hostContext
        ) { callbacks?.onDismissed() }

        // FrameLayout orders siblings by elevation, not by declaration - the card's shadow
        // otherwise draws on top of the close target and swallows both the glyph and the tap.
        closeTarget.elevation = card.cardElevation + 1f

        // Top trailing corner of the card, inset 4dp. It mirrors under RTL because the
        // gravity is END rather than RIGHT.
        val inset = MessageMetrics.dp(context, MessageMetrics.Modal.CLOSE_INSET_DP)
        val margin = MessageMetrics.dp(context, MessageMetrics.Modal.MARGIN_DP)
        (closeTarget.layoutParams as LayoutParams).apply {
            gravity = Gravity.TOP or Gravity.END
            marginEnd = margin + inset
            topMargin = margin + inset
        }
        closeTarget.post {
            // Align to the card's own top once it has been positioned.
            (closeTarget.layoutParams as LayoutParams).topMargin = card.top + inset
            closeTarget.requestLayout()
        }
    }

    internal fun buttonRow(): GbFlowRow = buttons
    internal fun scrollView(): NestedScrollView = scroll
    internal fun artworkView(): ImageView = image
}
