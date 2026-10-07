package com.overdrive.app.ui.parking

import org.json.JSONArray
import org.json.JSONObject

data class ParkingStatus(
    val running: Boolean = false,
    val enabled: Boolean = false,
    val current: ParkingSession? = null,
    val config: ParkingConfig = ParkingConfig(),
    val signageModels: Boolean = false
)

data class ParkingConfig(
    val enabled: Boolean = false,
    val endTrigger: String = "return", // "return", "power_on", "drive_away"
    val snapshots: Boolean = true,
    val neighbours: Boolean = true,
    val signage: Boolean = true,
    val retentionDays: Int = 30,
    val storageCapMb: Int = 300
)

data class GeocodingConfig(
    val enabled: Boolean = false,
    val allowOnline: Boolean = false,
    val inherited: Boolean = true
)

data class ParkingSession(
    val id: String,
    val start: Long,
    val end: Long? = null,
    val durationMs: Long = 0L,
    val place: String = "Bilinmeyen Konum",
    val signageLabel: String? = null,
    val signageConfidence: Double? = null,
    val signageState: String = "pending",
    val gpsQuality: String = "UNKNOWN",
    val sentryState: String = "unknown",
    val eventsCount: Int = 0,
    val neighboursCount: Int = 0,
    val socStart: Int? = null,
    val socEnd: Int? = null,
    val energyUsedKwh: Double? = null,
    val socDelta: Double? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val endTrigger: String? = null,
    val mosaicUrl: String? = null,
    val isLive: Boolean = false,
    val open: Boolean = false
) {
    companion object {
        fun fromJson(json: JSONObject, isCurrent: Boolean = false): ParkingSession {
            val id = json.optString("id", if (isCurrent) "current" else "")
            val start = json.optLong("start", json.optLong("startedMs", json.optLong("startTime", System.currentTimeMillis())))
            val end = if (json.has("end")) json.optLong("end") else if (json.has("endedMs")) json.optLong("endedMs") else null
            val duration = json.optLong("duration", if (end != null) (end - start) else (System.currentTimeMillis() - start))

            var place = json.optString("safeZone", "")
            if (place.isEmpty()) {
                val placeObj = json.optJSONObject("place")
                if (placeObj != null) {
                    place = placeObj.optString("short", placeObj.optString("displayName", ""))
                }
            }

            val gpsObj = json.optJSONObject("gps")
            val lat = if (gpsObj != null && gpsObj.has("lat")) gpsObj.optDouble("lat") else null
            val lng = if (gpsObj != null && gpsObj.has("lng")) gpsObj.optDouble("lng") else null

            if (place.isEmpty()) {
                if (lat != null && lng != null) {
                    place = String.format(java.util.Locale.US, "%.5f, %.5f", lat, lng)
                } else {
                    place = "Konum bilgisi yok"
                }
            }

            val signageObj = json.optJSONObject("signage")
            val signageLabel = if (signageObj != null && signageObj.optBoolean("found", false)) {
                val lbl = signageObj.optString("label", "")
                if (lbl.isNotEmpty()) lbl else null
            } else null
            val signageConfidence = signageObj?.optDouble("confidence")
            val signageState = json.optString("signageState", if (signageLabel != null) "done" else "pending")

            val gpsQuality = gpsObj?.optString("quality", "UNKNOWN") ?: json.optString("gpsQuality", "UNKNOWN")
            val sentryState = json.optString("sentryState", "unknown")

            val eventsArray = json.optJSONArray("events")
            val eventsCount = eventsArray?.length() ?: json.optInt("eventCount", 0)

            val neighboursArray = json.optJSONArray("neighbours")
            val neighboursCount = neighboursArray?.length() ?: json.optInt("neighbourCount", 0)

            val energyObj = json.optJSONObject("energy")
            val energyUsed = if (energyObj != null && energyObj.has("kwh")) energyObj.optDouble("kwh")
            else if (json.has("energyUsedKwh")) json.optDouble("energyUsedKwh") else null
            val socDelta = if (energyObj != null && energyObj.has("socDelta")) energyObj.optDouble("socDelta") else null

            val socStart = if (json.has("socStart")) json.optInt("socStart") else null
            val socEnd = if (json.has("socEnd")) json.optInt("socEnd") else null

            val endTrigger = json.optString("endTrigger", "").takeIf { it.isNotEmpty() }
            val open = json.optBoolean("open", isCurrent)

            val mosaicUrl = "/parking/asset/$id/arrived_mosaic.jpg"

            return ParkingSession(
                id = id,
                start = start,
                end = end,
                durationMs = duration,
                place = place,
                signageLabel = signageLabel,
                signageConfidence = signageConfidence,
                signageState = signageState,
                gpsQuality = gpsQuality,
                sentryState = sentryState,
                eventsCount = eventsCount,
                neighboursCount = neighboursCount,
                socStart = socStart,
                socEnd = socEnd,
                energyUsedKwh = energyUsed,
                socDelta = socDelta,
                lat = lat,
                lng = lng,
                endTrigger = endTrigger,
                mosaicUrl = mosaicUrl,
                isLive = isCurrent,
                open = open
            )
        }
    }
}

