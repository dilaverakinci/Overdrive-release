package com.overdrive.app.ui.seatpositions

import android.util.Log
import com.overdrive.app.util.DaemonHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

open class SeatPositionsRepository {

    companion object {
        private const val TAG = "SeatPositionsRepo"
    }

    data class PositionsApiResponse(
        val positions: List<SeatPosition>,
        val gate: GateState,
        val currentProfile: String?
    )

    data class CurrentPositionApiResponse(
        val axes: Map<String, Double>?,
        val ambientColour: Int?,
        val palette: List<String>,
        val colourMax: Int,
        val gate: GateState
    )

    open suspend fun getPositions(): Result<PositionsApiResponse> = withContext(Dispatchers.IO) {
        try {
            val conn = DaemonHttpClient.open("/api/positions?withProfile=1", "GET", 3000, 3000)
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(body)

                val list = mutableListOf<SeatPosition>()
                val positionsArr = json.optJSONArray("positions") ?: JSONArray()
                for (i in 0 until positionsArr.length()) {
                    val p = positionsArr.optJSONObject(i) ?: continue
                    val id = p.optString("id", "")
                    val name = p.optString("name", "")
                    val source = p.optString("source", "user")
                    val slot = if (p.has("slot") && !p.isNull("slot")) p.optInt("slot") else null
                    val alias = if (p.has("alias") && !p.isNull("alias")) p.optString("alias").takeIf { it.isNotEmpty() } else null
                    val ambientColour = if (p.has("ambientColour") && !p.isNull("ambientColour")) p.optInt("ambientColour") else null
                    val created = p.optLong("created", 0L)

                    val axesMap = mutableMapOf<String, Double>()
                    val axesObj = p.optJSONObject("axes")
                    if (axesObj != null) {
                        for (key in axesObj.keys()) {
                            axesMap[key] = axesObj.optDouble(key, SeatGeometryHelper.SENTINEL)
                        }
                    }

                    if (id.isNotEmpty()) {
                        list.add(SeatPosition(id, name, source, slot, alias, ambientColour, axesMap, created))
                    }
                }

                val gate = parseGateState(json)
                val profile = json.optString("currentProfile", "").takeIf { it.isNotEmpty() && !it.equals("null", true) }
                Result.success(PositionsApiResponse(list, gate, profile))
            } else {
                conn.disconnect()
                Result.failure(Exception("HTTP error ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "getPositions error: ${e.message}")
            Result.failure(e)
        }
    }

    open suspend fun getCurrentPosition(): Result<CurrentPositionApiResponse> = withContext(Dispatchers.IO) {
        try {
            val conn = DaemonHttpClient.open("/api/positions/current", "GET", 2500, 2500)
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(body)

                val axesMap = mutableMapOf<String, Double>()
                val axesObj = json.optJSONObject("axes")
                if (axesObj != null) {
                    for (key in axesObj.keys()) {
                        axesMap[key] = axesObj.optDouble(key, SeatGeometryHelper.SENTINEL)
                    }
                }

                var ambientColour: Int? = null
                val ambientObj = json.optJSONObject("ambient")
                if (ambientObj != null && ambientObj.has("colour")) {
                    ambientColour = ambientObj.optInt("colour")
                }

                val palette = mutableListOf<String>()
                val palArr = json.optJSONArray("ambientPalette")
                if (palArr != null) {
                    for (i in 0 until palArr.length()) {
                        val c = palArr.optString(i)
                        if (c.isNotEmpty()) palette.add(c)
                    }
                }

                val colourMax = json.optInt("ambientColourMax", 30)
                val gate = parseGateState(json)

                Result.success(CurrentPositionApiResponse(
                    axes = axesMap.takeIf { it.isNotEmpty() },
                    ambientColour = ambientColour,
                    palette = palette,
                    colourMax = colourMax,
                    gate = gate
                ))
            } else {
                conn.disconnect()
                Result.failure(Exception("HTTP error ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "getCurrentPosition error: ${e.message}")
            Result.failure(e)
        }
    }

    open suspend fun getAutomations(): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            val conn = DaemonHttpClient.open("/api/automations/list", "GET", 2500, 2500)
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(body)
                val map = mutableMapOf<String, String>() // positionId -> automationName
                val rulesArr = json.optJSONArray("rules")
                if (rulesArr != null) {
                    for (i in 0 until rulesArr.length()) {
                        val rule = rulesArr.optJSONObject(i) ?: continue
                        val ruleName = rule.optString("name", "Automation")
                        val actionsArr = rule.optJSONArray("actions") ?: JSONArray()
                        for (j in 0 until actionsArr.length()) {
                            val act = actionsArr.optJSONObject(j) ?: continue
                            val actionType = act.optString("type", "")
                            if (actionType.contains("seat_position", ignoreCase = true) ||
                                actionType.contains("position", ignoreCase = true)) {
                                val pid = act.optString("positionId", act.optString("id", ""))
                                if (pid.isNotEmpty()) {
                                    map[pid] = ruleName
                                }
                            }
                        }
                    }
                }
                Result.success(map)
            } else {
                conn.disconnect()
                Result.success(emptyMap())
            }
        } catch (e: Exception) {
            Log.w(TAG, "getAutomations error: ${e.message}")
            Result.success(emptyMap())
        }
    }

    open suspend fun applyPosition(id: String, ackModel: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        val query = if (ackModel) "?id=${URLEncoder.encode(id, "UTF-8")}&ackModel=1"
        else "?id=${URLEncoder.encode(id, "UTF-8")}"
        executePost("/api/positions/apply$query")
    }

    open suspend fun createPosition(name: String, parts: String = "all"): Result<Unit> = withContext(Dispatchers.IO) {
        val encodedName = URLEncoder.encode(name, "UTF-8")
        executePost("/api/positions/create?name=$encodedName&parts=$parts")
    }

    open suspend fun saveOverPosition(id: String, parts: String = "all"): Result<Unit> = withContext(Dispatchers.IO) {
        val encodedId = URLEncoder.encode(id, "UTF-8")
        executePost("/api/positions/save?id=$encodedId&parts=$parts")
    }

    open suspend fun renamePosition(id: String, newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        val encodedId = URLEncoder.encode(id, "UTF-8")
        val encodedName = URLEncoder.encode(newName, "UTF-8")
        executePost("/api/positions/rename?id=$encodedId&name=$encodedName")
    }

    open suspend fun setAlias(id: String, alias: String?): Result<Unit> = withContext(Dispatchers.IO) {
        val encodedId = URLEncoder.encode(id, "UTF-8")
        val query = if (alias != null) "&alias=${URLEncoder.encode(alias, "UTF-8")}" else ""
        executePost("/api/positions/alias?id=$encodedId$query")
    }

    open suspend fun deletePosition(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        val encodedId = URLEncoder.encode(id, "UTF-8")
        executePost("/api/positions/delete?id=$encodedId")
    }

    open suspend fun setAmbientColour(id: String, colourIndex: Int): Result<Unit> = withContext(Dispatchers.IO) {
        val encodedId = URLEncoder.encode(id, "UTF-8")
        executePost("/api/positions/ambient-colour?id=$encodedId&colour=$colourIndex")
    }

    private fun executePost(path: String): Result<Unit> {
        return try {
            val conn = DaemonHttpClient.open(path, "POST", 5000, 5000)
            val code = conn.responseCode
            val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val json = JSONObject(responseBody)
            if (code in 200..299 && !json.has("error")) {
                Result.success(Unit)
            } else {
                val errorMsg = json.optString("error", "Failed with code $code")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.w(TAG, "executePost $path error: ${e.message}")
            Result.failure(e)
        }
    }

    private fun parseGateState(json: JSONObject): GateState {
        val acc = json.optBoolean("acc", false)
        val movementBlocked = json.optBoolean("movementBlocked", false)
        val reason = json.optString("movementBlockReason", "").takeIf { it.isNotEmpty() && !it.equals("null", true) }
        val positioningBlocked = json.optBoolean("positioningBlocked", false)
        val modelId = json.optString("modelId", "").takeIf { it.isNotEmpty() && !it.equals("null", true) }
        val modelConfirmed = json.optBoolean("modelConfirmed", false)
        val modelAcknowledged = json.optBoolean("modelAcknowledged", false)
        return GateState(
            acc = acc,
            movementBlocked = movementBlocked,
            movementBlockReason = reason,
            positioningBlocked = positioningBlocked,
            modelId = modelId,
            modelConfirmed = modelConfirmed,
            modelAcknowledged = modelAcknowledged
        )
    }
}
