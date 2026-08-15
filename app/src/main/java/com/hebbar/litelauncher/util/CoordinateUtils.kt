package com.hebbar.litelauncher.util

import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup

fun MotionEvent.toRootPoint(root: ViewGroup): PointF {
    val rootLoc = IntArray(2)
    root.getLocationOnScreen(rootLoc)
    return PointF(rawX - rootLoc[0], rawY - rootLoc[1])
}

fun View.boundsInRoot(root: ViewGroup): RectF {
    val rootLoc = IntArray(2)
    root.getLocationOnScreen(rootLoc)

    val viewLoc = IntArray(2)
    getLocationOnScreen(viewLoc)

    val left = (viewLoc[0] - rootLoc[0]).toFloat()
    val top = (viewLoc[1] - rootLoc[1]).toFloat()
    return RectF(left, top, left + width, top + height)
}

object CoordinateUtils {

    fun localPointFromRoot(rootPoint: PointF, targetView: View, root: ViewGroup): PointF {
        val rootLoc = IntArray(2)
        root.getLocationOnScreen(rootLoc)

        val targetLoc = IntArray(2)
        targetView.getLocationOnScreen(targetLoc)

        val screenX = rootPoint.x + rootLoc[0]
        val screenY = rootPoint.y + rootLoc[1]

        val localX = screenX - targetLoc[0]
        val localY = screenY - targetLoc[1]
        return PointF(localX, localY)
    }
}
