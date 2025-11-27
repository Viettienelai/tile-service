package com.tilescan

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast

class QuickShareTileService : TileService() {

    override fun onClick() {
        super.onClick()

        val intent = Intent()
        // Cấu hình dựa trên ảnh bạn cung cấp
        intent.component = ComponentName(
            "com.google.android.gms",
            "com.google.android.gms.nearby.sharing.receive.ReceiveActivityReceiveActionAlias"
        )
        intent.flags = FLAG_ACTIVITY_NEW_TASK
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
            if (Build.VERSION.SDK_INT >= 34) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    1, // ID khác 0 để không trùng với Scanner
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                startActivityAndCollapse(intent)
            }
        } catch (e: Exception) {
            Log.e("QuickShareTile", "Loi: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Lỗi mở Quick Share", Toast.LENGTH_SHORT).show()
        }
    }
}