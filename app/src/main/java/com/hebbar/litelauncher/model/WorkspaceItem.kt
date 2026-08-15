package com.hebbar.litelauncher.model

import android.content.ComponentName

/**
 * Interface representing any item placed on a workspace page or dock.
 */
sealed interface WorkspaceItem {
    val id: Long
    var screenId: Int // Page index on workspace, or -1 for dock
    var cellX: Int
    var cellY: Int
    var spanX: Int
    var spanY: Int

    /**
     * Application item placed on workspace or dock.
     */
    data class AppItem(
        override val id: Long,
        override var screenId: Int,
        override var cellX: Int,
        override var cellY: Int,
        override var spanX: Int = 1,
        override var spanY: Int = 1,
        val packageName: String,
        val activityName: String,
        var customLabel: String? = null,
        var customIconPackPackage: String? = null,
        var customIconDrawableName: String? = null
    ) : WorkspaceItem {
        val componentName: ComponentName
            get() = ComponentName(packageName, activityName)
    }

    /**
     * Folder item containing multiple application items.
     */
    data class FolderItem(
        override val id: Long,
        override var screenId: Int,
        override var cellX: Int,
        override var cellY: Int,
        override var spanX: Int = 1,
        override var spanY: Int = 1,
        var title: String = "Folder",
        val items: MutableList<AppItem> = mutableListOf(),
        var color: Int? = null
    ) : WorkspaceItem

    /**
     * App Widget item hosted via AppWidgetHost.
     */
    data class WidgetItem(
        override val id: Long,
        override var screenId: Int,
        override var cellX: Int,
        override var cellY: Int,
        override var spanX: Int,
        override var spanY: Int,
        val appWidgetId: Int,
        val providerPackage: String,
        val providerClass: String
    ) : WorkspaceItem

    /**
     * Android Shortcut Item (Pinned dynamic/manifest shortcut).
     */
    data class ShortcutItem(
        override val id: Long,
        override var screenId: Int,
        override var cellX: Int,
        override var cellY: Int,
        override var spanX: Int = 1,
        override var spanY: Int = 1,
        val shortcutId: String,
        val packageName: String,
        val label: String,
        val intentUri: String
    ) : WorkspaceItem

    /**
     * Native launcher-owned Clock and Date Card workspace item.
     */
    data class ClockDateCardItem(
        override val id: Long = 1001L,
        override var screenId: Int = 0,
        override var cellX: Int = 0,
        override var cellY: Int = 0,
        override var spanX: Int = 4,
        override var spanY: Int = 2
    ) : WorkspaceItem
}
