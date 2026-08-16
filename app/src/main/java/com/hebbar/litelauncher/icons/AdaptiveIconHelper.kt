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
            return createCopy(context.packageManager.defaultActivityIcon)
        }

        val key = "$packageName/$activityName"
        val cached = cache.get(key)
        if (cached != null) {
            return createCopy(cached)
        }

        val iconPackPackage = prefs.selectedIconPack
        if (!iconPackPackage.isNullOrEmpty()) {
            val componentName = ComponentName(packageName, activityName.ifEmpty { "Main" })
            val packIcon = iconPackManager.loadIconFromPack(iconPackPackage, componentName)
            if (packIcon != null) {
                val flattened = renderToTargetDrawable(packIcon)
                cache.put(key, flattened)
                return createCopy(flattened)
            }
        }

        val rawIcon = loadRawLauncherIcon(packageName, activityName)
        val flattened = renderToTargetDrawable(rawIcon)
        cache.put(key, flattened)
        return createCopy(flattened)
    }

    private fun createCopy(drawable: Drawable): Drawable {
        return drawable.constantState?.newDrawable()?.mutate() ?: drawable
    }

    private fun loadRawLauncherIcon(packageName: String, activityName: String): Drawable {
        val pm = context.packageManager
        val componentName = ComponentName(packageName, activityName)

        if (activityName.isNotEmpty()) {
            try {
                val icon = pm.getActivityIcon(componentName)
                if (icon != pm.defaultActivityIcon) {
                    return icon
                }
            } catch (e: Exception) {
                // Fallthrough to getApplicationIcon
            }
        }

        try {
            val appIcon = pm.getApplicationIcon(packageName)
            if (appIcon != pm.defaultActivityIcon) {
                return appIcon
            }
        } catch (e: Exception) {
            // Fallthrough to LauncherApps
        }

        try {
            val user = Process.myUserHandle()
            val info = launcherApps?.getActivityList(packageName, user)?.run {
                firstOrNull { it.componentName.className == activityName } ?: firstOrNull()
            }
            info?.getIcon(context.resources.displayMetrics.densityDpi)?.let { return it }
        } catch (e: Exception) {
            // Fallthrough to default
        }

        return pm.defaultActivityIcon
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
