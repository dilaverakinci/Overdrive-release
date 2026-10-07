package com.overdrive.app.ui.automations

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AutomationsModelsAndParsingTest {

    @Test
    fun testAutomationItemParsing() {
        val jsonStr = """
            {
                "name": "Auto Mirror Fold on Lock",
                "mode": "automatic",
                "trigger": {
                    "type": "state_change",
                    "entity": "door_lock",
                    "to": "locked"
                },
                "conditions": [
                    {
                        "type": "gear",
                        "equals": "P"
                    }
                ],
                "actions": [
                    {
                        "type": "mirror_fold",
                        "state": true
                    }
                ],
                "elseActions": [
                    {
                        "type": "notification",
                        "message": "Car not in Park"
                    }
                ],
                "stats": {
                    "triggerCount": 14,
                    "lastTriggered": 1728345600000
                }
            }
        """.trimIndent()

        val item = AutomationItem.fromJson("rule-mirror-lock", JSONObject(jsonStr))

        assertEquals("rule-mirror-lock", item.id)
        assertEquals("Auto Mirror Fold on Lock", item.displayName)
        assertEquals("automatic", item.mode)
        assertTrue(item.isRunningEnabled)
        assertFalse(item.isManualOnly)
        assertFalse(item.isDisabled)

        assertEquals(true, item.triggerSummary.isNotEmpty())
        assertEquals(true, item.conditionSummary.isNotEmpty())
        assertEquals("Mirror fold", item.actionsSummary)
        assertEquals("Notification", item.elseActionsSummary)

        assertEquals(14, item.triggerCount)
        assertEquals(1728345600000L, item.lastTriggered)
    }

    @Test
    fun testAutomationItemManualAndDisabledModes() {
        val manualJson = JSONObject().apply {
            put("name", "Manual Cabin Precondition")
            put("mode", "manual")
        }
        val manualItem = AutomationItem.fromJson("rule-manual", manualJson)
        assertTrue(manualItem.isManualOnly)
        assertFalse(manualItem.isRunningEnabled)

        val disabledJson = JSONObject().apply {
            put("name", "Disabled Rule")
            put("mode", "disabled")
        }
        val disabledItem = AutomationItem.fromJson("rule-disabled", disabledJson)
        assertTrue(disabledItem.isDisabled)
        assertFalse(disabledItem.isRunningEnabled)
    }

    @Test
    fun testActionGroupItemParsing() {
        val groupJson = JSONObject().apply {
            put("name", "Welcome Sequence")
            put("actions", JSONArray().apply {
                put(JSONObject().apply { put("type", "mirror_unfold") })
                put(JSONObject().apply { put("type", "seat_recall"); put("slot", 1) })
                put(JSONObject().apply { put("type", "headlight"); put("state", "auto") })
            })
        }

        val group = ActionGroupItem.fromJson("grp-welcome", groupJson)
        assertEquals("grp-welcome", group.id)
        assertEquals("Welcome Sequence", group.name)
        assertEquals(3, group.actionCount)
        assertTrue(group.actionsSummary.contains("Mirror unfold"))
        assertTrue(group.actionsSummary.contains("Seat recall"))
    }

    @Test
    fun testAutomationSettingsItemParsing() {
        val settingsJson = JSONObject().apply {
            put("allowShell", true)
            put("drivingSafety", JSONObject().apply {
                put("doorLocks", true)
                put("trunk", true)
                put("mirrorFold", false)
                put("positioning", true)
                put("headlightOff", true)
                put("displayBrightness", false)
                put("displayPower", true)
                put("screenMedia", true)
            })
        }

        val settings = AutomationSettingsItem.fromJson(settingsJson)
        assertTrue(settings.allowShell)
        assertTrue(settings.doorLocksGuard)
        assertTrue(settings.trunkGuard)
        assertFalse(settings.mirrorFoldGuard)
        assertTrue(settings.positioningGuard)
        assertTrue(settings.headlightOffGuard)
        assertFalse(settings.displayBrightnessGuard)
        assertTrue(settings.displayPowerGuard)
        assertTrue(settings.screenMediaGuard)
    }

    @Test
    fun testAutomationUiStateDefaults() {
        val state = AutomationsUiState()
        assertEquals(AutomationsTab.RULES, state.activeTab)
        assertEquals(AutomationSortMode.DEFAULT, state.sortMode)
        assertTrue(state.automations.isEmpty())
        assertTrue(state.actionGroups.isEmpty())
        assertNull(state.testRunStatus)
        assertNull(state.errorMessage)
    }
}
