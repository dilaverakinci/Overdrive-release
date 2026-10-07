package com.overdrive.app.ui.integrations

import org.json.JSONArray
import org.json.JSONObject

enum class IntegrationTab {
    OVERVIEW,
    TELEGRAM,
    ABRP,
    MQTT,
    BYD_CLOUD
}

data class IntegrationsSummary(
    val telegramConfigured: Boolean = false,
    val abrpConnected: Boolean = false,
    val mqttConnected: Boolean = false,
    val bydCloudConfigured: Boolean = false
) {
    val allConfigured: Boolean
        get() = telegramConfigured && abrpConnected && mqttConnected && bydCloudConfigured

    val activeCount: Int
        get() = (if (telegramConfigured) 1 else 0) +
                (if (abrpConnected) 1 else 0) +
                (if (mqttConnected) 1 else 0) +
                (if (bydCloudConfigured) 1 else 0)
}

data class TelegramConfigState(
    val hasToken: Boolean = false,
    val botUsername: String = "",
    val botFirstName: String = "",
    val isPaired: Boolean = false,
    val ownerChatId: Long = 0L,
    val ownerUsername: String = "",
    val ownerFirstName: String = "",
    val pinCode: String = "",
    val pinExpiresInSec: Long = 0L,
    val videoUploads: Boolean = false,
    val criticalAlerts: Boolean = true,
    val motionText: Boolean = true,
    val tyreAlerts: Boolean = true,
    val daemonAlwaysOn: Boolean = false
) {
    companion object {
        fun fromJson(json: JSONObject): TelegramConfigState {
            val configured = json.optBoolean("configured", false) || json.optBoolean("hasToken", false)
            val paired = json.optBoolean("paired", false)
            return TelegramConfigState(
                hasToken = configured,
                botUsername = json.optString("botUsername", ""),
                botFirstName = json.optString("botFirstName", ""),
                isPaired = paired,
                ownerChatId = json.optLong("ownerChatId", 0L),
                ownerUsername = json.optString("ownerUsername", ""),
                ownerFirstName = json.optString("ownerFirstName", ""),
                pinCode = json.optString("pinCode", json.optString("pin", "")),
                pinExpiresInSec = json.optLong("pinExpiresInSec", json.optLong("pin_ttl", 0L)),
                videoUploads = json.optBoolean("videoUploads", false),
                criticalAlerts = json.optBoolean("criticalAlerts", true),
                motionText = json.optBoolean("motionText", true),
                tyreAlerts = json.optBoolean("tyreAlerts", true),
                daemonAlwaysOn = json.optBoolean("daemonAlwaysOn", false)
            )
        }
    }
}

data class AbrpConfigState(
    val running: Boolean = false,
    val hasToken: Boolean = false,
    val carModel: String = "",
    val lastUploadTime: String = "",
    val socPercent: Double = 0.0,
    val powerKw: Double = 0.0,
    val speedKmh: Double = 0.0,
    val isCharging: Boolean = false
) {
    companion object {
        fun fromJson(configJson: JSONObject?, statusJson: JSONObject?): AbrpConfigState {
            val hasToken = configJson?.optString("token", "")?.isNotBlank() == true ||
                    configJson?.optBoolean("has_token", false) == true
            val carModel = configJson?.optString("car_model", "BYD ATTO 3") ?: "BYD ATTO 3"
            val running = statusJson?.optBoolean("running", false) == true ||
                    configJson?.optBoolean("enabled", false) == true

            val telemetry = statusJson?.optJSONObject("last_telemetry")
            val soc = telemetry?.optDouble("soc", 0.0) ?: 0.0
            val power = telemetry?.optDouble("power", 0.0) ?: 0.0
            val speed = telemetry?.optDouble("speed", 0.0) ?: 0.0
            val charging = telemetry?.optBoolean("is_charging", false) ?: false

            return AbrpConfigState(
                running = running,
                hasToken = hasToken,
                carModel = carModel,
                lastUploadTime = statusJson?.optString("last_upload", "") ?: "",
                socPercent = soc,
                powerKw = power,
                speedKmh = speed,
                isCharging = charging
            )
        }
    }
}

data class MqttBrokerConfig(
    val id: String = "",
    val name: String = "",
    val host: String = "",
    val port: Int = 1883,
    val username: String = "",
    val clientId: String = "overdrive",
    val topicPrefix: String = "overdrive",
    val publishIntervalSec: Int = 10,
    val useTls: Boolean = false,
    val enabled: Boolean = true,
    val connected: Boolean = false,
    val messagesSent: Long = 0L,
    val lastConnectedTime: String = ""
) {
    val displayEndpoint: String
        get() = "$host:$port"

    companion object {
        fun fromJson(json: JSONObject): MqttBrokerConfig {
            val status = json.optJSONObject("status")
            val connected = status?.optBoolean("connected", false) ?: json.optBoolean("connected", false)
            val msgCount = status?.optLong("messages_sent", 0L) ?: json.optLong("messages_sent", 0L)

            return MqttBrokerConfig(
                id = json.optString("id", ""),
                name = json.optString("name", "MQTT Broker"),
                host = json.optString("host", json.optString("broker", "")),
                port = json.optInt("port", 1883),
                username = json.optString("username", ""),
                clientId = json.optString("clientId", json.optString("client_id", "overdrive")),
                topicPrefix = json.optString("topicPrefix", json.optString("topic_prefix", "overdrive")),
                publishIntervalSec = json.optInt("publishInterval", json.optInt("publish_interval", 10)),
                useTls = json.optBoolean("useTls", json.optBoolean("tls", false)),
                enabled = json.optBoolean("enabled", true),
                connected = connected,
                messagesSent = msgCount,
                lastConnectedTime = status?.optString("last_connected", "") ?: ""
            )
        }

        fun parseList(jsonArray: JSONArray): List<MqttBrokerConfig> {
            val list = mutableListOf<MqttBrokerConfig>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                list.add(fromJson(obj))
            }
            return list
        }
    }
}

data class BydCloudConfigState(
    val isConfigured: Boolean = false,
    val isVerified: Boolean = false,
    val enabled: Boolean = false,
    val username: String = "",
    val vin: String = "",
    val countryCode: String = "GB",
    val region: String = "EU",
    val cloudDataMerge: Boolean = false
) {
    companion object {
        fun fromJson(json: JSONObject): BydCloudConfigState {
            val status = json.optJSONObject("status") ?: json
            return BydCloudConfigState(
                isConfigured = status.optBoolean("configured", false),
                isVerified = status.optBoolean("verified", false),
                enabled = status.optBoolean("enabled", false),
                username = status.optString("username", ""),
                vin = status.optString("vin", ""),
                countryCode = status.optString("countryCode", "GB"),
                region = status.optString("region", "EU"),
                cloudDataMerge = status.optBoolean("cloudDataMerge", false)
            )
        }
    }
}
