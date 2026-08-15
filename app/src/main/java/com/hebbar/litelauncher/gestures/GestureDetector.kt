package com.hebbar.litelauncher.gestures

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.hebbar.litelauncher.model.GestureAction
import com.hebbar.litelauncher.model.GestureType
import com.hebbar.litelauncher.persistence.PreferencesManager
import kotlin.math.abs

class LauncherGestureDetector(
    private val context: Context,
    private val prefs: PreferencesManager,
    private val onExecuteAction: (GestureAction) -> Unit
) {

    private var lastTapTime: Long = 0L

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val action = prefs.getGestureAction(GestureType.DOUBLE_TAP)
            onExecuteAction(action)
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            val action = prefs.getGestureAction(GestureType.LONG_PRESS)
            onExecuteAction(action)
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null) return false
            val dx = e2.x - e1.x
            val dy = e2.y - e1.y

            if (abs(dy) > abs(dx) && abs(dy) > 100) {
                if (dy < 0) {
                    val action = prefs.getGestureAction(GestureType.SWIPE_UP)
                    onExecuteAction(action)
                    return true
                } else if (dy > 0) {
                    val action = prefs.getGestureAction(GestureType.SWIPE_DOWN)
                    onExecuteAction(action)
                    return true
                }
            } else if (abs(dx) > abs(dy) && abs(dx) > 100) {
                if (dx < 0) {
                    val action = prefs.getGestureAction(GestureType.SWIPE_LEFT)
                    onExecuteAction(action)
                    return true
                } else if (dx > 0) {
                    val action = prefs.getGestureAction(GestureType.SWIPE_RIGHT)
                    onExecuteAction(action)
                    return true
                }
            }
            return false
        }
    }

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleEnd(detector: ScaleGestureDetector) {
            val factor = detector.scaleFactor
            if (factor > 1.2f) {
                val action = prefs.getGestureAction(GestureType.PINCH_OUT)
                onExecuteAction(action)
            } else if (factor < 0.8f) {
                val action = prefs.getGestureAction(GestureType.PINCH_IN)
                onExecuteAction(action)
            }
        }
    }

    private val androidGestureDetector = GestureDetector(context, gestureListener)
    private val androidScaleDetector = ScaleGestureDetector(context, scaleListener)

    fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = androidScaleDetector.onTouchEvent(event)
        if (!androidScaleDetector.isInProgress) {
            handled = androidGestureDetector.onTouchEvent(event) || handled
        }
        return handled
    }
}
