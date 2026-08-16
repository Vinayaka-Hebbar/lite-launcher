package com.hebbar.litelauncher.settings

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.hebbar.litelauncher.R
import com.hebbar.litelauncher.backup.BackupManager
import com.hebbar.litelauncher.persistence.PreferencesManager
import com.hebbar.litelauncher.util.DensityUtil
import com.hebbar.litelauncher.workspace.WorkspaceRepository

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: PreferencesManager
    private lateinit var backupManager: BackupManager
    private lateinit var settingsPages: SettingsPages
    private lateinit var hiddenAppsPage: HiddenAppsPage

    private lateinit var rootContainer: FrameLayout
    private lateinit var mainPageContainer: View
    private lateinit var subPageContainer: View
    private lateinit var subHeaderTitle: TextView
    private lateinit var subContentContainer: LinearLayout
    private lateinit var mainHeaderLayout: LinearLayout
    private lateinit var subHeaderLayout: LinearLayout

    private var isSubPageOpen = false

    private data class SettingCategory(
        val title: String,
        val subtitle: String,
        val buildPage: (LinearLayout) -> Unit
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
        settingsPages = SettingsPages(this, prefs, backupManager)
        hiddenAppsPage = HiddenAppsPage(this, prefs, this)

        rootContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#000000"))
        }

        mainPageContainer = buildMainSettingsPage()
        subPageContainer = buildSubSettingsPage()

        subPageContainer.visibility = View.GONE

        rootContainer.addView(mainPageContainer)
        rootContainer.addView(subPageContainer)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isSubPageOpen) {
                    closeSubPage()
                } else {
                    finish()
                }
            }
        })

        setContentView(rootContainer)
    }

    private fun openSubPage(title: String, buildContent: (LinearLayout) -> Unit) {
        subHeaderTitle.text = title
        subContentContainer.removeAllViews()

        isSubPageOpen = true
        val width = rootContainer.width.toFloat().takeIf { it > 0 }
            ?: resources.displayMetrics.widthPixels.toFloat()

        subPageContainer.translationX = width
        subPageContainer.visibility = View.VISIBLE

        mainPageContainer.animate()
            .translationX(-width * 0.25f)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()

        subPageContainer.animate()
            .translationX(0f)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .withEndAction {
                buildContent(subContentContainer)
            }
            .start()
    }

    private fun closeSubPage() {
        if (!isSubPageOpen) return
        isSubPageOpen = false

        val width = rootContainer.width.toFloat().takeIf { it > 0 }
            ?: resources.displayMetrics.widthPixels.toFloat()

        mainPageContainer.animate()
            .translationX(0f)
            .setDuration(200)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()

        subPageContainer.animate()
            .translationX(width)
            .setDuration(200)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .withEndAction {
                subPageContainer.visibility = View.GONE
                subContentContainer.removeAllViews()
            }
            .start()
    }

    private fun buildMainSettingsPage(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#000000"))
        }

        mainHeaderLayout = LinearLayout(this).apply {
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
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(DensityUtil.dpToPx(context, 16f), 0, 0, 0)
        }

        mainHeaderLayout.addView(backButton)
        mainHeaderLayout.addView(headerTitle)
        root.addView(mainHeaderLayout)

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padH = DensityUtil.dpToPx(context, 16f)
            setPadding(padH, 0, padH, DensityUtil.dpToPx(context, 24f))
        }

        val scrollView = ScrollView(this).apply {
            addView(contentLayout)
            clipToPadding = false
        }

        root.addView(scrollView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val navBarBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

            mainHeaderLayout.setPadding(
                DensityUtil.dpToPx(this, 16f),
                statusBarTop + DensityUtil.dpToPx(this, 12f),
                DensityUtil.dpToPx(this, 16f),
                DensityUtil.dpToPx(this, 12f)
            )

            scrollView.setPadding(0, 0, 0, navBarBottom + DensityUtil.dpToPx(this, 16f))
            insets
        }

        buildSettingSections(contentLayout)
        return root
    }

    private fun buildSubSettingsPage(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#000000"))
        }

        subHeaderLayout = LinearLayout(this).apply {
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
            setOnClickListener { closeSubPage() }
        }

        subHeaderTitle = TextView(this).apply {
            text = "Setting"
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(DensityUtil.dpToPx(context, 16f), 0, 0, 0)
        }

        subHeaderLayout.addView(backButton)
        subHeaderLayout.addView(subHeaderTitle)
        root.addView(subHeaderLayout)

        subContentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padH = DensityUtil.dpToPx(context, 16f)
            setPadding(padH, 0, padH, DensityUtil.dpToPx(context, 24f))
        }

        // Parent container directly holds subContentContainer (no outer ScrollView) so sub-pages can manage sticky headers & scrolling RecyclerViews
        root.addView(subContentContainer, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val navBarBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

            subHeaderLayout.setPadding(
                DensityUtil.dpToPx(this, 16f),
                statusBarTop + DensityUtil.dpToPx(this, 12f),
                DensityUtil.dpToPx(this, 16f),
                DensityUtil.dpToPx(this, 12f)
            )

            subContentContainer.setPadding(
                DensityUtil.dpToPx(this, 16f),
                0,
                DensityUtil.dpToPx(this, 16f),
                navBarBottom + DensityUtil.dpToPx(this, 16f)
            )
            insets
        }

        return root
    }

    private fun buildSettingSections(container: LinearLayout) {
        val sections = listOf(
            SettingSection(
                sectionTitle = "Layout",
                categories = listOf(
                    SettingCategory("Home Screen", "Grid size, icon sizes, and workspace labels") { settingsPages.buildHomeScreenSettingsPage(it) },
                    SettingCategory("Dock", "Icon count, slots, and dock background") { settingsPages.buildDockSettingsPage(it) },
                    SettingCategory("Folders", "Preview layout and folder appearance") { settingsPages.buildFolderSettingsPage(it) }
                )
            ),
            SettingSection(
                sectionTitle = "App drawer & gestures",
                categories = listOf(
                    SettingCategory("App Drawer", "Sort mode, drawer columns, and list layout") { settingsPages.buildAppDrawerSettingsPage(it) },
                    SettingCategory("Hidden Apps", "Hide or unhide apps") { hiddenAppsPage.build(it) },
                    SettingCategory("Gestures", "Swipe up, swipe down, and double tap actions") { settingsPages.buildGestureSettingsPage(it) }
                )
            ),
            SettingSection(
                sectionTitle = "Appearance",
                categories = listOf(
                    SettingCategory("Theme mode", "Dark mode, light mode, and system default") { settingsPages.buildAppearanceSettingsPage(it) },
                    SettingCategory("Icon pack", "Third-party icon packs and icon themes") { settingsPages.buildIconPackSettingsPage(it) }
                )
            ),
            SettingSection(
                sectionTitle = "System & backup",
                categories = listOf(
                    SettingCategory("Backup & restore", "Export or import layout configurations") { settingsPages.buildBackupSettingsPage(it) },
                    SettingCategory("Default launcher", "Set Lite Launcher as default home app") { openDefaultLauncherSettings(); return@SettingCategory },
                    SettingCategory("About", "Lite Launcher version and details") { settingsPages.buildAboutSettingsPage(it) }
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
                        openSubPage(cat.title, cat.buildPage)
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

    private fun openDefaultLauncherSettings() {
        try {
            val intent = Intent(Settings.ACTION_HOME_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS)
                startActivity(intent)
            } catch (ex: Exception) {
                Toast.makeText(this, "Could not open system home settings", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
