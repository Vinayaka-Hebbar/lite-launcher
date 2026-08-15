package com.hebbar.litelauncher.drawer

import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hebbar.litelauncher.icons.AdaptiveIconHelper
import com.hebbar.litelauncher.model.LaunchableApp
import com.hebbar.litelauncher.persistence.PreferencesManager
import com.hebbar.litelauncher.util.DensityUtil

class AppAdapter(
    private val context: Context,
    private val iconHelper: AdaptiveIconHelper,
    private val prefs: PreferencesManager,
    private val onAppClickListener: (LaunchableApp) -> Unit,
    private val onAppLongClickListener: (LaunchableApp, View) -> Boolean
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    var apps: List<LaunchableApp> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var isGridMode: Boolean = true
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    class AppViewHolder(val container: LinearLayout, val iconView: ImageView, val labelView: TextView) :
        RecyclerView.ViewHolder(container)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val container = LinearLayout(context).apply {
            clipChildren = false
            clipToPadding = false
        }

        val iconView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        val labelView = TextView(context).apply {
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            gravity = Gravity.CENTER
        }

        if (isGridMode) {
            container.orientation = LinearLayout.VERTICAL
            container.gravity = Gravity.CENTER_HORIZONTAL
            val padH = DensityUtil.dpToPx(context, 4f)
            val padV = DensityUtil.dpToPx(context, 10f)
            container.setPadding(padH, padV, padH, padV)

            val iconSizePx = DensityUtil.dpToPx(context, 50f)
            container.addView(iconView, LinearLayout.LayoutParams(iconSizePx, iconSizePx).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            })
            container.addView(labelView, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = DensityUtil.dpToPx(context, 6f)
            })
        } else {
            container.orientation = LinearLayout.HORIZONTAL
            container.gravity = Gravity.CENTER_VERTICAL
            val padH = DensityUtil.dpToPx(context, 16f)
            val padV = DensityUtil.dpToPx(context, 10f)
            container.setPadding(padH, padV, padH, padV)

            val iconSizePx = DensityUtil.dpToPx(context, 48f)
            container.addView(iconView, LinearLayout.LayoutParams(iconSizePx, iconSizePx).apply {
                gravity = Gravity.CENTER_VERTICAL
            })
            container.addView(labelView, LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                leftMargin = DensityUtil.dpToPx(context, 16f)
            })
            labelView.gravity = Gravity.START
        }

        return AppViewHolder(container, iconView, labelView)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]

        val iconSizePx = DensityUtil.dpToPx(context, 50f)
        holder.iconView.layoutParams = (holder.iconView.layoutParams as LinearLayout.LayoutParams).apply {
            width = iconSizePx
            height = iconSizePx
        }

        holder.labelView.text = app.effectiveLabel
        holder.iconView.setImageDrawable(iconHelper.getAppIcon(app.packageName, app.activityName))

        holder.container.setOnClickListener {
            onAppClickListener(app)
        }

        holder.container.setOnLongClickListener { view ->
            onAppLongClickListener(app, view)
        }
    }

    override fun getItemCount(): Int = apps.size
}
