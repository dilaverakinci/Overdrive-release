package com.overdrive.app.ui.integrations

import android.content.Context
import com.overdrive.app.config.UnifiedConfigManager
import com.overdrive.app.telegram.impl.BotTokenConfig
import com.overdrive.app.util.DaemonHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection

open class IntegrationsRepository(private val context: Context) {

    suspend fun fetchSummary(): IntegrationsSummary = withContext(Dispatchers.IO) {
        val tgConfigured = isTelegramConfigured()
        val abrpRunning = fetchAbrpRunning()
        val mqttConnected = fetchMqttAnyConnected()
        val bydConfigured = fetchBydCloudConfigured()

        IntegrationsSummary(
            telegramConfigured = tgConfigured,
            abrpConnected = abrpRunning,
            mqttConnected = mqttConnected,
            bydCloudConfigured = bydConfigured
        )
    }

    // ==========================================
    // TELEGRAM
    // ==========================================

    fun isTelegramConfigured(): Boolean = try {
        UnifiedConfigManager.forceReload()
        BotTokenConfig(context.applicationContext).hasToken()
    } catch (_: Throwable) {
        false
    }

    suspend fun getTelegramStatus(): TelegramConfigState = withContext(Dispatchers.IO) {
        val json = fetchDaemonJson("/api/telegram/status", "GET")
        if (json != null && json.optBoolean("success", false)) {
            TelegramConfigState.fromJson(json)
        } else {
            TelegramConfigState(hasToken = isTelegramConfigured())
        }
    }

