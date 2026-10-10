package com.overdrive.app.ui.keymapping

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyMappingModelsAndParsingTest {

    @Test
    fun testParseKeymapConfig() {
        val jsonStr = """
        {
            "success": true,
            "enabled": true,
            "allowAdvanced": false,
            "doubleTapWindowMs": 500,
            "bindings": [
                {
                    "keycode": 87,
                    "pressType": "double",
                    "enabled": true,
                    "label": "Open Sunroof",
                    "blockNativeSingle": true,
                    "action": {
                        "kind": "catalog",
                        "key": "sunroof",
                        "payload": "OPEN"
                    }
                },
                {
                    "keycode": 305,
                    "pressType": "single",
                    "enabled": false,
                    "action": {
                        "kind": "vehicle",
                        "action": "flash"
                    }
                }
            ],
            "a11yEnabled": true,
            "a11yBound": true,
            "a11yPending": false,
            "restartRequired": false,
            "fuelCapableHybrid": false
        }
        """.trimIndent()

        val json = JSONObject(jsonStr)
        val config = KeymapConfig.fromJson(json)

        assertTrue(config.enabled)
        assertFalse(config.allowAdvanced)
        assertEquals(500L, config.doubleTapWindowMs)
        assertTrue(config.a11yEnabled)
        assertTrue(config.a11yBound)
        assertFalse(config.a11yPending)
        assertEquals(2, config.bindings.size)

        val b1 = config.bindings[0]
        assertEquals(87, b1.keycode)
        assertEquals("double", b1.pressType)
        assertTrue(b1.enabled)
        assertTrue(b1.blockNativeSingle)
        assertEquals("Open Sunroof", b1.label)
        assertNotNull(b1.action)
        assertEquals("catalog", b1.action?.kind)
        assertEquals("sunroof", b1.action?.key)
        assertEquals("OPEN", b1.action?.payload)
        assertEquals("Next Track (Steering Wheel)", b1.displayKeyName)

        val b2 = config.bindings[1]
        assertEquals(305, b2.keycode)
        assertEquals("single", b2.pressType)
        assertFalse(b2.enabled)
        assertNotNull(b2.action)
        assertEquals("vehicle", b2.action?.kind)
        assertEquals("flash", b2.action?.action)
        assertEquals("Rotate Screen", b2.displayKeyName)
    }

    @Test
    fun testParseActionTypes() {
        // 1. Open App
        val appJson = JSONObject("""
            {
                "kind": "openApp",
                "package": "com.android.chrome",
                "label": "Chrome",
                "split": true
            }
        """.trimIndent())
        val appAction = KeyAction.fromJson(appJson)
        assertEquals("openApp", appAction.kind)
        assertEquals("com.android.chrome", appAction.packageName)
        assertEquals("Chrome", appAction.label)
        assertTrue(appAction.split)
        assertEquals("Open Chrome (Split Screen)", appAction.displaySummary)

        // 2. Manual Clip
        val clipJson = JSONObject("""
            {
                "kind": "manualClip",
                "beforeSeconds": 30,
                "afterSeconds": 15
            }
        """.trimIndent())
        val clipAction = KeyAction.fromJson(clipJson)
        assertEquals("manualClip", clipAction.kind)
        assertEquals(30, clipAction.beforeSeconds)
        assertEquals(15, clipAction.afterSeconds)
        assertEquals("Instant Replay (30s before, 15s after)", clipAction.displaySummary)

        // 3. Shell Command
        val shellJson = JSONObject("""
            {
                "kind": "shell",
                "cmd": "input keyevent 26"
            }
        """.trimIndent())
        val shellAction = KeyAction.fromJson(shellJson)
        assertEquals("shell", shellAction.kind)
        assertEquals("input keyevent 26", shellAction.cmd)
        assertEquals("Shell: input keyevent 26", shellAction.displaySummary)

        // 4. Sequence
        val seqJson = JSONObject("""
            {
                "kind": "sequence",
                "steps": [
                    { "kind": "vehicle", "action": "lock" },
                    { "kind": "vehicle", "action": "flash" }
                ]
            }
        """.trimIndent())
        val seqAction = KeyAction.fromJson(seqJson)
        assertEquals("sequence", seqAction.kind)
        assertEquals(2, seqAction.steps.size)
        assertTrue(seqAction.displaySummary.contains("Lock") && seqAction.displaySummary.contains("Flash"))
    }

    @Test
    fun testConfigSerializationRoundtrip() {
        val binding = KeyBinding(
            keycode = 289,
            pressType = "single",
            enabled = true,
            label = "Drive Mode",
            action = KeyAction(
                kind = "catalog",
                key = "drive_mode",
                payload = "sport"
            )
        )
        val config = KeymapConfig(
            enabled = true,
            allowAdvanced = true,
            doubleTapWindowMs = 600L,
            bindings = listOf(binding)
        )

        val serialized = config.toJson()
        val parsed = KeymapConfig.fromJson(serialized)

        assertTrue(parsed.enabled)
        assertTrue(parsed.allowAdvanced)
        assertEquals(600L, parsed.doubleTapWindowMs)
        assertEquals(1, parsed.bindings.size)

        val parsedBinding = parsed.bindings[0]
        assertEquals(289, parsedBinding.keycode)
        assertEquals("single", parsedBinding.pressType)
        assertTrue(parsedBinding.enabled)
        assertEquals("Drive Mode", parsedBinding.label)
        assertEquals("drive_mode", parsedBinding.action?.key)
        assertEquals("sport", parsedBinding.action?.payload)
    }

    @Test
    fun testKnownButtonsMapping() {
        val nextTrack = KeyMappingConstants.KNOWN_BUTTONS.find { it.code == 87 }
        assertNotNull(nextTrack)
        assertEquals("Next Track (Steering Wheel)", nextTrack?.name)
        assertFalse(nextTrack?.isLongPressOnly == true)

        val nextLong = KeyMappingConstants.KNOWN_BUTTONS.find { it.code == 302 }
        assertNotNull(nextLong)
        assertTrue(nextLong?.isLongPressOnly == true)
        assertEquals(listOf("single"), nextLong?.allowedPressTypes)
    }
}
