package com.overdrive.app.ui.keymapping

import org.json.JSONArray
import org.json.JSONObject

enum class KeyMappingTab {
    BINDINGS,
    ADD
}

data class KnownButton(
    val code: Int,
    val name: String,
    val isLongPressOnly: Boolean = false,
    val allowedPressTypes: List<String> = listOf("single", "double")
)

data class CuratedActionDef(
    val id: String,
    val name: String,
    val kind: String, // "vehicle", "catalog", "api", "manualClip"
    val key: String? = null,
    val sub: String? = null,
    val method: String = "POST",
    val path: String? = null,
    val body: String? = null,
    val payloads: List<Pair<String, String>> = emptyList() // value -> display label
)

data class AppInfo(
    val packageName: String,
    val label: String
)

object KeyMappingConstants {
    val KNOWN_BUTTONS = listOf(
        KnownButton(87, "Next Track (Steering Wheel)", false, listOf("single", "double")),
        KnownButton(88, "Previous Track (Steering Wheel)", false, listOf("single", "double")),
        KnownButton(289, "Mode (Steering Wheel)", false, listOf("single", "double")),
        KnownButton(291, "Volume Up (Steering Wheel)", false, listOf("single")),
        KnownButton(292, "Volume Down (Steering Wheel)", false, listOf("single")),
        KnownButton(293, "Mute (Steering Wheel)", false, listOf("single", "double")),
        KnownButton(294, "Surround 360 Camera", false, listOf("single", "double")),
        KnownButton(304, "Voice Assistant (Steering Wheel)", false, listOf("single", "double")),
        KnownButton(305, "Rotate Screen", false, listOf("single", "double")),
        KnownButton(313, "Phone / Call Button", false, listOf("single", "double")),
        KnownButton(317, "Center Console Power", false, listOf("single", "double")),
        KnownButton(302, "Next Track (Long Press)", true, listOf("single")),
        KnownButton(303, "Previous Track (Long Press)", true, listOf("single")),
        KnownButton(306, "Rotate Screen (Long Press)", true, listOf("single")),
        KnownButton(312, "Voice Assistant (Long Press)", true, listOf("single"))
    )

