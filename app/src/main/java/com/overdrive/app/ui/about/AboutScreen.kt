package com.overdrive.app.ui.about

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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

data class VehicleDisplayInfo(
    val vin: String? = null,
    val firmware: String? = null,
    val dsp: String? = null,
    val mcu: String? = null,
    val android: String? = null,
    val securityPatch: String? = null,
    val headUnit: String? = null
)

data class AboutUiState(
    val installedVersion: String = "",
    val buildId: String = "",
    val updateChannel: String = "alpha",
    val isVinRevealed: Boolean = false,
    val vehicleInfo: VehicleDisplayInfo? = null,
    val isCheckingUpdate: Boolean = false
)

@Composable
fun AboutScreen(
    state: AboutUiState,
    onCheckForUpdates: () -> Unit,
    onSelectChannel: (String) -> Unit,
    onToggleVinVisibility: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onOpenLicense: () -> Unit,
    onOpenSource: () -> Unit,
    onOpenStar: () -> Unit,
    onShare: () -> Unit,
    onSupport: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Hakkında",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    OverdriveStatusPill(
                        label = state.installedVersion.ifEmpty { "v51.8" },
                        status = OverdrivePillStatus.SUCCESS
                    )
                }
                Text(
                    text = "OverDrive sürüm bilgisi, araç kimliği ve yedekleme yönetimi.",
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

        Spacer(modifier = Modifier.height(4.dp))

        // 1. App Identity Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "OverDrive",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = OverdriveTheme.colors.accentGreen
                        )
                        Text(
                            text = "BYD Akıllı Araç Platformu & Gelişmiş Asistan",
                            fontSize = 12.sp,
                            color = OverdriveTheme.colors.textSecondary
                        )
                    }

                    Button(
                        onClick = onCheckForUpdates,
                        colors = ButtonDefaults.buttonColors(containerColor = OverdriveTheme.colors.accentGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.SystemUpdate, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Güncellemeleri Denetle", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Yüklü Sürüm", fontSize = 11.sp, color = OverdriveTheme.colors.textSecondary)
                        Text(
                            text = state.installedVersion.ifEmpty { "alpha-v51.8" },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = OverdriveTheme.colors.textPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Paket Kimliği", fontSize = 11.sp, color = OverdriveTheme.colors.textSecondary)
                        Text(
                            text = state.buildId.ifEmpty { "com.overdrive.app" },
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = OverdriveTheme.colors.textSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Update Channel Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Güncelleme Kanalı",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OverdriveTheme.colors.accentGreen
                )
                Text(
                    text = if (state.updateChannel.equals("braveheart", ignoreCase = true))
                        "Braveheart (Beta): En yeni deneysel özellikleri içerir, bazen kararsız olabilir."
                    else
                        "Alpha: Test edilmiş, güvenli ve kararlı genel sürüm güncellemeleri.",
                    fontSize = 12.sp,
                    color = OverdriveTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F1115), RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isAlpha = !state.updateChannel.equals("braveheart", ignoreCase = true)

                    ChannelOptionBox(
                        title = "Alpha (Kararlı)",
                        isSelected = isAlpha,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectChannel("alpha") }
                    )

                    ChannelOptionBox(
                        title = "Braveheart (Beta)",
                        isSelected = !isAlpha,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectChannel("braveheart") }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Vehicle Identity Card
        val info = state.vehicleInfo
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = null, tint = OverdriveTheme.colors.accentGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Araç ve Donanım Kimliği",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = OverdriveTheme.colors.textPrimary
                        )
                    }

                    if (info?.vin != null) {
                        TextButton(onClick = onToggleVinVisibility) {
                            Icon(
                                imageVector = if (state.isVinRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = OverdriveTheme.colors.accentGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (state.isVinRevealed) "Gizle" else "Şasiyi Göster",
                                color = OverdriveTheme.colors.accentGreen,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val vinDisplay = when {
                    info?.vin == null -> "—"
                    state.isVinRevealed -> info.vin
                    else -> if (info.vin.length > 6) info.vin.take(3) + "••••••••" + info.vin.takeLast(4) else "••••••••"
                }

                VehicleInfoRow(label = "VIN (Şasi No):", value = vinDisplay, isMono = true)
                VehicleInfoRow(label = "Yazılım / Firmware:", value = info?.firmware ?: "—")
                VehicleInfoRow(label = "Multimedya Ünitesi (HeadUnit):", value = info?.headUnit ?: "—")
                VehicleInfoRow(label = "Android Sürümü:", value = info?.android ?: "—")
                VehicleInfoRow(label = "Güvenlik Yaması:", value = info?.securityPatch ?: "—")
                VehicleInfoRow(label = "MCU Sürümü:", value = info?.mcu ?: "—")
                VehicleInfoRow(label = "DSP Sürümü:", value = info?.dsp ?: "—")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Backup & Restore Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Yapılandırma Yedekleme ve Geri Yükleme",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OverdriveTheme.colors.accentGreen
                )
                Text(
                    text = "Tüm ayarlarınızı, otomasyonları ve entegrasyon yapılandırmanızı tek bir dosyada güvenle saklayın.",
                    fontSize = 12.sp,
                    color = OverdriveTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onExportBackup,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262C36)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Yedek Al (Dışa Aktar)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = onImportBackup,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262C36)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Upload, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Geri Yükle (İçe Aktar)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 5. Open Source Links & Support Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Bağlantılar ve Katkıda Bulunma",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OverdriveTheme.colors.accentGreen
                )
                Spacer(modifier = Modifier.height(10.dp))

                AboutLinkRow(icon = Icons.Default.Code, title = "Kaynak Kodu (GitHub)", onClick = onOpenSource)
                AboutLinkRow(icon = Icons.Default.Info, title = "Açık Kaynak Lisansı (MIT)", onClick = onOpenLicense)
                AboutLinkRow(icon = Icons.Default.Star, title = "GitHub'da Yıldız Ver", onClick = onOpenStar)
                AboutLinkRow(icon = Icons.Default.Share, title = "OverDrive'ı Paylaş", onClick = onShare)
                AboutLinkRow(icon = Icons.Default.Favorite, title = "Geliştiriciye Destek Ol (Ko-fi)", onClick = onSupport)
            }
        }
    }
}

@Composable
private fun ChannelOptionBox(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (isSelected) Color(0xFF1F242D) else Color.Transparent
    val border = if (isSelected) BorderStroke(1.dp, OverdriveTheme.colors.accentGreen.copy(alpha = 0.6f)) else null
    val textColor = if (isSelected) OverdriveTheme.colors.accentGreen else OverdriveTheme.colors.textSecondary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .then(if (border != null) Modifier.border(border, RoundedCornerShape(6.dp)) else Modifier)
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}

@Composable
private fun VehicleInfoRow(
    label: String,
    value: String,
    isMono: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = OverdriveTheme.colors.textSecondary)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = if (isMono) FontFamily.Monospace else FontFamily.Default,
            color = OverdriveTheme.colors.textPrimary
        )
    }
}

@Composable
private fun AboutLinkRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = OverdriveTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = title, fontSize = 13.sp, color = OverdriveTheme.colors.textPrimary)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = OverdriveTheme.colors.textSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}
