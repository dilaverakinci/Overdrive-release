package com.overdrive.app.ui.trips

import org.json.JSONArray
import org.json.JSONObject

enum class PeriodFilter(val days: Int, val label: String) {
    DAYS_7(7, "7d"),
    DAYS_14(14, "14d"),
    DAYS_30(30, "30d"),
    ALL(0, "all")
}

enum class TripsTab {
    TRIPS,
    STATS,
    STORAGE
}

data class TripRecordItem(
    val id: Long,
    val startTime: Long,
    val endTime: Long,
    val distanceKm: Double,
    val odometerStartKm: Double,
    val odometerEndKm: Double,
    val durationSeconds: Int,
    val avgSpeedKmh: Double,
    val maxSpeedKmh: Int,
    val socStart: Double,
    val socEnd: Double,
    val kwhStart: Double,
    val kwhEnd: Double,
    val elecConStart: Double,
    val elecConEnd: Double,
    val energyUsedKwh: Double,
    val signedEnergyKwh: Double,
    val energyMetered: Boolean,
    val efficiencySocPerKm: Double,
    val energyPerKm: Double,
    val electricityRate: Double,
    val currency: String,
    val rateSource: String,
    val rateLabel: String,
    val tripCost: Double,
    val kinematicState: String,
    val gradientProfile: String,
    val elevationGainM: Double,
    val elevationLossM: Double,
    val avgGradientPercent: Double,
    val startLat: Double,
    val startLon: Double,
    val endLat: Double,
    val endLon: Double,
    val extTempC: Int,
    val anticipationScore: Int,
    val smoothnessScore: Int,
    val speedDisciplineScore: Int,
    val efficiencyScore: Int,
    val consistencyScore: Int,
    val overallScore: Int,
    val isPhev: Boolean,
    val fuelPctStart: Double,
    val fuelPctEnd: Double,
    val fuelConStart: Double,
    val fuelConEnd: Double,
    val litresUsed: Double,
    val fuelPricePerL: Double,
    val fuelCost: Double,
    val electricCost: Double,
    val iceSeconds: Int,
    val microMomentsJson: String? = null,
    val telemetryFilePath: String? = null
) {
    val consumptionKwhPer100Km: Double
        get() = if (distanceKm > 0.1) (energyUsedKwh / distanceKm) * 100.0 else 0.0

    val efficiencyKmPerKwh: Double
        get() = if (energyUsedKwh > 0.05) distanceKm / energyUsedKwh else 0.0

    val socDelta: Double
        get() = socStart - socEnd

    val isElevationRecorded: Boolean
        get() = elevationGainM > 0 || elevationLossM > 0

    companion object {
        fun fromJson(json: JSONObject): TripRecordItem {
            return TripRecordItem(
                id = json.optLong("id", 0L),
                startTime = json.optLong("startTime", 0L),
                endTime = json.optLong("endTime", 0L),
                distanceKm = json.optDouble("distanceKm", 0.0),
                odometerStartKm = json.optDouble("odometerStartKm", 0.0),
                odometerEndKm = json.optDouble("odometerEndKm", 0.0),
                durationSeconds = json.optInt("durationSeconds", 0),
                avgSpeedKmh = json.optDouble("avgSpeedKmh", 0.0),
                maxSpeedKmh = json.optInt("maxSpeedKmh", 0),
                socStart = json.optDouble("socStart", 0.0),
                socEnd = json.optDouble("socEnd", 0.0),
                kwhStart = json.optDouble("kwhStart", 0.0),
                kwhEnd = json.optDouble("kwhEnd", 0.0),
                elecConStart = json.optDouble("elecConStart", -1.0),
                elecConEnd = json.optDouble("elecConEnd", -1.0),
                energyUsedKwh = json.optDouble("energyUsedKwh", 0.0),
                signedEnergyKwh = json.optDouble("signedEnergyKwh", json.optDouble("energyUsedKwh", 0.0)),
                energyMetered = json.optBoolean("energyMetered", false),
                efficiencySocPerKm = json.optDouble("efficiencySocPerKm", 0.0),
                energyPerKm = json.optDouble("energyPerKm", 0.0),
                electricityRate = json.optDouble("electricityRate", 0.0),
                currency = json.optString("currency", "₺"),
                rateSource = json.optString("rateSource", ""),
                rateLabel = json.optString("rateLabel", ""),
                tripCost = json.optDouble("tripCost", 0.0),
                kinematicState = json.optString("kinematicState", ""),
                gradientProfile = json.optString("gradientProfile", ""),
                elevationGainM = json.optDouble("elevationGainM", 0.0),
                elevationLossM = json.optDouble("elevationLossM", 0.0),
                avgGradientPercent = json.optDouble("avgGradientPercent", 0.0),
                startLat = json.optDouble("startLat", 0.0),
                startLon = json.optDouble("startLon", 0.0),
                endLat = json.optDouble("endLat", 0.0),
                endLon = json.optDouble("endLon", 0.0),
                extTempC = json.optInt("extTempC", 0),
                anticipationScore = json.optInt("anticipationScore", 0),
                smoothnessScore = json.optInt("smoothnessScore", 0),
                speedDisciplineScore = json.optInt("speedDisciplineScore", 0),
                efficiencyScore = json.optInt("efficiencyScore", 0),
                consistencyScore = json.optInt("consistencyScore", 0),
                overallScore = json.optInt("overallScore", 0),
                isPhev = json.optBoolean("isPhev", false),
                fuelPctStart = json.optDouble("fuelPctStart", -1.0),
                fuelPctEnd = json.optDouble("fuelPctEnd", -1.0),
                fuelConStart = json.optDouble("fuelConStart", -1.0),
                fuelConEnd = json.optDouble("fuelConEnd", -1.0),
                litresUsed = json.optDouble("litresUsed", 0.0),
                fuelPricePerL = json.optDouble("fuelPricePerL", 0.0),
                fuelCost = json.optDouble("fuelCost", 0.0),
                electricCost = json.optDouble("electricCost", 0.0),
                iceSeconds = json.optInt("iceSeconds", 0),
                microMomentsJson = if (json.has("microMomentsJson") && !json.isNull("microMomentsJson")) json.optString("microMomentsJson") else null,
                telemetryFilePath = if (json.has("telemetryFilePath") && !json.isNull("telemetryFilePath")) json.optString("telemetryFilePath") else null
            )
        }
    }
}

