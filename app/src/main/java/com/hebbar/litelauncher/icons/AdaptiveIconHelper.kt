package com.hebbar.litelauncher.icons

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Process
import androidx.collection.LruCache
import com.hebbar.litelauncher.model.LaunchableApp
import com.hebbar.litelauncher.persistence.PreferencesManager
import com.hebbar.litelauncher.util.DensityUtil

class AdaptiveIconHelper(
    private val context: Context,
    private val prefs: PreferencesManager,
    private val iconPackManager: IconPackManager
) {

    private val cache = LruCache<String, Drawable>(250)
    private val targetIconSize = DensityUtil.dpToPx(context, 54f).coerceAtLeast(144)
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps

    fun getAppIcon(packageName: String, activityName: String): Drawable {
        if (packageName.isEmpty()) {
            return context.packageManager.defaultActivityIcon
        }

        val key = "$packageName/$activityName"
        val cached = cache.get(key)
        if (cached != null) {
            return cached
        }

        val iconPackPackage = prefs.selectedIconPack
        if (!iconPackPackage.isNullOrEmpty()) {
            val componentName = ComponentName(packageName, activityName.ifEmpty { "Main" })
            val packIcon = iconPackManager.loadIconFromPack(iconPackPackage, componentName)
            if (packIcon != null) {
                val flattened = renderToTargetDrawable(packIcon)
                cache.put(key, flattened)
                return flattened
            }
        }

        val rawIcon = loadRawSystemIcon(packageName, activityName)
        val flattened = renderToTargetDrawable(rawIcon)
        cache.put(key, flattened)
        return flattened
    }

    private fun loadRawSystemIcon(packageName: String, activityName: String): Drawable {
        val pm = context.packageManager
        val componentName = ComponentName(packageName, activityName.ifEmpty { "Main" })

        try {
            val user = Process.myUserHandle()
            val activities = launcherApps?.getActivityList(packageName, user)
            val info = activities?.firstOrNull { it.componentName.className == activityName }
                ?: activities?.firstOrNull()
            if (info != null) {
                val icon = info.getIcon(context.resources.displayMetrics.densityDpi)
                if (icon != null) return icon
            }
        } catch (e: Exception) {
            // Fallback to PackageManager
        }

        return try {
            if (activityName.isNotEmpty()) {
                pm.getActivityIcon(componentName)
            } else {
                pm.getApplicationIcon(packageName)
            }
        } catch (e: Exception) {
            try {
                pm.getApplicationIcon(packageName)
            } catch (ex: Exception) {
                pm.defaultActivityIcon
            }
        }
    }

    private fun renderToTargetDrawable(drawable: Drawable): Drawable {
        val size = targetIconSize
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)

        return BitmapDrawable(context.resources, bitmap)
    }

    fun prewarmIcons(apps: List<LaunchableApp>) {
        for (app in apps) {
            getAppIcon(app.packageName, app.activityName)
        }
    }

    fun clearCache() {
        cache.evictAll()
    }
}
