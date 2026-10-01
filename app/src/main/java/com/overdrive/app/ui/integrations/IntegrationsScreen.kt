package com.overdrive.app.ui.integrations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.overdrive.app.R
import com.overdrive.app.ui.component.OverdriveButton
import com.overdrive.app.ui.component.OverdriveButtonVariant
import com.overdrive.app.ui.component.OverdriveCard
import com.overdrive.app.ui.component.OverdrivePillStatus
import com.overdrive.app.ui.component.OverdriveStatusPill
import com.overdrive.app.ui.theme.OverdriveDimensions

/**
 * State representing all third-party integration statuses.
 */
data class IntegrationsUiState(
    val telegramConfigured: Boolean = false,
    val abrpConnected: Boolean = false,
    val mqttConnected: Boolean = false,
    val bydCloudConfigured: Boolean = false,
) {
    val allReady: Boolean
        get() = telegramConfigured && abrpConnected && mqttConnected && bydCloudConfigured
}

/**
 * 100% Jetpack Compose Native Integrations Screen.
 * Complete replacement for legacy XML fragment_integrations.
 */
@Composable
fun IntegrationsScreen(
    state: IntegrationsUiState,
    onTelegramClick: () -> Unit = {},
    onAbrpClick: () -> Unit = {},
    onMqttClick: () -> Unit = {},
    onBydCloudClick: () -> Unit = {},
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
            // Header / Hero Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.integrations_hero_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                OverdriveStatusPill(
                    status = if (state.allReady) OverdrivePillStatus.SUCCESS else OverdrivePillStatus.INFO,
                    label = if (state.allReady) {
                        stringResource(R.string.integrations_status_configured)
                    } else {
                        stringResource(R.string.integrations_status_unknown)
                    },
                )
            }

            // Telegram Card
            IntegrationCard(
                title = stringResource(R.string.nav_page_telegram),
                description = stringResource(R.string.integrations_telegram_desc),
                iconRes = R.drawable.ic_telegram,
                isConfigured = state.telegramConfigured,
                onConfigureClick = onTelegramClick,
            )

            // ABRP Card
            IntegrationCard(
                title = stringResource(R.string.nav_page_abrp),
                description = stringResource(R.string.integrations_abrp_desc),
                iconRes = R.drawable.ic_route,
                isConfigured = state.abrpConnected,
                onConfigureClick = onAbrpClick,
            )

            // MQTT Card
            IntegrationCard(
                title = stringResource(R.string.nav_page_mqtt),
                description = stringResource(R.string.integrations_mqtt_desc),
                iconRes = R.drawable.ic_mqtt,
                isConfigured = state.mqttConnected,
                onConfigureClick = onMqttClick,
            )

            // BYD Cloud Card
            IntegrationCard(
                title = stringResource(R.string.nav_page_byd_cloud),
                description = stringResource(R.string.integrations_byd_cloud_desc),
                iconRes = R.drawable.ic_cloud,
                isConfigured = state.bydCloudConfigured,
                onConfigureClick = onBydCloudClick,
            )
        }
    }
}

@Composable
private fun IntegrationCard(
    title: String,
    description: String,
    iconRes: Int,
    isConfigured: Boolean,
    onConfigureClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OverdriveCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onConfigureClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status Pill
            OverdriveStatusPill(
                status = if (isConfigured) OverdrivePillStatus.SUCCESS else OverdrivePillStatus.WARNING,
                label = if (isConfigured) {
                    stringResource(R.string.integrations_status_configured)
                } else {
                    stringResource(R.string.integrations_status_not_set_up)
                },
            )

            // Header: Title and Service Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
            }

            // Description
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Action Button
            OverdriveButton(
                text = stringResource(R.string.integrations_configure_button),
                variant = OverdriveButtonVariant.TONAL,
                onClick = onConfigureClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