    val CURATED_ACTIONS = listOf(
        CuratedActionDef("lock", "Lock Vehicle Doors", "vehicle", key = "lock"),
        CuratedActionDef("unlock", "Unlock Vehicle Doors", "vehicle", key = "unlock"),
        CuratedActionDef("flash", "Flash Lights", "vehicle", key = "flash"),
        CuratedActionDef("find_car", "Find Car (Horn & Flash)", "vehicle", key = "find_car"),
        CuratedActionDef("windows_all", "Windows (All Doors)", "catalog", key = "windows_all", payloads = listOf(
            "OPEN" to "Open All",
            "CLOSE" to "Close All",
            "STOP" to "Stop"
        )),
        CuratedActionDef("tailgate", "Electric Trunk / Tailgate", "catalog", key = "tailgate", payloads = listOf(
            "OPEN" to "Open Trunk",
            "CLOSE" to "Close Trunk",
            "STOP" to "Stop"
        )),
        CuratedActionDef("sunroof", "Panoramic Sunroof", "catalog", key = "sunroof", payloads = listOf(
            "OPEN" to "Open Sunroof",
            "CLOSE" to "Close Sunroof",
            "STOP" to "Stop"
        )),
        CuratedActionDef("sunshade", "Sunroof Sunshade", "catalog", key = "sunshade", payloads = listOf(
            "OPEN" to "Open Sunshade",
            "CLOSE" to "Close Sunshade",
            "STOP" to "Stop"
        )),
        CuratedActionDef("climate", "A/C Climate Master", "catalog", key = "climate", sub = "mode", payloads = listOf(
            "auto" to "Turn On (Auto)",
            "off" to "Turn Off"
        )),
        CuratedActionDef("defrost_front", "Front Windshield Defrost", "api", path = "/api/vehicle/climate", body = "{\"action\":\"defrost_front_\${v}\"}", payloads = listOf(
            "on" to "Turn On",
            "off" to "Turn Off"
        )),
        CuratedActionDef("defrost_rear", "Rear Window Defrost & Mirrors", "api", path = "/api/vehicle/climate", body = "{\"action\":\"defrost_rear_\${v}\"}", payloads = listOf(
            "on" to "Turn On",
            "off" to "Turn Off"
        )),
        CuratedActionDef("recirculation", "Air Recirculation", "api", path = "/api/vehicle/climate", body = "{\"action\":\"recirculate_\${v}\"}", payloads = listOf(
            "on" to "Recirculate Inside",
            "off" to "Fresh Air"
        )),
        CuratedActionDef("steering_heat", "Steering Wheel Heating", "api", path = "/api/vehicle/climate", body = "{\"action\":\"steering_heat_\${v}\"}", payloads = listOf(
            "on" to "Turn On",
            "off" to "Turn Off"
        )),
        CuratedActionDef("seat_heat_driver", "Driver Seat Heating", "catalog", key = "seat_heat_driver", payloads = listOf(
            "off" to "Turn Off",
            "low" to "Low Heat",
            "high" to "High Heat"
        )),
        CuratedActionDef("seat_heat_passenger", "Passenger Seat Heating", "catalog", key = "seat_heat_passenger", payloads = listOf(
            "off" to "Turn Off",
            "low" to "Low Heat",
            "high" to "High Heat"
        )),
        CuratedActionDef("child_lock", "Rear Child Safety Lock", "catalog", key = "child_lock", payloads = listOf(
            "1" to "Enable Lock",
            "0" to "Disable Lock"
        )),
        CuratedActionDef("wireless_charging", "Wireless Phone Charging", "catalog", key = "wireless_charging", payloads = listOf(
            "1" to "Enable Charger",
            "0" to "Disable Charger"
        )),
        CuratedActionDef("drive_mode", "Drive Mode Selection", "catalog", key = "drive_mode", payloads = listOf(
            "normal" to "Normal Mode",
            "eco" to "Eco Mode",
            "sport" to "Sport Mode"
        )),
        CuratedActionDef("powertrain_mode", "Powertrain Mode (EV / HEV)", "catalog", key = "powertrain_mode", payloads = listOf(
            "ev" to "Pure EV",
            "hev" to "HEV Hybrid"
        )),
        CuratedActionDef("regen_level", "Brake Energy Regeneration", "catalog", key = "regen_level", payloads = listOf(
            "standard" to "Standard Regen",
            "high" to "High / One-Pedal",
            "toggle" to "Toggle Mode"
        )),
        CuratedActionDef("steering_mode", "Steering Assist Feel", "catalog", key = "steering_mode", payloads = listOf(
            "comfort" to "Comfort (Light)",
            "sport" to "Sport (Firm)",
            "toggle" to "Toggle Mode"
        )),
        CuratedActionDef("drl", "Daytime Running Lights (DRL)", "catalog", key = "drl", payloads = listOf(
            "on" to "Turn On",
            "off" to "Turn Off",
            "toggle" to "Toggle"
        )),
        CuratedActionDef("hazard", "Hazard Warning Lights", "catalog", key = "hazard", payloads = listOf(
            "on" to "Turn On",
            "off" to "Turn Off",
            "toggle" to "Toggle"
        )),
        CuratedActionDef("mirror_fold", "Side Rearview Mirrors Fold", "catalog", key = "mirror_fold", payloads = listOf(
            "on" to "Fold Mirrors",
            "off" to "Unfold Mirrors",
            "toggle" to "Toggle Fold"
        )),
        CuratedActionDef("manual_clip", "Dashcam Instant Replay Clip", "manualClip")
    )
}

