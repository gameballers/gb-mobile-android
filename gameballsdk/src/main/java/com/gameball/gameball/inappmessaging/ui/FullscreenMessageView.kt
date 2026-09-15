package com.gameball.gameball.inappmessaging.ui

import android.content.Context
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import com.gameball.gameball.R
import com.gameball.gameball.inappmessaging.artwork.IamImageLoader
import com.gameball.gameball.inappmessaging.domain.MessageContent
import com.gameball.gameball.inappmessaging.domain.MessageLayout
import com.gameball.gameball.inappmessaging.domain.MessageOrientation
import com.gameball.gameball.inappmessaging.runtime.PresentationCallbacks
import com.gameball.gameball.inappmessaging.runtime.ResolvedMessage

/**
 * messageType 3. Edge to edge, covering the app.
 *
 * No card, no margin and no scrim - the surface is opaque and nothing shows past it. This is
 * the only type that may insist on an orientation: a poster designed for portrait is not
 * merely narrow sideways, the copy baked into it becomes unreadable.
 */
internal class FullscreenMessageView(context: Context) : FrameLayout(context) {

    private val hostContext: Context = context
    private val themed = ContextThemeWrapper(context, R.style.Theme_GameballIAM)

    private val root: View = LayoutInflater.from(themed)
        .inflate(R.layout.gb_iam_fullscreen, this, true)

    private val bleedImage: ImageView = root.findViewById(R.id.gb_iam_fs_bleed_image)
    private val column: LinearLayout = root.findViewById(R.id.gb_iam_fs_column)
    private val image: ImageView = root.findViewById(R.id.gb_iam_fs_image)
    private val scroll: NestedScrollView = root.findViewById(R.id.gb_iam_fs_scroll)
    private val header: TextView = root.findViewById(R.id.gb_iam_fs_header)
    private val body: TextView = root.findViewById(R.id.gb_iam_fs_body)
    private val buttons: GbFlowRow = root.findViewById(R.id.gb_iam_fs_buttons)
    private val closeTarget: View = root.findViewById(R.id.gb_iam_fs_close_target)
    private val closeGlyph: ImageView = root.findViewById(R.id.gb_iam_fs_close)

    private var callbacks: PresentationCallbacks? = null

    /**
     * Orientation is enforced here and only here. A mismatch is refused so the service can
     * defer and retry on rotation; enforcing it for a banner or a card would suppress
     * messages for no benefit.
     */
    fun orientationMatches(content: MessageContent): Boolean = when (content.orientation) {
        MessageOrientation.ANY -> true
        MessageOrientation.PORTRAIT ->
            resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        MessageOrientation.LANDSCAPE ->
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    fun bind(
        content: MessageContent,
        resolved: ResolvedMessage,
        callbacks: PresentationCallbacks
    ) {
        this.callbacks = callbacks
        val imageOnly = content.layout == MessageLayout.IMAGE_ONLY && content.hasArtwork

        setBackgroundColor(
            ColorResolver.resolve(
                hostContext, content.colors.background,
                com.google.android.material.R.attr.colorSurface
            )
        )
        // Opaque, and it absorbs taps so nothing reaches the app beneath.
        isClickable = true
        setOnClickListener { content.clickAction?.let { callbacks.onTapped(null) } }

        if (imageOnly) bindImageOnly(content, resolved) else bindStacked(content, resolved)
        applyClose(content)
        applyInsets(imageOnly)
    }

    /**
     * The artwork fills the screen and the call to action floats over it. This is the one
     * place in the module where cropping is correct: the artwork is meant to reach every
     * edge, and letterboxing it would put bands of message background exactly where the
     * designer expected none.
     */
    private fun bindImageOnly(content: MessageContent, resolved: ResolvedMessage) {
        column.visibility = View.GONE
        bleedImage.visibility = View.VISIBLE
        bleedImage.scaleType = ImageView.ScaleType.CENTER_CROP
        loadInto(bleedImage, content.imageUrl)

        // Header and body are not drawn. A campaign declared image_only that also carries
        // text loses it silently - guarded in the dashboard, not here, so all platforms agree.
        buttons.removeAllViews()
        if (resolved.buttons.isEmpty()) {
            buttons.visibility = View.GONE
            return
        }
        // Re-parent the button row out of the hidden column and over the artwork.
        (buttons.parent as? android.view.ViewGroup)?.removeView(buttons)
        addView(
            buttons,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
                android.view.Gravity.BOTTOM
            )
        )
        buttons.visibility = View.VISIBLE
        val h = MessageMetrics.dp(context, MessageMetrics.Fullscreen.IMAGE_ONLY_BUTTONS_PADDING_H_DP)
        val b = MessageMetrics.dp(context, MessageMetrics.Fullscreen.IMAGE_ONLY_BUTTONS_PADDING_BOTTOM_DP)
        buttons.setPadding(h, 0, h, b)
        addButtons(resolved)
    }

