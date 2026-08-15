package com.hebbar.litelauncher.settings

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.hebbar.litelauncher.R
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

    private data class SettingCategory(
        val title: String,
        val subtitle: String,
        val action: (LinearLayout) -> Unit
    )

    private data class SettingSection(
        val sectionTitle: String,
        val categories: List<SettingCategory>
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        prefs = PreferencesManager(this)
        backupManager = BackupManager(this, prefs, WorkspaceRepository(this))

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#000000"))
        }

        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val padH = DensityUtil.dpToPx(context, 16f)
            val padV = DensityUtil.dpToPx(context, 16f)
            setPadding(padH, padV, padH, padV)
        }

        val backButton = ImageView(this).apply {
            setImageDrawable(ContextCompat.getDrawable(this@SettingsActivity, R.drawable.ic_arrow_back))
            val btnSize = DensityUtil.dpToPx(context, 32f)
            val pad = DensityUtil.dpToPx(context, 4f)
            setPadding(pad, pad, pad, pad)

            val typedValue = TypedValue()
            theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, typedValue, true)
            setBackgroundResource(typedValue.resourceId)

            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize)
            isClickable = true
            isFocusable = true
            setOnClickListener { finish() }
        }

        val headerTitle = TextView(this).apply {
            text = "Launcher settings"
            textSize = 20f
            setTextColor(Color.WHITE)
            setPadding(DensityUtil.dpToPx(context, 16f), 0, 0, 0)
        }

        headerLayout.addView(backButton)
        headerLayout.addView(headerTitle)
        rootLayout.addView(headerLayout)

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padH = DensityUtil.dpToPx(context, 16f)
            setPadding(padH, 0, padH, DensityUtil.dpToPx(context, 24f))
        }

        val scrollView = ScrollView(this).apply {
            addView(contentLayout)
            clipToPadding = false
        }
        rootLayout.addView(scrollView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, insets ->
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val navBarBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

            headerLayout.setPadding(
                DensityUtil.dpToPx(this, 16f),
                statusBarTop + DensityUtil.dpToPx(this, 12f),
                DensityUtil.dpToPx(this, 16f),
                DensityUtil.dpToPx(this, 12f)
            )

            scrollView.setPadding(0, 0, 0, navBarBottom + DensityUtil.dpToPx(this, 16f))
            insets
        }

        buildSettingSections(contentLayout)
        setContentView(rootLayout)
    }

    private fun buildSettingSections(container: LinearLayout) {
        val sections = listOf(
            SettingSection(
                sectionTitle = "Layout",
                categories = listOf(
                    SettingCategory("Home Screen", "Grid size, icon sizes, and workspace labels") { buildHomeScreenSettings(it) },
                    SettingCategory("Dock", "Icon count, slots, and dock background") { buildDockSettings(it) },
                    SettingCategory("Folders", "Preview layout and folder appearance") { buildFolderSettings(it) }
                )
            ),
            SettingSection(
                sectionTitle = "App drawer & gestures",
                categories = listOf(
                    SettingCategory("App Drawer", "Sort mode, drawer columns, and list layout") { buildAppDrawerSettings(it) },
                    SettingCategory("Gestures", "Swipe up, swipe down, and double tap actions") { buildGestureSettings(it) }
                )
            ),
            SettingSection(
                sectionTitle = "Appearance",
                categories = listOf(
                    SettingCategory("Theme mode", "Dark mode, light mode, and system default") { buildAppearanceSettings(it) },
                    SettingCategory("Icon pack", "Third-party icon packs and icon themes") { buildIconPackSettings(it) }
                )
            ),
            SettingSection(
                sectionTitle = "System & backup",
                categories = listOf(
                    SettingCategory("Backup & restore", "Export or import layout configurations") { buildBackupSettings(it) },
                    SettingCategory("Default launcher", "Set Lite Launcher as default home app") { openDefaultLauncherSettings(); return@SettingCategory },
                    SettingCategory("About", "Lite Launcher version and details") { buildAboutSettings(it) }
                )
            )
        )

        for ((secIndex, section) in sections.withIndex()) {
            val sectionHeader = TextView(this).apply {
                text = section.sectionTitle
                textSize = 14f
                setTextColor(Color.parseColor("#8E8E9F"))
                val topPad = if (secIndex == 0) DensityUtil.dpToPx(context, 12f) else DensityUtil.dpToPx(context, 20f)
                val botPad = DensityUtil.dpToPx(context, 8f)
                setPadding(DensityUtil.dpToPx(context, 4f), topPad, 0, botPad)
            }
            container.addView(sectionHeader)

            for ((catIndex, cat) in section.categories.withIndex()) {
                val cardView = createCategoryCard(cat.title, cat.subtitle) {
                    if (cat.title == "Default launcher") {
                        openDefaultLauncherSettings()
                    } else {
                        showCategoryDialog(cat.title, cat.action)
                    }
                }
                container.addView(cardView)

                if (catIndex < section.categories.size - 1) {
                    val spacer = View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, DensityUtil.dpToPx(context, 8f))
                    }
                    container.addView(spacer)
                }
            }
        }
    }

    private fun createCategoryCard(title: String, subtitle: String, onClick: () -> Unit): LinearLayout {
        val cardBg = GradientDrawable().apply {
            setColor(Color.parseColor("#12121C"))
            cornerRadius = DensityUtil.dpToPx(this@SettingsActivity, 16f).toFloat()
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = cardBg
            val padH = DensityUtil.dpToPx(context, 16f)
            val padV = DensityUtil.dpToPx(context, 16f)
            setPadding(padH, padV, padH, padV)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }

        val textLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val titleTv = TextView(this).apply {
            text = title
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }

        val subtitleTv = TextView(this).apply {
            text = subtitle
            textSize = 13f
            setTextColor(Color.parseColor("#A0A0B0"))
            setPadding(0, DensityUtil.dpToPx(context, 2f), 0, 0)
        }

        textLayout.addView(titleTv)
        textLayout.addView(subtitleTv)

        val chevronTv = TextView(this).apply {
            text = "›"
            textSize = 22f
            setTextColor(Color.parseColor("#606075"))
            gravity = Gravity.END
        }

        card.addView(textLayout, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        card.addView(chevronTv)

        return card
    }

    private fun showCategoryDialog(title: String, buildContent: (LinearLayout) -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = DensityUtil.dpToPx(context, 16f)
            setPadding(pad, pad, pad, pad)
        }

        buildContent(container)

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
        addSettingInfo(container, "Folder Preview: 2x2 Grid Thumbnail\nDrag icons into existing folders or onto other icons to merge into folders.")
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
            setTextColor(Color.parseColor("#A0A0B0"))
            setPadding(0, DensityUtil.dpToPx(context, 8f), 0, DensityUtil.dpToPx(context, 8f))
        }
        container.addView(tv)
    }
}
