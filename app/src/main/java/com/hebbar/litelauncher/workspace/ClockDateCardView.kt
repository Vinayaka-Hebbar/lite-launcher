package com.hebbar.litelauncher.workspace

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.provider.AlarmClock
import android.text.format.DateFormat
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.hebbar.litelauncher.R
import com.hebbar.litelauncher.util.DensityUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClockDateCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    val glassCardView: LinearLayout
    private val timeView: TextView
    private val dateView: TextView

    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            updateTimeAndDate()
        }
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.TOP
        clipChildren = false
        clipToPadding = false

        glassCardView = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_clock_card)
            elevation = DensityUtil.dpToPx(context, 4f).toFloat()
            val padH = DensityUtil.dpToPx(context, 28f)
            val padV = DensityUtil.dpToPx(context, 14f)
            setPadding(padH, padV, padH, padV)
        }

        timeView = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 44f)
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setShadowLayer(4f, 0f, 2f, Color.parseColor("#80000000"))
        }

        dateView = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(Color.parseColor("#E6FFFFFF"))
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setShadowLayer(3f, 0f, 1f, Color.parseColor("#80000000"))
        }

        glassCardView.addView(timeView)
        glassCardView.addView(dateView)

        val cardHeightPx = DensityUtil.dpToPx(context, 104f)
        val marginH = DensityUtil.dpToPx(context, 14f)
        val lp = LayoutParams(LayoutParams.MATCH_PARENT, cardHeightPx).apply {
            gravity = Gravity.TOP
            setMargins(marginH, 0, marginH, 0)
        }
        addView(glassCardView, lp)
    }

    fun getVisualBoundsOnScreen(): Rect {
        val loc = IntArray(2)
        glassCardView.getLocationOnScreen(loc)
        return Rect(loc[0], loc[1], loc[0] + glassCardView.width, loc[1] + glassCardView.height)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateTimeAndDate()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        context.registerReceiver(timeReceiver, filter)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try {
            context.unregisterReceiver(timeReceiver)
        } catch (e: Exception) {
            // Ignored
        }
    }

    private fun updateTimeAndDate() {
        val now = Date()
        val is24 = DateFormat.is24HourFormat(context)
        val timePattern = if (is24) "HH:mm" else "h:mm"
        val timeFormat = SimpleDateFormat(timePattern, Locale.getDefault())
        timeView.text = timeFormat.format(now)

        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        dateView.text = dateFormat.format(now)
    }

    fun openClockApp() {
        val intents = listOf(
            Intent(AlarmClock.ACTION_SHOW_ALARMS),
            Intent(Intent.ACTION_MAIN).addCategory("android.intent.category.DESK_CLOCK"),
            Intent(AlarmClock.ACTION_SET_ALARM)
        )

        for (intent in intents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) != null) {
                try {
                    context.startActivity(intent)
                    return
                } catch (e: Exception) {
                    // Try next intent contract
                }
            }
        }
    }
}
