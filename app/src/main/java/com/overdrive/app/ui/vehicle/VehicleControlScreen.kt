package com.overdrive.app.ui.vehicle

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.overdrive.app.daemon.CameraDaemon
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.overdrive.app.ui.component.OverdriveSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.overdrive.app.R
import com.overdrive.app.domain.model.Gear
import com.overdrive.app.domain.model.OperationMode
import com.overdrive.app.ui.component.ChassisEnergyFlowView
import com.overdrive.app.ui.component.CircularSpeedometerView
import com.overdrive.app.ui.component.OverdriveButton
import com.overdrive.app.ui.component.OverdriveButtonVariant
import com.overdrive.app.ui.component.OverdriveCard
import com.overdrive.app.ui.component.OverdrivePillStatus
import com.overdrive.app.ui.component.OverdriveStatusPill
import com.overdrive.app.ui.theme.OverdriveDimensions
import com.overdrive.app.ui.theme.OverdriveTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * 100% Jetpack Compose Native Vehicle Cockpit & Control Screen.
 *
 * Integrated Cockpit Layout:
 * - SOLDA (Left Column): Canlı hız/güç kadranı (CircularSpeedometerView) with drive mode, gear, and live power bar.
 * - ORTADA (Center Column): 2D Model Render (VehicleArt) with [3D] / [2D] toggle button, and directly beneath it the Batarya / Menzil Kartı.
 * - SAĞDA (Right Column): 4 tekerlek bağımsız lastik basınç/sıcaklık göstergesi (ChassisEnergyFlowView) with live cutaway energy stream.
 * - ALTINDA (Bottom Section): 10-tab vehicle controls matching the legacy PWA dock and media screenshot.
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
    onTabSelected: (VehicleControlTab) -> Unit = {},
    onToggle3DMode: () -> Unit = {},
    onToggleDrl: () -> Unit = {},
    onSelectAmbientColor: (Int) -> Unit = {},
    onToggleSlw: () -> Unit = {},
    onToggleCpd: () -> Unit = {},
    onStartCharging: () -> Unit = {},
    onToggleSmartCharge: () -> Unit = {},
    onSetChargeCap: (Int) -> Unit = {},
    onSetAcCurrentLimit: (Int) -> Unit = {},
    onPlayAvasTone: (Int) -> Unit = {},
    onStopAvas: () -> Unit = {},
    onToggleEngineSound: () -> Unit = {},
    onRebootIvi: () -> Unit = {},
    onSelectDriveMode: (OperationMode) -> Unit = {},
    onSelectModelClick: () -> Unit = {},
    onNavigateToCharging: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var expandedTab by remember { mutableStateOf<VehicleControlTab?>(null) }
    var chassisCarouselPage by remember { mutableIntStateOf(0) }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val screenBg = MaterialTheme.colorScheme.background

    Surface(
        modifier = modifier.fillMaxSize(),
        color = screenBg,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // LAYER 1: 100% Single-Page Cockpit (Zero scrolling needed, fits entire screen)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // 1. Sub-Header Row: [ ● NO DATA ]   [ Model Seç ▾ ] [ ⬡ 3D ]   [ ● BYD account not connected ]
                VehicleControlSubHeader(
                    isLocked = state.security.isLocked,
                    is3DMode = state.is3DMode,
                    isCloudConnected = state.security.isCloudConnected,
                    modelName = state.vehicleModelName,
                    onToggle3DMode = onToggle3DMode,
                    onSelectModelClick = onSelectModelClick,
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 3. Cockpit 3-Card Row: SOLDA (Speedometer), ORTADA (2D Car + Battery), SAĞDA (Chassis Telemetry)
                // Exactly matches media_1790944050816.png
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // SOLDA: Canlı Enerji & Sürüş Akışı Card
                    LiveEnergyCockpitCard(
                        powertrain = state.powertrain,
                        onSelectDriveMode = onSelectDriveMode,
                        modifier = Modifier
                            .weight(1.30f)
                            .fillMaxHeight(),
                    )

                    // ORTADA: 2D Model Render (VehicleArt) / 3D Model + Batarya, Menzil & Tüketim Card (Directly Below)
                    VehicleCenterModelAndBatteryCard(
                        modelId = state.selectedModelId,
                        modelName = state.vehicleModelName,
                        is3DMode = state.is3DMode,
                        battery = state.battery,
                        onNavigateToCharging = onNavigateToCharging,
                        onSelectModelClick = onSelectModelClick,
                        modifier = Modifier
                            .weight(1.70f)
                            .fillMaxHeight(),
                    )

                    // SAĞDA: Şasi & Güç Telemetrisi Card
                    ChassisTelemetryCockpitCard(
                        tyres = state.tyres,
                        doors = state.doors,
                        windows = state.windows,
                        battery = state.battery,
                        powertrain = state.powertrain,
                        health = state.health,
                        isAwd = state.isAwd,
                        carouselPage = chassisCarouselPage,
                        onPageChange = { chassisCarouselPage = it },
                        modifier = Modifier
                            .weight(1.0f)
                            .fillMaxHeight(),
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 4. ALTINDA: 10-Tab Category Dock Bar (Always visible at bottom, never pushed below fold)
                VehicleControlDockBar(
                    selectedTab = state.selectedTab,
                    onTabSelected = { tab ->
                        expandedTab = if (expandedTab == tab) null else tab
                        onTabSelected(tab)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                )
            }

            // LAYER 2: Floating Controls Overlay (Opens above dock without pushing or scrolling page)
            AnimatedVisibility(
                visible = expandedTab != null,
                enter = fadeIn(animationSpec = tween(180)),
                exit = fadeOut(animationSpec = tween(140)),
                modifier = Modifier.fillMaxSize(),
            ) {
                val activeTab = expandedTab ?: state.selectedTab
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.32f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            expandedTab = null
                        },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = 62.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { /* Consume taps to avoid closing overlay */ }
                    ) {
                        VehicleControlsExpandedCard(
                            state = state,
                            selectedTab = activeTab,
                            onClose = { expandedTab = null },
                            onLockClick = onLockClick,
                            onUnlockClick = onUnlockClick,
                            onFlashClick = onFlashClick,
                            onFindCarClick = onFindCarClick,
                            onToggleTrunk = onToggleTrunk,
                            onToggleHood = onToggleHood,
                            onWindowsCloseAll = onWindowsCloseAll,
                            onWindowsOpenAll = onWindowsOpenAll,
                            onWindowsVentMode = onWindowsVentMode,
                            onToggleSunroof = onToggleSunroof,
                            onToggleClimate = onToggleClimate,
                            onTempDown = onTempDown,
                            onTempUp = onTempUp,
                            onFanDown = onFanDown,
                            onFanUp = onFanUp,
                            onToggleBatteryHeat = onToggleBatteryHeat,
                            onCycleDriverSeatHeat = onCycleDriverSeatHeat,
                            onCycleDriverSeatVent = onCycleDriverSeatVent,
                            onCyclePassengerSeatHeat = onCyclePassengerSeatHeat,
                            onCyclePassengerSeatVent = onCyclePassengerSeatVent,
                            onToggleSteeringHeat = onToggleSteeringHeat,
                            onToggleMirrors = onToggleMirrors,
                            onRotateScreen = onRotateScreen,
                            onToggleDrl = onToggleDrl,
                            onSelectAmbientColor = onSelectAmbientColor,
                            onToggleSlw = onToggleSlw,
                            onToggleCpd = onToggleCpd,
                            onStartCharging = onStartCharging,
                            onToggleSmartCharge = onToggleSmartCharge,
                            onSetChargeCap = onSetChargeCap,
                            onSetAcCurrentLimit = onSetAcCurrentLimit,
                            onPlayAvasTone = onPlayAvasTone,
                            onStopAvas = onStopAvas,
                            onToggleEngineSound = onToggleEngineSound,
                            onRebootIvi = onRebootIvi,
                        )
                    }
                }
            }
        }
    }
}


