package com.overdrive.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.overdrive.app.ui.component.OverdriveCard
import com.overdrive.app.ui.component.OverdriveDialog
import com.overdrive.app.ui.component.OverdrivePillStatus
import com.overdrive.app.ui.component.OverdriveStatusPill
import com.overdrive.app.ui.theme.OverdriveTheme

data class SettingsUiState(
    val themeModeLabel: String = "Sistem / Otomatik",
    val languageLabel: String = "Türkçe",
    val cameraOverlayEnabled: Boolean = false,
    val tripOverlayEnabled: Boolean = false,
    val roadSenseOverlayEnabled: Boolean = false,
    val telegramConnected: Boolean = false,
    val abrpConnected: Boolean = false,
    val mqttConnected: Boolean = false,
    val bydCloudConnected: Boolean = false,
    val installedVersion: String = "",
    val appId: String = "",
    val showResetDialog: Boolean = false
)

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onOpenThemePicker: () -> Unit,
    onOpenLanguagePicker: () -> Unit,
    onNavigateRecording: () -> Unit,
    onNavigateSurveillance: () -> Unit,
    onNavigateSecurity: () -> Unit,
    onNavigateDaemons: () -> Unit,
    onNavigateTelegram: () -> Unit,
    onNavigateAbrp: () -> Unit,
    onNavigateMqtt: () -> Unit,
    onNavigateBydCloud: () -> Unit,
    onToggleCameraOverlay: (Boolean) -> Unit,
    onToggleTripOverlay: (Boolean) -> Unit,
    onToggleRoadSenseOverlay: (Boolean) -> Unit,
    onOpenResetDialog: () -> Unit,
    onConfirmReset: () -> Unit,
    onDismissResetDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ayarlar",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Arayüz tercihleri, güvenlik, katmanlar ve entegrasyon ayarları.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // 1. Quick Tiles (Theme & Language)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickTile(
                    icon = Icons.Default.Palette,
                    label = "Tema",
                    value = state.themeModeLabel,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenThemePicker
                )

                QuickTile(
                    icon = Icons.Default.Translate,
                    label = "Dil",
                    value = state.languageLabel,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenLanguagePicker
                )
            }

            // 2. Preferences Navigation Cards
            Text(
                text = "TERCİHLER VE ÖZELLİKLER",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )

            OverdriveCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsNavRow(
                        icon = Icons.Default.Videocam,
                        title = "Kayıt Ayarları",
                        subtitle = "Çözünürlük, depolama, döngüsel kayıt ve tampon süresi",
                        onClick = onNavigateRecording
                    )

                    SettingsDivider()

                    SettingsNavRow(
                        icon = Icons.Default.Shield,
                        title = "Gözetim ve Güvenlik (Sentry)",
                        subtitle = "Nöbet modu, tetikleyiciler, video analizi ve caydırıcı eylemler",
                        onClick = onNavigateSurveillance
                    )

                    SettingsDivider()

                    SettingsNavRow(
                        icon = Icons.Default.Lock,
                        title = "PIN Kilidi ve Güvenlik",
                        subtitle = "Ayarlar ve video erişimi için 4-6 haneli kilit",
                        onClick = onNavigateSecurity
                    )

                    SettingsDivider()

                    SettingsNavRow(
                        icon = Icons.Default.Settings,
                        title = "Arka Plan Hizmetleri (Daemons)",
                        subtitle = "CAN veriyolu, kamera sunucusu, web soket ve sistem servisleri",
                        onClick = onNavigateDaemons
                    )
                }
            }

            // 3. Remote Integrations Quick Hub
            Text(
                text = "UZAKTAN İLETİŞİM ENTEGRASYONLARI",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )

            OverdriveCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    IntegrationNavRow(
                        icon = Icons.AutoMirrored.Filled.Send,
                        title = "Telegram Botu",
                        subtitle = "Uzaktan video bildirimleri, komut ve konum sorgulama",
                        isConnected = state.telegramConnected,
                        onClick = onNavigateTelegram
                    )

                    SettingsDivider()

                    IntegrationNavRow(
                        icon = Icons.Default.ElectricCar,
                        title = "A Better Routeplanner (ABRP)",
                        subtitle = "Canlı batarya SOC, tüketim ve rota telemetrisi senkronizasyonu",
                        isConnected = state.abrpConnected,
                        onClick = onNavigateAbrp
                    )

                    SettingsDivider()

                    IntegrationNavRow(
                        icon = Icons.Default.Hub,
                        title = "MQTT Broker & Home Assistant",
                        subtitle = "Akıllı ev sistemlerine CAN telemetri ve durum yayını",
                        isConnected = state.mqttConnected,
                        onClick = onNavigateMqtt
                    )

                    SettingsDivider()

                    IntegrationNavRow(
                        icon = Icons.Default.Cloud,
                        title = "BYD Cloud Hesabı",
                        subtitle = "Uzaktan kapı kilidi, ışık/korna kontrolü ve bulut veri alma",
                        isConnected = state.bydCloudConnected,
                        onClick = onNavigateBydCloud
                    )
                }
            }

            // 4. Overlays Card
            Text(
                text = "EKRAN ÜSTÜ KATMANLAR (OVERLAY)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )

            OverdriveCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 12.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bilgi Katmanı Görünürlüğü",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OverlaySwitchRow(
                        title = "Kamera Durum Katmanı",
                        subtitle = "Kamera kayıt ve durum hapını diğer uygulamaların üzerinde göster",
                        checked = state.cameraOverlayEnabled,
                        onCheckedChange = onToggleCameraOverlay
                    )

                    SettingsDivider()

                    OverlaySwitchRow(
                        title = "Yolculuk Bilgi Katmanı (Trip)",
                        subtitle = "Hız, tüketim ve menzil özetini ekranda sabit tut",
                        checked = state.tripOverlayEnabled,
                        onCheckedChange = onToggleTripOverlay
                    )

                    SettingsDivider()

                    OverlaySwitchRow(
                        title = "RoadSense Yol Algılama Katmanı",
                        subtitle = "Yoldaki çukur ve sarsıntı uyarılarını sürüş sırasında anlık yansıt",
                        checked = state.roadSenseOverlayEnabled,
                        onCheckedChange = onToggleRoadSenseOverlay
                    )
                }
            }

            // 5. Destructive Action (Reset Data)
            Text(
                text = "VERİ VE SIFIRLAMA",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )

            OverdriveCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 12.dp,
                borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                onClick = onOpenResetDialog
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Uygulama Verilerini Sıfırla",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Kayıtlı oturumları, yapılandırmaları ve önbelleği fabrika ayarlarına döndürür.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 6. Footer
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "OverDrive ${state.installedVersion.ifEmpty { "v51.8" }} · ${state.appId.ifEmpty { "com.overdrive.app" }}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    // Reset confirmation dialog
    if (state.showResetDialog) {
        OverdriveDialog(
            onDismissRequest = onDismissResetDialog,
            title = "Tüm Veriler Sıfırlansın mı?",
            positiveButtonText = "Tümünü Sıfırla",
            onPositiveClick = onConfirmReset,
            negativeButtonText = "İptal",
            onNegativeClick = onDismissResetDialog
        ) {
            Text(
                text = "Bu işlem tüm yerel ayarları, kamera konfigürasyonunu ve entegrasyon anahtarlarını silecektir. Devam etmek istiyor musunuz?",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun QuickTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OverdriveCard(
        modifier = modifier,
        contentPadding = 12.dp,
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun IntegrationNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isConnected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isConnected) OverdriveTheme.colors.statusSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                OverdriveStatusPill(
                    label = if (isConnected) "BAĞLI" else "YAPILANDIRILMADI",
                    status = if (isConnected) OverdrivePillStatus.SUCCESS else OverdrivePillStatus.INFO
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun OverlaySwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
