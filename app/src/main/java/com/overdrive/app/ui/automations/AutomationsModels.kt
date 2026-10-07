package com.overdrive.app.ui.automations

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

enum class AutomationsTab {
    RULES,
    GROUPS,
    SAFETY,
    SETTINGS
}

enum class AutomationSortMode(val label: String) {
    DEFAULT("Default order"),
    NAME_AZ("Name A → Z"),
    NAME_ZA("Name Z → A"),
    RECENT("Recently run"),
    RUNS("Most runs"),
    ENABLED_FIRST("Enabled first"),
    DISABLED_FIRST("Disabled first")
}

data class AutomationItem(
    val id: String,
    val name: String,
    val mode: String, // "automatic", "manual", "disabled"
    val isDisabled: Boolean,
    val isManualOnly: Boolean,
    val delaySeconds: Int,
    val conditionLogic: String, // "AND", "OR"
    val triggerSummary: String,
    val conditionSummary: String,
    val actionsSummary: String,
    val elseActionsSummary: String?,
    val triggerCount: Long,
    val lastTriggered: Long,
    val rawJson: JSONObject
) {
    val displayName: String
        get() = if (name.isNotBlank()) name else triggerSummary.ifBlank { "Automation $id" }

    val isRunningEnabled: Boolean
        get() = mode == "automatic"

    companion object {
        fun fromJson(id: String, json: JSONObject): AutomationItem {
            val name = json.optString("name", "")
            val rawMode = json.optString("mode", "")
            val disabled = json.optBoolean("disabled", rawMode == "disabled")
            val manualOnly = json.optBoolean("manualOnly", rawMode == "manual")
            val mode = when {
                rawMode.isNotBlank() -> rawMode
                manualOnly -> "manual"
                disabled -> "disabled"
                else -> "automatic"
            }
            val delay = json.optInt("delay", 0)
            val conditionLogic = json.optString("conditionLogic", "AND").uppercase(Locale.ROOT)

            // Triggers (array or single object)
            val triggersArr = json.optJSONArray("triggers") ?: JSONArray().apply {
                json.optJSONObject("trigger")?.let { put(it) }
            }
            val triggerParts = mutableListOf<String>()
            for (i in 0 until triggersArr.length()) {
                val t = triggersArr.optJSONObject(i) ?: continue
                triggerParts.add(formatTrigger(t))
            }
            val triggerSummary = if (triggerParts.isNotEmpty()) {
                triggerParts.joinToString(" • ")
            } else {
                "Event trigger"
            }

            // Conditions (array or single object)
            val condsArr = json.optJSONArray("conditions") ?: JSONArray().apply {
                json.optJSONObject("condition")?.let { put(it) }
            }
            val condParts = mutableListOf<String>()
            for (i in 0 until condsArr.length()) {
                val c = condsArr.optJSONObject(i) ?: continue
                condParts.add(formatCondition(c))
            }
            val conditionSummary = condParts.joinToString(" $conditionLogic ")

            // Actions (array or single object)
            val actionsArr = json.optJSONArray("actions") ?: JSONArray().apply {
                json.optJSONObject("action")?.let { put(it) }
            }
            val actionParts = mutableListOf<String>()
            for (i in 0 until actionsArr.length()) {
                val a = actionsArr.optJSONObject(i) ?: continue
                actionParts.add(formatAction(a))
            }
            val actionsSummary = if (actionParts.isNotEmpty()) {
                actionParts.joinToString(" → ")
            } else {
                "No actions"
            }

            // Else Actions
            val elseArr = json.optJSONArray("elseActions") ?: JSONArray().apply {
                json.optJSONObject("elseAction")?.let { put(it) }
            }
            val elseSummary = if (elseArr.length() > 0) {
                val elseParts = mutableListOf<String>()
                for (i in 0 until elseArr.length()) {
                    val a = elseArr.optJSONObject(i) ?: continue
                    elseParts.add(formatAction(a))
                }
                elseParts.joinToString(" → ")
            } else null

            val statsObj = json.optJSONObject("stats")
            val triggerCount = statsObj?.optLong("triggerCount", 0L) ?: json.optLong("triggerCount", 0L)
            val lastTriggered = statsObj?.optLong("lastTriggered", 0L) ?: json.optLong("lastTriggered", 0L)

            return AutomationItem(
                id = id,
                name = name,
                mode = mode,
                isDisabled = disabled,
                isManualOnly = manualOnly,
                delaySeconds = delay,
                conditionLogic = conditionLogic,
                triggerSummary = triggerSummary,
                conditionSummary = conditionSummary,
                actionsSummary = actionsSummary,
                elseActionsSummary = elseSummary,
                triggerCount = triggerCount,
                lastTriggered = lastTriggered,
                rawJson = json
            )
        }

        private fun formatTrigger(json: JSONObject): String {
            val type = json.optString("type", "")
            val event = json.optString("event", json.optString("key", type))
            val value = json.opt("value")
            return when {
                value != null -> "$event is $value"
                event.isNotBlank() -> event.replace('_', ' ').replaceFirstChar { it.uppercase() }
                else -> type.replaceFirstChar { it.uppercase() }
            }
        }

        private fun formatCondition(json: JSONObject): String {
            val key = json.optString("key", json.optString("type", "condition"))
            val op = json.optString("operator", "==")
            val value = json.opt("value")?.toString() ?: ""
            return "$key $op $value"
        }

        private fun formatAction(json: JSONObject): String {
            val type = json.optString("type", "action")
            val name = json.optString("name", "")
            if (name.isNotBlank()) return name

            val control = json.optString("control", "")
            if (control.isNotBlank()) {
                return control.replace('_', ' ').replaceFirstChar { it.uppercase() }
            }

            val group = json.optString("groupName", json.optString("groupId", ""))
            if (group.isNotBlank()) return "Group: $group"

            val sound = json.optString("sound", "")
            if (sound.isNotBlank()) return "Play: $sound"

            return type.replace('_', ' ').replaceFirstChar { it.uppercase() }
        }
    }
}

