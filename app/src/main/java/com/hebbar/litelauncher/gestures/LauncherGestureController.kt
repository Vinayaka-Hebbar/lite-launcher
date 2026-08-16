package com.hebbar.litelauncher.gestures

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import com.hebbar.litelauncher.drawer.AppDrawerBottomSheet
import com.hebbar.litelauncher.workspace.WorkspaceView
import kotlin.math.abs
import kotlin.math.hypot

class LauncherGestureController(
    private val context: Context,
    private val workspaceView: WorkspaceView,
    private val appDrawer: AppDrawerBottomSheet,
    private val onOpenDesktopContextMenu: (rawX: Float, rawY: Float) -> Unit,
    private val onSwipeDownGesture: () -> Unit = {}
) {

    enum class Direction {
        UNDECIDED,
        HORIZONTAL,
        SWIPE_UP,
        SWIPE_DOWN
    }

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout()
    private val handler = Handler(Looper.getMainLooper())

    private var downX = 0f
    private var downY = 0f
    private var direction = Direction.UNDECIDED
    private var isLongPressScheduled = false
    private var velocityTracker: VelocityTracker? = null

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
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(event)

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
                    if (dy < -touchSlop && abs(dy) > abs(dx) * 1.1f) {
                        direction = Direction.SWIPE_UP
                        Log.d("LauncherGesture", "Gesture direction=SWIPE_UP TouchOwner=DRAWER")
                    } else if (dy > touchSlop && abs(dy) > abs(dx) * 1.1f) {
                        direction = Direction.SWIPE_DOWN
                        Log.d("LauncherGesture", "Gesture direction=SWIPE_DOWN TouchOwner=NOTIFICATIONS")
                    } else if (abs(dx) > abs(dy) * 1.1f) {
                        direction = Direction.HORIZONTAL
                        Log.d("LauncherGesture", "Gesture direction=HORIZONTAL TouchOwner=WORKSPACE")
                    }
                }

                if (direction == Direction.SWIPE_UP) {
                    val drawerHeight = context.resources.displayMetrics.heightPixels.toFloat()
                    val progress = (-dy / drawerHeight).coerceIn(0f, 1f)
                    appDrawer.setDrawerProgress(progress)
                    Log.d("LauncherGesture", "Drawer progress=%.2f dy=%.1f".format(progress, dy))
                    return true
                } else if (direction == Direction.SWIPE_DOWN) {
                    return true
                } else if (direction == Direction.HORIZONTAL) {
                    Log.d("LauncherGesture", "Workspace page progress dx=%.1f".format(dx))
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(emptyLongPressRunnable)
                isLongPressScheduled = false

                val vt = velocityTracker
                vt?.computeCurrentVelocity(1000)
                val vY = vt?.yVelocity ?: 0f

                if (direction == Direction.SWIPE_UP) {
                    val drawerHeight = context.resources.displayMetrics.heightPixels.toFloat()
                    val dy = event.rawY - downY
                    val progress = (-dy / drawerHeight).coerceIn(0f, 1f)

                    val targetProgress = if (vY < -300f || progress >= 0.20f) 1f else 0f
                    appDrawer.animateToProgress(targetProgress, vY)
                    direction = Direction.UNDECIDED
                    velocityTracker?.recycle()
                    velocityTracker = null
                    return true
                } else if (direction == Direction.SWIPE_DOWN) {
                    val dy = event.rawY - downY
                    if (dy > touchSlop || vY > 300f) {
                        onSwipeDownGesture()
                    }
                    direction = Direction.UNDECIDED
                    velocityTracker?.recycle()
                    velocityTracker = null
                    return true
                }

                direction = Direction.UNDECIDED
                velocityTracker?.recycle()
                velocityTracker = null
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