data class ParkingAssets(
    val arrivedMosaic: String? = null,
    val arrivedFront: String? = null,
    val arrivedRight: String? = null,
    val arrivedRear: String? = null,
    val arrivedLeft: String? = null,
    val returnedMosaic: String? = null,
    val returnedFront: String? = null,
    val returnedRight: String? = null,
    val returnedRear: String? = null,
    val returnedLeft: String? = null
) {
    companion object {
        fun fromJson(json: JSONObject?): ParkingAssets {
            if (json == null) return ParkingAssets()
            val arr = json.optJSONObject("arrived")
            val ret = json.optJSONObject("returned")
            return ParkingAssets(
                arrivedMosaic = arr?.optString("mosaic")?.takeIf { it.isNotEmpty() },
                arrivedFront = arr?.optString("front")?.takeIf { it.isNotEmpty() },
                arrivedRight = arr?.optString("right")?.takeIf { it.isNotEmpty() },
                arrivedRear = arr?.optString("rear")?.takeIf { it.isNotEmpty() },
                arrivedLeft = arr?.optString("left")?.takeIf { it.isNotEmpty() },
                returnedMosaic = ret?.optString("mosaic")?.takeIf { it.isNotEmpty() },
                returnedFront = ret?.optString("front")?.takeIf { it.isNotEmpty() },
                returnedRight = ret?.optString("right")?.takeIf { it.isNotEmpty() },
                returnedRear = ret?.optString("rear")?.takeIf { it.isNotEmpty() },
                returnedLeft = ret?.optString("left")?.takeIf { it.isNotEmpty() }
            )
        }
    }
}

data class ParkingNeighbourItem(
    val kind: String = "Araç",
    val classGroup: String = "vehicle",
    val side: Int = 1, // 0: front, 1: right, 2: rear, 3: left
    val status: String = "arrived",
    val arrivedMs: Long? = null,
    val departedMs: Long? = null,
    val lastSeenMs: Long? = null,
    val frames: List<String> = emptyList()
) {
    companion object {
        fun fromJson(json: JSONObject, sessionId: String): ParkingNeighbourItem {
            val classGroup = json.optString("classGroup", "vehicle")
            val kind = when (json.optString("kind", classGroup).uppercase()) {
                "CLOSE_PASS" -> "Yakın geçiş"
                "VEHICLE" -> "Araç"
                "BIKE" -> "Bisiklet"
                "PERSON" -> "Yaya"
                "ANIMAL" -> "Hayvan"
                else -> "Araç"
            }
            val side = json.optInt("side", 1)
            val status = json.optString("status", "arrived")
            val arrMs = if (json.has("arrivedMs")) json.optLong("arrivedMs") else null
            val depMs = if (json.has("departedMs")) json.optLong("departedMs") else null
            val lsMs = if (json.has("lastSeenMs")) json.optLong("lastSeenMs") else null

            val frameList = mutableListOf<String>()
            val framesArr = json.optJSONArray("frames")
            if (framesArr != null) {
                for (i in 0 until framesArr.length()) {
                    val fObj = framesArr.optJSONObject(i)
                    val fName = fObj?.optString("name")
                    if (!fName.isNullOrEmpty()) {
                        frameList.add("/parking/asset/$sessionId/$fName")
                    }
                }
            }

            return ParkingNeighbourItem(
                kind = kind,
                classGroup = classGroup,
                side = side,
                status = status,
                arrivedMs = arrMs,
                departedMs = depMs,
                lastSeenMs = lsMs,
                frames = frameList
            )
        }
    }
}

data class ParkingEventItem(
    val filename: String = "",
    val timestamp: Long = 0L,
    val peakSeverity: String? = null,
    val thumbnailUrl: String? = null,
    val personCount: Int = 0,
    val vehicleCount: Int = 0,
    val bikeCount: Int = 0,
    val animalCount: Int = 0,
    val cameras: String? = null
) {
    companion object {
        fun fromJson(json: JSONObject): ParkingEventItem {
            return ParkingEventItem(
                filename = json.optString("filename", ""),
                timestamp = json.optLong("timestamp", 0L),
                peakSeverity = json.optString("peakSeverity").takeIf { it.isNotEmpty() },
                thumbnailUrl = json.optString("heroThumbnailUrl", json.optString("thumbnailUrl")).takeIf { it.isNotEmpty() },
                personCount = json.optInt("personCount", 0),
                vehicleCount = json.optInt("vehicleCount", 0),
                bikeCount = json.optInt("bikeCount", 0),
                animalCount = json.optInt("animalCount", 0),
                cameras = json.optString("cameras").takeIf { it.isNotEmpty() }
            )
        }
    }
}

data class ParkingDetail(
    val session: ParkingSession,
    val neighbours: List<ParkingNeighbourItem> = emptyList(),
    val events: List<ParkingEventItem> = emptyList(),
    val assets: ParkingAssets = ParkingAssets()
)
