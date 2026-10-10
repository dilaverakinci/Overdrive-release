package com.overdrive.app.ui.roadsense

enum class RoadSenseTab {
    GENERAL,
    MAP,
    WARNINGS,
    DATA,
    BLIND_SPOT
}

enum class WarnMode(val wireValue: String) {
    VISUAL("visual"),
    AUDIO("audio"),
    BOTH("both");

    companion object {
        fun fromWire(v: String?): WarnMode = when (v?.lowercase()) {
            "visual" -> VISUAL
            "audio" -> AUDIO
            else -> BOTH
        }
    }
}

enum class SoundChannel(val wireValue: String) {
    NAVIGATION("navigation"),
    MEDIA("media"),
    VOICE("voice"),
    ALARM("alarm");

    companion object {
        fun fromWire(v: String?): SoundChannel = when (v?.lowercase()) {
            "media" -> MEDIA
            "voice" -> VOICE
            "alarm" -> ALARM
            else -> NAVIGATION
        }
    }
}

enum class BsMergeMode(val wireValue: String) {
    BOTH("both"),
    SIDE("side"),
    REAR("rear");

    companion object {
        fun fromWire(v: String?): BsMergeMode = when (v?.lowercase()) {
            "side" -> SIDE
            "rear" -> REAR
            else -> BOTH
        }
    }
}

enum class BsDisplayTarget(val wireValue: String) {
    HEAD_UNIT("head_unit"),
    CLUSTER("cluster");

    companion object {
        fun fromWire(v: String?): BsDisplayTarget = when (v?.lowercase()) {
            "cluster" -> CLUSTER
            else -> HEAD_UNIT
        }
    }
}

data class GeneralConfig(
    val enabled: Boolean = false,
    val detectionSensitivityMult: Float = 1.0f,
    val detectionSensitivityPct: Int = 50,
    val calibrationMode: Boolean = false,
    val overlayVisible: Boolean = true
)

data class MapConfig(
    val routingConfigured: Boolean = false,
    val routingEndpoint: String = "https://api.stadiamaps.com/route/v1",
    val hasRoutingKey: Boolean = false,
    val clusterProjecting: Boolean = false,
    val autoProjectCluster: Boolean = false,
    val clusterLayout: Int = 31 // 31=10.25", 30=12.3", 29=8.8"
)

data class WarningsConfig(
    val warnEnabled: Boolean = true,
    val warnMode: WarnMode = WarnMode.BOTH,
    val warnAudioChannel: SoundChannel = SoundChannel.NAVIGATION,
    val warnAudioVolume: Int = 75, // 10..100
    val warnLeadSeconds: Int = 4, // 2..8
    val warnConfidenceThreshold: Int = 0, // 0..100
    val severityMinor: Boolean = true,
    val severityModerate: Boolean = true,
    val severitySevere: Boolean = true
)

data class DataConfig(
    val crowdUpload: Boolean = false,
    val crowdDownload: Boolean = false,
    val syncWorkerUrl: String = "https://roadsense-edge.yash321sri.workers.dev"
)

data class BlindSpotConfig(
    val enabled: Boolean = false,
    val mergeMode: BsMergeMode = BsMergeMode.BOTH,
    val rotationLeft: String = "0",
    val rotationRight: String = "0",
    val rectifyStrength: Int = 0, // 0..100
    val minSpeedKmh: Int = 0,
    val maxSpeedKmh: Int = 0,
    val suppressInReverse: Boolean = false,
    val target: BsDisplayTarget = BsDisplayTarget.HEAD_UNIT,
    val clusterLayout: Int = 31,
    val sizePct: Int = 40, // 15..90
    val cornerLeft: String = "tr", // "tl", "tr", "bl", "br", "center"
    val cornerRight: String = "tr",
    // Alignment / stitch tuning
    val rearFov: Float = 1.66f,
    val sideFov: Float = 1.98f,
    val yaw: Float = 1.23f,
    val roll: Float = 0.25f,
    val pitch: Float = -0.275f,
    val feather: Float = 0.38f,
    val projExp: Float = 1.0f,
    val rearRoll: Float = 0.0f,
    val rearPitch: Float = 0.0f,
    val debugPreviewActive: Boolean = false
)

data class RoadSenseUiState(
    val activeTab: RoadSenseTab = RoadSenseTab.GENERAL,
    val isLoading: Boolean = false,
    val general: GeneralConfig = GeneralConfig(),
    val map: MapConfig = MapConfig(),
    val warnings: WarningsConfig = WarningsConfig(),
    val data: DataConfig = DataConfig(),
    val blindSpot: BlindSpotConfig = BlindSpotConfig(),
    val bannerMessage: String? = null,
    val isBannerError: Boolean = false
)

object RoadSenseSensitivityUtils {
    const val MULT_MIN = 0.7f
    const val MULT_MAX = 1.3f
    const val MULT_DEFAULT = 1.0f

    fun pctToMult(pct: Int): Float {
        val p = pct.coerceIn(0, 100)
        return if (p >= 50) {
            val tMore = (p - 50) / 50f
            MULT_DEFAULT + tMore * (MULT_MIN - MULT_DEFAULT)
        } else {
            val tLess = (50 - p) / 50f
            MULT_DEFAULT + tLess * (MULT_MAX - MULT_DEFAULT)
        }
    }

    fun multToPct(mult: Float): Int {
        val m = mult.coerceIn(MULT_MIN, MULT_MAX)
        return if (m <= MULT_DEFAULT) {
            val tMore = (MULT_DEFAULT - m) / (MULT_DEFAULT - MULT_MIN)
            Math.round(50 + tMore * 50).coerceIn(0, 100)
        } else {
            val tLess = (m - MULT_DEFAULT) / (MULT_MAX - MULT_DEFAULT)
            Math.round(50 - tLess * 50).coerceIn(0, 100)
        }
    }
}
