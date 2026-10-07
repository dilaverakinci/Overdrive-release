package com.overdrive.app.ui.vehicle

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

open class VehicleRepository {

    companion object {
        private const val TAG = "VehicleRepository"
        private const val BASE_URL = "http://127.0.0.1:8080/api/vehicle"
        private const val MODELS_URL = "http://127.0.0.1:8080/api/models/selected"
    }

    open suspend fun getVehicleState(): Result<VehicleState> = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch /api/vehicle/state
            val stateUrl = URL("$BASE_URL/state")
            val stateConn = stateUrl.openConnection() as HttpURLConnection
            stateConn.connectTimeout = 2500
            stateConn.readTimeout = 2500

            var stateJson: JSONObject? = null
            if (stateConn.responseCode == 200) {
                val body = stateConn.inputStream.bufferedReader().use { it.readText() }
                stateJson = JSONObject(body)
            }
            stateConn.disconnect()

            // 2. Fetch /api/vehicle/cloud-status
            var cloudConnected = false
            try {
                val cloudUrl = URL("$BASE_URL/cloud-status")
                val cloudConn = cloudUrl.openConnection() as HttpURLConnection
                cloudConn.connectTimeout = 2000
                cloudConn.readTimeout = 2000
                if (cloudConn.responseCode == 200) {
                    val body = cloudConn.inputStream.bufferedReader().use { it.readText() }
                    val cloudJson = JSONObject(body)
                    cloudConnected = cloudJson.optBoolean("verified", false) && cloudJson.optBoolean("enabled", false)
                }
                cloudConn.disconnect()
            } catch (_: Exception) {}

            // 3. Fetch modelId
            var modelId = "seal"
            try {
                val mUrl = URL(MODELS_URL)
                val mConn = mUrl.openConnection() as HttpURLConnection
                mConn.connectTimeout = 2000
                mConn.readTimeout = 2000
                if (mConn.responseCode == 200) {
                    val body = mConn.inputStream.bufferedReader().use { it.readText() }
                    val mJson = JSONObject(body)
                    modelId = mJson.optString("modelId", "seal")
                }
                mConn.disconnect()
            } catch (_: Exception) {}

            if (stateJson == null || !stateJson.optBoolean("success", false)) {
                return@withContext Result.success(
                    VehicleState(
                        isDataAvailable = false,
                        modelId = modelId,
                        cloudConnected = cloudConnected
                    )
                )
            }

            // Parse Doors
            val doorsObj = stateJson.optJSONObject("doors")
            val overallLock = doorsObj?.optInt("overall", -1) ?: -1
            val doorLockMap = mutableMapOf<String, Int>()
            if (doorsObj != null) {
                doorLockMap["lf"] = doorsObj.optInt("lf", -1)
                doorLockMap["rf"] = doorsObj.optInt("rf", -1)
                doorLockMap["lr"] = doorsObj.optInt("lr", -1)
                doorLockMap["rr"] = doorsObj.optInt("rr", -1)
            }

            // Parse DoorOpen
            val doorOpenObj = stateJson.optJSONObject("doorOpen")
            val doorOpenMap = mutableMapOf<String, Boolean>()
            if (doorOpenObj != null) {
                val keys = listOf("lf", "rf", "lr", "rr", "hood", "trunk")
                for (k in keys) {
                    if (doorOpenObj.has(k)) {
                        doorOpenMap[k] = doorOpenObj.optBoolean(k, false)
                    }
                }
            }

            // Parse Windows
            val windowsObj = stateJson.optJSONObject("windows")
            val windowPercentMap = mutableMapOf<String, Int>()
            if (windowsObj != null) {
                for (k in listOf("lf", "rf", "lr", "rr", "sunroof", "sunshade")) {
                    if (windowsObj.has(k)) {
                        windowPercentMap[k] = windowsObj.optInt(k, -1)
                    }
                }
            }

            val windowOpenObj = stateJson.optJSONObject("windowOpen")
            val windowOpenMap = mutableMapOf<String, Boolean>()
            if (windowOpenObj != null) {
                for (k in listOf("lf", "rf", "lr", "rr")) {
                    if (windowOpenObj.has(k)) {
                        windowOpenMap[k] = windowOpenObj.optBoolean(k, false)
                    }
                }
            }

            // Parse Trunk
            val trunkObj = stateJson.optJSONObject("trunk")
            val trunkOpen = trunkObj?.optBoolean("open", false) ?: (doorOpenMap["trunk"] ?: false)
            val trunkLocked = trunkObj?.optInt("lockStatus", 1) == 1

