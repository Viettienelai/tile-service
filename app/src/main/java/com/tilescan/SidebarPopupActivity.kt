package com.tilescan

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.*
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.core.content.edit
import androidx.core.graphics.drawable.toDrawable

@Suppress("DEPRECATION")
class SidebarPopupActivity : Activity() {

    private lateinit var root: FrameLayout
    private lateinit var container: LinearLayout
    private val prefs by lazy { getSharedPreferences("tile_prefs", MODE_PRIVATE) }

    // Biến theo dõi độ mờ hiện tại để animation đóng được mượt
    private var currentBlurRadius = 0

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(0, 0)
        super.onCreate(savedInstanceState)

        // 1. Setup Window trong suốt
        window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        window.setFormat(PixelFormat.TRANSLUCENT)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        )

        // 2. Kích hoạt cờ Blur (Android 12+) nhưng chưa set bán kính vội
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)

        // --- GIAO DIỆN ---
        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.argb(0, 255, 255, 255))
            }
            setPadding(40, 80, 40, 90)
            layoutParams = FrameLayout.LayoutParams(-1, -2).apply {
                gravity = Gravity.CENTER
                setMargins(80, 0, 80, 0)
            }
        }

        // Pin & Nhiệt độ
        container.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 60)
            addView(ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(100, 60)
                setImageDrawable(BattDrw())
            })
            addView(TextView(context).apply {
                setTextColor(Color.WHITE); textSize = 30f; typeface = Typeface.DEFAULT_BOLD; setPadding(30, -10, 0, 0)
                val t = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.getIntExtra("temperature", 0) ?: 0
                text = "${t / 10f}°"
            })
        })

        // Grid Tiles
        val grid = GridLayout(this).apply {
            columnCount = 3
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { gravity = Gravity.CENTER_HORIZONTAL }
        }

        val tiles = listOf(
            R.drawable.scan to { exec("com.google.android.gms", "com.google.android.gms.mlkit.barcode.v2.ScannerActivity") },
            R.drawable.lens to { exec("com.google.android.googlequicksearchbox", "com.google.android.apps.search.lens.LensExportedActivity", true) },
            R.drawable.quickshare to { exec("com.google.android.gms", "com.google.android.gms.nearby.sharing.ReceiveUsingSamsungQrCodeMainActivity", action = Intent.ACTION_MAIN) },
            R.drawable.dim to { toggleDim() },
            R.drawable.cts to { startActivity(Intent(this, CtsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        )

        tiles.forEachIndexed { i, (icon, act) ->
            val tile = mkTile(icon, act)
            tile.alpha = 0f; tile.translationY = -80f
            grid.addView(tile)
            tile.animate().alpha(1f).translationY(0f).setStartDelay(50 + (i * 40L))
                .setInterpolator(OvershootInterpolator(1.2f)).setDuration(450).start()
        }
        container.addView(grid)

        // Root Wrapper (Nền trắng mờ)
        root = FrameLayout(this).apply {
            // Màu nền trắng alpha 40
            setBackgroundColor(Color.argb(90, 100, 100, 100))
            // Bắt đầu với alpha 0 để fade in
            alpha = 0f
            setOnClickListener { close() }
            addView(container)
        }
        setContentView(root)

        // --- ANIMATION MỞ LÊN ---
        root.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                root.viewTreeObserver.removeOnPreDrawListener(this)

                // 1. Animation Blur (0 -> 1000) trong 350ms
                animateBlur(0, 1000, 350)

                // 2. Animation Nền trắng (Fade in) trong 350ms
                root.animate().alpha(1f).setDuration(350).start()

                // 3. Animation Container bay lên
                container.translationY = -150f
                container.animate().translationY(0f).setInterpolator(OvershootInterpolator(1f)).setDuration(350).start()

                return true
            }
        })
    }

    override fun onPause() {
        super.onPause()
        if (!isFinishing) close()
    }

    // --- ANIMATION ĐÓNG LẠI ---
    private fun close() {
        // 1. Animation Blur ngược lại (Hiện tại -> 0) trong 250ms cho nhanh gọn
        animateBlur(currentBlurRadius, 0, 250)

        // 2. Animation Nền trắng (Fade out)
        root.animate().alpha(0f).setDuration(250).start()

        // 3. Animation Container bay đi
        container.animate().translationY(-150f).alpha(0f).setDuration(250).withEndAction {
            finish()
            overridePendingTransition(0, 0)
        }.start()
    }

    // Hàm hỗ trợ animate Blur cho Window
    private fun animateBlur(fromRadius: Int, toRadius: Int, durationMs: Long) {

        ValueAnimator.ofInt(fromRadius, toRadius).apply {
            duration = durationMs
            addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                currentBlurRadius = value // Lưu lại trạng thái
                try {
                    val attrs = window.attributes
                    attrs.blurBehindRadius = value
                    window.attributes = attrs // Apply attributes mới để kích hoạt blur
                } catch (_: Exception) {
                    // Phòng trường hợp lỗi trên một số thiết bị lạ
                }
            }
            start()
        }
    }

    private fun mkTile(icon: Int, act: () -> Unit) = FrameLayout(this).apply {
        val size = 190
        layoutParams = GridLayout.LayoutParams().apply { width = size; height = size; setMargins(30, 40, 30, 40) }
        background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.argb(120, 0, 0, 0)) }
        addView(ImageView(context).apply {
            setImageResource(icon); setColorFilter(Color.WHITE)
            layoutParams = FrameLayout.LayoutParams(75, 75, Gravity.CENTER)
        })
        isClickable = true
        setOnClickListener { act(); close() }
    }

    private fun exec(pkg: String, cls: String, hist: Boolean = false, action: String? = null) {
        runCatching {
            startActivity(Intent().setClassName(pkg, cls).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (hist) addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
                if (action != null) setAction(action)
            })
        }
    }

    private fun toggleDim() {
        runCatching {
            if (checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED) {
                val isDim = prefs.getBoolean("is_dim_active", false)
                Settings.Secure.putInt(contentResolver, "reduce_bright_colors_activated", if (!isDim) 1 else 0)
                prefs.edit { putBoolean("is_dim_active", !isDim) }
            } else { Toast.makeText(this, "Cần quyền Secure Settings", Toast.LENGTH_SHORT).show() }
        }
    }

    class BattDrw : Drawable() {
        val p = Paint(1).apply { style = Paint.Style.STROKE; strokeWidth = 4f; color = Color.WHITE; isAntiAlias = true }
        val f = Paint(1).apply { color = Color.WHITE; isAntiAlias = true }
        override fun draw(c: Canvas) {
            val w = bounds.width().toFloat(); val h = bounds.height().toFloat()
            c.drawRoundRect(2f, 2f, w - 12f, h - 2f, 6f, 6f, p)
            c.drawRect(w - 10f, h / 3f, w, h * 2 / 3f, f)
            c.drawRoundRect(8f, 8f, w - 20f, h - 8f, 2f, 2f, f)
        }
        override fun setAlpha(a: Int) {}; override fun setColorFilter(cf: ColorFilter?) {}; @Deprecated("Deprecated in Java") override fun getOpacity() = PixelFormat.TRANSPARENT
    }
}