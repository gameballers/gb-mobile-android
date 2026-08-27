package com.gameball.gameball.inappmessaging.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatButton
import com.gameball.gameball.R
import com.gameball.gameball.inappmessaging.domain.MessageButton
import com.gameball.gameball.inappmessaging.domain.MessageColors

/** Button and close-glyph construction shared by the modal and fullscreen views. */
internal object MessageViewSupport {

    /**
     * Builds one campaign button.
     *
     * With no colours named it renders as a bare text button in the host's primary colour,
     * which is the only path any live campaign takes today. A border is drawn only when the
     * campaign sets one - there is no default outline.
     */
    fun button(
        context: Context,
        hostContext: Context,
        model: MessageButton,
        textSizeSp: Float,
        paddingH: Int,
        paddingV: Int,
        cornerRadius: Float,
        bold: Boolean,
        onClick: (MessageButton) -> Unit
    ): Button = AppCompatButton(context).apply {
        text = model.text
        isAllCaps = false
        textSize = textSizeSp
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(paddingH, paddingV, paddingH, paddingV)
        gravity = Gravity.CENTER
        minHeight = MessageMetrics.dp(context, MessageMetrics.Shared.CLOSE_TOUCH_TARGET_DP)

        setTextColor(
            model.colors?.text
                ?: ColorResolver.themeColor(
                    hostContext, com.google.android.material.R.attr.colorPrimary
                )
        )

        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            this.cornerRadius = cornerRadius
            // Absent fill means a text button, i.e. transparent - not a themed fill.
            setColor(model.colors?.background ?: 0x00000000)
            model.colors?.border?.let { setStroke(MessageMetrics.dp(context, 1f), it) }
        }
        setOnClickListener { onClick(model) }
    }

    /**
     * The close control, sized as a 48dp target around a 24dp glyph.
     *
     * Always created as a SIBLING of the tappable message body, never a child: closing a
     * message must never also fire its click action or count as engagement.
     */
    fun applyCloseGlyph(
        target: View,
        glyph: ImageView,
        colors: MessageColors,
        hostContext: Context,
        onClose: () -> Unit
    ) {
        glyph.setColorFilter(
            ColorResolver.closeGlyphColor(
                campaignCloseButton = colors.closeButton,
                background = colors.background,
                hostOnSurface = ColorResolver.themeColor(
                    hostContext, com.google.android.material.R.attr.colorOnSurface
                )
            )
        )
        glyph.contentDescription = hostContext.getString(R.string.gb_iam_close)
        target.visibility = View.VISIBLE
        target.setOnClickListener { onClose() }
    }
}
