package com.hebbar.litelauncher.gestures

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.hebbar.litelauncher.drawer.AppDrawerBottomSheet
import com.hebbar.litelauncher.workspace.WorkspaceView
import kotlin.math.abs
import kotlin.math.hypot

class LauncherGestureController(
    private val context: Context,
    private val workspaceView: WorkspaceView,
    private val appDrawer: AppDrawerBottomSheet,
    private val onOpenDesktopContextMenu: (rawX: Float, rawY: Float) -> Unit
) {

    enum class Direction {
        UNDECIDED,
        HORIZONTAL,
        VERTICAL
    }

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout()
    private val handler = Handler(Looper.getMainLooper())

    private var downX = 0f
    private var downY = 0f
    private var direction = Direction.UNDECIDED
    private var isLongPressScheduled = false

    private val emptyLongPressRunnable = Runnable {
        if (direction == Direction.UNDECIDED) {
            Log.d("LauncherGesture", "EmptyLongPress triggered at (%.1f, %.1f)".format(downX, downY))
            isLongPressScheduled = false
            workspaceView.isHapticFeedbackEnabled = true
            workspaceView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            onOpenDesktopContextMenu(downX, downY)
        }
    }

    fun onItemDown(rawX: Float, rawY: Float) {
        downX = rawX
        downY = rawY
        direction = Direction.UNDECIDED
        isLongPressScheduled = false
        handler.removeCallbacks(emptyLongPressRunnable)
        Log.d("LauncherGesture", "onItemDown recorded (%.1f, %.1f)".format(downX, downY))
    }

    fun onEmptyWorkspaceTouch(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                direction = Direction.UNDECIDED
                isLongPressScheduled = true

                handler.postDelayed(emptyLongPressRunnable, longPressTimeout.toLong())
                Log.d("LauncherGesture", "Gesture DOWN target=EMPTY TouchOwner=WORKSPACE")
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downX
                val dy = event.rawY - downY
                val distance = hypot(dx, dy)

                if (distance > touchSlop && isLongPressScheduled) {
                    handler.removeCallbacks(emptyLongPressRunnable)
                    isLongPressScheduled = false
                    Log.d("LauncherGesture", "EmptyLongPress cancelled reason=MOVEMENT")
                }

                if (distance > touchSlop && direction == Direction.UNDECIDED) {
                    if (abs(dy) > abs(dx) * 1.1f) {
                        direction = Direction.VERTICAL
                        Log.d("LauncherGesture", "Gesture direction=VERTICAL TouchOwner=DRAWER")
                    } else if (abs(dx) > abs(dy) * 1.1f) {
                        direction = Direction.HORIZONTAL
                        Log.d("LauncherGesture", "Gesture direction=HORIZONTAL TouchOwner=WORKSPACE")
                    }
                }

                if (direction == Direction.VERTICAL) {
                    val screenHeight = context.resources.displayMetrics.heightPixels.toFloat()
                    val progress = (-dy / (screenHeight * 0.55f)).coerceIn(0f, 1f)
                    appDrawer.setDrawerProgress(progress)
                    Log.d("LauncherGesture", "Drawer progress=%.2f dy=%.1f".format(progress, dy))
                    return true
                } else if (direction == Direction.HORIZONTAL) {
                    Log.d("LauncherGesture", "Workspace page progress dx=%.1f".format(dx))
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(emptyLongPressRunnable)
                isLongPressScheduled = false

                if (direction == Direction.VERTICAL) {
                    val screenHeight = context.resources.displayMetrics.heightPixels.toFloat()
                    val dy = event.rawY - downY
                    val progress = (-dy / (screenHeight * 0.55f)).coerceIn(0f, 1f)

                    val targetState = if (progress >= 0.30f) "OPEN" else "CLOSED"
                    val targetProgress = if (progress >= 0.30f) 1f else 0f

                    appDrawer.animateToProgress(targetProgress)
                    val performed = appDrawer.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    Log.d("LauncherGesture", "Drawer release target=$targetState progress=%.2f dy=%.1f Drawer haptic result=$performed".format(progress, dy))
                    direction = Direction.UNDECIDED
                    return true
                }

                direction = Direction.UNDECIDED
            }
        }
        return false
    }

    fun cancelLongPress() {
        if (isLongPressScheduled) {
            handler.removeCallbacks(emptyLongPressRunnable)
            isLongPressScheduled = false
            Log.d("LauncherGesture", "EmptyLongPress cancelled reason=EXTERNAL_CANCEL")
        }
    }
}
