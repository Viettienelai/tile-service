package com.tilescan

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast

class ScannerTileService : TileService() {

    override fun onClick() {
        super.onClick()

        val intent = Intent()
        intent.component = ComponentName(
            "com.google.android.gms",
            "com.google.android.gms.mlkit.barcode.v2.ScannerActivity"
        )
        intent.flags = FLAG_ACTIVITY_NEW_TASK

        // Thử thêm action MAIN để Intent trông "hợp lệ" hơn với hệ thống
        intent.action = Intent.ACTION_MAIN

        if (isLocked) {
            unlockAndRun {
                launchActivityCompat(intent)
            }
        } else {
            launchActivityCompat(intent)
        }
    }

    private fun launchActivityCompat(intent: Intent) {
        try {
            // Kiểm tra nếu là Android 14 (API 34) trở lên
            if (Build.VERSION.SDK_INT >= 34) {
                // Tạo PendingIntent (Bắt buộc cho Android 14+)
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Gọi hàm dành riêng cho API 34+
                startActivityAndCollapse(pendingIntent)
                Log.d("TileScan", "Android 14+: Launched using PendingIntent")
            }
            else {
                // Các phiên bản Android cũ hơn (13 trở xuống)
                startActivityAndCollapse(intent)
                Log.d("TileScan", "Android <14: Launched using raw Intent")
            }
        } catch (e: Exception) {
            Log.e("TileScan", "Loi khoi chay: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Lỗi: Không thể mở Scanner", Toast.LENGTH_SHORT).show()
        }
    }
}