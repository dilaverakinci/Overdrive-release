package com.overdrive.app.ui.abrp

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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

enum class AbrpTab(val title: String, val icon: ImageVector) {
    STATUS("Durum & Ayarlar", Icons.Default.Speed),
    TOKEN("Token", Icons.Default.Key),
    TELEMETRY("Canlı Veri", Icons.Default.ShowChart)
}

data class AbrpUiState(
    val selectedTab: AbrpTab = AbrpTab.STATUS,

    // Token
    val hasToken: Boolean = false,
    val tokenInput: String = "",
    val maskedToken: String = "",

    // Telemetry summary
    val soc: Float = 0f,
    val powerKw: Float = 0f,
    val speedKmh: Float = 0f,
    val isCharging: Boolean = false,
    val isDcfc: Boolean = false,
    val extTempC: Float = 0f,
    val battTempC: Float = 0f,
    val odometerKm: Double = 0.0,
    val soh: Float = 100f,
    val isConnected: Boolean = false,

    // Data saving & upload intervals
    val changeOnly: Boolean = true,
    val minIntervalSeconds: Int = 5,
    val maxIntervalSeconds: Int = 120,

    // App Gate
    val gateOnApp: Boolean = false,
    val appActiveMode: String = "foreground", // "foreground", "running"
    val appGraceSeconds: Int = 90,
    val appPresence: String = "Çalışmıyor",

    // Live Feed Map
    val telemetryMap: Map<String, String> = emptyMap(),

    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val isError: Boolean = false
)

@Composable
fun AbrpScreen(
    state: AbrpUiState,
    onTabSelected: (AbrpTab) -> Unit,
    onTokenInputChange: (String) -> Unit,
    onSaveToken: () -> Unit,
    onDeleteToken: () -> Unit,
    onToggleChangeOnly: (Boolean) -> Unit,
    onMinIntervalChange: (Int) -> Unit,
    onMaxIntervalChange: (Int) -> Unit,
    onToggleGateOnApp: (Boolean) -> Unit,
    onAppActiveModeSelected: (String) -> Unit,
    onAppGraceChange: (Int) -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OverdriveTheme.colors.background)
            .padding(16.dp)
    ) {
        // Top Header
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
                        text = "ABRP Telemetri",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    OverdriveStatusPill(
                        label = when {
                            state.hasToken && state.isConnected -> "CANLI BAĞLI"
                            state.hasToken -> "TOKEN KAYITLI"
                            else -> "YAPILANDIRILMADI"
                        },
                        status = when {
                            state.hasToken && state.isConnected -> OverdrivePillStatus.SUCCESS
                            state.hasToken -> OverdrivePillStatus.WARNING
                            else -> OverdrivePillStatus.INFO
                        }
                    )
                }
                Text(
                    text = "A Better Routeplanner canlı telemetri entegrasyonu (SoC, şarj hızı, tüketim verileri).",
                    fontSize = 12.sp,
                    color = OverdriveTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = OverdriveTheme.colors.accentGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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
        }

        // Sub-tabs row
        AbrpSubTabRow(
            selectedTab = state.selectedTab,
            onTabSelected = onTabSelected
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (state.selectedTab) {
                AbrpTab.STATUS -> StatusTabContent(
                    state = state,
                    onToggleChangeOnly = onToggleChangeOnly,
                    onMinIntervalChange = onMinIntervalChange,
                    onMaxIntervalChange = onMaxIntervalChange,
                    onToggleGateOnApp = onToggleGateOnApp,
                    onAppActiveModeSelected = onAppActiveModeSelected,
                    onAppGraceChange = onAppGraceChange
                )
                AbrpTab.TOKEN -> TokenTabContent(
                    state = state,
                    onTokenInputChange = onTokenInputChange,
                    onSaveToken = onSaveToken,
                    onDeleteToken = onDeleteToken
                )
                AbrpTab.TELEMETRY -> TelemetryTabContent(
                    state = state
                )
            }
        }
    }
}

