package com.overdrive.app.ui.trips

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.overdrive.app.ui.component.OverdriveSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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
import com.overdrive.app.ui.theme.OverdriveTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 100% Jetpack Compose Native Trips Screen.
 * Complete 1:1 replacement for the legacy WebView /trips page.
 * Includes interactive route map and synchronized timeline scrubber.
 */
@Composable
fun TripsScreen(
    state: TripsUiState,
    onFilterSelected: (TripsFilterPeriod) -> Unit = {},
    onTripClick: (TripUiItem) -> Unit = {},
    onExportClick: () -> Unit = {},
    onBackToList: () -> Unit = {},
    onScrubberChange: (Int) -> Unit = {},
    onTogglePlay: () -> Unit = {},
    onPlaybackSpeedChange: (Float) -> Unit = {},
    onCleanupCdr: () -> Unit = {},
    onExportKml: () -> Unit = {},
    onExportGpx: () -> Unit = {},
    onDeleteTripClick: (TripUiItem) -> Unit = {},
    onConfirmDeleteTrip: () -> Unit = {},
    onDismissDeleteTrip: () -> Unit = {},
    onToggleViewMode: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        if (state.tripToDelete != null) {
            val trip = state.tripToDelete
            OverdriveDialog(
                onDismissRequest = onDismissDeleteTrip,
                title = "Seyahat Kaydını Sil",
                positiveButtonText = "Sil",
                onPositiveClick = onConfirmDeleteTrip,
                negativeButtonText = "Vazgeç",
                onNegativeClick = onDismissDeleteTrip,
            ) {
                Text(
                    text = "Bu seyahat kaydını (${String.format(Locale.US, "%.1f km", trip.distanceKm)}) kalıcı olarak silmek istediğinizden emin misiniz?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (state.selectedTripForDetail != null) {
            TripDetailView(
                trip = state.selectedTripForDetail,
                scrubberIndex = state.scrubberIndex,
                isPlaying = state.isPlaying,
                playbackSpeed = state.playbackSpeed,
                onBackClick = onBackToList,
                onScrubberChange = onScrubberChange,
                onTogglePlay = onTogglePlay,
                onPlaybackSpeedChange = onPlaybackSpeedChange,
                onCleanupCdr = onCleanupCdr,
                onExportKml = onExportKml,
                onExportGpx = onExportGpx,
            )
        } else {
            TripsMasterView(
                state = state,
                onFilterSelected = onFilterSelected,
                onTripClick = onTripClick,
                onExportClick = onExportClick,
                onCleanupCdr = onCleanupCdr,
                onExportKml = onExportKml,
                onExportGpx = onExportGpx,
                onDeleteTripClick = onDeleteTripClick,
                onToggleViewMode = onToggleViewMode,
            )
        }
    }
}

// =============================================================================
// MASTER / LIST VIEW
// =============================================================================
@Composable
private fun TripsMasterView(
    state: TripsUiState,
    onFilterSelected: (TripsFilterPeriod) -> Unit,
    onTripClick: (TripUiItem) -> Unit,
    onExportClick: () -> Unit,
    onCleanupCdr: () -> Unit,
    onExportKml: () -> Unit,
    onExportGpx: () -> Unit,
    onDeleteTripClick: (TripUiItem) -> Unit,
    onToggleViewMode: (Boolean) -> Unit,
) {
    val scrollState = rememberScrollState()

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
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Header Bar (No duplicate "Seyahatler" title)
        TripsHeader(
            filter = state.filter,
            onFilterSelected = onFilterSelected,
            onExportClick = onExportClick,
        )

        // Hero Aggregates Card
        TripsHeroCard(state = state)

        // Section Title & View Switcher (Tablo / Kart)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.trips_recent_trips_fmt, state.trips.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                FilterTabButton(
                    text = "Tablo",
                    isSelected = state.isTableView,
                    onClick = { onToggleViewMode(true) }
                )
                FilterTabButton(
                    text = "Kart",
                    isSelected = !state.isTableView,
                    onClick = { onToggleViewMode(false) }
                )
            }
        }

        // Trip Items (Empty State vs Table vs Cards)
        if (state.trips.isEmpty()) {
            EmptyTripsCard()
        } else if (state.isTableView) {
            NavionTripsTableView(
                trips = state.trips,
                onTripClick = onTripClick,
                onDeleteTripClick = onDeleteTripClick,
            )
        } else {
            state.trips.forEach { trip ->
                TripItemCard(
                    trip = trip,
                    onClick = { onTripClick(trip) },
                    onDeleteClick = { onDeleteTripClick(trip) }
                )
            }
        }

        // Storage & Dashcam Management Card
        TripsStorageCard(
            onCleanupCdr = onCleanupCdr,
            onExportKml = onExportKml,
            onExportGpx = onExportGpx,
        )
    }
}

