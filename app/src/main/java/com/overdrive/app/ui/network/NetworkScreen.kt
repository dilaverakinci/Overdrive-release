package com.overdrive.app.ui.network

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    onRefresh: () -> Unit
) {
    var limitInput by remember(state.dataCapMb) {
        mutableStateOf(if (state.dataCapMb > 0L) state.dataCapMb.toString() else "")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Ağ ve Bağlantı Noktası",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
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
                    fontSize = 12.sp,
                    color = OverdriveTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Yenile",
                    tint = OverdriveTheme.colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 1. Hotspot Master Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                            tint = if (state.isEnabled) OverdriveTheme.colors.accentGreen else OverdriveTheme.colors.textSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Mobil Bağlantı Noktası (Hotspot)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = OverdriveTheme.colors.textPrimary
                            )
                            Text(
                                text = state.stateText,
                                fontSize = 12.sp,
                                color = if (state.isEnabled) OverdriveTheme.colors.accentGreen else OverdriveTheme.colors.textSecondary
                            )
                        }
                    }

                    Switch(
                        checked = state.isEnabled || state.isTransitioning,
                        onCheckedChange = { onToggleHotspot(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = OverdriveTheme.colors.accentGreen,
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = Color.DarkGray
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(OverdriveTheme.colors.cardBorder.copy(alpha = 0.5f))
                )
                Spacer(modifier = Modifier.height(14.dp))

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
                            fontSize = 12.sp,
                            color = OverdriveTheme.colors.textSecondary
                        )
                        Text(
                            text = state.ssid.ifEmpty { "—" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = OverdriveTheme.colors.textPrimary
                        )
                    }
                    IconButton(onClick = onCopySsid) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Kopyala",
                            tint = OverdriveTheme.colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                            fontSize = 12.sp,
                            color = OverdriveTheme.colors.textSecondary
                        )
                        val displayPw = when {
                            state.password.isEmpty() -> "—"
                            state.isPasswordRevealed -> state.password
                            else -> "•".repeat(maxOf(0, state.password.length - 2)) + state.password.takeLast(2)
                        }
                        Text(
                            text = displayPw,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = OverdriveTheme.colors.textPrimary
                        )
                    }
                    Row {
                        IconButton(onClick = onTogglePasswordRevealed) {
                            Icon(
                                imageVector = if (state.isPasswordRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Göster/Gizle",
                                tint = OverdriveTheme.colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = onCopyPassword) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Kopyala",
                                tint = OverdriveTheme.colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Session Stats
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Oturum İstatistikleri",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OverdriveTheme.colors.accentGreen
                )
                Spacer(modifier = Modifier.height(12.dp))

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

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Data Limit & Usage Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Veri Limiti ve Tüketim",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OverdriveTheme.colors.accentGreen
                )
                Text(
                    text = "Belirlenen MB sınırına ulaşıldığında bağlantı noktası otomatik durdurulur (0 limitsizdir).",
                    fontSize = 12.sp,
                    color = OverdriveTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Text(
                    text = "Toplam Kullanım: ${state.usageText}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OverdriveTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

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
                            focusedBorderColor = OverdriveTheme.colors.accentGreen,
                            unfocusedBorderColor = OverdriveTheme.colors.cardBorder,
                            focusedTextColor = OverdriveTheme.colors.textPrimary,
                            unfocusedTextColor = OverdriveTheme.colors.textPrimary
                        )
                    )

                    Button(
                        onClick = {
                            val cap = limitInput.toLongOrNull() ?: 0L
                            onSaveLimit(cap)
                        },
                        modifier = Modifier.height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OverdriveTheme.colors.accentGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Kaydet", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = onResetUsage,
                        modifier = Modifier.height(54.dp)
                    ) {
                        Text("Sıfırla", color = OverdriveTheme.colors.accentRed)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Connected Clients
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bağlı Cihazlar (${state.clients.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = OverdriveTheme.colors.accentGreen
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (state.clients.isEmpty()) {
                    Text(
                        text = "Şu anda bağlı cihaz bulunmuyor.",
                        fontSize = 13.sp,
                        color = OverdriveTheme.colors.textSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    state.clients.forEachIndexed { index, client ->
                        if (index > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(OverdriveTheme.colors.cardBorder.copy(alpha = 0.5f))
                                    .padding(vertical = 4.dp)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = null,
                                tint = OverdriveTheme.colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = client.name.ifEmpty { "Cihaz ${index + 1}" },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = OverdriveTheme.colors.textPrimary
                                )
                                Text(
                                    text = "${client.ip} • ${client.mac}",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = OverdriveTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Behavior Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Çalışma Davranışları",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OverdriveTheme.colors.accentGreen
                )
                Spacer(modifier = Modifier.height(12.dp))

                NetworkSwitchRow(
                    title = "Sürekli Açık Tut (Keep-Alive)",
                    subtitle = "Hotspot beklenmedik şekilde kapandığında arka planda otomatik yeniden başlatılır.",
                    checked = state.keepAlive,
                    onCheckedChange = { onToggleSwitch("keepAlive", it) }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(OverdriveTheme.colors.cardBorder.copy(alpha = 0.5f))
                        .padding(vertical = 6.dp)
                )

                NetworkSwitchRow(
                    title = "Açılışta Otomatik Başlat",
                    subtitle = "Araç ve sistem açıldığında mobil bağlantı noktasını otomatik devreye sokar.",
                    checked = state.autoStartBoot,
                    onCheckedChange = { onToggleSwitch("autoStartBoot", it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 6. Proxy & Relay Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Ağ Yönlendirme ve Proxy",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OverdriveTheme.colors.accentGreen
                )
                Spacer(modifier = Modifier.height(12.dp))

                NetworkSwitchRow(
                    title = "Sistem Genelinde Proxy",
                    subtitle = "Araç sistemi üzerindeki tüm HTTP isteklerini yerel tünel proxy'sine yönlendirir.",
                    checked = state.proxySystemWide,
                    onCheckedChange = { onToggleSwitch("proxySystemWide", it) }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(OverdriveTheme.colors.cardBorder.copy(alpha = 0.5f))
                        .padding(vertical = 6.dp)
                )

                NetworkSwitchRow(
                    title = "İstemcilere İnternet Rölesi",
                    subtitle = state.proxyClientsDesc.ifEmpty { "Hotspot'a bağlı cihazların SIM internetine çıkmasını sağlar." },
                    checked = state.proxyForClients,
                    onCheckedChange = { onToggleSwitch("proxyForClients", it) }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(OverdriveTheme.colors.cardBorder.copy(alpha = 0.5f))
                        .padding(vertical = 6.dp)
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

    // Warning dialog on first enable
    if (state.showWarningDialog) {
        AlertDialog(
            onDismissRequest = onDismissWarningDialog,
            title = {
                Text(
                    text = "Mobil Bağlantı Noktası Etkinleştirilsin mi?",
                    fontWeight = FontWeight.Bold,
                    color = OverdriveTheme.colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Aracın Wi-Fi alıcısı ve Hotspot vericisi aynı donanımı paylaşır. Hotspot açıldığında araç mevcut bir Wi-Fi ağına bağlı kalamaz ve kendi SIM kartındaki mobil veriyi kullanmaya başlar.",
                    color = OverdriveTheme.colors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmEnableHotspot,
                    colors = ButtonDefaults.buttonColors(containerColor = OverdriveTheme.colors.accentGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Devam Et", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissWarningDialog) {
                    Text("İptal", color = OverdriveTheme.colors.textSecondary)
                }
            },
            containerColor = Color(0xFF16181D)
        )
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
        Text(text = label, fontSize = 11.sp, color = OverdriveTheme.colors.textSecondary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OverdriveTheme.colors.textPrimary)
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
