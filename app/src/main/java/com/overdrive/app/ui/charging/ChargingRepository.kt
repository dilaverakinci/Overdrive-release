package com.overdrive.app.ui.charging

import android.util.Log
import com.overdrive.app.util.DaemonHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter

open class ChargingRepository {

    companion object {
        private const val TAG = "ChargingRepository"
    }

    open suspend fun getOverview(days: Int): Result<Pair<ChargingSummary, List<ChargingSession>>> = withContext(Dispatchers.IO) {
        try {
            val conn = DaemonHttpClient.open("/api/charging/overview?days=$days", "GET", 4000, 4000)
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(body)
                val summaryObj = json.optJSONObject("summary") ?: JSONObject()
                val sessionsArr = json.optJSONArray("sessions") ?: JSONArray()

                val summary = parseSummary(summaryObj)
                val sessions = parseSessions(sessionsArr)
                Result.success(Pair(summary, sessions))
            } else {
                conn.disconnect()
                Result.failure(Exception("HTTP error ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "getOverview error: ${e.message}")
            Result.failure(e)
        }
    }

    open suspend fun getBootstrap(days: Int): Result<ChargingBootstrapData> = withContext(Dispatchers.IO) {
        try {
            val conn = DaemonHttpClient.open("/api/charging/bootstrap?days=$days&hours=168&points=300", "GET", 5000, 5000)
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(body)
                val bootstrap = json.optJSONObject("bootstrap") ?: JSONObject()

                val summaryObj = bootstrap.optJSONObject("summary")?.optJSONObject("summary") ?: JSONObject()
                val sessionsArr = bootstrap.optJSONObject("sessions")?.optJSONArray("sessions") ?: JSONArray()
                val configObj = bootstrap.optJSONObject("config")?.optJSONObject("config") ?: JSONObject()
                val socObj = bootstrap.optJSONObject("soc")
                val socArr = socObj?.optJSONArray("soc")
                    ?: socObj?.optJSONArray("points")
                    ?: JSONArray()

                val summary = parseSummary(summaryObj)
                val sessions = parseSessions(sessionsArr)
                val config = parseConfig(configObj)
                val socHistory = parseSocPoints(socArr)
                val tariffsObj = bootstrap.optJSONObject("tariffs")
                val tariffsPayload = tariffsObj?.let { parseTariffsPayload(it) }

                Result.success(ChargingBootstrapData(summary, sessions, config, socHistory, tariffsPayload))
            } else {
                conn.disconnect()
                Result.failure(Exception("HTTP error ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "getBootstrap error: ${e.message}")
            Result.failure(e)
        }
    }

    open suspend fun getSessionDetail(id: Long): Result<Pair<ChargingSession, List<ChargingSample>>> = withContext(Dispatchers.IO) {
        try {
            val sessionConn = DaemonHttpClient.open("/api/charging/$id", "GET", 4000, 4000)
            if (sessionConn.responseCode != 200) {
                sessionConn.disconnect()
                return@withContext Result.failure(Exception("Failed to get session $id (HTTP ${sessionConn.responseCode})"))
            }
            val sessionBody = sessionConn.inputStream.bufferedReader().use { it.readText() }
            sessionConn.disconnect()
            val sessionJson = JSONObject(sessionBody)
            val sessionObj = sessionJson.optJSONObject("session")
                ?: return@withContext Result.failure(Exception("Missing session in response"))
            val session = parseSession(sessionObj)

            val samplesConn = DaemonHttpClient.open("/api/charging/$id/samples", "GET", 4000, 4000)
            val samples = if (samplesConn.responseCode == 200) {
                val samplesBody = samplesConn.inputStream.bufferedReader().use { it.readText() }
                samplesConn.disconnect()
                val samplesJson = JSONObject(samplesBody)
                val samplesArr = samplesJson.optJSONArray("samples") ?: JSONArray()
                parseSamples(samplesArr)
            } else {
                samplesConn.disconnect()
                emptyList()
            }

            Result.success(Pair(session, samples))
        } catch (e: Exception) {
            Log.w(TAG, "getSessionDetail error: ${e.message}")
            Result.failure(e)
        }
    }

    open suspend fun getSocHistory(hours: Int): Result<List<SocHistoryPoint>> = withContext(Dispatchers.IO) {
        try {
            val conn = DaemonHttpClient.open("/api/charging/soc?hours=$hours&points=300", "GET", 4000, 4000)
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(body)
                val pointsArr = json.optJSONArray("soc")
                    ?: json.optJSONArray("points")
                    ?: JSONArray()
                Result.success(parseSocPoints(pointsArr))
            } else {
                conn.disconnect()
                Result.failure(Exception("HTTP error ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "getSocHistory error: ${e.message}")
            Result.failure(e)
        }
    }

    open suspend fun updateCost(id: Long, cost: Double): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val conn = DaemonHttpClient.open("/api/charging/$id/cost", "POST", 4000, 4000)
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            val payload = JSONObject().apply { put("cost", cost) }
            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            val code = conn.responseCode
            conn.disconnect()
            Result.success(code in 200..299)
        } catch (e: Exception) {
            Log.w(TAG, "updateCost error: ${e.message}")
            Result.failure(e)
        }
    }

    open suspend fun deleteSession(id: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val conn = DaemonHttpClient.open("/api/charging/$id/delete", "POST", 4000, 4000)
            val code = conn.responseCode
            conn.disconnect()
            Result.success(code in 200..299)
        } catch (e: Exception) {
            Log.w(TAG, "deleteSession error: ${e.message}")
            Result.failure(e)
        }
    }

    open suspend fun clearHistory(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val conn = DaemonHttpClient.open("/api/charging/history/clear", "POST", 5000, 5000)
            val code = conn.responseCode
            conn.disconnect()
            Result.success(code in 200..299)
        } catch (e: Exception) {
            Log.w(TAG, "clearHistory error: ${e.message}")
            Result.failure(e)
        }
    }

    open suspend fun getConfig(): Result<ChargingConfigData> = withContext(Dispatchers.IO) {
        try {
            val conn = DaemonHttpClient.open("/api/charging/config", "GET", 3000, 3000)
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(body)
                val configObj = json.optJSONObject("config") ?: JSONObject()
                Result.success(parseConfig(configObj))
            } else {
                conn.disconnect()
                Result.failure(Exception("HTTP error ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "getConfig error: ${e.message}")
            Result.failure(e)
        }
    }

    open suspend fun saveConfig(config: ChargingConfigData): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val conn = DaemonHttpClient.open("/api/charging/config", "POST", 4000, 4000)
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            val payload = JSONObject().apply {
                put("enabled", config.enabled)
                put("electricityRate", config.electricityRate)
                put("currency", config.currency)
                put("dcRate", config.dcRate)
                put("fastSampleSec", config.fastSampleSec)
            }
            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            val code = conn.responseCode
            conn.disconnect()
            Result.success(code in 200..299)
        } catch (e: Exception) {
            Log.w(TAG, "saveConfig error: ${e.message}")
            Result.failure(e)
        }
    }

    open suspend fun getTariffs(): Result<TariffsPayload> = withContext(Dispatchers.IO) {
        try {
            val conn = DaemonHttpClient.open("/api/charging/tariffs", "GET", 4000, 4000)
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(body)
                Result.success(parseTariffsPayload(json))
            } else {
                conn.disconnect()
                Result.failure(Exception("HTTP error ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "getTariffs error: ${e.message}")
            Result.failure(e)
        }
    }

    open suspend fun saveTariff(payload: JSONObject): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val conn = DaemonHttpClient.open("/api/charging/tariffs", "POST", 4000, 4000)
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
            val code = conn.responseCode
            val body = if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }
            conn.disconnect()
            if (code in 200..299) {
                val json = JSONObject(body)
                if (json.optBoolean("success", false) || json.optBoolean("tariffSaved", false)) {
                    Result.success(true)
                } else {
                    val err = json.optString("error", "Failed to save tariff")
                    Result.failure(Exception(err))
                }
            } else {
                val err = try {
                    JSONObject(body).optString("error", "HTTP error $code")
                } catch (e: Exception) {
                    "HTTP error $code"
                }
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Log.w(TAG, "saveTariff error: ${e.message}")
            Result.failure(e)
        }
    }

    open suspend fun deleteTariff(id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val conn = DaemonHttpClient.open("/api/charging/tariffs/delete", "POST", 4000, 4000)
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            val payload = JSONObject().apply { put("id", id) }
            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
            val code = conn.responseCode
            conn.disconnect()
            Result.success(code in 200..299)
        } catch (e: Exception) {
            Log.w(TAG, "deleteTariff error: ${e.message}")
            Result.failure(e)
        }
    }

    open suspend fun setDefaultTariff(id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val conn = DaemonHttpClient.open("/api/charging/tariffs/default", "POST", 4000, 4000)
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            val payload = JSONObject().apply { put("id", id) }
            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
            val code = conn.responseCode
            conn.disconnect()
            Result.success(code in 200..299)
        } catch (e: Exception) {
            Log.w(TAG, "setDefaultTariff error: ${e.message}")
            Result.failure(e)
        }
    }

