package com.overdrive.app.ui.keymapping

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.overdrive.app.launcher.AppLauncher
import com.overdrive.app.config.UnifiedConfigManager
import com.overdrive.app.services.KeepAliveAccessibilityService
import com.overdrive.app.services.KeyMapDispatcher
import com.overdrive.app.ui.component.OverdriveComposeContainer
import com.overdrive.app.ui.theme.OverdriveTheme
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

/**
 * 100% Jetpack Compose Native Fragment for Physical Key Mapping.
 * Completely replaces legacy WebViewFragment (/key-mapping).
 */
class KeyMappingComposeFragment : Fragment() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "KeyMappingWorker").apply { isDaemon = true }
    }

    private var uiState by mutableStateOf(KeyMappingUiState())

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
                    KeyMappingScreen(
                        state = uiState,
                        onTabSelected = { tab ->
                            uiState = uiState.copy(selectedTab = tab)
                        },
                        onToggleMaster = { enabled ->
                            toggleMasterEnabled(enabled)
                        },
                        onToggleAllowAdvanced = { allow ->
                            toggleAllowAdvanced(allow)
                        },
                        onDoubleTapWindowChange = { windowMs ->
                            setDoubleTapWindow(windowMs)
                        },
                        onOpenAccessibilitySettings = {
                            openAccessibilitySettings()
                        },
                        onTestRunBinding = { item ->
                            testRunAction(item)
                        },
                        onToggleBinding = { index, enabled ->
                            toggleBindingEnabled(index, enabled)
                        },
                        onEditBinding = { index ->
                            startEditingBinding(index)
                        },
                        onDeleteRequest = { index ->
                            uiState = uiState.copy(deleteConfirmIndex = index)
                        },
                        onDeleteConfirm = { index ->
                            deleteBinding(index)
                        },
                        onDeleteDismiss = {
                            uiState = uiState.copy(deleteConfirmIndex = null)
                        },
                        onSelectKnownButton = { code ->
                            uiState = uiState.copy(
                                selectedButtonCode = code,
                                isCustomButton = false,
                                manualKeyCodeText = code.toString(),
                                isCapturing = false,
                                capturedFeedback = null
                            )
                        },
                        onSelectCustomButton = {
                            uiState = uiState.copy(
                                isCustomButton = true,
                                manualKeyCodeText = "",
                                isCapturing = false,
                                capturedFeedback = null
                            )
                        },
                        onManualKeyCodeChange = { text ->
                            val clean = text.filter { it.isDigit() }
                            uiState = uiState.copy(manualKeyCodeText = clean)
                        },
                        onStartCapture = {
                            startKeyCapture()
                        },
                        onStopCapture = {
                            stopKeyCapture()
                        },
                        onSelectPressType = { pressType ->
                            uiState = uiState.copy(selectedPressType = pressType)
                        },
                        onToggleBlockNativeSingle = { block ->
                            uiState = uiState.copy(blockNativeSingle = block)
                        },
                        onSelectActionKind = { kind ->
                            uiState = uiState.copy(actionKind = kind)
                        },
                        onSelectCuratedAction = { curatedId ->
                            val actionDef = KeyMappingCatalog.CURATED_ACTIONS.find { it.id == curatedId }
                            val defaultPayload = actionDef?.payloadOptions?.firstOrNull()?.first ?: ""
                            uiState = uiState.copy(
                                selectedCuratedId = curatedId,
                                selectedPayloadValue = defaultPayload
                            )
                        },
                        onSelectPayloadValue = { payload ->
                            uiState = uiState.copy(selectedPayloadValue = payload)
                        },
                        onClipBeforeChange = { sec ->
                            uiState = uiState.copy(clipBeforeSeconds = sec)
                        },
                        onClipAfterChange = { sec ->
                            uiState = uiState.copy(clipAfterSeconds = sec)
                        },
                        onSelectAppPackage = { pkg ->
                            uiState = uiState.copy(selectedAppPackage = pkg)
                        },
                        onToggleAppSplitScreen = { split ->
                            uiState = uiState.copy(appSplitScreen = split)
                        },
                        onShellCommandChange = { cmd ->
                            uiState = uiState.copy(shellCommandText = cmd)
                        },
                        onSaveBinding = {
                            saveCurrentBinding()
                        },
                        onCancelEdit = {
                            resetForm()
                            uiState = uiState.copy(selectedTab = KeyMappingTab.BINDINGS)
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadKeymapConfig()
        loadInstalledApps()
    }

    override fun onResume() {
        super.onResume()
        // Refresh a11y service running status on resume
        uiState = uiState.copy(isA11yBound = KeepAliveAccessibilityService.isRunning())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Ensure capture listener is detached
        stopKeyCapture()
    }

    private fun loadKeymapConfig() {
        worker.execute {
            try {
                UnifiedConfigManager.forceReload()
                val keymap = UnifiedConfigManager.getKeymap()
                val enabled = keymap.optBoolean("enabled", false)
                val allowAdvanced = keymap.optBoolean("allowAdvanced", false)
                val doubleTapWindowMs = UnifiedConfigManager.getKeymapDoubleTapWindowMs()
                val bindingsArray = UnifiedConfigManager.getKeymapBindings()

                val parsedBindings = mutableListOf<KeyBindingItem>()
                for (i in 0 until bindingsArray.length()) {
                    val b = bindingsArray.optJSONObject(i) ?: continue
                    val keyCode = b.optInt("keycode", 0)
                    val pressType = b.optString("pressType", "single")
                    val isBindingEnabled = b.optBoolean("enabled", true)
                    val blockNativeSingle = b.optBoolean("blockNativeSingle", false)
                    val label = b.optString("label", "")
                    val action = b.optJSONObject("action") ?: JSONObject()

                    val buttonName = KeyMappingCatalog.getButtonName(keyCode)
                    val description = if (label.isNotBlank()) label else describeAction(action)

                    parsedBindings.add(
                        KeyBindingItem(
                            index = i,
                            keyCode = keyCode,
                            buttonName = buttonName,
                            pressType = pressType,
                            isEnabled = isBindingEnabled,
                            blockNativeSingle = blockNativeSingle,
                            actionDescription = description,
                            actionRaw = action
                        )
                    )
                }

                val a11yBound = KeepAliveAccessibilityService.isRunning()

                mainHandler.post {
                    uiState = uiState.copy(
                        isMasterEnabled = enabled,
                        allowAdvanced = allowAdvanced,
                        doubleTapWindowMs = doubleTapWindowMs,
                        isA11yBound = a11yBound,
                        bindings = parsedBindings,
                        isLoading = false
                    )
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    showToast("Yapılandırma yüklenemedi: ${t.message}")
                }
            }
        }
    }

    private fun loadInstalledApps() {
        worker.execute {
            try {
                val appsJson = AppLauncher.listLaunchableApps()
                val list = mutableListOf<AppOption>()
                for (i in 0 until appsJson.length()) {
                    val obj = appsJson.optJSONObject(i) ?: continue
                    val pkg = obj.optString("package", "")
                    val label = obj.optString("label", pkg)
                    if (pkg.isNotBlank()) {
                        list.add(AppOption(pkg, label))
                    }
                }
                mainHandler.post {
                    uiState = uiState.copy(
                        launchableApps = list,
                        selectedAppPackage = if (uiState.selectedAppPackage.isBlank() && list.isNotEmpty()) list.first().packageName else uiState.selectedAppPackage
                    )
                }
            } catch (_: Throwable) {}
        }
    }

    private fun startKeyCapture() {
        uiState = uiState.copy(
            isCapturing = true,
            capturedFeedback = null
        )
        KeyMapDispatcher.nativeCaptureListener = { keyCode ->
            mainHandler.post {
                stopKeyCapture()
                val name = KeyMappingCatalog.getButtonName(keyCode)
                uiState = uiState.copy(
                    selectedButtonCode = keyCode,
                    manualKeyCodeText = keyCode.toString(),
                    capturedFeedback = "Tuş yakalandı: $keyCode ($name)"
                )
                showToast("Tuş yakalandı: $name ($keyCode)")
            }
        }
    }

    private fun stopKeyCapture() {
        KeyMapDispatcher.nativeCaptureListener = null
        uiState = uiState.copy(isCapturing = false)
    }

    private fun toggleMasterEnabled(enabled: Boolean) {
        worker.execute {
            try {
                val keymap = UnifiedConfigManager.getKeymap()
                keymap.put("enabled", enabled)
                val ok = UnifiedConfigManager.setKeymap(keymap)
                mainHandler.post {
                    if (ok) {
                        uiState = uiState.copy(isMasterEnabled = enabled)
                        showToast(if (enabled) "Tuş eşleme etkinleştirildi" else "Tuş eşleme devre dışı bırakıldı")
                    } else {
                        showToast("Ayar kaydedilemedi")
                    }
                }
            } catch (t: Throwable) {
                mainHandler.post { showToast("Hata: ${t.message}") }
            }
        }
    }

    private fun toggleAllowAdvanced(allow: Boolean) {
        worker.execute {
            try {
                val keymap = UnifiedConfigManager.getKeymap()
                keymap.put("allowAdvanced", allow)
                val ok = UnifiedConfigManager.setKeymap(keymap)
                mainHandler.post {
                    if (ok) {
                        uiState = uiState.copy(allowAdvanced = allow)
                    } else {
                        showToast("Ayar kaydedilemedi")
                    }
                }
            } catch (t: Throwable) {
                mainHandler.post { showToast("Hata: ${t.message}") }
            }
        }
    }

    private fun setDoubleTapWindow(windowMs: Long) {
        val clamped = windowMs.coerceIn(250L, 1500L)
        uiState = uiState.copy(doubleTapWindowMs = clamped)
        worker.execute {
            try {
                val keymap = UnifiedConfigManager.getKeymap()
                keymap.put("doubleTapWindowMs", clamped)
                UnifiedConfigManager.setKeymap(keymap)
            } catch (_: Throwable) {}
        }
    }

    private fun toggleBindingEnabled(index: Int, enabled: Boolean) {
        worker.execute {
            try {
                val keymap = UnifiedConfigManager.getKeymap()
                val bindings = keymap.optJSONArray("bindings") ?: JSONArray()
                if (index in 0 until bindings.length()) {
                    val b = bindings.optJSONObject(index)
                    b?.put("enabled", enabled)
                    val ok = UnifiedConfigManager.setKeymap(keymap)
                    if (ok) {
                        mainHandler.post {
                            loadKeymapConfig()
                        }
                    }
                }
            } catch (t: Throwable) {
                mainHandler.post { showToast("Hata: ${t.message}") }
            }
        }
    }

    private fun deleteBinding(index: Int) {
        worker.execute {
            try {
                val keymap = UnifiedConfigManager.getKeymap()
                val oldBindings = keymap.optJSONArray("bindings") ?: JSONArray()
                val newBindings = JSONArray()
                for (i in 0 until oldBindings.length()) {
                    if (i != index) {
                        newBindings.put(oldBindings.get(i))
                    }
                }
                keymap.put("bindings", newBindings)
                val ok = UnifiedConfigManager.setKeymap(keymap)
                mainHandler.post {
                    uiState = uiState.copy(deleteConfirmIndex = null)
                    if (ok) {
                        showToast("Eşleme silindi")
                        loadKeymapConfig()
                    } else {
                        showToast("Eşleme silinemedi")
                    }
                }
            } catch (t: Throwable) {
                mainHandler.post { showToast("Hata: ${t.message}") }
            }
        }
    }

    private fun testRunAction(item: KeyBindingItem) {
        showToast("${item.buttonName} eylemi tetikleniyor...")
        KeyMapDispatcher.fireAction(item.actionRaw) { ok, resp ->
            mainHandler.post {
                if (ok) {
                    showToast("Eylem başarıyla çalıştırıldı")
                } else {
                    showToast("Eylem çalıştırılamadı: $resp")
                }
            }
        }
    }

    private fun startEditingBinding(index: Int) {
        val binding = uiState.bindings.find { it.index == index } ?: return
        val action = binding.actionRaw
        val kind = action.optString("kind", "curated")

        var curatedId = "lock"
        var payloadVal = ""
        var beforeSec = 30
        var afterSec = 0
        var appPkg = ""
        var appSplit = false
        var shellCmd = ""

        when (kind) {
            "vehicle" -> {
                val act = action.optString("action", "lock")
                curatedId = act
            }
            "catalog" -> {
                val key = action.optString("key", "")
                val found = KeyMappingCatalog.CURATED_ACTIONS.find { it.key == key }
                if (found != null) {
                    curatedId = found.id
                    payloadVal = action.optString("payload", "")
                }
            }
            "api" -> {
                val id = action.optString("id", "")
                val found = KeyMappingCatalog.CURATED_ACTIONS.find { it.id == id }
                if (found != null) {
                    curatedId = found.id
                }
            }
            "manualClip" -> {
                beforeSec = action.optInt("beforeSeconds", 30)
                afterSec = action.optInt("afterSeconds", 0)
            }
            "openApp" -> {
                appPkg = action.optString("package", "")
                appSplit = action.optBoolean("split", false)
            }
            "shell" -> {
                shellCmd = action.optString("cmd", "")
            }
        }

        uiState = uiState.copy(
            selectedTab = KeyMappingTab.ADD_EDIT,
            editIndex = index,
            selectedButtonCode = binding.keyCode,
            isCustomButton = KeyMappingCatalog.KNOWN_BUTTONS.none { it.code == binding.keyCode },
            manualKeyCodeText = binding.keyCode.toString(),
            selectedPressType = binding.pressType,
            blockNativeSingle = binding.blockNativeSingle,
            actionKind = if (kind == "catalog" || kind == "vehicle" || kind == "api") "curated" else kind,
            selectedCuratedId = curatedId,
            selectedPayloadValue = payloadVal,
            clipBeforeSeconds = beforeSec,
            clipAfterSeconds = afterSec,
            selectedAppPackage = appPkg,
            appSplitScreen = appSplit,
            shellCommandText = shellCmd
        )
    }

    private fun saveCurrentBinding() {
        val keyCode = if (uiState.isCustomButton) {
            uiState.manualKeyCodeText.toIntOrNull() ?: 0
        } else {
            uiState.selectedButtonCode
        }

        if (keyCode <= 0) {
            showToast("Lütfen geçerli bir donanım tuşu seçin veya tuş kodu girin")
            return
        }

        val actionObj = JSONObject()
        var label = ""

        when (uiState.actionKind) {
            "curated" -> {
                val curatedDef = KeyMappingCatalog.CURATED_ACTIONS.find { it.id == uiState.selectedCuratedId }
                    ?: KeyMappingCatalog.CURATED_ACTIONS.first()

                when (curatedDef.kind) {
                    "vehicle" -> {
                        actionObj.put("kind", "vehicle")
                        actionObj.put("action", curatedDef.key)
                        label = curatedDef.title
                    }
                    "catalog" -> {
                        actionObj.put("kind", "catalog")
                        actionObj.put("key", curatedDef.key)
                        actionObj.put("payload", uiState.selectedPayloadValue)
                        if (curatedDef.sub != null) {
                            actionObj.put("sub", curatedDef.sub)
                        }
                        val optText = curatedDef.payloadOptions.find { it.first == uiState.selectedPayloadValue }?.second ?: uiState.selectedPayloadValue
                        label = "${curatedDef.title} — $optText"
                    }
                    "api" -> {
                        actionObj.put("kind", "api")
                        actionObj.put("id", curatedDef.id)
                        actionObj.put("method", curatedDef.method)
                        val payload = uiState.selectedPayloadValue
                        actionObj.put("path", curatedDef.path.replace("\${v}", payload))
                        actionObj.put("body", curatedDef.body.replace("\${v}", payload))
                        val optText = curatedDef.payloadOptions.find { it.first == payload }?.second ?: payload
                        label = if (optText.isNotBlank()) "${curatedDef.title} — $optText" else curatedDef.title
                    }
                    "radio" -> {
                        actionObj.put("kind", "radio")
                        actionObj.put("radio", curatedDef.key)
                        actionObj.put("state", uiState.selectedPayloadValue)
                        label = "${curatedDef.title} — ${if (uiState.selectedPayloadValue == "on") "Açık" else "Kapalı"}"
                    }
                }
            }
            "manualClip" -> {
                actionObj.put("kind", "manualClip")
                actionObj.put("beforeSeconds", uiState.clipBeforeSeconds)
                actionObj.put("afterSeconds", uiState.clipAfterSeconds)
                label = "Anlık Klip (-${uiState.clipBeforeSeconds}s / +${uiState.clipAfterSeconds}s)"
            }
            "openApp" -> {
                if (uiState.selectedAppPackage.isBlank()) {
                    showToast("Lütfen başlatılacak bir uygulama seçin")
                    return
                }
                actionObj.put("kind", "openApp")
                actionObj.put("package", uiState.selectedAppPackage)
                val appLabel = uiState.launchableApps.find { it.packageName == uiState.selectedAppPackage }?.label ?: uiState.selectedAppPackage
                actionObj.put("label", appLabel)
                if (uiState.appSplitScreen) {
                    actionObj.put("split", true)
                }
                label = "Uygulama Başlat: $appLabel${if (uiState.appSplitScreen) " (Bölünmüş)" else ""}"
            }
            "shell" -> {
                if (!uiState.allowAdvanced) {
                    showToast("Gelişmiş kabuk komutları devre dışı")
                    return
                }
                val cmd = uiState.shellCommandText.trim()
                if (cmd.isBlank()) {
                    showToast("Lütfen bir shell komutu girin")
                    return
                }
                actionObj.put("kind", "shell")
                actionObj.put("cmd", cmd)
                label = "Kabuk Komutu: $cmd"
            }
        }

        val newBinding = JSONObject().apply {
            put("keycode", keyCode)
            put("pressType", uiState.selectedPressType)
            put("enabled", true)
            if (uiState.selectedPressType == "double") {
                put("blockNativeSingle", uiState.blockNativeSingle)
            }
            put("label", label)
            put("action", actionObj)
        }

        worker.execute {
            try {
                val keymap = UnifiedConfigManager.getKeymap()
                val bindings = keymap.optJSONArray("bindings") ?: JSONArray()

                val editIdx = uiState.editIndex
                if (editIdx != null && editIdx in 0 until bindings.length()) {
                    bindings.put(editIdx, newBinding)
                } else {
                    bindings.put(newBinding)
                }

                keymap.put("bindings", bindings)
                val ok = UnifiedConfigManager.setKeymap(keymap)

                mainHandler.post {
                    if (ok) {
                        showToast(if (editIdx != null) "Eşleme güncellendi" else "Yeni eşleme kaydedildi")
                        resetForm()
                        uiState = uiState.copy(selectedTab = KeyMappingTab.BINDINGS)
                        loadKeymapConfig()
                    } else {
                        showToast("Kayıt başarısız")
                    }
                }
            } catch (t: Throwable) {
                mainHandler.post { showToast("Hata: ${t.message}") }
            }
        }
    }

    private fun resetForm() {
        uiState = uiState.copy(
            editIndex = null,
            selectedButtonCode = 87,
            isCustomButton = false,
            manualKeyCodeText = "87",
            isCapturing = false,
            capturedFeedback = null,
            selectedPressType = "single",
            blockNativeSingle = true,
            actionKind = "curated",
            selectedCuratedId = "lock",
            selectedPayloadValue = "",
            clipBeforeSeconds = 30,
            clipAfterSeconds = 0,
            shellCommandText = ""
        )
    }

    private fun describeAction(a: JSONObject): String {
        val kind = a.optString("kind", "")
        return when (kind) {
            "vehicle" -> "Araç Eylemi: ${a.optString("action")}"
            "catalog" -> "${a.optString("key")} — ${a.optString("payload")}"
            "api" -> "API Eylemi: ${a.optString("id")}"
            "manualClip" -> "Anlık Klip: -${a.optInt("beforeSeconds")}s / +${a.optInt("afterSeconds")}s"
            "openApp" -> "Uygulama: ${a.optString("label", a.optString("package"))}"
            "shell" -> "Komut: ${a.optString("cmd")}"
            else -> kind
        }
    }

    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (t: Throwable) {
            showToast("Erişilebilirlik ayarları açılamadı: ${t.message}")
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(context ?: return, msg, Toast.LENGTH_SHORT).show()
    }
}
