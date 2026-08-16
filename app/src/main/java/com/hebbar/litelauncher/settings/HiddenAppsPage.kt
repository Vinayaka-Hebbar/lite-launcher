package com.hebbar.litelauncher.settings

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.core.widget.CompoundButtonCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hebbar.litelauncher.drawer.AppRepository
import com.hebbar.litelauncher.icons.AdaptiveIconHelper
import com.hebbar.litelauncher.icons.IconPackManager
import com.hebbar.litelauncher.model.LaunchableApp
import com.hebbar.litelauncher.persistence.PreferencesManager
import com.hebbar.litelauncher.util.DensityUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HiddenAppsPage(
    private val context: Context,
    private val prefs: PreferencesManager,
    private val lifecycleOwner: LifecycleOwner
) {

    private class HiddenAppAdapter(
        private val iconHelper: AdaptiveIconHelper,
        private val onToggleHidden: (String, Boolean) -> Unit
    ) : RecyclerView.Adapter<HiddenAppAdapter.ViewHolder>() {

        private var appsList = listOf<LaunchableApp>()
        private var hiddenSet = setOf<String>()

        fun updateData(newApps: List<LaunchableApp>, newHidden: Set<String>) {
            appsList = newApps
            hiddenSet = newHidden.toSet()
            notifyDataSetChanged()
        }

        class ViewHolder(
            val wrapper: LinearLayout,
            val cardRow: LinearLayout,
            val iconView: ImageView,
            val labelTv: TextView,
            val checkBox: AppCompatCheckBox
        ) : RecyclerView.ViewHolder(wrapper)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val ctx = parent.context
            val cardBg = GradientDrawable().apply {
                setColor(Color.parseColor("#12121C"))
                cornerRadius = DensityUtil.dpToPx(ctx, 14f).toFloat()
            }

            val cardRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = cardBg
                val padH = DensityUtil.dpToPx(ctx, 14f)
                val padV = DensityUtil.dpToPx(ctx, 12f)
                setPadding(padH, padV, padH, padV)
                isClickable = true
                isFocusable = true
            }

            val iconView = ImageView(ctx).apply {
                val size = DensityUtil.dpToPx(ctx, 42f)
                layoutParams = LinearLayout.LayoutParams(size, size)
            }

            val labelTv = TextView(ctx).apply {
                textSize = 15f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                setPadding(DensityUtil.dpToPx(ctx, 14f), 0, 0, 0)
            }

            val checkBox = AppCompatCheckBox(ctx).apply {
                val states = arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(-android.R.attr.state_checked)
                )
                val colors = intArrayOf(
                    Color.parseColor("#6366F1"),
                    Color.parseColor("#7E7E94")
                )
                CompoundButtonCompat.setButtonTintList(this, ColorStateList(states, colors))
            }

            cardRow.addView(iconView)
            cardRow.addView(labelTv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            cardRow.addView(checkBox)

            val wrapper = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                addView(cardRow)
                val spacer = View(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, DensityUtil.dpToPx(ctx, 6f))
                }
                addView(spacer)
            }

            return ViewHolder(wrapper, cardRow, iconView, labelTv, checkBox)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = appsList[position]
            holder.labelTv.text = app.label
            holder.iconView.setImageDrawable(iconHelper.getAppIcon(app.packageName, app.activityName))

            holder.checkBox.setOnCheckedChangeListener(null)
            val isHidden = hiddenSet.contains(app.packageName)
            holder.checkBox.isChecked = isHidden

            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                onToggleHidden(app.packageName, isChecked)
            }

            holder.cardRow.setOnClickListener {
                holder.checkBox.toggle()
            }
        }

        override fun getItemCount(): Int = appsList.size
    }

    fun build(container: LinearLayout) {
        val repository = AppRepository(context)
        val iconHelper = AdaptiveIconHelper(context, prefs, IconPackManager(context))

        val currentHidden = prefs.getHiddenApps().toMutableSet()
        var showOnlyHidden = false

        // Search Bar with Pill Background (Fixed Header)
        val searchBg = GradientDrawable().apply {
            setColor(Color.parseColor("#161622"))
            cornerRadius = DensityUtil.dpToPx(context, 14f).toFloat()
        }

        val searchEditText = EditText(context).apply {
            hint = "Search apps to hide..."
            setHintTextColor(Color.parseColor("#7E7E94"))
            setTextColor(Color.WHITE)
            textSize = 15f
            background = searchBg
            val padH = DensityUtil.dpToPx(context, 16f)
            val padV = DensityUtil.dpToPx(context, 12f)
            setPadding(padH, padV, padH, padV)
        }
        container.addView(searchEditText)

        // Filter Pills Row (Fixed Header)
        val filterRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val padV = DensityUtil.dpToPx(context, 14f)
            setPadding(0, padV, 0, padV)
        }

        val pillActiveBg = GradientDrawable().apply {
            setColor(Color.parseColor("#262638"))
            cornerRadius = DensityUtil.dpToPx(context, 20f).toFloat()
        }

        val pillInactiveBg = GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            cornerRadius = DensityUtil.dpToPx(context, 20f).toFloat()
        }

        val filterAllBtn = TextView(context).apply {
            text = "All Apps"
            textSize = 14f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            background = pillActiveBg
            val padH = DensityUtil.dpToPx(context, 14f)
            val padV = DensityUtil.dpToPx(context, 6f)
            setPadding(padH, padV, padH, padV)
        }

        val filterHiddenBtn = TextView(context).apply {
            text = "Hidden Only (${currentHidden.size})"
            textSize = 14f
            setTextColor(Color.parseColor("#8E8E9F"))
            background = pillInactiveBg
            val padH = DensityUtil.dpToPx(context, 14f)
            val padV = DensityUtil.dpToPx(context, 6f)
            setPadding(padH, padV, padH, padV)
        }

        val spacerPill = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(DensityUtil.dpToPx(context, 8f), 1)
        }

        filterRow.addView(filterAllBtn)
        filterRow.addView(spacerPill)
        filterRow.addView(filterHiddenBtn)
        container.addView(filterRow)

        // RecyclerView with weight 1f for native smooth scrolling
        val adapter = HiddenAppAdapter(iconHelper) { pkg, isHidden ->
            if (isHidden) {
                currentHidden.add(pkg)
            } else {
                currentHidden.remove(pkg)
            }
            prefs.setHiddenApps(currentHidden)
            filterHiddenBtn.text = "Hidden Only (${currentHidden.size})"
        }

        val recyclerView = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = adapter
            isNestedScrollingEnabled = true
        }
        container.addView(recyclerView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        lifecycleOwner.lifecycleScope.launch {
            val allApps = withContext(Dispatchers.IO) {
                repository.getInstalledApps().sortedBy { app -> app.label.lowercase() }
            }

            fun updateList(animateTransition: Boolean = false) {
                val query = searchEditText.text.toString().trim().lowercase()

                val filtered = allApps.filter { app ->
                    val matchesQuery = query.isEmpty() || app.label.lowercase().contains(query)
                    val matchesHidden = if (showOnlyHidden) currentHidden.contains(app.packageName) else true
                    matchesQuery && matchesHidden
                }

                filterHiddenBtn.text = "Hidden Only (${currentHidden.size})"
                filterAllBtn.text = "All Apps (${allApps.size})"

                if (animateTransition) {
                    val autoTransition = AutoTransition().apply {
                        duration = 180
                    }
                    TransitionManager.beginDelayedTransition(recyclerView, autoTransition)
                }

                adapter.updateData(filtered, currentHidden)
            }

            filterAllBtn.setOnClickListener {
                if (!showOnlyHidden) return@setOnClickListener
                showOnlyHidden = false
                filterAllBtn.setTextColor(Color.WHITE)
                filterAllBtn.background = pillActiveBg
                filterHiddenBtn.setTextColor(Color.parseColor("#8E8E9F"))
                filterHiddenBtn.background = pillInactiveBg
                updateList(animateTransition = true)
            }

            filterHiddenBtn.setOnClickListener {
                if (showOnlyHidden) return@setOnClickListener
                showOnlyHidden = true
                filterHiddenBtn.setTextColor(Color.WHITE)
                filterHiddenBtn.background = pillActiveBg
                filterAllBtn.setTextColor(Color.parseColor("#8E8E9F"))
                filterAllBtn.background = pillInactiveBg
                updateList(animateTransition = true)
            }

            searchEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    updateList(animateTransition = false)
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            updateList()
        }
    }
}
