package com.tilescan

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.content.Context.WINDOW_SERVICE
import android.graphics.Color
import android.graphics.Rect
import android.media.AudioManager
import android.view.*
import android.widget.FrameLayout

class VolumeBarManager(private val ctx: Context) {

    private val wm = ctx.getSystemService(WINDOW_SERVICE) as WindowManager
    private var barView: View? = null

    fun setup() {
        // Vị trí: Cách top 400px, Cao 300px, Màu Xanh
        addBar(400, 300, Color.TRANSPARENT) {
            val am = ctx.getSystemService(AUDIO_SERVICE) as AudioManager
            am.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addBar(yPos: Int, h: Int, color: Int, act: () -> Unit) {
        val v = FrameLayout(ctx).apply {
            setBackgroundColor(color)
            // Chặn cử chỉ Back của hệ thống
            addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                view.systemGestureExclusionRects = listOf(Rect(0, 0, view.width, view.height))
            }
            // Xử lý vuốt
            var x0 = 0f; var d = false
            setOnTouchListener { _, e ->
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { x0 = e.rawX; d = false }
                    MotionEvent.ACTION_MOVE -> if (!d && e.rawX - x0 < -20) { d = true; act() }
                }
                true
            }
        }

        val p = WindowManager.LayoutParams(
            20, h, 2032, // Width, Height, Type_Accessibility_Overlay
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            -3 // PixelFormat.Translucent
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            y = yPos
        }

        wm.addView(v, p)
        barView = v
    }

    fun destroy() {
        barView?.let { if (it.isAttachedToWindow) wm.removeView(it) }
    }
}