package com.tilescan

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent.*
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.service.quicksettings.TileService
import android.view.WindowManager
import kotlinx.coroutines.*
import org.lsposed.hiddenapibypass.HiddenApiBypass

// 1. TILE SERVICE: Nút bấm trên thanh thông báo
class CtsTileService : TileService() {
    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        val intent = Intent(this, CtsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = getActivity(this, 0, intent, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)

        // Android 14+ bắt buộc dùng PendingIntent để vừa mở App vừa đóng Panel
        startActivityAndCollapse(pendingIntent)
    }
}

// 2. ACTIVITY: Trung gian vô hình để delay
@Suppress("DEPRECATION")
class CtsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Hack: Làm trong suốt + tràn màn hình chỉ với 1 dòng cờ (Flag)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        CoroutineScope(Dispatchers.Main).launch {
            delay(350) // Delay chuẩn 350ms theo yêu cầu
            CtsHelper.trigger()
            finish()
            overridePendingTransition(0, 0) // Tắt hiệu ứng đóng
        }
    }
}

// 3. HELPER: Logic kích hoạt "tà đạo"
object CtsHelper {
    @SuppressLint("PrivateApi")
    fun trigger() {
        runCatching {
            val bundle = Bundle().apply {
                putLong("invocation_time_ms", SystemClock.elapsedRealtime())
                putInt("omni.entry_point", 1)
                putBoolean("micts_trigger", true)
            }

            // Reflection rút gọn
            val serviceManager = Class.forName("android.os.ServiceManager")
            val binder = serviceManager.getMethod("getService", String::class.java).invoke(null, "voiceinteraction") as IBinder
            val vims = Class.forName("com.android.internal.app.IVoiceInteractionManagerService\$Stub")
                .getMethod("asInterface", IBinder::class.java).invoke(null, binder)

            // Gọi hàm ẩn
            val args = arrayOf(null, bundle, 7, "hyperOS_home")
            HiddenApiBypass.invoke(Class.forName("com.android.internal.app.IVoiceInteractionManagerService"), vims, "showSessionFromSession", *args)
        }
    }
}