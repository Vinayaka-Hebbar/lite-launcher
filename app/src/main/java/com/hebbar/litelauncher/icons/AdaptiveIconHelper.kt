package com.hebbar.litelauncher.icons

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import com.hebbar.litelauncher.persistence.PreferencesManager
import com.hebbar.litelauncher.util.BitmapCache

class AdaptiveIconHelper(
    private val context: Context,
    private val prefs: PreferencesManager,
    private val iconPackManager: IconPackManager
) {

    private val drawableCache = LruCache<String, Drawable>(250)

    fun getAppIcon(packageName: String, activityName: String): Drawable {
        val componentName = ComponentName(packageName, activityName)
        val key = componentName.flattenToShortString()

        val cachedDrawable = drawableCache.get(key)
        if (cachedDrawable != null) {
            return cachedDrawable
        }

        val cacheKey = "app_icon/$key"
        val cachedBitmap = BitmapCache.get(cacheKey)
        if (cachedBitmap != null) {
            val drawable = BitmapDrawable(context.resources, cachedBitmap)
            drawableCache.put(key, drawable)
            return drawable
        }

        val iconPackPackage = prefs.selectedIconPack
        if (!iconPackPackage.isNullOrEmpty()) {
            val packIcon = iconPackManager.loadIconFromPack(iconPackPackage, componentName)
            if (packIcon != null) {
                drawableCache.put(key, packIcon)
                return packIcon
            }
        }

        val systemIcon = try {
            val pm = context.packageManager
            val intent = android.content.Intent().setComponent(componentName)
            val resolveInfo = pm.resolveActivity(intent, 0)
            resolveInfo?.loadIcon(pm) ?: pm.getDefaultActivityIcon()
        } catch (e: Exception) {
            context.packageManager.defaultActivityIcon
        }

        val bitmap = drawableToBitmap(systemIcon)
        BitmapCache.put(cacheKey, bitmap)
        drawableCache.put(key, systemIcon)
        return systemIcon
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