data class TelemetrySampleItem(
    val timestampMs: Long,
    val speedKmh: Int,
    val accelPedalPercent: Int,
    val brakePedalPercent: Int,
    val brakePedalPressed: Boolean,
    val gearMode: Int,
    val lat: Double,
    val lon: Double,
    val altitude: Double,
    val verticalAccuracyM: Double,
    val altitudeIsMsl: Boolean
) {
    companion object {
        fun fromJson(json: JSONObject): TelemetrySampleItem {
            return TelemetrySampleItem(
                timestampMs = json.optLong("t", 0L),
                speedKmh = json.optInt("s", 0),
                accelPedalPercent = json.optInt("a", 0),
                brakePedalPercent = json.optInt("b", 0),
                brakePedalPressed = json.optBoolean("bp", false),
                gearMode = json.optInt("g", 1),
                lat = json.optDouble("la", 0.0),
                lon = json.optDouble("lo", 0.0),
                altitude = json.optDouble("al", 0.0),
                verticalAccuracyM = json.optDouble("va", 0.0),
                altitudeIsMsl = json.optBoolean("am", false)
            )
        }
    }
}

data class WeeklyRollupItem(
    val year: Int,
    val weekNumber: Int,
    val tripCount: Int,
    val totalDistanceKm: Double,
    val totalDurationSeconds: Int,
    val avgEfficiency: Double,
    val totalEnergyKwh: Double,
    val totalCost: Double,
    val avgEnergyPerKm: Double,
    val avgAnticipation: Int,
    val avgSmoothness: Int,
    val avgSpeedDiscipline: Int,
    val avgEfficiencyScore: Int,
    val avgConsistency: Int
) {
    companion object {
        fun fromJson(json: JSONObject): WeeklyRollupItem {
            return WeeklyRollupItem(
                year = json.optInt("year", 0),
                weekNumber = json.optInt("weekNumber", 0),
                tripCount = json.optInt("tripCount", 0),
                totalDistanceKm = json.optDouble("totalDistanceKm", 0.0),
                totalDurationSeconds = json.optInt("totalDurationSeconds", 0),
                avgEfficiency = json.optDouble("avgEfficiency", 0.0),
                totalEnergyKwh = json.optDouble("totalEnergyKwh", 0.0),
                totalCost = json.optDouble("totalCost", 0.0),
                avgEnergyPerKm = json.optDouble("avgEnergyPerKm", 0.0),
                avgAnticipation = json.optInt("avgAnticipation", 0),
                avgSmoothness = json.optInt("avgSmoothness", 0),
                avgSpeedDiscipline = json.optInt("avgSpeedDiscipline", 0),
                avgEfficiencyScore = json.optInt("avgEfficiencyScore", 0),
                avgConsistency = json.optInt("avgConsistency", 0)
            )
        }
    }
}