@Composable
private fun TripsHeader(
    filter: TripsFilterPeriod,
    onFilterSelected: (TripsFilterPeriod) -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        OverdriveStatusPill(
            status = OverdrivePillStatus.INFO,
            label = stringResource(R.string.trips_header_subtitle)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Filter Pills
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterTabButton(
                    text = stringResource(R.string.trips_filter_all),
                    isSelected = filter == TripsFilterPeriod.ALL,
                    onClick = { onFilterSelected(TripsFilterPeriod.ALL) }
                )
                FilterTabButton(
                    text = stringResource(R.string.trips_filter_this_week),
                    isSelected = filter == TripsFilterPeriod.THIS_WEEK,
                    onClick = { onFilterSelected(TripsFilterPeriod.THIS_WEEK) }
                )
                FilterTabButton(
                    text = stringResource(R.string.trips_filter_this_month),
                    isSelected = filter == TripsFilterPeriod.THIS_MONTH,
                    onClick = { onFilterSelected(TripsFilterPeriod.THIS_MONTH) }
                )
            }

            OverdriveButton(
                text = stringResource(R.string.trips_export_button),
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_copy),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                variant = OverdriveButtonVariant.OUTLINED,
                onClick = onExportClick,
            )
        }
    }
}

@Composable
private fun FilterTabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        label = "FilterBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "FilterText"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
        )
    }
}

@Composable
private fun TripsHeroCard(
    state: TripsUiState,
    modifier: Modifier = Modifier,
) {
    OverdriveCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Distance Metric
            Column {
                Text(
                    text = stringResource(R.string.trips_total_distance_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format(Locale.US, "%,.1f", state.totalDistanceKm),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "km",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.trips_total_trips_duration_fmt, state.totalTripsCount, state.totalDurationHours),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Center Driver Score Radial Gauge
            DriverScoreGauge(score = state.overallDnaScore)

            // Right side stats: Efficiency
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.trips_avg_efficiency_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format(Locale.US, "%.1f", state.overallEfficiencyKwhPer100Km),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = stringResource(R.string.trips_kwh_per_100km),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Circular Driver Score Gauge with automotive telemetry aesthetics.
 */
@Composable
fun DriverScoreGauge(
    score: Int,
    modifier: Modifier = Modifier,
) {
    val ratingText = when {
        score >= 90 -> stringResource(R.string.trips_score_rating_excellent)
        score >= 75 -> stringResource(R.string.trips_score_rating_good)
        score >= 60 -> stringResource(R.string.trips_score_rating_fair)
        else -> stringResource(R.string.trips_score_rating_poor)
    }
    val scoreColor = when {
        score >= 90 -> OverdriveTheme.colors.statusSuccess
        score >= 75 -> OverdriveTheme.colors.statusWarning
        else -> OverdriveTheme.colors.statusDanger
    }
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Box(
        modifier = modifier.size(105.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            // Background circle track
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Dynamic progress arc
            val sweep = 270f * (score.coerceIn(0, 100) / 100f)
            drawArc(
                color = scoreColor,
                startAngle = 135f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "★",
                    color = scoreColor,
                    style = MaterialTheme.typography.labelSmall,
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Text(
                text = ratingText,
                style = MaterialTheme.typography.labelSmall,
                color = scoreColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun TripItemCard(
    trip: TripUiItem,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val dateFormat = SimpleDateFormat("dd MMM · HH:mm", Locale("tr"))
    val endFormat = SimpleDateFormat("HH:mm", Locale("tr"))
    val timeLabel = "${dateFormat.format(Date(trip.startTimeMs))} - ${endFormat.format(Date(trip.endTimeMs))}"

    OverdriveCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    OverdriveStatusPill(
                        status = OverdrivePillStatus.INFO,
                        label = trip.kinematicState,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OverdriveStatusPill(
                        status = if (trip.drivingDnaScore >= 90) OverdrivePillStatus.SUCCESS else OverdrivePillStatus.WARNING,
                        label = stringResource(R.string.trips_dna_score_fmt, trip.drivingDnaScore),
                    )
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = "Sil",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "→",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TripStatColumn(label = stringResource(R.string.trips_stat_distance), value = "${String.format(Locale.US, "%.1f", trip.distanceKm)} km")
                TripStatColumn(label = stringResource(R.string.trips_stat_duration), value = "${trip.durationMinutes} dk")
                TripStatColumn(label = stringResource(R.string.trips_stat_avg_speed), value = "${trip.avgSpeedKmh.toInt()} km/s")
                TripStatColumn(label = stringResource(R.string.trips_stat_consumption), value = "${String.format(Locale.US, "%.1f", trip.energyUsedKwh)} kWh")
                TripStatColumn(label = stringResource(R.string.trips_stat_efficiency), value = "${String.format(Locale.US, "%.1f", trip.efficiencyKwhPer100Km)} kWh/100km")
                TripStatColumn(label = stringResource(R.string.trips_stat_battery_soc), value = "%${trip.socStart} ➔ %${trip.socEnd}")
            }
        }
    }
}

@Composable
private fun TripStatColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace,
        )
    }
}

// =============================================================================
// NAVION-STYLE TABLE VIEW & EMPTY STATE
// =============================================================================
@Composable
private fun NavionTripsTableView(
    trips: List<TripUiItem>,
    onTripClick: (TripUiItem) -> Unit,
    onDeleteTripClick: (TripUiItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    OverdriveCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Table Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tarih",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1.2f)
                )
                Text(
                    text = "Rota",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(2.2f)
                )
                Text(
                    text = "Mod",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.8f)
                )
                Text(
                    text = "Mesafe",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1.0f)
                )
                Text(
                    text = "Süre",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.9f)
                )
                Text(
                    text = "Tüketim",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1.1f)
                )
                Text(
                    text = "Eko Skor",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1.0f)
                )
                Text(
                    text = "İşlem",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.6f)
                )
            }

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Table Body
            trips.forEachIndexed { index, trip ->
                NavionTripTableRow(
                    trip = trip,
                    onClick = { onTripClick(trip) },
                    onDeleteClick = { onDeleteTripClick(trip) }
                )
                if (index < trips.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                    )
                }
            }
        }
    }
}

