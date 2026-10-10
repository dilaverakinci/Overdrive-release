package com.overdrive.app.ui.integrations

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IntegrationsModelsAndRepositoryTest {

    @Test
    fun summaryComputesActiveCountAndAllConfiguredCorrectly() {
        val emptySummary = IntegrationsSummary()
        assertEquals(0, emptySummary.activeCount)
        assertFalse(emptySummary.allConfigured)

        val partialSummary = IntegrationsSummary(
            telegramConfigured = true,
            abrpConnected = false,
            mqttConnected = true,
            bydCloudConfigured = false
        )
        assertEquals(2, partialSummary.activeCount)
        assertFalse(partialSummary.allConfigured)

        val fullSummary = IntegrationsSummary(
            telegramConfigured = true,
            abrpConnected = true,
            mqttConnected = true,
            bydCloudConfigured = true
        )
        assertEquals(4, fullSummary.activeCount)
        assertTrue(fullSummary.allConfigured)
    }

    @Test
    fun telegramConfigStateParsesCorrectly() {
        val json = JSONObject().apply {
            put("configured", true)
            put("paired", true)
            put("botUsername", "OverDriveBot")
            put("botFirstName", "OverDrive Bot")
            put("ownerChatId", 123456789L)
            put("ownerUsername", "johndoe")
            put("ownerFirstName", "John")
            put("pinCode", "482910")
            put("pinExpiresInSec", 540L)
            put("videoUploads", true)
            put("criticalAlerts", true)
            put("motionText", false)
            put("tyreAlerts", true)
            put("daemonAlwaysOn", true)
        }

        val state = TelegramConfigState.fromJson(json)
        assertTrue(state.hasToken)
        assertTrue(state.isPaired)
        assertEquals("OverDriveBot", state.botUsername)
        assertEquals("OverDrive Bot", state.botFirstName)
        assertEquals(123456789L, state.ownerChatId)
        assertEquals("johndoe", state.ownerUsername)
        assertEquals("John", state.ownerFirstName)
        assertEquals("482910", state.pinCode)
        assertEquals(540L, state.pinExpiresInSec)
        assertTrue(state.videoUploads)
        assertTrue(state.criticalAlerts)
        assertFalse(state.motionText)
        assertTrue(state.tyreAlerts)
        assertTrue(state.daemonAlwaysOn)
    }

    @Test
    fun abrpConfigStateParsesCorrectly() {
        val configJson = JSONObject().apply {
            put("token", "abc-123-token")
            put("enabled", true)
            put("car_model", "BYD SEAL")
        }

        val telemetryJson = JSONObject().apply {
            put("soc", 78.5)
            put("power", 45.2)
            put("speed", 105.0)
            put("is_charging", false)
        }

        val statusJson = JSONObject().apply {
            put("running", true)
            put("last_upload", "2026-10-08T02:00:00Z")
            put("last_telemetry", telemetryJson)
        }

        val state = AbrpConfigState.fromJson(configJson, statusJson)
        assertTrue(state.running)
        assertTrue(state.hasToken)
        assertEquals("BYD SEAL", state.carModel)
        assertEquals("2026-10-08T02:00:00Z", state.lastUploadTime)
        assertEquals(78.5, state.socPercent, 0.01)
        assertEquals(45.2, state.powerKw, 0.01)
        assertEquals(105.0, state.speedKmh, 0.01)
        assertFalse(state.isCharging)
    }

    @Test
    fun mqttBrokerConfigParsesAndFormatsCorrectly() {
        val brokerJson = JSONObject().apply {
            put("id", "conn-uuid-1")
            put("name", "Home Assistant")
            put("broker", "192.168.1.50")
            put("port", 1883)
            put("username", "homeassistant")
            put("client_id", "byd_atto3")
            put("topic_prefix", "overdrive/car")
            put("publish_interval", 5)
            put("tls", false)
            put("enabled", true)
            put("status", JSONObject().apply {
                put("connected", true)
                put("messages_sent", 1205L)
                put("last_connected", "2026-10-08T01:30:00Z")
            })
        }

        val config = MqttBrokerConfig.fromJson(brokerJson)
        assertEquals("conn-uuid-1", config.id)
        assertEquals("Home Assistant", config.name)
        assertEquals("192.168.1.50", config.host)
        assertEquals(1883, config.port)
        assertEquals("192.168.1.50:1883", config.displayEndpoint)
        assertEquals("homeassistant", config.username)
        assertEquals("byd_atto3", config.clientId)
        assertEquals("overdrive/car", config.topicPrefix)
        assertEquals(5, config.publishIntervalSec)
        assertFalse(config.useTls)
        assertTrue(config.enabled)
        assertTrue(config.connected)
        assertEquals(1205L, config.messagesSent)
        assertEquals("2026-10-08T01:30:00Z", config.lastConnectedTime)

        val arr = JSONArray().apply {
            put(brokerJson)
        }
        val list = MqttBrokerConfig.parseList(arr)
        assertEquals(1, list.size)
        assertEquals("conn-uuid-1", list[0].id)
    }

    @Test
    fun bydCloudConfigStateParsesCorrectly() {
        val json = JSONObject().apply {
            put("configured", true)
            put("verified", true)
            put("enabled", true)
            put("username", "driver@example.com")
            put("vin", "LC0BYDATTO300123")
            put("countryCode", "DE")
            put("region", "EU")
            put("cloudDataMerge", true)
        }

        val state = BydCloudConfigState.fromJson(json)
        assertTrue(state.isConfigured)
        assertTrue(state.isVerified)
        assertTrue(state.enabled)
        assertEquals("driver@example.com", state.username)
        assertEquals("LC0BYDATTO300123", state.vin)
        assertEquals("DE", state.countryCode)
        assertEquals("EU", state.region)
        assertTrue(state.cloudDataMerge)
    }
}
