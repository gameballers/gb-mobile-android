package com.gameball.gameball.inappmessaging.ui

import android.app.Activity
import android.view.View
import android.widget.ImageView
import com.gameball.gameball.R
import com.gameball.gameball.inappmessaging.domain.MessageAction
import com.gameball.gameball.inappmessaging.domain.MessageButton
import com.gameball.gameball.inappmessaging.domain.MessageColors
import com.gameball.gameball.inappmessaging.domain.MessageContent
import com.gameball.gameball.inappmessaging.domain.MessageLayout
import com.gameball.gameball.inappmessaging.domain.MessageOrientation
import com.gameball.gameball.inappmessaging.domain.SlidePosition
import com.gameball.gameball.inappmessaging.domain.TextAlign
import com.gameball.gameball.inappmessaging.runtime.PresentationCallbacks
import com.gameball.gameball.inappmessaging.runtime.ResolvedMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import com.gameball.gameball.inappmessaging.artwork.IamImageLoader
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class FullscreenMessageViewTest {

    private lateinit var activity: Activity
    private var dismissed = 0
    private var tapCount = 0

    private val callbacks = object : PresentationCallbacks {
        override fun onShown() = Unit
        override fun onTapped(button: MessageButton?) { tapCount++ }
        override fun onDismissed() { dismissed++ }
    }

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        // Without this the loader reports an immediate failure and every image
        // collapses - correct degradation, but it hides what these tests measure.
        IamImageLoader.init(activity)
        dismissed = 0; tapCount = 0
    }

    private fun content(
        imageUrl: String? = null,
        layout: MessageLayout = MessageLayout.DEFAULT,
        orientation: MessageOrientation = MessageOrientation.ANY,
        showClose: Boolean = true,
        headerAlign: TextAlign? = null,
        background: Int? = 0xFF111827.toInt()
    ) = MessageContent(
        header = "Order placed!", body = "Thank you", imageUrl = imageUrl, iconUrl = null,
        layout = layout, colors = MessageColors(background = background), buttons = emptyList(),
        clickAction = null, showCloseButton = showClose, dismissOnScrimTap = true,
        slidePosition = SlidePosition.BOTTOM, orientation = orientation,
        autoDismissMillis = null, headerAlign = headerAlign, bodyAlign = null, extras = emptyMap()
    )

    private fun view(
        content: MessageContent,
        header: String? = "Order placed!",
        body: String? = "Thank you",
        buttons: List<MessageButton> = emptyList(),
        height: Int = 1920
    ): FullscreenMessageView {
        val v = FullscreenMessageView(activity)
        v.bind(content, ResolvedMessage(header, body, buttons), callbacks)
        activity.setContentView(v)
        v.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
        v.layout(0, 0, 1080, height)
        return v
    }

    private fun button(id: String, text: String) = MessageButton(id, text, MessageAction.Dismiss)

    // --- image_only ---

    @Test
    fun `image_only fills the bounds and crops`() {
        val v = view(content(imageUrl = "https://x/a.jpg", layout = MessageLayout.IMAGE_ONLY))
        assertEquals(View.VISIBLE, v.bleedArtworkView().visibility)
        assertEquals(ImageView.ScaleType.CENTER_CROP, v.bleedArtworkView().scaleType)
        assertEquals("the poster must reach every edge", 1080, v.bleedArtworkView().width)
        assertEquals(1920, v.bleedArtworkView().height)
    }

    @Test
    fun `image_only draws no header or body even when the campaign supplies them`() {
        val v = view(content(imageUrl = "https://x/a.jpg", layout = MessageLayout.IMAGE_ONLY))
        assertEquals(View.GONE, v.columnView().visibility)
    }

    @Test
    fun `image_only anchors its buttons at the bottom`() {
        val v = view(
            content(imageUrl = "https://x/a.jpg", layout = MessageLayout.IMAGE_ONLY),
            buttons = listOf(button("a", "Shop the sale"))
        )
        val row = v.buttonRow()
        assertEquals(View.VISIBLE, row.visibility)
        assertTrue("buttons must sit near the bottom", row.top > 1920 / 2)
    }

    // --- text_with_image ---

    /** The fixed share is what stops the image letterboxing into the copy's leftover slack. */
    @Test
    fun `the image takes a fixed half of the available height`() {
        val v = view(content(imageUrl = "https://x/a.jpg"), height = 2000)
        val expected = (2000 * MessageMetrics.Fullscreen.IMAGE_HEIGHT_FRACTION).toInt()
        assertEquals(expected, v.artworkView().layoutParams.height)
    }

    /**
     * The Q1 decision. centerCrop here would discard 42% of the live 384x640 poster, which is
     * exactly the offer-baked-into-the-top loss that defect 9 recorded. centerCrop stays
     * correct only for image_only.
     */
    @Test
    fun `text_with_image draws the image fitCenter so nothing is cropped`() {
        val v = view(content(imageUrl = "https://x/a.jpg"))
        assertEquals(ImageView.ScaleType.FIT_CENTER, v.artworkView().scaleType)
    }

    @Test
    fun `no artwork hides the image and the copy still renders`() {
        val v = view(content(imageUrl = null))
        assertEquals(View.GONE, v.artworkView().visibility)
        assertEquals(View.VISIBLE, v.headerView().visibility)
        assertEquals(View.VISIBLE, v.bodyView().visibility)
    }

    @Test
    fun `copy scrolls within its remainder and never pushes the buttons off`() {
        val v = view(
            content(imageUrl = "https://x/a.jpg"),
            body = "Long promotional copy. ".repeat(200),
            buttons = listOf(button("a", "Track my order"))
        )
        val row = v.buttonRow()
        assertTrue(row.height > 0)
        assertTrue("the buttons must stay on screen", row.bottom <= 1920 + 1)
    }

    /** Fullscreen centres by default where a modal starts. */
    @Test
    fun `header and body default to centre`() {
        val v = view(content())
        assertEquals(
            android.view.Gravity.CENTER_HORIZONTAL,
            v.headerView().gravity and android.view.Gravity.HORIZONTAL_GRAVITY_MASK
        )
        assertEquals(
            android.view.Gravity.CENTER_HORIZONTAL,
            v.bodyView().gravity and android.view.Gravity.HORIZONTAL_GRAVITY_MASK
        )
    }

    /** START rather than LEFT, so it mirrors in Arabic. */
    @Test
    fun `textAlignment overrides the per-type default with a directional value`() {
        val v = view(content(headerAlign = TextAlign.START))
        assertEquals(
            android.view.Gravity.START,
            v.headerView().gravity and android.view.Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK
        )
    }

    @Test
    fun `buttons stack full width in payload order`() {
        val v = view(
            content(),
            buttons = listOf(button("a", "See how"), button("b", "Maybe later"))
        )
        val row = v.buttonRow()
        assertEquals("stacked, never side by side", 2, row.lineCount())
        assertEquals("See how", (row.getChildAt(0) as android.widget.Button).text)
        assertEquals("Maybe later", (row.getChildAt(1) as android.widget.Button).text)
    }

    @Test
    fun `the type scale is larger than a modal's`() {
        val v = view(content())
        assertTrue(
            MessageMetrics.Fullscreen.HEADER_TEXT_SP > MessageMetrics.Modal.HEADER_TEXT_SP
        )
        assertTrue(MessageMetrics.Fullscreen.BODY_TEXT_SP > MessageMetrics.Modal.BODY_TEXT_SP)
        assertEquals(
            MessageMetrics.sp(activity, MessageMetrics.Fullscreen.HEADER_TEXT_SP),
            v.headerView().textSize, 0.5f
        )
    }

    // --- close ---

    @Test
    fun `the close glyph is a sibling of the message body`() {
        val v = view(content())
        val target = v.findViewById<View>(R.id.gb_iam_fs_close_target)
        assertEquals(v, target.parent)
        target.performClick()
        assertEquals(1, dismissed)
        assertEquals("closing is never a tap", 0, tapCount)
    }

    @Test
    fun `no close glyph when the campaign asked for none`() {
        val v = view(content(showClose = false))
        assertEquals(View.GONE, v.findViewById<View>(R.id.gb_iam_fs_close_target).visibility)
    }

    // --- orientation ---

    @Test
    fun `a campaign with no orientation is accepted in portrait`() {
        val v = FullscreenMessageView(activity)
        assertTrue(v.orientationMatches(content(orientation = MessageOrientation.ANY)))
    }

    @Test
    fun `a portrait-only campaign is accepted in portrait`() {
        val v = FullscreenMessageView(activity)
        assertTrue(v.orientationMatches(content(orientation = MessageOrientation.PORTRAIT)))
    }

    /** Refused, so the service defers and retries on rotation rather than dropping it. */
    @Test
    @Config(qualifiers = "land")
    fun `a portrait-only campaign is refused in landscape`() {
        val landscapeActivity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val v = FullscreenMessageView(landscapeActivity)
        assertFalse(v.orientationMatches(content(orientation = MessageOrientation.PORTRAIT)))
        assertTrue(v.orientationMatches(content(orientation = MessageOrientation.LANDSCAPE)))
        assertTrue(v.orientationMatches(content(orientation = MessageOrientation.ANY)))
    }

    @Test
    fun `the campaign background is honoured, with no special ground for this type`() {
        val v = view(content(background = 0xFF111827.toInt()))
        val drawable = v.background as android.graphics.drawable.ColorDrawable
        assertEquals(0xFF111827.toInt(), drawable.color)
    }

    /** Opaque: nothing shows past it, so nothing reaches the app beneath. */
    @Test
    fun `the surface absorbs taps`() {
        assertTrue(view(content()).isClickable)
    }
}
