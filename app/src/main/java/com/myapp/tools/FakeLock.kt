package com.myapp.tools

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.*
import android.widget.FrameLayout

@Suppress("DEPRECATION")
class FakeLock(private val ctx: Context) {

    // THAY ĐỔI: Thêm tham số onUnlock (callback)
    @SuppressLint("ClickableViewAccessibility")
    fun lock(onUnlock: () -> Unit = {}) {
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val v = object : FrameLayout(ctx) {
            override fun dispatchKeyEvent(e: KeyEvent): Boolean {
                // Khi bấm Volume Up/Down để mở khóa
                if (e.action == KeyEvent.ACTION_DOWN && (e.keyCode == 24 || e.keyCode == 25)) {
                    runCatching {
                        wm.removeView(this)
                        // THAY ĐỔI: Gọi callback để báo cho Popup biết đã mở khóa
                        onUnlock()
                    }
                    return true
                }
                return super.dispatchKeyEvent(e)
            }
        }.apply {
            setBackgroundColor(Color.BLACK)
            isFocusable = true; isFocusableInTouchMode = true
            setOnTouchListener { _, _ -> true }

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
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        runCatching {
            wm.addView(v, p)
            v.post { v.requestFocus() }
        }
    }
}