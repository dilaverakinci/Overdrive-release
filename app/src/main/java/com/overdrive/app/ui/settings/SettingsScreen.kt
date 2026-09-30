package com.overdrive.app.ui.settings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.overdrive.app.ui.component.OverdrivePillStatus
import com.overdrive.app.ui.component.OverdriveStatusPill
import com.overdrive.app.ui.theme.OverdriveColors
import com.overdrive.app.ui.theme.OverdriveTheme

private val OverdriveColors.textPrimary: Color get() = onSurface
private val OverdriveColors.textSecondary: Color get() = onSurfaceVariant
private val OverdriveColors.cardBackground: Color get() = surfaceContainer
private val OverdriveColors.cardBorder: Color get() = outlineVariant
private val OverdriveColors.accentGreen: Color get() = statusSuccess
private val OverdriveColors.accentAmber: Color get() = statusWarning
private val OverdriveColors.accentRed: Color get() = statusDanger

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
    onDismissResetDialog: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Ayarlar",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = OverdriveTheme.colors.textPrimary
                )
                Text(
                    text = "Arayüz tercihleri, güvenlik, katmanlar ve entegrasyon ayarları.",
                    fontSize = 12.sp,
                    color = OverdriveTheme.colors.textSecondary,
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

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Preferences Navigation Cards
        Text(
            text = "TERCİHLER VE ÖZELLİKLER",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = OverdriveTheme.colors.accentGreen,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
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

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Remote Integrations Quick Hub
        Text(
            text = "UZAKTAN İLETİŞİM ENTEGRASYONLARI",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = OverdriveTheme.colors.accentGreen,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                IntegrationNavRow(
                    icon = Icons.Default.Send,
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

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Overlays Card
        Text(
            text = "EKRAN ÜSTÜ KATMANLAR (OVERLAY)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = OverdriveTheme.colors.accentGreen,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = OverdriveTheme.colors.accentGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bilgi Katmanı Görünürlüğü",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = OverdriveTheme.colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

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

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Destructive Action (Reset Data)
        Text(
            text = "VERİ VE SIFIRLAMA",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = OverdriveTheme.colors.accentRed,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, Color(0xFF3F1D1D))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenResetDialog() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = OverdriveTheme.colors.accentRed,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Uygulama Verilerini Sıfırla",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = OverdriveTheme.colors.accentRed
                    )
                    Text(
                        text = "Kayıtlı oturumları, yapılandırmaları ve önbelleği fabrika ayarlarına döndürür.",
                        fontSize = 12.sp,
                        color = OverdriveTheme.colors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = OverdriveTheme.colors.accentRed,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 6. Footer
        Text(
            text = "OverDrive ${state.installedVersion.ifEmpty { "v51.8" }} · ${state.appId.ifEmpty { "com.overdrive.app" }}",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = OverdriveTheme.colors.textSecondary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    // Reset confirmation dialog
    if (state.showResetDialog) {
        AlertDialog(
            onDismissRequest = onDismissResetDialog,
            title = {
                Text(
                    text = "Tüm Veriler Sıfırlansın mı?",
                    fontWeight = FontWeight.Bold,
                    color = OverdriveTheme.colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Bu işlem tüm yerel ayarları, kamera konfigürasyonunu ve entegrasyon anahtarlarını silecektir. Devam etmek istiyor musunuz?",
                    color = OverdriveTheme.colors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmReset,
                    colors = ButtonDefaults.buttonColors(containerColor = OverdriveTheme.colors.accentRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Tümünü Sıfırla", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissResetDialog) {
                    Text("İptal", color = OverdriveTheme.colors.textSecondary)
                }
            },
            containerColor = Color(0xFF16181D)
        )
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
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
        border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = OverdriveTheme.colors.accentGreen,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = OverdriveTheme.colors.textSecondary
            )
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = OverdriveTheme.colors.textPrimary,
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OverdriveTheme.colors.accentGreen,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = OverdriveTheme.colors.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = OverdriveTheme.colors.textSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = OverdriveTheme.colors.textSecondary,
            modifier = Modifier.size(16.dp)
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isConnected) OverdriveTheme.colors.accentGreen else OverdriveTheme.colors.textSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OverdriveTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                OverdriveStatusPill(
                    label = if (isConnected) "BAĞLI" else "YAPILANDIRILMADI",
                    status = if (isConnected) OverdrivePillStatus.SUCCESS else OverdrivePillStatus.INFO
                )
            }
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = OverdriveTheme.colors.textSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = OverdriveTheme.colors.textSecondary,
            modifier = Modifier.size(16.dp)
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
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = OverdriveTheme.colors.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = OverdriveTheme.colors.textSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = OverdriveTheme.colors.accentGreen,
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(OverdriveTheme.colors.cardBorder.copy(alpha = 0.5f))
    )
}
