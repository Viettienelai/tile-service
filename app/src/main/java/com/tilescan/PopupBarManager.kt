package com.tilescan

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.*
import android.os.Build
import android.provider.Settings
import android.view.*
import android.view.animation.*
import android.widget.*
import androidx.core.content.edit
import androidx.core.view.isVisible

class PopupBarManager(private val ctx: Context) {

    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var barView: View? = null
    private var popupView: View? = null

    // Xác định loại Layout phù hợp cho Android mới (8.0+) và cũ
    private val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        WindowManager.LayoutParams.TYPE_PHONE
    }

    fun setup() {
        addBar(0, 300, Color.TRANSPARENT) { showPopup() }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addBar(yPos: Int, h: Int, color: Int, act: () -> Unit) {
        val v = FrameLayout(ctx).apply {
            setBackgroundColor(color)
            setLayerType(View.LAYER_TYPE_SOFTWARE, null) // Tối ưu GPU cho view tĩnh
            systemGestureExclusionRects = listOf(Rect(0, 0, 20, h))

            var x0 = 0f; var d = false
            setOnTouchListener { _, e ->
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { x0 = e.rawX; d = false }
                    MotionEvent.ACTION_MOVE -> if (!d && e.rawX - x0 < -20) { d = true; act() }
                }
                true
            }
        }
        // SỬA: Dùng layoutType đã định nghĩa ở trên thay vì số cứng 2032
        val p = WindowManager.LayoutParams(20, h, layoutType, 296, -3).apply {
            gravity = Gravity.TOP or Gravity.END
            y = yPos
        }
        wm.addView(v, p); barView = v
    }

    @SuppressLint("SetTextI18n")
    private fun showPopup() {
        if (popupView != null) return

        val root = FrameLayout(ctx).apply {
            setBackgroundColor(Color.argb(90, 100, 100, 100))
            alpha = 0f; isClickable = true
            setOnClickListener { close(this) }
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }

        val con = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL; setPadding(40, 80, 40, 90)
            layoutParams = FrameLayout.LayoutParams(-1, -2, Gravity.CENTER).apply { setMargins(80, 0, 80, 0) }
            translationY = -150f
        }

        // --- (Giữ nguyên phần UI Pin & Grid Icons như cũ) ---
        // Info: Pin & Nhiệt độ
        con.addView(LinearLayout(ctx).apply {
            gravity = 17; setPadding(0, 0, 0, 60)
            addView(ImageView(context).apply { layoutParams = LinearLayout.LayoutParams(100, 60); setImageDrawable(Batt()) })
            addView(TextView(context).apply {
                setTextColor(-1); textSize = 30f; typeface = Typeface.DEFAULT_BOLD; setPadding(30, -10, 0, 0)
                val t = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.getIntExtra("temperature", 0) ?: 0
                text = "${t / 10f}°"
            })
        })

        // Grid Icons (Rút gọn cho ngắn, logic giữ nguyên)
        val grid = GridLayout(ctx).apply { columnCount = 3; layoutParams = LinearLayout.LayoutParams(-2, -2).apply { gravity = 1 } }
        val tiles = listOf(
            R.drawable.scan to { exec("com.google.android.gms", "com.google.android.gms.mlkit.barcode.v2.ScannerActivity") },
            R.drawable.lens to { exec("com.google.android.googlequicksearchbox", "com.google.android.apps.search.lens.LensExportedActivity", true) },
            R.drawable.quickshare to { exec("com.google.android.gms", "com.google.android.gms.nearby.sharing.ReceiveUsingSamsungQrCodeMainActivity", action = Intent.ACTION_MAIN) },
            R.drawable.dim to { toggleDim() },
            R.drawable.cts to { ctx.startActivity(Intent(ctx, CtsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        )

        tiles.forEachIndexed { i, (ic, fn) ->
            val t = FrameLayout(ctx).apply {
                layoutParams = GridLayout.LayoutParams().apply { width = 190; height = 190; setMargins(30, 40, 30, 40) }
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.argb(120, 0, 0, 0)) }
                addView(ImageView(context).apply { setImageResource(ic); setColorFilter(-1); layoutParams = FrameLayout.LayoutParams(75, 75, 17) })
                setOnClickListener { fn(); close(root) }
                alpha = 0f; translationY = -80f
            }
            grid.addView(t)
            t.animate().alpha(1f).translationY(0f).setStartDelay(50 + (i * 40L)).setInterpolator(OvershootInterpolator(1.2f)).setDuration(450).start()
        }
        con.addView(grid); root.addView(con)

        root.addView(TextView(ctx).apply {
            text = "ViệtTiến┇ᴱᴸᴬᴵ"; setTextColor(-1); textSize = 13f; typeface = Typeface.DEFAULT_BOLD
            layoutParams = FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = 60 }
            translationY = 100f
            animate().translationY(0f).setDuration(350).start()
        })

        // SỬA: Thay type 2032 bằng layoutType
        val p = WindowManager.LayoutParams(-1, -1, layoutType,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_BLUR_BEHIND, -3).apply {
            blurBehindRadius = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        wm.addView(root, p); popupView = root

        root.post {
            animateBlur(root, p, 0, 500, 350)
            root.animate().alpha(1f).setDuration(350).start()
            con.animate().translationY(0f).setInterpolator(OvershootInterpolator(1f)).setDuration(350).start()
        }
    }

    private fun close(root: View) {
        if (popupView == null) return
        val con = (root as ViewGroup).getChildAt(0)
        val p = root.layoutParams as WindowManager.LayoutParams

        animateBlur(root, p, p.blurBehindRadius, 0, 200)
        con.animate().translationY(-150f).alpha(0f).setDuration(250).setInterpolator(AccelerateInterpolator()).start()
        root.animate().alpha(0f).setDuration(250).withEndAction {
            root.visibility = View.GONE
            root.post {
                if (root.isAttachedToWindow) {
                    try { wm.removeView(root) } catch(_: Exception){}
                }
                popupView = null
            }
        }.start()
    }

    private fun animateBlur(v: View, p: WindowManager.LayoutParams, f: Int, t: Int, d: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Blur chỉ hỗ trợ tốt từ Android 12+
            ValueAnimator.ofInt(f, t).apply { duration = d; addUpdateListener {
                if (v.isAttachedToWindow && v.isVisible) try { p.blurBehindRadius = it.animatedValue as Int; wm.updateViewLayout(v, p) } catch (_: Exception){}
            }}.start()
        }
    }

    private fun exec(pkg: String, cls: String, hist: Boolean = false, action: String? = null) {
        runCatching {
            ctx.startActivity(Intent().setClassName(pkg, cls).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).apply {
                if (hist) addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
                if (action != null) setAction(action)
            })
        }
    }

    private fun toggleDim() {
        if (ctx.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED) {
            val prefs = ctx.getSharedPreferences("tile_prefs", Context.MODE_PRIVATE)
            val isDim = prefs.getBoolean("dim", false)
            Settings.Secure.putInt(ctx.contentResolver, "reduce_bright_colors_activated", if (!isDim) 1 else 0)
            prefs.edit { putBoolean("dim", !isDim) }
        } else Toast.makeText(ctx, "Cần quyền Secure Settings", Toast.LENGTH_SHORT).show()
    }

    fun destroy() {
        barView?.let { if (it.isAttachedToWindow) wm.removeView(it) }
        popupView?.let { if (it.isAttachedToWindow) wm.removeView(it) }
    }

    class Batt : Drawable() {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.WHITE
        }
        private val f = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }

        override fun draw(c: Canvas) {
            val w = bounds.width().toFloat()
            val h = bounds.height().toFloat()

            // Vẽ vỏ pin
            c.drawRoundRect(2f, 2f, w - 12f, h - 2f, 6f, 6f, p)
            // Vẽ đầu pin
            c.drawRect(w - 10f, h / 3f, w, h * 2 / 3f, f)

            // Vẽ mức pin (giả lập đầy) - Bạn có thể tùy chỉnh độ rộng theo % pin thực tế nếu muốn
            c.drawRoundRect(8f, 8f, w - 20f, h - 8f, 2f, 2f, f)
        }

        override fun setAlpha(a: Int) {}
        override fun setColorFilter(cf: ColorFilter?) {}
        @Deprecated("Deprecated in Java")
        override fun getOpacity() = PixelFormat.TRANSLUCENT
    }
}