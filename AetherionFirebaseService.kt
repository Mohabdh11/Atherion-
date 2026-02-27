package com.aetherion.noc.core.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.aetherion.noc.AetherionApp
import com.aetherion.noc.presentation.MainActivity
import timber.log.Timber
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — Push Notification Handler (stub)
// Developer: Mohammad Abdalftah Ibrahime
//
// NOTE: Firebase is not active in this build. This stub handles local
// notifications only. To enable FCM, add google-services.json and
// re-enable Firebase plugins in build.gradle.kts.
// ═══════════════════════════════════════════════════════════════════════════

class AetherionFirebaseService {

    @Inject
    lateinit var notificationManager: NotificationManager

    fun showAlertNotification(
        alertId: String,
        severity: String,
        title: String,
        body: String,
        deviceName: String,
        context: android.content.Context
    ) {
        val channelId = when (severity.uppercase()) {
            "CRITICAL" -> AetherionApp.CHANNEL_CRITICAL_ALERTS
            "MAJOR"    -> AetherionApp.CHANNEL_MAJOR_ALERTS
            "MINOR"    -> AetherionApp.CHANNEL_MINOR_ALERTS
            else       -> AetherionApp.CHANNEL_MINOR_ALERTS
        }

        val deepLinkIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = android.net.Uri.parse("aetherion://noc/alert/$alertId")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            alertId.hashCode(),
            deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val priority = when (severity.uppercase()) {
            "CRITICAL" -> NotificationCompat.PRIORITY_MAX
            "MAJOR"    -> NotificationCompat.PRIORITY_HIGH
            else       -> NotificationCompat.PRIORITY_DEFAULT
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setSubText(deviceName)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

        notificationManager.notify(alertId.hashCode(), notification)
        Timber.d("Alert notification shown: $severity - $alertId")
    }
}
