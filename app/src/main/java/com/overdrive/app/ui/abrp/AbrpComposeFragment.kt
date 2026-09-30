package com.overdrive.app.ui.abrp

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
import androidx.fragment.app.Fragment
import com.overdrive.app.ui.component.OverdriveComposeContainer
import com.overdrive.app.ui.theme.OverdriveTheme
import com.overdrive.app.util.DaemonHttpClient
import org.json.JSONObject
import java.util.concurrent.Executors

class AbrpComposeFragment : Fragment() {

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var uiState by mutableStateOf(AbrpUiState())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return OverdriveComposeContainer(requireContext()).apply {
            setContent {
                OverdriveTheme {
                    AbrpScreen(
                        state = uiState,
                        onTabSelected = { tab -> uiState = uiState.copy(selectedTab = tab) },
                        onTokenInputChange = { text -> uiState = uiState.copy(tokenInput = text) },
                        onSaveToken = { saveAndTestToken() },
                        onDeleteToken = { deleteToken() },
                        onToggleChangeOnly = { v -> postConfigUpdate(mapOf("change_only" to v)) },
                        onMinIntervalChange = { v -> postConfigUpdate(mapOf("min_interval" to v)) },
                        onMaxIntervalChange = { v -> postConfigUpdate(mapOf("max_interval" to v)) },
                        onToggleGateOnApp = { v -> postConfigUpdate(mapOf("gate_on_app" to v)) },
                        onAppActiveModeSelected = { m -> postConfigUpdate(mapOf("app_mode" to m)) },
                        onAppGraceChange = { g -> postConfigUpdate(mapOf("app_grace" to g)) },
                        onRefresh = { loadState() }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        executor.shutdown()
    }

    private fun loadState() {
        uiState = uiState.copy(isLoading = true)
        executor.execute {
            try {
                // Fetch config
                var hasToken = false
                var masked = ""
                var chgOnly = true
                var minInt = 5
                var maxInt = 120
                var gateApp = false
                var appMode = "foreground"
                var appGrace = 90

                try {
                    val conn = DaemonHttpClient.open("/api/abrp/config", "GET", 2000, 3000)
                    if (conn.responseCode == 200) {
                        val body = conn.inputStream.bufferedReader().readText()
                        val root = JSONObject(body)
                        val cfg = root.optJSONObject("config")
                        if (cfg != null) {
                            val token = cfg.optString("user_token", "")
                            hasToken = token.isNotBlank()
                            if (hasToken) {
                                masked = if (token.length > 4) "••••" + token.takeLast(4) else "••••"
                            }
                            chgOnly = cfg.optBoolean("change_only", true)
                            minInt = cfg.optInt("min_interval", 5)
                            maxInt = cfg.optInt("max_interval", 120)
                            gateApp = cfg.optBoolean("gate_on_app", false)
                            appMode = cfg.optString("app_mode", "foreground")
                            appGrace = cfg.optInt("app_grace", 90)
                        }
                    }
                    conn.disconnect()
                } catch (_: Throwable) {}

                // Fetch telemetry status
                var soc = 0f
                var pwr = 0f
                var spd = 0f
                var chg = false
                var dcfc = false
                var extT = 0f
                var batT = 0f
                var odo = 0.0
                var soh = 100f
                var connected = false
                val tlmMap = mutableMapOf<String, String>()

                try {
                    val conn = DaemonHttpClient.open("/api/abrp/status", "GET", 2000, 3000)
                    if (conn.responseCode == 200) {
                        val body = conn.inputStream.bufferedReader().readText()
                        val root = JSONObject(body)
                        connected = root.optBoolean("connected", true)
                        soc = root.optDouble("soc", 0.0).toFloat()
                        pwr = root.optDouble("power", 0.0).toFloat()
                        spd = root.optDouble("speed", 0.0).toFloat()
                        chg = root.optBoolean("is_charging", false)
                        dcfc = root.optBoolean("is_dcfc", false)
                        extT = root.optDouble("ext_temp", 0.0).toFloat()
                        batT = root.optDouble("batt_temp", 0.0).toFloat()
                        odo = root.optDouble("odometer", 0.0)
                        soh = root.optDouble("soh", 100.0).toFloat()

                        root.keys().forEach { k ->
                            tlmMap[k] = root.opt(k)?.toString() ?: ""
                        }
                    }
                    conn.disconnect()
                } catch (_: Throwable) {}

                mainHandler.post {
                    uiState = uiState.copy(
                        hasToken = hasToken,
                        maskedToken = masked,
                        changeOnly = chgOnly,
                        minIntervalSeconds = minInt,
                        maxIntervalSeconds = maxInt,
                        gateOnApp = gateApp,
                        appActiveMode = appMode,
                        appGraceSeconds = appGrace,
                        soc = soc,
                        powerKw = pwr,
                        speedKmh = spd,
                        isCharging = chg,
                        isDcfc = dcfc,
                        extTempC = extT,
                        battTempC = batT,
                        odometerKm = odo,
                        soh = soh,
                        isConnected = connected,
                        telemetryMap = tlmMap,
                        isLoading = false,
                        statusMessage = null
                    )
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    uiState = uiState.copy(isLoading = false)
                    Toast.makeText(requireContext(), "ABRP bilgisi alınamadı: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun postConfigUpdate(delta: Map<String, Any>) {
        // Optimistic UI updates
        delta.forEach { (k, v) ->
            when (k) {
                "change_only" -> uiState = uiState.copy(changeOnly = v as Boolean)
                "min_interval" -> uiState = uiState.copy(minIntervalSeconds = v as Int)
                "max_interval" -> uiState = uiState.copy(maxIntervalSeconds = v as Int)
                "gate_on_app" -> uiState = uiState.copy(gateOnApp = v as Boolean)
                "app_mode" -> uiState = uiState.copy(appActiveMode = v as String)
                "app_grace" -> uiState = uiState.copy(appGraceSeconds = v as Int)
            }
        }

        executor.execute {
            try {
                val conn = DaemonHttpClient.open("/api/abrp/config", "POST", 2000, 3000)
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                val json = JSONObject(delta)
                conn.outputStream.bufferedWriter().use { it.write(json.toString()) }
                conn.responseCode
                conn.disconnect()
            } catch (t: Throwable) {
                mainHandler.post {
                    Toast.makeText(requireContext(), "Ayar kaydedilemedi: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveAndTestToken() {
        val token = uiState.tokenInput.trim()
        if (token.isBlank()) {
            Toast.makeText(requireContext(), "Lütfen geçerli bir token girin", Toast.LENGTH_SHORT).show()
            return
        }

        uiState = uiState.copy(isLoading = true, statusMessage = "Token test ediliyor...", isError = false)
        executor.execute {
            try {
                // Post token config
                val conn = DaemonHttpClient.open("/api/abrp/config", "POST", 3000, 5000)
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                val json = JSONObject().apply { put("user_token", token) }
                conn.outputStream.bufferedWriter().use { it.write(json.toString()) }
                conn.responseCode
                conn.disconnect()

                // Test token
                val testConn = DaemonHttpClient.open("/api/abrp/test", "POST", 3000, 6000)
                val testCode = testConn.responseCode
                val testResp = testConn.inputStream.bufferedReader().readText()
                testConn.disconnect()

                val testObj = JSONObject(testResp)
                val isSuccess = testCode == 200 && testObj.optBoolean("success", true)

                mainHandler.post {
                    if (isSuccess) {
                        uiState = uiState.copy(
                            isLoading = false,
                            tokenInput = "",
                            hasToken = true,
                            maskedToken = if (token.length > 4) "••••" + token.takeLast(4) else "••••",
                            statusMessage = "ABRP Tokeni başarıyla doğrulandı ve bağlandı!",
                            isError = false
                        )
                        Toast.makeText(requireContext(), "ABRP Tokeni kaydedildi", Toast.LENGTH_SHORT).show()
                    } else {
                        val errMsg = testObj.optString("error", "Token testi başarısız oldu")
                        uiState = uiState.copy(isLoading = false, statusMessage = errMsg, isError = true)
                    }
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    uiState = uiState.copy(isLoading = false, statusMessage = "Hata: ${t.message}", isError = true)
                }
            }
        }
    }

    private fun deleteToken() {
        uiState = uiState.copy(isLoading = true)
        executor.execute {
            try {
                val conn = DaemonHttpClient.open("/api/abrp/token/delete", "POST", 2000, 3000)
                conn.responseCode
                conn.disconnect()
                mainHandler.post {
                    uiState = uiState.copy(
                        isLoading = false,
                        hasToken = false,
                        maskedToken = "",
                        statusMessage = "Token silindi"
                    )
                    Toast.makeText(requireContext(), "ABRP Tokeni silindi", Toast.LENGTH_SHORT).show()
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    uiState = uiState.copy(isLoading = false)
                    Toast.makeText(requireContext(), "Token silinemedi: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
