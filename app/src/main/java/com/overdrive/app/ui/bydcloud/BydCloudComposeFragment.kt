package com.overdrive.app.ui.bydcloud

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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BydCloudComposeFragment : Fragment() {

    private var executorService: ExecutorService? = null
    private val executor: ExecutorService
        get() = executorService?.takeUnless { it.isShutdown } ?: Executors.newSingleThreadExecutor().also { executorService = it }
    private val mainHandler = Handler(Looper.getMainLooper())

    private var uiState by mutableStateOf(BydCloudUiState())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return OverdriveComposeContainer(requireContext()).apply {
            setContent {
                OverdriveTheme {
                    BydCloudScreen(
                        state = uiState,
                        onTabSelected = { tab -> uiState = uiState.copy(selectedTab = tab) },
                        onSaveCredentials = { username, password, pin, countryCode, region ->
                            saveCredentials(username, password, pin, countryCode, region)
                        },
                        onTestConnection = {
                            testConnection()
                        },
                        onToggleCloudDataMerge = { enabled ->
                            toggleCloudDataMerge(enabled)
                        },
                        onConfirmClearCredentials = {
                            uiState = uiState.copy(showClearDialog = false)
                            clearCredentials()
                        },
                        onDismissClearDialog = {
                            uiState = uiState.copy(showClearDialog = false)
                        },
                        onOpenClearDialog = {
                            uiState = uiState.copy(showClearDialog = true)
                        },
                        onRefresh = {
                            loadStatus()
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadStatus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        executorService?.shutdownNow()
        executorService = null
    }

    private fun loadStatus() {
        uiState = uiState.copy(isLoading = true)
        executor.execute {
            if (!isAdded) return@execute
            try {
                val conn = DaemonHttpClient.open("/api/bydcloud/status", "GET", 3000, 4000)
                val code = conn.responseCode
                if (code == 200) {
                    val body = conn.inputStream.bufferedReader().readText()
                    val root = JSONObject(body)
                    if (root.optBoolean("success", false)) {
                        val st = root.optJSONObject("status")
                        if (st != null) {
                            val configured = st.optBoolean("configured", false)
                            val verified = st.optBoolean("verified", false)
                            val vin = st.optString("vin", "")
                            val username = st.optString("username", "")
                            val country = st.optString("countryCode", "TR")
                            val reg = st.optString("region", "tr")

                            val cp = st.optJSONObject("cloudPush")
                            val isPushConn = cp?.optBoolean("connected", false) ?: false
                            val age = cp?.optLong("lastMessageAge", -1L) ?: -1L
                            val lock = cp?.optString("lockState", "") ?: ""
                            val soc = if (cp != null && cp.has("socPercent") && !cp.isNull("socPercent")) cp.optInt("socPercent") else null
                            val chg = cp?.optString("chargingState", "") ?: ""
                            val merge = cp?.optBoolean("cloudDataMerge", false) ?: false

                            mainHandler.post {
                                uiState = uiState.copy(
                                    isConfigured = configured,
                                    isVerified = verified,
                                    vin = vin,
                                    username = username,
                                    countryCode = if (country.isNotBlank()) country else "TR",
                                    region = if (reg.isNotBlank()) reg else "tr",
                                    isPushConnected = isPushConn,
                                    lastPushAgeSeconds = age,
                                    pushLockState = lock,
                                    pushSocPercent = soc,
                                    pushChargingState = chg,
                                    cloudDataMerge = merge,
                                    isLoading = false
                                )
                            }
                            conn.disconnect()
                            return@execute
                        }
                    }
                }
                conn.disconnect()
            } catch (_: Throwable) {}

            mainHandler.post {
                uiState = uiState.copy(isLoading = false)
            }
        }
    }

    private fun saveCredentials(
        username: String,
        password: String,
        pin: String,
        countryCode: String,
        region: String
    ) {
        if (username.isBlank()) {
            Toast.makeText(requireContext(), "Lütfen e-posta adresinizi girin", Toast.LENGTH_LONG).show()
            return
        }
        if (!uiState.isConfigured && (password.isBlank() || pin.isBlank())) {
            Toast.makeText(requireContext(), "İlk kurulumda şifre ve kontrol PIN gereklidir", Toast.LENGTH_LONG).show()
            return
        }

        uiState = uiState.copy(isLoading = true, statusMessage = "BYD Cloud sunucusuna bağlanılıyor...", isStatusError = false)
        executor.execute {
            try {
                val payload = JSONObject().apply {
                    put("username", username.trim())
                    if (password.isNotBlank()) put("password", password.trim())
                    if (pin.isNotBlank()) put("controlPin", pin.trim())
                    put("countryCode", countryCode)
                    put("region", region)
                }

                val conn = DaemonHttpClient.open("/api/bydcloud/setup", "POST", 30000, 35000)
                conn.doOutput = true
                conn.outputStream.bufferedWriter().use { it.write(payload.toString()) }
                val code = conn.responseCode
                val body = if (code in 200..299) conn.inputStream.bufferedReader().readText() else conn.errorStream?.bufferedReader()?.readText() ?: ""
                conn.disconnect()

                val resJson = if (body.isNotBlank()) JSONObject(body) else JSONObject()
                val success = resJson.optBoolean("success", false)
                val errMsg = resJson.optString("error", "")

                mainHandler.post {
                    uiState = uiState.copy(
                        isLoading = false,
                        statusMessage = if (success) "BYD Cloud bağlantısı doğrulandı ve kaydedildi!" else "Hata: ${if (errMsg.isNotBlank()) errMsg else "HTTP $code"}",
                        isStatusError = !success
                    )
                    loadStatus()
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    uiState = uiState.copy(
                        isLoading = false,
                        statusMessage = "Bağlantı hatası: ${t.message}",
                        isStatusError = true
                    )
                }
            }
        }
    }

    private fun testConnection() {
        uiState = uiState.copy(isLoading = true, statusMessage = "Araç sinyalleri test ediliyor (ışık yakma)...", isStatusError = false)
        executor.execute {
            try {
                val payload = JSONObject().apply {
                    put("action", "flash_lights")
                }
                val conn = DaemonHttpClient.open("/api/bydcloud/test", "POST", 20000, 25000)
                conn.doOutput = true
                conn.outputStream.bufferedWriter().use { it.write(payload.toString()) }
                val code = conn.responseCode
                val body = if (code in 200..299) conn.inputStream.bufferedReader().readText() else ""
                conn.disconnect()

                val resJson = if (body.isNotBlank()) JSONObject(body) else JSONObject()
                val success = resJson.optBoolean("success", false)
                val errMsg = resJson.optString("error", "")

                mainHandler.post {
                    uiState = uiState.copy(
                        isLoading = false,
                        statusMessage = if (success) "Komut araca iletildi (ışıklar yanıp söndü)!" else "Test başarısız: $errMsg",
                        isStatusError = !success
                    )
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    uiState = uiState.copy(
                        isLoading = false,
                        statusMessage = "Test hatası: ${t.message}",
                        isStatusError = true
                    )
                }
            }
        }
    }

    private fun toggleCloudDataMerge(enabled: Boolean) {
        executor.execute {
            try {
                val payload = JSONObject().apply {
                    put("cloudDataMerge", enabled)
                }
                val conn = DaemonHttpClient.open("/api/bydcloud/settings", "POST", 3000, 4000)
                conn.doOutput = true
                conn.outputStream.bufferedWriter().use { it.write(payload.toString()) }
                val code = conn.responseCode
                conn.disconnect()

                mainHandler.post {
                    if (code == 200) {
                        Toast.makeText(requireContext(), if (enabled) "Bulut veri birleştirme açık" else "Bulut veri birleştirme kapalı", Toast.LENGTH_SHORT).show()
                    }
                    loadStatus()
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    Toast.makeText(requireContext(), "Ayar güncellenemedi: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun clearCredentials() {
        uiState = uiState.copy(isLoading = true)
        executor.execute {
            try {
                val conn = DaemonHttpClient.open("/api/bydcloud/clear", "POST", 3000, 4000)
                conn.doOutput = true
                val code = conn.responseCode
                conn.disconnect()

                mainHandler.post {
                    uiState = uiState.copy(
                        isLoading = false,
                        statusMessage = "Kimlik bilgileri temizlendi",
                        isStatusError = false
                    )
                    Toast.makeText(requireContext(), "Kimlik bilgileri temizlendi", Toast.LENGTH_SHORT).show()
                    loadStatus()
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    uiState = uiState.copy(
                        isLoading = false,
                        statusMessage = "Temizleme hatası: ${t.message}",
                        isStatusError = true
                    )
                }
            }
        }
    }
}
