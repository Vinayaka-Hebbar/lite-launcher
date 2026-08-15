package com.hebbar.litelauncher.themes

import android.app.Activity
import android.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.hebbar.litelauncher.model.ThemeMode
import com.hebbar.litelauncher.persistence.PreferencesManager

object ThemeManager {

    fun applyTheme(activity: Activity, prefs: PreferencesManager) {
        try {
            val themeMode = prefs.themeMode
            val window = activity.window

            WindowCompat.setDecorFitsSystemWindows(window, false)

            val isDark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK, ThemeMode.AMOLED_BLACK -> true
                ThemeMode.SYSTEM -> {
                    val nightModeFlags = activity.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                    nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
                }
            }

            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT

            val decorView = window.decorView
            val controller = WindowCompat.getInsetsController(window, decorView)
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
