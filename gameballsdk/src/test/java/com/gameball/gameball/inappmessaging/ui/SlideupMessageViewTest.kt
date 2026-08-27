package com.gameball.gameball.inappmessaging.ui

import android.app.Activity
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
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
import com.google.android.material.card.MaterialCardView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class SlideupMessageViewTest {

    private lateinit var activity: Activity
    private var tapped = 0
    private var dismissed = 0

    private val callbacks = object : PresentationCallbacks {
        override fun onShown() = Unit
        override fun onTapped(button: MessageButton?) { tapped++ }
        override fun onDismissed() { dismissed++ }
    }

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    }

    private fun content(
        iconUrl: String? = null,
        action: MessageAction? = null,
        slideFrom: SlidePosition = SlidePosition.BOTTOM,
        buttons: List<MessageButton> = emptyList()
    ) = MessageContent(
        header = "H", body = "Nice pick, it earns you points", imageUrl = null, iconUrl = iconUrl,
        layout = MessageLayout.DEFAULT, colors = MessageColors(background = 0xFF111827.toInt()),
        buttons = buttons, clickAction = action, showCloseButton = true, dismissOnScrimTap = true,
        slidePosition = slideFrom, orientation = MessageOrientation.ANY,
        autoDismissMillis = 8_000L, headerAlign = null, bodyAlign = null, extras = emptyMap()
    )

    private fun view(
        content: MessageContent,
        body: String? = "Nice pick, it earns you points"
    ): SlideupMessageView {
        val v = SlideupMessageView(activity)
        v.bind(content, ResolvedMessage("H", body, emptyList()), callbacks)
        activity.setContentView(v)
        v.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )
        v.layout(0, 0, 1080, 1920)
        return v
    }

    private fun copyOf(v: SlideupMessageView): TextView =
        v.findViewById(R.id.gb_iam_slideup_copy)

    @Test
    fun `copy clamps to three lines and ellipsises`() {
        val copy = copyOf(view(content()))
        assertEquals(3, copy.maxLines)
        assertEquals(android.text.TextUtils.TruncateAt.END, copy.ellipsize)
    }

    /** The obvious implementation of "make it scrollable" makes every banner full height. */
    @Test
    fun `a short message stays short`() {
        val v = view(content(), body = "Hi")
        val banner = v.findViewById<MaterialCardView>(R.id.gb_iam_slideup_card)
        assertTrue("banner was ${banner.height}px of 1920", banner.height < 1920 / 4)
    }

    @Test
    fun `long copy does not grow the banner past its clamp`() {
        val short = view(content(), body = "Hi")
            .findViewById<MaterialCardView>(R.id.gb_iam_slideup_card).height
        val long = view(content(), body = "Summer sale is live ".repeat(40))
            .findViewById<MaterialCardView>(R.id.gb_iam_slideup_card).height
        assertTrue("long copy must not exceed roughly three lines more than short", long < short * 4)
    }

    /** Otherwise every broken image shifts the copy. */
    @Test
    fun `no icon means the icon view is gone, not merely invisible`() {
        val icon = view(content(iconUrl = null)).findViewById<ImageView>(R.id.gb_iam_slideup_icon)
        assertEquals(View.GONE, icon.visibility)
    }

    @Test
    fun `the chevron appears only with a message action`() {
        val without = view(content()).findViewById<ImageView>(R.id.gb_iam_slideup_chevron)
        assertEquals(View.GONE, without.visibility)

        val with = view(content(action = MessageAction.Dismiss))
            .findViewById<ImageView>(R.id.gb_iam_slideup_chevron)
        assertEquals(View.VISIBLE, with.visibility)
    }

    @Test
    fun `the chevron is auto-mirrored so it flips under RTL`() {
        val chevron = view(content(action = MessageAction.Dismiss))
            .findViewById<ImageView>(R.id.gb_iam_slideup_chevron)
        assertTrue(chevron.drawable.isAutoMirrored)
    }

    @Test
    fun `the whole surface is the tap target when an action is set, and inert otherwise`() {
        val inert = view(content()).findViewById<MaterialCardView>(R.id.gb_iam_slideup_card)
        assertFalse(inert.isClickable)

        val tappable = view(content(action = MessageAction.Dismiss))
            .findViewById<MaterialCardView>(R.id.gb_iam_slideup_card)
        assertTrue(tappable.isClickable)
        tappable.performClick()
        assertEquals(1, tapped)
    }

    /** A slideup renders one composition; buttons are dropped at parse, never rendered. */
    @Test
    fun `buttons are never rendered even if the model carries them`() {
        val withButtons = content(
            buttons = listOf(MessageButton("a", "A", MessageAction.Dismiss))
        )
        val v = view(withButtons)
        assertEquals(3, (v.findViewById<View>(R.id.gb_iam_slideup_row) as android.view.ViewGroup).childCount)
    }

    @Test
    fun `no close glyph is drawn`() {
        val v = view(content())
        assertEquals(0, v.findViewsWithText(
            ArrayList(), activity.getString(R.string.gb_iam_close), View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION
        ).let { ArrayList<View>().apply {
            v.findViewsWithText(this, activity.getString(R.string.gb_iam_close),
                View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION) }.size })
    }

    /** The app underneath must stay usable: the overlay owns only its own band. */
    @Test
    fun `taps outside the banner are not intercepted`() {
        val v = view(content())
        val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 540f, 200f, 0)
        assertFalse("the root must not consume touches", v.onTouchEvent(event))
        event.recycle()
    }

    @Test
    fun `the banner is positioned at its own edge`() {
        val bottom = view(content(slideFrom = SlidePosition.BOTTOM))
            .findViewById<MaterialCardView>(R.id.gb_iam_slideup_card)
        val bottomGravity = (bottom.layoutParams as android.widget.FrameLayout.LayoutParams).gravity

        val top = view(content(slideFrom = SlidePosition.TOP))
            .findViewById<MaterialCardView>(R.id.gb_iam_slideup_card)
        val topGravity = (top.layoutParams as android.widget.FrameLayout.LayoutParams).gravity

        assertNotEquals(bottomGravity, topGravity)
        assertTrue(bottomGravity and android.view.Gravity.BOTTOM != 0)
        assertTrue(topGravity and android.view.Gravity.TOP != 0)
    }

    @Test
    fun `the banner stops growing at its maximum width`() {
        val v = view(content())
        val banner = v.findViewById<MaterialCardView>(R.id.gb_iam_slideup_card)
        val maxWidth = MessageMetrics.dp(activity, MessageMetrics.Slideup.MAX_WIDTH_DP)
        assertTrue(
            "width ${banner.layoutParams.width} exceeded the $maxWidth cap",
            banner.layoutParams.width <= maxWidth
        )
    }

    /**
     * Everything in the row is directional, not sided: under RTL the icon moves to the right
     * and its gap moves with it. Asserting marginStart/marginEnd rather than left/right is
     * what catches a layout that would not mirror in Arabic.
     *
     * Robolectric does not model View.textAlignment or resolve inherited layout direction, so
     * the visual mirroring itself is on the device-QA list.
     */
    @Test
    fun `the row uses directional margins so it mirrors under RTL`() {
        val v = view(content(iconUrl = "https://x/i.png", action = MessageAction.Dismiss))
        val icon = v.findViewById<ImageView>(R.id.gb_iam_slideup_icon)
        val chevron = v.findViewById<ImageView>(R.id.gb_iam_slideup_chevron)

        val iconParams = icon.layoutParams as android.view.ViewGroup.MarginLayoutParams
        val chevronParams = chevron.layoutParams as android.view.ViewGroup.MarginLayoutParams
        val gap = MessageMetrics.dp(activity, MessageMetrics.Slideup.ICON_SPACING_END_DP)
        val chevronGap = MessageMetrics.dp(activity, MessageMetrics.Slideup.CHEVRON_SPACING_START_DP)

        assertEquals("the icon's gap must be directional", gap, iconParams.marginEnd)
        assertEquals(0, iconParams.marginStart)
        assertEquals("the chevron's gap must be directional", chevronGap, chevronParams.marginStart)
        assertEquals(0, chevronParams.marginEnd)
    }

    @Test
    fun `the campaign background colour is applied`() {
        val banner = view(content()).findViewById<MaterialCardView>(R.id.gb_iam_slideup_card)
        assertEquals(0xFF111827.toInt(), banner.cardBackgroundColor.defaultColor)
    }
}