@Composable
private fun NavionTripTableRow(
    trip: TripUiItem,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormat = SimpleDateFormat("dd MMM · HH:mm", Locale("tr"))
    val dateLabel = dateFormat.format(Date(trip.startTimeMs))
    val ecoColor = Color(0xFF10B981)
    val consumptionColor = Color(0xFF06B6D4)

    val (modeText, modeColor) = when {
        trip.efficiencyKwhPer100Km < 16.0f -> "ECO" to Color(0xFF10B981)
        trip.avgSpeedKmh > 75f -> "SPORT" to Color(0xFFF59E0B)
        else -> "NORMAL" to Color(0xFF06B6D4)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Tarih
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.2f)
        )

        // 2. Rota / Kinematik
        Text(
            text = trip.kinematicState,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(2.2f)
        )

        // 3. Mod
        Box(
            modifier = Modifier.weight(0.8f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(modeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = modeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = modeColor
                )
            }
        }

        // 4. Mesafe
        Text(
            text = "${String.format(Locale.US, "%.1f", trip.distanceKm)} km",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1.0f)
        )

        // 5. Süre
        Text(
            text = "${trip.durationMinutes} dk",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(0.9f)
        )

        // 6. Tüketim
        Text(
            text = "${String.format(Locale.US, "%.1f", trip.efficiencyKwhPer100Km)} kWh/100",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = consumptionColor,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1.1f)
        )

        // 7. Eko Skor
        Row(
            modifier = Modifier.weight(1.0f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(ecoColor.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_regen_leaf),
                    contentDescription = null,
                    tint = ecoColor,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = "%${trip.drivingDnaScore}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ecoColor,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 8. İşlem / Sil
        Box(
            modifier = Modifier.weight(0.6f),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = "Seyahati Sil",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyTripsCard(
    modifier: Modifier = Modifier,
) {
    OverdriveCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_trips),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Henüz Kayıtlı Seyahat Bulunmuyor",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Aracınızla sürüşe başladığınızda seyahat kayıtları ve telemetri otomatik olarak burada listelenecektir.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// =============================================================================
// DETAIL VIEW (Interactive Map + Timeline Scrubber)
// =============================================================================
@Composable
private fun TripDetailView(
    trip: TripUiItem,
    scrubberIndex: Int,
    isPlaying: Boolean,
    playbackSpeed: Float,
    onBackClick: () -> Unit,
    onScrubberChange: (Int) -> Unit,
    onTogglePlay: () -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onCleanupCdr: () -> Unit,
    onExportKml: () -> Unit,
    onExportGpx: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val points = trip.telemetryPoints
    val safeIndex = scrubberIndex.coerceIn(0, (points.size - 1).coerceAtLeast(0))
    val currentPoint = points.getOrNull(safeIndex) ?: points.firstOrNull()

    val currentScrubberIndex by rememberUpdatedState(safeIndex)

    // Auto-playback loop
    LaunchedEffect(isPlaying, playbackSpeed, points.size) {
        if (isPlaying && points.isNotEmpty()) {
            while (true) {
                val delayMs = (500L / playbackSpeed).toLong().coerceAtLeast(80L)
                delay(delayMs)
                val nextIdx = (currentScrubberIndex + 1) % points.size
                onScrubberChange(nextIdx)
            }
        }
    }

    val dateFormat = SimpleDateFormat("dd MMMM yyyy · HH:mm", Locale("tr"))
    val endFormat = SimpleDateFormat("HH:mm", Locale("tr"))
    val timeLabel = "${dateFormat.format(Date(trip.startTimeMs))} - ${endFormat.format(Date(trip.endTimeMs))}"

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
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Navigation Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OverdriveButton(
                    text = stringResource(R.string.trips_back_button),
                    variant = OverdriveButtonVariant.TONAL,
                    onClick = onBackClick,
                )
                Column {
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.trips_detail_header_subtitle_fmt, trip.id),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OverdriveStatusPill(
                    status = OverdrivePillStatus.INFO,
                    label = trip.kinematicState,
                )
                OverdriveStatusPill(
                    status = if (trip.drivingDnaScore >= 90) OverdrivePillStatus.SUCCESS else OverdrivePillStatus.WARNING,
                    label = stringResource(R.string.trips_dna_score_fmt, trip.drivingDnaScore),
                )
            }
        }

        // Summary Statistics KPI Grid
        DetailSummaryCard(trip = trip)

        // Trip Timeline Scrubber Card
        if (currentPoint != null && points.size > 1) {
            TripTimelineScrubberCard(
                points = points,
                currentPoint = currentPoint,
                currentIndex = safeIndex,
                isPlaying = isPlaying,
                playbackSpeed = playbackSpeed,
                totalDurationMinutes = trip.durationMinutes,
                onScrubberChange = onScrubberChange,
                onTogglePlay = onTogglePlay,
                onPlaybackSpeedChange = onPlaybackSpeedChange,
            )
        }

        // Interactive Native Vector Route Map Card (MapLibre basemap + GPS telemetry trace)
        if (points.isNotEmpty()) {
            TripMapLibreCard(
                points = points,
                currentPoint = currentPoint,
            )
        }

        // Speed & Dynamics Timeline Canvas Chart
        if (points.size > 1) {
            SpeedTimelineChartCard(
                points = points,
                currentIndex = safeIndex,
            )
        }

        // Driving DNA Sub-scores Card
        DrivingDnaBreakdownCard(trip = trip)

        // Storage & Dashcam Management Card
        TripsStorageCard(
            onCleanupCdr = onCleanupCdr,
            onExportKml = onExportKml,
            onExportGpx = onExportGpx,
        )
    }
}