// -----------------------------------------------------------------------------
// 2. SUB-HEADER ROW
// -----------------------------------------------------------------------------
@Composable
private fun VehicleControlSubHeader(
    isLocked: Boolean?,
    is3DMode: Boolean,
    isCloudConnected: Boolean,
    modelName: String,
    onToggle3DMode: () -> Unit,
    onSelectModelClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val pillBg = MaterialTheme.colorScheme.surfaceContainer
    val pillBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val btnBg = MaterialTheme.colorScheme.surfaceContainer
    val btnBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Left Pill: [ ● NO DATA ]
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = pillBg,
            border = pillBorder,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val bulletColor = when (isLocked) {
                    true -> OverdriveTheme.colors.statusSuccess
                    false -> OverdriveTheme.colors.statusWarning
                    null -> MaterialTheme.colorScheme.outline
                }
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(bulletColor)
                )
                Text(
                    text = when (isLocked) {
                        true -> "LOCKED"
                        false -> "UNLOCKED"
                        null -> "NO DATA"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    letterSpacing = 0.5.sp,
                )
            }
        }

        // Center Controls: Model Selector Pill + 3D/2D Toggle Button (Matches vehicle-control.html mid-slot)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                onClick = onSelectModelClick,
                shape = RoundedCornerShape(8.dp),
                color = btnBg,
                border = btnBorder,
                modifier = Modifier.height(30.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (modelName.isNotEmpty()) modelName else "Model Seçin",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            Surface(
                onClick = onToggle3DMode,
                shape = RoundedCornerShape(8.dp),
                color = btnBg,
                border = btnBorder,
                modifier = Modifier.height(30.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_map_3d),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = if (is3DMode) "2D" else "3D",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                    )
                }
            }
        }

        // Right Pill: [ ● BYD account not connected ]
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = pillBg,
            border = pillBorder,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isCloudConnected) OverdriveTheme.colors.statusSuccess else MaterialTheme.colorScheme.outline)
                )
                Text(
                    text = if (isCloudConnected) "BYD account connected" else "BYD account not connected",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 3A. SOLDA: CANLI ENERJİ & SÜRÜŞ AKIŞI KARTI
// -----------------------------------------------------------------------------
@Composable
private fun LiveEnergyCockpitCard(
    powertrain: VehiclePowertrainUiState,
    onSelectDriveMode: (OperationMode) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val cardBg = MaterialTheme.colorScheme.surfaceContainer
    val cardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val capsuleBg = MaterialTheme.colorScheme.surfaceContainerLow
    val capsuleBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = cardBorder,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Header Row: ⚡ CANLI ENERJİ & SÜRÜŞ AKIŞI
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_energy_bolt),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "CANLI ENERJİ & SÜRÜŞ AKIŞI",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.5.sp,
                        letterSpacing = 0.5.sp,
                    )
                    Text(
                        text = "Rejenerasyon ve Güç Tüketim Monitörü",
                        style = MaterialTheme.typography.labelSmall,
                        color = textSecondary,
                        fontSize = 9.sp,
                    )
                }
            }

            // Drive Mode Selector: ECO | NORMAL | SPORT | KAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                DriveModePill(
                    label = "ECO",
                    iconRes = R.drawable.ic_drive_eco,
                    isActive = powertrain.operationMode == OperationMode.ECO,
                    onClick = { onSelectDriveMode(OperationMode.ECO) },
                    modifier = Modifier.weight(1f),
                )
                DriveModePill(
                    label = "NORMAL",
                    iconRes = R.drawable.ic_drive_normal,
                    isActive = powertrain.operationMode == OperationMode.NORMAL || powertrain.operationMode == OperationMode.UNKNOWN,
                    onClick = { onSelectDriveMode(OperationMode.NORMAL) },
                    modifier = Modifier.weight(1.15f),
                )
                DriveModePill(
                    label = "SPORT",
                    iconRes = R.drawable.ic_drive_sport,
                    isActive = powertrain.operationMode == OperationMode.SPORT,
                    onClick = { onSelectDriveMode(OperationMode.SPORT) },
                    modifier = Modifier.weight(1f),
                )
                DriveModePill(
                    label = "KAR",
                    iconRes = R.drawable.ic_drive_snow,
                    isActive = powertrain.operationMode == OperationMode.SNOW,
                    onClick = { onSelectDriveMode(OperationMode.SNOW) },
                    modifier = Modifier.weight(1f),
                )
            }

            // Circular Speedometer Gauge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                AndroidView(
                    factory = { ctx ->
                        CircularSpeedometerView(ctx).apply {
                            setDarkTheme(isDark)
                            setDriveMode(powertrain.operationMode)
                            setSpeedKmh(powertrain.speedKmh.toFloat())
                        }
                    },
                    update = { view ->
                        view.setDarkTheme(isDark)
                        view.setDriveMode(powertrain.operationMode)
                        view.setSpeedKmh(powertrain.speedKmh.toFloat())
                    },
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f),
                )
            }

            // Gear Selector Capsules: [ P ] [ R ] [ N ] [ D ]   (P)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = capsuleBg,
                    border = capsuleBorder,
                    modifier = Modifier.padding(end = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GearCircle(label = "P", isSelected = powertrain.gear == Gear.P, activeColor = OverdriveTheme.colors.gearPark)
                        GearCircle(label = "R", isSelected = powertrain.gear == Gear.R, activeColor = OverdriveTheme.colors.gearReverse)
                        GearCircle(label = "N", isSelected = powertrain.gear == Gear.N, activeColor = OverdriveTheme.colors.gearNeutral)
                        GearCircle(label = "D", isSelected = powertrain.gear == Gear.D || powertrain.gear == Gear.S || powertrain.gear == Gear.M, activeColor = OverdriveTheme.colors.gearDrive)
                    }
                }

                // Electric Park Brake circle (P)
                val epbBg = if (powertrain.gear == Gear.P) OverdriveTheme.colors.statusDangerContainer else capsuleBg
                val epbBorderColor = if (powertrain.gear == Gear.P) OverdriveTheme.colors.statusDanger else MaterialTheme.colorScheme.outlineVariant
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(epbBg)
                        .border(1.dp, epbBorderColor, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "(P)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (powertrain.gear == Gear.P) OverdriveTheme.colors.statusDanger else textSecondary,
                        fontSize = 10.sp,
                    )
                }
            }

            // Dual Direction Power Meter
            LivePowerMeterBar(
                powerKw = powertrain.powerKw,
                operationMode = powertrain.operationMode,
            )

            // Dual Direction Pedal Meter
            LivePedalMeterBar(
                accelPercent = powertrain.accelPedalPercent,
                brakePercent = powertrain.brakePedalPercent,
                operationMode = powertrain.operationMode,
            )
        }
    }
}

@Composable
private fun DriveModePill(
    label: String,
    @DrawableRes iconRes: Int,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val inactiveBg = MaterialTheme.colorScheme.surfaceContainerLow
    val inactiveBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val inactiveText = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else inactiveBg,
        border = if (isActive) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else inactiveBorder,
        modifier = modifier.height(26.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else inactiveText,
                modifier = Modifier.size(12.dp),
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                fontSize = 9.5.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else inactiveText,
            )
        }
    }
}

@Composable
private fun GearCircle(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
) {
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (isSelected) activeColor else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isSelected) Color.White else unselectedColor,
        )
    }
}

