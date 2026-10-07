package com.overdrive.app.ui.parking

import org.json.JSONObject

data class ParkingStatus(
    val running: Boolean,
    val enabled: Boolean,
    val current: ParkingSession?,
    val config: ParkingConfig
)

data class ParkingConfig(
    val enabled: Boolean = false,
    val endTrigger: String = "return", // "return", "power_on", "drive_away"
    val snapshots: Boolean = true,
    val neighbours: Boolean = true,
    val signage: Boolean = true,
    val retentionDays: Int = 30
)

data class ParkingSession(
    val id: String,
    val start: Long,
    val end: Long? = null,
    val durationMs: Long = 0L,
    val place: String = "Bilinmeyen Konum",
    val signageLabel: String? = null,
    val gpsQuality: String = "UNKNOWN",
    val eventsCount: Int = 0,
    val neighboursCount: Int = 0,
    val socStart: Int? = null,
    val socEnd: Int? = null,
    val energyUsedKwh: Double? = null,
    val mosaicUrl: String? = null,
    val isLive: Boolean = false
) {
    companion object {
        fun fromJson(json: JSONObject, isCurrent: Boolean = false): ParkingSession {
            val id = json.optString("id", if (isCurrent) "current" else "")
            val start = json.optLong("start", json.optLong("startTime", System.currentTimeMillis()))
            val end = if (json.has("end")) json.optLong("end") else null
            val duration = json.optLong("duration", if (end != null) (end - start) else (System.currentTimeMillis() - start))

            var place = json.optString("safeZone", "")
            if (place.isEmpty()) {
                val placeObj = json.optJSONObject("place")
                if (placeObj != null) {
                    place = placeObj.optString("short", placeObj.optString("displayName", ""))
                }
            }
            if (place.isEmpty()) {
                val gpsObj = json.optJSONObject("gps")
                if (gpsObj != null && gpsObj.has("lat") && gpsObj.has("lng")) {
                    place = String.format(java.util.Locale.US, "%.5f, %.5f", gpsObj.optDouble("lat"), gpsObj.optDouble("lng"))
                } else {
                    place = "Konum bilgisi yok"
                }
            }

            val signageObj = json.optJSONObject("signage")
            val signageLabel = if (signageObj != null && signageObj.optBoolean("found", false)) {
                val lbl = signageObj.optString("label", "")
                if (lbl.isNotEmpty()) lbl else null
            } else null

            val gpsObj = json.optJSONObject("gps")
            val gpsQuality = gpsObj?.optString("quality", "UNKNOWN") ?: "UNKNOWN"

            val eventsArray = json.optJSONArray("events")
            val eventsCount = eventsArray?.length() ?: json.optInt("eventCount", 0)

            val neighboursArray = json.optJSONArray("neighbours")
            val neighboursCount = neighboursArray?.length() ?: json.optInt("neighbourCount", 0)

            val socStart = if (json.has("socStart")) json.optInt("socStart") else null
            val socEnd = if (json.has("socEnd")) json.optInt("socEnd") else null
            val energyUsed = if (json.has("energyUsedKwh")) json.optDouble("energyUsedKwh") else null

            val mosaicUrl = "/parking/asset/$id/arrived_mosaic.jpg"

            return ParkingSession(
                id = id,
                start = start,
                end = end,
                durationMs = duration,
                place = place,
                signageLabel = signageLabel,
                gpsQuality = gpsQuality,
                eventsCount = eventsCount,
                neighboursCount = neighboursCount,
                socStart = socStart,
                socEnd = socEnd,
                energyUsedKwh = energyUsed,
                mosaicUrl = mosaicUrl,
                isLive = isCurrent
            )
        }
    }
}
