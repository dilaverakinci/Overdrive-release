package com.overdrive.app.ui.network

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
import androidx.fragment.app.Fragment
import com.overdrive.app.R
import com.overdrive.app.network.CellularRelay
import com.overdrive.app.network.HotspotManager
import com.overdrive.app.ui.component.OverdriveComposeContainer
import com.overdrive.app.ui.theme.OverdriveTheme
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class NetworkComposeFragment : Fragment() {

    private var ioService: ExecutorService? = null
    private val io: ExecutorService
        get() = ioService?.takeUnless { it.isShutdown } ?: Executors.newSingleThreadExecutor().also { ioService = it }
    private val mainHandler = Handler(Looper.getMainLooper())

    private var uiState by mutableStateOf(NetworkUiState())
    private var lastSnapshot: JSONObject? = null
    private var snapshotAtMs: Long = 0L

    private val uiTick = object : Runnable {
        override fun run() {
            renderUptime()
            mainHandler.postDelayed(this, 1000L)
        }
    }

    private val snapshotTick = object : Runnable {
        override fun run() {
            refreshSnapshot()
            mainHandler.postDelayed(this, 5000L)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return OverdriveComposeContainer(requireContext()).apply {
            setContent {
                OverdriveTheme {
                    NetworkScreen(
                        state = uiState,
                        onToggleHotspot = { on ->
                            handleToggleHotspot(on)
                        },
                        onConfirmEnableHotspot = {
                            uiState = uiState.copy(showWarningDialog = false)
                            io.execute { HotspotManager.applySettings(mapOf("warnAck" to true), null) }
                            setHotspot(true)
                        },
                        onDismissWarningDialog = {
                            uiState = uiState.copy(showWarningDialog = false)
                        },
                        onTogglePasswordRevealed = {
                            uiState = uiState.copy(isPasswordRevealed = !uiState.isPasswordRevealed)
                        },
                        onCopySsid = {
                            copyToClipboard(uiState.ssid, "SSID")
                        },
                        onCopyPassword = {
                            copyToClipboard(uiState.password, "Şifre")
                        },
                        onSaveLimit = { capMb ->
                            saveLimit(capMb)
                        },
                        onResetUsage = {
                            resetUsage()
                        },
                        onToggleSwitch = { key, value ->
                            io.execute { HotspotManager.applySettings(mapOf(key to value), null) }
                            refreshSnapshot()
                        },
                        onRefresh = {
                            refreshSnapshot()
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        HotspotManager.init(requireContext().applicationContext)
        refreshSnapshot()
    }

    override fun onResume() {
        super.onResume()
        mainHandler.post(uiTick)
        mainHandler.post(snapshotTick)
    }

    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(uiTick)
        mainHandler.removeCallbacks(snapshotTick)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ioService?.shutdownNow()
        ioService = null
    }

    private fun handleToggleHotspot(on: Boolean) {
        if (on) {
            val acked = lastSnapshot?.optBoolean("warnAck", false) ?: false
            if (!acked) {
                uiState = uiState.copy(showWarningDialog = true)
                return
            }
        }
        setHotspot(on)
    }

    private fun setHotspot(on: Boolean) {
        uiState = uiState.copy(
            isTransitioning = true,
            stateText = if (on) "Başlatılıyor..." else "Durduruluyor..."
        )
        io.execute {
            val cb: (Boolean, String) -> Unit = { ok, msg ->
                mainHandler.post {
                    if (!isAdded) return@post
                    if (!ok) {
                        toast(msg)
                    }
                    refreshSnapshot()
                }
            }
            if (on) HotspotManager.enable(cb) else HotspotManager.disable(cb)
        }
    }

    private fun saveLimit(mb: Long) {
        if (mb < 0L) {
            toast("Geçersiz limit değeri")
            return
        }
        io.execute { HotspotManager.applySettings(mapOf("dataCapMb" to mb), null) }
        toast("Veri limiti kaydedildi")
        refreshSnapshot()
    }

    private fun resetUsage() {
        io.execute { HotspotManager.resetUsage(null) }
        toast("Kullanım verisi sıfırlandı")
        refreshSnapshot()
    }

    private fun copyToClipboard(text: String, label: String) {
        if (text.isEmpty()) return
        val ctx = context ?: return
        val clip = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clip.setPrimaryClip(ClipData.newPlainText(label, text))
        toast("$label kopyalandı")
    }

    private fun toast(msg: String) {
        val ctx = context ?: return
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
    }

    private fun refreshSnapshot() {
        io.execute {
            val snap = try { HotspotManager.snapshot() } catch (t: Throwable) { null } ?: return@execute
            mainHandler.post {
                if (isAdded) bindSnapshot(snap)
            }
        }
    }

    private fun bindSnapshot(snap: JSONObject) {
        lastSnapshot = snap
        snapshotAtMs = System.currentTimeMillis()

        val enabled = snap.optBoolean("enabled", false)
        val transitioning = snap.optBoolean("transitioning", false)
        val lastErr = snap.optString("lastError", "")

        val stateText = when {
            transitioning -> "Başlatılıyor..."
            enabled -> "Açık"
            lastErr.isNotEmpty() -> lastErr
            else -> "Kapalı"
        }

        val ssid = snap.optString("activeSsid", "").ifEmpty { snap.optString("ssid", "") }
        val password = snap.optString("activePassword", "")
        val dataCap = snap.optLong("dataCapMb", 0L)

        // Usage calculation
        val used = snap.optLong("dataUsedBytes", 0L) +
                snap.optLong("rxBytes", 0L) + snap.optLong("txBytes", 0L)
        val usageLabel = if (dataCap > 0L) {
            "${formatBytes(used)} / $dataCap MB"
        } else {
            "${formatBytes(used)} (Limitsiz)"
        }

        // Clients
        val clientsList = mutableListOf<ConnectedClient>()
        val arr = snap.optJSONArray("clients")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val c = arr.optJSONObject(i) ?: continue
                clientsList.add(
                    ConnectedClient(
                        name = c.optString("name", "Cihaz"),
                        ip = c.optString("ip", ""),
                        mac = c.optString("mac", "")
                    )
                )
            }
        }

        val gw = snap.optString("gateway", HotspotManager.AP_GATEWAY)
        val relayP = snap.optInt("relayPort", CellularRelay.PORT)
        val tunnelP = snap.optInt("clientTunnelPort", com.overdrive.app.daemon.proxy.ProxyConfiguration.CLIENT_TUNNEL_PORT)

        uiState = uiState.copy(
            isEnabled = enabled,
            isTransitioning = transitioning,
            stateText = stateText,
            ssid = ssid,
            password = password,
            rxText = formatBytes(snap.optLong("rxBytes", 0L)),
            txText = formatBytes(snap.optLong("txBytes", 0L)),
            usageText = usageLabel,
            dataCapMb = dataCap,
            clients = clientsList,
            keepAlive = snap.optBoolean("keepAlive", false),
            autoStartBoot = snap.optBoolean("autoStartBoot", false),
            proxySystemWide = snap.optBoolean("proxySystemWide", false),
            proxyForClients = snap.optBoolean("proxyForClients", false),
            clientTunnel = snap.optBoolean("clientTunnel", false),
            proxyClientsDesc = "İstemciler HTTP proxy ayarını $gw:$relayP olarak yapılandırmalıdır.",
            clientTunnelDesc = "İstemci tüneli port $tunnelP → $relayP üzerinden yönlendirilir."
        )

        renderUptime()
    }

    private fun renderUptime() {
        val snap = lastSnapshot ?: return
        val base = snap.optLong("uptimeSeconds", 0L)
        val drift = if (snapshotAtMs > 0L) (System.currentTimeMillis() - snapshotAtMs) / 1000L else 0L
        val secs = if (snap.optBoolean("enabled", false)) base + drift.coerceAtLeast(0L) else 0L

        uiState = uiState.copy(uptimeText = formatDuration(secs))
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024L * 1024L * 1024L -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }

    private fun formatDuration(seconds: Long): String {
        if (seconds <= 0L) return "--"
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    }
}
