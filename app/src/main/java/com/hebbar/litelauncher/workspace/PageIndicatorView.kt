package com.hebbar.litelauncher.workspace

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.hebbar.litelauncher.util.DensityUtil

class PageIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var dotCount: Int = 0
    private var activePageIndex: Int = 0

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        setPadding(0, DensityUtil.dpToPx(context, 8f), 0, DensityUtil.dpToPx(context, 8f))
    }

    fun setPageCount(count: Int) {
        if (dotCount == count && childCount == count) return
        this.dotCount = count
        removeAllViews()

        if (count <= 1) {
            visibility = GONE
            return
        }

        visibility = VISIBLE
        val dotSize = DensityUtil.dpToPx(context, 6f)
        val dotGap = DensityUtil.dpToPx(context, 8f)

        for (i in 0 until count) {
            val dot = View(context).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (i == activePageIndex) Color.WHITE else Color.parseColor("#60FFFFFF"))
                }
            }
            val lp = LayoutParams(dotSize, dotSize).apply {
                setMargins(dotGap / 2, 0, dotGap / 2, 0)
            }
            addView(dot, lp)
        }
    }

    fun setActivePage(page: Int) {
        this.activePageIndex = page
        if (childCount != dotCount || dotCount <= 1) return

        for (i in 0 until childCount) {
            val dot = getChildAt(i)
            val drawable = dot.background as? GradientDrawable
            if (i == activePageIndex) {
                drawable?.setColor(Color.WHITE)
                dot.alpha = 1.0f
            } else {
                drawable?.setColor(Color.parseColor("#60FFFFFF"))
                dot.alpha = 0.5f
            }
        }
    }
}
