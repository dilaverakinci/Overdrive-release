package com.overdrive.app.ui.vehicle

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.overdrive.app.R
import com.overdrive.app.ui.component.OverdriveButton
import com.overdrive.app.ui.component.OverdriveButtonVariant
import com.overdrive.app.ui.component.OverdriveCard
import com.overdrive.app.ui.component.OverdrivePillStatus
import com.overdrive.app.ui.component.OverdriveStatusPill
import com.overdrive.app.ui.theme.OverdriveDimensions

/**
 * 100% Jetpack Compose Native Vehicle Control Screen.
 * Complete 1:1 replacement for the legacy WebView vehicle-control.html / vehicle-control.js.
 */
@Composable
fun VehicleControlScreen(
    state: VehicleControlUiState,
    onLockClick: () -> Unit = {},
    onUnlockClick: () -> Unit = {},
    onFlashClick: () -> Unit = {},
    onFindCarClick: () -> Unit = {},
    onToggleTrunk: () -> Unit = {},
    onToggleHood: () -> Unit = {},
    onWindowsCloseAll: () -> Unit = {},
    onWindowsOpenAll: () -> Unit = {},
    onWindowsVentMode: () -> Unit = {},
    onToggleSunroof: () -> Unit = {},
    onToggleClimate: () -> Unit = {},
    onTempDown: () -> Unit = {},
    onTempUp: () -> Unit = {},
    onFanDown: () -> Unit = {},
    onFanUp: () -> Unit = {},
    onToggleBatteryHeat: () -> Unit = {},
    onCycleDriverSeatHeat: () -> Unit = {},
    onCycleDriverSeatVent: () -> Unit = {},
    onCyclePassengerSeatHeat: () -> Unit = {},
    onCyclePassengerSeatVent: () -> Unit = {},
    onToggleSteeringHeat: () -> Unit = {},
    onToggleMirrors: () -> Unit = {},
    onRotateScreen: () -> Unit = {},
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
            // Header Bar: Model Name, Lock Status, Cloud Status
            VehicleControlHeader(
                modelName = state.vehicleModelName,
                isLocked = state.security.isLocked,
                isCloudConnected = state.security.isCloudConnected,
                cloudStatusText = state.security.cloudStatusText,
            )

            // Tyre Pressures Strip (4 Corners)
            TyresPressureCard(tyres = state.tyres)

            // Section 1: Security & Locks
            SecurityAndLocksCard(
                isLocked = state.security.isLocked,
                onLockClick = onLockClick,
                onUnlockClick = onUnlockClick,
                onFlashClick = onFlashClick,
                onFindCarClick = onFindCarClick,
            )

            // Section 2: Doors & Trunk
            DoorsAndTrunkCard(
                doors = state.doors,
                onToggleTrunk = onToggleTrunk,
                onToggleHood = onToggleHood,
            )

            // Section 3: Windows & Sunroof
            WindowsControlCard(
                windows = state.windows,
                onCloseAll = onWindowsCloseAll,
                onOpenAll = onWindowsOpenAll,
                onVentMode = onWindowsVentMode,
                onToggleSunroof = onToggleSunroof,
            )

            // Section 4: Climate & HVAC
            ClimateControlCard(
                climate = state.climate,
                onToggleClimate = onToggleClimate,
                onTempDown = onTempDown,
                onTempUp = onTempUp,
                onFanDown = onFanDown,
                onFanUp = onFanUp,
                onToggleBatteryHeat = onToggleBatteryHeat,
            )

            // Section 5: Comfort & Hardware Controls
            ConvenienceAndHardwareCard(
                comfort = state.comfort,
                mirrorsFolded = state.security.mirrorsFolded,
                onCycleDriverHeat = onCycleDriverSeatHeat,
                onCycleDriverVent = onCycleDriverSeatVent,
                onCyclePassengerHeat = onCyclePassengerSeatHeat,
                onCyclePassengerVent = onCyclePassengerSeatVent,
                onToggleSteeringHeat = onToggleSteeringHeat,
                onToggleMirrors = onToggleMirrors,
                onRotateScreen = onRotateScreen,
            )
        }
    }
}

