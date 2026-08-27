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
import com.gameball.gameball.inappmessaging.runtime.PresentationCallbacks
import com.gameball.gameball.inappmessaging.runtime.ResolvedMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ModalMessageViewTest {

    private lateinit var activity: Activity
    private var tapped: MessageButton? = null
    private var tapCount = 0
    private var dismissed = 0

    private val callbacks = object : PresentationCallbacks {
        override fun onShown() = Unit
        override fun onTapped(button: MessageButton?) { tapped = button; tapCount++ }
        override fun onDismissed() { dismissed++ }
    }

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        tapped = null; tapCount = 0; dismissed = 0
    }

    private fun content(
        imageUrl: String? = null,
        layout: MessageLayout = MessageLayout.DEFAULT,
        showClose: Boolean = true,
        dismissOnScrim: Boolean = true,
        frame: Int? = null,
        background: Int? = 0xFFFFFFFF.toInt(),
        action: MessageAction? = null
    ) = MessageContent(
        header = "Hello", body = "Body", imageUrl = imageUrl, iconUrl = null,
        layout = layout, colors = MessageColors(background = background, frame = frame),
        buttons = emptyList(), clickAction = action, showCloseButton = showClose,
        dismissOnScrimTap = dismissOnScrim, slidePosition = SlidePosition.BOTTOM,
        orientation = MessageOrientation.ANY, autoDismissMillis = null,
        headerAlign = null, bodyAlign = null, extras = emptyMap()
    )

    private fun view(
        content: MessageContent,
        header: String? = "Hello",
        body: String? = "Body",
        buttons: List<MessageButton> = emptyList(),
        width: Int = 1080,
        height: Int = 1920
    ): ModalMessageView {
        val v = ModalMessageView(activity)
        v.bind(content, ResolvedMessage(header, body, buttons), callbacks)
        activity.setContentView(v)
        v.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
        v.layout(0, 0, width, height)
        return v
    }

    private fun button(id: String, text: String) =
        MessageButton(id, text, MessageAction.Dismiss)

    // --- scrolling and the button block ---

    /** Clipping removes the buttons first, which is the one part that has to stay reachable. */
    @Test
    @Config(qualifiers = "w320dp-h568dp")
    fun `long copy does not push the buttons off a small screen`() {
        val v = view(
            content(),
            body = "Long promotional copy. ".repeat(120),
            buttons = listOf(button("ok", "Shop now"))
        )
        val row = v.buttonRow()
        assertEquals(View.VISIBLE, row.visibility)
        assertTrue("the button row must have height", row.height > 0)
        assertTrue(
            "the buttons must stay within the card",
            row.bottom <= v.card.height + v.card.top + 1
        )
    }

    @Test
    @Config(fontScale = 2.0f)
    fun `at 2x text scale the buttons are still laid out`() {
        val v = view(
            content(),
            body = "Long promotional copy. ".repeat(60),
            buttons = listOf(button("ok", "Shop now"))
        )
        assertTrue(v.buttonRow().height > 0)
        assertTrue(v.buttonRow().bottom <= v.card.height + v.card.top + 1)
    }

    /** The obvious "make it scrollable" makes every card full height. Test both directions. */
    @Test
    fun `a short message produces a short card`() {
        val v = view(content(), body = "Hi")
        assertTrue("card was ${v.card.height}px of 1920", v.card.height < 1920 * 0.6)
    }

    @Test
    fun `the scroll view sizes to its content rather than filling`() {
        assertEquals(
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            view(content(), body = "Hi").scrollView().layoutParams.height
        )
    }

    /**
     * Two German labels are wider than two English ones, and a plain row overflowed the card
     * by 360px in testing. Robolectric's text measurement is too coarse to force a real wrap,
     * so the wrapping rule itself is covered by GbFlowRowTest on fixed-size children; what
     * matters here is that nothing escapes the card.
     */
    @Test
    @Config(qualifiers = "w320dp-h568dp")
    fun `long localised labels never overflow the row`() {
        val v = view(
            content(),
            buttons = listOf(
                button("a", "Vielleicht spater erinnern"),
                button("b", "Jetzt einlosen und sparen")
            )
        )
        val row = v.buttonRow()
        assertTrue(row.width > 0)
        row.children().forEach {
            assertTrue("button overflowed the row", it.right <= row.width + 1)
            assertTrue("button started before the row", it.left >= -1)
        }
    }

    @Test
    fun `two short labels stay on one line`() {
        val v = view(content(), buttons = listOf(button("a", "No"), button("b", "Yes")))
        assertEquals(1, v.buttonRow().lineCount())
    }

    @Test
    fun `a button tap reports that button`() {
        val v = view(content(), buttons = listOf(button("ok", "Shop now")))
        (v.buttonRow().getChildAt(0) as View).performClick()
        assertEquals("ok", tapped?.id)
    }

    // --- artwork ---

    @Test
    fun `artwork is fitCenter and never cropped in the default layout`() {
        val v = view(content(imageUrl = "https://x/a.jpg"))
        assertEquals(ImageView.ScaleType.FIT_CENTER, v.artworkView().scaleType)
    }

    @Test
    fun `image_only crops and draws no text`() {
        val v = view(content(imageUrl = "https://x/a.jpg", layout = MessageLayout.IMAGE_ONLY))
        assertEquals(ImageView.ScaleType.CENTER_CROP, v.artworkView().scaleType)
        assertEquals(View.GONE, v.scrollView().visibility)
    }

    @Test
    fun `image_only stretches and stacks its buttons over the artwork`() {
        val v = view(
            content(imageUrl = "https://x/a.jpg", layout = MessageLayout.IMAGE_ONLY),
            buttons = listOf(button("a", "Shop"), button("b", "Later"))
        )
        assertEquals("stacked, not a trailing row", 2, v.buttonRow().lineCount())
    }

    @Test
    fun `no artwork hides the image view entirely`() {
        assertEquals(View.GONE, view(content(imageUrl = null)).artworkView().visibility)
    }

    /** The crossover is one number wherever there is room for it. */
    @Test
    fun `the artwork cap is the smaller of the shape bound and the room bound`() {
        val v = view(content(imageUrl = "https://x/a.jpg"))
        val cardWidth = 900
        val roomy = v.artworkHeightCap(cardWidth, 5000)
        assertEquals("with room, the shape bound wins", (cardWidth / 0.55f).toInt(), roomy)

        val cramped = v.artworkHeightCap(cardWidth, 1000)
        val reserve = MessageMetrics.dp(activity, MessageMetrics.Modal.COPY_RESERVE_DP)
        assertEquals("when cramped, the reserve wins", 1000 - reserve, cramped)
    }

    // --- scrim and close ---

    @Test
    fun `the scrim absorbs a tap even when closeBehaviour is button`() {
        val v = view(content(dismissOnScrim = false))
        assertTrue("the scrim must block the app beneath", v.isClickable)
        v.performClick()
        assertEquals("but it must not dismiss", 0, dismissed)
    }

    @Test
    fun `the scrim dismisses when closeBehaviour allows it`() {
        view(content(dismissOnScrim = true)).performClick()
        assertEquals(1, dismissed)
    }

    @Test
    fun `the campaign frame colour overrides the default scrim`() {
        val v = view(content(frame = 0xCC112233.toInt()))
        val drawable = v.background as android.graphics.drawable.ColorDrawable
        assertEquals(0xCC112233.toInt(), drawable.color)
    }

    @Test
    fun `the default scrim is used when the campaign names none`() {
        val v = view(content(frame = null))
        val drawable = v.background as android.graphics.drawable.ColorDrawable
        assertEquals(MessageMetrics.Shared.DEFAULT_SCRIM, drawable.color)
    }

    /** Closing must never also fire the click action or count as engagement. */
    @Test
    fun `the close glyph is a sibling of the card, not a child`() {
        val v = view(content(action = MessageAction.Dismiss))
        val closeTarget = v.findViewById<View>(R.id.gb_iam_modal_close_target)
        assertNotEquals(v.card, closeTarget.parent)
        assertEquals(v, closeTarget.parent)

        closeTarget.performClick()
        assertEquals("closing is a dismissal", 1, dismissed)
        assertEquals("and never a tap", 0, tapCount)
    }

    @Test
    fun `no close glyph is drawn when closeBehaviour is swipe`() {
        val v = view(content(showClose = false))
        assertEquals(View.GONE, v.findViewById<View>(R.id.gb_iam_modal_close_target).visibility)
    }

    @Test
    fun `the close target is at least 48dp`() {
        val v = view(content())
        val target = v.findViewById<View>(R.id.gb_iam_modal_close_target)
        val minimum = MessageMetrics.dp(activity, MessageMetrics.Shared.CLOSE_TOUCH_TARGET_DP)
        assertTrue(target.layoutParams.width >= minimum)
        assertTrue(target.layoutParams.height >= minimum)
    }

    @Test
    fun `the card stops growing at its maximum width`() {
        val v = view(content(), width = 2400)
        val maxWidth = MessageMetrics.dp(activity, MessageMetrics.Modal.MAX_WIDTH_DP)
        assertTrue(v.card.layoutParams.width <= maxWidth)
    }
}

private fun GbFlowRow.children(): List<View> =
    (0 until childCount).map { getChildAt(it) }
