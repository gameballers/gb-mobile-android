package com.gameball.gameball.inappmessaging.ui

import android.app.Activity
import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The wrapping algorithm, exercised with children of known width.
 *
 * Robolectric's text measurement is too coarse to drive a real button into wrapping, so the
 * rule is tested here on fixed-size children and the modal test asserts only that nothing
 * overflows.
 */
@RunWith(RobolectricTestRunner::class)
class GbFlowRowTest {

    private lateinit var activity: Activity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    }

    /** A child that always measures to a fixed size, whatever it is asked. */
    private fun child(w: Int, h: Int = 40) = object : View(activity) {
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) =
            setMeasuredDimension(w, h)
    }

    private fun row(
        width: Int,
        gap: Int = 8,
        alignEnd: Boolean = false,
        stretch: Boolean = false,
        rtl: Boolean = false,
        vararg widths: Int
    ): GbFlowRow {
        val parent = android.widget.FrameLayout(activity)
        if (rtl) parent.layoutDirection = View.LAYOUT_DIRECTION_RTL
        val row = GbFlowRow(activity).apply {
            horizontalGap = gap
            verticalGap = gap
            this.alignEnd = alignEnd
            stretchChildren = stretch
            widths.forEach { addView(child(it)) }
        }
        parent.addView(row)
        activity.setContentView(parent)
        parent.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        parent.layout(0, 0, width, parent.measuredHeight)
        return row
    }

    private fun GbFlowRow.kids() = (0 until childCount).map { getChildAt(it) }

    @Test
    fun `children that fit stay on one line`() {
        val r = row(width = 300, widths = intArrayOf(100, 100))
        assertEquals(1, r.lineCount())
    }

    @Test
    fun `a child that does not fit wraps to the next line`() {
        val r = row(width = 300, widths = intArrayOf(200, 200).toTypedArray().toIntArray())
        assertEquals(2, r.lineCount())
    }

    @Test
    fun `the gap counts toward the wrap decision`() {
        // 150 + 8 + 150 = 308 > 300, so these wrap even though 150+150 would fit.
        assertEquals(2, row(width = 300, widths = intArrayOf(150, 150)).lineCount())
        assertEquals(1, row(width = 320, widths = intArrayOf(150, 150)).lineCount())
    }

    @Test
    fun `nothing ever overflows the row`() {
        val r = row(width = 300, widths = intArrayOf(200, 200, 120))
        r.kids().forEach { assertTrue("child overflowed", it.right <= 300) }
    }

    @Test
    fun `wrapped lines are separated by the vertical gap`() {
        val r = row(width = 300, gap = 10, widths = intArrayOf(200, 200))
        val first = r.kids()[0]
        val second = r.kids()[1]
        assertEquals(first.bottom + 10, second.top)
    }

    @Test
    fun `height accounts for every line and the gaps between them`() {
        val r = row(width = 300, gap = 10, widths = intArrayOf(200, 200))
        assertEquals(40 + 10 + 40, r.measuredHeight)
    }

    @Test
    fun `trailing alignment pushes a short line to the end`() {
        val r = row(width = 300, alignEnd = true, widths = intArrayOf(100))
        assertEquals(300, r.kids()[0].right)
    }

    @Test
    fun `leading alignment is the default`() {
        val r = row(width = 300, alignEnd = false, widths = intArrayOf(100))
        assertEquals(0, r.kids()[0].left)
    }

    /** "end" is the right edge in LTR and the left edge in RTL. */
    @Test
    @Config(qualifiers = "ar-rEG-ldrtl")
    fun `trailing alignment mirrors under RTL`() {
        val r = row(width = 300, alignEnd = true, rtl = true, widths = intArrayOf(100))
        assertEquals(0, r.kids()[0].left)
    }

    @Test
    @Config(qualifiers = "ar-rEG-ldrtl")
    fun `children are laid out right-to-left under RTL`() {
        val r = row(width = 300, gap = 10, rtl = true, widths = intArrayOf(100, 100))
        val first = r.kids()[0]
        val second = r.kids()[1]
        assertEquals("the first child sits at the trailing edge", 300, first.right)
        assertTrue("the second sits to its left", second.right < first.left + 1)
    }

    @Test
    fun `stretch puts every child on its own full-width line`() {
        val r = row(width = 300, stretch = true, widths = intArrayOf(50, 50))
        assertEquals(2, r.lineCount())
    }

    @Test
    fun `an empty row measures to its padding only`() {
        val r = GbFlowRow(activity).apply {
            setPadding(0, 12, 0, 12)
            measure(
                View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
        }
        assertEquals(24, r.measuredHeight)
    }

    @Test
    fun `a gone child is skipped`() {
        val r = GbFlowRow(activity).apply {
            horizontalGap = 8
            addView(child(100).also { it.visibility = View.GONE })
            addView(child(100))
            measure(
                View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            layout(0, 0, 300, measuredHeight)
        }
        assertEquals(1, r.lineCount())
        assertEquals(0, r.getChildAt(1).left)
    }
}
