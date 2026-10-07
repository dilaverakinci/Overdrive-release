package com.overdrive.app.ui.keymapping

import android.content.Context
import com.overdrive.app.logging.DaemonLogger
import com.overdrive.app.util.DaemonHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

open class KeyMappingRepository(private val context: Context? = null) {

    private val logger = DaemonLogger.getInstance("KeyMappingRepository")

    open suspend fun getConfig(): Result<KeymapConfig> = withContext(Dispatchers.IO) {
        try {
            val response = executeHttp("/api/keymap/config", "GET")
                ?: return@withContext Result.failure(Exception("Empty response from server"))
            val json = JSONObject(response)
            Result.success(KeymapConfig.fromJson(json))
        } catch (e: Exception) {
            logger.error("Error fetching keymap config: ${e.message}", e)
            Result.failure(e)
        }
    }

    open suspend fun saveConfig(config: KeymapConfig): Result<KeymapConfig> = withContext(Dispatchers.IO) {
        try {
            val body = config.toJson().toString()
            val response = executeHttp("/api/keymap/config", "POST", body)
                ?: return@withContext Result.failure(Exception("Empty response when saving config"))
            val json = JSONObject(response)
            Result.success(KeymapConfig.fromJson(json))
        } catch (e: Exception) {
            logger.error("Error saving keymap config: ${e.message}", e)
            Result.failure(e)
        }
    }

    open suspend fun getInstalledApps(): Result<List<AppInfo>> = withContext(Dispatchers.IO) {
        try {
            val response = executeHttp("/api/apps/list", "GET")
                ?: return@withContext Result.failure(Exception("Empty response fetching apps"))
            val json = JSONObject(response)
            val appsArr = json.optJSONArray("apps") ?: JSONArray()
            val list = mutableListOf<AppInfo>()
            for (i in 0 until appsArr.length()) {
                val appObj = appsArr.optJSONObject(i) ?: continue
                val pkg = appObj.optString("package", "")
                val label = appObj.optString("label", pkg)
                if (pkg.isNotBlank()) {
                    list.add(AppInfo(pkg, label))
                }
            }
            list.sortBy { it.label.lowercase() }
            Result.success(list)
        } catch (e: Exception) {
            logger.error("Error fetching installed apps: ${e.message}", e)
            Result.failure(e)
        }
    }

    open suspend fun fireAction(action: KeyAction): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val body = action.toJson().toString()
            val response = executeHttp("/api/keymap/fire", "POST", body)
            Result.success(response != null)
        } catch (e: Exception) {
            logger.error("Error testing action fire: ${e.message}", e)
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
