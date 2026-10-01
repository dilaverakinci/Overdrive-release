package com.overdrive.app.ui.automations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
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
import com.overdrive.app.ui.theme.OverdriveDimensions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AutomationsTab(val label: String) {
    AUTOMATIONS("Otomasyonlar"),
    ADD_EDIT("Ekle"),
    SAFETY("Sürüş Güvenliği"),
    ACTION_GROUPS("Eylem Grupları"),
    BACKUP("Yedekleme & Araçlar")
}

data class AutomationItem(
    val id: String,
    val name: String,
    val mode: String, // "automatic", "manual", "disabled"
    val isEnabled: Boolean,
    val triggerText: String,
    val conditionsText: String = "",
    val actionsText: String,
    val delaySeconds: Int = 0,
    val lastTriggered: Long = 0L,
    val triggerCount: Long = 0L
)

data class ActionGroupItem(
    val id: String,
    val name: String,
    val actionsSummary: String,
    val actionCount: Int
)

data class AutomationsUiState(
    val selectedTab: AutomationsTab = AutomationsTab.AUTOMATIONS,
    val automations: List<AutomationItem> = emptyList(),
    val actionGroups: List<ActionGroupItem> = emptyList(),
    val allowShell: Boolean = false,
    val safetyGuards: Map<String, Boolean> = emptyMap(),
    val isLoading: Boolean = false,
    val deleteConfirmId: String? = null,
    val feedbackMessage: String? = null
)