    private fun bindStacked(content: MessageContent, resolved: ResolvedMessage) {
        bleedImage.visibility = View.GONE
        column.visibility = View.VISIBLE

        if (content.hasArtwork) {
            image.visibility = View.VISIBLE
            /*
             * fitCenter, not centerCrop: this artwork shares the screen with copy, and
             * cropping loses whatever the designer baked into the top of the poster - with
             * the live 384x640 asset in this box that is 42% of the image. centerCrop is
             * correct only for image_only, where bleeding to every edge is the point.
             *
             * The fixed half-height box below and this fit settle two different properties.
             * The box is the UI spec's rule and is what stops the image letterboxing into
             * whatever slack the copy leaves; the fit is defect 9's rule. Both hold.
             */
            image.scaleType = ImageView.ScaleType.FIT_CENTER
            loadInto(image, content.imageUrl)
        } else {
            image.visibility = View.GONE
        }

        val headerColor = ColorResolver.resolve(
            hostContext, content.colors.header, com.google.android.material.R.attr.colorOnSurface
        )
        val bodyColor = ColorResolver.resolve(
            hostContext, content.colors.text, com.google.android.material.R.attr.colorOnSurface
        )

        header.visibility = if (resolved.header.isNullOrBlank()) View.GONE else View.VISIBLE
        header.text = resolved.header
        header.textSize = MessageMetrics.Fullscreen.HEADER_TEXT_SP
        header.setTextColor(headerColor)
        // Header and body default to centre here and to start on a modal.
        header.gravity = content.headerAlign.toGravity(android.view.Gravity.CENTER_HORIZONTAL)

        body.visibility = if (resolved.body.isNullOrBlank()) View.GONE else View.VISIBLE
        body.text = resolved.body
        body.textSize = MessageMetrics.Fullscreen.BODY_TEXT_SP
        body.setTextColor(bodyColor)
        body.gravity = content.bodyAlign.toGravity(android.view.Gravity.CENTER_HORIZONTAL)

        val spacing = if (header.visibility == View.VISIBLE && body.visibility == View.VISIBLE) {
            MessageMetrics.dp(context, MessageMetrics.Fullscreen.HEADER_TO_BODY_SPACING_DP)
        } else {
            0
        }
        (body.layoutParams as android.view.ViewGroup.MarginLayoutParams).topMargin = spacing

        buttons.removeAllViews()
        buttons.visibility = if (resolved.buttons.isEmpty()) View.GONE else View.VISIBLE
        addButtons(resolved)
    }

    /** Stretched full width and stacked: a compact trailing row would look lost at this size. */
    private fun addButtons(resolved: ResolvedMessage) {
        buttons.stretchChildren = true
        buttons.alignEnd = false
        buttons.horizontalGap = MessageMetrics.dp(context, MessageMetrics.Fullscreen.BUTTON_SPACING_DP)
        buttons.verticalGap = buttons.horizontalGap
        resolved.buttons.forEach { model ->
            buttons.addView(
                MessageViewSupport.button(
                    context = themed,
                    hostContext = hostContext,
                    model = model,
                    textSizeSp = MessageMetrics.Fullscreen.BUTTON_TEXT_SP,
                    paddingH = 0,
                    paddingV = MessageMetrics.dp(context, MessageMetrics.Fullscreen.BUTTON_PADDING_V_DP),
                    cornerRadius = MessageMetrics.dp(
                        context, MessageMetrics.Shared.BUTTON_CORNER_RADIUS_DP
                    ).toFloat(),
                    bold = true
                ) { tapped -> callbacks?.onTapped(tapped) }
            )
        }
    }

    private fun loadInto(target: ImageView, url: String?) {
        if (url.isNullOrBlank()) { target.visibility = View.GONE; return }
        IamImageLoader.load(url, target, object : com.squareup.picasso.Callback {
            override fun onSuccess() = Unit
            override fun onError(e: Exception?) {
                // Collapses; the screen becomes flat background plus buttons.
                target.visibility = View.GONE
            }
        })
    }

    private fun applyClose(content: MessageContent) {
        if (!content.showCloseButton) {
            closeTarget.visibility = View.GONE
            return
        }
        MessageViewSupport.applyCloseGlyph(
            closeTarget, closeGlyph, content.colors, hostContext
        ) { callbacks?.onDismissed() }
    }

    /**
     * The stack sits inside the safe area; the full-bleed artwork deliberately does not. The
     * close glyph must clear the notch, since the surface goes edge to edge.
     */
    private fun applyInsets(imageOnly: Boolean) {
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            column.updatePadding(top = insets.top, bottom = insets.bottom)
            val pad = MessageMetrics.dp(context, MessageMetrics.Fullscreen.CLOSE_PADDING_DP)
            (closeTarget.layoutParams as LayoutParams).apply {
                topMargin = insets.top + pad
                marginEnd = pad
            }
            if (imageOnly) {
                (buttons.layoutParams as? LayoutParams)?.bottomMargin = insets.bottom
            }
            closeTarget.requestLayout()
            windowInsets
        }
        ViewCompat.requestApplyInsets(this)
    }

    /**
     * The image takes a fixed half of the available height rather than whatever slack the copy
     * leaves, which is what stops it letterboxing.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (image.visibility == View.VISIBLE) {
            val available = MeasureSpec.getSize(heightMeasureSpec) -
                column.paddingTop - column.paddingBottom
            image.layoutParams.height =
                (available * MessageMetrics.Fullscreen.IMAGE_HEIGHT_FRACTION).toInt()
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    internal fun artworkView(): ImageView = image
    internal fun bleedArtworkView(): ImageView = bleedImage
    internal fun buttonRow(): GbFlowRow = buttons
    internal fun scrollView(): NestedScrollView = scroll
    internal fun headerView(): TextView = header
    internal fun bodyView(): TextView = body
    internal fun columnView(): View = column
}