@Composable
private fun AbrpSubTabRow(
    selectedTab: AbrpTab,
    onTabSelected: (AbrpTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OverdriveTheme.colors.surfaceContainerLow, RoundedCornerShape(8.dp))
            .border(1.dp, OverdriveTheme.colors.cardBorder, RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AbrpTab.values().forEach { tab ->
            val isSelected = tab == selectedTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isSelected) OverdriveTheme.colors.primary.copy(alpha = 0.2f)
                        else Color.Transparent
                    )
                    .border(
                        1.dp,
                        if (isSelected) OverdriveTheme.colors.primary else Color.Transparent,
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { onTabSelected(tab) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        tint = if (isSelected) OverdriveTheme.colors.primary else OverdriveTheme.colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tab.title,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) OverdriveTheme.colors.textPrimary else OverdriveTheme.colors.textSecondary
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 1: STATUS & DATA SAVING
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StatusTabContent(
    state: AbrpUiState,
    onToggleChangeOnly: (Boolean) -> Unit,
    onMinIntervalChange: (Int) -> Unit,
    onMaxIntervalChange: (Int) -> Unit,
    onToggleGateOnApp: (Boolean) -> Unit,
    onAppActiveModeSelected: (String) -> Unit,
    onAppGraceChange: (Int) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Vehicle Telemetry Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Araç Telemetrisi",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    if (state.isCharging) {
                        OverdriveStatusPill(
                            label = if (state.isDcfc) "DC HIZLI ŞARJ" else "AC ŞARJ OLUYOR",
                            status = OverdrivePillStatus.SUCCESS
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "%.1f %%".format(state.soc),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = OverdriveTheme.colors.textPrimary
                        )
                        Text(
                            text = "Batarya Şarj Seviyesi (SoC)",
                            fontSize = 12.sp,
                            color = OverdriveTheme.colors.textSecondary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "%.1f kW".format(state.powerKw),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (state.powerKw < 0) OverdriveTheme.colors.accentGreen else OverdriveTheme.colors.textPrimary
                        )
                        Text(
                            text = "%.0f km/s".format(state.speedKmh),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = OverdriveTheme.colors.textPrimary
                        )
                        Text(
                            text = "Dış: %.1f °C · Bat: %.1f °C".format(state.extTempC, state.battTempC),
                            fontSize = 12.sp,
                            color = OverdriveTheme.colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Battery Progress Bar
                LinearProgressIndicator(
                    progress = { (state.soc / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = when {
                        state.soc <= 15f -> OverdriveTheme.colors.accentRed
                        state.soc <= 30f -> OverdriveTheme.colors.accentAmber
                        else -> OverdriveTheme.colors.accentGreen
                    },
                    trackColor = OverdriveTheme.colors.surfaceContainerHighest
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "%.0f km".format(state.odometerKm),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = OverdriveTheme.colors.textSecondary
                    )
                    Text(
                        text = "SOH: %.1f %%".format(state.soh),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = OverdriveTheme.colors.textSecondary
                    )
                }
            }
        }

        // Data Saving & Intervals Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Veri Tasarrufu ve Gönderim Sıklığı",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OverdriveTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                SettingToggleRow(
                    title = "Yalnızca Değişiklik Olduğunda Gönder (Change Only)",
                    subtitle = "Telemetri değerleri aynı kaldığında gereksiz veri harcamaz; maksimum aralıkta kalp atışı iletir.",
                    checked = state.changeOnly,
                    onCheckedChange = onToggleChangeOnly
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Min interval slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Minimum Gönderim Aralığı (En Hızlı)",
                        fontSize = 14.sp,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Text(
                        text = "${state.minIntervalSeconds} sn",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = OverdriveTheme.colors.accentGreen
                    )
                }
                Slider(
                    value = state.minIntervalSeconds.toFloat(),
                    onValueChange = { onMinIntervalChange(it.toInt()) },
                    valueRange = 1f..30f,
                    colors = SliderDefaults.colors(
                        thumbColor = OverdriveTheme.colors.accentGreen,
                        activeTrackColor = OverdriveTheme.colors.accentGreen
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Max interval slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Maksimum Kalp Atışı Aralığı (Heartbeat)",
                        fontSize = 14.sp,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Text(
                        text = if (state.maxIntervalSeconds >= 60) "${state.maxIntervalSeconds / 60} dk" else "${state.maxIntervalSeconds} sn",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = OverdriveTheme.colors.accentGreen
                    )
                }
                Slider(
                    value = state.maxIntervalSeconds.toFloat(),
                    onValueChange = { onMaxIntervalChange(it.toInt()) },
                    valueRange = 30f..1800f,
                    colors = SliderDefaults.colors(
                        thumbColor = OverdriveTheme.colors.accentGreen,
                        activeTrackColor = OverdriveTheme.colors.accentGreen
                    )
                )
            }
        }

        // App Gate Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ABRP Uygulama Koruması (App Gate)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OverdriveTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                SettingToggleRow(
                    title = "Yalnızca ABRP Açıkken Gönder",
                    subtitle = "ABRP uygulaması baş ünitede açık olmadığı sürece hiçbir telemetri verisi yüklenmez.",
                    checked = state.gateOnApp,
                    onCheckedChange = onToggleGateOnApp
                )

                if (state.gateOnApp) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Etkin Olma Koşulu",
                        fontSize = 14.sp,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SegmentOptionButton(
                            title = "Ön Planda (Ekranda)",
                            subtitle = "Yalnızca ekrandayken (önerilen)",
                            isSelected = state.appActiveMode == "foreground",
                            modifier = Modifier.weight(1f),
                            onClick = { onAppActiveModeSelected("foreground") }
                        )
                        SegmentOptionButton(
                            title = "Çalışırken (Arka Plan Dahil)",
                            subtitle = "Arka planda da olsa gönderir",
                            isSelected = state.appActiveMode == "running",
                            modifier = Modifier.weight(1f),
                            onClick = { onAppActiveModeSelected("running") }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Kapanma Tolerans Süresi (Grace Period)",
                            fontSize = 14.sp,
                            color = OverdriveTheme.colors.textPrimary
                        )
                        Text(
                            text = "${state.appGraceSeconds} sn",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = OverdriveTheme.colors.accentGreen
                        )
                    }
                    Slider(
                        value = state.appGraceSeconds.toFloat(),
                        onValueChange = { onAppGraceChange(it.toInt()) },
                        valueRange = 0f..600f,
                        colors = SliderDefaults.colors(
                            thumbColor = OverdriveTheme.colors.accentGreen,
                            activeTrackColor = OverdriveTheme.colors.accentGreen
                        )
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 2: TOKEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TokenTabContent(
    state: AbrpUiState,
    onTokenInputChange: (String) -> Unit,
    onSaveToken: () -> Unit,
    onDeleteToken: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ABRP Kullanıcı Tokeni (User Token)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OverdriveTheme.colors.textPrimary
                )
                Text(
                    text = "A Better Routeplanner uygulamasından Ayarlar → Canlı Veri → Standart Bağlantı altındaki 'Generic / OBD' veya 'Token' kodunuzu buraya girin.",
                    fontSize = 13.sp,
                    color = OverdriveTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                )

                if (state.hasToken) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(OverdriveTheme.colors.surfaceContainerLow, RoundedCornerShape(8.dp))
                            .border(1.dp, OverdriveTheme.colors.accentGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Mevcut Kayıtlı Token",
                                    fontSize = 12.sp,
                                    color = OverdriveTheme.colors.textSecondary
                                )
                                Text(
                                    text = state.maskedToken.ifBlank { "••••••••" },
                                    fontSize = 18.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = OverdriveTheme.colors.accentGreen
                                )
                            }
                            Button(
                                onClick = onDeleteToken,
                                colors = ButtonDefaults.buttonColors(containerColor = OverdriveTheme.colors.accentRed.copy(alpha = 0.2f)),
                                border = BorderStroke(1.dp, OverdriveTheme.colors.accentRed),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = OverdriveTheme.colors.accentRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Sil", color = OverdriveTheme.colors.accentRed)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                OutlinedTextField(
                    value = state.tokenInput,
                    onValueChange = onTokenInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (state.hasToken) "Yeni Token Girin (Değiştirmek İçin)" else "ABRP Token (UUID)") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Gizle" else "Göster",
                                tint = OverdriveTheme.colors.textSecondary
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OverdriveTheme.colors.primary,
                        unfocusedBorderColor = OverdriveTheme.colors.cardBorder,
                        focusedLabelColor = OverdriveTheme.colors.primary,
                        unfocusedLabelColor = OverdriveTheme.colors.textSecondary,
                        focusedTextColor = OverdriveTheme.colors.textPrimary,
                        unfocusedTextColor = OverdriveTheme.colors.textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onSaveToken,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OverdriveTheme.colors.primary),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = OverdriveTheme.colors.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tokeni Kaydet ve Test Et",
                        fontWeight = FontWeight.SemiBold,
                        color = OverdriveTheme.colors.onPrimary
                    )
                }

                if (state.statusMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = state.statusMessage,
                        fontSize = 13.sp,
                        color = if (state.isError) OverdriveTheme.colors.accentRed else OverdriveTheme.colors.accentGreen
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 3: TELEMETRY (LIVE FEED)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TelemetryTabContent(
    state: AbrpUiState
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Canlı Telemetri Parametreleri",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OverdriveTheme.colors.textPrimary
                )
                Text(
                    text = "ABRP API sunucusuna iletilen anlık ham araç CAN verileri.",
                    fontSize = 13.sp,
                    color = OverdriveTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                if (state.telemetryMap.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Canlı telemetri akışı bekleniyor...",
                            fontSize = 13.sp,
                            color = OverdriveTheme.colors.textSecondary
                        )
                    }
                } else {
                    state.telemetryMap.forEach { (key, value) ->
                        InfoRow(label = key, value = value)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE HELPER WIDGETS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = OverdriveTheme.colors.textSecondary
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = OverdriveTheme.colors.textPrimary
        )
    }
}

@Composable
private fun SegmentOptionButton(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected) OverdriveTheme.colors.primary.copy(alpha = 0.2f)
                else OverdriveTheme.colors.surfaceContainerLow
            )
            .border(
                1.dp,
                if (isSelected) OverdriveTheme.colors.primary else OverdriveTheme.colors.cardBorder,
                RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) OverdriveTheme.colors.textPrimary else OverdriveTheme.colors.textSecondary
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Seçili",
                        tint = OverdriveTheme.colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = OverdriveTheme.colors.textSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
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
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OverdriveTheme.colors.onPrimary,
                checkedTrackColor = OverdriveTheme.colors.primary,
                uncheckedThumbColor = OverdriveTheme.colors.outline,
                uncheckedTrackColor = OverdriveTheme.colors.surfaceContainerHighest
            )
        )
    }
}
