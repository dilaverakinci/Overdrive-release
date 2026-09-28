package com.overdrive.app.ui.parking

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.overdrive.app.util.DaemonHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection

object ParkingApiClient {
    private const val TAG = "ParkingApiClient"

    fun fetchStatus(): Triple<ParkingSessionModel?, ParkingConfigModel, Boolean> {
        var conn: HttpURLConnection? = null
        try {
            conn = DaemonHttpClient.open("/api/parking/status", "GET", 2500, 4000)
            if (conn.responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
                val json = JSONObject(reader.readText())
                reader.close()

                val running = json.optBoolean("running", false)
                val curObj = json.optJSONObject("current")
                val cur = if (curObj != null) ParkingSessionModel.fromJson(curObj) else null

                val cfgObj = json.optJSONObject("config")
                val cfg = if (cfgObj != null) {
                    ParkingConfigModel(
                        enabled = cfgObj.optBoolean("enabled", false),
                        snapshots = cfgObj.optBoolean("snapshots", true),
                        neighbours = cfgObj.optBoolean("neighbours", true),
                        signage = cfgObj.optBoolean("signage", true),
                        retentionDays = cfgObj.optInt("retentionDays", 90),
                        storageCapMb = cfgObj.optInt("storageCapMb", 300),
                        endTrigger = cfgObj.optString("endTrigger", "return")
                    )
                } else {
                    ParkingConfigModel()
                }
                return Triple(cur, cfg, running)
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchStatus failed: ${e.message}")
        } finally {
            conn?.disconnect()
        }
        return Triple(null, ParkingConfigModel(), false)
    }

    fun fetchGeocoding(): Pair<Boolean, Boolean> {
        var conn: HttpURLConnection? = null
        try {
            conn = DaemonHttpClient.open("/api/settings/geocoding", "GET", 2000, 3000)
            if (conn.responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
                val json = JSONObject(reader.readText())
                reader.close()
                val parkingGeo = json.optJSONObject("parking")
                if (parkingGeo != null) {
                    val enabled = parkingGeo.optBoolean("enabled", false)
                    val allowOnline = parkingGeo.optBoolean("allowOnline", false)
                    return Pair(enabled, allowOnline)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchGeocoding failed: ${e.message}")
        } finally {
            conn?.disconnect()
        }
        return Pair(false, false)
    }

    fun fetchSessions(rangeDays: Int, limit: Int = 50, offset: Int = 0): List<ParkingSessionModel> {
        val list = mutableListOf<ParkingSessionModel>()
        var conn: HttpURLConnection? = null
        try {
            val from = if (rangeDays > 0) System.currentTimeMillis() - (rangeDays * 86400000L) else 0L
            val query = StringBuilder("/api/parking/sessions?limit=").append(limit).append("&offset=").append(offset)
            if (from > 0) query.append("&from=").append(from)

            conn = DaemonHttpClient.open(query.toString(), "GET", 3000, 6000)
            if (conn.responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
                val json = JSONObject(reader.readText())
                reader.close()

                val arr = json.optJSONArray("sessions")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i)
                        if (obj != null) {
                            list.add(ParkingSessionModel.fromJson(obj))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchSessions failed: ${e.message}")
        } finally {
            conn?.disconnect()
        }
        return list
    }

    fun fetchSessionDetail(sessionId: String): ParkingDetailData? {
        var conn: HttpURLConnection? = null
        try {
            conn = DaemonHttpClient.open("/api/parking/sessions/${sessionId}", "GET", 3000, 6000)
            if (conn.responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
                val json = JSONObject(reader.readText())
                reader.close()

                val sObj = json.optJSONObject("session") ?: return null
                val session = ParkingSessionModel.fromJson(sObj)

                val nList = mutableListOf<ParkingNeighbourModel>()
                val nArr = json.optJSONArray("neighbours")
                if (nArr != null) {
                    for (i in 0 until nArr.length()) {
                        val nobj = nArr.optJSONObject(i) ?: continue
                        val framesList = mutableListOf<ParkingFrameModel>()
                        val fArr = nobj.optJSONArray("frames")
                        if (fArr != null) {
                            for (j in 0 until fArr.length()) {
                                val fObj = fArr.optJSONObject(j) ?: continue
                                framesList.add(
                                    ParkingFrameModel(
                                        name = fObj.optString("name"),
                                        score = if (fObj.has("score")) fObj.optDouble("score") else null,
                                        ms = if (fObj.has("ms")) fObj.optLong("ms") else null,
                                        side = fObj.optString("side", null)
                                    )
                                )
                            }
                        }
                        nList.add(
                            ParkingNeighbourModel(
                                id = nobj.optLong("id", 0L),
                                sessionId = nobj.optString("sessionId", sessionId),
                                key = nobj.optString("key", ""),
                                side = nobj.optInt("side", 0),
                                sideName = nobj.optString("sideName", "front"),
                                kind = nobj.optString("kind", "NEIGHBOUR"),
                                classGroup = nobj.optString("classGroup", "VEHICLE"),
                                status = nobj.optString("status", ""),
                                confirmed = nobj.optBoolean("confirmed", true),
                                arrivedMs = if (nobj.has("arrivedMs")) nobj.optLong("arrivedMs") else null,
                                departedMs = if (nobj.has("departedMs")) nobj.optLong("departedMs") else null,
                                lastSeenMs = if (nobj.has("lastSeenMs")) nobj.optLong("lastSeenMs") else null,
                                arrivalEvent = nobj.optString("arrivalEvent", null),
                                departureEvent = nobj.optString("departureEvent", null),
                                frames = framesList
                            )
                        )
                    }
                }

                val evList = mutableListOf<ParkingEventModel>()
                val evArr = json.optJSONArray("events")
                if (evArr != null) {
                    for (i in 0 until evArr.length()) {
                        val eobj = evArr.optJSONObject(i) ?: continue
                        evList.add(
                            ParkingEventModel(
                                id = eobj.optString("id", ""),
                                timestamp = eobj.optLong("timestamp", 0L),
                                filename = eobj.optString("filename", ""),
                                thumbnailUrl = eobj.optString("thumbnailUrl", null),
                                heroThumbnailUrl = eobj.optString("heroThumbnailUrl", null),
                                peakSeverity = eobj.optString("peakSeverity", null),
                                personCount = eobj.optInt("personCount", 0),
                                vehicleCount = eobj.optInt("vehicleCount", 0),
                                bikeCount = eobj.optInt("bikeCount", 0),
                                animalCount = eobj.optInt("animalCount", 0),
                                cameras = eobj.optString("cameras", null)
                            )
                        )
                    }
                }

                val assetsMap = mutableMapOf<String, Map<String, String>>()
                val aObj = json.optJSONObject("assets")
                if (aObj != null) {
                    for (key in listOf("arrived", "returned")) {
                        val sub = aObj.optJSONObject(key)
                        if (sub != null) {
                            val inner = mutableMapOf<String, String>()
                            for (side in listOf("mosaic", "front", "right", "rear", "left")) {
                                if (sub.has(side)) inner[side] = sub.optString(side)
                            }
                            if (inner.isNotEmpty()) assetsMap[key] = inner
                        }
                    }
                }

                return ParkingDetailData(session, nList, evList, assetsMap)
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchSessionDetail failed: ${e.message}")
        } finally {
            conn?.disconnect()
        }
        return null
    }

    fun saveConfig(cfg: ParkingConfigModel): Boolean {
        var conn: HttpURLConnection? = null
        try {
            conn = DaemonHttpClient.open("/api/parking/config", "POST", 3000, 5000)
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            val json = JSONObject().apply {
                put("enabled", cfg.enabled)
                put("snapshots", cfg.snapshots)
                put("neighbours", cfg.neighbours)
                put("signage", cfg.signage)
                put("retentionDays", cfg.retentionDays)
                put("storageCapMb", cfg.storageCapMb)
                put("endTrigger", cfg.endTrigger)
            }
            val writer = OutputStreamWriter(conn.outputStream, Charsets.UTF_8)
            writer.write(json.toString())
            writer.flush()
            writer.close()

            return conn.responseCode in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "saveConfig failed: ${e.message}")
            return false
        } finally {
            conn?.disconnect()
        }
    }

    fun saveGeocoding(enabled: Boolean, allowOnline: Boolean): Boolean {
        var conn: HttpURLConnection? = null
        try {
            conn = DaemonHttpClient.open("/api/settings/geocoding", "POST", 3000, 5000)
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            val pObj = JSONObject().apply {
                put("enabled", enabled)
                put("allowOnline", allowOnline)
            }
            val json = JSONObject().apply {
                put("parking", pObj)
            }
            val writer = OutputStreamWriter(conn.outputStream, Charsets.UTF_8)
            writer.write(json.toString())
            writer.flush()
            writer.close()

            return conn.responseCode in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "saveGeocoding failed: ${e.message}")
            return false
        } finally {
            conn?.disconnect()
        }
    }

    fun deleteSession(sessionId: String): Boolean {
        var conn: HttpURLConnection? = null
        try {
            conn = DaemonHttpClient.open("/api/parking/sessions/${sessionId}", "DELETE", 3000, 5000)
            if (conn.responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
                val json = JSONObject(reader.readText())
                reader.close()
                return json.optBoolean("success", false)
            }
        } catch (e: Exception) {
            Log.w(TAG, "deleteSession failed: ${e.message}")
        } finally {
            conn?.disconnect()
        }
        return false
    }

    fun requeueSignage(sessionId: String): Boolean {
        var conn: HttpURLConnection? = null
        try {
            conn = DaemonHttpClient.open("/api/parking/sessions/${sessionId}/signage", "POST", 3000, 5000)
            return conn.responseCode in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "requeueSignage failed: ${e.message}")
        } finally {
            conn?.disconnect()
        }
        return false
    }

    fun fetchBitmap(pathOrUrl: String): Bitmap? {
        var conn: HttpURLConnection? = null
        try {
            val endpoint = if (pathOrUrl.startsWith("/")) pathOrUrl else "/${pathOrUrl}"
            conn = DaemonHttpClient.open(endpoint, "GET", 4000, 8000)
            if (conn.responseCode in 200..299) {
                return BitmapFactory.decodeStream(conn.inputStream)
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchBitmap failed: ${e.message}")
        } finally {
            conn?.disconnect()
        }
        return null
    }
}
