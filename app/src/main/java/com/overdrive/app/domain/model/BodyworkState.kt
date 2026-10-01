package com.overdrive.app.domain.model

/**
 * Orientation of BYD rotating central screen (Pad).
 */
enum class PadOrientation {
    HORIZONTAL,
    VERTICAL,
    UNKNOWN
}

/**
 * Immutable domain model representing doors, windows, lighting, wipers, and bodywork sensors.
 */
data class BodyworkState(
    val isLocked: Boolean? = null,
    val doorOpenFl: Boolean = false, // Front Left
    val doorOpenFr: Boolean = false, // Front Right
    val doorOpenRl: Boolean = false, // Rear Left
    val doorOpenRr: Boolean = false, // Rear Right
    val hoodOpen: Boolean = false,
    val trunkOpen: Boolean = false,
    val chargePortOpen: Boolean = false,
    val windowPercentFl: Int = 0, // 0=closed, 100=open
    val windowPercentFr: Int = 0,
    val windowPercentRl: Int = 0,
    val windowPercentRr: Int = 0,
    val sunroofState: Int = 0,
    val sunshadePercent: Int = 0,
    val lowBeamOn: Boolean = false,
    val highBeamOn: Boolean = false,
    val frontFogOn: Boolean = false,
    val rearFogOn: Boolean = false,
    val hazardOn: Boolean = false,
    val daytimeLightOn: Boolean = false,
    val wiperActive: Boolean = false,
    val padOrientation: PadOrientation = PadOrientation.HORIZONTAL,
    val ambientColor: Int = 0,
    val ambientEnabled: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