    // ==================== PARSERS ====================

    fun parseSessions(arr: JSONArray): List<ChargingSession> {
        val list = mutableListOf<ChargingSession>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            list.add(parseSession(obj))
        }
        return list
    }

    fun parseSession(obj: JSONObject): ChargingSession {
        return ChargingSession(
            id = obj.optLong("id", -1L),
            startTime = obj.optLong("startTime", 0L),
            endTime = obj.optLong("endTime", 0L),
            inProgress = obj.optBoolean("inProgress", false),
            chargingNow = obj.optBoolean("chargingNow", false),
            startSoc = if (obj.has("startSoc") && !obj.isNull("startSoc")) obj.optDouble("startSoc") else null,
            endSoc = if (obj.has("endSoc") && !obj.isNull("endSoc")) obj.optDouble("endSoc") else null,
            energyAdded = if (obj.has("energyAdded") && !obj.isNull("energyAdded")) obj.optDouble("energyAdded") else null,
            peakPower = if (obj.has("peakPower") && !obj.isNull("peakPower")) obj.optDouble("peakPower") else null,
            avgPower = if (obj.has("avgPower") && !obj.isNull("avgPower")) obj.optDouble("avgPower") else null,
            rangeGained = if (obj.has("rangeGained") && !obj.isNull("rangeGained")) obj.optInt("rangeGained") else null,
            isDc = if (obj.has("isDc") && !obj.isNull("isDc")) obj.optBoolean("isDc") else null,
            electricityRate = if (obj.has("electricityRate") && !obj.isNull("electricityRate")) obj.optDouble("electricityRate") else null,
            cost = if (obj.has("cost") && !obj.isNull("cost")) obj.optDouble("cost") else null,
            currency = obj.optString("currency", ""),
            timeToFullMin = if (obj.has("timeToFullMin") && !obj.isNull("timeToFullMin")) obj.optInt("timeToFullMin") else null,
            tempHigh = if (obj.has("tempHigh") && !obj.isNull("tempHigh")) obj.optDouble("tempHigh") else null,
            tempLow = if (obj.has("tempLow") && !obj.isNull("tempLow")) obj.optDouble("tempLow") else null,
            tempAvg = if (obj.has("tempAvg") && !obj.isNull("tempAvg")) obj.optDouble("tempAvg") else null,
            durationMinutes = if (obj.has("durationMinutes") && !obj.isNull("durationMinutes")) obj.optLong("durationMinutes") else null,
            lat = if (obj.has("lat") && !obj.isNull("lat")) obj.optDouble("lat") else null,
            lng = if (obj.has("lng") && !obj.isNull("lng")) obj.optDouble("lng") else null,
            placeLabel = if (obj.has("placeLabel") && !obj.isNull("placeLabel")) obj.optString("placeLabel") else null,
            startOdometerKm = if (obj.has("startOdometerKm") && !obj.isNull("startOdometerKm")) obj.optInt("startOdometerKm") else null,
            tariffLabel = if (obj.has("tariffLabel") && !obj.isNull("tariffLabel")) obj.optString("tariffLabel") else null,
            isEstimated = obj.optBoolean("isEstimated", false),
            livePowerKw = if (obj.has("livePowerKw") && !obj.isNull("livePowerKw")) obj.optDouble("livePowerKw") else null
        )
    }

    fun parseSummary(obj: JSONObject): ChargingSummary {
        val dailyArr = obj.optJSONArray("daily") ?: JSONArray()
        val dailyList = mutableListOf<ChargingDailyPoint>()
        for (i in 0 until dailyArr.length()) {
            val d = dailyArr.optJSONObject(i) ?: continue
            dailyList.add(
                ChargingDailyPoint(
                    dayEpoch = d.optLong("day", 0L),
                    sessions = d.optInt("sessions", 0),
                    energy = d.optDouble("energy", 0.0),
                    cost = d.optDouble("cost", 0.0),
                    incomplete = d.optInt("incomplete", 0),
                    estimated = d.optInt("estimated", 0)
                )
            )
        }

        val sohArr = obj.optJSONArray("sohTrend") ?: JSONArray()
        val sohList = mutableListOf<SohPoint>()
        for (i in 0 until sohArr.length()) {
            val s = sohArr.optJSONObject(i) ?: continue
            sohList.add(
                SohPoint(
                    dayEpoch = s.optLong("day", 0L),
                    sohPercent = s.optDouble("soh", 0.0)
                )
            )
        }

        val liveObj = obj.optJSONObject("live") ?: JSONObject()
        val liveState = parseLive(liveObj)

        return ChargingSummary(
            periodSessions = obj.optInt("periodSessions", 0),
            periodEnergyKwh = obj.optDouble("periodEnergyKwh", 0.0),
            periodCost = obj.optDouble("periodCost", 0.0),
            periodDcCount = obj.optInt("periodDcCount", 0),
            periodAcCount = obj.optInt("periodAcCount", 0),
            periodRangeGained = obj.optInt("periodRangeGained", 0),
            periodIncompleteSessions = obj.optInt("periodIncompleteSessions", 0),
            periodEstimatedSessions = obj.optInt("periodEstimatedSessions", 0),
            avgCostPerKwh = if (obj.has("avgCostPerKwh") && !obj.isNull("avgCostPerKwh")) obj.optDouble("avgCostPerKwh") else null,
            lifetimeSessions = obj.optInt("lifetimeSessions", 0),
            lifetimeEnergyKwh = obj.optDouble("lifetimeEnergyKwh", 0.0),
            lifetimeCost = obj.optDouble("lifetimeCost", 0.0),
            lifetimeIncompleteSessions = obj.optInt("lifetimeIncompleteSessions", 0),
            lifetimeEstimatedSessions = obj.optInt("lifetimeEstimatedSessions", 0),
            sohTrend = sohList,
            daily = dailyList,
            live = liveState
        )
    }

    fun parseLive(obj: JSONObject): ChargingLiveState {
        return ChargingLiveState(
            charging = obj.optBoolean("charging", false),
            plugged = obj.optBoolean("plugged", false),
            full = obj.optBoolean("full", false),
            fault = obj.optBoolean("fault", false),
            socPercent = obj.optDouble("socPercent", 0.0),
            sessionKwh = obj.optDouble("sessionKwh", 0.0),
            sessionEnergyIncomplete = obj.optBoolean("sessionEnergyIncomplete", false),
            sessionEnergyEstimated = obj.optBoolean("sessionEnergyEstimated", false),
            sessionEnergySource = obj.optString("sessionEnergySource", ""),
            timeToFullMin = obj.optInt("timeToFullMin", -1),
            powerKw = obj.optDouble("powerKw", 0.0),
            isEstimated = obj.optBoolean("isEstimated", false),
            rangeKm = obj.optDouble("rangeKm", -1.0),
            sohPercent = obj.optDouble("sohPercent", -1.0)
        )
    }

    fun parseSamples(arr: JSONArray): List<ChargingSample> {
        val list = mutableListOf<ChargingSample>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            list.add(
                ChargingSample(
                    t = obj.optLong("t", 0L),
                    powerKw = if (obj.has("power") && !obj.isNull("power")) obj.optDouble("power") else null,
                    soc = if (obj.has("soc") && !obj.isNull("soc")) obj.optDouble("soc") else null,
                    temp = if (obj.has("temp") && !obj.isNull("temp")) obj.optDouble("temp") else null,
                    tempHigh = if (obj.has("tempHigh") && !obj.isNull("tempHigh")) obj.optDouble("tempHigh") else null,
                    tempLow = if (obj.has("tempLow") && !obj.isNull("tempLow")) obj.optDouble("tempLow") else null
                )
            )
        }
        return list
    }

    fun parseConfig(obj: JSONObject): ChargingConfigData {
        return ChargingConfigData(
            enabled = obj.optBoolean("enabled", false),
            electricityRate = obj.optDouble("electricityRate", 0.0),
            currency = obj.optString("currency", ""),
            dcRate = obj.optDouble("dcRate", 0.0),
            fastSampleSec = obj.optInt("fastSampleSec", 5)
        )
    }

    fun parseSocPoints(arr: JSONArray): List<SocHistoryPoint> {
        val list = mutableListOf<SocHistoryPoint>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val t = obj.optLong("t", 0L)
            val soc = obj.optDouble("soc", 0.0)
            val isCharging = obj.optInt("charging", 0) > 0 || obj.optBoolean("charging", false)
            val range = if (obj.has("range") && !obj.isNull("range")) obj.optDouble("range") else null
            val soh = if (obj.has("soh") && !obj.isNull("soh")) obj.optDouble("soh") else null
            val powerKw = if (obj.has("power") && !obj.isNull("power")) obj.optDouble("power") else null
            list.add(SocHistoryPoint(t, soc, isCharging, range, soh, powerKw))
        }
        return list
    }

    fun parseTariffsPayload(json: JSONObject): TariffsPayload {
        val tariffsArr = json.optJSONArray("tariffs")
            ?: json.optJSONObject("meta")?.optJSONArray("tariffs")
            ?: JSONArray()
        val meta = json.optJSONObject("meta") ?: json
        val tariffs = mutableListOf<LocationTariff>()
        for (i in 0 until tariffsArr.length()) {
            val obj = tariffsArr.optJSONObject(i) ?: continue
            tariffs.add(parseTariff(obj))
        }
        val defaultId = if (meta.has("defaultTariffId") && !meta.isNull("defaultTariffId")) meta.optString("defaultTariffId") else null
        val matchedId = if (meta.has("matchedTariffId") && !meta.isNull("matchedTariffId")) meta.optString("matchedTariffId") else null
        val lat = if (meta.has("lat") && !meta.isNull("lat")) meta.optDouble("lat") else null
        val lng = if (meta.has("lng") && !meta.isNull("lng")) meta.optDouble("lng") else null
        val globalRate = if (meta.has("globalRate") && !meta.isNull("globalRate")) meta.optDouble("globalRate") else null
        val globalDcRate = if (meta.has("globalDcRate") && !meta.isNull("globalDcRate")) meta.optDouble("globalDcRate") else null
        val currency = if (meta.has("currency") && !meta.isNull("currency")) meta.optString("currency") else null

        return TariffsPayload(
            tariffs = tariffs,
            defaultTariffId = if (defaultId.isNullOrEmpty()) null else defaultId,
            matchedTariffId = if (matchedId.isNullOrEmpty()) null else matchedId,
            lat = if (lat != null && lat != 0.0) lat else null,
            lng = if (lng != null && lng != 0.0) lng else null,
            globalRate = globalRate,
            globalDcRate = globalDcRate,
            currency = currency
        )
    }

    fun parseTariff(obj: JSONObject): LocationTariff {
        return LocationTariff(
            id = obj.optString("id", ""),
            label = obj.optString("label", ""),
            lat = obj.optDouble("lat", 0.0),
            lng = obj.optDouble("lng", 0.0),
            radiusM = obj.optInt("radiusM", 50),
            acRate = obj.optDouble("acRate", 0.0),
            dcRate = obj.optDouble("dcRate", 0.0),
            currency = obj.optString("currency", ""),
            enabled = obj.optBoolean("enabled", true),
            createdAt = obj.optLong("createdAt", 0L),
            lastUsedAt = obj.optLong("lastUsedAt", 0L),
            useCount = obj.optInt("useCount", 0)
        )
    }

    open suspend fun getNearbyStations(
        lat: Double,
        lng: Double,
        radiusM: Double = 15000.0,
        limit: Int = 40
    ): Result<List<EvStationItem>> = withContext(Dispatchers.IO) {
        try {
            val conn = DaemonHttpClient.open(
                "/api/charging/stations/nearby?lat=$lat&lng=$lng&radius=$radiusM&limit=$limit",
                "GET",
                4000,
                4000
            )
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(body)
                val arr = json.optJSONArray("stations") ?: JSONArray()
                val stations = parseStationItems(arr)
                Result.success(stations)
            } else {
                conn.disconnect()
                Result.failure(Exception("HTTP error ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "getNearbyStations error: ${e.message}")
            Result.failure(e)
        }
    }

    private fun parseStationItems(arr: JSONArray): List<EvStationItem> {
        val list = mutableListOf<EvStationItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val connsArr = obj.optJSONArray("connectors") ?: JSONArray()
            val connectors = mutableListOf<EvConnectorItem>()
            for (j in 0 until connsArr.length()) {
                val c = connsArr.optJSONObject(j) ?: continue
                connectors.add(
                    EvConnectorItem(
                        type = c.optString("type", "Type 2"),
                        powerKw = c.optDouble("power_kw", 0.0),
                        current = c.optString("current", "AC"),
                        count = c.optInt("count", 1),
                        pricePerKwh = if (c.has("price_per_kwh")) c.optDouble("price_per_kwh") else null
                    )
                )
            }
            list.add(
                EvStationItem(
                    id = obj.optString("id", ""),
                    operator = obj.optString("operator", ""),
                    name = obj.optString("name", ""),
                    city = obj.optString("city", ""),
                    district = obj.optString("district", ""),
                    address = obj.optString("address", ""),
                    latitude = obj.optDouble("latitude", 0.0),
                    longitude = obj.optDouble("longitude", 0.0),
                    chargingType = obj.optString("chargingType", "AC"),
                    maxPowerKw = obj.optDouble("maxPowerKw", 0.0),
                    socketCount = obj.optInt("socketCount", 0),
                    acPrice = obj.optDouble("acPrice", 0.0),
                    dcPrice = obj.optDouble("dcPrice", 0.0),
                    logoUrl = obj.optString("logoUrl", ""),
                    distanceMeters = obj.optDouble("distanceMeters", 0.0),
                    connectors = connectors
                )
            )
        }
        return list
    }
}
