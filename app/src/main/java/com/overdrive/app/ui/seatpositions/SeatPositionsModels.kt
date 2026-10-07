package com.overdrive.app.ui.seatpositions

/**
 * Data models and state definitions for the pure native Seat Positions screen.
 */

data class SeatPosition(
    val id: String,
    val name: String,
    val source: String = "user", // "user" or "car"
    val slot: Int? = null, // 1..3 for car memory slots
    val alias: String? = null,
    val ambientColour: Int? = null, // 1-based index into LightConstants.AMBIENT_COLOURS
    val axes: Map<String, Double> = emptyMap(),
    val created: Long = 0L
) {
    val isFromCar: Boolean get() = source != "user" || slot != null
    val displayName: String get() = alias?.takeIf { it.isNotBlank() } ?: name
}

data class GateState(
    val acc: Boolean = false,
    val movementBlocked: Boolean = false,
    val movementBlockReason: String? = null,
    val positioningBlocked: Boolean = false,
    val modelId: String? = null,
    val modelConfirmed: Boolean = false,
    val modelAcknowledged: Boolean = false
) {
    /**
     * Whether the motors are powered and can execute movements.
     */
    val canApply: Boolean get() = acc && !movementBlocked && !positioningBlocked
    val canSave: Boolean get() = acc
}

data class SeatPositionsState(
    val currentAxes: Map<String, Double>? = null,
    val currentAmbient: Int? = null,
    val currentProfile: String? = null,
    val matchedPosition: SeatPosition? = null,
    val positions: List<SeatPosition> = emptyList(),
    val automations: Map<String, String> = emptyMap(), // positionId -> automationName
    val gate: GateState = GateState(),
    val palette: List<String> = emptyList(),
    val colourMax: Int = 30,
    val isLoading: Boolean = false,
    val isApplying: Boolean = false,
    val applyingPositionId: String? = null,
    val error: String? = null
)

object SeatGeometryHelper {
    const val SENTINEL = 127.5
    const val TOLERANCE = 2.5

    data class AxisDef(val key: String, val labelEn: String, val labelTr: String, val group: String)

    val AXES = listOf(
        AxisDef("HORIZONTAL", "Fore/aft", "İleri/Geri", "seat"),
        AxisDef("BACKREST", "Backrest", "Sırtlık", "seat"),
        AxisDef("HEIGHT", "Height", "Yükseklik", "seat"),
        AxisDef("SITPOINT", "Cushion", "Minder", "seat"),
        AxisDef("LEGHOLDER", "Leg support", "Bacak Desteği", "seat"),
        AxisDef("HEADREST_H", "Headrest fore/aft", "Kafalık İleri/Geri", "seat"),
        AxisDef("HEADREST_V", "Headrest height", "Kafalık Yükseklik", "seat"),
        AxisDef("LEFT_H", "Left mirror horizontal", "Sol Ayna Yatay", "mirror"),
        AxisDef("LEFT_V", "Left mirror vertical", "Sol Ayna Dikey", "mirror"),
        AxisDef("RIGHT_H", "Right mirror horizontal", "Sağ Ayna Yatay", "mirror"),
        AxisDef("RIGHT_V", "Right mirror vertical", "Sağ Ayna Dikey", "mirror"),
        AxisDef("ST_H", "Wheel reach", "Direksiyon Derinlik", "mirror"),
        AxisDef("ST_V", "Wheel height", "Direksiyon Yükseklik", "mirror")
    )

    fun isValidValue(v: Double?): Boolean {
        if (v == null || v.isNaN()) return false
        return Math.abs(v - SENTINEL) > 0.05
    }

    /**
     * Determines whether the current live axes match any saved position.
     */
    fun findMatchingPosition(current: Map<String, Double>?, positions: List<SeatPosition>): SeatPosition? {
        if (current == null || current.isEmpty() || positions.isEmpty()) return null

        var best: SeatPosition? = null
        var bestDist = Double.MAX_VALUE

        for (p in positions) {
            if (p.axes.isEmpty()) continue
            var match = true
            var dist = 0.0

            for (axis in AXES) {
                val cv = current[axis.key]
                val pv = p.axes[axis.key]
                val cValid = isValidValue(cv)
                val pValid = isValidValue(pv)

                if (!cValid && !pValid) continue
                if (cValid != pValid) {
                    match = false
                    break
                }
                val d = Math.abs(cv!! - pv!!)
                if (d > TOLERANCE) {
                    match = false
                    break
                }
                dist += d
            }

            if (match && dist < bestDist) {
                bestDist = dist
                best = p
            }
        }
        return best
    }
}
