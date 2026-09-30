package com.overdrive.app.ui.mqtt

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
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MqttComposeFragment : Fragment() {

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var uiState by mutableStateOf(MqttUiState())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return OverdriveComposeContainer(requireContext()).apply {
            setContent {
                OverdriveTheme {
                    MqttScreen(
                        state = uiState,
                        onTabSelected = { tab ->
                            if (tab == MqttTab.ADD_EDIT && !uiState.isEditing) {
                                uiState = uiState.copy(
                                    selectedTab = tab,
                                    isEditing = false,
                                    formState = MqttFormState()
                                )
                            } else {
                                uiState = uiState.copy(selectedTab = tab)
                            }
                        },
                        onToggleConnectionEnabled = { id, enabled ->
                            toggleConnection(id, enabled)
                        },
                        onEditConnection = { item ->
                            uiState = uiState.copy(
                                selectedTab = MqttTab.ADD_EDIT,
                                isEditing = true,
                                formState = MqttFormState(
                                    id = item.id,
                                    name = item.name,
                                    brokerUrl = item.brokerUrl,
                                    port = item.port.toString(),
                                    topic = item.topic
                                )
                            )
                        },
                        onDeleteConnection = { id ->
                            deleteConnection(id)
                        },
                        onTestConnection = { form ->
                            testConnection(form)
                        },
                        onSaveForm = { form ->
                            saveConnection(form)
                        },
                        onFormChange = { form ->
                            uiState = uiState.copy(formState = form)
                        },
                        onCancelEdit = {
                            uiState = uiState.copy(
                                selectedTab = MqttTab.CONNECTIONS,
                                isEditing = false,
                                formState = MqttFormState()
                            )
                        },
                        onRefresh = {
                            loadAllData()
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadAllData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        executor.shutdown()
    }

    private fun loadAllData() {
        uiState = uiState.copy(isLoading = true)
        executor.execute {
            val connections = mutableListOf<MqttConnectionItem>()
            val telemetryMap = mutableMapOf<String, String>()

            // 1. Load Status / Connections
            try {
                val conn = DaemonHttpClient.open("/api/mqtt/status", "GET", 2500, 3500)
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().readText()
                    val root = JSONObject(body)
                    if (root.optBoolean("success", false)) {
                        val arr = root.optJSONArray("connections") ?: JSONArray()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            connections.add(parseConnectionItem(obj))
                        }
                    }
                }
                conn.disconnect()
            } catch (_: Throwable) {
                // Fallback to /api/mqtt/connections if status call is unavailable
                try {
                    val connFallback = DaemonHttpClient.open("/api/mqtt/connections", "GET", 2000, 3000)
                    if (connFallback.responseCode == 200) {
                        val body = connFallback.inputStream.bufferedReader().readText()
                        val root = JSONObject(body)
                        if (root.optBoolean("success", false)) {
                            val arr = root.optJSONArray("connections") ?: JSONArray()
                            for (i in 0 until arr.length()) {
                                connections.add(parseConnectionItem(arr.getJSONObject(i)))
                            }
                        }
                    }
                    connFallback.disconnect()
                } catch (_: Throwable) {}
            }

            // 2. Load Telemetry Snapshot
            try {
                val tlmConn = DaemonHttpClient.open("/api/mqtt/telemetry", "GET", 2000, 3000)
                if (tlmConn.responseCode == 200) {
                    val body = tlmConn.inputStream.bufferedReader().readText()
                    val root = JSONObject(body)
                    if (root.optBoolean("success", false)) {
                        val tlm = root.optJSONObject("telemetry")
                        if (tlm != null) {
                            tlm.keys().forEach { k ->
                                telemetryMap[k] = tlm.opt(k)?.toString() ?: ""
                            }
                        }
                    }
                }
                tlmConn.disconnect()
            } catch (_: Throwable) {}

            mainHandler.post {
                uiState = uiState.copy(
                    connections = connections,
                    liveTelemetry = telemetryMap,
                    isLoading = false
                )
            }
        }
    }

    private fun parseConnectionItem(obj: JSONObject): MqttConnectionItem {
        val id = obj.optString("id", "")
        val name = obj.optString("name", "Broker")
        val broker = obj.optString("brokerUrl", "")
        val port = obj.optInt("port", 1883)
        val topic = obj.optString("topic", "overdrive/vehicle/telemetry")
        val enabled = obj.optBoolean("enabled", false)

        val st = obj.optJSONObject("status")
        val isConnected = st?.optBoolean("connected", false) ?: false
        val isRunning = st?.optBoolean("running", false) ?: false
        val published = st?.optLong("totalPublishes", 0L) ?: 0L
        val failed = st?.optLong("failedPublishes", 0L) ?: 0L
        val lastPub = st?.optLong("lastPublishTime", 0L) ?: 0L

        val stateText = when {
            enabled && isConnected -> "Bağlı"
            enabled && isRunning && !isConnected -> "Yeniden Bağlanıyor"
            enabled && !isRunning -> "Bağlantı Kesildi"
            else -> "Durduruldu"
        }

        val lastSeenStr = if (lastPub > 0) {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            sdf.format(Date(lastPub))
        } else "—"

        return MqttConnectionItem(
            id = id,
            name = name,
            brokerUrl = broker,
            port = port,
            topic = topic,
            enabled = enabled,
            isConnected = isConnected,
            stateText = stateText,
            publishedCount = published,
            receivedCount = 0L,
            errorCount = failed,
            lastSeenText = lastSeenStr
        )
    }

    private fun toggleConnection(id: String, enabled: Boolean) {
        executor.execute {
            try {
                val payload = JSONObject().apply {
                    put("enabled", enabled)
                }
                val conn = DaemonHttpClient.open("/api/mqtt/connections/$id", "PUT", 2500, 3500)
                conn.doOutput = true
                conn.outputStream.bufferedWriter().use { it.write(payload.toString()) }
                val code = conn.responseCode
                conn.disconnect()

                mainHandler.post {
                    if (code == 200) {
                        Toast.makeText(requireContext(), if (enabled) "Broker etkinleştirildi" else "Broker durduruldu", Toast.LENGTH_SHORT).show()
                    }
                    loadAllData()
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    Toast.makeText(requireContext(), "Hata: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteConnection(id: String) {
        executor.execute {
            try {
                val conn = DaemonHttpClient.open("/api/mqtt/connections/$id", "DELETE", 2500, 3500)
                val code = conn.responseCode
                conn.disconnect()

                mainHandler.post {
                    if (code == 200) {
                        Toast.makeText(requireContext(), "Bağlantı silindi", Toast.LENGTH_SHORT).show()
                    }
                    loadAllData()
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    Toast.makeText(requireContext(), "Silme hatası: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun testConnection(form: MqttFormState) {
        uiState = uiState.copy(isLoading = true, statusMessage = "Bağlantı test ediliyor...")
        executor.execute {
            try {
                // Check if broker host is reachable or use test endpoint
                val testPayload = JSONObject().apply {
                    put("brokerUrl", form.brokerUrl)
                    put("port", form.port.toIntOrNull() ?: 1883)
                }
                val conn = DaemonHttpClient.open("/api/mqtt/test", "POST", 3000, 4000)
                conn.doOutput = true
                conn.outputStream.bufferedWriter().use { it.write(testPayload.toString()) }
                val code = conn.responseCode
                val response = if (code in 200..299) conn.inputStream.bufferedReader().readText() else ""
                conn.disconnect()

                mainHandler.post {
                    uiState = uiState.copy(
                        isLoading = false,
                        statusMessage = if (code == 200) "Test başarılı! Broker erişilebilir." else "Test yanıtı: $code",
                        isError = code != 200
                    )
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    uiState = uiState.copy(
                        isLoading = false,
                        statusMessage = "Bağlantı testi yapılamadı: ${t.message}",
                        isError = true
                    )
                }
            }
        }
    }

    private fun saveConnection(form: MqttFormState) {
        if (form.name.isBlank() || form.brokerUrl.isBlank() || form.topic.isBlank()) {
            Toast.makeText(requireContext(), "Lütfen Ad, Sunucu ve Topic alanlarını doldurun", Toast.LENGTH_LONG).show()
            return
        }

        uiState = uiState.copy(isLoading = true)
        executor.execute {
            try {
                val payload = JSONObject().apply {
                    put("name", form.name.trim())
                    put("brokerUrl", form.brokerUrl.trim())
                    put("port", form.port.toIntOrNull() ?: 1883)
                    put("topic", form.topic.trim())
                    put("username", form.username.trim())
                    if (form.password.isNotBlank() || form.id == null) {
                        put("password", form.password)
                    }
                    put("clientId", form.clientId.trim())
                    put("qos", form.qos)
                    put("minIntervalSeconds", form.minIntervalSeconds)
                    put("maxIntervalSeconds", form.maxIntervalSeconds)
                    put("changeOnly", form.changeOnly)
                    put("enabled", true)
                }

                val isEdit = form.id != null
                val path = if (isEdit) "/api/mqtt/connections/${form.id}" else "/api/mqtt/connections"
                val method = if (isEdit) "PUT" else "POST"

                val conn = DaemonHttpClient.open(path, method, 3000, 4000)
                conn.doOutput = true
                conn.outputStream.bufferedWriter().use { it.write(payload.toString()) }
                val code = conn.responseCode
                val body = if (code in 200..299) conn.inputStream.bufferedReader().readText() else ""
                conn.disconnect()

                mainHandler.post {
                    uiState = uiState.copy(isLoading = false)
                    if (code in 200..299) {
                        Toast.makeText(requireContext(), if (isEdit) "Bağlantı güncellendi" else "Bağlantı eklendi", Toast.LENGTH_SHORT).show()
                        uiState = uiState.copy(
                            selectedTab = MqttTab.CONNECTIONS,
                            isEditing = false,
                            formState = MqttFormState()
                        )
                        loadAllData()
                    } else {
                        Toast.makeText(requireContext(), "Kaydetme hatası (HTTP $code): $body", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    uiState = uiState.copy(isLoading = false)
                    Toast.makeText(requireContext(), "Hata: ${t.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
