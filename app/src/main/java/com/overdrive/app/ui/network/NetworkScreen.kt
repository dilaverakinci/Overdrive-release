package com.overdrive.app.ui.network

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.overdrive.app.ui.component.OverdriveButton
import com.overdrive.app.ui.component.OverdriveButtonVariant
import com.overdrive.app.ui.component.OverdriveCard
import com.overdrive.app.ui.component.OverdriveDialog
import com.overdrive.app.ui.component.OverdrivePillStatus
import com.overdrive.app.ui.component.OverdriveStatusPill
import com.overdrive.app.ui.theme.OverdriveTheme

data class ConnectedClient(
    val name: String,
    val ip: String,
    val mac: String
)

data class NetworkUiState(
    val isEnabled: Boolean = false,
    val isTransitioning: Boolean = false,
    val stateText: String = "Kapalı",
    val ssid: String = "",
    val password: String = "",
    val isPasswordRevealed: Boolean = false,
    val uptimeText: String = "--",
    val rxText: String = "0 B",
    val txText: String = "0 B",
    val clients: List<ConnectedClient> = emptyList(),
    val usageText: String = "0 B",
    val dataCapMb: Long = 0L,
    val keepAlive: Boolean = false,
    val autoStartBoot: Boolean = false,
    val proxySystemWide: Boolean = false,
    val proxyForClients: Boolean = false,
    val clientTunnel: Boolean = false,
    val proxyClientsDesc: String = "",
    val clientTunnelDesc: String = "",
    val showWarningDialog: Boolean = false
)

