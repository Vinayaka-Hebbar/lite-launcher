package com.hebbar.litelauncher.workspace

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.hebbar.litelauncher.folders.FolderPreviewDrawable
import com.hebbar.litelauncher.icons.AdaptiveIconHelper
import com.hebbar.litelauncher.model.WorkspaceItem
import com.hebbar.litelauncher.persistence.PreferencesManager
import com.hebbar.litelauncher.util.DensityUtil

class WorkspaceItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    val iconView: ImageView
    val labelView: TextView

    var item: WorkspaceItem? = null
        private set

    var hasNotificationBadge: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        clipChildren = false
        clipToPadding = false

        val iconSizeDp = 48f
        val iconSizePx = DensityUtil.dpToPx(context, iconSizeDp)

        iconView = ImageView(context).apply {
            layoutParams = LayoutParams(iconSizePx, iconSizePx).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        labelView = TextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = DensityUtil.dpToPx(context, 3f)
            }
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setLineSpacing(0f, 1.05f)
            textSize = 12f
            setTextColor(Color.WHITE)
            setShadowLayer(3f, 0f, 1f, Color.parseColor("#A0000000"))
            gravity = Gravity.CENTER
        }

        addView(iconView)
        addView(labelView)
    }

    fun bind(
        workspaceItem: WorkspaceItem,
        iconHelper: AdaptiveIconHelper,
        prefs: PreferencesManager,
        isDock: Boolean = false
    ) {
        this.item = workspaceItem

        gravity = Gravity.CENTER
        setPadding(0, 0, 0, 0)

        val targetIconDp = if (isDock) 46f else prefs.iconSizeDp.toFloat()
        val iconSizePx = DensityUtil.dpToPx(context, targetIconDp)
        iconView.layoutParams = (iconView.layoutParams as LayoutParams).apply {
            width = iconSizePx
            height = iconSizePx
            gravity = Gravity.CENTER
        }

        labelView.visibility = if (prefs.showLabels && !isDock) VISIBLE else GONE
        labelView.textSize = prefs.labelSizeSp

        when (workspaceItem) {
            is WorkspaceItem.AppItem -> {
                labelView.text = workspaceItem.customLabel ?: getAppName(workspaceItem.packageName, workspaceItem.activityName)
                val iconDrawable = iconHelper.getAppIcon(workspaceItem.packageName, workspaceItem.activityName)
                iconView.setImageDrawable(iconDrawable)
            }
            is WorkspaceItem.FolderItem -> {
                labelView.text = workspaceItem.title
                val previewDrawable = FolderPreviewDrawable(context, workspaceItem.items, iconHelper)
                iconView.setImageDrawable(previewDrawable)
            }
            is WorkspaceItem.ShortcutItem -> {
                labelView.text = workspaceItem.label
                val iconDrawable = iconHelper.getAppIcon(workspaceItem.packageName, "")
                iconView.setImageDrawable(iconDrawable)
            }
            is WorkspaceItem.WidgetItem -> {
                // Widgets handled separately
            }
            is WorkspaceItem.ClockDateCardItem -> {
                // Clock handled separately
            }
        }
    }

    private fun getAppName(packageName: String, activityName: String): String {
        return try {
            val pm = context.packageManager
            val info = pm.getActivityInfo(android.content.ComponentName(packageName, activityName), 0)
            info.loadLabel(pm).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.')
        }
    }

    fun getVisualBoundsOnScreen(): Rect {
        val loc = IntArray(2)
        iconView.getLocationOnScreen(loc)
        return Rect(loc[0], loc[1], loc[0] + iconView.width, loc[1] + iconView.height)
    }
}
