package com.aetherion.noc

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.aetherion.noc.BuildConfig
import com.aetherion.noc.core.logging.AetherionTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Aetherion Mobile NOC — Application Entry Point
 * Developer: Mohammad Abdalftah Ibrahime
 *
 * Initialises: Hilt DI, Timber logging (conditional), FCM notification channels.
 */
@HiltAndroidApp
class AetherionApp : Application() {

    override fun onCreate() {
        super.onCreate()

        initLogging()
        createNotificationChannels()
    }

    // ─── Logging ─────────────────────────────────────────────────────────────

    private fun initLogging() {
        if (BuildConfig.ENABLE_LOGGING) {
            // Debug: full verbose logging
            Timber.plant(Timber.DebugTree())
        } else {
            // Production: route to Crashlytics only, strip PII
            Timber.plant(AetherionTree())
        }
    }

    // ─── Notification Channels ───────────────────────────────────────────────

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)

            val channels = listOf(
                NotificationChannel(
                    CHANNEL_CRITICAL_ALERTS,
                    "Critical Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Carrier-grade critical network alerts"
                    enableVibration(true)
                    enableLights(true)
                },
                NotificationChannel(
                    CHANNEL_MAJOR_ALERTS,
                    "Major Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Major network events requiring attention"
                },
                NotificationChannel(
                    CHANNEL_MINOR_ALERTS,
                    "Minor Alerts",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Minor network events"
                },
                NotificationChannel(
                    CHANNEL_BACKGROUND_SYNC,
                    "Background Sync",
                    NotificationManager.IMPORTANCE_MIN
                ).apply {
                    description = "Silent background data synchronization"
                    setShowBadge(false)
                }
            )

            channels.forEach { nm.createNotificationChannel(it) }
        }
    }

    companion object {
        const val CHANNEL_CRITICAL_ALERTS = "aetherion_critical"
        const val CHANNEL_MAJOR_ALERTS    = "aetherion_major"
        const val CHANNEL_MINOR_ALERTS    = "aetherion_minor"
        const val CHANNEL_BACKGROUND_SYNC = "aetherion_sync"
    }
}