@Composable
private fun DetailSummaryCard(trip: TripUiItem) {
    OverdriveCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.trips_summary_section_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                DetailKpiTile(label = stringResource(R.string.trips_stat_distance), value = "${String.format(Locale.US, "%.1f", trip.distanceKm)} km", highlight = true)
                DetailKpiTile(label = stringResource(R.string.trips_stat_duration), value = "${trip.durationMinutes} dk")
                DetailKpiTile(label = stringResource(R.string.trips_kpi_soc_consumption), value = "%${trip.socStart - trip.socEnd}", subtext = "%${trip.socStart} ➔ %${trip.socEnd}")
                DetailKpiTile(label = stringResource(R.string.trips_kpi_energy_used), value = "${String.format(Locale.US, "%.1f", trip.energyUsedKwh)} kWh")
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                DetailKpiTile(label = stringResource(R.string.trips_kpi_avg_speed), value = "${trip.avgSpeedKmh.toInt()} km/s")
                DetailKpiTile(label = stringResource(R.string.trips_kpi_max_speed), value = "${trip.maxSpeedKmh} km/s")
                DetailKpiTile(label = stringResource(R.string.trips_stat_efficiency), value = "${String.format(Locale.US, "%.1f", trip.efficiencyKwhPer100Km)} kWh/100km")
                DetailKpiTile(label = stringResource(R.string.trips_kpi_trip_cost), value = trip.tripCostFormatted ?: "--")
                DetailKpiTile(label = stringResource(R.string.trips_kpi_elevation), value = "↑ ${trip.elevationGainM}m · ↓ ${trip.elevationLossM}m")
            }
        }
    }
}

