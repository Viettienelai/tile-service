package com.tilescan

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class SidebarService : AccessibilityService() {

    // Khởi tạo 2 quản lý riêng biệt
    private val volumeManager by lazy { VolumeBarManager(this) }
    private val popupManager by lazy { PopupBarManager(this) }

    override fun onServiceConnected() {
        // Kích hoạt cả 2
        popupManager.setup()  // Thanh Đỏ (Góc trên)
        volumeManager.setup() // Thanh Xanh (Góc dưới)
    }

    override fun onDestroy() {
        // Dọn dẹp khi tắt service
        popupManager.destroy()
        volumeManager.destroy()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}