package com.hebbar.litelauncher.workspace

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.hebbar.litelauncher.R
import com.hebbar.litelauncher.util.DensityUtil

class CellLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    var countX: Int = 4
    var countY: Int = 5

    var pageIndex: Int = 0
    var cellWidth: Int = 0
        private set
    var cellHeight: Int = 0
        private set

    private var dragHighlightRect: RectF? = null
    private var isHighlightValid = true

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = DensityUtil.dpToPx(context, 2f).toFloat()
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false
    }

    fun setGridSize(cols: Int, rows: Int) {
        this.countX = cols
        this.countY = rows
        requestLayout()
    }

    fun setDragHighlight(cellX: Int, cellY: Int, spanX: Int = 1, spanY: Int = 1, isValid: Boolean = true) {
        val rectLocal = visualRectForItem(cellX, cellY, spanX, spanY)
        this.dragHighlightRect = rectLocal
        this.isHighlightValid = isValid

        val strokeColor = if (isValid) R.color.accent_blue else R.color.error_red
        val fillColor = if (isValid) R.color.accent_blue_alpha30 else R.color.error_red_alpha25

        strokePaint.color = ContextCompat.getColor(context, strokeColor)
        fillPaint.color = ContextCompat.getColor(context, fillColor)

        invalidate()
    }

    fun setDragHighlight(cellX: Int, cellY: Int, isValid: Boolean) {
        setDragHighlight(cellX, cellY, 1, 1, isValid)
    }

    fun clearDragHighlight() {
        if (dragHighlightRect != null) {
            dragHighlightRect = null
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        val padTop = if (pageIndex == 0) DensityUtil.dpToPx(context, 16f) else 0
        val availW = width - paddingLeft - paddingRight
        val availH = height - padTop - paddingBottom

        cellWidth = if (countX > 0) availW / countX else availW
        cellHeight = if (countY > 0) availH / countY else availH

        for (i in 0 until childCount) {
            val child = getChildAt(i) ?: continue
            if (child.visibility == GONE) continue

            val lp = child.layoutParams as LayoutParams
            val childW = cellWidth * lp.spanX
            val childH = cellHeight * lp.spanY

            val childWidthSpec = MeasureSpec.makeMeasureSpec(childW, MeasureSpec.EXACTLY)
            val childHeightSpec = MeasureSpec.makeMeasureSpec(childH, MeasureSpec.EXACTLY)
            child.measure(childWidthSpec, childHeightSpec)
        }

        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val padTop = if (pageIndex == 0) DensityUtil.dpToPx(context, 16f) else 0

        var clockBottomPx = 0
        var firstRowTopPx = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i) ?: continue
            if (child.visibility == GONE) continue

            val lp = child.layoutParams as LayoutParams
            val left = paddingLeft + (lp.cellX * cellWidth)
            val top = padTop + (lp.cellY * cellHeight)

            child.layout(left, top, left + child.measuredWidth, top + child.measuredHeight)

            if (pageIndex == 0) {
                if (child is ClockDateCardView) {
                    val bottomInset = DensityUtil.dpToPx(context, 14f)
                    clockBottomPx = top + child.measuredHeight - bottomInset
                } else if (lp.cellY == 1 && firstRowTopPx == 0) {
                    val topInset = DensityUtil.dpToPx(context, 8f)
                    firstRowTopPx = top + topInset
                }
            }
        }

        if (pageIndex == 0 && clockBottomPx > 0 && firstRowTopPx > 0) {
            val gapPx = firstRowTopPx - clockBottomPx
            val density = context.resources.displayMetrics.density
            val gapDp = (gapPx / density).toInt()
            Log.d("ClockSpacing", "ClockVisualBottom=$clockBottomPx FirstWorkspaceVisualTop=$firstRowTopPx GapPx=$gapPx GapDp=$gapDp")
        }
    }

    fun pointToCell(localX: Float, localY: Float): Pair<Int, Int> {
        val padTop = if (pageIndex == 0) DensityUtil.dpToPx(context, 16f) else 0
        val xInBounds = (localX - paddingLeft).coerceIn(0f, (width - paddingLeft - paddingRight).toFloat())
        val yInBounds = (localY - padTop).coerceIn(0f, (height - padTop - paddingBottom).toFloat())

        val cX = (xInBounds / cellWidth).toInt().coerceIn(0, countX - 1)
        val cY = (yInBounds / cellHeight).toInt().coerceIn(0, countY - 1)
        return Pair(cX, cY)
    }

    fun isCellOccupied(cellX: Int, cellY: Int, spanX: Int, spanY: Int, ignoreView: View? = null): Boolean {
        if (cellX < 0 || cellY < 0 || (cellX + spanX) > countX || (cellY + spanY) > countY) {
            return true
        }

        for (i in 0 until childCount) {
            val child = getChildAt(i) ?: continue
            if (child == ignoreView || child.visibility == GONE) continue

            val lp = child.layoutParams as LayoutParams
            val intersectsX = cellX < (lp.cellX + lp.spanX) && (cellX + spanX) > lp.cellX
            val intersectsY = cellY < (lp.cellY + lp.spanY) && (cellY + spanY) > lp.cellY
            if (intersectsX && intersectsY) {
                return true
            }
        }
        return false
    }

    fun getItemAtCell(cellX: Int, cellY: Int): View? {
        for (i in 0 until childCount) {
            val child = getChildAt(i) ?: continue
            if (child.visibility == GONE) continue

            val lp = child.layoutParams as LayoutParams
            if (cellX >= lp.cellX && cellX < (lp.cellX + lp.spanX) &&
                cellY >= lp.cellY && cellY < (lp.cellY + lp.spanY)
            ) {
                return child
            }
        }
        return null
    }

    fun findNearestVacantCell(spanX: Int = 1, spanY: Int = 1): Pair<Int, Int>? {
        val startY = if (pageIndex == 0) 1 else 0
        for (y in startY until countY) {
            for (x in 0 until countX) {
                if (!isCellOccupied(x, y, spanX, spanY)) {
                    return Pair(x, y)
                }
            }
        }
        return null
    }

    fun visualRectForItem(cellX: Int, cellY: Int, spanX: Int = 1, spanY: Int = 1): RectF {
        val padTop = if (pageIndex == 0) DensityUtil.dpToPx(context, 16f) else 0
        val left = paddingLeft + (cellX * cellWidth).toFloat()
        val top = padTop + (cellY * cellHeight).toFloat()
        val right = left + (spanX * cellWidth).toFloat()
        val bottom = top + (spanY * cellHeight).toFloat()

        if (spanX > 1 || spanY > 1) {
            val sideMargin = DensityUtil.dpToPx(context, 14f).toFloat()
            val widgetHeight = DensityUtil.dpToPx(context, 110f).toFloat()
            val widgetTop = top + DensityUtil.dpToPx(context, 8f).toFloat()
            return RectF(left + sideMargin, widgetTop, right - sideMargin, widgetTop + widgetHeight)
        }

        val centerX = (left + right) / 2f
        val centerY = (top + bottom) / 2f
        val halfWidth = DensityUtil.dpToPx(context, 34f).toFloat()
        val halfHeight = DensityUtil.dpToPx(context, 38f).toFloat()
        return RectF(centerX - halfWidth, centerY - halfHeight, centerX + halfWidth, centerY + halfHeight)
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        dragHighlightRect?.let { rect ->
            val corner = DensityUtil.dpToPx(context, 16f).toFloat()
            canvas.drawRoundRect(rect, corner, corner, fillPaint)
            canvas.drawRoundRect(rect, corner, corner, strokePaint)
        }
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean = p is LayoutParams
    override fun generateLayoutParams(p: ViewGroup.LayoutParams?): ViewGroup.LayoutParams = LayoutParams(p)
    override fun generateLayoutParams(attrs: AttributeSet?): ViewGroup.LayoutParams = LayoutParams(context, attrs)
    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams = LayoutParams(1, 1, 0, 0)

    class LayoutParams : ViewGroup.LayoutParams {
        var spanX: Int = 1
        var spanY: Int = 1
        var cellX: Int = 0
        var cellY: Int = 0

        constructor(width: Int, height: Int) : super(width, height)
        constructor(spanX: Int, spanY: Int, cellX: Int, cellY: Int) : super(WRAP_CONTENT, WRAP_CONTENT) {
            this.spanX = spanX
            this.spanY = spanY
            this.cellX = cellX
            this.cellY = cellY
        }

        constructor(source: ViewGroup.LayoutParams?) : super(source) {
            if (source is LayoutParams) {
                this.spanX = source.spanX
                this.spanY = source.spanY
                this.cellX = source.cellX
                this.cellY = source.cellY
            }
        }

        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    }
}
