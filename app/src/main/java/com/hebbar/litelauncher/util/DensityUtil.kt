package com.hebbar.litelauncher.util

import android.content.Context
import android.content.res.Configuration

object DensityUtil {

    fun dpToPx(context: Context, dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }

    fun pxToDp(context: Context, px: Int): Float {
        val density = context.resources.displayMetrics.density
        return px / density
    }

    fun isTablet(context: Context): Boolean {
        val screenLayout = context.resources.configuration.screenLayout
        return (screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    fun isLandscape(context: Context): Boolean {
        return context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    /**
     * Determine default workspace grid columns and rows based on screen type & orientation.
     */
    fun getDefaultGridSize(context: Context): Pair<Int, Int> {
        val tablet = isTablet(context)
        val landscape = isLandscape(context)

        return when {
            tablet && landscape -> Pair(10, 6) // Columns x Rows
            tablet && !landscape -> Pair(7, 7)
            !tablet && landscape -> Pair(6, 4)
            else -> Pair(4, 5) // Phone portrait default: 4 x 5 (spacious & clean)
        }
    }

    /**
     * Determine default drawer grid columns based on screen width.
     */
    fun getDefaultDrawerColumns(context: Context): Int {
        val tablet = isTablet(context)
        val landscape = isLandscape(context)

        return when {
            tablet && landscape -> 9
            tablet -> 7
            landscape -> 6
            else -> 4
        }
    }
}