    suspend fun saveTelegramToken(token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply { put("token", token) }.toString()
            val json = fetchDaemonJson("/api/telegram/token", "POST", body)
            if (json != null && json.optBoolean("success", false)) {
                Result.success(true)
            } else {
                val error = json?.optString("error", "Failed to validate Telegram bot token")
                    ?: "Connection error"
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateTelegramPin(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val json = fetchDaemonJson("/api/telegram/pin", "POST")
            if (json != null && json.optBoolean("success", false)) {
                val pin = json.optString("pin", "")
                Result.success(pin)
            } else {
                Result.failure(Exception(json?.optString("error", "Failed to generate PIN")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unpairTelegram(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = fetchDaemonJson("/api/telegram/owner/clear", "POST")
            if (json != null && json.optBoolean("success", false)) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to unpair Telegram bot"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTelegramPreferences(
        videoUploads: Boolean,
        criticalAlerts: Boolean,
        motionText: Boolean,
        tyreAlerts: Boolean
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("video_uploads", videoUploads)
                put("critical_alerts", criticalAlerts)
                put("motion_text", motionText)
                put("tyre_alerts", tyreAlerts)
            }.toString()
            val json = fetchDaemonJson("/api/telegram/preferences", "POST", body)
            if (json != null && json.optBoolean("success", false)) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to save Telegram preferences"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // ABRP
    // ==========================================

    private fun fetchAbrpRunning(): Boolean {
        val json = fetchDaemonJson("/api/abrp/status", "GET") ?: return false
        if (!json.optBoolean("success", false)) return false
        val status = json.optJSONObject("status") ?: return false
        return status.optBoolean("running", false)
    }

    suspend fun getAbrpStatus(): AbrpConfigState = withContext(Dispatchers.IO) {
        val cfgJson = fetchDaemonJson("/api/abrp/config", "GET")
        val statusJson = fetchDaemonJson("/api/abrp/status", "GET")
        AbrpConfigState.fromJson(cfgJson, statusJson)
    }

    suspend fun saveAbrpConfig(token: String, enabled: Boolean, carModel: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("token", token)
                    put("enabled", enabled)
                    put("car_model", carModel)
                }.toString()
                val json = fetchDaemonJson("/api/abrp/config", "POST", body)
                if (json != null && (json.optBoolean("success", false) || json.has("status"))) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Failed to update ABRP settings"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun deleteAbrpToken(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = fetchDaemonJson("/api/abrp/token", "DELETE")
            if (json != null && (json.optBoolean("success", false) || json.has("status"))) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to clear ABRP token"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // MQTT
    // ==========================================

    private fun fetchMqttAnyConnected(): Boolean {
        val json = fetchDaemonJson("/api/mqtt/status", "GET") ?: return false
        if (!json.optBoolean("success", false)) return false
        val arr: JSONArray = json.optJSONArray("connections") ?: return false
        for (i in 0 until arr.length()) {
            val entry = arr.optJSONObject(i) ?: continue
            val status = entry.optJSONObject("status") ?: continue
            if (status.optBoolean("connected", false)) return true
        }
        return false
    }

    suspend fun getMqttConnections(): List<MqttBrokerConfig> = withContext(Dispatchers.IO) {
        val json = fetchDaemonJson("/api/mqtt/status", "GET")
            ?: fetchDaemonJson("/api/mqtt/connections", "GET")
        val arr = json?.optJSONArray("connections") ?: JSONArray()
        MqttBrokerConfig.parseList(arr)
    }

    suspend fun addMqttConnection(broker: MqttBrokerConfig): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("name", broker.name)
                put("broker", broker.host)
                put("port", broker.port)
                put("username", broker.username)
                put("client_id", broker.clientId)
                put("topic_prefix", broker.topicPrefix)
                put("publish_interval", broker.publishIntervalSec)
                put("tls", broker.useTls)
                put("enabled", broker.enabled)
            }.toString()
            val json = fetchDaemonJson("/api/mqtt/connections", "POST", body)
            if (json != null && json.optBoolean("success", true)) {
                Result.success(true)
            } else {
                Result.failure(Exception(json?.optString("error", "Failed to add MQTT broker")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMqttConnection(id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = fetchDaemonJson("/api/mqtt/connections/$id", "DELETE")
            if (json != null && json.optBoolean("success", true)) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to delete MQTT broker"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // BYD CLOUD
    // ==========================================

    private fun fetchBydCloudConfigured(): Boolean {
        val json = fetchDaemonJson("/api/bydcloud/status", "GET") ?: return false
        if (!json.optBoolean("success", false)) return false
        val status = json.optJSONObject("status") ?: return false
        return status.optBoolean("configured", false)
    }

    suspend fun getBydCloudStatus(): BydCloudConfigState = withContext(Dispatchers.IO) {
        val json = fetchDaemonJson("/api/bydcloud/status", "GET")
        if (json != null && json.optBoolean("success", false)) {
            BydCloudConfigState.fromJson(json)
        } else {
            BydCloudConfigState()
        }
    }

    suspend fun saveBydCloudSetup(
        username: String,
        password: String,
        controlPin: String,
        countryCode: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("username", username)
                put("password", password)
                put("controlPin", controlPin)
                put("countryCode", countryCode)
            }.toString()
            val json = fetchDaemonJson("/api/bydcloud/setup", "POST", body)
            if (json != null && json.optBoolean("success", false)) {
                Result.success(true)
            } else {
                Result.failure(Exception(json?.optString("error", "BYD Cloud login failed")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testBydCloudCommand(action: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply { put("action", action) }.toString()
            val json = fetchDaemonJson("/api/bydcloud/test", "POST", body)
            if (json != null && json.optBoolean("success", false)) {
                Result.success(true)
            } else {
                Result.failure(Exception(json?.optString("error", "Command failed")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearBydCloud(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = fetchDaemonJson("/api/bydcloud/clear", "POST")
            if (json != null && json.optBoolean("success", false)) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to clear credentials"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // HTTP UTILS
    // ==========================================

    private fun fetchDaemonJson(
        path: String,
        method: String,
        body: String? = null,
        timeoutMs: Int = 3000
    ): JSONObject? {
        var conn: HttpURLConnection? = null
        return try {
            conn = DaemonHttpClient.open(path, method, timeoutMs, timeoutMs)
            if (body != null && (method == "POST" || method == "PUT")) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.bufferedWriter().use { it.write(body) }
            }
            val code = conn.responseCode
            if (code in 200..299) {
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                if (resp.isNotEmpty()) JSONObject(resp) else JSONObject()
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }
                if (!err.isNullOrEmpty()) JSONObject(err) else null
            }
        } catch (_: Throwable) {
            null
        } finally {
            try { conn?.disconnect() } catch (_: Throwable) {}
        }
    }
}
