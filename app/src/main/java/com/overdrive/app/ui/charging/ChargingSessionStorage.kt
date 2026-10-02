package com.overdrive.app.ui.charging

import android.content.Context
import com.overdrive.app.util.DaemonHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

/**
 * Manages charging sessions: local persistence and daemon synchronization.
 * Supports manual entry of charging cost, odometer, date, unit price,
 * start/end battery SoC, station/operator, and duration.
 */
object ChargingSessionStorage {

    private const val FILE_NAME = "overdrive_charging_sessions.json"
    private val memorySessions = mutableListOf<ChargingSession>()
    private var isMemoryLoaded = false

    @Synchronized
    fun loadSessions(context: Context): List<ChargingSession> {
        val result = mutableListOf<ChargingSession>()

        // 1. Read locally persisted sessions
        val localList = readLocalSessions(context)
        result.addAll(localList)

        // 2. Try fetching sessions from daemon API
        try {
            val conn = DaemonHttpClient.open("/api/charging?days=0&limit=200", "GET", 2500, 3000)
            if (conn.responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(body)
                val arr = root.optJSONArray("sessions")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.optLong("id", -1L).toString()
                        val startTime = obj.optLong("startTime", 0L)
                        if (startTime <= 0) continue

                        val startSoc = obj.optDouble("startSoc", 0.0).toInt().coerceIn(0, 100)
                        val endSoc = obj.optDouble("endSoc", 0.0).toInt().coerceIn(0, 100)
                        val energy = obj.optDouble("energyAdded", 0.0).toFloat()
                        val dur = obj.optInt("durationMinutes", 0)
                        val peak = obj.optDouble("peakPower", 0.0).toFloat()
                        val cost = obj.optDouble("cost", -1.0).takeIf { it >= 0 }?.toFloat()
                        val odo = obj.optInt("startOdometerKm", -1).takeIf { it >= 0 }
                        val rate = obj.optDouble("electricityRate", -1.0).takeIf { it > 0 }?.toFloat()
                        val isDc = obj.optBoolean("isDc", false)
                        val loc = obj.optString("placeLabel", "").takeIf { it.isNotEmpty() }
                            ?: obj.optString("tariffLabel", "").takeIf { it.isNotEmpty() }
                            ?: (if (isDc) "DC Hızlı Şarj İstasyonu" else "AC Şarj İstasyonu")

                        val costStr = if (cost != null && cost > 0) String.format(Locale.getDefault(), "₺%.2f", cost) else null

                        // Check if not already in local list
                        if (result.none { it.id == id || (kotlin.math.abs(it.timestamp - startTime) < 60000) }) {
                            result.add(
                                ChargingSession(
                                    id = id,
                                    timestamp = startTime,
                                    location = loc,
                                    startSoc = startSoc,
                                    endSoc = endSoc,
                                    energyKwh = energy,
                                    durationMinutes = dur,
                                    peakPowerKw = peak,
                                    costEstimate = costStr,
                                    odometerKm = odo,
                                    unitPrice = rate,
                                    totalCost = cost,
                                    isDc = isDc
                                )
                            )
                        }
                    }
                }
            }
            conn.disconnect()
        } catch (_: Throwable) {
            // Daemon not reachable or in offline mode, fallback cleanly to local sessions
        }

        result.sortByDescending { it.timestamp }
        memorySessions.clear()
        memorySessions.addAll(result)
        isMemoryLoaded = true
        return result
    }

    @Synchronized
    fun saveSession(context: Context, session: ChargingSession) {
        val list = readLocalSessions(context).toMutableList()
        // Replace or add
        list.removeAll { it.id == session.id }
        list.add(0, session)
        writeLocalSessions(context, list)

        memorySessions.removeAll { it.id == session.id }
        memorySessions.add(0, session)
        memorySessions.sortByDescending { it.timestamp }

        // Also push to daemon if available
        Thread {
            try {
                val json = JSONObject().apply {
                    put("startTime", session.timestamp)
                    put("endTime", session.timestamp + (session.durationMinutes * 60_000L))
                    put("startSoc", session.startSoc)
                    put("endSoc", session.endSoc)
                    put("energyAddedKwh", session.energyKwh)
                    put("cost", session.totalCost ?: 0f)
                    put("rate", session.unitPrice ?: 0f)
                    put("currency", "₺")
                    put("placeLabel", session.location)
                    put("odometerKm", session.odometerKm ?: -1)
                    put("isDc", session.isDc)
                    put("durationMinutes", session.durationMinutes)
                }
                val conn = DaemonHttpClient.open("/api/charging/manual", "POST", 3000, 5000)
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(json.toString().toByteArray()) }
                conn.responseCode
                conn.disconnect()
            } catch (_: Throwable) {}
        }.start()
    }

    @Synchronized
    fun deleteSession(context: Context, sessionId: String) {
        val list = readLocalSessions(context).toMutableList()
        list.removeAll { it.id == sessionId }
        writeLocalSessions(context, list)

        memorySessions.removeAll { it.id == sessionId }

        Thread {
            try {
                val conn = DaemonHttpClient.open("/api/charging/$sessionId/delete", "POST", 3000, 5000)
                conn.responseCode
                conn.disconnect()
            } catch (_: Throwable) {}
        }.start()
    }

    private fun getStorageFile(context: Context): File {
        return File(context.filesDir, FILE_NAME)
    }

    private fun readLocalSessions(context: Context): List<ChargingSession> {
        val file = getStorageFile(context)
        if (!file.exists()) return emptyList()
        val list = mutableListOf<ChargingSession>()
        try {
            val content = file.readText()
            val arr = JSONArray(content)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id", System.currentTimeMillis().toString())
                val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                val location = obj.optString("location", "Ev / AC İstasyon")
                val startSoc = obj.optInt("startSoc", 0)
                val endSoc = obj.optInt("endSoc", 0)
                val energyKwh = obj.optDouble("energyKwh", 0.0).toFloat()
                val durationMinutes = obj.optInt("durationMinutes", 0)
                val peakPowerKw = obj.optDouble("peakPowerKw", 0.0).toFloat()
                val costEstimate = obj.optString("costEstimate", "").takeIf { it.isNotEmpty() }
                val odometerKm = if (obj.has("odometerKm")) obj.optInt("odometerKm") else null
                val unitPrice = if (obj.has("unitPrice")) obj.optDouble("unitPrice").toFloat() else null
                val totalCost = if (obj.has("totalCost")) obj.optDouble("totalCost").toFloat() else null
                val isDc = obj.optBoolean("isDc", false)
                val chargeType = obj.optString("chargeType", if (isDc) "DC" else "AC")
                val isManualEdit = obj.optBoolean("isManualEdit", false)

                list.add(
                    ChargingSession(
                        id = id,
                        timestamp = timestamp,
                        location = location,
                        startSoc = startSoc,
                        endSoc = endSoc,
                        energyKwh = energyKwh,
                        durationMinutes = durationMinutes,
                        peakPowerKw = peakPowerKw,
                        costEstimate = costEstimate,
                        odometerKm = odometerKm,
                        unitPrice = unitPrice,
                        totalCost = totalCost,
                        isDc = isDc,
                        chargeType = chargeType,
                        isManualEdit = isManualEdit
                    )
                )
            }
        } catch (_: Throwable) {}
        return list
    }

    private fun writeLocalSessions(context: Context, list: List<ChargingSession>) {
        try {
            val arr = JSONArray()
            list.forEach { session ->
                val obj = JSONObject().apply {
                    put("id", session.id)
                    put("timestamp", session.timestamp)
                    put("location", session.location)
                    put("startSoc", session.startSoc)
                    put("endSoc", session.endSoc)
                    put("energyKwh", session.energyKwh)
                    put("durationMinutes", session.durationMinutes)
                    put("peakPowerKw", session.peakPowerKw)
                    put("costEstimate", session.costEstimate ?: "")
                    if (session.odometerKm != null) put("odometerKm", session.odometerKm)
                    if (session.unitPrice != null) put("unitPrice", session.unitPrice.toDouble())
                    if (session.totalCost != null) put("totalCost", session.totalCost.toDouble())
                    put("isDc", session.isDc)
                    put("chargeType", session.chargeType)
                    put("isManualEdit", session.isManualEdit)
                }
                arr.put(obj)
            }
            getStorageFile(context).writeText(arr.toString())
        } catch (_: Throwable) {}
    }
}