@Composable
private fun LivePowerMeterBar(
    powerKw: Double,
    operationMode: OperationMode,
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val isPower = powerKw > 0.05
    val isRegen = powerKw < -0.05
    val regenText = if (isRegen) String.format(Locale.US, "%.1f kW", abs(powerKw)).replace('.', ',') else "0,0 kW"
    val powerText = if (isPower) String.format(Locale.US, "+%.1f kW", powerKw).replace('.', ',') else "+0,0 kW"

    val modeColor = when (operationMode) {
        OperationMode.ECO -> OverdriveTheme.colors.statusSuccess
        OperationMode.NORMAL -> MaterialTheme.colorScheme.primary
        OperationMode.SPORT -> OverdriveTheme.colors.statusDanger
        OperationMode.SNOW -> OverdriveTheme.colors.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    val regenFillColor = OverdriveTheme.colors.statusSuccess
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val tickColor = MaterialTheme.colorScheme.outline
    val scaleColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        // Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "◀ REGEN $regenText",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = textSecondary,
            )
            Text(
                text = "GÜÇ $powerText ▶",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Canvas Track & Fill
        val targetFraction = when {
            powerKw < -0.05 -> (abs(powerKw) / 50.0).coerceIn(0.0, 1.0).toFloat()
            powerKw > 0.05 -> (powerKw / 150.0).coerceIn(0.0, 1.0).toFloat()
            else -> 0f
        }
        val animatedFraction by animateFloatAsState(
            targetValue = targetFraction,
            animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
            label = "powerFraction"
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        ) {
            val w = size.width
            val h = size.height
            val r = h / 2f
            val cx = w / 2f

            // Track background
            drawRoundRect(
                color = trackColor,
                size = size,
                cornerRadius = CornerRadius(r, r),
            )

            // Active fill
            if (powerKw < -0.05) {
                val fillW = animatedFraction * cx
                drawRoundRect(
                    color = regenFillColor,
                    topLeft = Offset(cx - fillW, 0f),
                    size = Size(fillW, h),
                    cornerRadius = CornerRadius(r, r),
                )
            } else if (powerKw > 0.05) {
                val fillW = animatedFraction * cx
                drawRoundRect(
                    color = modeColor,
                    topLeft = Offset(cx, 0f),
                    size = Size(fillW, h),
                    cornerRadius = CornerRadius(r, r),
                )
            }

            // Center tick line
            drawLine(
                color = tickColor,
                start = Offset(cx, -1.dp.toPx()),
                end = Offset(cx, h + 1.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Scale labels row: -50   -25   0 kW   +50   +100   +150
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("-50", fontSize = 7.5.sp, color = scaleColor)
            Text("-25", fontSize = 7.5.sp, color = scaleColor)
            Text("0 kW", fontSize = 7.5.sp, color = scaleColor, fontWeight = FontWeight.Bold)
            Text("+50", fontSize = 7.5.sp, color = scaleColor)
            Text("+100", fontSize = 7.5.sp, color = scaleColor)
            Text("+150", fontSize = 7.5.sp, color = scaleColor)
        }
    }
}

@Composable
private fun LivePedalMeterBar(
    accelPercent: Int,
    brakePercent: Int,
    operationMode: OperationMode,
    modifier: Modifier = Modifier,
) {
    val modeColor = when (operationMode) {
        OperationMode.ECO -> OverdriveTheme.colors.statusSuccess
        OperationMode.NORMAL -> MaterialTheme.colorScheme.primary
        OperationMode.SPORT -> OverdriveTheme.colors.statusDanger
        OperationMode.SNOW -> OverdriveTheme.colors.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    val brakeFillColor = OverdriveTheme.colors.statusDanger
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val tickColor = MaterialTheme.colorScheme.outline
    val scaleColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        // Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "◀ FREN %$brakePercent",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = textSecondary,
            )
            Text(
                text = "GAZ %$accelPercent ▶",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = textSecondary,
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        val brakeFraction by animateFloatAsState(
            targetValue = (brakePercent / 100f).coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
            label = "brakeFraction"
        )
        val accelFraction by animateFloatAsState(
            targetValue = (accelPercent / 100f).coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
            label = "accelFraction"
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        ) {
            val w = size.width
            val h = size.height
            val r = h / 2f
            val cx = w / 2f

            // Track background
            drawRoundRect(
                color = trackColor,
                size = size,
                cornerRadius = CornerRadius(r, r),
            )

            // Brake (Left)
            if (brakeFraction > 0f) {
                val fillW = brakeFraction * cx
                drawRoundRect(
                    color = brakeFillColor,
                    topLeft = Offset(cx - fillW, 0f),
                    size = Size(fillW, h),
                    cornerRadius = CornerRadius(r, r),
                )
            }

            // Accel (Right)
            if (accelFraction > 0f) {
                val fillW = accelFraction * cx
                drawRoundRect(
                    color = modeColor,
                    topLeft = Offset(cx, 0f),
                    size = Size(fillW, h),
                    cornerRadius = CornerRadius(r, r),
                )
            }

            // Center tick line
            drawLine(
                color = tickColor,
                start = Offset(cx, -1.dp.toPx()),
                end = Offset(cx, h + 1.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Scale labels row: Fren %100   %50   0   %50   Gaz %100
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Fren %100", fontSize = 7.5.sp, color = scaleColor)
            Text("%50", fontSize = 7.5.sp, color = scaleColor)
            Text("0", fontSize = 7.5.sp, color = scaleColor, fontWeight = FontWeight.Bold)
            Text("%50", fontSize = 7.5.sp, color = scaleColor)
            Text("Gaz %100", fontSize = 7.5.sp, color = scaleColor)
        }
    }
}

// -----------------------------------------------------------------------------
// 3B. ORTADA: 2D MODEL RESMİ / 3D MODEL PENCERESİ + BATARYA & MENZİL KARTI (ALTINDA)
// -----------------------------------------------------------------------------
@Composable
private fun Vehicle3DModelViewport(
    modelId: String?,
    modelName: String,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        // Embedded 3D Three.js WebGL WebView
        AndroidView(
            factory = { context ->
                try {
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            allowFileAccess = true
                        }
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                view?.evaluateJavascript(
                                    "(function() {" +
                                    "  try {" +
                                    "    localStorage.setItem('vc.viewMode', 'immersive');" +
                                    "    if (window.VC && window.VC.switchViewMode && window.VC.liteMode) {" +
                                    "      window.VC.switchViewMode();" +
                                    "    }" +
                                    "    var css = '.vc-status-bar, .vc-bar, .vc-nav-btn, #app-shell-mount, .app-shell-sidebar, .sidebar, #sidebarOverlay { display: none !important; } html, body, .app-layout, .main-content, .vc-container, .vc-viewport { background: transparent !important; }';" +
                                    "    var style = document.createElement('style');" +
                                    "    style.type = 'text/css';" +
                                    "    style.appendChild(document.createTextNode(css));" +
                                    "    document.head.appendChild(style);" +
                                    "    function zoomInCompactView() {" +
                                    "      try {" +
                                    "        if (window.VC && window.VC.camera && window.VC.controls) {" +
                                    "          var cam = window.VC.camera;" +
                                    "          var tgt = (window.VC.controls.target && window.VC.controls.target.clone) ? window.VC.controls.target : { x: 0, y: 0.35, z: 0 };" +
                                    "          var dx = cam.position.x - tgt.x;" +
                                    "          var dy = cam.position.y - tgt.y;" +
                                    "          var dz = cam.position.z - tgt.z;" +
                                    "          var d = Math.sqrt(dx*dx + dy*dy + dz*dz);" +
                                    "          if (d > 5.0) {" +
                                    "            var s = 4.4 / d;" +
                                    "            cam.position.x = tgt.x + dx * s;" +
                                    "            cam.position.y = tgt.y + dy * s;" +
                                    "            cam.position.z = tgt.z + dz * s;" +
                                    "            window.VC.controls.minDistance = 1.5;" +
                                    "            if (window.VC.controls.target && window.VC.controls.target.set) {" +
                                    "              window.VC.controls.target.set(0, 0.35, 0);" +
                                    "            }" +
                                    "            if (window.VC.controls.update) {" +
                                    "              window.VC.controls.update();" +
                                    "            }" +
                                    "          }" +
                                    "        }" +
                                    "      } catch(err) {}" +
                                    "    }" +
                                    "    zoomInCompactView();" +
                                    "    var timer = setInterval(zoomInCompactView, 250);" +
                                    "    setTimeout(function() { clearInterval(timer); }, 4000);" +
                                    "  } catch(e) {}" +
                                    "})();",
                                    null
                                )
                            }
                        }
                        loadUrl("http://127.0.0.1:${CameraDaemon.HTTP_PORT}/vehicle-control.html")
                    }
                } catch (_: Throwable) {
                    android.view.View(context)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // 3D Status Overlay Badge (Top Left)
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = "3D CANLI",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // Expand / Fullscreen Button (Top Right)
        IconButton(
            onClick = { isExpanded = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Fullscreen,
                contentDescription = "Tam Ekran 3D",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(18.dp),
            )
        }

        // Bottom hint: 360° Çevir & Yakınlaştır
        Text(
            text = "⟳ 360° Döndürmek için sürükleyin",
            fontSize = 7.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp),
        )
    }

    // Fullscreen 3D Model Dialog / Window
    if (isExpanded) {
        Dialog(
            onDismissRequest = { isExpanded = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { context ->
                            try {
                                WebView(context).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                    }
                                    setBackgroundColor(android.graphics.Color.BLACK)
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            view?.evaluateJavascript(
                                                "(function() {" +
                                                "  try {" +
                                                "    localStorage.setItem('vc.viewMode', 'immersive');" +
                                                "    if (window.VC && window.VC.switchViewMode && window.VC.liteMode) {" +
                                                "      window.VC.switchViewMode();" +
                                                "    }" +
                                                "    var style = document.createElement('style');" +
                                                "    style.type = 'text/css';" +
                                                "    style.appendChild(document.createTextNode('#app-shell-mount, .app-shell-sidebar, .sidebar, #sidebarOverlay { display: none !important; }'));" +
                                                "    document.head.appendChild(style);" +
                                                "  } catch(e) {}" +
                                                "})();",
                                                null
                                            )
                                        }
                                    }
                                    loadUrl("http://127.0.0.1:${CameraDaemon.HTTP_PORT}/vehicle-control.html")
                                }
                            } catch (_: Throwable) {
                                android.view.View(context)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Close Button
                    IconButton(
                        onClick = { isExpanded = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.7f), CircleShape),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleCenterModelAndBatteryCard(
    modelId: String?,
    modelName: String,
    is3DMode: Boolean,
    battery: VehicleBatteryUiState,
    onNavigateToCharging: () -> Unit = {},
    onSelectModelClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Top: Vehicle Model Render (VehicleArt) OR 3D Model Viewport
        // In 2D mode: when modelId is unset/null, VehicleArt.drawableFor() serves vehicle_fallback
        // (which is the black vehicle render with the '?' badge on its hood).
        // When a vehicle is selected, the clean model render is displayed without any '?' overlay.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (is3DMode) {
                Vehicle3DModelViewport(
                    modelId = modelId,
                    modelName = modelName,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = painterResource(VehicleArt.drawableFor(modelId)),
                    contentDescription = modelName.ifEmpty { "Araç Modeli" },
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 90.dp),
                            onClick = onSelectModelClick,
                        ),
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Bottom: BATARYA, MENZİL & TÜKETİM Card
        BatteryRangeCardContent(
            battery = battery,
            onNavigateToCharging = onNavigateToCharging,
        )
    }
}

@Composable
private fun BatteryRangeCardContent(
    battery: VehicleBatteryUiState,
    onNavigateToCharging: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val cardBg = MaterialTheme.colorScheme.surfaceContainer
    val cardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val innerContainerBg = MaterialTheme.colorScheme.surfaceContainerLow
    val innerBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val textMuted = MaterialTheme.colorScheme.outline
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    val soc = battery.socPercent.coerceIn(0, 100)
    val batteryCap = if (battery.batteryCapacityKwh > 0) battery.batteryCapacityKwh else 82.5
    val currentKwh = soc * batteryCap / 100.0

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = cardBorder,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Header Row: 🔋 BATARYA, MENZİL & TÜKETİM   SOH %100  Geçmiş ➔
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_card_battery),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.vc_battery_card_title),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp,
                        )
                        Text(
                            text = stringResource(R.string.vc_battery_card_subtitle),
                            fontSize = 9.5.sp,
                            color = textSecondary,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // SOH Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(OverdriveTheme.colors.statusSuccessContainer)
                            .border(1.dp, OverdriveTheme.colors.statusSuccess, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "SOH %${battery.sohPercent.toInt()}",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = OverdriveTheme.colors.statusSuccess,
                        )
                    }

                    Text(
                        text = stringResource(R.string.vc_history_arrow),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onNavigateToCharging() },
                    )
                }
            }

            // 3 KPI Columns Section in rounded container
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = innerContainerBg,
                border = innerBorder,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Col 1: Kalan Tahmini Menzil
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "KALAN TAHMİNİ MENZİL",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = textSecondary,
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${battery.elecRangeKm}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = textPrimary,
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "km",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                    modifier = Modifier.padding(bottom = 2.dp),
                                )
                            }
                            Text(
                                text = "Dinamik Sürüş Tahmini",
                                fontSize = 9.sp,
                                color = textMuted,
                            )
                        }

                        // Divider 1
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(42.dp)
                                .background(dividerColor)
                        )

                        // Col 2: Gerçekçi Menzil
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 6.dp)
                        ) {
                            Text(
                                text = "GERÇEKÇİ MENZİL",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = textSecondary,
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                val realistic = if (battery.realisticRangeKm > 0) battery.realisticRangeKm else battery.elecRangeKm
                                Text(
                                    text = "$realistic",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "km",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 2.dp),
                                )
                            }
                            Text(
                                text = "Dinamik Sürüş Tahmini",
                                fontSize = 9.sp,
                                color = textMuted,
                            )
                        }

                        // Divider 2
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(42.dp)
                                .background(dividerColor)
                        )

                        // Col 3: Şarj Seviyesi (SOC)
                        Column(
                            modifier = Modifier
                                .weight(1.15f)
                                .padding(start = 6.dp)
                        ) {
                            Text(
                                text = "ŞARJ SEVİYESİ (SOC)",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = textSecondary,
                            )
                            Text(
                                text = "%$soc",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f / %.1f kWh", currentKwh, batteryCap).replace('.', ','),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textSecondary,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Thin Battery Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    ) {
                        val animatedSoc by animateFloatAsState(
                            targetValue = (soc / 100f),
                            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                            label = "heroSoc"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedSoc)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            // Sub-Stats 4-Card Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Card 1
                val avg50Text = when {
                    battery.avg50KmKwh > 0.0 -> String.format(Locale.US, "%.1f kWh/100km", battery.avg50KmKwh).replace('.', ',')
                    battery.avgLifetimeKwh > 0.0 -> String.format(Locale.US, "%.1f kWh/100km", battery.avgLifetimeKwh).replace('.', ',')
                    else -> "-- kWh/100km"
                }
                val avg50Sub = if (battery.avgLifetimeKwh > 0.0) {
                    stringResource(R.string.vc_substat_overall_fmt, String.format(Locale.US, "%.1f kWh/100km", battery.avgLifetimeKwh).replace('.', ','))
                } else {
                    stringResource(R.string.vc_substat_overall_fmt, "--")
                }
                SubStatCard(
                    title = stringResource(R.string.vc_substat_last50km),
                    value = avg50Text,
                    subtitle = avg50Sub,
                    modifier = Modifier.weight(1f),
                )
                // Card 2
                val sinceChargeDistText = if (battery.sinceLastChargeKm > 0.0) {
                    String.format(Locale.US, "%.1f km", battery.sinceLastChargeKm).replace('.', ',')
                } else {
                    "0,0 km"
                }
                val sinceChargeAvgSub = if (battery.sinceLastChargeAvgKwh > 0.0) {
                    stringResource(R.string.vc_substat_avg_fmt, String.format(Locale.US, "%.1f kWh/100km", battery.sinceLastChargeAvgKwh).replace('.', ','))
                } else {
                    stringResource(R.string.vc_substat_avg_fmt, "--")
                }
                SubStatCard(
                    title = stringResource(R.string.vc_substat_since_charge),
                    value = sinceChargeDistText,
                    subtitle = sinceChargeAvgSub,
                    modifier = Modifier.weight(1f),
                )
                // Card 3
                val tripValText = if (battery.activeTripKm > 0.0 || battery.activeTripMinutes > 0) {
                    String.format(Locale.US, "%.1f km (%d dk)", battery.activeTripKm, battery.activeTripMinutes).replace('.', ',')
                } else {
                    "0,0 km (0 dk)"
                }
                val tripSub = if (battery.activeTripKm > 0.0) {
                    stringResource(R.string.vc_substat_dist_fmt, String.format(Locale.US, "%.1f km", battery.activeTripKm).replace('.', ','))
                } else {
                    stringResource(R.string.vc_substat_awaiting_drive)
                }
                SubStatCard(
                    title = stringResource(R.string.vc_substat_active_trip),
                    value = tripValText,
                    subtitle = tripSub,
                    modifier = Modifier.weight(1f),
                )
                // Card 4
                val regenValText = if (battery.regenKwh > 0.0) {
                    String.format(Locale.US, "+%.2f kWh", battery.regenKwh).replace('.', ',')
                } else {
                    "+0,00 kWh"
                }
                SubStatCard(
                    title = stringResource(R.string.vc_substat_regen_savings),
                    value = regenValText,
                    subtitle = stringResource(R.string.vc_substat_recovered_energy),
                    valueColor = OverdriveTheme.colors.statusSuccess,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SubStatCard(
    title: String,
    value: String,
    subtitle: String,
    valueColor: Color? = null,
    modifier: Modifier = Modifier,
) {
    val containerBg = MaterialTheme.colorScheme.surfaceContainerLow
    val containerBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val textMuted = MaterialTheme.colorScheme.outline

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = containerBg,
        border = containerBorder,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = title,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = textSecondary,
                maxLines = 1,
            )
            Text(
                text = value,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor ?: textPrimary,
                maxLines = 1,
            )
            Text(
                text = subtitle,
                fontSize = 8.sp,
                color = textMuted,
                maxLines = 1,
            )
        }
    }
}


