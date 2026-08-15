package com.hebbar.litelauncher.folders

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import com.hebbar.litelauncher.icons.AdaptiveIconHelper
import com.hebbar.litelauncher.model.WorkspaceItem
import com.hebbar.litelauncher.persistence.PreferencesManager
import com.hebbar.litelauncher.util.DensityUtil
import com.hebbar.litelauncher.workspace.WorkspaceItemView

class FolderDialog(
    context: Context,
    private val folderItem: WorkspaceItem.FolderItem,
    private val iconHelper: AdaptiveIconHelper,
    private val prefs: PreferencesManager,
    private val onAppClick: (WorkspaceItem.AppItem) -> Unit,
    private val onAppLongClick: (WorkspaceItem.AppItem) -> Unit,
    private val onTitleChanged: (String) -> Unit
) : Dialog(context) {

    private lateinit var titleEdit: EditText
    private lateinit var gridLayout: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val dialogBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = DensityUtil.dpToPx(context, 24f).toFloat()
            setColor(Color.parseColor("#F0181824"))
        }

        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                DensityUtil.dpToPx(context, 16f),
                DensityUtil.dpToPx(context, 16f),
                DensityUtil.dpToPx(context, 16f),
                DensityUtil.dpToPx(context, 20f)
            )
            background = dialogBg
            elevation = DensityUtil.dpToPx(context, 20f).toFloat()
        }

        titleEdit = EditText(context).apply {
            setText(folderItem.title)
            textSize = 19f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.TRANSPARENT)
            setHintTextColor(Color.GRAY)
            hint = "Folder Title"
            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val newTitle = text.toString().trim()
                    if (newTitle.isNotEmpty()) {
                        folderItem.title = newTitle
                        onTitleChanged(newTitle)
                    }
                }
            }
        }

        rootLayout.addView(
            titleEdit,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = DensityUtil.dpToPx(context, 12f)
            }
        )

        gridLayout = GridLayout(context).apply {
            columnCount = 3
            useDefaultMargins = false
        }

        populateFolderApps()

        val scrollView = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            addView(
                gridLayout,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        rootLayout.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                DensityUtil.dpToPx(context, 280f)
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        )

        setContentView(rootLayout)

        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.CENTER)
        }
    }

    private fun populateFolderApps() {
        gridLayout.removeAllViews()
        val itemMarginH = DensityUtil.dpToPx(context, 4f)
        val itemMarginV = DensityUtil.dpToPx(context, 6f)

        for (app in folderItem.items) {
            val itemView = WorkspaceItemView(context).apply {
                bind(app, iconHelper, prefs)
                setOnClickListener {
                    dismiss()
                    onAppClick(app)
                }
                setOnLongClickListener {
                    dismiss()
                    onAppLongClick(app)
                    true
                }
            }

            val params = GridLayout.LayoutParams().apply {
                width = DensityUtil.dpToPx(context, 78f)
                height = DensityUtil.dpToPx(context, 92f)
                setMargins(itemMarginH, itemMarginV, itemMarginH, itemMarginV)
            }
            gridLayout.addView(itemView, params)
        }
    }
}