data class KeyAction(
    val kind: String, // "catalog", "vehicle", "api", "openApp", "manualClip", "shell", "sequence", "radio", "automation", "actionGroup"
    val key: String? = null,
    val sub: String? = null,
    val payload: String? = null,
    val action: String? = null,
    val id: String? = null,
    val method: String? = null,
    val path: String? = null,
    val body: String? = null,
    val cmd: String? = null,
    val packageName: String? = null,
    val label: String? = null,
    val split: Boolean = false,
    val beforeSeconds: Int? = null,
    val afterSeconds: Int? = null,
    val steps: List<KeyAction> = emptyList(),
    val rawJson: JSONObject? = null
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("kind", kind)
        key?.let { obj.put("key", it) }
        sub?.let { obj.put("sub", it) }
        payload?.let { obj.put("payload", it) }
        action?.let { obj.put("action", it) }
        id?.let { obj.put("id", it) }
        method?.let { obj.put("method", it) }
        path?.let { obj.put("path", it) }
        body?.let { obj.put("body", it) }
        cmd?.let { obj.put("cmd", it) }
        packageName?.let { obj.put("package", it) }
        label?.let { obj.put("label", it) }
        if (split) obj.put("split", true)
        beforeSeconds?.let { obj.put("beforeSeconds", it) }
        afterSeconds?.let { obj.put("afterSeconds", it) }
        if (steps.isNotEmpty()) {
            val arr = JSONArray()
            steps.forEach { arr.put(it.toJson()) }
            obj.put("steps", arr)
        }
        return obj
    }

    val displaySummary: String
        get() {
            return when (kind) {
                "vehicle" -> {
                    val actName = action?.replace("_", " ")?.capitalize() ?: "Command"
                    "Vehicle: $actName"
                }
                "catalog" -> {
                    val def = KeyMappingConstants.CURATED_ACTIONS.find { it.key == key }
                    val actionName = def?.name ?: (key?.replace("_", " ")?.capitalize() ?: "Action")
                    val pName = def?.payloads?.find { it.first == payload }?.second ?: payload
                    if (!pName.isNullOrBlank()) "$actionName ($pName)" else actionName
                }
                "openApp" -> {
                    val app = label?.ifBlank { null } ?: packageName ?: "App"
                    val s = if (split) " (Split Screen)" else ""
                    "Open $app$s"
                }
                "manualClip" -> "Instant Replay (${beforeSeconds ?: 30}s before, ${afterSeconds ?: 0}s after)"
                "shell" -> "Shell: ${cmd ?: ""}"
                "sequence" -> {
                    if (steps.isEmpty()) "Empty Sequence"
                    else steps.joinToString(" → ") { it.displaySummary }
                }
                "automation" -> "Run Automation: ${label ?: id ?: ""}"
                "actionGroup" -> "Run Action Group: ${label ?: id ?: ""}"
                "api" -> {
                    val def = KeyMappingConstants.CURATED_ACTIONS.find { it.id == id }
                    val name = def?.name ?: (id ?: path ?: "API Action")
                    val pName = def?.payloads?.find { it.first == payload }?.second ?: payload
                    if (!pName.isNullOrBlank()) "$name ($pName)" else name
                }
                else -> kind
            }
        }

    companion object {
        fun fromJson(json: JSONObject): KeyAction {
            val kind = json.optString("kind", "")
            val key = if (json.has("key") && !json.isNull("key")) json.optString("key") else null
            val sub = if (json.has("sub") && !json.isNull("sub")) json.optString("sub") else null
            val payload = if (json.has("payload") && !json.isNull("payload")) json.optString("payload") else null
            val action = if (json.has("action") && !json.isNull("action")) json.optString("action") else null
            val id = if (json.has("id") && !json.isNull("id")) json.optString("id") else null
            val method = if (json.has("method") && !json.isNull("method")) json.optString("method") else null
            val path = if (json.has("path") && !json.isNull("path")) json.optString("path") else null
            val body = if (json.has("body") && !json.isNull("body")) json.optString("body") else null
            val cmd = if (json.has("cmd") && !json.isNull("cmd")) json.optString("cmd") else null
            val pkg = if (json.has("package") && !json.isNull("package")) json.optString("package") else null
            val label = if (json.has("label") && !json.isNull("label")) json.optString("label") else null
            val split = json.optBoolean("split", false)
            val beforeSeconds = if (json.has("beforeSeconds") && !json.isNull("beforeSeconds")) json.optInt("beforeSeconds") else null
            val afterSeconds = if (json.has("afterSeconds") && !json.isNull("afterSeconds")) json.optInt("afterSeconds") else null

            val stepsList = mutableListOf<KeyAction>()
            val stepsArr = json.optJSONArray("steps")
            if (stepsArr != null) {
                for (i in 0 until stepsArr.length()) {
                    val sObj = stepsArr.optJSONObject(i)
                    if (sObj != null) {
                        stepsList.add(fromJson(sObj))
                    }
                }
            }

            return KeyAction(
                kind = kind,
                key = key,
                sub = sub,
                payload = payload,
                action = action,
                id = id,
                method = method,
                path = path,
                body = body,
                cmd = cmd,
                packageName = pkg,
                label = label,
                split = split,
                beforeSeconds = beforeSeconds,
                afterSeconds = afterSeconds,
                steps = stepsList,
                rawJson = json
            )
        }
    }
}