data class DnaScoresItem(
    val anticipation: Int,
    val smoothness: Int,
    val speedDiscipline: Int,
    val efficiency: Int,
    val consistency: Int,
    val overall: Int
) {
    companion object {
        fun fromJson(json: JSONObject): DnaScoresItem {
            val a = json.optInt("anticipation", 0)
            val s = json.optInt("smoothness", 0)
            val d = json.optInt("speedDiscipline", 0)
            val e = json.optInt("efficiency", 0)
            val c = json.optInt("consistency", 0)
            val o = json.optInt("overall", (a + s + d + e + c) / 5)
            return DnaScoresItem(a, s, d, e, c, o)
        }
    }
}

data class RangeEstimateItem(
    val predictedRangeKm: Double,
    val lowerBoundKm: Double,
    val upperBoundKm: Double,
    val bucketKey: String,
    val sampleCount: Int,
    val builtInRangeKm: Int,
    val halFuelRangeKm: Double? = null,
    val fuelPercent: Double? = null,
    val totalRangeKm: Double? = null
) {
    companion object {
        fun fromJson(json: JSONObject): RangeEstimateItem {
            return RangeEstimateItem(
                predictedRangeKm = json.optDouble("predictedRangeKm", 0.0),
                lowerBoundKm = json.optDouble("lowerBoundKm", 0.0),
                upperBoundKm = json.optDouble("upperBoundKm", 0.0),
                bucketKey = json.optString("bucketKey", ""),
                sampleCount = json.optInt("sampleCount", 0),
                builtInRangeKm = json.optInt("builtInRangeKm", 0),
                halFuelRangeKm = if (json.has("halFuelRangeKm")) json.optDouble("halFuelRangeKm") else null,
                fuelPercent = if (json.has("fuelPercent")) json.optDouble("fuelPercent") else null,
                totalRangeKm = if (json.has("totalRangeKm")) json.optDouble("totalRangeKm") else null
            )
        }
    }
}

