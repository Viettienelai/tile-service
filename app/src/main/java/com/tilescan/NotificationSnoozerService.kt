package com.tilescan

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.os.Build

class NotificationSnoozerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        // Kiểm tra nếu thông báo đến từ Hệ thống Android
        if (sbn.packageName == "android") {
            val extras = sbn.notification.extras
            val title = extras.getString("android.title") ?: ""

            // Kiểm tra nội dung thông báo.
            // Android thường hiện: "com.tilescan is displaying over other apps"
            // Ta check xem nó có chứa tên package của mình không
            if (title.contains("com.tilescan") || title.contains("displaying over other apps")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        // Snooze nó trong 1 thời gian rất dài (ví dụ 12 tiếng)
                        // key: ID của thông báo
                        // duration: mili giây
                        snoozeNotification(sbn.key, 12 * 60 * 60 * 1000L)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}