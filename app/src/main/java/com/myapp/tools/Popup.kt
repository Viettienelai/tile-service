package com.myapp.tools

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.*
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.core.content.edit
import com.myapp.R

@Suppress("DEPRECATION")
class Popup(private val c: Context) {
    private val w = c.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val p = c.getSharedPreferences("tile_prefs", Context.MODE_PRIVATE)
    private var bar: View? = null
    private var pop: View? = null

    // ip: Dùng cho background, text và animation đóng (mượt mà, không nảy)
    private val ip = DecelerateInterpolator(2f)

    private val lpBar = WindowManager.LayoutParams(20, 300, 2038, 296, -3).apply { gravity = Gravity.TOP or Gravity.END }

    @SuppressLint("ClickableViewAccessibility")
    fun setup() {
        val v = FrameLayout(c).apply {
            setBackgroundColor(0); setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            systemGestureExclusionRects = listOf(Rect(0, 0, 20, 300))
            var x = 0f; var d = false
            setOnTouchListener { _, e ->
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { x = e.rawX; d = false }
                    MotionEvent.ACTION_MOVE -> if (!d && e.rawX - x < -20) { d = true; show() }
                }
                true
            }
        }
        w.addView(v, lpBar); bar = v
    }

    private fun keep(on: Boolean) = bar?.let {
        lpBar.flags = if (on) lpBar.flags or 128 else lpBar.flags and 128.inv()
        w.updateViewLayout(it, lpBar)
    }

    @SuppressLint("SetTextI18n")
    private fun show() {
        if (pop != null) return
        val root = FrameLayout(c).apply {
            val save = (c.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager).isPowerSaveMode
            setBackgroundColor(Color.argb(if (save) 255 else 90, 110, 110, 110))
            alpha = 0f; isClickable = true; setOnClickListener { close(this) }
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            systemUiVisibility = 1280
        }
        val grid = GridLayout(c).apply { columnCount = 3; layoutParams = FrameLayout.LayoutParams(-2, -2, 17) }
        val setOn = { on: Boolean -> p.edit { putBoolean("on", on) }; keep(on) }

        // osIp: Dùng riêng cho hiệu ứng nảy của các icon (1.5f là độ nảy)
        val osIp = OvershootInterpolator(2f)

        listOf(
            R.drawable.scan to { exec("com.google.android.gms", "com.google.android.gms.mlkit.barcode.v2.ScannerActivity") },
            R.drawable.lens to { exec("com.google.android.googlequicksearchbox", "com.google.android.apps.search.lens.LensExportedActivity", true) },
            R.drawable.quickshare to { exec("com.google.android.gms", "com.google.android.gms.nearby.sharing.ReceiveUsingSamsungQrCodeMainActivity", action = Intent.ACTION_MAIN) },
            R.drawable.screenon to { setOn(!p.getBoolean("on", false)) },
            R.drawable.dim to {
                if (c.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == 0) {
                    val d = !p.getBoolean("dim", false)
                    Settings.Secure.putInt(c.contentResolver, "reduce_bright_colors_activated", if (d) 1 else 0)
                    p.edit { putBoolean("dim", d) }
                }
            },
            R.drawable.cts to { c.startActivity(Intent(c, CtsActivity::class.java).addFlags(268435456)) },
            R.drawable.light to { setOn(true); FakeLock(c).lock { setOn(false) } },
            R.drawable.clean to { Cleaner.clean() }
        ).forEachIndexed { i, (ic, fn) ->
            grid.addView(FrameLayout(c).apply {
                layoutParams = GridLayout.LayoutParams().apply { width = 190; height = 190; setMargins(30, 40, 30, 40) }
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(if (ic == R.drawable.screenon && p.getBoolean("on", false)) Color.rgb(33, 124, 255) else Color.argb(120, 0, 0, 0)) }
                addView(ImageView(c).apply { setImageResource(ic); setColorFilter(-1); layoutParams = FrameLayout.LayoutParams(75, 75, 17) })
                setOnClickListener { fn(); close(root) }

                // Animation Overshoot cho icon/oval
                alpha = 0f; translationY = -250f; scaleX = 0.5f; scaleY = 0.1f

                // Tính toán delay: Hàng 1 (index 0,1,2) delay 0; Hàng 2 (index 3,4,5) delay 200...
                val d = (i / 3 * 100).toLong()

                animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                    .setInterpolator(osIp)
                    .setDuration(600)
                    .setStartDelay(d)
                    .start()
            })
        }

        root.addView(grid)
        root.addView(TextView(c).apply {
            text = "ViệtTiến┇ᴱᴸᴬᴵ"; setTextColor(-1); textSize = 13f; typeface = Typeface.DEFAULT_BOLD
            layoutParams = FrameLayout.LayoutParams(-2, -2, 81).apply { bottomMargin = 80 }
            // Animation Decelerate (ip) cho text
            translationY = 50f; animate().translationY(0f).setInterpolator(ip).setDuration(800).start()
        })

        val lp = WindowManager.LayoutParams(-1, -1, 2038, 262404, -3).apply { blurBehindRadius = 0; layoutInDisplayCutoutMode = 1 }
        w.addView(root, lp); pop = root
        // Animation Decelerate (ip) cho background blur và alpha toàn bộ view
        root.post { blur(root, lp, 0, 500); root.animate().alpha(1f).setInterpolator(ip).setDuration(800).start() }
    }

    private fun close(v: View) {
        if (pop == null) return
        val lp = v.layoutParams as WindowManager.LayoutParams
        blur(v, lp, lp.blurBehindRadius, 0)
        // Khi đóng dùng ip (Decelerate) để thu về mượt mà, không cần nảy
        (v as ViewGroup).getChildAt(0).animate().translationY(-50f).alpha(0f).setInterpolator(ip).setDuration(250).start()
        v.getChildAt(1).animate().translationY(50f).setInterpolator(ip).setDuration(250).start()
        v.animate().alpha(0f).setInterpolator(ip).setDuration(250).withEndAction {
            v.visibility = View.GONE
            v.post { if (v.isAttachedToWindow) runCatching { w.removeView(v) }; pop = null }
        }.start()
    }

    private fun blur(v: View, lp: WindowManager.LayoutParams, f: Int, t: Int) {
        ValueAnimator.ofInt(f, t).apply {
            duration = 800; interpolator = ip
            addUpdateListener { if (v.isAttachedToWindow) runCatching { lp.blurBehindRadius = it.animatedValue as Int; w.updateViewLayout(v, lp) } }
        }.start()
    }

    private fun exec(pkg: String, cls: String, hist: Boolean = false, action: String? = null) {
        runCatching { c.startActivity(Intent().setClassName(pkg, cls).addFlags(268435456).apply { if (hist) addFlags(1048576); if (action != null) setAction(action) }) }
    }

    fun destroy() { bar?.let { if (it.isAttachedToWindow) w.removeView(it) }; pop?.let { if (it.isAttachedToWindow) w.removeView(it) } }
}