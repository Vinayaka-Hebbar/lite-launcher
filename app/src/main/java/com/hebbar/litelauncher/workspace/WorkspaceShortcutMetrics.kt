package com.hebbar.litelauncher.workspace

import android.content.Context
import com.hebbar.litelauncher.persistence.PreferencesManager
import com.hebbar.litelauncher.util.DensityUtil

data class WorkspaceShortcutMetrics(
    val iconSizePx: Int,
    val labelHeightPx: Int,
    val iconLabelGapPx: Int,
    val visualWidthPx: Int,
    val visualHeightPx: Int,
    val touchWidthPx: Int,
    val touchHeightPx: Int
) {
    companion object {
        fun create(context: Context, prefs: PreferencesManager, isDock: Boolean = false): WorkspaceShortcutMetrics {
            val iconDp = if (isDock) 46f else prefs.iconSizeDp.toFloat()
            val iconSizePx = DensityUtil.dpToPx(context, iconDp)

            if (isDock) {
                return WorkspaceShortcutMetrics(
                    iconSizePx = iconSizePx,
                    labelHeightPx = 0,
                    iconLabelGapPx = 0,
                    visualWidthPx = iconSizePx,
                    visualHeightPx = iconSizePx,
                    touchWidthPx = DensityUtil.dpToPx(context, 56f),
                    touchHeightPx = DensityUtil.dpToPx(context, 56f)
                )
            }

            val gapPx = DensityUtil.dpToPx(context, 4f)
            val labelHeightPx = DensityUtil.dpToPx(context, 20f)
            val visualWidthPx = DensityUtil.dpToPx(context, 65f)
            val visualHeightPx = iconSizePx + gapPx + labelHeightPx

            val touchWidthPx = DensityUtil.dpToPx(context, 68f)
            val touchHeightPx = visualHeightPx + DensityUtil.dpToPx(context, 4f)

            return WorkspaceShortcutMetrics(
                iconSizePx = iconSizePx,
                labelHeightPx = labelHeightPx,
                iconLabelGapPx = gapPx,
                visualWidthPx = visualWidthPx,
                visualHeightPx = visualHeightPx,
                touchWidthPx = touchWidthPx,
                touchHeightPx = touchHeightPx
            )
        }
    }
}
