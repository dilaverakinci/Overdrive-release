package com.overdrive.app.ui.parking

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

open class ParkingRepository {

    companion object {
        private const val TAG = "ParkingRepository"
        private const val BASE_URL = "http://127.0.0.1:8080/api/parking"
    }

    open suspend fun getStatus(): Result<ParkingStatus> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/status")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000

            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(body)

                val running = json.optBoolean("running", false)
                val enabled = json.optBoolean("enabled", false)

                val currentObj = json.optJSONObject("current")
                val current = if (currentObj != null) ParkingSession.fromJson(currentObj, isCurrent = true) else null

                val configObj = json.optJSONObject("config")
                val config = if (configObj != null) {
                    ParkingConfig(
                        enabled = configObj.optBoolean("enabled", enabled),
                        endTrigger = configObj.optString("endTrigger", "return"),
                        snapshots = configObj.optBoolean("snapshots", true),
                        neighbours = configObj.optBoolean("neighbours", true),
                        signage = configObj.optBoolean("signage", true),
                        retentionDays = configObj.optInt("retentionDays", 30)
                    )
                } else ParkingConfig(enabled = enabled)

                Result.success(ParkingStatus(running, enabled, current, config))
            } else {
                conn.disconnect()
                Result.failure(Exception("HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "getStatus failed: ${e.message}")
            Result.failure(e)
        }
    }

    open suspend fun getSessions(days: Int, limit: Int = 50, offset: Int = 0): Result<List<ParkingSession>> = withContext(Dispatchers.IO) {
        try {
            val query = if (days > 0) "days=$days&limit=$limit&offset=$offset" else "limit=$limit&offset=$offset"
            val url = URL("$BASE_URL/sessions?$query")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000

            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(body)

                val list = mutableListOf<ParkingSession>()
                val arr = json.optJSONArray("sessions") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val sObj = arr.optJSONObject(i) ?: continue
                    list.add(ParkingSession.fromJson(sObj, isCurrent = false))
                }
                Result.success(list)
            } else {
                conn.disconnect()
                Result.failure(Exception("HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "getSessions failed: ${e.message}")
            Result.failure(e)
        }
    }

    open suspend fun saveConfig(config: ParkingConfig): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/config")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 3000
            conn.readTimeout = 3000

            val json = JSONObject().apply {
                put("enabled", config.enabled)
                put("endTrigger", config.endTrigger)
                put("snapshots", config.snapshots)
                put("neighbours", config.neighbours)
                put("signage", config.signage)
                put("retentionDays", config.retentionDays)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }

            val success = conn.responseCode in 200..299
            conn.disconnect()
            Result.success(success)
        } catch (e: Exception) {
            Log.w(TAG, "saveConfig failed: ${e.message}")
            Result.failure(e)
        }
    }

    open suspend fun deleteSession(sessionId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/session/$sessionId")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "DELETE"
            conn.connectTimeout = 3000
            conn.readTimeout = 3000

            val success = conn.responseCode in 200..299
            conn.disconnect()
            Result.success(success)
        } catch (e: Exception) {
            Log.w(TAG, "deleteSession failed: ${e.message}")
            Result.failure(e)
        }
    }
}
