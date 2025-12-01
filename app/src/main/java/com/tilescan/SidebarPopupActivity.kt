package com.tilescan

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.*
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.core.graphics.toColorInt
import androidx.core.content.edit

@Suppress("DEPRECATION")
class SidebarPopupActivity : Activity() {
    private lateinit var root: FrameLayout
    private lateinit var container: LinearLayout
    // Dùng SharedPreferences để nhớ trạng thái Dim
    private val prefs by lazy { getSharedPreferences("tile_prefs", MODE_PRIVATE) }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(512, 512)

        // 1. Setup Popup
        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply { setColor("#FF222222".toColorInt()); cornerRadius = 55f }
            setPadding(40, 80, 40, 90)
            layoutParams = FrameLayout.LayoutParams(-1, -2).apply {
                gravity = Gravity.CENTER; setMargins(80, 0, 80, 0)
            }
        }

        // 2. Pin & Temp
        container.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER; setPadding(0, 0, 0, 60)
            addView(ImageView(context).apply { layoutParams = LinearLayout.LayoutParams(70, 40); setImageDrawable(BattDrw()) })
            addView(TextView(context).apply {
                setTextColor(Color.LTGRAY); textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setPadding(25, 0, 0, 0)
                val t = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.getIntExtra("temperature", 0) ?: 0
                text = "${t / 10f}°"
            })
        })

        // 3. Grid Tiles
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
            tile.animate().alpha(1f).translationY(0f).setStartDelay(50 + (i * 40L)).setInterpolator(OvershootInterpolator(1.2f)).setDuration(450).start()
        }
        container.addView(grid)

        // 4. Root Wrapper
        root = FrameLayout(this).apply {
            setBackgroundColor("#33FFFFFF".toColorInt()); alpha = 0f
            setOnClickListener { close() }; addView(container)
        }
        setContentView(root)

        root.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                root.viewTreeObserver.removeOnPreDrawListener(this)
                container.translationY = -150f
                container.animate().translationY(0f).setInterpolator(OvershootInterpolator(1f)).setDuration(350).start()
                root.animate().alpha(1f).setDuration(300).start()
                return true
            }
        })
    }

    private fun close() {
        root.animate().alpha(0f).setDuration(200).start()
        container.animate().translationY(-150f).alpha(0f).setDuration(200).withEndAction {
            finish(); overridePendingTransition(0, 0)
        }.start()
    }

    private fun mkTile(icon: Int, act: () -> Unit) = FrameLayout(this).apply {
        val size = 190
        layoutParams = GridLayout.LayoutParams().apply {
            width = size; height = size; setMargins(30, 40, 30, 40)
        }
        background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor("#22FFFFFF".toColorInt()) }

        addView(ImageView(context).apply {
            setImageResource(icon)
            setColorFilter(Color.WHITE)
            layoutParams = FrameLayout.LayoutParams(90, 90, Gravity.CENTER)
        })
        isClickable = true
        setOnClickListener { act(); close() }
    }

    private fun exec(pkg: String, cls: String, hist: Boolean = false, action: String? = null) {
        runCatching {
            startActivity(Intent().setClassName(pkg, cls).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); if(hist) addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY); if(action!=null) setAction(action)
            })
        }
    }

    // --- FIX LOGIC DIM ---
    private fun toggleDim() {
        runCatching {
            if (checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED) {
                // 1. Lấy trạng thái từ bộ nhớ đệm của APP (Vì không đọc được từ hệ thống)
                val isCurrentlyDim = prefs.getBoolean("is_dim_active", false)

                // 2. Đảo ngược trạng thái
                val newState = !isCurrentlyDim

                // 3. Gửi lệnh ghi vào hệ thống (Chỉ GHI, không ĐỌC)
                Settings.Secure.putInt(contentResolver, "reduce_bright_colors_activated", if (newState) 1 else 0)

                // 4. Lưu lại trạng thái mới vào bộ nhớ App
                prefs.edit { putBoolean("is_dim_active", newState) }

                // (Tùy chọn) Toast báo cho user biết
                // Toast.makeText(this, if(newState) "Đã làm tối" else "Đã làm sáng", 0).show()
            } else {
                Toast.makeText(this, "Cần quyền Secure Settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    class BattDrw : Drawable() {
        val p = Paint(1).apply { style = Paint.Style.STROKE; strokeWidth = 5f; color = Color.LTGRAY }
        val f = Paint(1).apply { color = Color.LTGRAY }
        override fun draw(c: Canvas) {
            val w = bounds.width().toFloat(); val h = bounds.height().toFloat()
            c.drawRoundRect(0f, 0f, w-10f, h, 8f, 8f, p)
            c.drawRect(w-8f, h/3f, w, h*2/3f, f)
            c.drawRoundRect(8f, 8f, w-18f, h-8f, 3f, 3f, f)
        }
        override fun setAlpha(a: Int) {}; override fun setColorFilter(cf: ColorFilter?) {}; @Deprecated(
            "Deprecated in Java"
        )
        override fun getOpacity() = PixelFormat.TRANSPARENT
    }
}