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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.overdrive.app.R
import com.overdrive.app.ui.component.OverdriveButton
import com.overdrive.app.ui.component.OverdriveButtonVariant
import com.overdrive.app.ui.component.OverdriveCard
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
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
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
            )
        } else {
            TripsMasterView(
                state = state,
                onFilterSelected = onFilterSelected,
                onTripClick = onTripClick,
                onExportClick = onExportClick,
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header Bar
        TripsHeader(
            filter = state.filter,
            onFilterSelected = onFilterSelected,
            onExportClick = onExportClick,
        )

        // Hero Aggregates Card
        TripsHeroCard(state = state)

        // Section Title
        Text(
            text = "Son Sürüş Oturumları (${state.trips.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        // Trip Cards List
        state.trips.forEach { trip ->
            TripItemCard(
                trip = trip,
                onClick = { onTripClick(trip) }
            )
        }
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
        Column {
            Text(
                text = stringResource(R.string.rail_trips),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Sürüş Tüketimi, Verimlilik ve Driving DNA Analizi",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

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
                    text = "Tümü",
                    isSelected = filter == TripsFilterPeriod.ALL,
                    onClick = { onFilterSelected(TripsFilterPeriod.ALL) }
                )
                FilterTabButton(
                    text = "Bu Hafta",
                    isSelected = filter == TripsFilterPeriod.THIS_WEEK,
                    onClick = { onFilterSelected(TripsFilterPeriod.THIS_WEEK) }
                )
                FilterTabButton(
                    text = "Bu Ay",
                    isSelected = filter == TripsFilterPeriod.THIS_MONTH,
                    onClick = { onFilterSelected(TripsFilterPeriod.THIS_MONTH) }
                )
            }

            OverdriveButton(
                text = "Dışa Aktar",
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
                    text = "Toplam Sürüş Mesafesi",
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
                    text = "${state.totalTripsCount} Seyahat · ${String.format(Locale.US, "%.1f", state.totalDurationHours)} Saat Toplam",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Right side stats
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Ort. Verimlilik",
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
                        text = "kWh / 100km",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Sürüş DNA",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${state.overallDnaScore}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = OverdriveTheme.colors.statusSuccess,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = "100 Puan Üzerinden",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TripItemCard(
    trip: TripUiItem,
    onClick: () -> Unit,
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
                        label = "${trip.drivingDnaScore} DNA Skoru",
                    )
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
                TripStatColumn(label = "Mesafe", value = "${String.format(Locale.US, "%.1f", trip.distanceKm)} km")
                TripStatColumn(label = "Süre", value = "${trip.durationMinutes} dk")
                TripStatColumn(label = "Ort. Hız", value = "${trip.avgSpeedKmh.toInt()} km/s")
                TripStatColumn(label = "Tüketim", value = "${String.format(Locale.US, "%.1f", trip.energyUsedKwh)} kWh")
                TripStatColumn(label = "Verimlilik", value = "${String.format(Locale.US, "%.1f", trip.efficiencyKwhPer100Km)} kWh/100km")
                TripStatColumn(label = "Batarya (SoC)", value = "%${trip.socStart} ➔ %${trip.socEnd}")
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
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
                    text = "← Seyahatlere Dön",
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
                        text = "Seyahat #${trip.id} · Detaylı GPS Telemetrisi ve Sürüş Simülasyonu",
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
                    label = "${trip.drivingDnaScore} DNA Skoru",
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

        // Interactive Route Map Card
        if (points.isNotEmpty() && currentPoint != null) {
            TripRouteMapCard(
                points = points,
                currentPoint = currentPoint,
                currentIndex = safeIndex,
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
                text = "Seyahat Özeti ve Enerji Metrikleri",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                DetailKpiTile(label = "Mesafe", value = "${String.format(Locale.US, "%.1f", trip.distanceKm)} km", highlight = true)
                DetailKpiTile(label = "Süre", value = "${trip.durationMinutes} dk")
                DetailKpiTile(label = "SoC Tüketimi", value = "%${trip.socStart - trip.socEnd}", subtext = "%${trip.socStart} ➔ %${trip.socEnd}")
                DetailKpiTile(label = "Harcanan Enerji", value = "${String.format(Locale.US, "%.1f", trip.energyUsedKwh)} kWh")
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                DetailKpiTile(label = "Ortalama Hız", value = "${trip.avgSpeedKmh.toInt()} km/s")
                DetailKpiTile(label = "Azami Hız", value = "${trip.maxSpeedKmh} km/s")
                DetailKpiTile(label = "Verimlilik", value = "${String.format(Locale.US, "%.1f", trip.efficiencyKwhPer100Km)} kWh/100km")
                DetailKpiTile(label = "Seyahat Maliyeti", value = trip.tripCostFormatted ?: "--")
                DetailKpiTile(label = "Yükseklik", value = "↑ ${trip.elevationGainM}m · ↓ ${trip.elevationLossM}m")
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
                        text = "Seyahat Zaman Çizelgesi (Timeline Scrubber)",
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
                    HudBadge(label = "Hız", value = "${currentPoint.speedKmh} km/s", isAccent = true)
                    HudBadge(label = "Gaz", value = "%${currentPoint.accelPedalPercent}")
                    HudBadge(label = "Fren", value = "%${currentPoint.brakePedalPercent}")
                    HudBadge(label = "SoC", value = String.format(Locale.US, "%%%.1f", currentPoint.socPercent))
                    HudBadge(label = "Güç", value = String.format(Locale.US, "%.1f kW", currentPoint.powerKw))
                }
            }

            // Slider Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
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
                        text = if (isPlaying) "⏸ Duraklat" else "▶ Oynat",
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
                    text = "Nokta ${currentIndex + 1} / ${points.size} · GPS: ${String.format(Locale.US, "%.4f, %.4f", currentPoint.lat, currentPoint.lon)}",
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
// INTERACTIVE ROUTE MAP CARD
// =============================================================================
@Composable
private fun TripRouteMapCard(
    points: List<TripTelemetryPoint>,
    currentPoint: TripTelemetryPoint,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
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
                Text(
                    text = "Rota Haritası (GPS Trace)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MapSpeedLegendDot(color = OverdriveTheme.colors.statusSuccess, label = "<40 km/s")
                    MapSpeedLegendDot(color = OverdriveTheme.colors.statusWarning, label = "40–80 km/s")
                    MapSpeedLegendDot(color = OverdriveTheme.colors.statusDanger, label = ">80 km/s")
                }
            }

            // Map Viewport Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                TripRouteCanvas(
                    points = points,
                    currentPoint = currentPoint,
                    currentIndex = currentIndex,
                    modifier = Modifier.fillMaxSize(),
                )

                // Current heading / altitude badge on top-right of map
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Yön: ${currentPoint.headingDegrees.toInt()}° · İrtifa: ${currentPoint.altitudeM.toInt()} m",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}

@Composable
private fun MapSpeedLegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TripRouteCanvas(
    points: List<TripTelemetryPoint>,
    currentPoint: TripTelemetryPoint,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    var minLat = Double.MAX_VALUE
    var maxLat = -Double.MAX_VALUE
    var minLon = Double.MAX_VALUE
    var maxLon = -Double.MAX_VALUE

    for (p in points) {
        if (p.lat < minLat) minLat = p.lat
        if (p.lat > maxLat) maxLat = p.lat
        if (p.lon < minLon) minLon = p.lon
        if (p.lon > maxLon) maxLon = p.lon
    }

    val deltaLat = (maxLat - minLat).coerceAtLeast(0.0005)
    val deltaLon = (maxLon - minLon).coerceAtLeast(0.0005)
    val centerLat = (minLat + maxLat) / 2.0
    val centerLon = (minLon + maxLon) / 2.0
    val latCos = Math.cos(Math.toRadians(centerLat))

    val colors = OverdriveTheme.colors
    val primaryColor = colors.primary
    val gridColor = colors.outlineVariant.copy(alpha = 0.25f)
    val successColor = colors.statusSuccess
    val warningColor = colors.statusWarning
    val dangerColor = colors.statusDanger

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padding = 40f

        val drawWidth = (width - padding * 2).coerceAtLeast(10f)
        val drawHeight = (height - padding * 2).coerceAtLeast(10f)

        val geoWidth = deltaLon * latCos
        val geoHeight = deltaLat

        val scale = Math.min(drawWidth / geoWidth, drawHeight / geoHeight).toFloat()
        val centerX = width / 2f
        val centerY = height / 2f

        fun toCanvasOffset(lat: Double, lon: Double): Offset {
            val x = centerX + ((lon - centerLon) * latCos * scale).toFloat()
            val y = centerY - ((lat - centerLat) * scale).toFloat()
            return Offset(x, y)
        }

        // 1. Grid pattern for automotive map feel
        val gridStep = 45f
        var gx = 0f
        while (gx < width) {
            drawLine(gridColor, Offset(gx, 0f), Offset(gx, height), strokeWidth = 1f)
            gx += gridStep
        }
        var gy = 0f
        while (gy < height) {
            drawLine(gridColor, Offset(0f, gy), Offset(width, gy), strokeWidth = 1f)
            gy += gridStep
        }

        // 2. Speed-colored Route Polyline
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val o1 = toCanvasOffset(p1.lat, p1.lon)
            val o2 = toCanvasOffset(p2.lat, p2.lon)

            val segColor = when {
                p1.speedKmh < 40 -> successColor
                p1.speedKmh <= 80 -> warningColor
                else -> dangerColor
            }

            // Outer soft glow
            drawLine(
                color = segColor.copy(alpha = 0.35f),
                start = o1,
                end = o2,
                strokeWidth = 10f,
                cap = StrokeCap.Round
            )
            // Inner crisp ribbon
            drawLine(
                color = segColor,
                start = o1,
                end = o2,
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )
        }

        // 3. Start Pin (Green)
        val startOffset = toCanvasOffset(points.first().lat, points.first().lon)
        drawCircle(color = Color.White, radius = 9f, center = startOffset)
        drawCircle(color = successColor, radius = 7f, center = startOffset)

        // 4. End Pin (Red)
        val endOffset = toCanvasOffset(points.last().lat, points.last().lon)
        drawCircle(color = Color.White, radius = 9f, center = endOffset)
        drawCircle(color = dangerColor, radius = 7f, center = endOffset)

        // 5. Interactive Vehicle Marker (Rotated by heading at currentPoint)
        val carOffset = toCanvasOffset(currentPoint.lat, currentPoint.lon)

        // Pulsing radar glow
        drawCircle(
            color = primaryColor.copy(alpha = 0.22f),
            radius = 26f,
            center = carOffset
        )
        drawCircle(
            color = primaryColor.copy(alpha = 0.45f),
            radius = 16f,
            center = carOffset
        )

        // Oriented top-down arrow
        val headingRad = Math.toRadians(currentPoint.headingDegrees.toDouble())
        val headingVec = Offset(
            Math.sin(headingRad).toFloat(),
            -Math.cos(headingRad).toFloat()
        )
        val perpVec = Offset(-headingVec.y, headingVec.x)

        val carLength = 22f
        val carWidth = 12f

        val nose = carOffset + headingVec * (carLength * 0.6f)
        val tailLeft = carOffset - headingVec * (carLength * 0.4f) + perpVec * (carWidth * 0.5f)
        val tailRight = carOffset - headingVec * (carLength * 0.4f) - perpVec * (carWidth * 0.5f)

        val carPath = Path().apply {
            moveTo(nose.x, nose.y)
            lineTo(tailLeft.x, tailLeft.y)
            lineTo(carOffset.x - headingVec.x * (carLength * 0.2f), carOffset.y - headingVec.y * (carLength * 0.2f))
            lineTo(tailRight.x, tailRight.y)
            close()
        }

        drawPath(carPath, color = primaryColor)
        drawPath(carPath, color = Color.White, style = Stroke(width = 2f))
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
                    text = "Hız ve Pedal Grafiği (Dynamics Profile)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Hız (km/s) / Zaman",
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Driving DNA Puanlama Dağılımı",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            DnaScoreBar(title = "Öngörü & Mesafe Kontrolü", score = trip.anticipationScore)
            DnaScoreBar(title = "Akıcılık & G-Kuvveti Dengesi", score = trip.smoothnessScore)
            DnaScoreBar(title = "Hız Disiplini & Limit Uyumu", score = trip.speedDisciplineScore)
            DnaScoreBar(title = "Enerji Verimliliği & Rejenerasyon", score = trip.efficiencyScore)
            DnaScoreBar(title = "Tutarlılık & Sürüş Stabilitesi", score = trip.consistencyScore)
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
