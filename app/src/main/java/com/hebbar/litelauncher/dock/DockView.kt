package com.hebbar.litelauncher.dock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.hebbar.litelauncher.R
import com.hebbar.litelauncher.util.DensityUtil
import com.hebbar.litelauncher.workspace.CellLayout

class DockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    val cellLayout: CellLayout
    private var highlightSlotIndex = -1

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = DensityUtil.dpToPx(context, 2f).toFloat()
        color = ContextCompat.getColor(context, R.color.accent_blue)
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.accent_blue_alpha30)
    }

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false
        background = ContextCompat.getDrawable(context, R.drawable.bg_dock_glass)
        elevation = DensityUtil.dpToPx(context, 8f).toFloat()

        cellLayout = CellLayout(context).apply {
            countX = 5
            countY = 1
        }
        addView(cellLayout, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun setDockColumns(cols: Int) {
        cellLayout.setGridSize(cols, 1)
    }

    fun highlightSlot(slotIndex: Int) {
        if (this.highlightSlotIndex != slotIndex) {
            this.highlightSlotIndex = slotIndex
            invalidate()
        }
    }

    fun clearDockHighlight() {
        if (this.highlightSlotIndex != -1) {
            this.highlightSlotIndex = -1
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val w = measuredWidth.toFloat()
        val h = measuredHeight.toFloat()
        if (w > 0 && h > 0) {
            val totalIconWidth = DensityUtil.dpToPx(context, 230f).toFloat()
            val iconHeight = DensityUtil.dpToPx(context, 46f).toFloat()
            val computedPadH = ((w - totalIconWidth) / 12f).coerceAtLeast(0f)
            val computedPadV = ((h - iconHeight) / 2f).coerceAtLeast(0f)
            cellLayout.setPadding(computedPadH.toInt(), computedPadV.toInt(), computedPadH.toInt(), computedPadV.toInt())
        }
    }

    fun pointToSlot(localX: Float): Int {
        val usableWidth = (cellLayout.width - cellLayout.paddingLeft - cellLayout.paddingRight).coerceAtLeast(1)
        val slotWidth = usableWidth / 5f
        val xInUsable = (localX - cellLayout.paddingLeft).coerceIn(0f, usableWidth.toFloat())
        return (xInUsable / slotWidth).toInt().coerceIn(0, 4)
    }

    fun visualRectForSlot(slotIndex: Int): RectF {
        val padLeft = cellLayout.paddingLeft.toFloat()
        val usableWidth = (cellLayout.width - cellLayout.paddingLeft - cellLayout.paddingRight).coerceAtLeast(1).toFloat()
        val slotWidth = usableWidth / 5f
        val iconSizePx = DensityUtil.dpToPx(context, 46f).toFloat()

        val slotLeft = padLeft + (slotIndex * slotWidth)
        val left = slotLeft + (slotWidth - iconSizePx) / 2f
        val top = (height - iconSizePx) / 2f
        return RectF(left, top, left + iconSizePx, top + iconSizePx)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (highlightSlotIndex in 0..4) {
            val rect = visualRectForSlot(highlightSlotIndex)
            val corner = DensityUtil.dpToPx(context, 14f).toFloat()
            canvas.drawRoundRect(rect, corner, corner, fillPaint)
            canvas.drawRoundRect(rect, corner, corner, strokePaint)
        }
    }
}
