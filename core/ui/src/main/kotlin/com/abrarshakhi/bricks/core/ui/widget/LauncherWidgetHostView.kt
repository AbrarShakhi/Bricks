package com.abrarshakhi.bricks.core.ui.widget

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.hypot

class LauncherWidgetHostView(context: Context) : AppWidgetHostView(context) {

    var onLongPress: (() -> Unit)? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var longPressTriggered = false
    private var lastSize: SizeF? = null

    private val longPressRunnable = Runnable {
        if (isAttachedToWindow && onLongPress != null) {
            longPressTriggered = true
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            onLongPress?.invoke()
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                longPressTriggered = false
                postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE -> if (hypot(ev.x - downX, ev.y - downY) > touchSlop) {
                removeCallbacks(longPressRunnable)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> removeCallbacks(longPressRunnable)
        }
        return longPressTriggered
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (longPressTriggered) {
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                longPressTriggered = false
            }
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun cancelLongPress() {
        super.cancelLongPress()
        removeCallbacks(longPressRunnable)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(longPressRunnable)
        super.onDetachedFromWindow()
    }

    fun updateSizeDp(widthDp: Float, heightDp: Float) {
        val size = SizeF(widthDp, heightDp)
        if (size == lastSize || widthDp <= 0f || heightDp <= 0f) return
        lastSize = size
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                updateAppWidgetSize(Bundle(), listOf(size))
            } else {
                @Suppress("DEPRECATION")
                updateAppWidgetSize(null, widthDp.toInt(), heightDp.toInt(), widthDp.toInt(), heightDp.toInt())
            }
        }
    }
}
