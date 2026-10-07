package com.overdrive.app.ui.assistant

import android.content.Context
import com.overdrive.app.util.DaemonHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection

/**
 * Repository for OverDrive BYOK GenAI assistant daemon endpoints.
 * All HTTP operations run strictly on [Dispatchers.IO] with Proxy.NO_PROXY.
 */
open class AssistantRepository(
    private val appContext: Context,
) {
    companion object {
        private const val CONNECT_TIMEOUT_MS = 2_000
        private const val READ_TIMEOUT_MS = 5_000
        private const val CHAT_READ_TIMEOUT_MS = 60_000
    }

    open suspend fun fetchStatus(): GenAiStatus? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = DaemonHttpClient.open("/api/genai/status", "GET", CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS)
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                parseStatusJson(JSONObject(body))
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        } finally {
            try { conn?.disconnect() } catch (_: Throwable) {}
        }
    }

    open suspend fun saveConfig(payload: JSONObject): GenAiStatus? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = DaemonHttpClient.open("/api/genai/config", "POST", CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS)
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                parseStatusJson(JSONObject(body))
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        } finally {
            try { conn?.disconnect() } catch (_: Throwable) {}
        }
    }

    open suspend fun testProvider(): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = DaemonHttpClient.open("/api/genai/test", "POST", CONNECT_TIMEOUT_MS, 15_000)
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write("{}".toByteArray(Charsets.UTF_8)) }
            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = body.takeIf { it.isNotBlank() }?.let { JSONObject(it) }
            if (status in 200..299 && json?.optBoolean("success", true) != false) {
                Pair(true, null)
            } else {
                Pair(false, json?.optString("error", "HTTP $status") ?: "HTTP $status")
            }
        } catch (t: Throwable) {
            Pair(false, t.message ?: "Connection failed")
        } finally {
            try { conn?.disconnect() } catch (_: Throwable) {}
        }
    }

    open suspend fun sendChatMessage(prompt: String, mode: String = "general"): String? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = DaemonHttpClient.open("/api/genai/chat", "POST", CONNECT_TIMEOUT_MS, CHAT_READ_TIMEOUT_MS)
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            val payload = JSONObject().apply {
                put("message", prompt)
                put("mode", mode)
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                json.optString("response", "").ifEmpty {
                    json.optString("text", "")
                }
            } else {
                val errStream = conn.errorStream?.bufferedReader()?.use { it.readText() }
                val errJson = errStream?.let { runCatching { JSONObject(it) }.getOrNull() }
                throw RuntimeException(errJson?.optString("error") ?: "HTTP ${conn.responseCode}")
            }
        } catch (t: Throwable) {
            throw t
        } finally {
            try { conn?.disconnect() } catch (_: Throwable) {}
        }
    }

    open suspend fun fetchRoutines(): List<String> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = DaemonHttpClient.open("/api/genai/routines", "GET", CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS)
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val arr = json.optJSONArray("suggestions") ?: JSONArray()
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    arr.optString(i, "")?.takeIf { it.isNotEmpty() }?.let { list.add(it) }
                }
                list
            } else {
                emptyList()
            }
        } catch (_: Throwable) {
            emptyList()
        } finally {
            try { conn?.disconnect() } catch (_: Throwable) {}
        }
    }

    open suspend fun fetchIncidentPacks(): List<String> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = DaemonHttpClient.open("/api/genai/incidents", "GET", CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS)
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val arr = json.optJSONArray("packs") ?: JSONArray()
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    arr.optString(i, "")?.takeIf { it.isNotEmpty() }?.let { list.add(it) }
                }
                list
            } else {
                emptyList()
            }
        } catch (_: Throwable) {
            emptyList()
        } finally {
            try { conn?.disconnect() } catch (_: Throwable) {}
        }
    }

    private fun parseStatusJson(json: JSONObject): GenAiStatus {
        return GenAiStatus(
            enabled = json.optBoolean("enabled", false),
            configured = json.optBoolean("configured", false),
            provider = json.optString("provider", "openai"),
            model = json.optString("model", ""),
            baseUrl = json.optString("baseUrl", ""),
            maxOutputTokens = json.optInt("maxOutputTokens", 1200),
            apiKeyConfigured = json.optBoolean("apiKeyConfigured", false),
            realtimeModel = json.optString("realtimeModel", ""),
            nativeRealtimeAudioAvailable = json.optBoolean("nativeRealtimeAudioAvailable", false),
            availableWhileParked = json.optBoolean("availableWhileParked", false),
            transportActive = json.optBoolean("transportActive", false),
            lastNetworkRoute = json.optString("lastNetworkRoute", ""),
            proxyExpected = json.optBoolean("proxyExpected", false),
            activeRequests = json.optInt("activeRequests", 0),
            routineLearningEnabled = json.optBoolean("routineLearningEnabled", false),
        )
    }
}