// -----------------------------------------------------------------------------
// 3C. SAĞDA: ŞASİ & GÜÇ TELEMETRİSİ KARTI
// -----------------------------------------------------------------------------
// 3C. SAĞDA: ŞASİ & GÜÇ TELEMETRİSİ KARTI (3-Sayfalı Swipe/Tab Yapısı)
// -----------------------------------------------------------------------------
@Composable
private fun ChassisTelemetryCockpitCard(
    tyres: VehicleTyresState,
    doors: VehicleDoorsState,
    windows: VehicleWindowsState,
    battery: VehicleBatteryUiState,
    powertrain: VehiclePowertrainUiState,
    health: VehicleHealthUiState,
    isAwd: Boolean,
    carouselPage: Int = 0,
    onPageChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val cardBg = MaterialTheme.colorScheme.surfaceContainer
    val cardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val innerContainerBg = MaterialTheme.colorScheme.surfaceContainerLow
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    val dotInactiveColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val pagerState = rememberPagerState(initialPage = carouselPage.coerceIn(0, 2), pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        onPageChange(pagerState.currentPage)
    }
    LaunchedEffect(carouselPage) {
        if (pagerState.currentPage != carouselPage && carouselPage in 0..2) {
            pagerState.animateScrollToPage(carouselPage)
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = cardBorder,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Header Row: ⚡ with dynamic title and status badge based on page
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "⚡",
                    fontSize = 15.sp,
                    color = OverdriveTheme.colors.statusWarning,
                )
                Spacer(modifier = Modifier.width(6.dp))

                when (pagerState.currentPage) {
                    0 -> {
                        Text(
                            text = stringResource(R.string.vc_chassis_telemetry_title),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    1 -> {
                        val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
                        val formattedTime = sdf.format(Date(health.timestamp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.vc_vehicle_health_title),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp,
                            )
                            Text(
                                text = stringResource(R.string.vc_health_live_time, formattedTime),
                                fontSize = 8.5.sp,
                                color = textSecondary,
                            )
                        }

                        val badgeOk = health.isAllNormal
                        val badgeBg = if (badgeOk) OverdriveTheme.colors.statusSuccessContainer else OverdriveTheme.colors.statusDangerContainer
                        val badgeBorderColor = if (badgeOk) OverdriveTheme.colors.statusSuccess else OverdriveTheme.colors.statusDanger
                        val badgeTextColor = if (badgeOk) OverdriveTheme.colors.statusSuccess else OverdriveTheme.colors.statusDanger
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeBg)
                                .border(1.dp, badgeBorderColor, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = stringResource(if (badgeOk) R.string.vc_health_badge_normal else R.string.vc_health_badge_warning),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeTextColor,
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = stringResource(R.string.vc_energy_flow_title),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = if (isAwd) "AWD" else "RWD",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            // Body: HorizontalPager for swipe gestures between Page 0 (Şasi), Page 1 (Sağlık), and Page 2 (Güç Akışı)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                when (page) {
                    0 -> {
                        // Page 0: Top-Down Chassis View + 4 Tyre Callouts (Precisely aligned with vehicle wheels)
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                        BoxWithConstraints(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxHeight(),
                        ) {
                            val totalH = maxHeight
                            val carH = (totalH * 0.90f).coerceAtMost(215.dp)
                            val carW = carH / 2.14f

                            Box(
                                modifier = Modifier
                                    .height(carH)
                                    .width(214.dp),
                            ) {
                                // Center: Top-Down Vehicle Silhouette with Battery SoC Overlay
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(width = carW, height = carH),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.img_car_topview),
                                        contentDescription = "Car Top View",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit,
                                    )

                                    // Battery SoC inside center of car silhouette
                                    Text(
                                        text = "%${battery.socPercent}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                    )
                                }

                                // Front Left Tyre (FL) - Exactly aligned with front wheel
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .offset(y = (carH * 0.17f - 16.dp).coerceAtLeast(0.dp))
                                ) {
                                    TyreCallout(
                                        kpa = tyres.flPsi?.let { (it * 6.89476f).toInt() } ?: 290,
                                        tempC = tyres.flTemp ?: 33,
                                    )
                                }

                                // Front Right Tyre (FR) - Exactly aligned with front wheel
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(y = (carH * 0.17f - 16.dp).coerceAtLeast(0.dp))
                                ) {
                                    TyreCallout(
                                        kpa = tyres.frPsi?.let { (it * 6.89476f).toInt() } ?: 295,
                                        tempC = tyres.frTemp ?: 35,
                                    )
                                }

                                // Rear Left Tyre (RL) - Exactly aligned with rear wheel
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .offset(y = carH * 0.63f - 16.dp)
                                ) {
                                    TyreCallout(
                                        kpa = tyres.rlPsi?.let { (it * 6.89476f).toInt() } ?: 290,
                                        tempC = tyres.rlTemp ?: 33,
                                    )
                                }

                                // Rear Right Tyre (RR) - Exactly aligned with rear wheel
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(y = carH * 0.63f - 16.dp)
                                ) {
                                    TyreCallout(
                                        kpa = tyres.rrPsi?.let { (it * 6.89476f).toInt() } ?: 297,
                                        tempC = tyres.rrTemp ?: 37,
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Quick Status Row: Ön Kaput | Kapılar | Pencereler | Arka Bagaj
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        QuickStatusItem(
                            label = stringResource(R.string.vc_quick_hood),
                            statusText = stringResource(if (doors.hoodOpen) R.string.vc_status_open else R.string.vc_status_closed),
                            isWarning = doors.hoodOpen,
                            modifier = Modifier.weight(1f),
                        )
                        QuickStatusItem(
                            label = stringResource(R.string.vc_quick_doors),
                            statusText = stringResource(if (doors.frontLeftOpen || doors.frontRightOpen || doors.rearLeftOpen || doors.rearRightOpen) R.string.vc_status_open else R.string.vc_status_locked),
                            isWarning = doors.frontLeftOpen || doors.frontRightOpen || doors.rearLeftOpen || doors.rearRightOpen,
                            modifier = Modifier.weight(1f),
                        )
                        QuickStatusItem(
                            label = stringResource(R.string.vc_quick_windows),
                            statusText = stringResource(if (windows.frontLeftOpen || windows.frontRightOpen || windows.rearLeftOpen || windows.rearRightOpen || windows.isVentMode) R.string.vc_status_open else R.string.vc_status_closed),
                            isWarning = windows.frontLeftOpen || windows.frontRightOpen || windows.rearLeftOpen || windows.rearRightOpen,
                            modifier = Modifier.weight(1f),
                        )
                        QuickStatusItem(
                            label = stringResource(R.string.vc_quick_trunk),
                            statusText = stringResource(if (doors.trunkOpen) R.string.vc_status_open else R.string.vc_status_closed),
                            isWarning = doors.trunkOpen,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            1 -> {
                // Page 1: 9-System Vehicle Health Diagnostics
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(dividerColor)
                        )
                        HealthItemRow(title = stringResource(R.string.vc_health_tpms), item = health.tpms)
                        HealthItemRow(title = stringResource(R.string.vc_health_steering), item = health.steering)
                        HealthItemRow(title = stringResource(R.string.vc_health_srs), item = health.srsAirbag)
                        HealthItemRow(title = stringResource(R.string.vc_health_power_sys), item = health.powerSystem)
                        HealthItemRow(title = stringResource(R.string.vc_health_battery), item = health.tractionBattery)
                        HealthItemRow(title = stringResource(R.string.vc_health_esc), item = health.escStability)
                        HealthItemRow(title = stringResource(R.string.vc_health_charging), item = health.chargingSystem)
                        HealthItemRow(title = stringResource(R.string.vc_health_epb), item = health.epbBrake)
                        HealthItemRow(title = stringResource(R.string.vc_health_abs), item = health.absBrake)
                    }
                }
                else -> {
                    // Page 2: Chassis Energy Flow View & Live Regeneration Stream
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                ChassisEnergyFlowView(ctx).apply {
                                    setAwd(isAwd)
                                    setSoc(battery.socPercent)
                                    setPowerKw(powertrain.powerKw)
                                }
                            },
                            update = { view ->
                                view.setAwd(isAwd)
                                view.setSoc(battery.socPercent)
                                view.setPowerKw(powertrain.powerKw)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        )

                        val powerKw = powertrain.powerKw
                        val isDriving = powerKw > 0.8
                        val isRegen = powerKw < -0.8
                        val isChg = battery.isCharging

                        val modeText = when {
                            isDriving -> stringResource(R.string.vc_flow_driving)
                            isRegen -> stringResource(R.string.vc_flow_regen)
                            isChg -> stringResource(R.string.vc_flow_charging)
                            else -> stringResource(R.string.vc_flow_idle)
                        }
                        val modeColor = when {
                            isDriving -> MaterialTheme.colorScheme.primary
                            isRegen -> OverdriveTheme.colors.statusSuccess
                            isChg -> OverdriveTheme.colors.statusWarning
                            else -> textSecondary
                        }
                        val schemaText = when {
                            isDriving -> stringResource(R.string.vc_flow_schema_driving)
                            isRegen -> stringResource(R.string.vc_flow_schema_regen)
                            isChg -> stringResource(R.string.vc_flow_schema_charging)
                            else -> stringResource(R.string.vc_flow_schema_idle)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(innerContainerBg, RoundedCornerShape(8.dp))
                                .border(1.dp, dividerColor, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(modeColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = modeText,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = modeColor,
                                    )
                                }

                                Text(
                                    text = when {
                                        isDriving -> String.format(Locale.getDefault(), "+%.1f kW", powerKw)
                                        isRegen -> String.format(Locale.getDefault(), "%.1f kW", powerKw)
                                        isChg -> String.format(Locale.getDefault(), "+%.1f kW", battery.chargingPowerKw)
                                        else -> "0.0 kW"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = modeColor,
                                )
                            }

                            Text(
                                text = schemaText,
                                fontSize = 8.5.sp,
                                color = textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

            // Bottom Page Indicators (3 Bars matching Navion's touchAreaIndicatorPage1..3)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (i in 0 until 3) {
                    val isSelected = pagerState.currentPage == i
                    Box(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(i)
                                }
                            }
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(if (isSelected) 24.dp else 14.dp)
                                .height(3.5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else dotInactiveColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TyreCallout(
    kpa: Int,
    tempC: Int,
    modifier: Modifier = Modifier,
) {
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(50.dp),
    ) {
        Text(
            text = "$kpa kPa",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
        )
        Box(
            modifier = Modifier
                .padding(vertical = 2.dp)
                .width(42.dp)
                .height(1.dp)
                .background(dividerColor)
        )
        Text(
            text = "$tempC °C",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = textSecondary,
        )
    }
}

@Composable
private fun HealthItemRow(
    title: String,
    item: HealthCheckItem,
    modifier: Modifier = Modifier,
) {
    val textPrimary = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 10.sp,
            color = textPrimary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (item.isNormal) stringResource(R.string.vc_status_normal) else "● ${item.statusText}",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (item.isNormal) OverdriveTheme.colors.statusSuccess else OverdriveTheme.colors.statusDanger,
        )
    }
}

@Composable
private fun QuickStatusItem(
    label: String,
    statusText: String,
    isWarning: Boolean,
    modifier: Modifier = Modifier,
) {
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            color = textSecondary,
        )
        Text(
            text = statusText,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (isWarning) OverdriveTheme.colors.statusDanger else OverdriveTheme.colors.statusSuccess,
        )
    }
}

