package com.hebbar.litelauncher

import android.content.Intent
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hebbar.litelauncher.dock.DockView
import com.hebbar.litelauncher.drawer.AppDrawerBottomSheet
import com.hebbar.litelauncher.drawer.AppRepository
import com.hebbar.litelauncher.folders.FolderDialog
import com.hebbar.litelauncher.gestures.LauncherGestureController
import com.hebbar.litelauncher.gestures.LauncherGestureDetector
import com.hebbar.litelauncher.icons.AdaptiveIconHelper
import com.hebbar.litelauncher.icons.IconPackManager
import com.hebbar.litelauncher.model.GestureAction
import com.hebbar.litelauncher.model.LaunchableApp
import com.hebbar.litelauncher.model.WorkspaceItem
import com.hebbar.litelauncher.notifications.LiteNotificationListener
import com.hebbar.litelauncher.persistence.PreferencesManager
import com.hebbar.litelauncher.settings.SettingsActivity
import com.hebbar.litelauncher.themes.ThemeManager
import com.hebbar.litelauncher.util.BitmapCache
import com.hebbar.litelauncher.util.DensityUtil
import com.hebbar.litelauncher.workspace.CellLayout
import com.hebbar.litelauncher.workspace.ClockDateCardView
import com.hebbar.litelauncher.workspace.FirstRunOrganizer
import com.hebbar.litelauncher.workspace.PageIndicatorView
import com.hebbar.litelauncher.workspace.WorkspaceInteractionController
import com.hebbar.litelauncher.workspace.WorkspaceItemView
import com.hebbar.litelauncher.workspace.WorkspaceRepository
import com.hebbar.litelauncher.workspace.WorkspaceView
import kotlinx.coroutines.launch

open class LauncherActivity : AppCompatActivity(), WorkspaceInteractionController.InteractionListener {

    private lateinit var prefs: PreferencesManager
    private lateinit var workspaceRepo: WorkspaceRepository
    private lateinit var appRepo: AppRepository
    private lateinit var iconPackManager: IconPackManager
    private lateinit var iconHelper: AdaptiveIconHelper

    private lateinit var rootContainer: FrameLayout
    private lateinit var workspaceView: WorkspaceView
    private lateinit var dockView: DockView
    private lateinit var pageIndicatorView: PageIndicatorView
    private lateinit var appDrawer: AppDrawerBottomSheet
    private lateinit var interactionController: WorkspaceInteractionController
    private lateinit var gestureController: LauncherGestureController
    private lateinit var gestureDetector: LauncherGestureDetector

    private val workspaceItems = mutableListOf<WorkspaceItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PreferencesManager(this)
        workspaceRepo = WorkspaceRepository(this)
        iconPackManager = IconPackManager(this)
        iconHelper = AdaptiveIconHelper(this, prefs, iconPackManager)
        appRepo = AppRepository(this, iconHelper)

        buildLayout()
        ThemeManager.applyTheme(this, prefs)
        initServices()
        loadWorkspace()

        appRepo.registerPackageReceiver {
            lifecycleScope.launch {
                val apps = appRepo.getInstalledApps(forceRefresh = true)
                appDrawer.setApps(apps, prefs)
                refreshWorkspaceViews()
            }
        }

