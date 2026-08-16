package com.hebbar.litelauncher.drawer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import com.hebbar.litelauncher.icons.AdaptiveIconHelper
import com.hebbar.litelauncher.model.LaunchableApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(
    private val context: Context,
    private val iconHelper: AdaptiveIconHelper? = null
) {
    companion object {
        private var staticCachedApps: List<LaunchableApp> = emptyList()
    }

    private val launchCountPrefs = context.getSharedPreferences("app_launch_stats", Context.MODE_PRIVATE)
    private var onAppsChangedListener: (() -> Unit)? = null

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == Intent.ACTION_PACKAGE_ADDED ||
                action == Intent.ACTION_PACKAGE_REMOVED ||
                action == Intent.ACTION_PACKAGE_CHANGED ||
                action == Intent.ACTION_PACKAGE_REPLACED
            ) {
                staticCachedApps = emptyList()
                onAppsChangedListener?.invoke()
            }
        }
    }

    fun hasCachedApps(): Boolean = staticCachedApps.isNotEmpty()

    fun getCachedApps(): List<LaunchableApp> = staticCachedApps

    fun registerPackageReceiver(listener: () -> Unit) {
        onAppsChangedListener = listener
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(packageReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(packageReceiver, filter)
        }
    }

    fun unregisterPackageReceiver() {
        try {
            context.unregisterReceiver(packageReceiver)
        } catch (e: Exception) {
            // Ignore if not registered
        }
        onAppsChangedListener = null
    }

    suspend fun getInstalledApps(forceRefresh: Boolean = false): List<LaunchableApp> {
        if (staticCachedApps.isNotEmpty() && !forceRefresh) {
            return staticCachedApps
        }

        return withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                pm.queryIntentActivities(mainIntent, 0)
            }

            val ownPackageName = context.packageName
            val appsList = mutableListOf<LaunchableApp>()

            for (info in resolveInfos) {
                val pkgName = info.activityInfo.packageName
                if (pkgName == ownPackageName) continue

                val actName = info.activityInfo.name
                val label = info.loadLabel(pm).toString()
                
                var installTime = 0L
                try {
                    val pkgInfo = pm.getPackageInfo(pkgName, 0)
                    installTime = pkgInfo.firstInstallTime
                } catch (e: Exception) {
                    // Ignore
                }

                val stats = getLaunchStats(pkgName, actName)

                iconHelper?.getAppIcon(pkgName, actName)

                appsList.add(
                    LaunchableApp(
                        packageName = pkgName,
                        activityName = actName,
                        label = label,
                        installTime = installTime,
                        launchCount = stats.first,
                        lastLaunchTime = stats.second
                    )
                )
            }

            staticCachedApps = appsList
            appsList
        }
    }

    fun recordAppLaunch(packageName: String, activityName: String) {
        val key = "$packageName/$activityName"
        val currentCount = launchCountPrefs.getInt("${key}_count", 0)
        val now = System.currentTimeMillis()
        launchCountPrefs.edit()
            .putInt("${key}_count", currentCount + 1)
            .putLong("${key}_time", now)
            .apply()
    }

    private fun getLaunchStats(packageName: String, activityName: String): Pair<Int, Long> {
        val key = "$packageName/$activityName"
        val count = launchCountPrefs.getInt("${key}_count", 0)
        val time = launchCountPrefs.getLong("${key}_time", 0L)
        return Pair(count, time)
    }

    fun sortApps(apps: List<LaunchableApp>, sortMode: Int): List<LaunchableApp> {
        val collator = java.text.Collator.getInstance()
        return when (sortMode) {
            1 -> apps.sortedByDescending { it.installTime }
            2 -> apps.sortedWith(compareByDescending<LaunchableApp> { it.launchCount }.thenBy { it.label })
            3 -> apps.sortedWith(compareByDescending<LaunchableApp> { it.lastLaunchTime }.thenBy { it.label })
            else -> apps.sortedWith(Comparator { a, b -> collator.compare(a.label, b.label) })
        }
    }
}
