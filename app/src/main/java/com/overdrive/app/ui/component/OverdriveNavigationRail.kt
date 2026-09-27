package com.overdrive.app.ui.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.overdrive.app.R
import com.overdrive.app.ui.theme.OverdriveDimensions

/**
 * Data model for a destination item inside the navigation rail.
 */
data class OverdriveRailDestination(
    val key: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val destinationId: Int = 0,
    val ownedDestinationIds: Set<Int> = emptySet(),
    val badgeText: String? = null,
)

/**
 * Section grouping for destinations (e.g. Cameras, Controls, Driving, System).
 */
data class OverdriveRailSectionGroup(
    val id: String,
    @StringRes val titleRes: Int,
    val items: List<OverdriveRailDestination>,
)

/**
 * Modern Material 3 Navigation Rail for Overdrive.
 * Fully supports smooth transition between compact (80dp) and expanded (216dp) states,
 * adhering 1:1 to the native XML design tokens, padding, and animations.
 */
@Composable
fun OverdriveNavigationRail(
    selectedKey: String,
    isExpanded: Boolean,
    onDestinationClick: (OverdriveRailDestination) -> Unit,
    onToggleExpanded: () -> Unit,
    primaryDestinations: List<OverdriveRailDestination>,
    sections: List<OverdriveRailSectionGroup>,
    bottomDestinations: List<OverdriveRailDestination> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val railWidth by animateDpAsState(
        targetValue = if (isExpanded) OverdriveDimensions.railExpandedWidth else OverdriveDimensions.railCompactWidth,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "RailWidthAnimation"
    )

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "ChevronRotationAnimation"
    )

    Surface(
        modifier = modifier
            .width(railWidth)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // App Brand Wordmark
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 12.dp),
                contentAlignment = if (isExpanded) Alignment.CenterStart else Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = if (isExpanded) TextAlign.Start else TextAlign.Center,
                )
            }

            // Scrollable Navigation Item List
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Primary unsectioned items (e.g. Dashboard, Assistant)
                primaryDestinations.forEach { item ->
                    NavigationRailRow(
                        destination = item,
                        isSelected = item.key == selectedKey || item.ownedDestinationIds.contains(item.destinationId),
                        isExpanded = isExpanded,
                        onClick = { onDestinationClick(item) },
                    )
                }

                // Sectioned destinations
                sections.forEach { section ->
                    if (section.items.isNotEmpty()) {
                        RailSectionHeader(
                            titleRes = section.titleRes,
                            isExpanded = isExpanded,
                        )
                        section.items.forEach { item ->
                            NavigationRailRow(
                                destination = item,
                                isSelected = item.key == selectedKey || item.ownedDestinationIds.contains(item.destinationId),
                                isExpanded = isExpanded,
                                onClick = { onDestinationClick(item) },
                            )
                        }
                    }
                }

                // Bottom destinations (e.g. Settings, About)
                bottomDestinations.forEach { item ->
                    NavigationRailRow(
                        destination = item,
                        isSelected = item.key == selectedKey || item.ownedDestinationIds.contains(item.destinationId),
                        isExpanded = isExpanded,
                        onClick = { onDestinationClick(item) },
                    )
                }
            }

            // Bottom Collapse / Expand Button Row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(OverdriveDimensions.railItemPillRadius))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true),
                        onClick = onToggleExpanded,
                    )
                    .padding(horizontal = OverdriveDimensions.railItemPillInsetHorizontal),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier.size(OverdriveDimensions.railCompactWidth - (OverdriveDimensions.railItemPillInsetHorizontal * 2)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_right),
                            contentDescription = stringResource(R.string.rail_collapse),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(chevronRotation),
                        )
                    }

                    if (isExpanded) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.rail_collapse),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationRailRow(
    destination: OverdriveRailDestination,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 200),
        label = "RailRowBg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 200),
        label = "RailRowContentColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = OverdriveDimensions.railItemPillInsetHorizontal,
                vertical = OverdriveDimensions.railItemPillInsetVertical,
            )
            .clip(RoundedCornerShape(OverdriveDimensions.railItemPillRadius))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .height(52.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center,
        ) {
            // Icon holder with fixed compact width for alignment
            Box(
                modifier = Modifier.size(OverdriveDimensions.railCompactWidth - (OverdriveDimensions.railItemPillInsetHorizontal * 2)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(destination.iconRes),
                    contentDescription = stringResource(destination.labelRes),
                    tint = contentColor,
                    modifier = Modifier.size(24.dp),
                )
            }

            // Expanded Label
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(100)),
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(destination.labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )

                    if (!destination.badgeText.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        OverdriveStatusPill(
                            label = destination.badgeText,
                            status = OverdrivePillStatus.INFO,
                            showDot = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RailSectionHeader(
    @StringRes titleRes: Int,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isExpanded) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = stringResource(titleRes).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold,
                letterSpacing = androidx.compose.ui.unit.TextUnit(1.2f, androidx.compose.ui.unit.TextUnitType.Sp),
            )
        }
    } else {
        HorizontalDivider(
            modifier = modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            thickness = 1.dp,
        )
    }
}