@Composable
fun AutomationsScreen(
    state: AutomationsUiState,
    onTabSelected: (AutomationsTab) -> Unit,
    onToggleAutomation: (String, Boolean) -> Unit,
    onSetMode: (String, String) -> Unit,
    onTestRun: (String) -> Unit,
    onDeleteRequest: (String) -> Unit,
    onDeleteConfirm: (String) -> Unit,
    onDeleteDismiss: () -> Unit,
    onSaveAutomation: (id: String?, name: String, mode: String, triggerType: String, actionType: String, delay: Int) -> Unit,
    onToggleSafetyGuard: (String, Boolean) -> Unit,
    onToggleAllowShell: (Boolean) -> Unit,
    onRunActionGroup: (String) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = OverdriveDimensions.pagePaddingHorizontal,
                    end = OverdriveDimensions.pagePaddingHorizontal,
                    top = OverdriveDimensions.pagePaddingTop,
                    bottom = OverdriveDimensions.pagePaddingBottom
                )
        ) {
            // Header & Sub-Tabs Row
            AutomationsHeader(
                selectedTab = state.selectedTab,
                onTabSelected = onTabSelected,
                totalCount = state.automations.size,
                activeCount = state.automations.count { it.isEnabled }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Body Content based on Tab
            Box(modifier = Modifier.weight(1f)) {
                when (state.selectedTab) {
                    AutomationsTab.AUTOMATIONS -> {
                        AutomationsListContent(
                            automations = state.automations,
                            allowShell = state.allowShell,
                            onToggleAllowShell = onToggleAllowShell,
                            onToggle = onToggleAutomation,
                            onTestRun = onTestRun,
                            onDelete = onDeleteRequest,
                            onAddNewClick = { onTabSelected(AutomationsTab.ADD_EDIT) }
                        )
                    }
                    AutomationsTab.ADD_EDIT -> {
                        AddEditAutomationContent(
                            onSave = { name, mode, trigger, action, delay ->
                                onSaveAutomation(null, name, mode, trigger, action, delay)
                                onTabSelected(AutomationsTab.AUTOMATIONS)
                            },
                            onCancel = { onTabSelected(AutomationsTab.AUTOMATIONS) }
                        )
                    }
                    AutomationsTab.SAFETY -> {
                        SafetyGuardsContent(
                            guards = state.safetyGuards,
                            onToggleGuard = onToggleSafetyGuard
                        )
                    }
                    AutomationsTab.ACTION_GROUPS -> {
                        ActionGroupsContent(
                            groups = state.actionGroups,
                            onRunGroup = onRunActionGroup
                        )
                    }
                    AutomationsTab.BACKUP -> {
                        BackupAndToolsContent(
                            totalAutomations = state.automations.size,
                            onExport = onExportBackup,
                            onImport = onImportBackupClick
                        )
                    }
                }

                if (state.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (state.deleteConfirmId != null) {
        val target = state.automations.firstOrNull { it.id == state.deleteConfirmId }
        OverdriveDialog(
            title = "Otomasyonu Sil",
            onDismissRequest = onDeleteDismiss,
            positiveButtonText = "Sil",
            onPositiveClick = { onDeleteConfirm(state.deleteConfirmId) },
            negativeButtonText = "İptal",
            onNegativeClick = onDeleteDismiss
        ) {
            Text(
                text = "'${target?.name ?: "Seçili otomasyon"}' kalıcı olarak silinecek. Emin misiniz?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// -----------------------------------------------------------------------------
// HEADER & TABS
// -----------------------------------------------------------------------------
@Composable
private fun AutomationsHeader(
    selectedTab: AutomationsTab,
    onTabSelected: (AutomationsTab) -> Unit,
    totalCount: Int,
    activeCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_automations),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = stringResource(R.string.rail_automations),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$activeCount / $totalCount aktif kural",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Sub-Tab Switcher Pills
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AutomationsTab.values().forEach { tab ->
                val isSelected = tab == selectedTab
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    label = "tab_bg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "tab_text"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(bgColor)
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 1: AUTOMATIONS LIST
// -----------------------------------------------------------------------------
@Composable
private fun AutomationsListContent(
    automations: List<AutomationItem>,
    allowShell: Boolean,
    onToggleAllowShell: (Boolean) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onTestRun: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAddNewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Quick Action Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kayıtlı Otomasyonlar (${automations.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OverdriveButton(
                    text = "Yeni Otomasyon",
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    variant = OverdriveButtonVariant.PRIMARY,
                    onClick = onAddNewClick
                )
            }
        }

        // Shell Permission Info Card
        item {
            OverdriveCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 12.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_console),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kabuk (Shell) Eylemlerine İzin Ver",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Otomasyonların arka planda doğrudan Linux kabuk komutları çalıştırmasını sağlar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Switch(
                        checked = allowShell,
                        onCheckedChange = onToggleAllowShell,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.surface,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    )
                }
            }
        }

        if (automations.isEmpty()) {
            item {
                OverdriveCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentPadding = 12.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_automations),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Henüz tanımlı bir otomasyon yok",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Araç olaylarına (vites, kontak, kapı, şarj vb.) göre otomatik eylemler oluşturun.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OverdriveButton(
                            text = "Otomasyon Oluştur",
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_add),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            variant = OverdriveButtonVariant.PRIMARY,
                            onClick = onAddNewClick
                        )
                    }
                }
            }
        } else {
            items(items = automations, key = { it.id }) { item ->
                AutomationCard(
                    item = item,
                    onToggle = { isChecked -> onToggle(item.id, isChecked) },
                    onTestRun = { onTestRun(item.id) },
                    onDelete = { onDelete(item.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun AutomationCard(
    item: AutomationItem,
    onToggle: (Boolean) -> Unit,
    onTestRun: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusDotColor = when {
        item.mode == "disabled" || !item.isEnabled -> MaterialTheme.colorScheme.outline
        item.mode == "manual" -> Color(0xFFF9A825)
        else -> Color(0xFF00D4AA)
    }

    val lastRunText = if (item.lastTriggered > 0L) {
        val sdf = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
        "Son çalışma: ${sdf.format(Date(item.lastTriggered))} • ${item.triggerCount} kez"
    } else {
        "Henüz tetiklenmedi"
    }

    OverdriveCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 12.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusDotColor)
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Name & Stats
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name.ifEmpty { "İsimsiz Otomasyon" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = lastRunText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Test Run button
                IconButton(
                    onClick = onTestRun,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_play_circle),
                        contentDescription = "Test Çalıştır",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = "Sil",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Enable/Disable switch
                Switch(
                    checked = item.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.surface,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // Trigger -> Action flow chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Trigger Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Tetik: ${item.triggerText}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Icon(
                    painter = painterResource(R.drawable.ic_arrow_outward),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )

                // Actions Chip
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Eylem: ${item.actionsText}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 2: ADD / EDIT AUTOMATION FORM
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditAutomationContent(
    onSave: (name: String, mode: String, triggerType: String, actionType: String, delay: Int) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf("automatic") }
    var delaySecs by remember { mutableStateOf("0") }

    // Dropdown state for triggers
    val triggers = listOf(
        "gear_p" to "Vites: Park (P) Moduna Alındı",
        "gear_d" to "Vites: Sürüş (D) Moduna Alındı",
        "gear_r" to "Vites: Geri (R) Moduna Alındı",
        "doors_locked" to "Kapılar: Kilitlendi",
        "doors_unlocked" to "Kapılar: Kilit Açıldı",
        "acc_on" to "Araç Gücü: Kontak / ACC Açıldı",
        "acc_off" to "Araç Gücü: Kontak / ACC Kapandı",
        "charging_start" to "Şarj: Şarj Başladı",
        "charging_stop" to "Şarj: Şarj Bitti / Durdu"
    )
    var expandedTrigger by remember { mutableStateOf(false) }
    var selectedTrigger by remember { mutableStateOf(triggers[0]) }

    // Dropdown state for actions
    val actions = listOf(
        "mirror_fold" to "Yan Aynaları Katla",
        "mirror_unfold" to "Yan Aynaları Aç",
        "seat_pos_1" to "Sürücü Koltuğu 1. Konuma Git",
        "seat_pos_2" to "Sürücü Koltuğu 2. Konuma Git",
        "screen_off" to "Merkezi Ekranı Kapat",
        "screen_on" to "Merkezi Ekranı Aç",
        "play_welcome" to "Karşılama Sesi Çal",
        "doors_lock" to "Tüm Kapıları Kilitle",
        "doors_unlock" to "Tüm Kapıların Kilidini Aç"
    )
    var expandedAction by remember { mutableStateOf(false) }
    var selectedAction by remember { mutableStateOf(actions[0]) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Yeni Otomasyon Kuralı Tanımla",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Kural Adı (Örn: Park Edince Aynaları Katla)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Execution Mode Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Çalışma Modu",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "automatic" to "Otomatik",
                            "manual" to "Yalnızca Manuel",
                            "disabled" to "Devre Dışı"
                        ).forEach { (m, label) ->
                            val isSel = selectedMode == m
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSel) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceContainerHighest
                                    )
                                    .clickable { selectedMode = m }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Trigger Selector
                ExposedDropdownMenuBox(
                    expanded = expandedTrigger,
                    onExpandedChange = { expandedTrigger = !expandedTrigger }
                ) {
                    OutlinedTextField(
                        value = selectedTrigger.second,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tetikleyici Olay (Araç Sinyali)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTrigger) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTrigger,
                        onDismissRequest = { expandedTrigger = false }
                    ) {
                        triggers.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.second) },
                                onClick = {
                                    selectedTrigger = item
                                    expandedTrigger = false
                                }
                            )
                        }
                    }
                }

                // Action Selector
                ExposedDropdownMenuBox(
                    expanded = expandedAction,
                    onExpandedChange = { expandedAction = !expandedAction }
                ) {
                    OutlinedTextField(
                        value = selectedAction.second,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Yürütülecek Eylem") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAction) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedAction,
                        onDismissRequest = { expandedAction = false }
                    ) {
                        actions.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.second) },
                                onClick = {
                                    selectedAction = item
                                    expandedAction = false
                                }
                            )
                        }
                    }
                }

                // Delay
                OutlinedTextField(
                    value = delaySecs,
                    onValueChange = { delaySecs = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Gecikme (Saniye)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OverdriveButton(
                        text = "İptal",
                        variant = OverdriveButtonVariant.TONAL,
                        onClick = onCancel
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    OverdriveButton(
                        text = "Otomasyonu Kaydet",
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        variant = OverdriveButtonVariant.PRIMARY,
                        onClick = {
                            val finalName = name.ifBlank { "${selectedTrigger.second} ➔ ${selectedAction.second}" }
                            val delayInt = delaySecs.toIntOrNull() ?: 0
                            onSave(finalName, selectedMode, selectedTrigger.first, selectedAction.first, delayInt)
                        }
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 3: SAFETY GUARDS
// -----------------------------------------------------------------------------
@Composable
private fun SafetyGuardsContent(
    guards: Map<String, Boolean>,
    onToggleGuard: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Warning Banner
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_warning),
                    contentDescription = null,
                    tint = Color(0xFFF9A825),
                    modifier = Modifier
                        .size(24.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Sürüş Güvenliği Muhafızları",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Bu muhafızlar araç hareket halindeyken veya durumu belirsizken kritik eylemleri engeller. Bir muhafızı kapatmak, aracın sürüş anında o eylemi gerçekleştirmesine izin verir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // List of Guards
        val guardList = listOf(
            Triple("doorLocks", "Kapı Kilitleme ve Açma", "Araç güvenli bir şekilde Park (P) durumunda olmadıkça kilitlerin değiştirilmesini engeller."),
            Triple("trunk", "Bagaj Kapağı Hareketi", "Sürüş sırasında bagaj kapağının açılmasını veya kapanmasını engeller."),
            Triple("mirrorFold", "Yan Aynaları Katlama", "Araç hareket halindeyken dış aynaların katlanmasını engeller. (Açma her zaman serbesttir)."),
            Triple("positioning", "Koltuk ve Ayna Hafıza Konumu", "Sürüş anında kayıtlı koltuk hafızasının çağrılmasını ve motor hareketini engeller."),
            Triple("headlightOff", "Farları Tamamen Kapatma", "Sürüş anında farların 'Kapalı' konuma getirilmesini engeller. (Otomatik ve Park serbesttir)."),
            Triple("displayBrightness", "Ekran Parlaklığı Değişimi", "Sürüş anında multimedya ve gösterge parlaklığının aniden kısılmasını engeller."),
            Triple("displayPower", "Ekran Gücünü Kapatma", "Sürüş sırasında gösterge veya orta ekranın tamamen karartılmasını engeller."),
            Triple("screenMedia", "Tam Ekran Video / Medya", "Sürüş esnasında dikkati dağıtabilecek tam ekran video oynatımını engeller.")
        )

        guardList.forEach { (key, title, desc) ->
            val isChecked = guards[key] ?: true
            OverdriveCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 12.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = isChecked,
                        onCheckedChange = { onToggleGuard(key, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.surface,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// -----------------------------------------------------------------------------
// TAB 4: ACTION GROUPS
// -----------------------------------------------------------------------------
@Composable
private fun ActionGroupsContent(
    groups: List<ActionGroupItem>,
    onRunGroup: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Tekrar Kullanılabilir Eylem Grupları (${groups.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (groups.isEmpty()) {
            item {
                OverdriveCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentPadding = 12.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Tanımlı eylem grubu bulunmuyor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Birden fazla eylemi tek bir grupta toplayıp tuş eşleme ve otomasyonlardan çağırabilirsiniz.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(groups, key = { it.id }) { group ->
                OverdriveCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = 12.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${group.actionCount} eylem • ${group.actionsSummary}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OverdriveButton(
                            text = "Çalıştır",
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_play_circle),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            variant = OverdriveButtonVariant.TONAL,
                            onClick = { onRunGroup(group.id) }
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 5: BACKUP & TOOLS
// -----------------------------------------------------------------------------
@Composable
private fun BackupAndToolsContent(
    totalAutomations: Int,
    onExport: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Yedekleme ve Geri Yükleme",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tüm otomasyonlarınızı JSON formatında dışa aktarabilir veya daha önce kaydettiğiniz bir yedekten geri yükleyebilirsiniz.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OverdriveButton(
                        text = "Dışa Aktar ($totalAutomations)",
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_backup_export),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        variant = OverdriveButtonVariant.PRIMARY,
                        onClick = onExport,
                        modifier = Modifier.weight(1f)
                    )
                    OverdriveButton(
                        text = "Yedekten İçe Aktar",
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_backup_import),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        variant = OverdriveButtonVariant.TONAL,
                        onClick = onImport,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 16.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Ses Kütüphanesi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "'Ses Çal' eylemi için araç belleğinde MP3/WAV/AAC formatında özel ses dosyaları kullanılabilir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
