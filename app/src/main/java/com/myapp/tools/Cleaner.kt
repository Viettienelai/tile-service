package com.myapp.tools

import java.io.File

object Cleaner {
    // DANH SÁCH ĐEN: Điền đường dẫn cụ thể muốn xóa tại đây
    private val targets = listOf(
        "/storage/emulated/0/DCIM/.thumbnails",
        "/storage/emulated/0/Movies/.thumbnails",
        "/storage/emulated/0/Music/.thumbnails",
        "/storage/emulated/0/Pictures/.thumbnails",
        "/storage/emulated/0/Pictures/.camera_cache"
    )

    fun clean() {
        Thread {
            // 1. Xóa không thương tiếc danh sách chỉ định
            targets.forEach { File(it).deleteRecursively() }

            // 2. Quét dọn thư mục rỗng (chỉ quét 2 nơi này)
            scan(File("/storage/emulated/0"), true)
            scan(File("/storage/emulated/0/Android/media"), true)
        }.start()
    }

    private fun scan(d: File, root: Boolean) {
        if (!d.exists() || !d.isDirectory) return

        // Đệ quy quét con trước
        d.listFiles()?.forEach { scan(it, false) }

        // Xóa thư mục nếu rỗng (trừ thư mục gốc scan ban đầu)
        if (!root && d.listFiles()?.isEmpty() == true) d.delete()
    }
}