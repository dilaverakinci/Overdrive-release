package com.overdrive.app.ui.daemons

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.overdrive.app.R
import com.overdrive.app.ui.component.OverdriveCard
import com.overdrive.app.ui.model.DaemonState
import com.overdrive.app.ui.model.DaemonStatus
import com.overdrive.app.ui.model.DaemonType
import com.overdrive.app.ui.model.SubprocessInfo
import com.overdrive.app.ui.model.localizedName
import com.overdrive.app.ui.theme.OverdriveTheme

data class DaemonsUiState(
    val daemons: List<DaemonState> = emptyList(),
    val isWifiAutoEnable: Boolean = false,
    val isWifiAutoEnableLoading: Boolean = false,
)

@Composable
fun DaemonsScreen(
    state: DaemonsUiState,
    onToggleDaemon: (DaemonType, Boolean) -> Unit,
    onConfigureDaemon: (DaemonType) -> Unit,
    onDownloadLog: ((DaemonType) -> Unit)? = null,
    onToggleWifiAutoEnable: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val expandedStates = remember { mutableStateMapOf<DaemonType, Boolean>() }
    val totalCount = state.daemons.size
    val runningCount = state.daemons.count { it.status == DaemonStatus.RUNNING }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            horizontal = OverdriveTheme.dimensions.pagePaddingHorizontal,
            vertical = OverdriveTheme.dimensions.pagePaddingTop
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Hero Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
            ) {
                Text(
                    text = stringResource(R.string.daemons_hero_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.daemons_count_fmt, runningCount, totalCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Wi-Fi Keep-Alive Preference Card
        item {
            WifiAutoEnableCard(
                enabled = state.isWifiAutoEnable,
                isLoading = state.isWifiAutoEnableLoading,
                onToggle = onToggleWifiAutoEnable,
            )
        }

        // Daemon Cards
        items(
            items = state.daemons,
            key = { it.type.name },
        ) { daemonState ->
            val isExpanded = expandedStates[daemonState.type] ?: false
            DaemonCardItem(
                state = daemonState,
                isExpanded = isExpanded,
                onToggleExpand = {
                    expandedStates[daemonState.type] = !isExpanded
                },
                onToggle = { enabled ->
                    onToggleDaemon(daemonState.type, enabled)
                },
                onConfigureClick = {
                    onConfigureDaemon(daemonState.type)
                },
                onDownloadLog = onDownloadLog?.let { action ->
                    { action(daemonState.type) }
                },
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WifiAutoEnableCard(
    enabled: Boolean,
    isLoading: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    OverdriveCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 12.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_signal_disconnected),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.daemons_wifi_auto_enable_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.daemons_wifi_auto_enable_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                enabled = !isLoading,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
            )
        }
    }
}

@Composable
private fun DaemonCardItem(
    state: DaemonState,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onConfigureClick: () -> Unit,
    onDownloadLog: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isRunningOrStarting = state.status == DaemonStatus.RUNNING || state.status == DaemonStatus.STARTING

    // Status dot color
    val colors = OverdriveTheme.colors
    val statusDotColor = when {
        state.needsConfiguration -> colors.statusWarning
        state.status == DaemonStatus.RUNNING -> colors.statusSuccess
        state.status == DaemonStatus.STOPPED -> MaterialTheme.colorScheme.outline
        state.status == DaemonStatus.ERROR -> colors.statusDanger
        else -> colors.statusWarning
    }

    // Status text
    val statusText = when {
        state.needsConfiguration -> state.configurationMessage
            ?: stringResource(R.string.daemon_status_needs_config)
        state.status == DaemonStatus.RUNNING -> {
            val extra = state.statusText
            val uptimeStr = state.uptime
            when {
                isEndpointStatus(extra) && !uptimeStr.isNullOrEmpty() -> "$extra • $uptimeStr"
                isEndpointStatus(extra) -> extra
                !uptimeStr.isNullOrEmpty() -> stringResource(R.string.daemon_status_running_uptime, uptimeStr)
                else -> stringResource(R.string.daemon_status_running)
            }
        }
        state.status == DaemonStatus.STOPPED -> stringResource(R.string.daemon_status_stopped)
        state.status == DaemonStatus.STARTING -> stringResource(R.string.daemon_status_starting)
        state.status == DaemonStatus.STOPPING -> stringResource(R.string.daemon_status_stopping)
        state.status == DaemonStatus.ERROR -> state.statusText.ifEmpty {
            stringResource(R.string.daemon_status_error)
        }
        else -> state.statusText
    }

    val isConfigurable = state.type == DaemonType.ZROK_TUNNEL ||
            state.type == DaemonType.TAILSCALE_TUNNEL ||
            state.type == DaemonType.CLOUDFLARED_TUNNEL ||
            state.needsConfiguration

    OverdriveCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        contentPadding = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Main Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = MaterialTheme.colorScheme.primary),
                        onClick = onToggleExpand,
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Status Indicator Dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusDotColor),
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Daemon Icon
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(getDaemonIcon(state.type)),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title + Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.type.localizedName(context),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.status == DaemonStatus.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Action Buttons
                if (isConfigurable) {
                    IconButton(
                        onClick = onConfigureClick,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.cd_configure),
                            tint = if (state.needsConfiguration) OverdriveTheme.colors.statusWarning else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                if (onDownloadLog != null) {
                    IconButton(
                        onClick = onDownloadLog,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_download_log),
                            contentDescription = stringResource(R.string.cd_download_log),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Main Switch
                Switch(
                    checked = isRunningOrStarting,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                )

                // Chevron for expansion if it has subprocesses
                if (state.subprocesses.isNotEmpty()) {
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(start = 4.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_expand),
                            contentDescription = stringResource(R.string.cd_expand),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(if (isExpanded) 180f else 0f),
                        )
                    }
                }
            }

            // Subprocesses list when expanded
            AnimatedVisibility(visible = isExpanded && state.subprocesses.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 1.dp,
                    )

                    Text(
                        text = stringResource(R.string.daemon_card_subprocesses),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    state.subprocesses.forEach { sub ->
                        SubprocessRow(sub = sub)
                    }
                }
            }
        }
    }
}

@Composable
private fun SubprocessRow(
    sub: SubprocessInfo,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.daemon_subprocess_pid, sub.name, sub.pid),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.daemon_uptime_fmt, sub.uptime),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun getDaemonIcon(type: DaemonType): Int {
    return when (type) {
        DaemonType.CAMERA_DAEMON -> R.drawable.ic_camera_select
        DaemonType.SENTRY_DAEMON -> R.drawable.ic_sentry
        DaemonType.ACC_SENTRY_DAEMON -> R.drawable.ic_directions_car
        DaemonType.SINGBOX_PROXY -> R.drawable.ic_vpn_lock
        DaemonType.CLOUDFLARED_TUNNEL -> R.drawable.ic_cloud
        DaemonType.ZROK_TUNNEL -> R.drawable.ic_link
        DaemonType.TAILSCALE_TUNNEL -> R.drawable.ic_mqtt
        DaemonType.TELEGRAM_DAEMON -> R.drawable.ic_telegram
    }
}

private fun isEndpointStatus(status: String?): Boolean {
    if (status.isNullOrEmpty()) return false
    return status.contains("http://", ignoreCase = true) ||
            status.contains("https://", ignoreCase = true) ||
            status.contains(".zrok.io", ignoreCase = true) ||
            status.contains(".trycloudflare.com", ignoreCase = true)
}
