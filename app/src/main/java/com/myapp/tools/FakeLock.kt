package com.myapp.tools

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.*
import android.widget.FrameLayout

@Suppress("DEPRECATION")
class FakeLock(private val ctx: Context) {
    @SuppressLint("ClickableViewAccessibility")
    fun lock() {
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val v = object : FrameLayout(ctx) {
            override fun dispatchKeyEvent(e: KeyEvent): Boolean {
                if (e.action == KeyEvent.ACTION_DOWN && (e.keyCode == 24 || e.keyCode == 25)) {
                    runCatching { wm.removeView(this) }
                    return true
                }
                return super.dispatchKeyEvent(e)
            }
        }.apply {
            setBackgroundColor(Color.BLACK)
            isFocusable = true; isFocusableInTouchMode = true
            setOnTouchListener { _, _ -> true }

            // Ẩn thanh trạng thái và điều hướng (Full Immersive)
            @Suppress("DEPRECATION")
            systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }

        val p = WindowManager.LayoutParams(-1, -1,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, -3).apply {

            // QUAN TRỌNG: Cho phép vẽ tràn qua vùng tai thỏ/camera
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        runCatching {
            wm.addView(v, p)
            v.post { v.requestFocus() }
        }
    }
}