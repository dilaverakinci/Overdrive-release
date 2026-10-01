package com.overdrive.app.ui.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.overdrive.app.R
import com.overdrive.app.ui.component.OverdriveButton
import com.overdrive.app.ui.component.OverdriveButtonVariant
import com.overdrive.app.ui.component.OverdriveCard
import com.overdrive.app.ui.component.OverdriveDialog
import com.overdrive.app.ui.component.OverdrivePillStatus
import com.overdrive.app.ui.component.OverdriveStatusPill
import com.overdrive.app.ui.telegram.TelegramScreen
import com.overdrive.app.ui.theme.OverdriveTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 1. Top Bar: Title & Primary Tab Switcher
            NotificationsHeaderBar(
                selectedTab = state.selectedTab,
                totalCount = state.totalCount,
                onSelectTab = { viewModel.selectTab(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Tab Content
            when (state.selectedTab) {
                NotificationsTab.LOG -> {
                    NotificationLogTabContent(
                        state = state,
                        onRefresh = { viewModel.loadLogs() },
                        onSetSeverity = { viewModel.setSeverityFilter(it) },
                        onSetDays = { viewModel.setDateFilter(it) },
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onToggleSelectMode = { viewModel.toggleSelectionMode() },
                        onToggleSelect = { viewModel.toggleSelect(it) },
                        onSelectAll = { viewModel.selectAll() },
                        onClearSelection = { viewModel.clearSelection() },
                        onDeleteSelected = { viewModel.showDeleteSelectedDialog() },
                        onClearAll = { viewModel.showClearAllDialog() },
                        onDeleteItem = { viewModel.deleteItem(it) },
                        onNextPage = { viewModel.nextPage() },
                        onPrevPage = { viewModel.prevPage() }
                    )
                }
                NotificationsTab.TELEGRAM -> {
                    TelegramScreen(
                        state = state.telegramState,
                        onTabSelected = { viewModel.setTelegramTab(it) },
                        onTokenInputChange = { viewModel.setTelegramTokenInput(it) },
                        onConnectToken = { viewModel.connectTelegramToken() },
                        onClearToken = { viewModel.clearTelegramToken() },
                        onGeneratePin = { viewModel.generateTelegramPin() },
                        onUnpair = { viewModel.unpairTelegram() },
                        onToggleAutoStart = { viewModel.updateTelegramPref("autoStartAccOff", it) },
                        onToggleVideoUploads = { viewModel.updateTelegramPref("videoUploads", it) },
                        onToggleCriticalAlerts = { viewModel.updateTelegramPref("criticalAlerts", it) },
                        onToggleTyreAlerts = { viewModel.updateTelegramPref("tyreAlerts", it) },
                        onToggleParkingMessages = { viewModel.updateTelegramPref("parkingMessages", it) },
                        onToggleMotionText = { viewModel.updateTelegramPref("motionText", it) },
                        onToggleTierNotices = { viewModel.updateTelegramPref("tierNotices", it) },
                        onToggleTierAlerts = { viewModel.updateTelegramPref("tierAlerts", it) },
                        onToggleTierCritical = { viewModel.updateTelegramPref("tierCritical", it) },
                        onRefresh = { viewModel.loadTelegramState() }
                    )
                }
            }
        }

        // 3. Confirmation Dialogs
        if (state.showClearAllDialog) {
            val clearTitle = stringResource(R.string.notif_clear_title)
            val posText = stringResource(R.string.notif_filter_all)
            val negText = stringResource(R.string.action_cancel)
            val clearDesc = stringResource(R.string.notif_clear_desc)
            OverdriveDialog(
                onDismissRequest = { viewModel.dismissDialogs() },
                title = clearTitle,
                positiveButtonText = posText,
                onPositiveClick = { viewModel.clearAll() },
                negativeButtonText = negText,
                onNegativeClick = { viewModel.dismissDialogs() }
            ) {
                Text(
                    text = clearDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (state.showDeleteSelectedDialog) {
            val delTitle = stringResource(R.string.notif_clear_title)
            val posText = stringResource(R.string.action_delete)
            val negText = stringResource(R.string.action_cancel)
            val delDesc = stringResource(R.string.notif_delete_selected_fmt, state.selectedIds.size)
            OverdriveDialog(
                onDismissRequest = { viewModel.dismissDialogs() },
                title = delTitle,
                positiveButtonText = posText,
                onPositiveClick = { viewModel.deleteSelected() },
                negativeButtonText = negText,
                onNegativeClick = { viewModel.dismissDialogs() }
            ) {
                Text(
                    text = delDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NotificationsHeaderBar(
    selectedTab: NotificationsTab,
    totalCount: Int,
    onSelectTab: (NotificationsTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_notifications),
                contentDescription = null,
                tint = OverdriveTheme.colors.primary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.rail_notifications),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (totalCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                OverdriveStatusPill(
                    label = stringResource(R.string.notif_count_fmt, totalCount),
                    status = OverdrivePillStatus.INFO
                )
            }
        }

        // Primary Segmented Tab Switcher
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                NotificationsTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    Surface(
                        onClick = { onSelectTab(tab) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(tab.iconRes),
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(tab.titleRes),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationLogTabContent(
    state: NotificationsUiState,
    onRefresh: () -> Unit,
    onSetSeverity: (NotificationSeverity) -> Unit,
    onSetDays: (Int) -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleSelectMode: () -> Unit,
    onToggleSelect: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onClearAll: () -> Unit,
    onDeleteItem: (Long) -> Unit,
    onNextPage: () -> Unit,
    onPrevPage: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Filter & Controls Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Severity Chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NotificationSeverity.values().forEach { sev ->
                    val isSelected = state.filterSeverity == sev
                    Surface(
                        onClick = { onSetSeverity(sev) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = stringResource(sev.labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Date Range Chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val dateOptions = listOf(
                    0 to R.string.notif_date_all,
                    1 to R.string.notif_date_today,
                    7 to R.string.notif_date_week,
                    30 to R.string.notif_date_month
                )
                dateOptions.forEach { (days, labelRes) ->
                    val isSelected = state.filterDays == days
                    Surface(
                        onClick = { onSetDays(days) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Right Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onToggleSelectMode) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Select Mode",
                        tint = if (state.isSelectionMode) OverdriveTheme.colors.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onClearAll) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.notif_clear_title),
                        tint = OverdriveTheme.colors.statusDanger
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.recordings_refresh_btn),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Bulk Selection Bar
        AnimatedVisibility(visible = state.isSelectionMode) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.notif_delete_selected_fmt, state.selectedIds.size),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OverdriveButton(
                            text = stringResource(R.string.notif_filter_all),
                            variant = OverdriveButtonVariant.OUTLINED,
                            onClick = onSelectAll
                        )
                        OverdriveButton(
                            text = stringResource(R.string.action_cancel),
                            variant = OverdriveButtonVariant.OUTLINED,
                            onClick = onClearSelection
                        )
                        OverdriveButton(
                            text = stringResource(R.string.action_delete),
                            variant = OverdriveButtonVariant.DANGER,
                            enabled = state.selectedIds.isNotEmpty(),
                            onClick = onDeleteSelected
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Log Items List
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (state.isLoading && state.logItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = OverdriveTheme.colors.primary)
                }
            } else if (state.logItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_notifications),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = stringResource(R.string.notif_empty),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.notif_empty_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.logItems, key = { it.id }) { item ->
                        NotificationCard(
                            item = item,
                            isSelectionMode = state.isSelectionMode,
                            isSelected = state.selectedIds.contains(item.id),
                            onToggleSelect = { onToggleSelect(item.id) },
                            onDelete = { onDeleteItem(item.id) }
                        )
                    }
                }
            }
        }

        // Pagination Bar
        if (state.totalPages > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrevPage,
                    enabled = state.currentPage > 1
                ) {
                    Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Prev")
                }
                Text(
                    text = "${state.currentPage} / ${state.totalPages}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = onNextPage,
                    enabled = state.currentPage < state.totalPages
                ) {
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next")
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    item: NotificationLogItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = when (item.severity) {
        NotificationSeverity.CRITICAL -> OverdriveTheme.colors.statusDanger
        NotificationSeverity.WARN -> OverdriveTheme.colors.statusWarning
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    val timeFormatted = remember(item.timestamp) {
        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(item.timestamp))
    }

    OverdriveCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isSelectionMode, onClick = onToggleSelect),
        borderColor = borderColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(checkedColor = OverdriveTheme.colors.primary)
                )
            } else {
                val sevColor = when (item.severity) {
                    NotificationSeverity.CRITICAL -> OverdriveTheme.colors.statusDanger
                    NotificationSeverity.WARN -> OverdriveTheme.colors.statusWarning
                    else -> OverdriveTheme.colors.primary
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(sevColor)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = item.title.ifEmpty { item.category.uppercase() },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OverdriveStatusPill(
                            label = item.category,
                            status = when (item.severity) {
                                NotificationSeverity.CRITICAL -> OverdrivePillStatus.DANGER
                                NotificationSeverity.WARN -> OverdrivePillStatus.WARNING
                                else -> OverdrivePillStatus.INFO
                            }
                        )
                    }
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (item.body.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (item.place != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.place,
                        style = MaterialTheme.typography.labelSmall,
                        color = OverdriveTheme.colors.primary
                    )
                }
            }

            if (!isSelectionMode) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