        LiteNotificationListener.onNotificationChangedListener = {
            refreshNotificationBadges()
        }
    }

    override fun onResume() {
        super.onResume()
        ThemeManager.applyTheme(this, prefs)
        lifecycleScope.launch {
            val apps = appRepo.getInstalledApps()
            appDrawer.setApps(apps, prefs)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appRepo.unregisterPackageReceiver()
        iconHelper.clearCache()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_LOW || level >= TRIM_MEMORY_BACKGROUND) {
            iconHelper.clearCache()
            System.gc()
        }
    }

    private fun buildLayout() {
        rootContainer = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            clipChildren = false
            clipToPadding = false
        }

        val workspaceContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            clipChildren = false
            clipToPadding = false
        }

        // Workspace Pager View
        workspaceView = WorkspaceView(this).apply {
            onPageChangeListener = { page ->
                updatePageIndicator(page)
            }
        }
        workspaceContainer.addView(workspaceView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        // Page Indicator Dots (● ○)
        pageIndicatorView = PageIndicatorView(this)
        workspaceContainer.addView(pageIndicatorView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        // Bottom Glass Dock Bar
        dockView = DockView(this)
        if (prefs.dockEnabled) {
            dockView.setDockColumns(prefs.dockColumns)
            val dockHeight = DensityUtil.dpToPx(this, 76f)
            workspaceContainer.addView(dockView, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dockHeight
            ))
        }

        rootContainer.addView(workspaceContainer)

        // App Drawer Layer
        appDrawer = AppDrawerBottomSheet(this).apply {
            setup(prefs, iconHelper, appRepo)
            onAppClickListener = { app ->
                launchApp(app.packageName, app.activityName)
                close()
            }
            onAppLongClickListener = { app, view ->
                showAppLongClickMenu(app, view)
                true
            }
            onDrawerProgressListener = { progress ->
                workspaceView.translationY = 0f
                workspaceView.scaleX = 1.0f
                workspaceView.scaleY = 1.0f
                workspaceView.alpha = (1f - (0.4f * progress)).coerceIn(0.6f, 1f)
                dockView.alpha = (1f - (progress * 2.5f)).coerceIn(0f, 1f)
            }
        }
        rootContainer.addView(appDrawer, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        setContentView(rootContainer)

        gestureController = LauncherGestureController(
            this,
            workspaceView,
            appDrawer,
            onOpenDesktopContextMenu = { rawX, rawY -> showDesktopContextMenuAt(rawX, rawY) },
            onSwipeDownGesture = { openNotificationPanel() }
        )

        workspaceView.gestureController = gestureController

        interactionController = WorkspaceInteractionController(this, rootContainer, workspaceView, dockView).apply {
            listener = this@LauncherActivity
            gestureController = this@LauncherActivity.gestureController
        }

        appDrawer.isWorkspaceDragging = { interactionController.isDragging }

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootContainer) { _, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            val topInset = systemBars.top
            val bottomInset = systemBars.bottom

            workspaceContainer.setPadding(0, topInset + DensityUtil.dpToPx(this, 8f), 0, 0)

            dockView.layoutParams = (dockView.layoutParams as LinearLayout.LayoutParams).apply {
                setMargins(
                    DensityUtil.dpToPx(this@LauncherActivity, 22f),
                    0,
                    DensityUtil.dpToPx(this@LauncherActivity, 22f),
                    bottomInset + DensityUtil.dpToPx(this@LauncherActivity, 14f)
                )
            }

            appDrawer.applyWindowInsets(topInset, bottomInset)
            insets
        }

        gestureDetector = LauncherGestureDetector(this, prefs) { action ->
            executeGestureAction(action)
        }
    }

    private fun initServices() {
        refreshWorkspacePageCount(1)
    }

    private fun refreshWorkspacePageCount(count: Int) {
        val currentPages = workspaceView.pageCount
        if (currentPages < count) {
            for (i in currentPages until count) {
                val page = CellLayout(this).apply {
                    pageIndex = i
                    setGridSize(prefs.desktopColumns, prefs.desktopRows)
                    setOnTouchListener { _, event ->
                        gestureController.onEmptyWorkspaceTouch(event)
                    }
                }
                workspaceView.addView(page)
            }
        } else if (currentPages > count) {
            for (i in currentPages - 1 downTo count) {
                workspaceView.removeViewAt(i)
            }
        }
        updatePageIndicator(workspaceView.currentPage)
    }

    private fun loadWorkspace() {
        lifecycleScope.launch {
            val loadedItems = workspaceRepo.loadItems()
            workspaceItems.clear()

            if (!prefs.isWorkspaceInitialized || loadedItems.isEmpty()) {
                setupDefaultWorkspace()
            } else {
                workspaceItems.addAll(loadedItems)
                validateAndRepairWorkspace()
                refreshWorkspaceViews()
                workspaceView.snapToPage(prefs.defaultPage, smooth = false)
            }
        }
    }

    private fun validateAndRepairWorkspace() {
        val maxScreenId = workspaceItems.map { it.screenId }.filter { it >= 0 }.maxOrNull() ?: 0
        val pageCount = (maxScreenId + 1).coerceAtLeast(1)

        val occupancyGrids = Array(pageCount) {
            Array(prefs.desktopRows) { BooleanArray(prefs.desktopColumns) }
        }

        var hasRepairs = false
        val repairedItems = mutableListOf<WorkspaceItem>()

        for (item in workspaceItems) {
            if (item.screenId == -1) {
                repairedItems.add(item)
                continue
            }

            var p = item.screenId.coerceAtLeast(0)
            var cX = item.cellX
            var cY = item.cellY
            val sX = item.spanX
            val sY = item.spanY

            var isConflict = false
            if (p >= occupancyGrids.size || cX < 0 || cY < 0 || cX + sX > prefs.desktopColumns || cY + sY > prefs.desktopRows) {
                isConflict = true
            } else {
                for (r in cY until (cY + sY).coerceAtMost(prefs.desktopRows)) {
                    for (c in cX until (cX + sX).coerceAtMost(prefs.desktopColumns)) {
                        if (occupancyGrids[p][r][c]) {
                            isConflict = true
                            break
                        }
                    }
                }
            }

            if (isConflict) {
                hasRepairs = true
                val freeCell = findFreeCell(occupancyGrids, p, sX, sY)
                if (freeCell != null) {
                    item.screenId = freeCell.first
                    item.cellX = freeCell.second
                    item.cellY = freeCell.third
                    p = item.screenId
                    cX = item.cellX
                    cY = item.cellY
                    Log.d("WorkspaceLayout", "REPAIRED item ${item.id} to Page $p ($cX, $cY)")
                }
            }

            if (p < occupancyGrids.size) {
                for (r in cY until (cY + sY).coerceAtMost(prefs.desktopRows)) {
                    for (c in cX until (cX + sX).coerceAtMost(prefs.desktopColumns)) {
                        if (r < prefs.desktopRows && c < prefs.desktopColumns) {
                            occupancyGrids[p][r][c] = true
                        }
                    }
                }
            }
            repairedItems.add(item)
        }

        if (hasRepairs) {
            workspaceItems.clear()
            workspaceItems.addAll(repairedItems)
            workspaceRepo.saveItems(workspaceItems)
            Log.d("WorkspaceLayout", "Repaired layout coordinates successfully.")
        }
    }

    private fun findFreeCell(occupancy: Array<Array<BooleanArray>>, startPage: Int, spanX: Int, spanY: Int): Triple<Int, Int, Int>? {
        for (p in startPage until occupancy.size) {
            for (r in 0..prefs.desktopRows - spanY) {
                for (c in 0..prefs.desktopColumns - spanX) {
                    var available = true
                    for (row in r until (r + spanY)) {
                        for (col in c until (c + spanX)) {
                            if (occupancy[p][row][col]) {
                                available = false
                                break
                            }
                        }
                    }
                    if (available) {
                        return Triple(p, c, r)
                    }
                }
            }
        }
        return Triple(occupancy.size, 0, 0)
    }

    private fun setupDefaultWorkspace() {
        lifecycleScope.launch {
            val installed = appRepo.getInstalledApps()
            val initialItems = FirstRunOrganizer.generateInitialWorkspace(this@LauncherActivity, installed, prefs)

            workspaceItems.clear()
            workspaceItems.addAll(initialItems)
            validateAndRepairWorkspace()
            workspaceRepo.saveItems(workspaceItems)
            prefs.isWorkspaceInitialized = true

            refreshWorkspaceViews()
            workspaceView.snapToPage(0, smooth = false)
        }
    }

    private fun refreshWorkspaceViews() {
        val maxScreenId = workspaceItems.map { it.screenId }.filter { it >= 0 }.maxOrNull() ?: 0
        val neededPages = (maxScreenId + 1).coerceAtLeast(1)
        refreshWorkspacePageCount(neededPages)

        for (p in 0 until workspaceView.pageCount) {
            workspaceView.getCellLayout(p)?.apply {
                pageIndex = p
                removeAllViews()
            }
        }
        dockView.cellLayout.removeAllViews()

        for (item in workspaceItems) {
            val view: View = when (item) {
                is WorkspaceItem.ClockDateCardItem -> {
                    ClockDateCardView(this).apply {
                        setOnTouchListener { v, event ->
                            interactionController.onItemTouch(v, item, item.screenId, event)
                        }
                    }
                }
                else -> {
                    WorkspaceItemView(this).apply {
                        bind(item, iconHelper, prefs, isDock = (item.screenId == -1))

                        setOnTouchListener { v, event ->
                            interactionController.onItemTouch(v, item, item.screenId, event)
                        }
                    }
                }
            }

            val lp = CellLayout.LayoutParams(item.spanX, item.spanY, item.cellX, item.cellY)

            if (item.screenId == -1) {
                dockView.cellLayout.addView(view, lp)
            } else if (item.screenId in 0 until workspaceView.pageCount) {
                workspaceView.getCellLayout(item.screenId)?.addView(view, lp)
            }
        }
    }

    private fun refreshNotificationBadges() {
        val activePkgs = LiteNotificationListener.activeNotificationPackages
        for (p in 0 until workspaceView.pageCount) {
            val cellLayout = workspaceView.getCellLayout(p) ?: continue
            for (i in 0 until cellLayout.childCount) {
                val child = cellLayout.getChildAt(i) as? WorkspaceItemView ?: continue
                val item = child.item as? WorkspaceItem.AppItem ?: continue
                child.hasNotificationBadge = activePkgs.contains(item.packageName)
            }
        }
    }

    private fun openFolder(folder: WorkspaceItem.FolderItem) {
        FolderDialog(
            context = this,
            folderItem = folder,
            iconHelper = iconHelper,
            prefs = prefs,
            onAppClick = { app ->
                launchApp(app.packageName, app.activityName)
            },
            onAppLongClick = { app ->
                folder.items.remove(app)

                val page = workspaceView.currentPage
                val cellLayout = workspaceView.getCellLayout(page)
                val freeCell = cellLayout?.findNearestVacantCell() ?: Pair(0, 0)

                val newAppItem = WorkspaceItem.AppItem(
                    id = System.currentTimeMillis(),
                    screenId = page,
                    cellX = freeCell.first,
                    cellY = freeCell.second,
                    packageName = app.packageName,
                    activityName = app.activityName
                )
                workspaceItems.add(newAppItem)

                if (folder.items.size == 1) {
                    val remainingApp = folder.items.removeAt(0)
                    val remainingItem = WorkspaceItem.AppItem(
                        id = System.currentTimeMillis(),
                        screenId = folder.screenId,
                        cellX = folder.cellX,
                        cellY = folder.cellY,
                        packageName = remainingApp.packageName,
                        activityName = remainingApp.activityName
                    )
                    workspaceItems.remove(folder)
                    workspaceItems.add(remainingItem)
                } else if (folder.items.isEmpty()) {
                    workspaceItems.remove(folder)
                }

                workspaceRepo.saveItems(workspaceItems)
                postRefreshWorkspaceViews()
                Toast.makeText(this, "Moved ${app.customLabel ?: app.packageName.substringAfterLast('.')} out of folder", Toast.LENGTH_SHORT).show()
            },
            onTitleChanged = {
                workspaceRepo.saveItems(workspaceItems)
                postRefreshWorkspaceViews()
            }
        ).show()
    }

    private fun postRefreshWorkspaceViews() {
        rootContainer.post {
            refreshWorkspaceViews()
        }
    }

    private fun launchApp(packageName: String, activityName: String, sourceView: View? = null) {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setClassName(packageName, activityName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
            val options = if (sourceView != null) {
                androidx.core.app.ActivityOptionsCompat.makeScaleUpAnimation(
                    sourceView,
                    0, 0,
                    sourceView.width.coerceAtLeast(1),
                    sourceView.height.coerceAtLeast(1)
                ).toBundle()
            } else null

            startActivity(intent, options)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to launch app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchShortcut(shortcut: WorkspaceItem.ShortcutItem) {
        try {
            val intent = Intent.parseUri(shortcut.intentUri, Intent.URI_INTENT_SCHEME)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Shortcut broken", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAppLongClickMenu(app: LaunchableApp, view: View) {
        val popup = PopupMenu(this, view, Gravity.TOP or Gravity.CENTER_HORIZONTAL)
        popup.menu.add("App Info")
        popup.menu.add("Uninstall")
        popup.menu.add("Add to Home")
        popup.menu.add(if (app.isHidden) "Unhide" else "Hide App")

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.title) {
                "App Info" -> openAppInfo(app.packageName)
                "Uninstall" -> requestUninstall(app.packageName)
                "Add to Home" -> addAppToHome(app)
                "Hide App" -> toggleAppHidden(app.packageName, true)
                "Unhide" -> toggleAppHidden(app.packageName, false)
            }
            true
        }
        popup.show()
    }

    private fun showWorkspaceAppLongClickMenu(item: WorkspaceItem.AppItem, view: View) {
        val anchorView = (view as? WorkspaceItemView)?.iconView ?: view
        val popup = PopupMenu(this, anchorView, Gravity.TOP or Gravity.CENTER_HORIZONTAL)
        popup.menu.add("App Info")
        popup.menu.add("Uninstall")
        popup.menu.add("Remove from Home")

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.title) {
                "App Info" -> openAppInfo(item.packageName)
                "Uninstall" -> requestUninstall(item.packageName)
                "Remove from Home" -> removeFromHome(item)
            }
            true
        }
        popup.show()
    }

    private fun showFolderLongClickMenu(item: WorkspaceItem.FolderItem, view: View) {
        val anchorView = (view as? WorkspaceItemView)?.iconView ?: view
        val popup = PopupMenu(this, anchorView, Gravity.TOP or Gravity.CENTER_HORIZONTAL)
        popup.menu.add("Open Folder")
        popup.menu.add("Remove Folder")

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.title) {
                "Open Folder" -> openFolder(item)
                "Remove Folder" -> removeFromHome(item)
            }
            true
        }
        popup.show()
    }

    private fun removeFromHome(item: WorkspaceItem) {
        workspaceItems.remove(item)
        workspaceRepo.saveItems(workspaceItems)
        refreshWorkspaceViews()
        Toast.makeText(this, "Removed from Home", Toast.LENGTH_SHORT).show()
    }

    private fun addAppToHome(app: LaunchableApp) {
        val page = workspaceView.currentPage
        val cellLayout = workspaceView.getCellLayout(page) ?: return
        val freeCell = cellLayout.findNearestVacantCell()

        if (freeCell != null) {
            val newItem = WorkspaceItem.AppItem(
                id = System.currentTimeMillis(),
                screenId = page,
                cellX = freeCell.first,
                cellY = freeCell.second,
                packageName = app.packageName,
                activityName = app.activityName
            )
            workspaceItems.add(newItem)
            workspaceRepo.saveItems(workspaceItems)
            refreshWorkspaceViews()
            appDrawer.close()
            Toast.makeText(this, "Added to Home", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Current home screen page is full", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAppInfo(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open App Info", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestUninstall(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                data = Uri.fromParts("package", packageName, null)
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not launch uninstaller", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleAppHidden(packageName: String, hide: Boolean) {
        val hidden = prefs.getHiddenApps().toMutableSet()
        if (hide) hidden.add(packageName) else hidden.remove(packageName)
        prefs.setHiddenApps(hidden)
        lifecycleScope.launch {
            val apps = appRepo.getInstalledApps()
            appDrawer.setApps(apps, prefs)
        }
    }

    private fun executeGestureAction(action: GestureAction) {
        when (action) {
            GestureAction.OPEN_DRAWER -> appDrawer.open()
            GestureAction.OPEN_SETTINGS -> showDesktopContextMenuAt(rootContainer.width / 2f, rootContainer.height / 2f)
            GestureAction.DEFAULT_HOME_PAGE -> workspaceView.snapToPage(prefs.defaultPage)
            GestureAction.OPEN_SEARCH -> appDrawer.open()
            GestureAction.OPEN_NOTIFICATIONS -> openNotificationPanel()
            else -> {}
        }
    }

    private fun showDesktopContextMenuAt(rawX: Float, rawY: Float) {
        val rootLoc = IntArray(2)
        rootContainer.getLocationOnScreen(rootLoc)
        val localX = (rawX - rootLoc[0]).coerceIn(40f, (rootContainer.width - 40).toFloat())
        val localY = (rawY - rootLoc[1]).coerceIn(40f, (rootContainer.height - 40).toFloat())

        val tempAnchorView = View(this).apply {
            x = localX
            y = localY
        }
        rootContainer.addView(tempAnchorView, FrameLayout.LayoutParams(1, 1))

        val popup = PopupMenu(this, tempAnchorView, Gravity.NO_GRAVITY)
        popup.menu.add("Launcher Settings")
        popup.menu.add("Set as Default Launcher")
        popup.menu.add("App Drawer")

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.title) {
                "Launcher Settings" -> startActivity(Intent(this, SettingsActivity::class.java))
                "Set as Default Launcher" -> openSetDefaultLauncher()
                "App Drawer" -> appDrawer.open()
            }
            true
        }
        popup.setOnDismissListener {
            rootContainer.removeView(tempAnchorView)
        }
        popup.show()
    }

    private fun openSetDefaultLauncher() {
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

    private fun openNotificationPanel() {
        try {
            @Suppress("WRONG_DOCUMENTATION_TARGET")
            val statusBarService = getSystemService("statusbar")
            val method = statusBarService.javaClass.getMethod("expandNotificationsPanel")
            method.invoke(statusBarService)
        } catch (e: Exception) {
            try {
                @Suppress("WRONG_DOCUMENTATION_TARGET")
                val statusBarService = getSystemService("statusbar")
                val method = statusBarService.javaClass.getMethod("expand")
                method.invoke(statusBarService)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    override fun onItemClicked(item: WorkspaceItem, view: View) {
        when (item) {
            is WorkspaceItem.AppItem -> launchApp(item.packageName, item.activityName, view)
            is WorkspaceItem.FolderItem -> openFolder(item)
            is WorkspaceItem.ShortcutItem -> launchShortcut(item)
            is WorkspaceItem.ClockDateCardItem -> (view as? ClockDateCardView)?.openClockApp()
            else -> {}
        }
    }

    override fun onItemLongClicked(item: WorkspaceItem, view: View) {
        when (item) {
            is WorkspaceItem.AppItem -> showWorkspaceAppLongClickMenu(item, view)
            is WorkspaceItem.FolderItem -> showFolderLongClickMenu(item, view)
            else -> {}
        }
    }

    override fun onItemDropped(
        item: WorkspaceItem,
        fromScreen: Int,
        toScreen: Int,
        toCellX: Int,
        toCellY: Int,
        targetItem: WorkspaceItem?
    ) {
        if (targetItem is WorkspaceItem.FolderItem && item is WorkspaceItem.AppItem) {
            workspaceItems.remove(item)
            if (!targetItem.items.any { it.packageName == item.packageName }) {
                targetItem.items.add(item)
            }
        } else if (targetItem is WorkspaceItem.AppItem && item is WorkspaceItem.AppItem && targetItem.id != item.id) {
            workspaceItems.remove(item)
            workspaceItems.remove(targetItem)

            val folderItem = WorkspaceItem.FolderItem(
                id = System.currentTimeMillis(),
                screenId = toScreen,
                cellX = toCellX,
                cellY = toCellY,
                title = "Folder",
                items = mutableListOf(targetItem, item)
            )
            workspaceItems.add(folderItem)
        } else {
            item.screenId = toScreen
            item.cellX = toCellX
            item.cellY = toCellY
        }

        workspaceRepo.saveItems(workspaceItems)
        postRefreshWorkspaceViews()
    }

    private fun updatePageIndicator(activePage: Int) {
        pageIndicatorView.setPageCount(workspaceView.pageCount)
        pageIndicatorView.setActivePage(activePage)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (Intent.ACTION_MAIN == intent.action && intent.hasCategory(Intent.CATEGORY_HOME)) {
            if (appDrawer.isOpen) {
                appDrawer.close()
            } else {
                workspaceView.snapToPage(prefs.defaultPage)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (appDrawer.isOpen) {
            appDrawer.close()
        } else {
            workspaceView.snapToPage(prefs.defaultPage)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }
}
