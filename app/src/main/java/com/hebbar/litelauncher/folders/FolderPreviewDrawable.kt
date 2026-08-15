package com.hebbar.litelauncher.folders

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import com.hebbar.litelauncher.icons.AdaptiveIconHelper
import com.hebbar.litelauncher.model.WorkspaceItem
import com.hebbar.litelauncher.util.DensityUtil

class FolderPreviewDrawable(
    private val context: Context,
    private val folderApps: List<WorkspaceItem.AppItem>,
    private val iconHelper: AdaptiveIconHelper
) : Drawable() {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#25FFFFFF") // Subtle rounded translucent surface
        style = Paint.Style.FILL
    }

    private val bgRect = RectF()

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        if (bounds.isEmpty) return

        bgRect.set(bounds)
        val cornerRadius = bounds.width() * 0.28f
        canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, bgPaint)

        val itemsToDraw = folderApps.take(4)
        if (itemsToDraw.isEmpty()) return

        val padding = bounds.width() * 0.14f
        val availableWidth = bounds.width() - (padding * 2)
        val itemSize = availableWidth / 2f
        val itemGap = DensityUtil.dpToPx(context, 1.5f).toFloat()

        for (i in itemsToDraw.indices) {
            val app = itemsToDraw[i]
            val row = i / 2
            val col = i % 2

            val left = bounds.left + padding + (col * itemSize) + itemGap
            val top = bounds.top + padding + (row * itemSize) + itemGap
            val right = left + itemSize - (itemGap * 2)
            val bottom = top + itemSize - (itemGap * 2)

            val iconDrawable = iconHelper.getAppIcon(app.packageName, app.activityName)
            iconDrawable.setBounds(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
            iconDrawable.draw(canvas)
        }
    }

    override fun setAlpha(alpha: Int) {
        bgPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        bgPaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java", ReplaceWith("PixelFormat.TRANSLUCENT", "android.graphics.PixelFormat"))
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
