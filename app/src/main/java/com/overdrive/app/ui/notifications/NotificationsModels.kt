package com.overdrive.app.ui.notifications

import com.overdrive.app.R
import com.overdrive.app.ui.telegram.TelegramUiState

enum class NotificationSeverity(val key: String, val labelRes: Int) {
    ALL("", R.string.notif_filter_all),
    CRITICAL("critical", R.string.notif_filter_critical),
    WARN("warn", R.string.notif_filter_warn),
    INFO("info", R.string.notif_filter_info)
}

data class NotificationLogItem(
    val id: Long,
    val title: String,
    val body: String,
    val severity: NotificationSeverity,
    val category: String,
    val timestamp: Long,
    val snapshotUrl: String? = null,
    val place: String? = null
)

enum class NotificationsTab(val titleRes: Int, val iconRes: Int) {
    LOG(R.string.notif_tab_log, R.drawable.ic_notifications),
    TELEGRAM(R.string.notif_tab_telegram, R.drawable.ic_telegram)
}

data class NotificationsUiState(
    val selectedTab: NotificationsTab = NotificationsTab.LOG,
    val logItems: List<NotificationLogItem> = emptyList(),
    val totalCount: Int = 0,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val isLoading: Boolean = false,
    val filterSeverity: NotificationSeverity = NotificationSeverity.ALL,
    val filterDays: Int = 0, // 0 = all time, 1 = today, 7 = 7 days, 30 = 30 days
    val searchQuery: String = "",
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val showClearAllDialog: Boolean = false,
    val showDeleteSelectedDialog: Boolean = false,
    val telegramState: TelegramUiState = TelegramUiState()
)
