package com.overdrive.app.ui.integrations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.overdrive.app.R
import com.overdrive.app.ui.component.OverdriveCard
import com.overdrive.app.ui.component.OverdrivePillStatus
import com.overdrive.app.ui.component.OverdriveStatusPill
import com.overdrive.app.ui.theme.OverdriveDimensions
import com.overdrive.app.ui.theme.OverdriveTheme

/**
 * State representing all third-party integration statuses.
 */
data class IntegrationsUiState(
    val telegramConfigured: Boolean = false,
    val abrpConnected: Boolean = false,
    val mqttConnected: Boolean = false,
    val bydCloudConfigured: Boolean = false,
    val safeKeepConfigured: Boolean = false,
) {
    val allReady: Boolean
        get() = telegramConfigured && abrpConnected && mqttConnected && bydCloudConfigured
}

/**
 * 100% Jetpack Compose Native Integrations Screen.
 * Complete replacement for legacy XML fragment_integrations.
 *
 * Implements 4-column side-by-side cards for Telegram, ABRP, MQTT, BYD Cloud
 * and a full-width SafeKeep backup card below them, sized cleanly to fit
 * within the automotive viewport without scrolling.
 */
@Composable
fun IntegrationsScreen(
    state: IntegrationsUiState,
    onTelegramClick: () -> Unit = {},
    onAbrpClick: () -> Unit = {},
    onMqttClick: () -> Unit = {},
    onBydCloudClick: () -> Unit = {},
    onSafeKeepClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    start = OverdriveDimensions.pagePaddingHorizontal,
                    end = OverdriveDimensions.pagePaddingHorizontal,
                    top = OverdriveDimensions.pagePaddingTop,
                    bottom = OverdriveDimensions.pagePaddingBottom,
                ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header / Hero Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.integrations_hero_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OverdriveStatusPill(
                    status = if (state.allReady) OverdrivePillStatus.SUCCESS else OverdrivePillStatus.INFO,
                    label = if (state.allReady) {
                        stringResource(R.string.integrations_status_configured)
                    } else {
                        stringResource(R.string.integrations_status_unknown)
                    },
                )
            }

            // Top 4-Column Row (Telegram, ABRP, MQTT, BYD Cloud)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Telegram Card
                IntegrationColumnCard(
                    title = stringResource(R.string.nav_page_telegram),
                    description = stringResource(R.string.integrations_telegram_desc),
                    iconRes = R.drawable.ic_telegram,
                    isConfigured = state.telegramConfigured,
                    onConfigureClick = onTelegramClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )

                // ABRP Card
                IntegrationColumnCard(
                    title = stringResource(R.string.nav_page_abrp),
                    description = stringResource(R.string.integrations_abrp_desc),
                    iconRes = R.drawable.ic_route,
                    isConfigured = state.abrpConnected,
                    onConfigureClick = onAbrpClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )

                // MQTT Card
                IntegrationColumnCard(
                    title = stringResource(R.string.nav_page_mqtt),
                    description = stringResource(R.string.integrations_mqtt_desc),
                    iconRes = R.drawable.ic_mqtt,
                    isConfigured = state.mqttConnected,
                    onConfigureClick = onMqttClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )

                // BYD Cloud Card
                IntegrationColumnCard(
                    title = stringResource(R.string.nav_page_byd_cloud),
                    description = stringResource(R.string.integrations_byd_cloud_desc),
                    iconRes = R.drawable.ic_cloud,
                    isConfigured = state.bydCloudConfigured,
                    onConfigureClick = onBydCloudClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }

            // Bottom SafeKeep Card (Spanning underneath the 4 columns)
            SafeKeepCard(
                isConfigured = state.safeKeepConfigured,
                onConfigureClick = onSafeKeepClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun IntegrationColumnCard(
    title: String,
    description: String,
    iconRes: Int,
    isConfigured: Boolean,
    onConfigureClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OverdriveCard(
        modifier = modifier,
        onClick = onConfigureClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Status dot indicator: • NOT SET UP / • CONFIGURED
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (isConfigured) OverdriveTheme.colors.statusSuccess
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                    )
                    Text(
                        text = if (isConfigured) {
                            stringResource(R.string.integrations_status_configured).uppercase()
                        } else {
                            stringResource(R.string.integrations_status_not_set_up).uppercase()
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            fontSize = 11.sp
                        ),
                        color = if (isConfigured) OverdriveTheme.colors.statusSuccess
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Title + Service Icon Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Description (3 lines)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 3,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button: Configure
            Button(
                onClick = onConfigureClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OverdriveTheme.colors.primaryContainer,
                    contentColor = OverdriveTheme.colors.onPrimaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Text(
                    text = stringResource(R.string.integrations_configure_button),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun SafeKeepCard(
    isConfigured: Boolean,
    onConfigureClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OverdriveCard(
        modifier = modifier,
        onClick = onConfigureClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // SafeKeep Tray Download Icon
                Icon(
                    painter = painterResource(R.drawable.ic_safekeep),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                )

                // Info Column
                Column(modifier = Modifier.weight(1f)) {
                    // Status indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isConfigured) OverdriveTheme.colors.statusSuccess
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                        )
                        Text(
                            text = if (isConfigured) {
                                stringResource(R.string.integrations_status_configured).uppercase()
                            } else {
                                stringResource(R.string.integrations_status_not_set_up).uppercase()
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                fontSize = 11.sp
                            ),
                            color = if (isConfigured) OverdriveTheme.colors.statusSuccess
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Title
                    Text(
                        text = stringResource(R.string.integrations_safekeep_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Description
                    Text(
                        text = stringResource(R.string.integrations_safekeep_desc),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Action Button
            Button(
                onClick = onConfigureClick,
                modifier = Modifier
                    .width(130.dp)
                    .height(38.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OverdriveTheme.colors.primaryContainer,
                    contentColor = OverdriveTheme.colors.onPrimaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Text(
                    text = stringResource(R.string.integrations_configure_button),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                )
            }
        }
    }
}
