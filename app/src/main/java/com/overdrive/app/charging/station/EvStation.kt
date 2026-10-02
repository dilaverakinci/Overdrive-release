package com.overdrive.app.charging.station

/**
 * EV Charging Station Data Model (22,000+ Turkey EV Stations Database).
 * Compatible with Navion's ev_stations.db format and custom user chargers.
 */
data class EvStation(
    val id: String,
    val operator: String,
    val name: String,
    val city: String,
    val district: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val chargingType: String,
    val maxPowerKw: Double,
    val socketCount: Int,
    val acPrice: Double,
    val dcPrice: Double,
    val logoUrl: String = "",
    val connectorsJson: String = "",
    var distanceKm: Double = 0.0,
    val isCustom: Boolean = false
) {
    val displayTitle: String
        get() = when {
            isCustom -> "⭐ $name"
            operator.isNotBlank() && !name.contains(operator, ignoreCase = true) -> "$operator - $name"
            else -> name
        }

    val isDc: Boolean
        get() = chargingType.contains("DC", ignoreCase = true)

    val bestPricePerKwh: Double
        get() = when {
            isDc && dcPrice > 0.0 -> dcPrice
            !isDc && acPrice > 0.0 -> acPrice
            dcPrice > 0.0 -> dcPrice
            acPrice > 0.0 -> acPrice
            isDc -> 8.90
            else -> 2.60
        }
}