@Composable
fun NetworkScreen(
    state: NetworkUiState,
    onToggleHotspot: (Boolean) -> Unit,
    onConfirmEnableHotspot: () -> Unit,
    onDismissWarningDialog: () -> Unit,
    onTogglePasswordRevealed: () -> Unit,
    onCopySsid: () -> Unit,
    onCopyPassword: () -> Unit,
    onSaveLimit: (Long) -> Unit,
    onResetUsage: () -> Unit,
    onToggleSwitch: (key: String, value: Boolean) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var limitInput by remember(state.dataCapMb) {
        mutableStateOf(if (state.dataCapMb > 0L) state.dataCapMb.toString() else "")
    }

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
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Ağ ve Bağlantı Noktası",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        OverdriveStatusPill(
                            label = when {
                                state.isTransitioning -> "GEÇİŞ YAPILIYOR"
                                state.isEnabled -> "AÇIK"
                                else -> "KAPALI"
                            },
                            status = when {
                                state.isTransitioning -> OverdrivePillStatus.WARNING
                                state.isEnabled -> OverdrivePillStatus.SUCCESS
                                else -> OverdrivePillStatus.INFO
                            }
                        )
                    }
                    Text(
                        text = "Aracın SIM kart internetini Wi-Fi üzerinden yolcular ve harici cihazlarla paylaşın.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Yenile",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 1. Hotspot Master Card
            OverdriveCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 12.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiTethering,
                                contentDescription = null,
                                tint = if (state.isEnabled) OverdriveTheme.colors.statusSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Mobil Bağlantı Noktası (Hotspot)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = state.stateText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (state.isEnabled) OverdriveTheme.colors.statusSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = state.isEnabled || state.isTransitioning,
                            onCheckedChange = { onToggleHotspot(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    // SSID row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onCopySsid() }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Ağ Adı (SSID)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = state.ssid.ifEmpty { "—" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = onCopySsid) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Kopyala",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Password row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onCopyPassword() }
                        ) {
                            Text(
                                text = "Ağ Şifresi",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val displayPw = when {
                                state.password.isEmpty() -> "—"
                                state.isPasswordRevealed -> state.password
                                else -> "•".repeat(maxOf(0, state.password.length - 2)) + state.password.takeLast(2)
                            }
                            Text(
                                text = displayPw,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row {
                            IconButton(onClick = onTogglePasswordRevealed) {
                                Icon(
                                    imageVector = if (state.isPasswordRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Göster/Gizle",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(onClick = onCopyPassword) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Kopyala",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Session Stats
            OverdriveCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 12.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Oturum İstatistikleri",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatBox(label = "Çalışma Süresi", value = state.uptimeText, modifier = Modifier.weight(1f))
                        StatBox(label = "İndirme (RX)", value = state.rxText, modifier = Modifier.weight(1f))
                        StatBox(label = "Yükleme (TX)", value = state.txText, modifier = Modifier.weight(1f))
                        StatBox(label = "Bağlı Cihaz", value = "${state.clients.size}", modifier = Modifier.weight(1f))
                    }
                }
            }

            // 3. Data Limit & Usage Card
            OverdriveCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 12.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Veri Limiti ve Tüketim",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Belirlenen MB sınırına ulaşıldığında bağlantı noktası otomatik durdurulur (0 limitsizdir).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )

                    Text(
                        text = "Toplam Kullanım: ${state.usageText}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = limitInput,
                            onValueChange = { limitInput = it },
                            label = { Text("Limit (MB)") },
                            placeholder = { Text("Limitsiz için 0") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                cursorColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        OverdriveButton(
                            text = "Kaydet",
                            variant = OverdriveButtonVariant.PRIMARY,
                            onClick = {
                                val cap = limitInput.toLongOrNull() ?: 0L
                                onSaveLimit(cap)
                            }
                        )

                        OverdriveButton(
                            text = "Sıfırla",
                            variant = OverdriveButtonVariant.DANGER,
                            onClick = onResetUsage
                        )
                    }
                }
            }

            // 4. Connected Clients
            OverdriveCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 12.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Bağlı Cihazlar (${state.clients.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (state.clients.isEmpty()) {
                        Text(
                            text = "Şu anda bağlı cihaz bulunmuyor.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        state.clients.forEachIndexed { index, client ->
                            if (index > 0) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Devices,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = client.name.ifEmpty { "Cihaz ${index + 1}" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${client.ip} • ${client.mac}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Behavior Settings
            OverdriveCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 12.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Çalışma Davranışları",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    NetworkSwitchRow(
                        title = "Sürekli Açık Tut (Keep-Alive)",
                        subtitle = "Hotspot beklenmedik şekilde kapandığında arka planda otomatik yeniden başlatılır.",
                        checked = state.keepAlive,
                        onCheckedChange = { onToggleSwitch("keepAlive", it) }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    NetworkSwitchRow(
                        title = "Açılışta Otomatik Başlat",
                        subtitle = "Araç ve sistem açıldığında mobil bağlantı noktasını otomatik devreye sokar.",
                        checked = state.autoStartBoot,
                        onCheckedChange = { onToggleSwitch("autoStartBoot", it) }
                    )
                }
            }

            // 6. Proxy & Relay Settings
            OverdriveCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 12.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Ağ Yönlendirme ve Proxy",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    NetworkSwitchRow(
                        title = "Sistem Genelinde Proxy",
                        subtitle = "Araç sistemi üzerindeki tüm HTTP isteklerini yerel tünel proxy'sine yönlendirir.",
                        checked = state.proxySystemWide,
                        onCheckedChange = { onToggleSwitch("proxySystemWide", it) }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    NetworkSwitchRow(
                        title = "İstemcilere İnternet Rölesi",
                        subtitle = state.proxyClientsDesc.ifEmpty { "Hotspot'a bağlı cihazların SIM internetine çıkmasını sağlar." },
                        checked = state.proxyForClients,
                        onCheckedChange = { onToggleSwitch("proxyForClients", it) }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    NetworkSwitchRow(
                        title = "İstemci Tüneli",
                        subtitle = state.clientTunnelDesc.ifEmpty { "İstemci cihazlar için özel port röle tünelini etkinleştirir." },
                        checked = state.clientTunnel,
                        onCheckedChange = { onToggleSwitch("clientTunnel", it) }
                    )
                }
            }
        }
    }

    // Warning dialog on first enable
    if (state.showWarningDialog) {
        OverdriveDialog(
            onDismissRequest = onDismissWarningDialog,
            title = "Mobil Bağlantı Noktası Etkinleştirilsin mi?",
            positiveButtonText = "Devam Et",
            onPositiveClick = onConfirmEnableHotspot,
            negativeButtonText = "İptal",
            onNegativeClick = onDismissWarningDialog
        ) {
            Text(
                text = "Aracın Wi-Fi alıcısı ve Hotspot vericisi aynı donanımı paylaşır. Hotspot açıldığında araç mevcut bir Wi-Fi ağına bağlı kalamaz ve kendi SIM kartındaki mobil veriyi kullanmaya başlar.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun NetworkSwitchRow(
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