@Composable
private fun DetailKpiTile(
    label: String,
    value: String,
    subtext: String? = null,
    highlight: Boolean = false,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace,
        )
        if (subtext != null) {
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// =============================================================================
// TIMELINE SCRUBBER CARD
// =============================================================================
@Composable
private fun TripTimelineScrubberCard(
    points: List<TripTelemetryPoint>,
    currentPoint: TripTelemetryPoint,
    currentIndex: Int,
    isPlaying: Boolean,
    playbackSpeed: Float,
    totalDurationMinutes: Int,
    onScrubberChange: (Int) -> Unit,
    onTogglePlay: () -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
) {
    OverdriveCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Live Telemetry HUD
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "⏱",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Text(
                        text = stringResource(R.string.trips_timeline_scrubber_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                // Instant HUD values
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    HudBadge(label = stringResource(R.string.trips_hud_speed), value = "${currentPoint.speedKmh} km/s", isAccent = true)
                    HudBadge(label = stringResource(R.string.trips_hud_accel), value = "%${currentPoint.accelPedalPercent}")
                    HudBadge(label = stringResource(R.string.trips_hud_brake), value = "%${currentPoint.brakePedalPercent}")
                    HudBadge(label = stringResource(R.string.trips_hud_soc), value = String.format(Locale.US, "%%%.1f", currentPoint.socPercent))
                    HudBadge(label = stringResource(R.string.trips_hud_power), value = String.format(Locale.US, "%.1f kW", currentPoint.powerKw))
                }
            }

            // Slider Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                OverdriveSlider(
                    value = currentIndex.toFloat(),
                    onValueChange = { onScrubberChange(it.toInt()) },
                    valueRange = 0f..(points.size - 1).coerceAtLeast(1).toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                // Time labels
                val elapsedMinutes = currentPoint.elapsedSeconds / 60
                val elapsedSeconds = currentPoint.elapsedSeconds % 60
                val currentFormatted = String.format(Locale.US, "%02d:%02d", elapsedMinutes, elapsedSeconds)
                val totalFormatted = String.format(Locale.US, "%02d:00", totalDurationMinutes)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "00:00",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = currentFormatted,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = totalFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            // Playback Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OverdriveButton(
                        text = if (isPlaying) stringResource(R.string.trips_playback_pause) else stringResource(R.string.trips_playback_play),
                        variant = if (isPlaying) OverdriveButtonVariant.PRIMARY else OverdriveButtonVariant.TONAL,
                        onClick = onTogglePlay,
                    )

                    // Playback speed toggles
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        listOf(1.0f, 2.0f, 4.0f).forEach { speed ->
                            val isSelected = playbackSpeed == speed
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { onPlaybackSpeedChange(speed) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text = "${speed.toInt()}x",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.trips_point_progress_fmt, currentIndex + 1, points.size, currentPoint.lat, currentPoint.lon),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
private fun HudBadge(label: String, value: String, isAccent: Boolean = false) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isAccent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = if (isAccent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isAccent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace,
        )
    }
}

