package com.hebbar.litelauncher.workspace

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import com.hebbar.litelauncher.dock.DockView
import com.hebbar.litelauncher.model.WorkspaceItem
import com.hebbar.litelauncher.util.DensityUtil

class DragAndDropManager(
    private val context: Context,
    private val workspaceView: WorkspaceView,
    private val dockView: DockView
) {
    interface DragListener {
        fun onItemDropped(
            item: WorkspaceItem,
            fromScreen: Int,
            toScreen: Int,
            toCellX: Int,
            toCellY: Int,
            targetItem: WorkspaceItem?
        )
        fun onItemRemoved(item: WorkspaceItem)
        fun onItemUninstallRequested(item: WorkspaceItem)
        fun onItemInfoRequested(item: WorkspaceItem)
    }

    var dragListener: DragListener? = null

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var dragImageView: ImageView? = null
    private var draggingItem: WorkspaceItem? = null
    private var draggedView: View? = null
    private var fromScreenId: Int = 0

    private var touchOffsetX: Float = 0f
    private var touchOffsetY: Float = 0f

    fun startDrag(view: View, item: WorkspaceItem, fromScreen: Int, initialTouchX: Float, initialTouchY: Float): Boolean {
        if (dragImageView != null) return false

        draggingItem = item
        draggedView = view
        fromScreenId = fromScreen

        // Create drag shadow bitmap
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        view.visibility = View.INVISIBLE

        touchOffsetX = view.width / 2f
        touchOffsetY = view.height / 2f

        dragImageView = ImageView(context).apply {
            setImageBitmap(bitmap)
            alpha = 0.85f
            scaleX = 1.1f
            scaleY = 1.1f
        }

        val lp = WindowManager.LayoutParams().apply {
            gravity = Gravity.TOP or Gravity.START
            x = (initialTouchX - touchOffsetX).toInt()
            y = (initialTouchY - touchOffsetY).toInt()
            width = view.width
            height = view.height
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            format = PixelFormat.TRANSLUCENT
        }

        try {
            windowManager.addView(dragImageView, lp)
        } catch (e: Exception) {
            e.printStackTrace()
            view.visibility = View.VISIBLE
            dragImageView = null
            return false
        }

        return true
    }

    fun handleTouchEvent(ev: MotionEvent): Boolean {
        if (dragImageView == null || draggingItem == null) return false

        val x = ev.rawX
        val y = ev.rawY

        when (ev.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_MOVE -> {
                updateDragPosition(x, y)
                highlightTargetCell(x, y)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                endDrag(x, y)
            }
        }
        return true
    }

    private fun updateDragPosition(x: Float, y: Float) {
        val img = dragImageView ?: return
        val lp = img.layoutParams as WindowManager.LayoutParams
        lp.x = (x - touchOffsetX).toInt()
        lp.y = (y - touchOffsetY).toInt()
        try {
            windowManager.updateViewLayout(img, lp)
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun highlightTargetCell(rawX: Float, rawY: Float) {
        // Clear highlights on all pages
        for (p in 0 until workspaceView.pageCount) {
            workspaceView.getCellLayout(p)?.clearDragHighlight()
        }
        dockView.cellLayout.clearDragHighlight()

        // Check if inside dock
        val dockLocation = IntArray(2)
        dockView.getLocationOnScreen(dockLocation)
        if (rawX >= dockLocation[0] && rawX <= dockLocation[0] + dockView.width &&
            rawY >= dockLocation[1] && rawY <= dockLocation[1] + dockView.height
        ) {
            val localX = rawX - dockLocation[0]
            val localY = rawY - dockLocation[1]
            val (cX, cY) = dockView.cellLayout.pointToCell(localX, localY)
            dockView.cellLayout.setDragHighlight(cX, cY)
            return
        }

        // Check current workspace page
        val activePage = workspaceView.currentPage
        val cellLayout = workspaceView.getCellLayout(activePage) ?: return

        val wsLocation = IntArray(2)
        cellLayout.getLocationOnScreen(wsLocation)
        val localX = rawX - wsLocation[0]
        val localY = rawY - wsLocation[1]

        val (cX, cY) = cellLayout.pointToCell(localX, localY)
        cellLayout.setDragHighlight(cX, cY, draggingItem?.spanX ?: 1, draggingItem?.spanY ?: 1)
    }

    private fun endDrag(rawX: Float, rawY: Float) {
        val item = draggingItem
        val view = draggedView

        if (dragImageView != null) {
            try {
                windowManager.removeView(dragImageView)
            } catch (e: Exception) {
                // Ignore
            }
            dragImageView = null
        }

        if (item == null || view == null) return

        // Clear drag highlight
        for (p in 0 until workspaceView.pageCount) {
            workspaceView.getCellLayout(p)?.clearDragHighlight()
        }
        dockView.cellLayout.clearDragHighlight()

        // Determine target screen and target cell
        val dockLocation = IntArray(2)
        dockView.getLocationOnScreen(dockLocation)
        val isDock = (rawX >= dockLocation[0] && rawX <= dockLocation[0] + dockView.width &&
                rawY >= dockLocation[1] && rawY <= dockLocation[1] + dockView.height)

        if (isDock) {
            val localX = rawX - dockLocation[0]
            val localY = rawY - dockLocation[1]
            val (cX, cY) = dockView.cellLayout.pointToCell(localX, localY)
            val targetView = dockView.cellLayout.getItemAtCell(cX, cY)
            val targetItem = (targetView as? WorkspaceItemView)?.item
            dragListener?.onItemDropped(item, fromScreenId, -1, cX, cY, targetItem)
        } else {
            val activePage = workspaceView.currentPage
            val cellLayout = workspaceView.getCellLayout(activePage)
            if (cellLayout != null) {
                val wsLocation = IntArray(2)
                cellLayout.getLocationOnScreen(wsLocation)
                val localX = rawX - wsLocation[0]
                val localY = rawY - wsLocation[1]
                val (cX, cY) = cellLayout.pointToCell(localX, localY)

                val targetView = cellLayout.getItemAtCell(cX, cY)
                val targetItem = (targetView as? WorkspaceItemView)?.item
                dragListener?.onItemDropped(item, fromScreenId, activePage, cX, cY, targetItem)
            } else {
                view.visibility = View.VISIBLE
            }
        }

        draggingItem = null
        draggedView = null
    }
}
