package com.example.idledisplay

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color

data class MediaState(
    val title: String = "Idle",
    val artist: String = "No Media Playing",
    val sourceApp: String = "",
    val packageName: String = "",
    val albumArt: Bitmap? = null,
    val isPlaying: Boolean = false,
    val duration: Long = 1L,
    val position: Long = 0L,
    val lastUpdate: Long = System.currentTimeMillis()
)

data class NotificationData(
    val title: String = "",
    val text: String = "",
    val icon: Bitmap? = null,
    val id: Long = 0L
)

data class AppSettings(
    val burnInProtection: Boolean = true,
    val showBattery: Boolean = true
)