package com.hebbar.litelauncher.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.hebbar.litelauncher.model.WorkspaceItem
import com.hebbar.litelauncher.util.DensityUtil

class WidgetResizeFrame @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var widgetItem: WorkspaceItem.WidgetItem? = null
    var onResizeListener: ((newSpanX: Int, newSpanY: Int) -> Unit)? = null

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF3D5AFE")
        style = Paint.Style.STROKE
        strokeWidth = DensityUtil.dpToPx(context, 2f).toFloat()
    }

    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF3D5AFE")
        style = Paint.Style.FILL
    }

    private val boundsRect = RectF()
    private val handleRadius = DensityUtil.dpToPx(context, 8f).toFloat()

    init {
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        boundsRect.set(
            handleRadius,
            handleRadius,
            width.toFloat() - handleRadius,
            height.toFloat() - handleRadius
        )

        canvas.drawRect(boundsRect, borderPaint)

        // Mid-edge handles
        canvas.drawCircle(boundsRect.centerX(), boundsRect.top, handleRadius, handlePaint)
        canvas.drawCircle(boundsRect.centerX(), boundsRect.bottom, handleRadius, handlePaint)
        canvas.drawCircle(boundsRect.left, boundsRect.centerY(), handleRadius, handlePaint)
        canvas.drawCircle(boundsRect.right, boundsRect.centerY(), handleRadius, handlePaint)
    }
}
