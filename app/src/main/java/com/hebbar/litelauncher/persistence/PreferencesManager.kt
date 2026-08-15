package com.hebbar.litelauncher.persistence

import android.content.Context
import android.content.SharedPreferences
import com.hebbar.litelauncher.model.GestureAction
import com.hebbar.litelauncher.model.GestureType
import com.hebbar.litelauncher.model.ThemeMode
import com.hebbar.litelauncher.util.DensityUtil
import org.json.JSONArray
import org.json.JSONObject

class PreferencesManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("lite_launcher_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DESKTOP_COLUMNS = "desktop_columns"
        private const val KEY_DESKTOP_ROWS = "desktop_rows"
        private const val KEY_DRAWER_COLUMNS = "drawer_columns"
        private const val KEY_DOCK_COLUMNS = "dock_columns"
        private const val KEY_ICON_SIZE_DP = "icon_size_dp"
        private const val KEY_SHOW_LABELS = "show_labels"
        private const val KEY_LABEL_SIZE_SP = "label_size_sp"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_ICON_PACK = "selected_icon_pack"
        private const val KEY_DRAWER_SORT_MODE = "drawer_sort_mode"
        private const val KEY_DRAWER_IS_GRID = "drawer_is_grid"
        private const val KEY_HIDDEN_APPS = "hidden_apps_json"
        private const val KEY_APP_ALIASES = "app_aliases_json"
        private const val KEY_GESTURES = "gestures_json"
        private const val KEY_WALLPAPER_SCROLLING = "wallpaper_scrolling"
        private const val KEY_PAGE_COUNT = "page_count"
        private const val KEY_DEFAULT_PAGE = "default_page"
        private const val KEY_DOCK_ENABLED = "dock_enabled"
        private const val KEY_SEARCH_IN_HIDDEN = "search_in_hidden"
        private const val KEY_WORKSPACE_INITIALIZED = "workspace_initialized"
    }

    var isWorkspaceInitialized: Boolean
        get() = prefs.getBoolean(KEY_WORKSPACE_INITIALIZED, false)
        set(value) = prefs.edit().putBoolean(KEY_WORKSPACE_INITIALIZED, value).apply()

    var desktopColumns: Int
        get() {
            val defaultGrid = DensityUtil.getDefaultGridSize(context)
            return prefs.getInt(KEY_DESKTOP_COLUMNS, defaultGrid.first)
        }
        set(value) = prefs.edit().putInt(KEY_DESKTOP_COLUMNS, value).apply()

    var desktopRows: Int
        get() {
            val defaultGrid = DensityUtil.getDefaultGridSize(context)
            return prefs.getInt(KEY_DESKTOP_ROWS, defaultGrid.second)
        }
        set(value) = prefs.edit().putInt(KEY_DESKTOP_ROWS, value).apply()

    var drawerColumns: Int
        get() = prefs.getInt(KEY_DRAWER_COLUMNS, DensityUtil.getDefaultDrawerColumns(context))
        set(value) = prefs.edit().putInt(KEY_DRAWER_COLUMNS, value).apply()

    var dockColumns: Int
        get() = prefs.getInt(KEY_DOCK_COLUMNS, 5)
        set(value) = prefs.edit().putInt(KEY_DOCK_COLUMNS, value).apply()

    var iconSizeDp: Int
        get() = prefs.getInt(KEY_ICON_SIZE_DP, 56)
        set(value) = prefs.edit().putInt(KEY_ICON_SIZE_DP, value).apply()

    var showLabels: Boolean
        get() = prefs.getBoolean(KEY_SHOW_LABELS, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_LABELS, value).apply()

    var labelSizeSp: Float
        get() = prefs.getFloat(KEY_LABEL_SIZE_SP, 12f)
        set(value) = prefs.edit().putFloat(KEY_LABEL_SIZE_SP, value).apply()

    var themeMode: ThemeMode
        get() {
            val name = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
            return try { ThemeMode.valueOf(name) } catch (e: Exception) { ThemeMode.SYSTEM }
        }
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value.name).apply()

    var accentColor: Int
        get() = prefs.getInt(KEY_ACCENT_COLOR, 0xFF3D5AFE.toInt())
        set(value) = prefs.edit().putInt(KEY_ACCENT_COLOR, value).apply()

    var selectedIconPack: String?
        get() = prefs.getString(KEY_ICON_PACK, null)
        set(value) = prefs.edit().putString(KEY_ICON_PACK, value).apply()

    var drawerSortMode: Int // 0: Alpha, 1: InstallDate, 2: MostLaunched, 3: RecentlyLaunched
        get() = prefs.getInt(KEY_DRAWER_SORT_MODE, 0)
        set(value) = prefs.edit().putInt(KEY_DRAWER_SORT_MODE, value).apply()

    var drawerIsGrid: Boolean
        get() = prefs.getBoolean(KEY_DRAWER_IS_GRID, true)
        set(value) = prefs.edit().putBoolean(KEY_DRAWER_IS_GRID, value).apply()

    var wallpaperScrolling: Boolean
        get() = prefs.getBoolean(KEY_WALLPAPER_SCROLLING, true)
        set(value) = prefs.edit().putBoolean(KEY_WALLPAPER_SCROLLING, value).apply()

    var pageCount: Int
        get() = prefs.getInt(KEY_PAGE_COUNT, 3)
        set(value) = prefs.edit().putInt(KEY_PAGE_COUNT, value.coerceAtLeast(1)).apply()

    var defaultPage: Int
        get() = prefs.getInt(KEY_DEFAULT_PAGE, 0)
        set(value) = prefs.edit().putInt(KEY_DEFAULT_PAGE, value).apply()

    var dockEnabled: Boolean
        get() = prefs.getBoolean(KEY_DOCK_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_DOCK_ENABLED, value).apply()

    var searchInHidden: Boolean
        get() = prefs.getBoolean(KEY_SEARCH_IN_HIDDEN, false)
        set(value) = prefs.edit().putBoolean(KEY_SEARCH_IN_HIDDEN, value).apply()

    fun getHiddenApps(): Set<String> {
        val json = prefs.getString(KEY_HIDDEN_APPS, "[]") ?: "[]"
        val set = mutableSetOf<String>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                set.add(array.getString(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return set
    }

    fun setHiddenApps(hiddenApps: Set<String>) {
        val array = JSONArray(hiddenApps)
        prefs.edit().putString(KEY_HIDDEN_APPS, array.toString()).apply()
    }

    fun getAppAliases(): Map<String, String> {
        val json = prefs.getString(KEY_APP_ALIASES, "{}") ?: "{}"
        val map = mutableMapOf<String, String>()
        try {
            val obj = JSONObject(json)
            obj.keys().forEach { key ->
                map[key] = obj.getString(key)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    fun setAppAlias(packageName: String, alias: String) {
        val map = getAppAliases().toMutableMap()
        if (alias.isBlank()) {
            map.remove(packageName)
        } else {
            map[packageName] = alias
        }
        val obj = JSONObject(map as Map<*, *>)
        prefs.edit().putString(KEY_APP_ALIASES, obj.toString()).apply()
    }

    fun getGestureAction(type: GestureType): GestureAction {
        val json = prefs.getString(KEY_GESTURES, "{}") ?: "{}"
        try {
            val obj = JSONObject(json)
            if (obj.has(type.name)) {
                return GestureAction.valueOf(obj.getString(type.name))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Defaults
        return when (type) {
            GestureType.SWIPE_UP -> GestureAction.OPEN_DRAWER
            GestureType.SWIPE_DOWN -> GestureAction.OPEN_NOTIFICATIONS
            GestureType.DOUBLE_TAP -> GestureAction.OPEN_SEARCH
            GestureType.LONG_PRESS -> GestureAction.OPEN_SETTINGS
            else -> GestureAction.DO_NOTHING
        }
    }

    fun setGestureAction(type: GestureType, action: GestureAction) {
        val json = prefs.getString(KEY_GESTURES, "{}") ?: "{}"
        val obj = try { JSONObject(json) } catch (e: Exception) { JSONObject() }
        obj.put(type.name, action.name)
        prefs.edit().putString(KEY_GESTURES, obj.toString()).apply()
    }
}
