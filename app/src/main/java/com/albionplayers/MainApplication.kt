package com.albionplayers

import android.app.*
import android.os.Build

class MainApplication : Application() {
    companion object {
        const val CHANNEL_ID = "AlbionRadarChannel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Albion Radar VPN",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when VPN capture is active"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
