package com.hebbar.litelauncher.workspace

import android.content.Context
import com.hebbar.litelauncher.drawer.AppRepository
import com.hebbar.litelauncher.model.LaunchableApp
import com.hebbar.litelauncher.model.WorkspaceItem
import com.hebbar.litelauncher.persistence.PreferencesManager

object FirstRunOrganizer {

    fun generateInitialWorkspace(
        context: Context,
        installedApps: List<LaunchableApp>,
        prefs: PreferencesManager
    ): List<WorkspaceItem> {
        val workspaceItems = mutableListOf<WorkspaceItem>()

        // Add Clock & Date Card item to Page 0, Cell (0, 0), Span (4, 1)
        workspaceItems.add(
            WorkspaceItem.ClockDateCardItem(
                id = 1001L,
                screenId = 0,
                cellX = 0,
                cellY = 0,
                spanX = 4,
                spanY = 1
            )
        )

        // Default Dock Apps (5 key essential apps)
        val dockPackageCandidates = listOf(
            listOf("com.android.dialer", "com.google.android.dialer", "com.samsung.android.dialer", "dialer", "phone"),
            listOf("com.android.mms", "com.google.android.apps.messaging", "messaging", "mms"),
            listOf("com.android.chrome", "org.mozilla.firefox", "com.sec.android.app.sbrowser", "browser"),
            listOf("com.google.android.youtube", "youtube"),
            listOf("com.android.camera", "com.google.android.GoogleCamera", "camera")
        )

        val usedDockApps = mutableSetOf<String>()

        for ((dockSlot, candidateList) in dockPackageCandidates.withIndex()) {
            val matchedApp = installedApps.firstOrNull { app ->
                candidateList.any { candidate -> app.packageName.lowercase().contains(candidate) }
            } ?: installedApps.firstOrNull { !usedDockApps.contains(it.packageName) }

            if (matchedApp != null) {
                usedDockApps.add(matchedApp.packageName)
                workspaceItems.add(
                    WorkspaceItem.AppItem(
                        id = System.currentTimeMillis() + dockSlot,
                        screenId = -1, // Dock
                        cellX = dockSlot,
                        cellY = 0,
                        packageName = matchedApp.packageName,
                        activityName = matchedApp.activityName
                    )
                )
            }
        }

        // Curated Category Folders on Page 0 (Rows 1 and 2)
        val availableForFolders = installedApps.filterNot { usedDockApps.contains(it.packageName) }

        val googleApps = availableForFolders.filter { it.packageName.contains("google") || it.label.contains("Google") }
        val socialApps = availableForFolders.filter {
            val pkg = it.packageName.lowercase()
            pkg.contains("whatsapp") || pkg.contains("facebook") || pkg.contains("instagram") ||
                    pkg.contains("twitter") || pkg.contains("telegram") || pkg.contains("snapchat") || pkg.contains("social")
        }
        val gameApps = availableForFolders.filter {
            val pkg = it.packageName.lowercase()
            pkg.contains("game") || pkg.contains("pubg") || pkg.contains("subway") || pkg.contains("candy")
        }
        val mediaApps = availableForFolders.filter {
            val pkg = it.packageName.lowercase()
            pkg.contains("spotify") || pkg.contains("music") || pkg.contains("video") || pkg.contains("netflix") || pkg.contains("player")
        }
        val shoppingApps = availableForFolders.filter {
            val pkg = it.packageName.lowercase()
            pkg.contains("amazon") || pkg.contains("flipkart") || pkg.contains("shop") || pkg.contains("store")
        }
        val toolsApps = availableForFolders.filter {
            val pkg = it.packageName.lowercase()
            pkg.contains("calculator") || pkg.contains("clock") || pkg.contains("calendar") || pkg.contains("settings") || pkg.contains("files")
        }

        fun createFolderItem(title: String, apps: List<LaunchableApp>): WorkspaceItem.FolderItem? {
            if (apps.isEmpty()) return null
            val appItems = apps.take(9).mapIndexed { idx, app ->
                WorkspaceItem.AppItem(
                    id = System.currentTimeMillis() + idx + title.hashCode(),
                    screenId = 0,
                    cellX = 0,
                    cellY = 0,
                    packageName = app.packageName,
                    activityName = app.activityName
                )
            }.toMutableList()

            return WorkspaceItem.FolderItem(
                id = System.currentTimeMillis() + title.hashCode(),
                screenId = 0,
                cellX = 0,
                cellY = 0,
                title = title,
                items = appItems
            )
        }

        val createdFolders = mutableListOf<WorkspaceItem.FolderItem>()
        createFolderItem("Google", googleApps)?.let { createdFolders.add(it) }
        createFolderItem("Social", socialApps)?.let { createdFolders.add(it) }
        createFolderItem("Games", gameApps)?.let { createdFolders.add(it) }
        createFolderItem("Media", mediaApps)?.let { createdFolders.add(it) }
        createFolderItem("Shopping", shoppingApps)?.let { createdFolders.add(it) }
        createFolderItem("Tools", toolsApps)?.let { createdFolders.add(it) }

        // Limit to 6 curated folders on Page 0 starting at cellY = 1
        val finalFolders = createdFolders.take(6)
        val foldersPerRow = 4

        for (index in finalFolders.indices) {
            val folder = finalFolders[index]
            folder.cellX = index % foldersPerRow
            folder.cellY = 1 + (index / foldersPerRow)
            folder.screenId = 0
            workspaceItems.add(folder)
        }

        return workspaceItems
    }
}
