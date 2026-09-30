package com.overdrive.app.ui.automations

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.overdrive.app.automation.ActionGroups
import com.overdrive.app.automation.Automation
import com.overdrive.app.automation.Automations
import com.overdrive.app.automation.condition.BydEvent
import com.overdrive.app.automation.condition.EventData
import com.overdrive.app.byd.routing.DrivingSafetyGuard
import com.overdrive.app.config.UnifiedConfigManager
import com.overdrive.app.ui.component.OverdriveComposeContainer
import com.overdrive.app.ui.theme.OverdriveTheme
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

/**
 * 100% Jetpack Compose Native Fragment for Automations & Driving Safety.
 * Completely replaces legacy WebViewFragment (/automations).
 */
class AutomationsComposeFragment : Fragment() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "AutomationsWorker").apply { isDaemon = true }
    }

    private var uiState by mutableStateOf(AutomationsUiState())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return OverdriveComposeContainer(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OverdriveTheme {
                    AutomationsScreen(
                        state = uiState,
                        onTabSelected = { tab ->
                            uiState = uiState.copy(selectedTab = tab)
                        },
                        onToggleAutomation = { id, enabled ->
                            toggleAutomation(id, enabled)
                        },
                        onSetMode = { id, mode ->
                            setAutomationMode(id, mode)
                        },
                        onTestRun = { id ->
                            testRunAutomation(id)
                        },
                        onDeleteRequest = { id ->
                            uiState = uiState.copy(deleteConfirmId = id)
                        },
                        onDeleteConfirm = { id ->
                            deleteAutomation(id)
                        },
                        onDeleteDismiss = {
                            uiState = uiState.copy(deleteConfirmId = null)
                        },
                        onSaveAutomation = { id, name, mode, triggerType, actionType, delay ->
                            saveAutomation(id, name, mode, triggerType, actionType, delay)
                        },
                        onToggleSafetyGuard = { key, enabled ->
                            toggleSafetyGuard(key, enabled)
                        },
                        onToggleAllowShell = { enabled ->
                            toggleAllowShell(enabled)
                        },
                        onRunActionGroup = { id ->
                            runActionGroup(id)
                        },
                        onExportBackup = {
                            exportBackup()
                        },
                        onImportBackupClick = {
                            showFeedback("Yedekten içe aktarma panodaki JSON üzerinden hazırlanıyor")
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
    }

    override fun onDestroy() {
        worker.shutdown()
        super.onDestroy()
    }

    private fun loadData() {
        uiState = uiState.copy(isLoading = true)
        worker.execute {
            try {
                // 1. Load automations
                val automationsMap = Automations.toJson()
                val items = mutableListOf<AutomationItem>()
                val keys = automationsMap.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val obj = automationsMap.optJSONObject(key) ?: continue

                    val name = obj.optString("name", "")
                    val disabled = obj.optBoolean("disabled", false)
                    val manualOnly = obj.optBoolean("manualOnly", false)
                    val mode = when {
                        disabled && !manualOnly -> "disabled"
                        manualOnly -> "manual"
                        else -> "automatic"
                    }

                    // Format triggers summary
                    val triggersArr = obj.optJSONArray("triggers")
                    val triggerSummary = if (triggersArr != null && triggersArr.length() > 0) {
                        val firstTrig = triggersArr.optJSONObject(0)
                        val type = firstTrig?.optString("type", "") ?: ""
                        val keyVal = firstTrig?.optString("key", "") ?: ""
                        "$type / $keyVal"
                    } else {
                        "Herhangi bir olay"
                    }

                    // Format actions summary
                    val actionsArr = obj.optJSONArray("actions")
                    val actionSummary = if (actionsArr != null && actionsArr.length() > 0) {
                        val firstAct = actionsArr.optJSONObject(0)
                        val type = firstAct?.optString("type", "") ?: ""
                        if (actionsArr.length() > 1) "$type (+${actionsArr.length() - 1} eylem)" else type
                    } else {
                        "Eylem yok"
                    }

                    items.add(
                        AutomationItem(
                            id = key,
                            name = name.ifEmpty { "Kural #$key" },
                            mode = mode,
                            isEnabled = !disabled,
                            triggerText = triggerSummary,
                            actionsText = actionSummary,
                            delaySeconds = obj.optInt("delay", 0),
                            lastTriggered = obj.optLong("lastTriggered", 0L),
                            triggerCount = obj.optLong("triggerCount", 0L)
                        )
                    )
                }

                // 2. Load shell permission
                UnifiedConfigManager.forceReload()
                val allowShell = UnifiedConfigManager.isAutomationShellAllowed()

                // 3. Load safety guards
                val guardsObj = DrivingSafetyGuard.getGuardSettings()
                val guardsMap = mutableMapOf<String, Boolean>()
                val gKeys = guardsObj.keys()
                while (gKeys.hasNext()) {
                    val gk = gKeys.next()
                    guardsMap[gk] = guardsObj.optBoolean(gk, true)
                }

                // 4. Load action groups
                val groupsObj = ActionGroups.toJson()
                val groupItems = mutableListOf<ActionGroupItem>()
                val grpKeys = groupsObj.keys()
                while (grpKeys.hasNext()) {
                    val gid = grpKeys.next()
                    val gobj = groupsObj.optJSONObject(gid) ?: continue
                    val gName = gobj.optString("name", "Grup")
                    val gActs = gobj.optJSONArray("actions")
                    groupItems.add(
                        ActionGroupItem(
                            id = gid,
                            name = gName,
                            actionsSummary = "${gActs?.length() ?: 0} eylem tanımlı",
                            actionCount = gActs?.length() ?: 0
                        )
                    )
                }

                mainHandler.post {
                    uiState = uiState.copy(
                        automations = items,
                        allowShell = allowShell,
                        safetyGuards = guardsMap,
                        actionGroups = groupItems,
                        isLoading = false
                    )
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    uiState = uiState.copy(isLoading = false)
                    showFeedback("Otomasyonlar yüklenirken bir hata oluştu")
                }
            }
        }
    }

    private fun toggleAutomation(id: String, enabled: Boolean) {
        worker.execute {
            Automations.disableAutomation(id, !enabled)
            loadData()
            mainHandler.post {
                showFeedback(if (enabled) "Otomasyon etkinleştirildi" else "Otomasyon devre dışı bırakıldı")
            }
        }
    }

    private fun setAutomationMode(id: String, mode: String) {
        worker.execute {
            Automations.setAutomationMode(id, mode)
            loadData()
        }
    }

    private fun testRunAutomation(id: String) {
        worker.execute {
            Automations.triggerExplicitActions(id, true)
            mainHandler.post {
                showFeedback("Otomasyon eylemleri çalıştırıldı")
            }
        }
    }

    private fun deleteAutomation(id: String) {
        worker.execute {
            Automations.deleteAutomation(id)
            mainHandler.post {
                uiState = uiState.copy(deleteConfirmId = null)
                showFeedback("Otomasyon silindi")
            }
            loadData()
        }
    }

    private fun saveAutomation(
        id: String?,
        name: String,
        mode: String,
        triggerType: String,
        actionType: String,
        delay: Int
    ) {
        uiState = uiState.copy(isLoading = true)
        worker.execute {
            try {
                val json = JSONObject().apply {
                    put("name", name)
                    put("delay", delay)
                    put("disabled", mode == "disabled")
                    if (mode == "manual") put("manualOnly", true)

                    // Map trigger
                    val triggersArr = JSONArray()
                    val trigObj = JSONObject().apply {
                        when (triggerType) {
                            "gear_p" -> {
                                put("type", "byd")
                                put("key", "gear")
                                put("value", "P")
                            }
                            "gear_d" -> {
                                put("type", "byd")
                                put("key", "gear")
                                put("value", "D")
                            }
                            "gear_r" -> {
                                put("type", "byd")
                                put("key", "gear")
                                put("value", "R")
                            }
                            "doors_locked" -> {
                                put("type", "byd")
                                put("key", "lock")
                                put("value", "true")
                            }
                            "doors_unlocked" -> {
                                put("type", "byd")
                                put("key", "lock")
                                put("value", "false")
                            }
                            "acc_on" -> {
                                put("type", "byd")
                                put("key", "acc")
                                put("value", "true")
                            }
                            "acc_off" -> {
                                put("type", "byd")
                                put("key", "acc")
                                put("value", "false")
                            }
                            "charging_start" -> {
                                put("type", "byd")
                                put("key", "charging")
                                put("value", "true")
                            }
                            else -> {
                                put("type", "byd")
                                put("key", "charging")
                                put("value", "false")
                            }
                        }
                    }
                    triggersArr.put(trigObj)
                    put("triggers", triggersArr)

                    // Empty conditions array
                    put("conditions", JSONArray())
                    put("conditionLogic", "AND")

                    // Map action
                    val actionsArr = JSONArray()
                    val actObj = JSONObject().apply {
                        when (actionType) {
                            "mirror_fold" -> {
                                put("type", "vehicleControl")
                                put("command", "mirror_fold")
                            }
                            "mirror_unfold" -> {
                                put("type", "vehicleControl")
                                put("command", "mirror_unfold")
                            }
                            "seat_pos_1" -> {
                                put("type", "vehicleControl")
                                put("command", "seat_position_1")
                            }
                            "seat_pos_2" -> {
                                put("type", "vehicleControl")
                                put("command", "seat_position_2")
                            }
                            "screen_off" -> {
                                put("type", "vehicleControl")
                                put("command", "screen_off")
                            }
                            "screen_on" -> {
                                put("type", "vehicleControl")
                                put("command", "screen_on")
                            }
                            "doors_lock" -> {
                                put("type", "vehicleControl")
                                put("command", "lock_doors")
                            }
                            "doors_unlock" -> {
                                put("type", "vehicleControl")
                                put("command", "unlock_doors")
                            }
                            else -> {
                                put("type", "playAudio")
                                put("sound", "welcome.mp3")
                            }
                        }
                    }
                    actionsArr.put(actObj)
                    put("actions", actionsArr)
                    put("elseActions", JSONArray())
                }

                val parsed = Automation.fromJson(json)
                if (parsed != null) {
                    Automations.updateAutomation(id, parsed)
                    mainHandler.post {
                        showFeedback("Otomasyon başarıyla kaydedildi")
                    }
                } else {
                    mainHandler.post {
                        showFeedback("Otomasyon ayrıştırılamadı, lütfen alanları kontrol edin")
                    }
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    showFeedback("Kayıt hatası: ${t.message}")
                }
            }
            loadData()
        }
    }

    private fun toggleSafetyGuard(key: String, enabled: Boolean) {
        worker.execute {
            UnifiedConfigManager.updateSection("drivingSafety", JSONObject().put(key, enabled))
            loadData()
            mainHandler.post {
                showFeedback("Sürüş güvenliği ayarı güncellendi")
            }
        }
    }

    private fun toggleAllowShell(enabled: Boolean) {
        worker.execute {
            UnifiedConfigManager.setAutomationShellAllowed(enabled)
            loadData()
            mainHandler.post {
                showFeedback(if (enabled) "Kabuk eylemleri etkinleştirildi" else "Kabuk eylemleri kapatıldı")
            }
        }
    }

    private fun runActionGroup(id: String) {
        worker.execute {
            val actions = ActionGroups.getActions(id)
            Automations.runActionList(actions)
            mainHandler.post {
                showFeedback("Eylem grubu çalıştırıldı")
            }
        }
    }

    private fun exportBackup() {
        worker.execute {
            val jsonStr = Automations.toJson().toString(2)
            mainHandler.post {
                val clip = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clip?.setPrimaryClip(ClipData.newPlainText("overdrive_automations_backup", jsonStr))
                showFeedback("Tüm otomasyonlar panoya kopyalandı!")
            }
        }
    }

    private fun showFeedback(message: String) {
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
    }
}
