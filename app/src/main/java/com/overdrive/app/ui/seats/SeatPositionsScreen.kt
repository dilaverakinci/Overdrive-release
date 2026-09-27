package com.overdrive.app.ui.seats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import com.overdrive.app.R
import com.overdrive.app.ui.component.OverdriveButton
import com.overdrive.app.ui.component.OverdriveButtonVariant
import com.overdrive.app.ui.component.OverdriveCard
import com.overdrive.app.ui.component.OverdriveDialog
import com.overdrive.app.ui.component.OverdrivePillStatus
import com.overdrive.app.ui.component.OverdriveStatusPill
import com.overdrive.app.ui.theme.OverdriveDimensions

/**
 * 100% Jetpack Compose Native Seat Positions Screen.
 * Complete 1:1 replacement for the legacy WebView /seat-positions page.
 */
@Composable
fun SeatPositionsScreen(
    state: SeatPositionsUiState,
    onApplyPosition: (String) -> Unit = {},
    onSaveHere: (String) -> Unit = {},
    onSaveAsNewClick: () -> Unit = {},
    onCreateConfirm: (name: String, includeGeometry: Boolean, includeAmbient: Boolean) -> Unit = { _, _, _ -> },
    onCreateDismiss: () -> Unit = {},
    onRenameClick: (SavedSeatPosition) -> Unit = {},
    onRenameConfirm: (id: String, newName: String) -> Unit = { _, _ -> },
    onRenameDismiss: () -> Unit = {},
    onDeleteClick: (SavedSeatPosition) -> Unit = {},
    onDeleteConfirm: (String) -> Unit = {},
    onDeleteDismiss: () -> Unit = {},
    onToggleDetails: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(
                    start = OverdriveDimensions.pagePaddingHorizontal,
                    end = OverdriveDimensions.pagePaddingHorizontal,
                    top = OverdriveDimensions.pagePaddingTop,
                    bottom = OverdriveDimensions.pagePaddingBottom,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header Bar
            SeatPositionsHeader(
                isAccOn = state.isCarAccOn,
                isMovementBlocked = state.isMovementBlocked,
                blockReason = state.movementBlockReason,
            )

            // Current Position Hero Card
            CurrentPositionCard(
                matchName = state.currentPositionMatchName,
                axes = state.currentAxes,
                isDetailsExpanded = state.isDetailsExpanded,
                isAccOn = state.isCarAccOn,
                onToggleDetails = onToggleDetails,
                onSaveAsNewClick = onSaveAsNewClick,
            )

            // Section Title: Saved Positions
            Text(
                text = "Kayıtlı Koltuk ve Ayna Konumları",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // List of Saved Positions
            state.savedPositions.forEach { position ->
                SavedPositionCard(
                    position = position,
                    canApply = state.isCarAccOn && !state.isPositioningBlocked,
                    canSaveHere = state.isCarAccOn,
                    isApplying = state.isApplyingPositionId == position.id,
                    onApply = { onApplyPosition(position.id) },
                    onSaveHere = { onSaveHere(position.id) },
                    onRename = { onRenameClick(position) },
                    onDelete = { onDeleteClick(position) },
                )
            }
        }
    }

    // Modal: Save as new position
    if (state.showCreateDialog) {
        CreatePositionDialog(
            onConfirm = onCreateConfirm,
            onDismiss = onCreateDismiss,
        )
    }

    // Modal: Rename position
    state.showRenameDialogFor?.let { position ->
        RenamePositionDialog(
            position = position,
            onConfirm = { newName -> onRenameConfirm(position.id, newName) },
            onDismiss = onRenameDismiss,
        )
    }

    // Modal: Delete position confirmation
    state.showDeleteConfirmFor?.let { position ->
        DeletePositionDialog(
            position = position,
            onConfirm = { onDeleteConfirm(position.id) },
            onDismiss = onDeleteDismiss,
        )
    }
}

// -----------------------------------------------------------------------------
// HEADER
// -----------------------------------------------------------------------------
@Composable
private fun SeatPositionsHeader(
    isAccOn: Boolean,
    isMovementBlocked: Boolean,
    blockReason: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = stringResource(R.string.rail_seat_positions),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Hafızalı Sürücü Koltuğu ve Ayna Geometrisi",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OverdriveStatusPill(
                label = if (isAccOn) "MOTORLAR AKTİF" else "GÜÇ YOK (ACC KAPALI)",
                status = if (isAccOn) OverdrivePillStatus.SUCCESS else OverdrivePillStatus.WARNING,
            )
            if (isMovementBlocked) {
                OverdriveStatusPill(
                    label = if (blockReason == "not_park") "VİTES P'DE DEĞİL" else "HAREKET KİLİTLİ",
                    status = OverdrivePillStatus.DANGER,
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// CURRENT POSITION HERO CARD
// -----------------------------------------------------------------------------
@Composable
private fun CurrentPositionCard(
    matchName: String?,
    axes: SeatAxesState,
    isDetailsExpanded: Boolean,
    isAccOn: Boolean,
    onToggleDetails: () -> Unit,
    onSaveAsNewClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OverdriveCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OverdriveDimensions.cardPaddingStandard)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_seat_positions),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                    }

                    Column {
                        Text(
                            text = "Mevcut Koltuk Konumu",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = matchName ?: "Özel Konum (Hafıza Dışı)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                OverdriveButton(
                    text = "Yeni Olarak Kaydet",
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_copy),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    variant = OverdriveButtonVariant.PRIMARY,
                    enabled = isAccOn,
                    onClick = onSaveAsNewClick,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))

            // Expand / Collapse Details Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onToggleDetails)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (isDetailsExpanded) "Ayrıntılı Eksen Değerlerini Gizle" else "Ayrıntılı Eksen Değerlerini Göster",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = if (isDetailsExpanded) "▲" else "▼",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            AnimatedVisibility(visible = isDetailsExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AxesRow(label = "İleri / Geri", value = axes.horizontal, max = 100)
                    AxesRow(label = "Sırtlık Eğimi", value = axes.backrest, max = 100)
                    AxesRow(label = "Koltuk Yüksekliği", value = axes.height, max = 100)
                    AxesRow(label = "Minder Açısı", value = axes.sitpoint, max = 100)
                    AxesRow(label = "Sol Yan Ayna", value = axes.leftMirrorH, max = 100)
                    AxesRow(label = "Sağ Yan Ayna", value = axes.rightMirrorH, max = 100)
                }
            }
        }
    }
}

