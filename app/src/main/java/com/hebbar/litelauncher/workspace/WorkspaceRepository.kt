package com.hebbar.litelauncher.workspace

import android.content.Context
import com.hebbar.litelauncher.model.WorkspaceItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class WorkspaceRepository(private val context: Context) {
    private val file = File(context.filesDir, "workspace.json")
    private val backupFile = File(context.filesDir, "workspace.json.bak")

    fun loadItems(): List<WorkspaceItem> {
        val targetFile = if (file.exists()) file else if (backupFile.exists()) backupFile else null
        if (targetFile == null || !targetFile.exists()) {
            return emptyList()
        }

        val items = mutableListOf<WorkspaceItem>()
        try {
            val jsonStr = targetFile.readText()
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val item = parseWorkspaceItem(obj)
                if (item != null) {
                    items.add(item)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Graceful fallback to backup if main file is corrupted
            if (targetFile == file && backupFile.exists()) {
                return loadItemsFromBackup()
            }
        }
        return items
    }

    private fun loadItemsFromBackup(): List<WorkspaceItem> {
        return try {
            val jsonStr = backupFile.readText()
            val array = JSONArray(jsonStr)
            val items = mutableListOf<WorkspaceItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val item = parseWorkspaceItem(obj)
                if (item != null) {
                    items.add(item)
                }
            }
            items
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveItems(items: List<WorkspaceItem>) {
        try {
            val array = JSONArray()
            items.forEach { item ->
                array.put(serializeWorkspaceItem(item))
            }

            // Create backup of previous safe state
            if (file.exists()) {
                file.copyTo(backupFile, overwrite = true)
            }

            val tempFile = File(context.filesDir, "workspace.json.tmp")
            tempFile.writeText(array.toString(2))
            if (tempFile.exists()) {
                tempFile.renameTo(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseWorkspaceItem(obj: JSONObject): WorkspaceItem? {
        val type = obj.optString("type")
        val id = obj.optLong("id", System.currentTimeMillis())
        val screenId = obj.optInt("screenId", 0)
        val cellX = obj.optInt("cellX", 0)
        val cellY = obj.optInt("cellY", 0)
        val spanX = obj.optInt("spanX", 1)
        val spanY = obj.optInt("spanY", 1)

        return when (type) {
            "APP" -> {
                val pkg = obj.optString("packageName")
                val act = obj.optString("activityName")
                if (pkg.isNotEmpty() && act.isNotEmpty()) {
                    WorkspaceItem.AppItem(
                        id = id,
                        screenId = screenId,
                        cellX = cellX,
                        cellY = cellY,
                        spanX = spanX,
                        spanY = spanY,
                        packageName = pkg,
                        activityName = act,
                        customLabel = if (obj.has("customLabel")) obj.getString("customLabel") else null,
                        customIconPackPackage = if (obj.has("customIconPackPackage")) obj.getString("customIconPackPackage") else null,
                        customIconDrawableName = if (obj.has("customIconDrawableName")) obj.getString("customIconDrawableName") else null
                    )
                } else null
            }
            "FOLDER" -> {
                val title = obj.optString("title", "Folder")
                val folderItemsArray = obj.optJSONArray("items") ?: JSONArray()
                val folderApps = mutableListOf<WorkspaceItem.AppItem>()
                for (i in 0 until folderItemsArray.length()) {
                    val appObj = folderItemsArray.getJSONObject(i)
                    val appItem = parseWorkspaceItem(appObj) as? WorkspaceItem.AppItem
                    if (appItem != null) {
                        folderApps.add(appItem)
                    }
                }
                WorkspaceItem.FolderItem(
                    id = id,
                    screenId = screenId,
                    cellX = cellX,
                    cellY = cellY,
                    spanX = spanX,
                    spanY = spanY,
                    title = title,
                    items = folderApps,
                    color = if (obj.has("color")) obj.optInt("color") else null
                )
            }
            "WIDGET" -> {
                val widgetId = obj.optInt("appWidgetId", -1)
                val pkg = obj.optString("providerPackage")
                val cls = obj.optString("providerClass")
                if (widgetId != -1 && pkg.isNotEmpty() && cls.isNotEmpty()) {
                    WorkspaceItem.WidgetItem(
                        id = id,
                        screenId = screenId,
                        cellX = cellX,
                        cellY = cellY,
                        spanX = spanX,
                        spanY = spanY,
                        appWidgetId = widgetId,
                        providerPackage = pkg,
                        providerClass = cls
                    )
                } else null
            }
            "SHORTCUT" -> {
                val shortcutId = obj.optString("shortcutId")
                val pkg = obj.optString("packageName")
                val label = obj.optString("label")
                val intentUri = obj.optString("intentUri")
                WorkspaceItem.ShortcutItem(
                    id = id,
                    screenId = screenId,
                    cellX = cellX,
                    cellY = cellY,
                    spanX = spanX,
                    spanY = spanY,
                    shortcutId = shortcutId,
                    packageName = pkg,
                    label = label,
                    intentUri = intentUri
                )
            }
            "CLOCK" -> {
                WorkspaceItem.ClockDateCardItem(
                    id = id,
                    screenId = screenId,
                    cellX = cellX,
                    cellY = cellY,
                    spanX = spanX,
                    spanY = spanY
                )
            }
            else -> null
        }
    }

    private fun serializeWorkspaceItem(item: WorkspaceItem): JSONObject {
        val obj = JSONObject()
        obj.put("id", item.id)
        obj.put("screenId", item.screenId)
        obj.put("cellX", item.cellX)
        obj.put("cellY", item.cellY)
        obj.put("spanX", item.spanX)
        obj.put("spanY", item.spanY)

        when (item) {
            is WorkspaceItem.AppItem -> {
                obj.put("type", "APP")
                obj.put("packageName", item.packageName)
                obj.put("activityName", item.activityName)
                item.customLabel?.let { obj.put("customLabel", it) }
                item.customIconPackPackage?.let { obj.put("customIconPackPackage", it) }
                item.customIconDrawableName?.let { obj.put("customIconDrawableName", it) }
            }
            is WorkspaceItem.FolderItem -> {
                obj.put("type", "FOLDER")
                obj.put("title", item.title)
                item.color?.let { obj.put("color", it) }
                val folderArray = JSONArray()
                item.items.forEach { folderApp ->
                    folderArray.put(serializeWorkspaceItem(folderApp))
                }
                obj.put("items", folderArray)
            }
            is WorkspaceItem.WidgetItem -> {
                obj.put("type", "WIDGET")
                obj.put("appWidgetId", item.appWidgetId)
                obj.put("providerPackage", item.providerPackage)
                obj.put("providerClass", item.providerClass)
            }
            is WorkspaceItem.ShortcutItem -> {
                obj.put("type", "SHORTCUT")
                obj.put("shortcutId", item.shortcutId)
                obj.put("packageName", item.packageName)
                obj.put("label", item.label)
                obj.put("intentUri", item.intentUri)
            }
            is WorkspaceItem.ClockDateCardItem -> {
                obj.put("type", "CLOCK")
            }
        }
        return obj
    }
}
