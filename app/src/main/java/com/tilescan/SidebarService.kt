package com.tilescan

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.AudioManager
import android.view.*
import android.widget.FrameLayout

class SidebarService : AccessibilityService() {

    private lateinit var v: View
    private var y0 = 0f
    private var done = false

    override fun onServiceConnected() {
        v = FrameLayout(this).apply {
            // [LƯU Ý]: Đang để màu ĐỎ để test. Sửa thành Color.TRANSPARENT khi dùng thật.
            setBackgroundColor(Color.TRANSPARENT)

            setOnTouchListener { _, e ->
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { y0 = e.rawY; done = false }
                    MotionEvent.ACTION_MOVE -> if (!done) {
                        val d = e.rawY - y0
                        if (d > 30) runOnce { // Vuốt xuống -> Mở Popup
                            val intent = Intent(context, SidebarPopupActivity::class.java).apply {
                                // NEW_TASK: Bắt buộc khi gọi từ Service
                                // CLEAR_TOP: Nếu đang mở thì reset lại, tránh chồng layer
                                // NO_ANIMATION: Tắt hiệu ứng bay của hệ thống (để Activity tự lo)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                            }
                            startActivity(intent)
                        } else if (d < -30) runOnce { // Vuốt lên -> Volume
                            (getSystemService(AUDIO_SERVICE) as AudioManager)
                                .adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> done = false
                }
                true
            }
        }

        val p = WindowManager.LayoutParams(100, 1000,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            y = 200
        }

        (getSystemService(WINDOW_SERVICE) as WindowManager).addView(v, p)
    }

    private inline fun runOnce(action: () -> Unit) { done = true; action() }

    override fun onDestroy() {
        if (::v.isInitialized) (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(v)
        super.onDestroy()
    }
    override fun onAccessibilityEvent(e: android.view.accessibility.AccessibilityEvent?) {}
    override fun onInterrupt() {}
}