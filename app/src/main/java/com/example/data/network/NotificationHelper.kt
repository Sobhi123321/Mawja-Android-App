package com.example.data.network

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.AppLanguage

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "mawja_security_alerts"
    }

    init {
        createChannelIfNeeded()
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_security_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_security_desc)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun postUnknownDeviceAlert(deviceName: String, ipAddress: String, language: AppLanguage) {
        if (!hasNotificationPermission()) return

        val title = if (language == AppLanguage.AR) {
            "تنبيه أمان: جهاز غريب اتصل بالشبكة"
        } else {
            "Security Alert: Unknown device joined Wi-Fi"
        }
        val body = if (language == AppLanguage.AR) {
            "الجهاز $deviceName ($ipAddress) متصل الآن. افتح موجة للحجب أو التوثيق."
        } else {
            "$deviceName ($ipAddress) just connected. Open Mawja to block or trust."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(
                (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                notification
            )
        }
    }
}