data class TripStorageItem(
    val storageType: String,
    val storageTypeActive: String,
    val limitMb: Long,
    val maxLimitMb: Long,
    val maxLimitMbInternal: Long,
    val maxLimitMbSdCard: Long,
    val maxLimitMbUsb: Long,
    val effectiveLimitMb: Long,
    val usedMb: Double,
    val usedUnit: String,
    val sdCardAvailable: Boolean,
    val usbAvailable: Boolean,
    val sdCardTotalSpace: Long,
    val sdCardFreeSpace: Long,
    val usbTotalSpace: Long,
    val usbFreeSpace: Long,
    val tripsCount: Int,
    val storagePath: String
) {
    companion object {
        fun fromJson(json: JSONObject): TripStorageItem {
            return TripStorageItem(
                storageType = json.optString("storageType", "INTERNAL"),
                storageTypeActive = json.optString("storageTypeActive", "INTERNAL"),
                limitMb = json.optLong("limitMb", 1000L),
                maxLimitMb = json.optLong("maxLimitMb", 5000L),
                maxLimitMbInternal = json.optLong("maxLimitMbInternal", 5000L),
                maxLimitMbSdCard = json.optLong("maxLimitMbSdCard", 0L),
                maxLimitMbUsb = json.optLong("maxLimitMbUsb", 0L),
                effectiveLimitMb = json.optLong("effectiveLimitMb", 1000L),
                usedMb = json.optDouble("usedMb", 0.0),
                usedUnit = json.optString("usedUnit", "MB"),
                sdCardAvailable = json.optBoolean("sdCardAvailable", false),
                usbAvailable = json.optBoolean("usbAvailable", false),
                sdCardTotalSpace = json.optLong("sdCardTotalSpace", 0L),
                sdCardFreeSpace = json.optLong("sdCardFreeSpace", 0L),
                usbTotalSpace = json.optLong("usbTotalSpace", 0L),
                usbFreeSpace = json.optLong("usbFreeSpace", 0L),
                tripsCount = json.optInt("tripsCount", 0),
                storagePath = json.optString("storagePath", "")
            )
        }
    }
}

data class TripConfigItem(
    val enabled: Boolean,
    val electricityRate: Double,
    val currency: String,
    val tankCapacityL: Double,
    val fuelPricePerL: Double,
    val fuelUnit: String,
    val distanceUnit: String,
    val isPhev: Boolean,
    val nominalKwh: Double,
    val lastChargeRate: Double? = null,
    val lastChargeCurrency: String? = null,
    val lastChargeTariffLabel: String? = null,
    val lastChargeAt: Long? = null
) {
    companion object {
        fun fromJson(json: JSONObject): TripConfigItem {
            return TripConfigItem(
                enabled = json.optBoolean("enabled", true),
                electricityRate = json.optDouble("electricityRate", 0.0),
                currency = json.optString("currency", "₺"),
                tankCapacityL = json.optDouble("tankCapacityL", 0.0),
                fuelPricePerL = json.optDouble("fuelPricePerL", 0.0),
                fuelUnit = json.optString("fuelUnit", "L"),
                distanceUnit = json.optString("distanceUnit", "km"),
                isPhev = json.optBoolean("isPhev", false),
                nominalKwh = json.optDouble("nominalKwh", 82.5),
                lastChargeRate = if (json.has("lastChargeRate")) json.optDouble("lastChargeRate") else null,
                lastChargeCurrency = if (json.has("lastChargeCurrency")) json.optString("lastChargeCurrency") else null,
                lastChargeTariffLabel = if (json.has("lastChargeTariffLabel")) json.optString("lastChargeTariffLabel") else null,
                lastChargeAt = if (json.has("lastChargeAt")) json.optLong("lastChargeAt") else null
            )
        }
    }
}

data class TripsSummaryPeriod(
    val tripCount: Int = 0,
    val totalDistanceKm: Double = 0.0,
    val totalDurationSeconds: Int = 0,
    val totalEnergyKwh: Double = 0.0,
    val totalCost: Double = 0.0
)

data class TripsUiState(
    val isLoading: Boolean = false,
    val activeTab: TripsTab = TripsTab.TRIPS,
    val periodFilter: PeriodFilter = PeriodFilter.DAYS_7,
    val trips: List<TripRecordItem> = emptyList(),
    val summary: TripsSummaryPeriod = TripsSummaryPeriod(),
    val activeTripDetail: TripRecordItem? = null,
    val telemetrySamples: List<TelemetrySampleItem> = emptyList(),
    val dnaScores: DnaScoresItem? = null,
    val rangeEstimate: RangeEstimateItem? = null,
    val weeklyRollups: List<WeeklyRollupItem> = emptyList(),
    val storage: TripStorageItem? = null,
    val config: TripConfigItem? = null,
    val isRecovering: Boolean = false,
    val recoveryMessage: String? = null,
    val error: String? = null,
    val infoMessage: String? = null
)
