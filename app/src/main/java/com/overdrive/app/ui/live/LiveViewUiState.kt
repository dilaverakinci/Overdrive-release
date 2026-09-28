package com.overdrive.app.ui.live

import androidx.annotation.StringRes
import com.overdrive.app.R

enum class LiveCameraMode(val id: Int, @StringRes val labelRes: Int, val shortCode: String) {
    ALL(0, R.string.live_camera_all, "360°"),
    FRONT(1, R.string.live_camera_front, "ÖN"),
    RIGHT(2, R.string.live_camera_right, "SAĞ"),
    REAR(3, R.string.live_camera_rear, "ARKA"),
    LEFT(4, R.string.live_camera_left, "SOL"),
    DVR(6, R.string.live_camera_dvr, "DVR");

    companion object {
        fun fromId(id: Int): LiveCameraMode = entries.firstOrNull { it.id == id } ?: ALL
    }
}

enum class StreamQuality(val key: String, val label: String, val fps: Int) {
    ULTRA_LOW("ULTRA_LOW", "Ultra Low (400k)", 15),
    LOW("LOW", "Low (600k)", 15),
    MEDIUM("MEDIUM", "Medium (1M)", 15),
    HIGH("HIGH", "High (1.5M)", 20),
    ULTRA_HIGH("ULTRA_HIGH", "Ultra High (2.5M)", 20),
    SMOOTH("SMOOTH", "Smooth (3.5M)", 25),
    MAX("MAX", "Max (5M)", 30);

    companion object {
        fun fromKey(key: String): StreamQuality = entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: MEDIUM
    }
}

enum class StreamConnectionState {
    LIVE,
    CONNECTING,
    IDLE,
    ERROR
}

data class LiveViewUiState(
    val activeCamera: LiveCameraMode = LiveCameraMode.ALL,
    val selectedQuality: StreamQuality = StreamQuality.MEDIUM,
    val connectionState: StreamConnectionState = StreamConnectionState.LIVE,
    val isRecording: Boolean = true,
    val isCabinListening: Boolean = false,
    val isFullscreen: Boolean = false,
    val isMapExpanded: Boolean = false,
    val hasOemDashcam: Boolean = true,
    val confirmDeterrentKind: String? = null,
    val statusMessage: String? = null,
    val fps: Int = 15,
    val bitrateKbps: Int = 1000,
    val vehicleLatitude: Double = 39.9255,
    val vehicleLongitude: Double = 32.8663,
    val lastGpsUpdateText: String = "Şimdi",
    val distanceToVehicleMeters: Float = 0f,
    val streamUrl: String = "http://127.0.0.1:8080/live-view.html"
)
