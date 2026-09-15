package com.gameball.gameball.inappmessaging.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import kotlin.math.max

/**
 * A wrapping row of buttons.
 *
 * Buttons must wrap rather than overflow: two German or Arabic labels are wider than two
 * English ones, and a plain row overflowed the modal card by 360px in testing. A second line
 * is always better than a clipped button.
 *
 * Written rather than taken from Flexbox because the module adds no runtime dependencies -
 * every integrator would pay for it. ConstraintLayout's Flow could do this too, but it needs
 * its children registered by id, which does not suit a 0-to-2 set built at bind time.
 *
 * Lays out in the resolved layout direction, so it mirrors under RTL.
 */
internal class GbFlowRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    /** Between buttons on a line. */
    var horizontalGap: Int = 0

    /** Between wrapped lines. */
    var verticalGap: Int = 0

    /** True aligns each line to the trailing edge, which is the modal's dialog convention. */
    var alignEnd: Boolean = false

    /** True stretches every child to the full width, one per line — the fullscreen style. */
    var stretchChildren: Boolean = false

    private val lineStarts = ArrayList<Int>()
    private val lineHeights = ArrayList<Int>()
    private val lineWidths = ArrayList<Int>()

    private fun visibleChildren(): List<View> =
        (0 until childCount).map { getChildAt(it) }.filter { it.visibility != View.GONE }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val available = MeasureSpec.getSize(widthMeasureSpec) - paddingStart - paddingEnd
        lineStarts.clear(); lineHeights.clear(); lineWidths.clear()

        val children = visibleChildren()
        if (children.isEmpty()) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), paddingTop + paddingBottom)
            return
        }

        val childSpec = if (stretchChildren) {
            MeasureSpec.makeMeasureSpec(available, MeasureSpec.EXACTLY)
        } else {
            MeasureSpec.makeMeasureSpec(available, MeasureSpec.AT_MOST)
        }
        children.forEach {
            it.measure(childSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        }

        var lineWidth = 0
        var lineHeight = 0
        var lineStart = 0
        children.forEachIndexed { index, child ->
            val next = if (lineWidth == 0) child.measuredWidth else lineWidth + horizontalGap + child.measuredWidth
            val mustWrap = stretchChildren || (lineWidth > 0 && next > available)
            if (mustWrap && lineWidth > 0) {
                lineStarts.add(lineStart); lineWidths.add(lineWidth); lineHeights.add(lineHeight)
                lineStart = index; lineWidth = child.measuredWidth; lineHeight = child.measuredHeight
            } else {
                lineWidth = next
                lineHeight = max(lineHeight, child.measuredHeight)
            }
        }
        lineStarts.add(lineStart); lineWidths.add(lineWidth); lineHeights.add(lineHeight)

        val height = paddingTop + paddingBottom +
            lineHeights.sum() + verticalGap * (lineHeights.size - 1)
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val children = visibleChildren()
        if (children.isEmpty()) return
        val rtl = isRtl()
        val available = width - paddingStart - paddingEnd
        val contentLeft = if (rtl) paddingEnd else paddingStart

        var y = paddingTop
        lineStarts.forEachIndexed { lineIndex, start ->
            val end = if (lineIndex + 1 < lineStarts.size) lineStarts[lineIndex + 1] else children.size
            val lineWidth = lineWidths[lineIndex]
            // Trailing alignment mirrors: "end" is the right edge in LTR and the left in RTL.
            val leading = if (alignEnd) available - lineWidth else 0
            var x = contentLeft + if (rtl) 0 else leading
            var xRtl = contentLeft + available - (if (rtl) leading else 0)

            for (i in start until end) {
                val child = children[i]
                val w = child.measuredWidth
                val h = child.measuredHeight
                if (rtl) {
                    child.layout(xRtl - w, y, xRtl, y + h)
                    xRtl -= w + horizontalGap
                } else {
                    child.layout(x, y, x + w, y + h)
                    x += w + horizontalGap
                }
            }
            y += lineHeights[lineIndex] + verticalGap
        }
    }

    /**
     * The view's resolved direction when the framework has computed one, falling back to the
     * configuration's.
     *
     * The configuration is the signal that actually matters here: it reflects the locale AND
     * the host manifest's android:supportsRtl, which is the thing an SDK cannot force. The
     * resolved view direction is checked first so a host that mirrors one subtree explicitly
     * is still honoured.
     */
    private fun isRtl(): Boolean =
        layoutDirection == View.LAYOUT_DIRECTION_RTL ||
            resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

    /** The number of lines the last measure produced. Lets a test assert wrapping happened. */
    internal fun lineCount(): Int = lineHeights.size
}
