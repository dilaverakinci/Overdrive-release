package com.overdrive.app.ui.telegram

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
import com.overdrive.app.config.UnifiedConfigManager
import com.overdrive.app.telegram.config.UnifiedTelegramConfig
import com.overdrive.app.ui.component.OverdriveComposeContainer
import com.overdrive.app.ui.theme.OverdriveTheme
import com.overdrive.app.util.DaemonHttpClient
import org.json.JSONObject
import java.util.concurrent.Executors

class TelegramComposeFragment : Fragment() {

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var uiState by mutableStateOf(TelegramUiState())
    private var countdownRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return OverdriveComposeContainer(requireContext()).apply {
            setContent {
                OverdriveTheme {
                    TelegramScreen(
                        state = uiState,
                        onTabSelected = { tab -> uiState = uiState.copy(selectedTab = tab) },
                        onTokenInputChange = { text -> uiState = uiState.copy(botTokenInput = text) },
                        onConnectToken = { connectToken() },
                        onClearToken = { clearToken() },
                        onGeneratePin = { generatePin() },
                        onUnpair = { unpair() },
                        onToggleAutoStart = { v -> updateTelegramPref("autoStartAccOff", v) },
                        onToggleVideoUploads = { v -> updateTelegramPref("videoUploads", v) },
                        onToggleCriticalAlerts = { v -> updateTelegramPref("criticalAlerts", v) },
                        onToggleTyreAlerts = { v -> updateTelegramPref("tyreAlerts", v) },
                        onToggleParkingMessages = { v -> updateTelegramPref("parkingMessages", v) },
                        onToggleMotionText = { v -> updateTelegramPref("motionText", v) },
                        onToggleTierNotices = { v -> updateTelegramPref("tierNotices", v) },
                        onToggleTierAlerts = { v -> updateTelegramPref("tierAlerts", v) },
                        onToggleTierCritical = { v -> updateTelegramPref("tierCritical", v) },
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
        countdownRunnable?.let { mainHandler.removeCallbacks(it) }
        executor.shutdown()
    }

    private fun loadState() {
        uiState = uiState.copy(isLoading = true)
        executor.execute {
            try {
                val fullConfig = UnifiedConfigManager.loadConfig()
                val tg = fullConfig.optJSONObject("telegram") ?: JSONObject()

                val autoStart = tg.optBoolean("autoStartAccOff", false)
                val videoUp = tg.optBoolean("videoUploads", false)
                val critAlerts = tg.optBoolean("criticalAlerts", true)
                val tyreAlerts = tg.optBoolean("tyreAlerts", true)
                val parkMsgs = tg.optBoolean("parkingMessages", true)
                val motion = tg.optBoolean("motionText", true)
                val tNotices = tg.optBoolean("tierNotices", false)
                val tAlerts = tg.optBoolean("tierAlerts", true)
                val tCrit = tg.optBoolean("tierCritical", true)

                // Live status query
                var isConfigured = false
                var isPaired = false
                var bUser = ""
                var bFirst = ""
                var oFirst = ""
                var oUser = ""
                var oChat = -1L
                var pin: String? = null
                var pinExpiry = 0L

                try {
                    val conn = DaemonHttpClient.open("/api/telegram/status", "GET", 2000, 3000)
                    if (conn.responseCode == 200) {
                        val body = conn.inputStream.bufferedReader().readText()
                        val s = JSONObject(body)
                        isConfigured = s.optBoolean("configured", false)
                        isPaired = s.optBoolean("paired", false)
                        bUser = s.optString("botUsername", "")
                        bFirst = s.optString("botFirstName", "")
                        oFirst = s.optString("ownerFirstName", "")
                        oUser = s.optString("ownerUsername", "")
                        oChat = s.optLong("ownerChatId", -1L)
                        val p = s.optString("pendingPin", "")
                        if (p.isNotBlank()) pin = p
                        pinExpiry = s.optLong("pendingPinExpiresAt", 0L)
                    }
                    conn.disconnect()
                } catch (_: Throwable) {
                    // Fallback to local config
                    isConfigured = tg.has("botToken") && tg.optString("botToken").isNotBlank()
                    isPaired = tg.optLong("ownerChatId", -1L) > 0
                    bUser = tg.optString("botUsername", "")
                    bFirst = tg.optString("botFirstName", "")
                    oFirst = tg.optString("ownerFirstName", "")
                    oUser = tg.optString("ownerUsername", "")
                    oChat = tg.optLong("ownerChatId", -1L)
                }

                mainHandler.post {
                    val remSeconds = if (pinExpiry > System.currentTimeMillis()) {
                        ((pinExpiry - System.currentTimeMillis()) / 1000).toInt()
                    } else 0

                    uiState = uiState.copy(
                        isConfigured = isConfigured,
                        isPaired = isPaired,
                        botUsername = bUser,
                        botFirstName = bFirst,
                        ownerFirstName = oFirst,
                        ownerUsername = oUser,
                        ownerChatId = oChat,
                        pendingPin = pin,
                        pinExpiresSeconds = remSeconds,
                        autoStartAccOff = autoStart,
                        videoUploads = videoUp,
                        criticalAlerts = critAlerts,
                        tyreAlerts = tyreAlerts,
                        parkingMessages = parkMsgs,
                        motionText = motion,
                        tierNotices = tNotices,
                        tierAlerts = tAlerts,
                        tierCritical = tCrit,
                        isLoading = false,
                        statusMessage = null
                    )

                    if (remSeconds > 0) {
                        startCountdown()
                    }
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    uiState = uiState.copy(isLoading = false)
                    Toast.makeText(requireContext(), "Telegram durumu alınamadı: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun connectToken() {
        val token = uiState.botTokenInput.trim()
        if (token.isBlank()) {
            Toast.makeText(requireContext(), "Lütfen geçerli bir bot tokeni girin", Toast.LENGTH_SHORT).show()
            return
        }

        uiState = uiState.copy(isLoading = true, statusMessage = "Bağlantı test ediliyor...", isError = false)
        executor.execute {
            try {
                val conn = DaemonHttpClient.open("/api/telegram/token", "POST", 3000, 5000)
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                val json = JSONObject().apply { put("token", token) }
                conn.outputStream.bufferedWriter().use { it.write(json.toString()) }

                val code = conn.responseCode
                val respText = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                val resp = JSONObject(respText)
                if (code == 200 && resp.optBoolean("success", false)) {
                    mainHandler.post {
                        uiState = uiState.copy(
                            isLoading = false,
                            botTokenInput = "",
                            statusMessage = "Bot başarıyla bağlandı!",
                            isError = false
                        )
                        Toast.makeText(requireContext(), "Bot başarıyla bağlandı", Toast.LENGTH_SHORT).show()
                        loadState()
                    }
                } else {
                    val err = resp.optString("error", "Bağlantı başarısız oldu")
                    mainHandler.post {
                        uiState = uiState.copy(isLoading = false, statusMessage = err, isError = true)
                    }
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    uiState = uiState.copy(isLoading = false, statusMessage = "Hata: ${t.message}", isError = true)
                }
            }
        }
    }

    private fun clearToken() {
        uiState = uiState.copy(isLoading = true)
        executor.execute {
            try {
                val conn = DaemonHttpClient.open("/api/telegram/clear", "POST", 2000, 3000)
                conn.responseCode
                conn.disconnect()
                mainHandler.post {
                    uiState = uiState.copy(isLoading = false, isConfigured = false, isPaired = false)
                    Toast.makeText(requireContext(), "Token temizlendi", Toast.LENGTH_SHORT).show()
                    loadState()
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    uiState = uiState.copy(isLoading = false)
                    Toast.makeText(requireContext(), "Temizleme başarısız: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun generatePin() {
        uiState = uiState.copy(isLoading = true)
        executor.execute {
            try {
                val conn = DaemonHttpClient.open("/api/telegram/pin", "POST", 2000, 3000)
                val resp = JSONObject(conn.inputStream.bufferedReader().readText())
                conn.disconnect()

                if (resp.optBoolean("success", false)) {
                    val pin = resp.optString("pin")
                    val expiresAt = resp.optLong("expiresAt", System.currentTimeMillis() + 300_000L)
                    val rem = ((expiresAt - System.currentTimeMillis()) / 1000).toInt()

                    mainHandler.post {
                        uiState = uiState.copy(
                            pendingPin = pin,
                            pinExpiresSeconds = rem,
                            isLoading = false
                        )
                        startCountdown()
                    }
                } else {
                    val err = resp.optString("error", "PIN oluşturulamadı")
                    mainHandler.post {
                        uiState = uiState.copy(isLoading = false)
                        Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    uiState = uiState.copy(isLoading = false)
                    Toast.makeText(requireContext(), "PIN oluşturulamadı: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun unpair() {
        uiState = uiState.copy(isLoading = true)
        executor.execute {
            try {
                val conn = DaemonHttpClient.open("/api/telegram/unpair", "POST", 2000, 3000)
                conn.responseCode
                conn.disconnect()
                mainHandler.post {
                    uiState = uiState.copy(isLoading = false, isPaired = false)
                    Toast.makeText(requireContext(), "Eşleşme kaldırıldı", Toast.LENGTH_SHORT).show()
                    loadState()
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    uiState = uiState.copy(isLoading = false)
                    Toast.makeText(requireContext(), "Eşleşme kaldırılamadı: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startCountdown() {
        countdownRunnable?.let { mainHandler.removeCallbacks(it) }
        countdownRunnable = object : Runnable {
            override fun run() {
                val next = uiState.pinExpiresSeconds - 1
                if (next > 0) {
                    uiState = uiState.copy(pinExpiresSeconds = next)
                    mainHandler.postDelayed(this, 1000)
                } else {
                    uiState = uiState.copy(pendingPin = null, pinExpiresSeconds = 0)
                }
            }
        }
        mainHandler.postDelayed(countdownRunnable!!, 1000)
    }

    private fun updateTelegramPref(key: String, value: Any) {
        when (key) {
            "autoStartAccOff" -> uiState = uiState.copy(autoStartAccOff = value as Boolean)
            "videoUploads" -> uiState = uiState.copy(videoUploads = value as Boolean)
            "criticalAlerts" -> uiState = uiState.copy(criticalAlerts = value as Boolean)
            "tyreAlerts" -> uiState = uiState.copy(tyreAlerts = value as Boolean)
            "parkingMessages" -> uiState = uiState.copy(parkingMessages = value as Boolean)
            "motionText" -> uiState = uiState.copy(motionText = value as Boolean)
            "tierNotices" -> uiState = uiState.copy(tierNotices = value as Boolean)
            "tierAlerts" -> uiState = uiState.copy(tierAlerts = value as Boolean)
            "tierCritical" -> uiState = uiState.copy(tierCritical = value as Boolean)
        }

        executor.execute {
            try {
                UnifiedConfigManager.updateValues("telegram", mapOf(key to value))
            } catch (t: Throwable) {
                mainHandler.post {
                    Toast.makeText(requireContext(), "Ayar kaydedilemedi: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
