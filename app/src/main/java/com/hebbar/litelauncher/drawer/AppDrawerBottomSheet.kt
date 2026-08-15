package com.hebbar.litelauncher.drawer

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hebbar.litelauncher.R
import com.hebbar.litelauncher.icons.AdaptiveIconHelper
import com.hebbar.litelauncher.model.LaunchableApp
import com.hebbar.litelauncher.persistence.PreferencesManager
import com.hebbar.litelauncher.util.DensityUtil
import kotlin.math.abs

class AppDrawerBottomSheet @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class DrawerState {
        CLOSED,
        PENDING,
        DRAGGING,
        SETTLING,
        OPEN
    }

    var drawerState: DrawerState = DrawerState.CLOSED
        private set

    val isOpen: Boolean get() = drawerState == DrawerState.OPEN

    var progress: Float = 0f
        private set

    lateinit var searchInput: EditText
        private set

    private lateinit var searchContainer: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppAdapter

    private var allApps: List<LaunchableApp> = emptyList()
    private var hiddenApps: Set<String> = emptySet()

    var onAppClickListener: ((LaunchableApp) -> Unit)? = null
    var onAppLongClickListener: ((LaunchableApp, View) -> Boolean)? = null
    var onDrawerProgressListener: ((progress: Float) -> Unit)? = null

    private var velocityTracker: VelocityTracker? = null
    private var activeAnimator: ValueAnimator? = null
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val minFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
    private val maxFlingVelocity = ViewConfiguration.get(context).scaledMaximumFlingVelocity

    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var drawerStartTranslationY = 0f
    private var hasVibratedOpen = false

    fun setup(
        prefs: PreferencesManager,
        iconHelper: AdaptiveIconHelper,
        appRepository: AppRepository
    ) {
        val drawerBgColor = ContextCompat.getColor(context, R.color.drawer_background)
        setBackgroundColor(drawerBgColor)
        elevation = DensityUtil.dpToPx(context, 16f).toFloat()

        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            clipChildren = true
            clipToPadding = false
        }

        searchContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(drawerBgColor)
            elevation = DensityUtil.dpToPx(context, 4f).toFloat()
            val padH = DensityUtil.dpToPx(context, 16f)
            val padV = DensityUtil.dpToPx(context, 8f)
            setPadding(padH, padV, padH, padV)
        }

        searchInput = EditText(context).apply {
            hint = "Search apps..."
            setHintTextColor(ContextCompat.getColor(context, R.color.search_hint))
            setTextColor(Color.WHITE)
            textSize = 15f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            background = ContextCompat.getDrawable(context, R.drawable.bg_search_bar)
            val searchIcon = ContextCompat.getDrawable(context, R.drawable.ic_search)
            setCompoundDrawablesWithIntrinsicBounds(searchIcon, null, null, null)
            compoundDrawablePadding = DensityUtil.dpToPx(context, 12f)
            val padH = DensityUtil.dpToPx(context, 16f)
            val padV = DensityUtil.dpToPx(context, 10f)
            setPadding(padH, padV, padH, padV)
        }

        searchContainer.addView(searchInput, LinearLayout.LayoutParams(
            0,
            DensityUtil.dpToPx(context, 48f),
            1f
        ))

        rootLayout.addView(searchContainer)

        recyclerView = RecyclerView(context).apply {
            layoutManager = GridLayoutManager(context, calculateDynamicColumns())
            clipToPadding = false
            clipChildren = true
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            val padH = DensityUtil.dpToPx(context, 12f)
            val padV = DensityUtil.dpToPx(context, 8f)
            setPadding(padH, padV, padH, padV)
        }

        adapter = AppAdapter(
            context = context,
            iconHelper = iconHelper,
            prefs = prefs,
            onAppClickListener = { app -> onAppClickListener?.invoke(app) },
            onAppLongClickListener = { app, view -> onAppLongClickListener?.invoke(app, view) ?: false }
        ).apply {
            isGridMode = true
        }

        recyclerView.adapter = adapter
        rootLayout.addView(recyclerView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        addView(rootLayout)
        visibility = VISIBLE
        setDrawerProgress(0f)

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s.toString(), prefs)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && ::recyclerView.isInitialized) {
            val cols = calculateDynamicColumns()
            (recyclerView.layoutManager as? GridLayoutManager)?.spanCount = cols
        }
        setDrawerProgress(progress)
    }

    fun applyWindowInsets(statusBarTop: Int, navBarBottom: Int) {
        searchContainer.setPadding(
            DensityUtil.dpToPx(context, 16f),
            statusBarTop + DensityUtil.dpToPx(context, 8f),
            DensityUtil.dpToPx(context, 16f),
            DensityUtil.dpToPx(context, 8f)
        )

        recyclerView.setPadding(
            DensityUtil.dpToPx(context, 12f),
            DensityUtil.dpToPx(context, 8f),
            DensityUtil.dpToPx(context, 12f),
            navBarBottom + DensityUtil.dpToPx(context, 16f)
        )
    }

    private fun calculateDynamicColumns(): Int {
        val availableWidthPx = if (width > 0) width else context.resources.displayMetrics.widthPixels
        val desiredCellWidthPx = DensityUtil.dpToPx(context, 80f)
        return (availableWidthPx / desiredCellWidthPx).coerceIn(4, 10)
    }

    fun setApps(apps: List<LaunchableApp>, prefs: PreferencesManager) {
        this.allApps = apps
        this.hiddenApps = prefs.getHiddenApps()
        filterApps(searchInput.text.toString(), prefs)
    }

    private fun filterApps(query: String, prefs: PreferencesManager) {
        val visibleApps = if (prefs.searchInHidden || query.isNotBlank()) {
            allApps
        } else {
            allApps.filterNot { hiddenApps.contains(it.packageName) }
        }

        val filtered = AppSearch.filter(visibleApps, query, prefs.getAppAliases())
        val sorted = AppRepository(context).sortApps(filtered, prefs.drawerSortMode)
        adapter.apps = sorted
    }

    fun setDrawerProgress(newProgress: Float) {
        activeAnimator?.cancel()
        val p = newProgress.coerceIn(0f, 1f)
        this.progress = p

        val drawerHeight = if (height > 0) height.toFloat() else context.resources.displayMetrics.heightPixels.toFloat()
        translationY = (1f - p) * drawerHeight

        onDrawerProgressListener?.invoke(p)
    }

    fun animateToProgress(targetProgress: Float, velocityY: Float = 0f) {
        activeAnimator?.cancel()
        val startP = progress
        val endP = targetProgress.coerceIn(0f, 1f)
        drawerState = DrawerState.SETTLING

        val drawerHeight = if (height > 0) height.toFloat() else context.resources.displayMetrics.heightPixels.toFloat()
        val distance = abs(endP - startP) * drawerHeight

        var duration = (distance / 5f).toLong().coerceIn(120L, 240L)
        if (abs(velocityY) > minFlingVelocity) {
            val flingDuration = (distance / (abs(velocityY) / 1000f)).toLong()
            duration = flingDuration.coerceIn(100L, 200L)
        }

        if (endP == 1f && !hasVibratedOpen) {
            isHapticFeedbackEnabled = true
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            hasVibratedOpen = true
        } else if (endP == 0f) {
            hasVibratedOpen = false
        }

        activeAnimator = ValueAnimator.ofFloat(startP, endP).apply {
            setDuration(duration)
            interpolator = DecelerateInterpolator(2.0f)
            addUpdateListener { anim ->
                val p = anim.animatedValue as Float
                this@AppDrawerBottomSheet.progress = p
                translationY = (1f - p) * drawerHeight
                onDrawerProgressListener?.invoke(p)
            }
            start()
        }

        activeAnimator?.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                drawerState = if (endP == 1f) DrawerState.OPEN else DrawerState.CLOSED
                if (drawerState == DrawerState.OPEN) {
                    Log.d("AppDrawer", "DrawerState=OPEN TouchOwner=RECYCLER_VIEW")
                } else {
                    Log.d("AppDrawer", "DrawerState=CLOSED")
                }
            }
        })
    }

    fun open() {
        animateToProgress(1f)
    }

    fun close() {
        animateToProgress(0f)
        searchInput.setText("")
    }

    var isWorkspaceDragging: () -> Boolean = { false }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (isWorkspaceDragging()) {
            return false
        }

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(ev)

        when (ev.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = ev.getPointerId(0)
                initialTouchX = ev.rawX
                initialTouchY = ev.rawY
                drawerStartTranslationY = translationY

                if (activeAnimator?.isRunning == true) {
                    activeAnimator?.cancel()
                    drawerState = DrawerState.DRAGGING
                    return true
                } else {
                    drawerState = if (drawerState == DrawerState.OPEN) DrawerState.OPEN else DrawerState.PENDING
                }
            }
            MotionEvent.ACTION_MOVE -> {
                var pointerIndex = ev.findPointerIndex(activePointerId)
                if (pointerIndex < 0) {
                    activePointerId = ev.getPointerId(0)
                    initialTouchX = ev.rawX
                    initialTouchY = ev.rawY
                    drawerStartTranslationY = translationY
                    pointerIndex = 0
                }

                val dx = ev.getRawX(pointerIndex) - initialTouchX
                val dy = ev.getRawY(pointerIndex) - initialTouchY

                if (drawerState == DrawerState.OPEN) {
                    val touchYLocal = ev.y
                    val isHeaderArea = touchYLocal < searchContainer.bottom

                    if (dy > touchSlop && (isHeaderArea || !recyclerView.canScrollVertically(-1))) {
                        drawerState = DrawerState.DRAGGING
                        initialTouchY = ev.getRawY(pointerIndex) - touchSlop
                        drawerStartTranslationY = translationY
                        Log.d("AppDrawer", "RecyclerAtTopOrHeader=true Gesture=DOWN TouchOwner=DRAWER")
                        return true
                    }
                    return false
                } else if (drawerState == DrawerState.PENDING || drawerState == DrawerState.CLOSED) {
                    if (dy < -touchSlop && abs(dy) > abs(dx) * 1.1f) {
                        drawerState = DrawerState.DRAGGING
                        initialTouchY = ev.getRawY(pointerIndex) + touchSlop
                        drawerStartTranslationY = translationY
                        Log.d("AppDrawer", "Gesture=UP TouchOwner=DRAWER DrawerState=DRAGGING")
                        return true
                    }
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(event)

        val drawerHeight = if (height > 0) height.toFloat() else context.resources.displayMetrics.heightPixels.toFloat()

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(0)
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                drawerStartTranslationY = translationY
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                var pointerIndex = event.findPointerIndex(activePointerId)
                if (pointerIndex < 0) {
                    activePointerId = event.getPointerId(0)
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    drawerStartTranslationY = translationY
                    drawerState = DrawerState.DRAGGING
                    pointerIndex = 0
                }

                val dy = event.getRawY(pointerIndex) - initialTouchY

                if (drawerState == DrawerState.PENDING) {
                    if (abs(dy) > touchSlop) {
                        drawerState = DrawerState.DRAGGING
                    }
                }

                if (drawerState == DrawerState.DRAGGING) {
                    val newTransY = (drawerStartTranslationY + dy).coerceIn(0f, drawerHeight)
                    val newProgress = 1f - (newTransY / drawerHeight)

                    this.progress = newProgress
                    translationY = newTransY

                    onDrawerProgressListener?.invoke(newProgress)
                    Log.d("AppDrawer", "DrawerState=DRAGGING progress=%.2f".format(newProgress))
                    return true
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = (event.action and MotionEvent.ACTION_POINTER_INDEX_MASK) shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId == activePointerId) {
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    if (newPointerIndex < event.pointerCount) {
                        activePointerId = event.getPointerId(newPointerIndex)
                        initialTouchY = event.getRawY(newPointerIndex)
                        drawerStartTranslationY = translationY
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (drawerState == DrawerState.DRAGGING) {
                    val vt = velocityTracker
                    vt?.computeCurrentVelocity(1000, maxFlingVelocity.toFloat())
                    val vY = vt?.yVelocity ?: 0f

                    val targetP = when {
                        vY > 350f -> 0f   // Flinging down closes drawer
                        vY < -350f -> 1f  // Flinging up opens drawer
                        progress < 0.82f -> 0f // Pulling down even 18% closes drawer!
                        else -> 1f
                    }
                    animateToProgress(targetP, vY)
                } else if (drawerState == DrawerState.PENDING) {
                    drawerState = DrawerState.CLOSED
                }

                velocityTracker?.recycle()
                velocityTracker = null
                activePointerId = MotionEvent.INVALID_POINTER_ID
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun MotionEvent.getRawX(pointerIndex: Int): Float {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {
                this.getRawX(pointerIndex)
            } catch (e: Exception) {
                this.rawX
            }
        } else {
            this.rawX
        }
    }

    private fun MotionEvent.getRawY(pointerIndex: Int): Float {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {
                this.getRawY(pointerIndex)
            } catch (e: Exception) {
                this.rawY
            }
        } else {
            this.rawY
        }
    }
}
