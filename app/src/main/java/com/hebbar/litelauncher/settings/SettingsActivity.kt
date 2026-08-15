package com.hebbar.litelauncher.settings

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.hebbar.litelauncher.backup.BackupManager
import com.hebbar.litelauncher.icons.IconPackManager
import com.hebbar.litelauncher.model.GestureAction
import com.hebbar.litelauncher.model.GestureType
import com.hebbar.litelauncher.model.ThemeMode
import com.hebbar.litelauncher.persistence.PreferencesManager
import com.hebbar.litelauncher.util.DensityUtil
import com.hebbar.litelauncher.workspace.WorkspaceRepository

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: PreferencesManager
    private lateinit var backupManager: BackupManager

    private val categories = listOf(
        "Home Screen",
        "App Drawer",
        "Dock",
        "Folders",
        "Gestures",
        "Appearance",
        "Icon Pack",
        "Backup & Restore",
        "Default Launcher",
        "About"
    )

    private var detailContainer: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PreferencesManager(this)
        backupManager = BackupManager(this, prefs, WorkspaceRepository(this))

        val isTablet2Pane = DensityUtil.isTablet(this) && DensityUtil.isLandscape(this)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#12121B"))
        }

        val masterListView = ListView(this).apply {
            adapter = ArrayAdapter(this@SettingsActivity, android.R.layout.simple_list_item_1, categories)
            setBackgroundColor(Color.parseColor("#1E1E2E"))
        }

        if (isTablet2Pane) {
            // Master-Detail 2 Pane Layout
            rootLayout.addView(masterListView, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.35f))

            detailContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(DensityUtil.dpToPx(context, 24f), DensityUtil.dpToPx(context, 24f), DensityUtil.dpToPx(context, 24f), DensityUtil.dpToPx(context, 24f))
            }

            val scrollView = ScrollView(this).apply {
                addView(detailContainer)
            }
            rootLayout.addView(scrollView, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.65f))

            masterListView.setOnItemClickListener { _, _, position, _ ->
                showCategoryDetail(position)
            }
            showCategoryDetail(0)
        } else {
            rootLayout.addView(masterListView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT))
            masterListView.setOnItemClickListener { _, _, position, _ ->
                showCategoryDialog(position)
            }
        }

        setContentView(rootLayout)
    }

    private fun showCategoryDetail(index: Int) {
        val container = detailContainer ?: return
        container.removeAllViews()

        val titleView = TextView(this).apply {
            text = categories[index]
            textSize = 22f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, DensityUtil.dpToPx(context, 16f))
        }
        container.addView(titleView)

        when (index) {
            0 -> buildHomeScreenSettings(container)
            1 -> buildAppDrawerSettings(container)
            2 -> buildDockSettings(container)
            3 -> buildFolderSettings(container)
            4 -> buildGestureSettings(container)
            5 -> buildAppearanceSettings(container)
            6 -> buildIconPackSettings(container)
            7 -> buildBackupSettings(container)
            8 -> openDefaultLauncherSettings()
            9 -> buildAboutSettings(container)
        }
    }

    private fun showCategoryDialog(index: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(categories[index])

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(DensityUtil.dpToPx(context, 16f), DensityUtil.dpToPx(context, 16f), DensityUtil.dpToPx(context, 16f), DensityUtil.dpToPx(context, 16f))
        }

        when (index) {
            0 -> buildHomeScreenSettings(container)
            1 -> buildAppDrawerSettings(container)
            2 -> buildDockSettings(container)
            3 -> buildFolderSettings(container)
            4 -> buildGestureSettings(container)
            5 -> buildAppearanceSettings(container)
            6 -> buildIconPackSettings(container)
            7 -> buildBackupSettings(container)
            8 -> { openDefaultLauncherSettings(); return }
            9 -> buildAboutSettings(container)
        }

        builder.setView(container)
        builder.setPositiveButton("Close", null)
        builder.show()
    }

    private fun buildHomeScreenSettings(container: LinearLayout) {
        addSettingButton(container, "Grid Size: ${prefs.desktopColumns} x ${prefs.desktopRows}") {
            val options = arrayOf("4 x 5", "4 x 6", "5 x 5", "5 x 6", "6 x 6", "7 x 7")
            AlertDialog.Builder(this).setItems(options) { _, which ->
                when (which) {
                    0 -> { prefs.desktopColumns = 4; prefs.desktopRows = 5 }
                    1 -> { prefs.desktopColumns = 4; prefs.desktopRows = 6 }
                    2 -> { prefs.desktopColumns = 5; prefs.desktopRows = 5 }
                    3 -> { prefs.desktopColumns = 5; prefs.desktopRows = 6 }
                    4 -> { prefs.desktopColumns = 6; prefs.desktopRows = 6 }
                    5 -> { prefs.desktopColumns = 7; prefs.desktopRows = 7 }
                }
                Toast.makeText(this, "Grid updated", Toast.LENGTH_SHORT).show()
            }.show()
        }

        addSettingButton(container, "Icon Size: ${prefs.iconSizeDp} dp") {
            val options = arrayOf("48 dp", "56 dp", "64 dp", "72 dp")
            AlertDialog.Builder(this).setItems(options) { _, which ->
                val sizes = intArrayOf(48, 56, 64, 72)
                prefs.iconSizeDp = sizes[which]
                Toast.makeText(this, "Icon size updated", Toast.LENGTH_SHORT).show()
            }.show()
        }

        addSettingButton(container, "Show Labels: ${if (prefs.showLabels) "ON" else "OFF"}") {
            prefs.showLabels = !prefs.showLabels
            Toast.makeText(this, "Labels updated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildAppDrawerSettings(container: LinearLayout) {
        addSettingButton(container, "Drawer Columns: ${prefs.drawerColumns}") {
            val options = arrayOf("4", "5", "6", "7", "8")
            AlertDialog.Builder(this).setItems(options) { _, which ->
                prefs.drawerColumns = which + 4
            }.show()
        }

        addSettingButton(container, "Drawer Style: ${if (prefs.drawerIsGrid) "Grid" else "List"}") {
            prefs.drawerIsGrid = !prefs.drawerIsGrid
        }

        addSettingButton(container, "Sort Mode") {
            val options = arrayOf("Alphabetical", "Installation Date", "Most Launched", "Recently Launched")
            AlertDialog.Builder(this).setSingleChoiceItems(options, prefs.drawerSortMode) { dialog, which ->
                prefs.drawerSortMode = which
                dialog.dismiss()
            }.show()
        }
    }

    private fun buildDockSettings(container: LinearLayout) {
        addSettingButton(container, "Dock Enabled: ${if (prefs.dockEnabled) "ON" else "OFF"}") {
            prefs.dockEnabled = !prefs.dockEnabled
        }
        addSettingButton(container, "Dock Icons: ${prefs.dockColumns}") {
            val options = arrayOf("4", "5", "6", "7")
            AlertDialog.Builder(this).setItems(options) { _, which ->
                prefs.dockColumns = which + 4
            }.show()
        }
    }

    private fun buildFolderSettings(container: LinearLayout) {
        addSettingInfo(container, "Folder Preview: 2x2 Grid Thumbnail")
    }

    private fun buildGestureSettings(container: LinearLayout) {
        addSettingButton(container, "Swipe Up: ${prefs.getGestureAction(GestureType.SWIPE_UP)}") {
            showGestureActionPicker(GestureType.SWIPE_UP)
        }
        addSettingButton(container, "Swipe Down: ${prefs.getGestureAction(GestureType.SWIPE_DOWN)}") {
            showGestureActionPicker(GestureType.SWIPE_DOWN)
        }
        addSettingButton(container, "Double Tap: ${prefs.getGestureAction(GestureType.DOUBLE_TAP)}") {
            showGestureActionPicker(GestureType.DOUBLE_TAP)
        }
    }

    private fun showGestureActionPicker(type: GestureType) {
        val actions = GestureAction.values()
        val names = actions.map { it.name }.toTypedArray()
        AlertDialog.Builder(this).setItems(names) { _, which ->
            prefs.setGestureAction(type, actions[which])
        }.show()
    }

    private fun buildAppearanceSettings(container: LinearLayout) {
        addSettingButton(container, "Theme: ${prefs.themeMode}") {
            val modes = ThemeMode.values()
            val names = modes.map { it.name }.toTypedArray()
            AlertDialog.Builder(this).setItems(names) { _, which ->
                prefs.themeMode = modes[which]
                recreate()
            }.show()
        }
    }

    private fun buildIconPackSettings(container: LinearLayout) {
        val iconPackManager = IconPackManager(this)
        val packs = iconPackManager.getAvailableIconPacks()
        if (packs.isEmpty()) {
            addSettingInfo(container, "No third-party icon packs detected. System icons in use.")
        } else {
            val names = mutableListOf("System Default").apply {
                addAll(packs.map { it.label })
            }.toTypedArray()

            addSettingButton(container, "Selected Pack: ${prefs.selectedIconPack ?: "System Default"}") {
                AlertDialog.Builder(this).setItems(names) { _, which ->
                    prefs.selectedIconPack = if (which == 0) null else packs[which - 1].packageName
                    Toast.makeText(this, "Icon pack updated", Toast.LENGTH_SHORT).show()
                }.show()
            }
        }
    }

    private fun buildBackupSettings(container: LinearLayout) {
        addSettingButton(container, "Export Layout & Settings Backup") {
            val jsonStr = backupManager.createBackupJson()
            val file = getFileStreamPath("lite_launcher_backup.json")
            file.writeText(jsonStr)
            Toast.makeText(this, "Backup saved to ${file.name}", Toast.LENGTH_LONG).show()
        }
        addSettingButton(container, "Restore Backup") {
            val file = getFileStreamPath("lite_launcher_backup.json")
            if (file.exists()) {
                val success = backupManager.restoreBackupJson(file.readText())
                if (success) {
                    Toast.makeText(this, "Backup restored!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Invalid backup format", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No backup file found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openDefaultLauncherSettings() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val roleManager = getSystemService(android.app.role.RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME)) {
                    val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME)
                    startActivity(intent)
                    return
                }
            }
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            } catch (ex: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }

    private fun buildAboutSettings(container: LinearLayout) {
        addSettingInfo(container, "Lite Launcher v1.0\nHigh-Performance, Private, Extremely Lightweight Launcher.\nZero Ads | Zero Trackers | Kotlin Native XML")
    }

    private fun addSettingButton(container: LinearLayout, text: String, onClick: () -> Unit) {
        val tv = TextView(this).apply {
            this.text = text
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(0, DensityUtil.dpToPx(context, 12f), 0, DensityUtil.dpToPx(context, 12f))
            setOnClickListener { onClick() }
        }
        container.addView(tv)
    }

    private fun addSettingInfo(container: LinearLayout, text: String) {
        val tv = TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(Color.GRAY)
            setPadding(0, DensityUtil.dpToPx(context, 8f), 0, DensityUtil.dpToPx(context, 8f))
        }
        container.addView(tv)
    }
}