            // Parse Battery
            val batteryObj = stateJson.optJSONObject("battery")
            val soc = if (batteryObj != null && batteryObj.has("soc")) batteryObj.optDouble("soc") else null
            val range = if (batteryObj != null && batteryObj.has("rangeKm")) batteryObj.optInt("rangeKm") else null

            // Parse Tyres
            val tyresObj = stateJson.optJSONObject("tyres")
            val tyreMap = mutableMapOf<String, TyreData>()
            for (corner in listOf("fl", "fr", "rl", "rr")) {
                val tObj = tyresObj?.optJSONObject(corner)
                if (tObj != null && tObj.optBoolean("available", false)) {
                    val psi = if (tObj.has("psi")) tObj.optDouble("psi") else null
                    val kPa = if (tObj.has("kPa")) tObj.optInt("kPa") else null
                    val temp = if (tObj.has("temperatureC")) tObj.optInt("temperatureC") else null
                    tyreMap[corner] = TyreData(psi = psi, kPa = kPa, tempC = temp, available = true)
                } else {
                    tyreMap[corner] = TyreData(available = false)
                }
            }

            // Parse Climate
            val climateObj = stateJson.optJSONObject("climate")
            val acOn = climateObj?.optBoolean("acOn", false) ?: false
            val insideTemp = if (climateObj != null && climateObj.has("insideTempC")) climateObj.optDouble("insideTempC") else null
            val targetTemp = if (climateObj != null && climateObj.has("targetTempC")) climateObj.optDouble("targetTempC") else 22.0
            val fanSpeed = climateObj?.optInt("windMode", 3) ?: 3
            val batteryHeat = stateJson.optBoolean("batteryHeat", false)

            // Parse Seats
            val seatsObj = stateJson.optJSONObject("seats")
            val heatArr = seatsObj?.optJSONArray("heat")
            val coolArr = seatsObj?.optJSONArray("cool")
            val driverHeat = heatArr?.optInt(0, 0) ?: 0
            val passengerHeat = heatArr?.optInt(1, 0) ?: 0
            val driverCool = coolArr?.optInt(0, 0) ?: 0
            val passengerCool = coolArr?.optInt(1, 0) ?: 0
            val steeringHeat = seatsObj?.optBoolean("steeringHeat", false) ?: false

            // Parse Lights
            val lightsObj = stateJson.optJSONObject("lights")
            val daytime = lightsObj?.optBoolean("dayTimeLight", true) ?: true
            val ambientOn = lightsObj?.optBoolean("ambientEnabled", false) ?: false
            val ambientColor = lightsObj?.optInt("ambientColour", 0) ?: 0

            // Parse ADAS
            val adasObj = stateJson.optJSONObject("adas")
            val speedLimit = adasObj?.optBoolean("speedLimitWarning", false) ?: false
            val settingObj = stateJson.optJSONObject("setting")
            val childPres = settingObj?.optBoolean("childPresenceDetection", false) ?: false

            val parsedState = VehicleState(
                isDataAvailable = true,
                modelId = modelId,
                overallLockState = overallLock,
                cloudConnected = cloudConnected,
                doorLockStates = doorLockMap,
                doorOpenStates = doorOpenMap,
                windowPercent = windowPercentMap,
                windowOpenStates = windowOpenMap,
                trunkOpen = trunkOpen,
                trunkLocked = trunkLocked,
                socPercent = soc,
                rangeKm = range,
                tyres = tyreMap,
                climate = ClimateData(
                    acOn = acOn,
                    insideTempC = insideTemp,
                    targetTempC = targetTemp,
                    fanSpeed = fanSpeed,
                    batteryHeat = batteryHeat
                ),
                seats = SeatsData(
                    driverHeat = driverHeat,
                    driverCool = driverCool,
                    passengerHeat = passengerHeat,
                    passengerCool = passengerCool,
                    steeringHeat = steeringHeat
                ),
                lights = LightsData(
                    daytimeLight = daytime,
                    ambientEnabled = ambientOn,
                    ambientColour = ambientColor
                ),
                adas = AdasData(
                    speedLimitWarning = speedLimit,
                    childPresenceDetection = childPres
                )
            )

            Result.success(parsedState)
        } catch (e: Exception) {
            Log.w(TAG, "getVehicleState failed: ${e.message}")
            Result.failure(e)
        }
    }

    open suspend fun postCommand(path: String, payload: JSONObject? = null): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL(if (path.startsWith("http")) path else "http://127.0.0.1:8080$path")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.doOutput = true

            if (payload != null) {
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }
            } else {
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.close()
            }

            val code = conn.responseCode
            conn.disconnect()
            if (code in 200..299) {
                Result.success(true)
            } else {
                Result.failure(Exception("HTTP $code"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "postCommand $path failed: ${e.message}")
            Result.failure(e)
        }
    }
}
