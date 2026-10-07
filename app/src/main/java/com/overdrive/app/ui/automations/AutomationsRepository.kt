package com.overdrive.app.ui.automations

import android.content.Context
import com.overdrive.app.logging.DaemonLogger
import com.overdrive.app.util.DaemonHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

open class AutomationsRepository(private val context: Context) {

    private val logger = DaemonLogger.getInstance("AutomationsRepository")

    open suspend fun getAutomations(): Result<List<AutomationItem>> = withContext(Dispatchers.IO) {
        try {
            val response = executeHttp("/api/automations/list", "GET")
                ?: return@withContext Result.failure(Exception("Empty response from server"))
            val json = JSONObject(response)
            val list = mutableListOf<AutomationItem>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val autoObj = json.optJSONObject(key) ?: continue
                list.add(AutomationItem.fromJson(key, autoObj))
            }
            Result.success(list)
        } catch (e: Exception) {
            logger.error("Error fetching automations: ${e.message}", e)
            Result.failure(e)
        }
    }

    open suspend fun setAutomationMode(id: String, mode: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply { put("mode", mode) }.toString()
            val response = executeHttp("/api/automations/mode/$id", "POST", payload)
            Result.success(response != null)
        } catch (e: Exception) {
            logger.error("Error setting automation mode ($id -> $mode): ${e.message}", e)
            Result.failure(e)
        }
    }

    open suspend fun testAutomation(id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = executeHttp("/api/automations/test/$id", "POST")
            Result.success(response != null)
        } catch (e: Exception) {
            logger.error("Error testing automation ($id): ${e.message}", e)
            Result.failure(e)
        }
    }

    open suspend fun deleteAutomation(id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = executeHttp("/api/automations/automation/$id", "DELETE")
            Result.success(response != null)
        } catch (e: Exception) {
            logger.error("Error deleting automation ($id): ${e.message}", e)
            Result.failure(e)
        }
    }

    open suspend fun getActionGroups(): Result<List<ActionGroupItem>> = withContext(Dispatchers.IO) {
        try {
            val response = executeHttp("/api/action-groups", "GET")
                ?: return@withContext Result.failure(Exception("Empty response from server"))
            val list = mutableListOf<ActionGroupItem>()
            if (response.trim().startsWith("[")) {
                val arr = JSONArray(response)
                for (i in 0 until arr.length()) {
                    val g = arr.optJSONObject(i) ?: continue
                    val gid = g.optString("id", "group_$i")
                    list.add(ActionGroupItem.fromJson(gid, g))
                }
            } else {
                val json = JSONObject(response)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val gObj = json.optJSONObject(key) ?: continue
                    list.add(ActionGroupItem.fromJson(key, gObj))
                }
            }
            Result.success(list)
        } catch (e: Exception) {
            logger.error("Error fetching action groups: ${e.message}", e)
            Result.failure(e)
        }
    }

    open suspend fun runActionGroup(id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = executeHttp("/api/action-groups/$id/run", "POST")
            Result.success(response != null)
        } catch (e: Exception) {
            logger.error("Error running action group ($id): ${e.message}", e)
            Result.failure(e)
        }
    }

    open suspend fun deleteActionGroup(id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = executeHttp("/api/action-groups/$id", "DELETE")
            Result.success(response != null)
        } catch (e: Exception) {
            logger.error("Error deleting action group ($id): ${e.message}", e)
            Result.failure(e)
        }
    }

    open suspend fun getSettings(): Result<AutomationSettingsItem> = withContext(Dispatchers.IO) {
        try {
            val response = executeHttp("/api/automations/settings", "GET")
                ?: return@withContext Result.failure(Exception("Empty response from server"))
            val json = JSONObject(response)
            Result.success(AutomationSettingsItem.fromJson(json))
        } catch (e: Exception) {
            logger.error("Error fetching settings: ${e.message}", e)
            Result.failure(e)
        }
    }

    open suspend fun updateSafetyGuard(guardKey: String, enabled: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("drivingSafety", JSONObject().apply {
                    put(guardKey, enabled)
                })
            }.toString()
            val response = executeHttp("/api/automations/settings", "POST", payload)
            Result.success(response != null)
        } catch (e: Exception) {
            logger.error("Error updating safety guard ($guardKey=$enabled): ${e.message}", e)
            Result.failure(e)
        }
    }

    open suspend fun updateAllowShell(enabled: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("allowShell", enabled)
            }.toString()
            val response = executeHttp("/api/automations/settings", "POST", payload)
            Result.success(response != null)
        } catch (e: Exception) {
            logger.error("Error updating allowShell ($enabled): ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun executeHttp(path: String, method: String, body: String? = null): String? {
        val conn = DaemonHttpClient.open(path, method, 3000, 5000)
        if (body != null) {
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.bufferedWriter().use { it.write(body) }
        }
        return try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream ?: conn.inputStream
            BufferedReader(InputStreamReader(stream)).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