// =============================================================================
// SPEED & DYNAMICS TIMELINE CHART
// =============================================================================
@Composable
private fun SpeedTimelineChartCard(
    points: List<TripTelemetryPoint>,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    OverdriveCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.trips_dynamics_chart_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.trips_dynamics_chart_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(6.dp)
                    )
            ) {
                val maxSpeed = (points.maxOfOrNull { it.speedKmh } ?: 100).coerceAtLeast(60).toFloat()
                val primaryColor = MaterialTheme.colorScheme.primary
                val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                val cursorColor = OverdriveTheme.colors.statusWarning

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val padding = 16f

                    val chartWidth = width - padding * 2
                    val chartHeight = height - padding * 2

                    // Horizontal guideline at 50%
                    drawLine(
                        color = outlineColor,
                        start = Offset(padding, padding + chartHeight / 2),
                        end = Offset(width - padding, padding + chartHeight / 2),
                        strokeWidth = 1f
                    )

                    // Draw speed curve
                    val path = Path()
                    val stepX = chartWidth / (points.size - 1).coerceAtLeast(1)

                    points.forEachIndexed { i, p ->
                        val x = padding + i * stepX
                        val y = padding + chartHeight - (p.speedKmh / maxSpeed) * chartHeight
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }

                    drawPath(
                        path = path,
                        color = primaryColor,
                        style = Stroke(width = 3f, cap = StrokeCap.Round)
                    )

                    // Scrubber vertical cursor line
                    val cursorX = padding + currentIndex * stepX
                    drawLine(
                        color = cursorColor,
                        start = Offset(cursorX, padding),
                        end = Offset(cursorX, height - padding),
                        strokeWidth = 2.5f
                    )
                    drawCircle(
                        color = cursorColor,
                        radius = 5f,
                        center = Offset(cursorX, padding + chartHeight - (points[currentIndex].speedKmh / maxSpeed) * chartHeight)
                    )
                }
            }
        }
    }
}

// =============================================================================
// DRIVING DNA SUB-SCORES CARD
// =============================================================================
@Composable
private fun DrivingDnaBreakdownCard(trip: TripUiItem) {
    OverdriveCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.trips_radar_chart_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                OverdriveStatusPill(
                    status = if (trip.drivingDnaScore >= 90) OverdrivePillStatus.SUCCESS else OverdrivePillStatus.WARNING,
                    label = "${trip.drivingDnaScore} / 100",
                )
            }

            // Radar Chart Canvas
            DrivingDnaRadarChart(trip = trip)

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )

            // Linear Progress Bars
            DnaScoreBar(title = stringResource(R.string.trips_dna_anticipation), score = trip.anticipationScore)
            DnaScoreBar(title = stringResource(R.string.trips_dna_smoothness), score = trip.smoothnessScore)
            DnaScoreBar(title = stringResource(R.string.trips_dna_speed_discipline), score = trip.speedDisciplineScore)
            DnaScoreBar(title = stringResource(R.string.trips_dna_regen_efficiency), score = trip.efficiencyScore)
            DnaScoreBar(title = stringResource(R.string.trips_dna_consistency), score = trip.consistencyScore)
        }
    }
}

/**
 * 5-axis Radar / Spider Chart visualising Driving DNA metrics.
 */