data class ActionGroupItem(
    val id: String,
    val name: String,
    val actionCount: Int,
    val actionsSummary: String,
    val rawJson: JSONObject
) {
    companion object {
        fun fromJson(id: String, json: JSONObject): ActionGroupItem {
            val name = json.optString("name", "Action Group")
            val actionsArr = json.optJSONArray("actions") ?: JSONArray()
            val actionParts = mutableListOf<String>()
            for (i in 0 until actionsArr.length()) {
                val a = actionsArr.optJSONObject(i) ?: continue
                val type = a.optString("type", a.optString("control", "action"))
                actionParts.add(type.replace('_', ' ').replaceFirstChar { it.uppercase() })
            }
            val summary = if (actionParts.isNotEmpty()) actionParts.joinToString(" → ") else "0 actions"
            return ActionGroupItem(
                id = id,
                name = name,
                actionCount = actionsArr.length(),
                actionsSummary = summary,
                rawJson = json
            )
        }
    }
}

data class AutomationSettingsItem(
    val allowShell: Boolean = false,
    val doorLocksGuard: Boolean = true,
    val trunkGuard: Boolean = true,
    val mirrorFoldGuard: Boolean = true,
    val positioningGuard: Boolean = true,
    val headlightOffGuard: Boolean = true,
    val displayBrightnessGuard: Boolean = true,
    val displayPowerGuard: Boolean = true,
    val screenMediaGuard: Boolean = true
) {
    companion object {
        fun fromJson(json: JSONObject): AutomationSettingsItem {
            val allowShell = json.optBoolean("allowShell", false)
            val safety = json.optJSONObject("drivingSafety") ?: JSONObject()
            return AutomationSettingsItem(
                allowShell = allowShell,
                doorLocksGuard = safety.optBoolean("doorLocks", true),
                trunkGuard = safety.optBoolean("trunk", true),
                mirrorFoldGuard = safety.optBoolean("mirrorFold", true),
                positioningGuard = safety.optBoolean("positioning", true),
                headlightOffGuard = safety.optBoolean("headlightOff", true),
                displayBrightnessGuard = safety.optBoolean("displayBrightness", true),
                displayPowerGuard = safety.optBoolean("displayPower", true),
                screenMediaGuard = safety.optBoolean("screenMedia", true)
            )
        }
    }
}

data class AutomationsUiState(
    val isLoading: Boolean = false,
    val activeTab: AutomationsTab = AutomationsTab.RULES,
    val sortMode: AutomationSortMode = AutomationSortMode.DEFAULT,
    val automations: List<AutomationItem> = emptyList(),
    val actionGroups: List<ActionGroupItem> = emptyList(),
    val settings: AutomationSettingsItem = AutomationSettingsItem(),
    val testRunStatus: String? = null,
    val errorMessage: String? = null
)