// -----------------------------------------------------------------------------
// HEADER
// -----------------------------------------------------------------------------
@Composable
private fun VehicleControlHeader(
    modelName: String,
    isLocked: Boolean,
    isCloudConnected: Boolean,
    cloudStatusText: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = modelName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.rail_vehicle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OverdriveStatusPill(
                label = if (isLocked) "KİLİTLİ" else "KİLİTSİZ",
                status = if (isLocked) OverdrivePillStatus.SUCCESS else OverdrivePillStatus.WARNING,
            )
            OverdriveStatusPill(
                label = "Bulut: $cloudStatusText",
                status = if (isCloudConnected) OverdrivePillStatus.SUCCESS else OverdrivePillStatus.INFO,
            )
        }
    }
}

// -----------------------------------------------------------------------------
// TYRE PRESSURES CARD
// -----------------------------------------------------------------------------
@Composable
private fun TyresPressureCard(
    tyres: VehicleTyresState,
    modifier: Modifier = Modifier,
) {
    OverdriveCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OverdriveDimensions.cardPaddingStandard)
        ) {
            Text(
                text = "Lastik Basınçları & Sıcaklıkları",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TyreCell(
                    title = "Ön Sol",
                    psi = tyres.flPsi,
                    temp = tyres.flTemp,
                    modifier = Modifier.weight(1f),
                )
                TyreCell(
                    title = "Ön Sağ",
                    psi = tyres.frPsi,
                    temp = tyres.frTemp,
                    modifier = Modifier.weight(1f),
                )
                TyreCell(
                    title = "Arka Sol",
                    psi = tyres.rlPsi,
                    temp = tyres.rlTemp,
                    modifier = Modifier.weight(1f),
                )
                TyreCell(
                    title = "Arka Sağ",
                    psi = tyres.rrPsi,
                    temp = tyres.rrTemp,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TyreCell(
    title: String,
    psi: Float?,
    temp: Int?,
    modifier: Modifier = Modifier,
) {
    val isLow = psi != null && psi < 30.0f
    val bgColor = if (isLow) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (psi != null) String.format("%.1f", psi) else "—",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "PSI",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (temp != null) "$temp°C" else "—",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// -----------------------------------------------------------------------------
// SECURITY & LOCKS CARD
// -----------------------------------------------------------------------------
@Composable
private fun SecurityAndLocksCard(
    isLocked: Boolean,
    onLockClick: () -> Unit,
    onUnlockClick: () -> Unit,
    onFlashClick: () -> Unit,
    onFindCarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OverdriveCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OverdriveDimensions.cardPaddingStandard)
        ) {
            Text(
                text = "Güvenlik & Kilitler",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ActionTile(
                    title = "Kilitle",
                    iconRes = R.drawable.ic_parking,
                    isActive = isLocked,
                    onClick = onLockClick,
                    modifier = Modifier.weight(1f),
                )
                ActionTile(
                    title = "Kilit Aç",
                    iconRes = R.drawable.ic_vehicle_control,
                    isActive = !isLocked,
                    onClick = onUnlockClick,
                    modifier = Modifier.weight(1f),
                )
                ActionTile(
                    title = "Flaşör Yak",
                    iconRes = R.drawable.ic_roadsense,
                    onClick = onFlashClick,
                    modifier = Modifier.weight(1f),
                )
                ActionTile(
                    title = "Aracı Bul",
                    iconRes = R.drawable.ic_roadsense_map,
                    onClick = onFindCarClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// DOORS & TRUNK CARD
// -----------------------------------------------------------------------------
@Composable
private fun DoorsAndTrunkCard(
    doors: VehicleDoorsState,
    onToggleTrunk: () -> Unit,
    onToggleHood: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OverdriveCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OverdriveDimensions.cardPaddingStandard)
        ) {
            Text(
                text = "Kapılar & Bagaj",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Door States Indicator Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DoorStatusPill(label = "Ön Sol", isOpen = doors.frontLeftOpen, modifier = Modifier.weight(1f))
                DoorStatusPill(label = "Ön Sağ", isOpen = doors.frontRightOpen, modifier = Modifier.weight(1f))
                DoorStatusPill(label = "Arka Sol", isOpen = doors.rearLeftOpen, modifier = Modifier.weight(1f))
                DoorStatusPill(label = "Arka Sağ", isOpen = doors.rearRightOpen, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Actions: Trunk & Hood
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ActionTile(
                    title = if (doors.trunkOpen) "Bagajı Kapat" else "Bagajı Aç",
                    iconRes = R.drawable.ic_vehicle_control,
                    isActive = doors.trunkOpen,
                    onClick = onToggleTrunk,
                    modifier = Modifier.weight(1f),
                )
                ActionTile(
                    title = if (doors.hoodOpen) "Kaput Açık" else "Ön Kaput",
                    iconRes = R.drawable.ic_vehicle_control,
                    isActive = doors.hoodOpen,
                    onClick = onToggleHood,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DoorStatusPill(
    label: String,
    isOpen: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isOpen) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (isOpen) "AÇIK" else "KAPALI",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isOpen) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// -----------------------------------------------------------------------------
// WINDOWS & SUNROOF CARD
// -----------------------------------------------------------------------------
@Composable
private fun WindowsControlCard(
    windows: VehicleWindowsState,
    onCloseAll: () -> Unit,
    onOpenAll: () -> Unit,
    onVentMode: () -> Unit,
    onToggleSunroof: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OverdriveCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OverdriveDimensions.cardPaddingStandard)
        ) {
            Text(
                text = "Camlar & Havalandırma",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ActionTile(
                    title = "Tümünü Kapat",
                    iconRes = R.drawable.ic_vehicle_control,
                    onClick = onCloseAll,
                    modifier = Modifier.weight(1f),
                )
                ActionTile(
                    title = "Havalandır",
                    subtitle = "2-3 cm arala",
                    iconRes = R.drawable.ic_vehicle_control,
                    isActive = windows.isVentMode,
                    onClick = onVentMode,
                    modifier = Modifier.weight(1f),
                )
                ActionTile(
                    title = "Tümünü Aç",
                    iconRes = R.drawable.ic_vehicle_control,
                    onClick = onOpenAll,
                    modifier = Modifier.weight(1f),
                )
                ActionTile(
                    title = "Sunroof / Perde",
                    iconRes = R.drawable.ic_vehicle_control,
                    isActive = windows.sunroofOpen,
                    onClick = onToggleSunroof,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// CLIMATE & HVAC CARD
// -----------------------------------------------------------------------------
@Composable
private fun ClimateControlCard(
    climate: VehicleClimateState,
    onToggleClimate: () -> Unit,
    onTempDown: () -> Unit,
    onTempUp: () -> Unit,
    onFanDown: () -> Unit,
    onFanUp: () -> Unit,
    onToggleBatteryHeat: () -> Unit,
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
                Text(
                    text = "Kabin İklimlendirme (Klima)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                OverdriveButton(
                    text = if (climate.isAcOn) "KLİMA AÇIK" else "KLİMA KAPALI",
                    variant = if (climate.isAcOn) OverdriveButtonVariant.PRIMARY else OverdriveButtonVariant.OUTLINED,
                    onClick = onToggleClimate,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Temperature Stepper
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Hedef Sıcaklık",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            IconButton(onClick = onTempDown) {
                                Text("-", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                text = "${climate.targetTemp}°C",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            IconButton(onClick = onTempUp) {
                                Text("+", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // Fan Level Stepper
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Fan Gücü",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            IconButton(onClick = onFanDown) {
                                Text("-", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                text = "${climate.fanLevel} / 7",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            IconButton(onClick = onFanUp) {
                                Text("+", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Battery Pre-Heating Action
            ActionTile(
                title = "Batarya Ön Isıtma",
                subtitle = if (climate.isBatteryHeatOn) "Isıtma Aktif" else "Kapalı",
                iconRes = R.drawable.ic_charging,
                isActive = climate.isBatteryHeatOn,
                onClick = onToggleBatteryHeat,
            )
        }
    }
}

// -----------------------------------------------------------------------------
// CONVENIENCE & HARDWARE CONTROLS
// -----------------------------------------------------------------------------
@Composable
private fun ConvenienceAndHardwareCard(
    comfort: VehicleComfortState,
    mirrorsFolded: Boolean,
    onCycleDriverHeat: () -> Unit,
    onCycleDriverVent: () -> Unit,
    onCyclePassengerHeat: () -> Unit,
    onCyclePassengerVent: () -> Unit,
    onToggleSteeringHeat: () -> Unit,
    onToggleMirrors: () -> Unit,
    onRotateScreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OverdriveCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OverdriveDimensions.cardPaddingStandard)
        ) {
            Text(
                text = "Koltuk & Donanım Kolaylıkları",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Row 1: Seat Comfort
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ActionTile(
                    title = "Sürücü Isıtma",
                    subtitle = seatLevelLabel(comfort.driverSeatHeat),
                    iconRes = R.drawable.ic_seat_positions,
                    isActive = comfort.driverSeatHeat > 0,
                    onClick = onCycleDriverHeat,
                    modifier = Modifier.weight(1f),
                )
                ActionTile(
                    title = "Sürücü Havalandırma",
                    subtitle = seatLevelLabel(comfort.driverSeatVent),
                    iconRes = R.drawable.ic_seat_positions,
                    isActive = comfort.driverSeatVent > 0,
                    onClick = onCycleDriverVent,
                    modifier = Modifier.weight(1f),
                )
                ActionTile(
                    title = "Yolcu Isıtma",
                    subtitle = seatLevelLabel(comfort.passengerSeatHeat),
                    iconRes = R.drawable.ic_seat_positions,
                    isActive = comfort.passengerSeatHeat > 0,
                    onClick = onCyclePassengerHeat,
                    modifier = Modifier.weight(1f),
                )
                ActionTile(
                    title = "Yolcu Havalandırma",
                    subtitle = seatLevelLabel(comfort.passengerSeatVent),
                    iconRes = R.drawable.ic_seat_positions,
                    isActive = comfort.passengerSeatVent > 0,
                    onClick = onCyclePassengerVent,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Steering Heat, Mirror Folding, Screen Rotation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ActionTile(
                    title = "Direksiyon Isıtma",
                    subtitle = if (comfort.steeringHeatOn) "Açık" else "Kapalı",
                    iconRes = R.drawable.ic_vehicle_control,
                    isActive = comfort.steeringHeatOn,
                    onClick = onToggleSteeringHeat,
                    modifier = Modifier.weight(1f),
                )
                ActionTile(
                    title = "Ayna Katlama",
                    subtitle = if (mirrorsFolded) "Katlı" else "Açık",
                    iconRes = R.drawable.ic_vehicle_control,
                    isActive = mirrorsFolded,
                    onClick = onToggleMirrors,
                    modifier = Modifier.weight(1f),
                )
                ActionTile(
                    title = "Döner Ekran",
                    subtitle = "15.6\" Çevir",
                    iconRes = R.drawable.ic_projection,
                    onClick = onRotateScreen,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun seatLevelLabel(level: Int): String {
    return when (level) {
        1 -> "Düşük (1)"
        2 -> "Orta (2)"
        3 -> "Yüksek (3)"
        else -> "Kapalı"
    }
}

@Composable
private fun ActionTile(
    title: String,
    subtitle: String? = null,
    @DrawableRes iconRes: Int,
    isActive: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "TileBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        label = "TileContent"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(OverdriveDimensions.cardRadiusStandard))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .padding(12.dp)
            .height(56.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