// -----------------------------------------------------------------------------
// 3. ALTINDA: EXPANDABLE VEHICLE CONTROLS PANEL FOR ACTIVE TAB
// -----------------------------------------------------------------------------
@Composable
private fun VehicleControlsExpandedCard(
    state: VehicleControlUiState,
    selectedTab: VehicleControlTab,
    onClose: () -> Unit = {},
    onLockClick: () -> Unit,
    onUnlockClick: () -> Unit,
    onFlashClick: () -> Unit,
    onFindCarClick: () -> Unit,
    onToggleTrunk: () -> Unit,
    onToggleHood: () -> Unit,
    onWindowsCloseAll: () -> Unit,
    onWindowsOpenAll: () -> Unit,
    onWindowsVentMode: () -> Unit,
    onToggleSunroof: () -> Unit,
    onToggleClimate: () -> Unit,
    onTempDown: () -> Unit,
    onTempUp: () -> Unit,
    onFanDown: () -> Unit,
    onFanUp: () -> Unit,
    onToggleBatteryHeat: () -> Unit,
    onCycleDriverSeatHeat: () -> Unit,
    onCycleDriverSeatVent: () -> Unit,
    onCyclePassengerSeatHeat: () -> Unit,
    onCyclePassengerSeatVent: () -> Unit,
    onToggleSteeringHeat: () -> Unit,
    onToggleMirrors: () -> Unit,
    onRotateScreen: () -> Unit,
    onToggleDrl: () -> Unit,
    onSelectAmbientColor: (Int) -> Unit,
    onToggleSlw: () -> Unit,
    onToggleCpd: () -> Unit,
    onStartCharging: () -> Unit,
    onToggleSmartCharge: () -> Unit,
    onSetChargeCap: (Int) -> Unit,
    onSetAcCurrentLimit: (Int) -> Unit,
    onPlayAvasTone: (Int) -> Unit,
    onStopAvas: () -> Unit,
    onToggleEngineSound: () -> Unit,
    onRebootIvi: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OverdriveCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            // Panel Header with Tab Title, Status and Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = getTabTitle(selectedTab),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.lastActionMessage != null) {
                        Text(
                            text = state.lastActionMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_clear),
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic Content Based on Selected Tab
            when (selectedTab) {
                VehicleControlTab.SECURITY -> {
                    SecurityTabControls(
                        isLocked = state.security.isLocked,
                        mirrorsFolded = state.security.mirrorsFolded,
                        onLockClick = onLockClick,
                        onUnlockClick = onUnlockClick,
                        onFlashClick = onFlashClick,
                        onFindCarClick = onFindCarClick,
                        onToggleMirrors = onToggleMirrors,
                    )
                }
                VehicleControlTab.TRUNK -> {
                    TrunkTabControls(
                        doors = state.doors,
                        onToggleTrunk = onToggleTrunk,
                        onToggleHood = onToggleHood,
                    )
                }
                VehicleControlTab.CLIMATE -> {
                    ClimateTabControls(
                        climate = state.climate,
                        onToggleClimate = onToggleClimate,
                        onTempDown = onTempDown,
                        onTempUp = onTempUp,
                        onFanDown = onFanDown,
                        onFanUp = onFanUp,
                        onToggleBatteryHeat = onToggleBatteryHeat,
                    )
                }
                VehicleControlTab.SEATS -> {
                    SeatsTabControls(
                        comfort = state.comfort,
                        onCycleDriverHeat = onCycleDriverSeatHeat,
                        onCycleDriverVent = onCycleDriverSeatVent,
                        onCyclePassengerHeat = onCyclePassengerSeatHeat,
                        onCyclePassengerVent = onCyclePassengerSeatVent,
                        onToggleSteeringHeat = onToggleSteeringHeat,
                    )
                }
                VehicleControlTab.WINDOWS -> {
                    WindowsTabControls(
                        windows = state.windows,
                        onCloseAll = onWindowsCloseAll,
                        onOpenAll = onWindowsOpenAll,
                        onVentMode = onWindowsVentMode,
                        onToggleSunroof = onToggleSunroof,
                    )
                }
                VehicleControlTab.LIGHTS -> {
                    LightsTabControls(
                        isDrlOn = state.isDrlOn,
                        ambientColor = state.ambientColorPreset,
                        onToggleDrl = onToggleDrl,
                        onSelectAmbientColor = onSelectAmbientColor,
                    )
                }
                VehicleControlTab.ADAS -> {
                    AdasTabControls(
                        slwEnabled = state.slwEnabled,
                        cpdEnabled = state.cpdEnabled,
                        onToggleSlw = onToggleSlw,
                        onToggleCpd = onToggleCpd,
                    )
                }
                VehicleControlTab.CHARGING -> {
                    ChargingTabControls(
                        isCharging = state.battery.isCharging,
                        chargingPowerKw = state.battery.chargingPowerKw,
                        smartChargeEnabled = state.smartChargeEnabled,
                        chargeCapPercent = state.chargeCapPercent,
                        acCurrentLimit = state.acCurrentLimit,
                        onStartCharging = onStartCharging,
                        onToggleSmartCharge = onToggleSmartCharge,
                        onSetChargeCap = onSetChargeCap,
                        onSetAcCurrentLimit = onSetAcCurrentLimit,
                    )
                }
                VehicleControlTab.SOUND -> {
                    SoundTabControls(
                        activeTone = state.activeAvasTone,
                        isEngineSoundOn = state.isEngineSoundOn,
                        onPlayTone = onPlayAvasTone,
                        onStopAvas = onStopAvas,
                        onToggleEngineSound = onToggleEngineSound,
                    )
                }
                VehicleControlTab.SYSTEM -> {
                    SystemTabControls(
                        onRotateScreen = onRotateScreen,
                        onRebootIvi = onRebootIvi,
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB PANELS IMPLEMENTATIONS
// -----------------------------------------------------------------------------

@Composable
private fun SecurityTabControls(
    isLocked: Boolean?,
    mirrorsFolded: Boolean,
    onLockClick: () -> Unit,
    onUnlockClick: () -> Unit,
    onFlashClick: () -> Unit,
    onFindCarClick: () -> Unit,
    onToggleMirrors: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ActionTile(
            title = stringResource(R.string.vc_action_lock),
            iconRes = R.drawable.ic_parking,
            isActive = isLocked == true,
            onClick = onLockClick,
            modifier = Modifier.weight(1f),
        )
        ActionTile(
            title = stringResource(R.string.vc_action_unlock),
            iconRes = R.drawable.ic_vehicle_control,
            isActive = isLocked == false,
            onClick = onUnlockClick,
            modifier = Modifier.weight(1f),
        )
        ActionTile(
            title = stringResource(R.string.vc_action_flash),
            iconRes = R.drawable.ic_roadsense,
            onClick = onFlashClick,
            modifier = Modifier.weight(1f),
        )
        ActionTile(
            title = stringResource(R.string.vc_action_find_car),
            iconRes = R.drawable.ic_roadsense_map,
            onClick = onFindCarClick,
            modifier = Modifier.weight(1f),
        )
        ActionTile(
            title = stringResource(R.string.vc_action_fold_mirrors),
            subtitle = if (mirrorsFolded) stringResource(R.string.vc_status_folded) else stringResource(R.string.vc_status_open),
            iconRes = R.drawable.ic_vehicle_control,
            isActive = mirrorsFolded,
            onClick = onToggleMirrors,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TrunkTabControls(
    doors: VehicleDoorsState,
    onToggleTrunk: () -> Unit,
    onToggleHood: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DoorStatusPill(label = stringResource(R.string.vc_door_fl), isOpen = doors.frontLeftOpen, modifier = Modifier.weight(1f))
            DoorStatusPill(label = stringResource(R.string.vc_door_fr), isOpen = doors.frontRightOpen, modifier = Modifier.weight(1f))
            DoorStatusPill(label = stringResource(R.string.vc_door_rl), isOpen = doors.rearLeftOpen, modifier = Modifier.weight(1f))
            DoorStatusPill(label = stringResource(R.string.vc_door_rr), isOpen = doors.rearRightOpen, modifier = Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ActionTile(
                title = stringResource(if (doors.trunkOpen) R.string.vc_action_close_trunk else R.string.vc_action_open_trunk),
                subtitle = stringResource(if (doors.trunkOpen) R.string.vc_status_open else R.string.vc_status_closed),
                iconRes = R.drawable.ic_vehicle_control,
                isActive = doors.trunkOpen,
                onClick = onToggleTrunk,
                modifier = Modifier.weight(1f),
            )
            ActionTile(
                title = stringResource(if (doors.hoodOpen) R.string.vc_action_close_hood else R.string.vc_action_open_hood),
                subtitle = stringResource(if (doors.hoodOpen) R.string.vc_status_open else R.string.vc_status_closed),
                iconRes = R.drawable.ic_vehicle_control,
                isActive = doors.hoodOpen,
                onClick = onToggleHood,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ClimateTabControls(
    climate: VehicleClimateState,
    onToggleClimate: () -> Unit,
    onTempDown: () -> Unit,
    onTempUp: () -> Unit,
    onFanDown: () -> Unit,
    onFanUp: () -> Unit,
    onToggleBatteryHeat: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ActionTile(
                title = stringResource(R.string.vc_action_climate_toggle),
                subtitle = if (climate.isAcOn) stringResource(R.string.vc_status_open) else stringResource(R.string.vc_status_closed),
                iconRes = R.drawable.ic_charging,
                isActive = climate.isAcOn,
                onClick = onToggleClimate,
                modifier = Modifier.weight(1f),
            )
            ActionTile(
                title = stringResource(R.string.vc_action_battery_heat),
                subtitle = if (climate.isBatteryHeatOn) stringResource(R.string.vc_status_active) else stringResource(R.string.vc_status_closed),
                iconRes = R.drawable.ic_charging,
                isActive = climate.isBatteryHeatOn,
                onClick = onToggleBatteryHeat,
                modifier = Modifier.weight(1f),
            )
        }

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
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.vc_climate_target_temp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
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
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
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
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.vc_climate_fan_speed),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
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
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        IconButton(onClick = onFanUp) {
                            Text("+", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeatsTabControls(
    comfort: VehicleComfortState,
    onCycleDriverHeat: () -> Unit,
    onCycleDriverVent: () -> Unit,
    onCyclePassengerHeat: () -> Unit,
    onCyclePassengerVent: () -> Unit,
    onToggleSteeringHeat: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ActionTile(
                title = stringResource(R.string.vc_action_driver_heat),
                subtitle = seatLevelLabel(comfort.driverSeatHeat),
                iconRes = R.drawable.ic_seat_positions,
                isActive = comfort.driverSeatHeat > 0,
                onClick = onCycleDriverHeat,
                modifier = Modifier.weight(1f),
            )
            ActionTile(
                title = stringResource(R.string.vc_action_driver_vent),
                subtitle = seatLevelLabel(comfort.driverSeatVent),
                iconRes = R.drawable.ic_seat_positions,
                isActive = comfort.driverSeatVent > 0,
                onClick = onCycleDriverVent,
                modifier = Modifier.weight(1f),
            )
            ActionTile(
                title = stringResource(R.string.vc_action_steering_heat),
                subtitle = if (comfort.steeringHeatOn) stringResource(R.string.vc_status_open) else stringResource(R.string.vc_status_closed),
                iconRes = R.drawable.ic_vehicle_control,
                isActive = comfort.steeringHeatOn,
                onClick = onToggleSteeringHeat,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ActionTile(
                title = stringResource(R.string.vc_action_passenger_heat),
                subtitle = seatLevelLabel(comfort.passengerSeatHeat),
                iconRes = R.drawable.ic_seat_positions,
                isActive = comfort.passengerSeatHeat > 0,
                onClick = onCyclePassengerHeat,
                modifier = Modifier.weight(1f),
            )
            ActionTile(
                title = stringResource(R.string.vc_action_passenger_vent),
                subtitle = seatLevelLabel(comfort.passengerSeatVent),
                iconRes = R.drawable.ic_seat_positions,
                isActive = comfort.passengerSeatVent > 0,
                onClick = onCyclePassengerVent,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun WindowsTabControls(
    windows: VehicleWindowsState,
    onCloseAll: () -> Unit,
    onOpenAll: () -> Unit,
    onVentMode: () -> Unit,
    onToggleSunroof: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ActionTile(
            title = stringResource(R.string.vc_action_close_all_windows),
            subtitle = stringResource(R.string.vc_window_close_all_sub),
            iconRes = R.drawable.ic_vehicle_control,
            onClick = onCloseAll,
            modifier = Modifier.weight(1f),
        )
        ActionTile(
            title = stringResource(R.string.vc_action_vent_windows),
            subtitle = stringResource(R.string.vc_window_vent_sub),
            iconRes = R.drawable.ic_vehicle_control,
            isActive = windows.isVentMode,
            onClick = onVentMode,
            modifier = Modifier.weight(1f),
        )
        ActionTile(
            title = stringResource(R.string.vc_action_open_all_windows),
            subtitle = stringResource(R.string.vc_window_open_all_sub),
            iconRes = R.drawable.ic_vehicle_control,
            onClick = onOpenAll,
            modifier = Modifier.weight(1f),
        )
        ActionTile(
            title = stringResource(R.string.vc_action_sunroof),
            subtitle = if (windows.sunroofOpen) stringResource(R.string.vc_status_open) else stringResource(R.string.vc_status_closed),
            iconRes = R.drawable.ic_vehicle_control,
            isActive = windows.sunroofOpen,
            onClick = onToggleSunroof,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LightsTabControls(
    isDrlOn: Boolean,
    ambientColor: Int,
    onToggleDrl: () -> Unit,
    onSelectAmbientColor: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ActionTile(
                title = stringResource(R.string.vc_action_drl),
                subtitle = if (isDrlOn) stringResource(R.string.vc_status_open) else stringResource(R.string.vc_status_closed),
                iconRes = R.drawable.ic_roadsense,
                isActive = isDrlOn,
                onClick = onToggleDrl,
                modifier = Modifier.weight(1f),
            )
        }

        // Ambient Lights Color Palette
        Text(
            text = stringResource(R.string.vc_ambient_select_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val colors = listOf(
            1 to Color(0xFF00E5FF), // Cyan
            5 to Color(0xFF3B82F6), // Blue
            10 to Color(0xFF10B981), // Emerald
            16 to Color(0xFFF59E0B), // Amber
            22 to Color(0xFFEC4899), // Pink
            28 to Color(0xFF8B5CF6), // Purple
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            colors.forEach { (preset, col) ->
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(col)
                        .border(
                            if (ambientColor == preset) 3.dp else 1.dp,
                            if (ambientColor == preset) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            CircleShape
                        )
                        .clickable { onSelectAmbientColor(preset) }
                )
            }
        }
    }
}

@Composable
private fun AdasTabControls(
    slwEnabled: Boolean,
    cpdEnabled: Boolean,
    onToggleSlw: () -> Unit,
    onToggleCpd: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ActionTile(
            title = stringResource(R.string.vc_action_slw),
            subtitle = if (slwEnabled) stringResource(R.string.vc_status_active) else stringResource(R.string.vc_status_closed),
            iconRes = R.drawable.ic_roadsense_arrow,
            isActive = slwEnabled,
            onClick = onToggleSlw,
            modifier = Modifier.weight(1f),
        )
        ActionTile(
            title = stringResource(R.string.vc_action_cpd),
            subtitle = if (cpdEnabled) stringResource(R.string.vc_status_active) else stringResource(R.string.vc_status_closed),
            iconRes = R.drawable.ic_roadsense,
            isActive = cpdEnabled,
            onClick = onToggleCpd,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ChargingTabControls(
    isCharging: Boolean,
    chargingPowerKw: Double,
    smartChargeEnabled: Boolean,
    chargeCapPercent: Int,
    acCurrentLimit: Int,
    onStartCharging: () -> Unit,
    onToggleSmartCharge: () -> Unit,
    onSetChargeCap: (Int) -> Unit,
    onSetAcCurrentLimit: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ActionTile(
                title = stringResource(R.string.vc_action_start_charging),
                subtitle = if (isCharging) "${stringResource(R.string.vc_flow_charging)} ($chargingPowerKw kW)" else stringResource(R.string.vc_charge_now),
                iconRes = R.drawable.ic_charging,
                isActive = isCharging,
                onClick = onStartCharging,
                modifier = Modifier.weight(1f),
            )
            ActionTile(
                title = stringResource(R.string.vc_action_smart_charge),
                subtitle = if (smartChargeEnabled) stringResource(R.string.vc_status_active) else stringResource(R.string.vc_status_closed),
                iconRes = R.drawable.ic_charging,
                isActive = smartChargeEnabled,
                onClick = onToggleSmartCharge,
                modifier = Modifier.weight(1f),
            )
        }

        // Charge Limit (SoC Cap) Slider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(12.dp),
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.vc_charge_slider_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "$chargeCapPercent%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                OverdriveSlider(
                    value = chargeCapPercent.toFloat(),
                    onValueChange = { onSetChargeCap(it.toInt()) },
                    valueRange = 50f..100f,
                    steps = 9, // 50, 55, 60, ... 100
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // AC Current Limits (6A, 8A, 10A, 16A, Max)
        Column {
            Text(
                text = stringResource(R.string.vc_charge_ac_limit_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val maxLabel = stringResource(R.string.vc_charge_max)
                listOf(6 to "6 A", 8 to "8 A", 10 to "10 A", 16 to "16 A", 32 to maxLabel).forEach { (amps, text) ->
                    val isSelected = acCurrentLimit == amps
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(6.dp))
                            .clickable { onSetAcCurrentLimit(amps) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundTabControls(
    activeTone: Int?,
    isEngineSoundOn: Boolean,
    onPlayTone: (Int) -> Unit,
    onStopAvas: () -> Unit,
    onToggleEngineSound: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionTile(
                title = stringResource(R.string.vc_sound_ding_dong),
                subtitle = stringResource(R.string.vc_sound_tone_fmt, 0),
                iconRes = R.drawable.ic_volume_on,
                isActive = activeTone == 0,
                onClick = { onPlayTone(0) },
                modifier = Modifier.weight(1f),
            )
            ActionTile(
                title = stringResource(R.string.vc_sound_triple_beep),
                subtitle = stringResource(R.string.vc_sound_tone_fmt, 2),
                iconRes = R.drawable.ic_volume_on,
                isActive = activeTone == 2,
                onClick = { onPlayTone(2) },
                modifier = Modifier.weight(1f),
            )
            ActionTile(
                title = stringResource(R.string.vc_sound_melody),
                subtitle = stringResource(R.string.vc_sound_tone_fmt, 4),
                iconRes = R.drawable.ic_volume_on,
                isActive = activeTone == 4,
                onClick = { onPlayTone(4) },
                modifier = Modifier.weight(1f),
            )
            ActionTile(
                title = stringResource(R.string.vc_sound_alarm),
                subtitle = stringResource(R.string.vc_sound_tone_fmt, 6),
                iconRes = R.drawable.ic_volume_on,
                isActive = activeTone == 6,
                onClick = { onPlayTone(6) },
                modifier = Modifier.weight(1f),
            )
            ActionTile(
                title = stringResource(R.string.vc_action_stop_avas),
                subtitle = stringResource(R.string.vc_sound_stop_sub),
                iconRes = R.drawable.ic_volume_off,
                onClick = onStopAvas,
                modifier = Modifier.weight(1f),
            )
        }

        ActionTile(
            title = stringResource(R.string.vc_action_engine_sound),
            subtitle = if (isEngineSoundOn) stringResource(R.string.vc_status_active) else stringResource(R.string.vc_status_closed),
            iconRes = R.drawable.ic_volume_on,
            isActive = isEngineSoundOn,
            onClick = onToggleEngineSound,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SystemTabControls(
    onRotateScreen: () -> Unit,
    onRebootIvi: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ActionTile(
            title = stringResource(R.string.vc_action_rotate_screen),
            subtitle = stringResource(R.string.vc_screen_rotate_sub),
            iconRes = R.drawable.ic_projection,
            onClick = onRotateScreen,
            modifier = Modifier.weight(1f),
        )
        ActionTile(
            title = stringResource(R.string.vc_action_reboot_ivi),
            subtitle = stringResource(R.string.vc_reboot_ivi_sub),
            iconRes = R.drawable.ic_settings,
            onClick = onRebootIvi,
            modifier = Modifier.weight(1f),
        )
    }
}

// -----------------------------------------------------------------------------
// 4. ALTINDA: 10-TAB CATEGORY DOCK BAR
// -----------------------------------------------------------------------------
@Composable
private fun VehicleControlDockBar(
    selectedTab: VehicleControlTab,
    onTabSelected: (VehicleControlTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardBg = MaterialTheme.colorScheme.surfaceContainer
    val cardBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val textSelected = MaterialTheme.colorScheme.primary
    val textUnselected = MaterialTheme.colorScheme.onSurfaceVariant
    val iconSelected = MaterialTheme.colorScheme.primary
    val iconUnselected = MaterialTheme.colorScheme.onSurfaceVariant

    val tabs = listOf(
        VehicleControlTab.SECURITY to (R.string.vc_dock_security to R.drawable.ic_dock_security),
        VehicleControlTab.TRUNK to (R.string.vc_dock_trunk to R.drawable.ic_dock_trunk),
        VehicleControlTab.CLIMATE to (R.string.vc_dock_climate to R.drawable.ic_dock_climate),
        VehicleControlTab.SEATS to (R.string.vc_dock_seats to R.drawable.ic_dock_seats),
        VehicleControlTab.WINDOWS to (R.string.vc_dock_windows to R.drawable.ic_dock_windows),
        VehicleControlTab.LIGHTS to (R.string.vc_dock_lights to R.drawable.ic_dock_lights),
        VehicleControlTab.ADAS to (R.string.vc_dock_adas to R.drawable.ic_dock_adas),
        VehicleControlTab.CHARGING to (R.string.vc_dock_charging to R.drawable.ic_dock_charging),
        VehicleControlTab.SOUND to (R.string.vc_dock_sound to R.drawable.ic_dock_sound),
        VehicleControlTab.SYSTEM to (R.string.vc_dock_system to R.drawable.ic_dock_system),
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = cardBorder,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { (tab, meta) ->
                val isSelected = selectedTab == tab
                val (titleRes, iconRes) = meta
                val title = stringResource(titleRes)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        // Brand Teal Dot Indicator for Active Tab
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        )

                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = title,
                            tint = if (isSelected) iconSelected else iconUnselected,
                            modifier = Modifier.size(19.dp),
                        )

                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) textSelected else textUnselected,
                            fontSize = 10.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun getTabTitle(tab: VehicleControlTab): String {
    return when (tab) {
        VehicleControlTab.SECURITY -> stringResource(R.string.vc_title_security)
        VehicleControlTab.TRUNK -> stringResource(R.string.vc_title_trunk)
        VehicleControlTab.CLIMATE -> stringResource(R.string.vc_title_climate)
        VehicleControlTab.SEATS -> stringResource(R.string.vc_title_seats)
        VehicleControlTab.WINDOWS -> stringResource(R.string.vc_title_windows)
        VehicleControlTab.LIGHTS -> stringResource(R.string.vc_title_lights)
        VehicleControlTab.ADAS -> stringResource(R.string.vc_title_adas)
        VehicleControlTab.CHARGING -> stringResource(R.string.vc_title_charging)
        VehicleControlTab.SOUND -> stringResource(R.string.vc_title_sound)
        VehicleControlTab.SYSTEM -> stringResource(R.string.vc_title_system)
    }
}

// -----------------------------------------------------------------------------
// HELPER COMPONENTS
// -----------------------------------------------------------------------------
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
                text = if (isOpen) stringResource(R.string.status_open) else stringResource(R.string.status_closed),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isOpen) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun seatLevelLabel(level: Int): String {
    return when (level) {
        1 -> stringResource(R.string.vc_level_1)
        2 -> stringResource(R.string.vc_level_2)
        3 -> stringResource(R.string.vc_level_3)
        else -> stringResource(R.string.vc_level_0)
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
            .padding(10.dp)
            .height(58.dp),
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
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                )
                if (!subtitle.isNullOrEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}
