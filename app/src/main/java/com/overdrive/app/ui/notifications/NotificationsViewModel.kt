package com.overdrive.app.ui.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.overdrive.app.config.UnifiedConfigManager
import com.overdrive.app.ui.telegram.TelegramTab
import com.overdrive.app.ui.telegram.TelegramUiState
import com.overdrive.app.util.DaemonHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        loadLogs()
        loadTelegramState()
    }

    fun selectTab(tab: NotificationsTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        if (tab == NotificationsTab.LOG) {
            loadLogs()
        } else {
            loadTelegramState()
        }
    }

    fun loadLogs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            withContext(Dispatchers.IO) {
                try {
                    val s = _uiState.value
                    val path = buildString {
                        append("/api/notifications/log?page=${s.currentPage}&pageSize=20&days=${s.filterDays}")
                        if (s.filterSeverity.key.isNotEmpty()) {
                            append("&severity=${s.filterSeverity.key}")
                        }
                    }
                    val conn = DaemonHttpClient.open(path, "GET")
                    if (conn.responseCode == 200) {
                        val text = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(text)
                        val total = json.optInt("total", 0)
                        val totalPages = json.optInt("totalPages", 1)
                        val page = json.optInt("page", 1)
                        val itemsArray = json.optJSONArray("notifications") ?: JSONArray()
                        val items = mutableListOf<NotificationLogItem>()

                        for (i in 0 until itemsArray.length()) {
                            val obj = itemsArray.optJSONObject(i) ?: continue
                            val id = obj.optLong("id", i.toLong())
                            val title = obj.optString("title", "")
                            val body = obj.optString("body", obj.optString("message", ""))
                            val sevStr = obj.optString("severity", "info").lowercase()
                            val severity = when (sevStr) {
                                "critical" -> NotificationSeverity.CRITICAL
                                "warn", "warning" -> NotificationSeverity.WARN
                                else -> NotificationSeverity.INFO
                            }
                            val category = obj.optString("category", "system")
                            val timestamp = obj.optLong("ts", obj.optLong("timestamp", System.currentTimeMillis()))
                            val dataObj = obj.optJSONObject("data")
                            val snapshot = dataObj?.optString("snapshot")
                            val place = dataObj?.optString("place")

                            items.add(
                                NotificationLogItem(
                                    id = id,
                                    title = title,
                                    body = body,
                                    severity = severity,
                                    category = category,
                                    timestamp = timestamp,
                                    snapshotUrl = snapshot,
                                    place = place
                                )
                            )
                        }

                        _uiState.update {
                            it.copy(
                                logItems = items,
                                totalCount = total,
                                currentPage = page,
                                totalPages = totalPages.coerceAtLeast(1),
                                isLoading = false
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun setSeverityFilter(severity: NotificationSeverity) {
        _uiState.update { it.copy(filterSeverity = severity, currentPage = 1) }
        loadLogs()
    }

    fun setDateFilter(days: Int) {
        _uiState.update { it.copy(filterDays = days, currentPage = 1) }
        loadLogs()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleSelectionMode() {
        _uiState.update {
            it.copy(
                isSelectionMode = !it.isSelectionMode,
                selectedIds = emptySet()
            )
        }
    }

    fun toggleSelect(id: Long) {
        _uiState.update { state ->
            val set = state.selectedIds.toMutableSet()
            if (set.contains(id)) {
                set.remove(id)
            } else {
                set.add(id)
            }
            state.copy(selectedIds = set)
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            state.copy(selectedIds = state.logItems.map { it.id }.toSet())
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    fun showClearAllDialog() {
        _uiState.update { it.copy(showClearAllDialog = true) }
    }

    fun showDeleteSelectedDialog() {
        _uiState.update { it.copy(showDeleteSelectedDialog = true) }
    }

    fun dismissDialogs() {
        _uiState.update {
            it.copy(showClearAllDialog = false, showDeleteSelectedDialog = false)
        }
    }

    fun deleteSelected() {
        val idsToDelete = _uiState.value.selectedIds.toList()
        if (idsToDelete.isEmpty()) return
        dismissDialogs()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            withContext(Dispatchers.IO) {
                try {
                    val conn = DaemonHttpClient.open("/api/notifications/log/bulk-delete", "POST")
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    val body = JSONObject().apply {
                        put("ids", JSONArray(idsToDelete))
                    }.toString()
                    conn.outputStream.bufferedWriter().use { it.write(body) }

                    if (conn.responseCode == 200) {
                        _uiState.update {
                            it.copy(
                                selectedIds = emptySet(),
                                isSelectionMode = false
                            )
                        }
                        loadLogs()
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val conn = DaemonHttpClient.open("/api/notifications/log/delete", "POST")
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    val body = JSONObject().apply {
                        put("id", id)
                    }.toString()
                    conn.outputStream.bufferedWriter().use { it.write(body) }

                    if (conn.responseCode == 200) {
                        loadLogs()
                    }
                } catch (e: Exception) {
                    // silently noop
                }
            }
        }
    }

    fun clearAll() {
        dismissDialogs()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            withContext(Dispatchers.IO) {
                try {
                    val conn = DaemonHttpClient.open("/api/notifications/log/clear", "POST")
                    if (conn.responseCode == 200) {
                        _uiState.update {
                            it.copy(
                                logItems = emptyList(),
                                totalCount = 0,
                                selectedIds = emptySet(),
                                isSelectionMode = false,
                                isLoading = false
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun nextPage() {
        val s = _uiState.value
        if (s.currentPage < s.totalPages) {
            _uiState.update { it.copy(currentPage = s.currentPage + 1) }
            loadLogs()
        }
    }

    fun prevPage() {
        val s = _uiState.value
        if (s.currentPage > 1) {
            _uiState.update { it.copy(currentPage = s.currentPage - 1) }
            loadLogs()
        }
    }

    // =========================================================================
    // Telegram State & Actions
    // =========================================================================

    fun loadTelegramState() {
        _uiState.update { it.copy(telegramState = it.telegramState.copy(isLoading = true)) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
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

                    var isConfigured = false
                    var isPaired = false
                    var bUser = ""
                    var bFirst = ""
                    var oFirst = ""
                    var oUser = ""
                    var oChat = -1L

                    try {
                        val conn = DaemonHttpClient.open("/api/telegram/status", "GET")
                        if (conn.responseCode == 200) {
                            val text = conn.inputStream.bufferedReader().use { it.readText() }
                            val st = JSONObject(text)
                            isConfigured = st.optBoolean("configured", false)
                            isPaired = st.optBoolean("paired", false)
                            val bot = st.optJSONObject("bot")
                            if (bot != null) {
                                bUser = bot.optString("username", "")
                                bFirst = bot.optString("first_name", "")
                            }
                            val owner = st.optJSONObject("owner")
                            if (owner != null) {
                                oFirst = owner.optString("first_name", "")
                                oUser = owner.optString("username", "")
                                oChat = owner.optLong("chat_id", -1L)
                            }
                        }
                    } catch (e: Exception) {
                        // fallback
                    }

                    _uiState.update {
                        it.copy(
                            telegramState = it.telegramState.copy(
                                isConfigured = isConfigured,
                                isPaired = isPaired,
                                botUsername = bUser,
                                botFirstName = bFirst,
                                ownerFirstName = oFirst,
                                ownerUsername = oUser,
                                ownerChatId = oChat,
                                autoStartAccOff = autoStart,
                                videoUploads = videoUp,
                                criticalAlerts = critAlerts,
                                tyreAlerts = tyreAlerts,
                                parkingMessages = parkMsgs,
                                motionText = motion,
                                tierNotices = tNotices,
                                tierAlerts = tAlerts,
                                tierCritical = tCrit,
                                isLoading = false
                            )
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(telegramState = it.telegramState.copy(isLoading = false))
                    }
                }
            }
        }
    }

    fun setTelegramTab(tab: TelegramTab) {
        _uiState.update { it.copy(telegramState = it.telegramState.copy(selectedTab = tab)) }
    }

    fun setTelegramTokenInput(token: String) {
        _uiState.update { it.copy(telegramState = it.telegramState.copy(botTokenInput = token)) }
    }

    fun connectTelegramToken() {
        val token = _uiState.value.telegramState.botTokenInput.trim()
        if (token.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(telegramState = it.telegramState.copy(isLoading = true)) }
            withContext(Dispatchers.IO) {
                try {
                    val conn = DaemonHttpClient.open("/api/telegram/token", "POST")
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.outputStream.bufferedWriter().use { it.write(JSONObject().put("token", token).toString()) }
                    loadTelegramState()
                } catch (e: Exception) {
                    _uiState.update { it.copy(telegramState = it.telegramState.copy(isLoading = false)) }
                }
            }
        }
    }

    fun clearTelegramToken() {
        viewModelScope.launch {
            _uiState.update { it.copy(telegramState = it.telegramState.copy(isLoading = true)) }
            withContext(Dispatchers.IO) {
                try {
                    val conn = DaemonHttpClient.open("/api/telegram/clear", "POST")
                    loadTelegramState()
                } catch (e: Exception) {
                    _uiState.update { it.copy(telegramState = it.telegramState.copy(isLoading = false)) }
                }
            }
        }
    }

    fun generateTelegramPin() {
        viewModelScope.launch {
            _uiState.update { it.copy(telegramState = it.telegramState.copy(isLoading = true)) }
            withContext(Dispatchers.IO) {
                try {
                    val conn = DaemonHttpClient.open("/api/telegram/pair-pin", "POST")
                    if (conn.responseCode == 200) {
                        val text = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(text)
                        val pin = json.optString("pin")
                        val expires = json.optInt("expires_seconds", 300)
                        _uiState.update {
                            it.copy(
                                telegramState = it.telegramState.copy(
                                    pendingPin = pin,
                                    pinExpiresSeconds = expires,
                                    isLoading = false
                                )
                            )
                        }
                    } else {
                        _uiState.update { it.copy(telegramState = it.telegramState.copy(isLoading = false)) }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(telegramState = it.telegramState.copy(isLoading = false)) }
                }
            }
        }
    }

    fun unpairTelegram() {
        viewModelScope.launch {
            _uiState.update { it.copy(telegramState = it.telegramState.copy(isLoading = true)) }
            withContext(Dispatchers.IO) {
                try {
                    DaemonHttpClient.open("/api/telegram/unpair", "POST")
                    loadTelegramState()
                } catch (e: Exception) {
                    _uiState.update { it.copy(telegramState = it.telegramState.copy(isLoading = false)) }
                }
            }
        }
    }

    fun updateTelegramPref(key: String, value: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val conn = DaemonHttpClient.open("/api/telegram/preferences", "POST")
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.outputStream.bufferedWriter().use {
                        it.write(JSONObject().put(key, value).toString())
                    }
                    loadTelegramState()
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }
}
