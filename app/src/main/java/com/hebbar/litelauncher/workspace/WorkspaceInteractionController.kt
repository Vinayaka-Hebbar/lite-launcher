package com.hebbar.litelauncher.workspace

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import com.hebbar.litelauncher.dock.DockView
import com.hebbar.litelauncher.gestures.LauncherGestureController
import com.hebbar.litelauncher.model.WorkspaceItem
import com.hebbar.litelauncher.util.CoordinateUtils
import com.hebbar.litelauncher.util.DensityUtil
import com.hebbar.litelauncher.util.boundsInRoot
import com.hebbar.litelauncher.util.toRootPoint
import kotlin.math.abs
import kotlin.math.hypot

class WorkspaceInteractionController(
    private val context: android.content.Context,
    private val rootContainer: FrameLayout,
    private val workspaceView: WorkspaceView,
    private val dockView: DockView
) {

    interface InteractionListener {
        fun onItemClicked(item: WorkspaceItem, view: View)
        fun onItemLongClicked(item: WorkspaceItem, view: View)
        fun onItemDropped(
            item: WorkspaceItem,
            fromScreen: Int,
            toScreen: Int,
            toCellX: Int,
            toCellY: Int,
            targetItem: WorkspaceItem?
        )
    }

    enum class State {
        IDLE,
        PRESSED,
        LONG_PRESS_READY,
        DRAGGING,
        GESTURE_ROUTING
    }

    var listener: InteractionListener? = null
    var gestureController: LauncherGestureController? = null

    val isDragging: Boolean get() = (state == State.DRAGGING)

    private var state = State.IDLE
    private var activeView: View? = null
    private var activeItem: WorkspaceItem? = null
    private var fromScreenId: Int = 0

    private var dragOverlayView: ImageView? = null
    private var touchOffsetX = 0f
    private var touchOffsetY = 0f

    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private val handler = Handler(Looper.getMainLooper())
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout()

    private val longPressRunnable = Runnable {
        if (state == State.PRESSED) {
            state = State.LONG_PRESS_READY
            triggerHapticFeedback()
            Log.d("WorkspaceInteraction", "Item LONG_PRESS_READY item=$activeItem")
        }
    }

    fun onItemTouch(v: View, item: WorkspaceItem, screenId: Int, event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                if (state != State.IDLE) return false

                state = State.PRESSED
                activeView = v
                activeItem = item
                fromScreenId = screenId

                initialTouchX = event.rawX
                initialTouchY = event.rawY

                gestureController?.onItemDown(event.rawX, event.rawY)

                v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).start()
                v.postDelayed(longPressRunnable, longPressTimeout.toLong())
                Log.d("WorkspaceInteraction", "ItemTouch DOWN item=$item")
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (state == State.GESTURE_ROUTING) {
                    return gestureController?.onEmptyWorkspaceTouch(event) ?: true
                }

                if (state == State.IDLE) return false

                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY

                if (state == State.PRESSED) {
                    if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                        v.removeCallbacks(longPressRunnable)
                        v.animate().cancel()
                        v.scaleX = 1f
                        v.scaleY = 1f

                        state = State.GESTURE_ROUTING
                        Log.d("WorkspaceInteraction", "Swipe detected before long-press, transitioning to GESTURE_ROUTING")
                        return gestureController?.onEmptyWorkspaceTouch(event) ?: true
                    }
                } else if (state == State.LONG_PRESS_READY) {
                    if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                        startDrag(event.rawX, event.rawY)
                    }
                } else if (state == State.DRAGGING) {
                    updateDragOverlayPosition(event.rawX, event.rawY)
                    checkPageEdgeScroll(event.rawX)
                    highlightTargetCell(event)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (state == State.GESTURE_ROUTING) {
                    val handled = gestureController?.onEmptyWorkspaceTouch(event) ?: true
                    finishInteraction(commitDrop = false)
                    return handled
                }

                v.removeCallbacks(longPressRunnable)
                v.animate().cancel()
                v.scaleX = 1f
                v.scaleY = 1f

                if (state == State.PRESSED) {
                    finishInteraction(commitDrop = false)
                    listener?.onItemClicked(item, v)
                } else if (state == State.LONG_PRESS_READY) {
                    val currentItem = item
                    val currentView = v
                    finishInteraction(commitDrop = false)
                    listener?.onItemLongClicked(currentItem, currentView)
                } else if (state == State.DRAGGING) {
                    handleDrop(event)
                } else {
                    finishInteraction(commitDrop = false)
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                if (state == State.DRAGGING) {
                    Log.d("WorkspaceInteraction", "ACTION_CANCEL ignored while DRAGGING")
                    return true
                }

                if (state == State.GESTURE_ROUTING) {
                    gestureController?.onEmptyWorkspaceTouch(event)
                }
                v.removeCallbacks(longPressRunnable)
                v.animate().cancel()
                v.scaleX = 1f
                v.scaleY = 1f
                finishInteraction(commitDrop = false)
                return true
            }
        }
        return false
    }

    private fun triggerHapticFeedback() {
        val v = activeView ?: return
        v.isHapticFeedbackEnabled = true
        val performed = v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        Log.d("WorkspaceInteraction", "HAPTIC result=$performed")
    }

    private fun startDrag(rawX: Float, rawY: Float) {
        val view = activeView ?: return
        state = State.DRAGGING
        Log.d("WorkspaceInteraction", "DRAG_START item=$activeItem")

        view.parent?.requestDisallowInterceptTouchEvent(true)
        rootContainer.requestDisallowInterceptTouchEvent(true)

        val bitmap = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        view.alpha = 0.35f

        touchOffsetX = view.width / 2f
        touchOffsetY = view.height / 2f

        dragOverlayView = ImageView(context).apply {
            setImageBitmap(bitmap)
            scaleX = 1.05f
            scaleY = 1.05f
            elevation = DensityUtil.dpToPx(context, 16f).toFloat()
            isClickable = false
            isFocusable = false
        }

        val lp = FrameLayout.LayoutParams(view.width, view.height)
        dragOverlayView?.layoutParams = lp
        rootContainer.addView(dragOverlayView)

        updateDragOverlayPosition(rawX, rawY)
    }

    private fun updateDragOverlayPosition(rawX: Float, rawY: Float) {
        val overlay = dragOverlayView ?: return
        val rootLoc = IntArray(2)
        rootContainer.getLocationOnScreen(rootLoc)

        overlay.x = rawX - rootLoc[0] - touchOffsetX
        overlay.y = rawY - rootLoc[1] - touchOffsetY
    }

    private var lastDockSlotHaptic: Int = -1

    private fun highlightTargetCell(event: MotionEvent) {
        for (p in 0 until workspaceView.pageCount) {
            workspaceView.getCellLayout(p)?.clearDragHighlight()
        }
        dockView.clearDockHighlight()

        val rootPoint = event.toRootPoint(rootContainer)
        val dockBoundsRoot = dockView.boundsInRoot(rootContainer)

        if (dockBoundsRoot.contains(rootPoint.x, rootPoint.y) && activeItem !is WorkspaceItem.ClockDateCardItem) {
            val dockLocal = CoordinateUtils.localPointFromRoot(rootPoint, dockView, rootContainer)
            val slotIndex = dockView.pointToSlot(dockLocal.x)

            dockView.highlightSlot(slotIndex)
            val slotRectLocal = dockView.visualRectForSlot(slotIndex)
            val slotRectRoot = RectF(
                dockBoundsRoot.left + slotRectLocal.left,
                dockBoundsRoot.top + slotRectLocal.top,
                dockBoundsRoot.left + slotRectLocal.right,
                dockBoundsRoot.top + slotRectLocal.bottom
            )

            if (slotIndex != lastDockSlotHaptic) {
                lastDockSlotHaptic = slotIndex
                rootContainer.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                Log.d("DragCoordinates", "DockRootBounds=$dockBoundsRoot CandidateSlot=$slotIndex DockPreviewRootRect=$slotRectRoot")
            }
            return
        }
        lastDockSlotHaptic = -1

        val activePage = workspaceView.currentPage
        val cellLayout = workspaceView.getCellLayout(activePage) ?: return
        val wsBoundsRoot = cellLayout.boundsInRoot(rootContainer)

        val cellLocal = CoordinateUtils.localPointFromRoot(rootPoint, cellLayout, rootContainer)
        var (cX, cY) = cellLayout.pointToCell(cellLocal.x, cellLocal.y)

        val spanX = activeItem?.spanX ?: 1
        val spanY = activeItem?.spanY ?: 1

        if (spanX >= cellLayout.countX) {
            cX = 0
        }

        val isOccupied = cellLayout.isCellOccupied(cX, cY, spanX, spanY, ignoreView = activeView)
        var targetItem: WorkspaceItem? = null

        if (isOccupied && spanX == 1 && spanY == 1 && activeItem is WorkspaceItem.AppItem) {
            val targetView = cellLayout.getItemAtCell(cX, cY) as? WorkspaceItemView
            if (targetView != null && targetView != activeView) {
                val itemRectLocal = cellLayout.visualRectForItem(cX, cY, 1, 1)
                val dist = hypot(cellLocal.x - itemRectLocal.centerX(), cellLocal.y - itemRectLocal.centerY())
                val maxRadius = itemRectLocal.width() * 0.40f
                if (dist <= maxRadius) {
                    targetItem = targetView.item
                }
            }
        }

        val isValidDrop = !isOccupied || targetItem != null
        cellLayout.setDragHighlight(cX, cY, spanX, spanY, isValid = isValidDrop)

        val placeholderLocal = cellLayout.visualRectForItem(cX, cY, spanX, spanY)
        val placeholderRoot = RectF(
            wsBoundsRoot.left + placeholderLocal.left,
            wsBoundsRoot.top + placeholderLocal.top,
            wsBoundsRoot.left + placeholderLocal.right,
            wsBoundsRoot.top + placeholderLocal.bottom
        )
        val previewCenter = PointF(placeholderRoot.centerX(), placeholderRoot.centerY())

        Log.d("DragCoordinates", "TouchRoot=$rootPoint WorkspaceRootBounds=$wsBoundsRoot CandidateCell=($cX, $cY) PreviewRootRect=$placeholderRoot PreviewCenter=$previewCenter")
    }

    private var edgeScrollHoverStart: Long = 0
    private fun checkPageEdgeScroll(rawX: Float) {
        val screenWidth = context.resources.displayMetrics.widthPixels
        val edgeWidth = DensityUtil.dpToPx(context, 32f)

        if (rawX > screenWidth - edgeWidth && workspaceView.currentPage == workspaceView.pageCount - 1) {
            if (edgeScrollHoverStart == 0L) {
                edgeScrollHoverStart = System.currentTimeMillis()
            } else if (System.currentTimeMillis() - edgeScrollHoverStart > 400) {
                val tempIndex = workspaceView.createTemporaryNextPage()
                workspaceView.snapToPage(tempIndex)
                edgeScrollHoverStart = 0L
            }
        } else if (rawX < edgeWidth && workspaceView.currentPage > 0) {
            if (edgeScrollHoverStart == 0L) {
                edgeScrollHoverStart = System.currentTimeMillis()
            } else if (System.currentTimeMillis() - edgeScrollHoverStart > 400) {
                workspaceView.snapToPage(workspaceView.currentPage - 1)
                edgeScrollHoverStart = 0L
            }
        } else {
            edgeScrollHoverStart = 0L
        }
    }

    private fun handleDrop(event: MotionEvent) {
        val item = activeItem ?: run {
            finishInteraction(commitDrop = false)
            return
        }

        val rootPoint = event.toRootPoint(rootContainer)
        val dockBoundsRoot = dockView.boundsInRoot(rootContainer)

        if (dockBoundsRoot.contains(rootPoint.x, rootPoint.y) && item !is WorkspaceItem.ClockDateCardItem) {
            val dockLocal = CoordinateUtils.localPointFromRoot(rootPoint, dockView, rootContainer)
            val slotIndex = dockView.pointToSlot(dockLocal.x)

            val isOccupied = dockView.cellLayout.isCellOccupied(slotIndex, 0, 1, 1, ignoreView = activeView)
            if (!isOccupied) {
                val slotRectLocal = dockView.visualRectForSlot(slotIndex)
                val finalRect = RectF(
                    dockBoundsRoot.left + slotRectLocal.left,
                    dockBoundsRoot.top + slotRectLocal.top,
                    dockBoundsRoot.left + slotRectLocal.right,
                    dockBoundsRoot.top + slotRectLocal.bottom
                )

                animateDropToTarget(finalRect) {
                    listener?.onItemDropped(item, fromScreenId, -1, slotIndex, 0, null)
                }
                return
            }
        }

        val targetPage = workspaceView.currentPage
        val cellLayout = workspaceView.getCellLayout(targetPage)

        if (cellLayout != null) {
            val cellLocal = CoordinateUtils.localPointFromRoot(rootPoint, cellLayout, rootContainer)
            var (cX, cY) = cellLayout.pointToCell(cellLocal.x, cellLocal.y)

            val spanX = item.spanX
            val spanY = item.spanY

            if (spanX >= cellLayout.countX) {
                cX = 0
            }

            val isOccupied = cellLayout.isCellOccupied(cX, cY, spanX, spanY, ignoreView = activeView)
            var targetItem: WorkspaceItem? = null

            if (isOccupied && spanX == 1 && spanY == 1 && item is WorkspaceItem.AppItem) {
                val targetView = cellLayout.getItemAtCell(cX, cY) as? WorkspaceItemView
                if (targetView != null && targetView != activeView) {
                    val itemRectLocal = cellLayout.visualRectForItem(cX, cY, 1, 1)
                    val dist = hypot(cellLocal.x - itemRectLocal.centerX(), cellLocal.y - itemRectLocal.centerY())
                    val maxRadius = itemRectLocal.width() * 0.40f
                    if (dist <= maxRadius) {
                        targetItem = targetView.item
                    }
                }
            }

            var finalCellX = cX
            var finalCellY = cY

            if (isOccupied && targetItem == null) {
                val vacant = cellLayout.findNearestVacantCell(spanX, spanY)
                if (vacant != null) {
                    finalCellX = vacant.first
                    finalCellY = vacant.second
                } else {
                    workspaceView.removeTemporaryPage()
                    finishInteraction(commitDrop = false)
                    return
                }
            }

            val previewLocal = cellLayout.visualRectForItem(finalCellX, finalCellY, spanX, spanY)
            val wsBoundsRoot = cellLayout.boundsInRoot(rootContainer)

            val finalRect = RectF(
                wsBoundsRoot.left + previewLocal.left,
                wsBoundsRoot.top + previewLocal.top,
                wsBoundsRoot.left + previewLocal.right,
                wsBoundsRoot.top + previewLocal.bottom
            )

            animateDropToTarget(finalRect) {
                workspaceView.commitTemporaryPage()
                listener?.onItemDropped(item, fromScreenId, targetPage, finalCellX, finalCellY, targetItem)
            }
            return
        }

        workspaceView.removeTemporaryPage()
        finishInteraction(commitDrop = false)
    }

    private fun animateDropToTarget(targetRectInRoot: RectF, onComplete: () -> Unit) {
        val overlay = dragOverlayView
        if (overlay == null) {
            onComplete()
            finishInteraction(commitDrop = true)
            return
        }

        val rootLoc = IntArray(2)
        rootContainer.getLocationOnScreen(rootLoc)
        val targetX = targetRectInRoot.left - rootLoc[0]
        val targetY = targetRectInRoot.top - rootLoc[1]

        overlay.animate()
            .x(targetX)
            .y(targetY)
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(180)
            .setInterpolator(OvershootInterpolator(1.1f))
            .withEndAction {
                onComplete()
                finishInteraction(commitDrop = true)
            }
            .start()
    }

    private fun finishInteraction(commitDrop: Boolean) {
        state = State.IDLE
        activeView?.alpha = 1.0f

        activeView?.parent?.requestDisallowInterceptTouchEvent(false)
        rootContainer.requestDisallowInterceptTouchEvent(false)

        dragOverlayView?.let {
            rootContainer.removeView(it)
        }
        dragOverlayView = null

        for (p in 0 until workspaceView.pageCount) {
            workspaceView.getCellLayout(p)?.clearDragHighlight()
        }
        dockView.clearDockHighlight()

        activeView = null
        activeItem = null
    }
}