@Composable
private fun AxesRow(
    label: String,
    value: Float?,
    max: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (value != null) String.format("%.0f / %d", value, max) else "—",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// -----------------------------------------------------------------------------
// SAVED POSITION CARD
// -----------------------------------------------------------------------------
@Composable
private fun SavedPositionCard(
    position: SavedSeatPosition,
    canApply: Boolean,
    canSaveHere: Boolean,
    isApplying: Boolean,
    onApply: () -> Unit,
    onSaveHere: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OverdriveCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OverdriveDimensions.cardPaddingStandard),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Left: Position details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = position.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (position.slot != null) {
                        OverdriveStatusPill(
                            label = "Yuva ${position.slot}",
                            status = OverdrivePillStatus.INFO,
                        )
                    }
                }

                if (!position.alias.isNullOrBlank()) {
                    Text(
                        text = position.alias,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (position.hasGeometry) {
                        PartTag(label = "Koltuk & Aynalar")
                    }
                    if (position.hasAmbient) {
                        PartTag(label = "Ambiyans")
                    }
                }
            }

            // Right: Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OverdriveButton(
                    text = if (isApplying) "Uygulanıyor..." else "Uygula",
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_seat_positions),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    variant = OverdriveButtonVariant.PRIMARY,
                    enabled = canApply && !isApplying,
                    onClick = onApply,
                )

                OverdriveButton(
                    text = "Buraya Kaydet",
                    variant = OverdriveButtonVariant.OUTLINED,
                    enabled = canSaveHere,
                    onClick = onSaveHere,
                )

                // Overflow / action options
                IconButton(onClick = onRename) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = "Düzenle",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (position.source != "captured") {
                    IconButton(onClick = onDelete) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = "Sil",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PartTag(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// -----------------------------------------------------------------------------
// DIALOGS
// -----------------------------------------------------------------------------
@Composable
private fun CreatePositionDialog(
    onConfirm: (name: String, includeGeometry: Boolean, includeAmbient: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var includeGeometry by remember { mutableStateOf(true) }
    var includeAmbient by remember { mutableStateOf(false) }

    OverdriveDialog(
        onDismissRequest = onDismiss,
        title = "Yeni Konum Olarak Kaydet",
        positiveButtonText = "Kaydet",
        onPositiveClick = {
            if (name.isNotBlank()) {
                onConfirm(name.trim(), includeGeometry, includeAmbient)
            }
        },
        negativeButtonText = "İptal",
        onNegativeClick = onDismiss,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Aracın mevcut koltuk ve ayna ayarlarını yeni bir profil olarak saklayın.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Konum Adı") },
                    placeholder = { Text("Örn: Rahat Yolculuk") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { includeGeometry = !includeGeometry }
                ) {
                    Checkbox(
                        checked = includeGeometry,
                        onCheckedChange = { includeGeometry = it },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Koltuk ve Ayna Geometrisi Dahil Edilsin", style = MaterialTheme.typography.bodyMedium)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { includeAmbient = !includeAmbient }
                ) {
                    Checkbox(
                        checked = includeAmbient,
                        onCheckedChange = { includeAmbient = it },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ortam Aydınlatması Dahil Edilsin", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    )
}

@Composable
private fun RenamePositionDialog(
    position: SavedSeatPosition,
    onConfirm: (newName: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(position.alias ?: position.name) }

    OverdriveDialog(
        onDismissRequest = onDismiss,
        title = "Konumu Yeniden Adlandır",
        positiveButtonText = "Güncelle",
        onPositiveClick = {
            if (name.isNotBlank()) {
                onConfirm(name.trim())
            }
        },
        negativeButtonText = "İptal",
        onNegativeClick = onDismiss,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Yeni Ad") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                )
            }
        }
    )
}

@Composable
private fun DeletePositionDialog(
    position: SavedSeatPosition,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    OverdriveDialog(
        onDismissRequest = onDismiss,
        title = "Konumu Sil",
        positiveButtonText = "Sil",
        onPositiveClick = onConfirm,
        negativeButtonText = "Vazgeç",
        onNegativeClick = onDismiss,
        content = {
            Text(
                text = "'${position.name}' konumunu silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    )
}
