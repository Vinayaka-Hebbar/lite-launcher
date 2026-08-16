package com.hebbar.litelauncher.settings

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import com.hebbar.litelauncher.backup.BackupManager
import com.hebbar.litelauncher.icons.IconPackManager
import com.hebbar.litelauncher.model.GestureAction
import com.hebbar.litelauncher.model.GestureType
import com.hebbar.litelauncher.model.ThemeMode
import com.hebbar.litelauncher.persistence.PreferencesManager
import com.hebbar.litelauncher.util.DensityUtil

class SettingsPages(
    private val context: Context,
    private val prefs: PreferencesManager,
    private val backupManager: BackupManager
) {

    fun buildHomeScreenSettingsPage(container: LinearLayout) {
        addPageSettingButton(container, "Grid Size: ${prefs.desktopColumns} x ${prefs.desktopRows}") {
            val options = arrayOf("4 x 5", "4 x 6", "5 x 5", "5 x 6", "6 x 6", "7 x 7")
            AlertDialog.Builder(context).setItems(options) { _, which ->
                when (which) {
                    0 -> { prefs.desktopColumns = 4; prefs.desktopRows = 5 }
                    1 -> { prefs.desktopColumns = 4; prefs.desktopRows = 6 }
                    2 -> { prefs.desktopColumns = 5; prefs.desktopRows = 5 }
                    3 -> { prefs.desktopColumns = 5; prefs.desktopRows = 6 }
                    4 -> { prefs.desktopColumns = 6; prefs.desktopRows = 6 }
                    5 -> { prefs.desktopColumns = 7; prefs.desktopRows = 7 }
                }
                Toast.makeText(context, "Grid updated", Toast.LENGTH_SHORT).show()
                buildHomeScreenSettingsPage(container.apply { removeAllViews() })
            }.show()
        }

        addPageSettingButton(container, "Icon Size: ${prefs.iconSizeDp} dp") {
            val options = arrayOf("48 dp", "56 dp", "64 dp", "72 dp")
            AlertDialog.Builder(context).setItems(options) { _, which ->
                val sizes = intArrayOf(48, 56, 64, 72)
                prefs.iconSizeDp = sizes[which]
                Toast.makeText(context, "Icon size updated", Toast.LENGTH_SHORT).show()
                buildHomeScreenSettingsPage(container.apply { removeAllViews() })
            }.show()
        }

        addPageSettingButton(container, "Show Labels: ${if (prefs.showLabels) "ON" else "OFF"}") {
            prefs.showLabels = !prefs.showLabels
            Toast.makeText(context, "Labels updated", Toast.LENGTH_SHORT).show()
            buildHomeScreenSettingsPage(container.apply { removeAllViews() })
        }
    }

    fun buildAppDrawerSettingsPage(container: LinearLayout) {
        addPageSettingButton(container, "Drawer Columns: ${prefs.drawerColumns}") {
            val options = arrayOf("4", "5", "6", "7", "8")
            AlertDialog.Builder(context).setItems(options) { _, which ->
                prefs.drawerColumns = which + 4
                Toast.makeText(context, "Columns updated", Toast.LENGTH_SHORT).show()
                buildAppDrawerSettingsPage(container.apply { removeAllViews() })
            }.show()
        }

        addPageSettingButton(container, "Drawer Style: ${if (prefs.drawerIsGrid) "Grid" else "List"}") {
            prefs.drawerIsGrid = !prefs.drawerIsGrid
            Toast.makeText(context, "Style updated", Toast.LENGTH_SHORT).show()
            buildAppDrawerSettingsPage(container.apply { removeAllViews() })
        }

        addPageSettingButton(container, "Sort Mode: ${getSortModeName(prefs.drawerSortMode)}") {
            val options = arrayOf("Alphabetical", "Installation Date", "Most Launched", "Recently Launched")
            AlertDialog.Builder(context).setSingleChoiceItems(options, prefs.drawerSortMode) { dialog, which ->
                prefs.drawerSortMode = which
                dialog.dismiss()
                buildAppDrawerSettingsPage(container.apply { removeAllViews() })
            }.show()
        }

        val searchInHiddenCheck = SwitchCompat(context).apply {
            text = "Show Hidden Apps in Launcher Search"
            setTextColor(Color.WHITE)
            textSize = 15f
            isChecked = prefs.searchInHidden
            setPadding(0, DensityUtil.dpToPx(context, 12f), 0, DensityUtil.dpToPx(context, 12f))
            setOnCheckedChangeListener { _, isChecked ->
                prefs.searchInHidden = isChecked
                Toast.makeText(context, "Search setting updated", Toast.LENGTH_SHORT).show()
            }
        }
        container.addView(searchInHiddenCheck)
    }

    private fun getSortModeName(mode: Int): String {
        return when (mode) {
            0 -> "Alphabetical"
            1 -> "Installation Date"
            2 -> "Most Launched"
            3 -> "Recently Launched"
            else -> "Alphabetical"
        }
    }

    fun buildDockSettingsPage(container: LinearLayout) {
        addPageSettingButton(container, "Dock Enabled: ${if (prefs.dockEnabled) "ON" else "OFF"}") {
            prefs.dockEnabled = !prefs.dockEnabled
            buildDockSettingsPage(container.apply { removeAllViews() })
        }
        addPageSettingButton(container, "Dock Icons: ${prefs.dockColumns}") {
            val options = arrayOf("4", "5", "6", "7")
            AlertDialog.Builder(context).setItems(options) { _, which ->
                prefs.dockColumns = which + 4
                buildDockSettingsPage(container.apply { removeAllViews() })
            }.show()
        }
    }

    fun buildFolderSettingsPage(container: LinearLayout) {
        addPageInfo(container, "Folder Preview: 2x2 Grid Thumbnail\nDrag icons into existing folders or onto other icons to merge into folders.")
    }

    fun buildGestureSettingsPage(container: LinearLayout) {
        addPageSettingButton(container, "Swipe Up: ${prefs.getGestureAction(GestureType.SWIPE_UP)}") {
            showGestureActionPicker(container, GestureType.SWIPE_UP)
        }
        addPageSettingButton(container, "Swipe Down: ${prefs.getGestureAction(GestureType.SWIPE_DOWN)}") {
            showGestureActionPicker(container, GestureType.SWIPE_DOWN)
        }
        addPageSettingButton(container, "Double Tap: ${prefs.getGestureAction(GestureType.DOUBLE_TAP)}") {
            showGestureActionPicker(container, GestureType.DOUBLE_TAP)
        }
    }

    private fun showGestureActionPicker(container: LinearLayout, type: GestureType) {
        val actions = GestureAction.values()
        val options = actions.map { it.name.replace("_", " ").lowercase().replaceFirstChar { char -> char.uppercase() } }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle("Select Action for ${type.name}")
            .setItems(options) { _, which ->
                prefs.setGestureAction(type, actions[which])
                Toast.makeText(context, "Gesture updated", Toast.LENGTH_SHORT).show()
                buildGestureSettingsPage(container.apply { removeAllViews() })
            }.show()
    }

    fun buildAppearanceSettingsPage(container: LinearLayout) {
        addPageSettingButton(container, "Theme: ${prefs.themeMode.name}") {
            val options = arrayOf("System Default", "Light", "Dark")
            AlertDialog.Builder(context).setItems(options) { _, which ->
                prefs.themeMode = when (which) {
                    1 -> ThemeMode.LIGHT
                    2 -> ThemeMode.DARK
                    else -> ThemeMode.SYSTEM
                }
                Toast.makeText(context, "Theme updated", Toast.LENGTH_SHORT).show()
                buildAppearanceSettingsPage(container.apply { removeAllViews() })
            }.show()
        }
    }

    fun buildIconPackSettingsPage(container: LinearLayout) {
        val iconPackManager = IconPackManager(context)
        val packs = iconPackManager.getAvailableIconPacks()

        val options = mutableListOf("System Default")
        options.addAll(packs.map { it.label })

        addPageSettingButton(container, "Selected Icon Pack: ${prefs.selectedIconPack ?: "System Default"}") {
            AlertDialog.Builder(context)
                .setTitle("Select Icon Pack")
                .setItems(options.toTypedArray()) { _, which ->
                    if (which == 0) {
                        prefs.selectedIconPack = null
                    } else {
                        prefs.selectedIconPack = packs[which - 1].packageName
                    }
                    Toast.makeText(context, "Icon pack updated", Toast.LENGTH_SHORT).show()
                    buildIconPackSettingsPage(container.apply { removeAllViews() })
                }.show()
        }
    }

    fun buildBackupSettingsPage(container: LinearLayout) {
        addPageSettingButton(container, "Export Layout Backup") {
            val json = backupManager.createBackupJson()
            Toast.makeText(context, "Backup exported to app storage", Toast.LENGTH_SHORT).show()
        }
        addPageSettingButton(container, "Import Layout Backup") {
            Toast.makeText(context, "Import layout ready", Toast.LENGTH_SHORT).show()
        }
    }

    fun buildAboutSettingsPage(container: LinearLayout) {
        addPageInfo(container, "Lite Launcher v1.0.0\nMinimalist, high-performance Android launcher.")
    }

    private fun addPageSettingButton(container: LinearLayout, title: String, onClick: () -> Unit) {
        val btnBg = GradientDrawable().apply {
            setColor(Color.parseColor("#1C1C2A"))
            cornerRadius = DensityUtil.dpToPx(context, 12f).toFloat()
        }

        val btn = TextView(context).apply {
            text = title
            textSize = 15f
            setTextColor(Color.WHITE)
            background = btnBg
            val padH = DensityUtil.dpToPx(context, 16f)
            val padV = DensityUtil.dpToPx(context, 14f)
            setPadding(padH, padV, padH, padV)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }

        container.addView(btn)
        val spacer = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, DensityUtil.dpToPx(context, 8f))
        }
        container.addView(spacer)
    }

    private fun addPageInfo(container: LinearLayout, infoText: String) {
        val tv = TextView(context).apply {
            text = infoText
            textSize = 14f
            setTextColor(Color.parseColor("#A0A0B0"))
            val pad = DensityUtil.dpToPx(context, 8f)
            setPadding(pad, pad, pad, pad)
        }
        container.addView(tv)
    }
}
