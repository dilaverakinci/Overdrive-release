package com.overdrive.app.ui.fragment

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.overdrive.app.R
import com.overdrive.app.config.UnifiedConfigManager
import com.overdrive.app.telegram.impl.BotTokenConfig
import com.overdrive.app.ui.component.OverdriveComposeContainer
import com.overdrive.app.ui.integrations.IntegrationsScreen
import com.overdrive.app.ui.integrations.IntegrationsUiState
import com.overdrive.app.ui.theme.OverdriveTheme
import com.overdrive.app.ui.util.navigateDrillDown
import com.overdrive.app.util.DaemonHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.util.concurrent.Executors

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 100% Jetpack Compose Native Integrations Fragment.
 * Rolls up Telegram / ABRP / MQTT / BYD Cloud statuses with live background polling.
 */
class IntegrationsFragment : Fragment() {

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val daemonPoll = object : Runnable {
        override fun run() {
            refreshDaemonStatuses()
            mainHandler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    private var uiState by mutableStateOf(IntegrationsUiState())
    private var showSafeKeepInfoDialog by mutableStateOf(false)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = OverdriveComposeContainer(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OverdriveTheme {
                IntegrationsScreen(
                    state = uiState,
                    onTelegramClick = {
                        findNavController().navigateDrillDown(R.id.telegramSettingsFragment)
                    },
                    onAbrpClick = {
                        findNavController().navigateDrillDown(R.id.abrpSettingsFragment)
                    },
                    onMqttClick = {
                        findNavController().navigateDrillDown(R.id.mqttFragment)
                    },
                    onBydCloudClick = {
                        findNavController().navigateDrillDown(R.id.bydCloudFragment)
                    },
                    onSafeKeepClick = {
                        showSafeKeepInfoDialog = true
                    }
                )

                if (showSafeKeepInfoDialog) {
                    AlertDialog(
                        onDismissRequest = { showSafeKeepInfoDialog = false },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_safekeep),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp),
                            )
                        },
                        title = {
                            Text(
                                text = stringResource(R.string.integrations_safekeep_info_dialog_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(R.string.integrations_safekeep_info_dialog_message),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { showSafeKeepInfoDialog = false }) {
                                Text(
                                    text = stringResource(R.string.integrations_safekeep_dialog_ok),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshTelegramStatus()
        mainHandler.post(daemonPoll)
    }

    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(daemonPoll)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        executor.shutdownNow()
    }

    // ============== Refresh ==============

    private fun refreshTelegramStatus() {
        val ctx = context ?: return
        val telegram = isTelegramConfigured(ctx)
        uiState = uiState.copy(telegramConfigured = telegram)
    }

    private fun refreshDaemonStatuses() {
        executor.execute {
            val ctx = context
            val telegram = ctx?.let { isTelegramConfigured(it) } ?: uiState.telegramConfigured
            val abrp = fetchAbrpRunning()
            val mqtt = fetchMqttAnyConnected()
            val bydCloud = fetchBydCloudConfigured()
            mainHandler.post {
                uiState = IntegrationsUiState(
                    telegramConfigured = telegram,
                    abrpConnected = abrp,
                    mqttConnected = mqtt,
                    bydCloudConfigured = bydCloud
                )
            }
        }
    }

    // ============== Probes ==============

    private fun isTelegramConfigured(ctx: Context): Boolean = try {
        UnifiedConfigManager.forceReload()
        BotTokenConfig(ctx.applicationContext).hasToken()
    } catch (_: Throwable) {
        false
    }

    private fun fetchAbrpRunning(): Boolean {
        val json = fetchDaemonJson("/api/abrp/status") ?: return false
        if (!json.optBoolean("success", false)) return false
        val status = json.optJSONObject("status") ?: return false
        return status.optBoolean("running", false)
    }

    private fun fetchMqttAnyConnected(): Boolean {
        val json = fetchDaemonJson("/api/mqtt/status") ?: return false
        if (!json.optBoolean("success", false)) return false
        val arr: JSONArray = json.optJSONArray("connections") ?: return false
        for (i in 0 until arr.length()) {
            val entry = arr.optJSONObject(i) ?: continue
            val status = entry.optJSONObject("status") ?: continue
            if (status.optBoolean("connected", false)) return true
        }
        return false
    }

    private fun fetchBydCloudConfigured(): Boolean {
        val json = fetchDaemonJson("/api/bydcloud/status") ?: return false
        if (!json.optBoolean("success", false)) return false
        val status = json.optJSONObject("status") ?: return false
        return status.optBoolean("configured", false)
    }

    private fun fetchDaemonJson(path: String): JSONObject? {
        var conn: HttpURLConnection? = null
        return try {
            conn = DaemonHttpClient.open(path, "GET", 1500, 2500)
            if (conn.responseCode != 200) return null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            if (body.isEmpty()) null else JSONObject(body)
        } catch (_: Throwable) {
            null
        } finally {
            try { conn?.disconnect() } catch (_: Throwable) {}
        }
    }

    companion object {
        private const val POLL_INTERVAL_MS = 3000L
    }
}
