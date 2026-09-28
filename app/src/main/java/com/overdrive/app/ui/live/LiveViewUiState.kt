package com.overdrive.app.ui.live

import androidx.annotation.StringRes
import com.overdrive.app.R

enum class LiveCameraMode(
    val id: Int,
    @StringRes val labelRes: Int,
    @StringRes val shortLabelRes: Int,
    val shortCode: String
) {
    ALL(0, R.string.live_camera_all, R.string.live_hotspot_all, "Hepsi"),
    FRONT(1, R.string.live_camera_front, R.string.live_hotspot_front, "Ön"),
    RIGHT(2, R.string.live_camera_right, R.string.live_hotspot_right, "Sağ"),
    REAR(3, R.string.live_camera_rear, R.string.live_hotspot_rear, "Arka"),
    LEFT(4, R.string.live_camera_left, R.string.live_hotspot_left, "Sol"),
    DVR(6, R.string.live_camera_dvr, R.string.live_hotspot_dvr, "DVR");

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

    fun getLocalizedLabel(): String {
        val loc = try {
            val l = com.overdrive.app.server.LocaleManager.get()
            if (l.isNullOrBlank()) java.util.Locale.getDefault().language else l
        } catch (e: Exception) {
            java.util.Locale.getDefault().language
        }
        return if (loc.startsWith("tr", ignoreCase = true)) {
            when (this) {
                ULTRA_LOW -> "Çok Düşük (400k)"
                LOW -> "Düşük (600k)"
                MEDIUM -> "Ortalama (1M)"
                HIGH -> "Yüksek (1.5M)"
                ULTRA_HIGH -> "Çok Yüksek (2.5M)"
                SMOOTH -> "Akıcı (3.5M, 25 fps)"
                MAX -> "Maksimum (5M, 30 fps)"
            }
        } else {
            label
        }
    }

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
    val hasOemDashcam: Boolean = false,
    val confirmDeterrentKind: String? = null,
    val statusMessage: String? = null,
    val fps: Int = 15,
    val bitrateKbps: Int = 1000,
    val vehicleLatitude: Double = 39.9255,
    val vehicleLongitude: Double = 32.8663,
    val vehicleHeading: Float = 0f,
    val lastGpsUpdateText: String = "Şimdi",
    val distanceToVehicleMeters: Float = 0f,
    val vehicleModelId: String = "seal",
    val vehicleModelName: String = "BYD Seal",
    val streamUrl: String = "http://127.0.0.1:8080/live-view.html"
)
