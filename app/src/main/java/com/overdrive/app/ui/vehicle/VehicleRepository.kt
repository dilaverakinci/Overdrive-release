package com.overdrive.app.ui.vehicle

import android.util.Log
import com.overdrive.app.util.DaemonHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

open class VehicleRepository {

    companion object {
        private const val TAG = "VehicleRepository"
    }

    open suspend fun getSelectedModelId(): String? = withContext(Dispatchers.IO) {
        try {
            val mConn = DaemonHttpClient.open("/api/models/selected", "GET", 2000, 2000)
            if (mConn.responseCode == 200) {
                val body = mConn.inputStream.bufferedReader().use { it.readText() }
                val mJson = JSONObject(body)
                val m = when {
                    mJson.has("selectedModelId") && !mJson.isNull("selectedModelId") ->
                        mJson.optString("selectedModelId", "")
                    mJson.has("modelSource") && mJson.optString("modelSource", "unset") == "unset" ->
                        ""
                    else ->
                        mJson.optString("modelId", "")
                }
                if (m.isNotEmpty() && !m.equals("null", ignoreCase = true)) {
                    return@withContext m
                }
            }
            mConn.disconnect()
        } catch (_: Exception) {}
        null
    }

    open suspend fun getVehicleState(): Result<VehicleState> = getVehicleState(getSelectedModelId())

    open suspend fun getVehicleState(modelId: String?): Result<VehicleState> = coroutineScope {
        try {
            // Fetch /api/vehicle/state and /api/vehicle/cloud-status concurrently
            val stateDeferred = async(Dispatchers.IO) {
                try {
                    val stateConn = DaemonHttpClient.open("/api/vehicle/state", "GET", 2500, 2500)
                    val body = if (stateConn.responseCode == 200) {
                        stateConn.inputStream.bufferedReader().use { it.readText() }
                    } else null
                    stateConn.disconnect()
                    if (body != null) JSONObject(body) else null
                } catch (_: Exception) {
                    null
                }
            }

            val cloudDeferred = async(Dispatchers.IO) {
                try {
                    val cloudConn = DaemonHttpClient.open("/api/vehicle/cloud-status", "GET", 2000, 2000)
                    val body = if (cloudConn.responseCode == 200) {
                        cloudConn.inputStream.bufferedReader().use { it.readText() }
                    } else null
                    cloudConn.disconnect()
                    if (body != null) {
                        val cloudJson = JSONObject(body)
                        cloudJson.optBoolean("verified", false) && cloudJson.optBoolean("enabled", false)
                    } else false
                } catch (_: Exception) {
                    false
                }
            }

            val stateJson = stateDeferred.await()
            val cloudConnected = cloudDeferred.await()

            if (stateJson == null || !stateJson.optBoolean("success", false)) {
                return@coroutineScope Result.success(
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

    open suspend fun selectModel(modelId: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = DaemonHttpClient.open("/api/models/selected", "POST", 3000, 5000)
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            val payload = if (modelId.isNullOrEmpty()) {
                JSONObject().put("clearModelSelection", true)
            } else {
                JSONObject().put("modelId", modelId)
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray()) }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "Error selecting model: $modelId", e)
            false
        }
    }

    open suspend fun getAvailableModels(): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val conn = DaemonHttpClient.open("/api/models/manifest", "GET", 2000, 3000)
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val arr = json.optJSONArray("models")
                if (arr != null && arr.length() > 0) {
                    val list = mutableListOf<Pair<String, String>>()
                    for (i in 0 until arr.length()) {
                        val m = arr.getJSONObject(i)
                        val id = m.optString("id", "")
                        val name = m.optString("name", id)
                        if (id.isNotEmpty()) {
                            list.add(id to name)
                        }
                    }
                    return@withContext list
                }
            }
            conn.disconnect()
        } catch (_: Exception) {}
        listOf(
            "seal" to "BYD Seal",
            "sealion7" to "BYD Sealion 7",
            "shark" to "BYD Shark",
            "seal-u" to "BYD Seal U",
            "seal-u-dmi" to "BYD Seal U DM-i",
            "dolphin" to "BYD Dolphin",
            "atto3" to "BYD Atto 3",
            "atto3-evo" to "BYD Atto 3 Evo",
            "atto2" to "BYD Atto 2",
            "han" to "BYD Han",
            "tang" to "BYD Tang",
            "m6" to "BYD M6",
            "seagull" to "BYD Seagull",
            "destroyer" to "BYD Destroyer 05"
        )
    }

    open suspend fun postCommand(path: String, payload: JSONObject? = null): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val endpoint = if (path.startsWith("http")) {
                URL(path).path
            } else path
            val conn = DaemonHttpClient.open(endpoint, "POST", 4000, 4000)
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