data class KeyBinding(
    val keycode: Int,
    val pressType: String = "single", // "single", "double", "long"
    val enabled: Boolean = true,
    val label: String? = null,
    val blockNativeSingle: Boolean = false,
    val action: KeyAction? = null,
    val rawJson: JSONObject? = null
) {
    val displayKeyName: String
        get() {
            val match = KeyMappingConstants.KNOWN_BUTTONS.find { it.code == keycode }
            return match?.name ?: "Keycode #$keycode"
        }

    val displaySummary: String
        get() {
            if (!label.isNullOrBlank()) return label
            return action?.displaySummary ?: "Key $keycode"
        }

    val pressTypeLabel: String
        get() {
            return when (pressType) {
                "double" -> "Double Press"
                "long" -> "Long Press"
                else -> "Single Press"
            }
        }

    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("keycode", keycode)
        obj.put("pressType", pressType)
        obj.put("enabled", enabled)
        label?.let { obj.put("label", it) }
        if (blockNativeSingle && pressType == "double") {
            obj.put("blockNativeSingle", true)
        }
        action?.let { obj.put("action", it.toJson()) }
        return obj
    }

    companion object {
        fun fromJson(json: JSONObject): KeyBinding {
            val keycode = json.optInt("keycode", 0)
            val pressType = json.optString("pressType", "single")
            val enabled = json.optBoolean("enabled", true)
            val label = if (json.has("label") && !json.isNull("label")) json.optString("label") else null
            val blockNativeSingle = json.optBoolean("blockNativeSingle", false)
            val actionObj = json.optJSONObject("action")
            val action = actionObj?.let { KeyAction.fromJson(it) }

            return KeyBinding(
                keycode = keycode,
                pressType = pressType,
                enabled = enabled,
                label = label,
                blockNativeSingle = blockNativeSingle,
                action = action,
                rawJson = json
            )
        }
    }
}

data class KeymapConfig(
    val enabled: Boolean = false,
    val allowAdvanced: Boolean = false,
    val doubleTapWindowMs: Long = 450L,
    val bindings: List<KeyBinding> = emptyList(),
    val a11yEnabled: Boolean = false,
    val a11yBound: Boolean = false,
    val a11yPending: Boolean = false,
    val restartRequired: Boolean = false,
    val fuelCapableHybrid: Boolean = false
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("enabled", enabled)
        obj.put("allowAdvanced", allowAdvanced)
        obj.put("doubleTapWindowMs", doubleTapWindowMs)
        val arr = JSONArray()
        bindings.forEach { arr.put(it.toJson()) }
        obj.put("bindings", arr)
        return obj
    }

    companion object {
        fun fromJson(json: JSONObject): KeymapConfig {
            val enabled = json.optBoolean("enabled", false)
            val allowAdvanced = json.optBoolean("allowAdvanced", false)
            val doubleTapWindowMs = json.optLong("doubleTapWindowMs", 450L)
            val a11yEnabled = json.optBoolean("a11yEnabled", false)
            val a11yBound = json.optBoolean("a11yBound", false)
            val a11yPending = json.optBoolean("a11yPending", false)
            val restartRequired = json.optBoolean("restartRequired", false)
            val fuelCapableHybrid = json.optBoolean("fuelCapableHybrid", false)

            val bindingList = mutableListOf<KeyBinding>()
            val bindingsArr = json.optJSONArray("bindings")
            if (bindingsArr != null) {
                for (i in 0 until bindingsArr.length()) {
                    val bObj = bindingsArr.optJSONObject(i)
                    if (bObj != null) {
                        bindingList.add(KeyBinding.fromJson(bObj))
                    }
                }
            }

            return KeymapConfig(
                enabled = enabled,
                allowAdvanced = allowAdvanced,
                doubleTapWindowMs = doubleTapWindowMs,
                bindings = bindingList,
                a11yEnabled = a11yEnabled,
                a11yBound = a11yBound,
                a11yPending = a11yPending,
                restartRequired = restartRequired,
                fuelCapableHybrid = fuelCapableHybrid
            )
        }
    }
}

private fun String.capitalize(): String {
    return replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
