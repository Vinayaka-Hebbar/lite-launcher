package com.hebbar.litelauncher.workspace

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.Scroller
import com.hebbar.litelauncher.gestures.LauncherGestureController
import kotlin.math.abs

class WorkspaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private val scroller = Scroller(context)
    private var velocityTracker: VelocityTracker? = null

    val pageCount: Int get() = childCount
    var currentPage: Int = 0
        private set

    var temporaryPageCreated: Boolean = false

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val minFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity

    private var initialX = 0f
    private var initialY = 0f
    private var isBeingDragged = false

    var onPageChangeListener: ((Int) -> Unit)? = null
    var gestureController: LauncherGestureController? = null

    init {
        clipChildren = false
        clipToPadding = false
    }

    fun getCellLayout(pageIndex: Int): CellLayout? {
        if (pageIndex in 0 until childCount) {
            return getChildAt(pageIndex) as? CellLayout
        }
        return null
    }

    fun createTemporaryNextPage(): Int {
        if (!temporaryPageCreated) {
            val newPageIndex = pageCount
            val page = CellLayout(context).apply {
                pageIndex = newPageIndex
                setGridSize(4, 5)
                val gc = gestureController
                if (gc != null) {
                    setOnTouchListener { _, event -> gc.onEmptyWorkspaceTouch(event) }
                }
            }
            addView(page)
            temporaryPageCreated = true
            onPageChangeListener?.invoke(currentPage)
        }
        return pageCount - 1
    }

    fun removeTemporaryPage() {
        if (temporaryPageCreated && pageCount > 1) {
            val lastIndex = pageCount - 1
            if (currentPage == lastIndex) {
                snapToPage(lastIndex - 1, smooth = false)
            }
            removeViewAt(lastIndex)
            temporaryPageCreated = false
            onPageChangeListener?.invoke(currentPage)
        }
    }

    fun commitTemporaryPage() {
        temporaryPageCreated = false
        onPageChangeListener?.invoke(currentPage)
    }

    fun snapToPage(page: Int, smooth: Boolean = true) {
        val targetPage = page.coerceIn(0, pageCount - 1)
        val targetX = targetPage * width
        val deltaX = targetX - scrollX

        if (smooth) {
            scroller.startScroll(scrollX, 0, deltaX, 0, 250)
            invalidate()
        } else {
            scrollTo(targetX, 0)
        }

        if (currentPage != targetPage) {
            currentPage = targetPage
            onPageChangeListener?.invoke(currentPage)
        }
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.currX, scroller.currY)
            postInvalidateOnAnimation()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        val childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        val childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.measure(childWidthSpec, childHeightSpec)
        }

        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        val height = b - t

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val left = i * width
            child.layout(left, 0, left + width, height)
        }

        if (changed && width > 0) {
            scrollTo(currentPage * width, 0)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                initialX = ev.x
                initialY = ev.y
                isBeingDragged = false
                if (!scroller.isFinished) {
                    scroller.abortAnimation()
                    isBeingDragged = true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.x - initialX
                val dy = ev.y - initialY
                if (abs(dx) > touchSlop && abs(dx) > abs(dy)) {
                    isBeingDragged = true
                    return true
                }
            }
        }
        return isBeingDragged
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(event)

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                initialX = event.x
                initialY = event.y
                gestureController?.onItemDown(event.rawX, event.rawY)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - initialX
                val dy = event.y - initialY
                if (abs(dy) > touchSlop && abs(dy) > abs(dx) * 1.1f) {
                    gestureController?.onEmptyWorkspaceTouch(event)
                    return true
                }
                if (isBeingDragged || abs(dx) > touchSlop) {
                    isBeingDragged = true
                    val scrollByX = (initialX - event.x).toInt()
                    initialX = event.x
                    val maxScrollX = (pageCount - 1) * width
                    val newScrollX = (scrollX + scrollByX).coerceIn(0, maxScrollX)
                    scrollTo(newScrollX, 0)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                gestureController?.onEmptyWorkspaceTouch(event)
                val vt = velocityTracker
                vt?.computeCurrentVelocity(1000)
                val velocityX = vt?.xVelocity ?: 0f

                val targetPage = when {
                    velocityX < -minFlingVelocity && currentPage < pageCount - 1 -> currentPage + 1
                    velocityX > minFlingVelocity && currentPage > 0 -> currentPage - 1
                    width > 0 -> (scrollX + width / 2) / width
                    else -> currentPage
                }

                snapToPage(targetPage, smooth = true)
                velocityTracker?.recycle()
                velocityTracker = null
                isBeingDragged = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
