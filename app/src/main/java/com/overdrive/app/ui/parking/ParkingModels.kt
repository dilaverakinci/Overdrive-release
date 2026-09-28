package com.overdrive.app.ui.parking

import org.json.JSONArray
import org.json.JSONObject

enum class ParkingTab {
    SESSIONS,
    SETTINGS
}

enum class ParkingFilterRange(val days: Int) {
    DAYS_7(7),
    DAYS_30(30),
    DAYS_90(90),
    ALL(0)
}

data class ParkingFrameModel(
    val name: String,
    val score: Double? = null,
    val ms: Long? = null,
    val side: String? = null
)

data class ParkingNeighbourModel(
    val id: Long = 0L,
    val sessionId: String = "",
    val key: String = "",
    val side: Int = 0,
    val sideName: String = "front",
    val kind: String = "NEIGHBOUR",
    val classGroup: String = "VEHICLE",
    val status: String = "",
    val confirmed: Boolean = true,
    val arrivedMs: Long? = null,
    val departedMs: Long? = null,
    val lastSeenMs: Long? = null,
    val arrivalEvent: String? = null,
    val departureEvent: String? = null,
    val frames: List<ParkingFrameModel> = emptyList()
)

data class ParkingEventModel(
    val id: String = "",
    val timestamp: Long = 0L,
    val filename: String = "",
    val thumbnailUrl: String? = null,
    val heroThumbnailUrl: String? = null,
    val peakSeverity: String? = null,
    val personCount: Int = 0,
    val vehicleCount: Int = 0,
    val bikeCount: Int = 0,
    val animalCount: Int = 0,
    val cameras: String? = null
)

data class ParkingConfigModel(
    val enabled: Boolean = false,
    val snapshots: Boolean = true,
    val neighbours: Boolean = true,
    val signage: Boolean = true,
    val retentionDays: Int = 90,
    val storageCapMb: Int = 300,
    val endTrigger: String = "return",
    val geocodingEnabled: Boolean = false,
    val geocodingOnline: Boolean = false
)

data class ParkingSessionModel(
    val sessionId: String,
    val startedMs: Long,
    val endedMs: Long? = null,
    val isOpen: Boolean = false,
    val place: String = "",
    val level: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val gpsQuality: String = "UNKNOWN",
    val sentryState: String = "unknown",
    val eventCount: Int = 0,
    val neighbourCount: Int = 0,
    val signageState: String = "pending",
    val signageLabel: String? = null,
    val signageConfidence: Double? = null,
    val signageEvidence: List<String> = emptyList(),
    val endTrigger: String? = null,
    val kwh: Double? = null,
    val socDelta: Double? = null,
    val isCharged: Boolean = false,
    val energySource: String? = null,
    val hasMeasurableEnergy: Boolean = false,
    val arrivedOk: Boolean = false,
    val returnedOk: Boolean = false,
    val arrivedMs: Long? = null,
    val returnedMs: Long? = null
) {
    val durationMs: Long
        get() {
            val end = endedMs ?: System.currentTimeMillis()
            return (end - startedMs).coerceAtLeast(0L)
        }

    val mapsUrl: String?
        get() = if (lat != null && lng != null) "https://maps.google.com/?q=${lat},${lng}" else null

    companion object {
        fun fromJson(j: JSONObject): ParkingSessionModel {
            val sId = j.optString("sessionId", "")
            val start = j.optLong("startedMs", 0L)
            val end = if (j.has("endedMs") && j.optLong("endedMs") > 0) j.optLong("endedMs") else null
            val open = j.optBoolean("open", end == null)

            var place = ""
            var level: String? = null
            var lat: Double? = null
            var lng: Double? = null
            var gpsQ = "UNKNOWN"

            if (j.has("gps")) {
                val g = j.optJSONObject("gps")
                if (g != null) {
                    if (g.has("lat")) lat = g.optDouble("lat")
                    if (g.has("lng")) lng = g.optDouble("lng")
                    gpsQ = g.optString("quality", "UNKNOWN")
                }
            }

            if (j.has("safeZone") && j.optString("safeZone").isNotBlank()) {
                place = j.optString("safeZone")
            } else if (j.has("place")) {
                val p = j.optJSONObject("place")
                if (p != null) {
                    place = p.optString("short", p.optString("displayName", ""))
                }
            }
            if (place.isBlank() && lat != null && lng != null) {
                place = String.format(java.util.Locale.US, "%.5f, %.5f", lat, lng)
            }

            var signLabel: String? = null
            var signConf: Double? = null
            val signEv = mutableListOf<String>()
            val signState = j.optString("signageState", "pending")
            if (j.has("signage")) {
                val sign = j.optJSONObject("signage")
                if (sign != null) {
                    if (sign.optBoolean("found", false)) {
                        signLabel = sign.optString("label")
                        level = signLabel
                        if (sign.has("confidence")) signConf = sign.optDouble("confidence")
                        val evArr = sign.optJSONArray("evidence")
                        if (evArr != null) {
                            for (i in 0 until evArr.length()) {
                                val item = evArr.optJSONObject(i)
                                if (item != null && item.has("text")) {
                                    signEv.add(item.optString("text"))
                                }
                            }
                        }
                    }
                }
            }

            var kwh: Double? = null
            var socDelta: Double? = null
            var isCharged = false
            var energySrc: String? = null
            var measurable = false

            if (j.has("energy")) {
                val e = j.optJSONObject("energy")
                if (e != null) {
                    if (e.has("kwh")) kwh = e.optDouble("kwh")
                    if (e.has("socDelta")) socDelta = e.optDouble("socDelta")
                    isCharged = e.optBoolean("charged", false)
                    energySrc = e.optString("source", null)
                    measurable = e.optBoolean("measurable", false)
                }
            }

            val snaps = j.optJSONObject("snapshots")
            val arrOk = snaps?.optBoolean("arrivedOk", false) ?: false
            val retOk = snaps?.optBoolean("returnedOk", false) ?: false
            val arrMs = if (snaps != null && snaps.has("arrivedMs")) snaps.optLong("arrivedMs") else null
            val retMs = if (snaps != null && snaps.has("returnedMs")) snaps.optLong("returnedMs") else null

            return ParkingSessionModel(
                sessionId = sId,
                startedMs = start,
                endedMs = end,
                isOpen = open,
                place = place,
                level = level,
                lat = lat,
                lng = lng,
                gpsQuality = gpsQ,
                sentryState = j.optString("sentryState", "unknown"),
                eventCount = j.optInt("eventCount", 0),
                neighbourCount = j.optInt("neighbourCount", 0),
                signageState = signState,
                signageLabel = signLabel,
                signageConfidence = signConf,
                signageEvidence = signEv,
                endTrigger = if (j.has("endTrigger")) j.optString("endTrigger") else null,
                kwh = kwh,
                socDelta = socDelta,
                isCharged = isCharged,
                energySource = energySrc,
                hasMeasurableEnergy = measurable,
                arrivedOk = arrOk,
                returnedOk = retOk,
                arrivedMs = arrMs,
                returnedMs = retMs
            )
        }
    }
}

data class ParkingDetailData(
    val session: ParkingSessionModel,
    val neighbours: List<ParkingNeighbourModel> = emptyList(),
    val events: List<ParkingEventModel> = emptyList(),
    val assets: Map<String, Map<String, String>> = emptyMap()
)
