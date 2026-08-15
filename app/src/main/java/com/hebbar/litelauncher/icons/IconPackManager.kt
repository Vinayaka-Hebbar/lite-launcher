package com.hebbar.litelauncher.icons

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.hebbar.litelauncher.util.BitmapCache
import org.xmlpull.v1.XmlPullParser

class IconPackManager(private val context: Context) {

    data class IconPackInfo(
        val packageName: String,
        val label: String
    )

    fun getAvailableIconPacks(): List<IconPackInfo> {
        val pm = context.packageManager
        val iconPacks = mutableListOf<IconPackInfo>()

        val categories = arrayOf(
            "com.novalauncher.THEME",
            "org.adw.launcher.THEME",
            "com.gau.go.launcherex.theme",
            "com.dlto.atom.launcher.theme"
        )

        val foundPackages = mutableSetOf<String>()

        for (category in categories) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            val list = pm.queryIntentActivities(intent, 0)
            for (info in list) {
                val pkg = info.activityInfo.packageName
                if (foundPackages.add(pkg)) {
                    val label = info.loadLabel(pm).toString()
                    iconPacks.add(IconPackInfo(pkg, label))
                }
            }
        }
        return iconPacks
    }

    /**
     * Load custom icon from an icon pack for a given ComponentName.
     */
    fun loadIconFromPack(iconPackPackage: String, componentName: ComponentName): Drawable? {
        val cacheKey = "$iconPackPackage/${componentName.flattenToShortString()}"
        val cached = BitmapCache.get(cacheKey)
        if (cached != null) {
            return BitmapDrawable(context.resources, cached)
        }

        try {
            val pm = context.packageManager
            val res = pm.getResourcesForApplication(iconPackPackage)
            val appFilterId = res.getIdentifier("appfilter", "xml", iconPackPackage)

            if (appFilterId != 0) {
                val parser = res.getXml(appFilterId)
                var drawableName: String? = null
                val targetComponentStr = componentName.flattenToString()

                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType == XmlPullParser.START_TAG && parser.name == "item") {
                        val comp = parser.getAttributeValue(null, "component")
                        if (comp != null && comp.contains(targetComponentStr)) {
                            drawableName = parser.getAttributeValue(null, "drawable")
                            break
                        }
                    }
                }

                if (drawableName != null) {
                    val drawableId = res.getIdentifier(drawableName, "drawable", iconPackPackage)
                    if (drawableId != 0) {
                        val drawable = res.getDrawable(drawableId, null)
                        if (drawable != null) {
                            val bitmap = drawableToBitmap(drawable)
                            BitmapCache.put(cacheKey, bitmap)
                            return drawable
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
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