@Composable
fun DrivingDnaRadarChart(
    trip: TripUiItem,
    modifier: Modifier = Modifier,
) {
    val labels = listOf(
        stringResource(R.string.trips_radar_anticipation),
        stringResource(R.string.trips_radar_smoothness),
        stringResource(R.string.trips_radar_discipline),
        stringResource(R.string.trips_radar_efficiency),
        stringResource(R.string.trips_radar_consistency),
    )
    val scores = listOf(
        trip.anticipationScore.coerceIn(0, 100) / 100f,
        trip.smoothnessScore.coerceIn(0, 100) / 100f,
        trip.speedDisciplineScore.coerceIn(0, 100) / 100f,
        trip.efficiencyScore.coerceIn(0, 100) / 100f,
        trip.consistencyScore.coerceIn(0, 100) / 100f,
    )

    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val accentColor = MaterialTheme.colorScheme.primary
    val fillColor = accentColor.copy(alpha = 0.22f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val nativePaint = remember(labelColor) {
        android.graphics.Paint().apply {
            color = labelColor.toArgb()
            textSize = 26f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f) - 34.dp.toPx()
            val sides = 5
            val angleStep = (2.0 * Math.PI / sides).toFloat()
            val startAngle = (-Math.PI / 2.0).toFloat()

            // Concentric grids
            listOf(0.25f, 0.50f, 0.75f, 1.0f).forEach { fraction ->
                val gridPath = Path()
                for (i in 0 until sides) {
                    val angle = startAngle + i * angleStep
                    val x = center.x + radius * fraction * Math.cos(angle.toDouble()).toFloat()
                    val y = center.y + radius * fraction * Math.sin(angle.toDouble()).toFloat()
                    if (i == 0) gridPath.moveTo(x, y) else gridPath.lineTo(x, y)
                }
                gridPath.close()
                drawPath(gridPath, gridColor, style = Stroke(width = 1.dp.toPx()))
            }

            // Spoke lines
            for (i in 0 until sides) {
                val angle = startAngle + i * angleStep
                val x = center.x + radius * Math.cos(angle.toDouble()).toFloat()
                val y = center.y + radius * Math.sin(angle.toDouble()).toFloat()
                drawLine(gridColor, center, Offset(x, y), strokeWidth = 1.dp.toPx())
            }

            // Data polygon
            val dataPath = Path()
            val dataPoints = ArrayList<Offset>(sides)
            for (i in 0 until sides) {
                val angle = startAngle + i * angleStep
                val r = radius * scores[i]
                val x = center.x + r * Math.cos(angle.toDouble()).toFloat()
                val y = center.y + r * Math.sin(angle.toDouble()).toFloat()
                val pt = Offset(x, y)
                dataPoints.add(pt)
                if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
            }
            dataPath.close()

            // Fill & stroke
            drawPath(dataPath, fillColor)
            drawPath(dataPath, accentColor, style = Stroke(width = 2.dp.toPx()))

            // Vertex dots
            dataPoints.forEach { pt ->
                drawCircle(color = accentColor, radius = 4.dp.toPx(), center = pt)
            }

            // Labels around polygon
            for (i in 0 until sides) {
                val angle = startAngle + i * angleStep
                val labelRadius = radius + 20.dp.toPx()
                val lx = center.x + labelRadius * Math.cos(angle.toDouble()).toFloat()
                val ly = center.y + labelRadius * Math.sin(angle.toDouble()).toFloat() + 8f
                drawContext.canvas.nativeCanvas.drawText(labels[i], lx, ly, nativePaint)
            }
        }
    }
}

@Composable
private fun DnaScoreBar(title: String, score: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "$score / 100",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (score >= 90) OverdriveTheme.colors.statusSuccess else OverdriveTheme.colors.statusWarning,
                fontFamily = FontFamily.Monospace,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { score / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (score >= 90) OverdriveTheme.colors.statusSuccess else OverdriveTheme.colors.statusWarning,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    }
}

// =============================================================================
// STORAGE & DASHCAM MANAGEMENT CARD
// =============================================================================
@Composable
private fun TripsStorageCard(
    onCleanupCdr: () -> Unit,
    onExportKml: () -> Unit,
    onExportGpx: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    OverdriveCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.trips_storage_mgmt_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.trips_storage_mgmt_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OverdriveStatusPill(
                    status = OverdrivePillStatus.SUCCESS,
                    label = "SD: Aktif",
                )
            }

            // Storage bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.trips_storage_used_fmt, "1.4 GB", "58.6 GB"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "%2.3",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                LinearProgressIndicator(
                    progress = { 0.023f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                )
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OverdriveButton(
                    text = stringResource(R.string.trips_cdr_cleanup_btn),
                    variant = OverdriveButtonVariant.OUTLINED,
                    onClick = { showConfirmDialog = true },
                    modifier = Modifier.weight(1.5f),
                )
                OverdriveButton(
                    text = stringResource(R.string.trips_export_gpx),
                    variant = OverdriveButtonVariant.TONAL,
                    onClick = onExportGpx,
                    modifier = Modifier.weight(1f),
                )
                OverdriveButton(
                    text = stringResource(R.string.trips_export_kml),
                    variant = OverdriveButtonVariant.TONAL,
                    onClick = onExportKml,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (showConfirmDialog) {
        OverdriveDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = stringResource(R.string.trips_cdr_cleanup_btn),
            positiveButtonText = "Temizle",
            onPositiveClick = {
                showConfirmDialog = false
                onCleanupCdr()
            },
            negativeButtonText = stringResource(R.string.action_cancel),
            onNegativeClick = { showConfirmDialog = false }
        ) {
            Text(
                text = "Eski BYD dashcam videoları ve seyahat telemetri arşivleri temizlenerek SD kartta yer açılacak. Korunan kilitli dosyalar silinmez.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
