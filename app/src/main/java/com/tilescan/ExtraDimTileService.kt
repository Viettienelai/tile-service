package com.tilescan

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class ExtraDimTileService : TileService() {

    private val prefs by lazy { getSharedPreferences("tile_prefs", Context.MODE_PRIVATE) }

    override fun onStartListening() {
        updateUi()
    }

    override fun onClick() {
        // Nếu chưa có quyền thì im lặng thoát, không hiện thông báo
        if (checkSelfPermission(WRITE_SECURE_SETTINGS) != PERMISSION_GRANTED) return

        // 1. Tính toán trạng thái mới
        val newState = !prefs.getBoolean("is_dim", false)

        // 2. Gửi lệnh hệ thống (Bọc runCatching để an toàn)
        runCatching {
            Settings.Secure.putInt(contentResolver, "reduce_bright_colors_activated", if (newState) 1 else 0)
        }

        // 3. Lưu trạng thái và cập nhật icon
        prefs.edit().putBoolean("is_dim", newState).apply()
        updateUi()
    }

    private fun updateUi() {
        qsTile?.run {
            state = if (prefs.getBoolean("is_dim", false)) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateTile()
        }
    }
}