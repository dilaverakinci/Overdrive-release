package com.overdrive.app.ui.trips

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.overdrive.app.ui.component.OverdrivePillStatus
import com.overdrive.app.ui.component.OverdriveStatusPill
import com.overdrive.app.ui.theme.OverdriveDimensions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 100% Jetpack Compose Native Trips Screen.
 * Complete 1:1 replacement for the legacy WebView /trips page.
 */
@Composable
fun TripsScreen(
    state: TripsUiState,
    onFilterSelected: (TripsFilterPeriod) -> Unit = {},
    onTripClick: (TripUiItem) -> Unit = {},
    onExportClick: () -> Unit = {},
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
}

// -----------------------------------------------------------------------------
// HEADER & FILTER SELECTOR
// -----------------------------------------------------------------------------
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

// -----------------------------------------------------------------------------
// HERO AGGREGATES CARD
// -----------------------------------------------------------------------------
@Composable
private fun TripsHeroCard(
    state: TripsUiState,
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
                // Distance & Trips
                Column {
                    Text(
                        text = "Toplam Sürüş Mesafesi",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%,.1f km", state.totalDistanceKm),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "${state.totalTripsCount} Seyahat · ${state.totalDurationHours} Saat Toplam",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Average Efficiency & DNA Score
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Ort. Verimlilik",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${state.overallEfficiencyKwhPer100Km}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
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
                        Text(
                            text = "${state.overallDnaScore}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary,
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
}

// -----------------------------------------------------------------------------
// TRIP ITEM CARD
// -----------------------------------------------------------------------------
@Composable
private fun TripItemCard(
    trip: TripUiItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormat = SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault())
    val startString = dateFormat.format(Date(trip.startTimeMs))
    val endFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val endString = endFormat.format(Date(trip.endTimeMs))

    OverdriveCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OverdriveDimensions.cardPaddingStandard)
        ) {
            // Row 1: Time, Distance, DNA Score
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
                        text = "$startString - $endString",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    OverdriveStatusPill(
                        label = trip.kinematicState,
                        status = OverdrivePillStatus.INFO,
                    )
                }

                OverdriveStatusPill(
                    label = "${trip.drivingDnaScore} DNA Skoru",
                    status = if (trip.drivingDnaScore >= 90) OverdrivePillStatus.SUCCESS else OverdrivePillStatus.WARNING,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Metrics Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TripMetric(
                    label = "Mesafe",
                    value = String.format("%.1f km", trip.distanceKm),
                )
                TripMetric(
                    label = "Süre",
                    value = "${trip.durationMinutes} dk",
                )
                TripMetric(
                    label = "Ort. Hız",
                    value = String.format("%.0f km/s", trip.avgSpeedKmh),
                )
                TripMetric(
                    label = "Tüketim",
                    value = String.format("%.1f kWh", trip.energyUsedKwh),
                )
                TripMetric(
                    label = "Verimlilik",
                    value = String.format("%.1f", trip.efficiencyKwhPer100Km),
                    unit = "kWh/100km",
                )
                TripMetric(
                    label = "Batarya (SoC)",
                    value = "%${trip.socStart} ➔ %${trip.socEnd}",
                )
            }
        }
    }
}

@Composable
private fun TripMetric(
    label: String,
    value: String,
    unit: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (unit != null) {
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
