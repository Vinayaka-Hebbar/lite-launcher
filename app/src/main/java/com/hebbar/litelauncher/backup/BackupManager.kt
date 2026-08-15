package com.hebbar.litelauncher.backup

import android.content.Context
import com.hebbar.litelauncher.persistence.PreferencesManager
import com.hebbar.litelauncher.workspace.WorkspaceRepository
import org.json.JSONObject

class BackupManager(
    private val context: Context,
    private val prefs: PreferencesManager,
    private val workspaceRepo: WorkspaceRepository
) {

    companion object {
        const val CURRENT_VERSION = 1
    }

    fun createBackupJson(): String {
        val root = JSONObject()
        root.put("version", CURRENT_VERSION)
        root.put("timestamp", System.currentTimeMillis())

        // Preferences
        val settingsObj = JSONObject().apply {
            put("desktopColumns", prefs.desktopColumns)
            put("desktopRows", prefs.desktopRows)
            put("drawerColumns", prefs.drawerColumns)
            put("dockColumns", prefs.dockColumns)
            put("iconSizeDp", prefs.iconSizeDp)
            put("showLabels", prefs.showLabels)
            put("labelSizeSp", prefs.labelSizeSp)
            put("themeMode", prefs.themeMode.name)
            put("accentColor", prefs.accentColor)
            put("pageCount", prefs.pageCount)
            put("defaultPage", prefs.defaultPage)
            put("dockEnabled", prefs.dockEnabled)
        }
        root.put("settings", settingsObj)

        // Hidden Apps
        root.put("hiddenApps", JSONObject.wrap(prefs.getHiddenApps().toList()))

        // Workspace Items
        val items = workspaceRepo.loadItems()
        val rawWorkspaceJson = context.getFileStreamPath("workspace.json")
        val workspaceJsonStr = if (rawWorkspaceJson.exists()) rawWorkspaceJson.readText() else "[]"
        root.put("workspace", workspaceJsonStr)

        return root.toString(2)
    }

    fun restoreBackupJson(jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            val version = root.optInt("version", 1)
            if (version > CURRENT_VERSION) {
                return false // Unsupported future schema version
            }

            if (root.has("settings")) {
                val settingsObj = root.getJSONObject("settings")
                if (settingsObj.has("desktopColumns")) prefs.desktopColumns = settingsObj.getInt("desktopColumns")
                if (settingsObj.has("desktopRows")) prefs.desktopRows = settingsObj.getInt("desktopRows")
                if (settingsObj.has("drawerColumns")) prefs.drawerColumns = settingsObj.getInt("drawerColumns")
                if (settingsObj.has("dockColumns")) prefs.dockColumns = settingsObj.getInt("dockColumns")
                if (settingsObj.has("iconSizeDp")) prefs.iconSizeDp = settingsObj.getInt("iconSizeDp")
                if (settingsObj.has("showLabels")) prefs.showLabels = settingsObj.getBoolean("showLabels")
                if (settingsObj.has("pageCount")) prefs.pageCount = settingsObj.getInt("pageCount")
                if (settingsObj.has("defaultPage")) prefs.defaultPage = settingsObj.getInt("defaultPage")
            }

            if (root.has("workspace")) {
                val wsStr = root.getString("workspace")
                val wsFile = context.getFileStreamPath("workspace.json")
                wsFile.writeText(wsStr)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
