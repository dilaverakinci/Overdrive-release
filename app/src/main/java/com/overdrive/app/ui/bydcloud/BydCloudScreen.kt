package com.overdrive.app.ui.bydcloud

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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

enum class BydCloudTab(val title: String, val icon: ImageVector) {
    ACCOUNT("Hesap", Icons.Default.AccountCircle),
    STATUS("Durum", Icons.Default.CloudDone),
    ADVANCED("Gelişmiş", Icons.Default.Settings)
}

data class CountryOption(
    val name: String,
    val code: String,
    val region: String
)

val BYD_COUNTRIES = listOf(
    CountryOption("Türkiye", "TR", "tr"),
    CountryOption("United Kingdom", "GB", "eu"),
    CountryOption("Germany", "DE", "eu"),
    CountryOption("Netherlands", "NL", "eu"),
    CountryOption("France", "FR", "eu"),
    CountryOption("Spain", "ES", "eu"),
    CountryOption("Italy", "IT", "eu"),
    CountryOption("Norway", "NO", "eu"),
    CountryOption("Sweden", "SE", "eu"),
    CountryOption("Denmark", "DK", "eu"),
    CountryOption("Belgium", "BE", "eu"),
    CountryOption("Austria", "AT", "eu"),
    CountryOption("Switzerland", "CH", "eu"),
    CountryOption("Australia", "AU", "au"),
    CountryOption("New Zealand", "NZ", "au"),
    CountryOption("Singapore", "SG", "sg"),
    CountryOption("Malaysia", "MY", "sg"),
    CountryOption("Thailand", "TH", "sg"),
    CountryOption("Brazil", "BR", "br"),
    CountryOption("Mexico", "MX", "mx"),
    CountryOption("Japan", "JP", "jp"),
    CountryOption("South Korea", "KR", "kr"),
    CountryOption("Israel", "IL", "eu"),
    CountryOption("United Arab Emirates", "AE", "no"),
    CountryOption("Saudi Arabia", "SA", "sa"),
    CountryOption("China (Mainland)", "CN", "cn")
)

data class BydCloudUiState(
    val selectedTab: BydCloudTab = BydCloudTab.ACCOUNT,
    val isConfigured: Boolean = false,
    val isVerified: Boolean = false,
    val vin: String = "",
    val username: String = "",
    val countryCode: String = "TR",
    val region: String = "tr",
    val cloudDataMerge: Boolean = false,
    val isPushConnected: Boolean = false,
    val lastPushAgeSeconds: Long = -1L,
    val pushLockState: String = "",
    val pushSocPercent: Int? = null,
    val pushChargingState: String = "",
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val isStatusError: Boolean = false,
    val showClearDialog: Boolean = false
)

@Composable
fun BydCloudScreen(
    state: BydCloudUiState,
    onTabSelected: (BydCloudTab) -> Unit,
    onSaveCredentials: (username: String, password: String, pin: String, countryCode: String, region: String) -> Unit,
    onTestConnection: () -> Unit,
    onToggleCloudDataMerge: (Boolean) -> Unit,
    onConfirmClearCredentials: () -> Unit,
    onDismissClearDialog: () -> Unit,
    onOpenClearDialog: () -> Unit,
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
                        text = "BYD Cloud",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    OverdriveStatusPill(
                        label = when {
                            state.isVerified -> "BAĞLI"
                            state.isConfigured -> "KAYDEDİLDİ"
                            else -> "AYARLANMADI"
                        },
                        status = when {
                            state.isVerified -> OverdrivePillStatus.SUCCESS
                            state.isConfigured -> OverdrivePillStatus.WARNING
                            else -> OverdrivePillStatus.INFO
                        }
                    )
                }
                Text(
                    text = "Uzaktan komutlar (kapı kilidi, ışık yakma, korna) ve canlı bulut telemetrisi.",
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
        BydCloudSubTabRow(
            selectedTab = state.selectedTab,
            onTabSelected = onTabSelected
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Status banner if present
        if (!state.statusMessage.isNullOrBlank()) {
            val bannerBg = if (state.isStatusError) OverdriveTheme.colors.statusDangerContainer else OverdriveTheme.colors.statusSuccessContainer
            val bannerBorder = if (state.isStatusError) OverdriveTheme.colors.statusDanger else OverdriveTheme.colors.statusSuccess
            val bannerText = if (state.isStatusError) OverdriveTheme.colors.statusDanger else OverdriveTheme.colors.statusSuccess

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = bannerBg),
                border = BorderStroke(1.dp, bannerBorder)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (state.isStatusError) Icons.Default.Warning else Icons.Default.Check,
                        contentDescription = null,
                        tint = bannerBorder,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = state.statusMessage,
                        fontSize = 13.sp,
                        color = bannerText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (state.selectedTab) {
                BydCloudTab.ACCOUNT -> AccountTabContent(
                    state = state,
                    onSaveCredentials = onSaveCredentials,
                    onTestConnection = onTestConnection
                )
                BydCloudTab.STATUS -> StatusTabContent(
                    state = state,
                    onToggleCloudDataMerge = onToggleCloudDataMerge
                )
                BydCloudTab.ADVANCED -> AdvancedTabContent(
                    state = state,
                    onOpenClearDialog = onOpenClearDialog
                )
            }
        }
    }

    // Confirmation dialog for clearing credentials
    if (state.showClearDialog) {
        AlertDialog(
            onDismissRequest = onDismissClearDialog,
            title = {
                Text(
                    text = "BYD Cloud Kimlik Bilgilerini Temizle?",
                    fontWeight = FontWeight.Bold,
                    color = OverdriveTheme.colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Kayıtlı oturum anahtarları ve PIN bu cihazdan kaldırılacak. Caydırıcı eylemler (ışık/korna tetikleme) yeniden giriş yapana kadar çalışmayacaktır.",
                    color = OverdriveTheme.colors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmClearCredentials,
                    colors = ButtonDefaults.buttonColors(containerColor = OverdriveTheme.colors.accentRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Temizle", color = OverdriveTheme.colors.onError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissClearDialog) {
                    Text("İptal", color = OverdriveTheme.colors.textSecondary)
                }
            },
            containerColor = OverdriveTheme.colors.surfaceContainer
        )
    }
}

@Composable
private fun BydCloudSubTabRow(
    selectedTab: BydCloudTab,
    onTabSelected: (BydCloudTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OverdriveTheme.colors.surfaceContainerLow, RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        BydCloudTab.values().forEach { tab ->
            val isSelected = tab == selectedTab
            val bg = if (isSelected) OverdriveTheme.colors.surfaceContainerHigh else Color.Transparent
            val textColor = if (isSelected) OverdriveTheme.colors.textPrimary else OverdriveTheme.colors.textSecondary
            val border = if (isSelected) BorderStroke(1.dp, OverdriveTheme.colors.accentGreen.copy(alpha = 0.5f)) else null

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .then(if (border != null) Modifier.border(border, RoundedCornerShape(6.dp)) else Modifier)
                    .background(bg)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        tint = if (isSelected) OverdriveTheme.colors.accentGreen else textColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tab.title,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = textColor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountTabContent(
    state: BydCloudUiState,
    onSaveCredentials: (username: String, password: String, pin: String, countryCode: String, region: String) -> Unit,
    onTestConnection: () -> Unit
) {
    var email by remember(state.username) { mutableStateOf(state.username) }
    var password by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showPin by remember { mutableStateOf(false) }

    var selectedCountry by remember(state.countryCode) {
        mutableStateOf(
            BYD_COUNTRIES.find { it.code.equals(state.countryCode, ignoreCase = true) }
                ?: BYD_COUNTRIES.first()
        )
    }
    var countryDropdownExpanded by remember { mutableStateOf(false) }
    var showSteps by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Recommend Callout (Expandable accordion)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.surfaceContainerLow),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSteps = !showSteps },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = OverdriveTheme.colors.statusInfo,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Öneri: Alternatif Araç Yetkilendirme Profili",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = OverdriveTheme.colors.textPrimary
                        )
                        Text(
                            text = "Ana hesabınızı güvende tutar. Adımları görmek için dokunun.",
                            fontSize = 12.sp,
                            color = OverdriveTheme.colors.textSecondary
                        )
                    }
                    Icon(
                        imageVector = if (showSteps) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = OverdriveTheme.colors.textSecondary
                    )
                }

                AnimatedVisibility(visible = showSteps) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        val steps = listOf(
                            "1. Telefonunuzdaki resmi BYD uygulamasını açın.",
                            "2. Araç Yetkilendirme (Vehicle Authorization) bölümüne gidin.",
                            "3. Yeni Yetkilendirme (New Authorization) düğmesine dokunun.",
                            "4. İkincil bir e-posta adresi girin (ana hesabınızdan farklı olmalı).",
                            "5. Yetkilendirme süresini belirleyin.",
                            "6. BYD uygulamasından çıkış yapıp bu ikincil e-posta ile kayıt/giriş yapın ve şifre oluşturun.",
                            "7. Kilitle veya Aç komutuna dokunarak bir Kontrol PIN'i (Control PIN) belirleyin.",
                            "8. Buraya bu ikincil e-postayı, şifreyi ve belirlediğiniz PIN'i girin."
                        )
                        steps.forEach { step ->
                            Text(
                                text = step,
                                fontSize = 12.sp,
                                color = OverdriveTheme.colors.textSecondary,
                                modifier = Modifier.padding(vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Account Form Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Hesap Bilgileri",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = OverdriveTheme.colors.textPrimary
                )
                Text(
                    text = "Yalnızca türetilmiş anahtar özetleri saklanır, parolanız asla ham metin olarak kaydedilmez.",
                    fontSize = 12.sp,
                    color = OverdriveTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )

                // Country Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = countryDropdownExpanded,
                    onExpandedChange = { countryDropdownExpanded = !countryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = "${selectedCountry.name} (${selectedCountry.code})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ülke (Kayıtlı Bölge)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OverdriveTheme.colors.accentGreen,
                            unfocusedBorderColor = OverdriveTheme.colors.cardBorder,
                            focusedTextColor = OverdriveTheme.colors.textPrimary,
                            unfocusedTextColor = OverdriveTheme.colors.textPrimary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = countryDropdownExpanded,
                        onDismissRequest = { countryDropdownExpanded = false },
                        modifier = Modifier.background(OverdriveTheme.colors.surfaceContainer)
                    ) {
                        BYD_COUNTRIES.forEach { country ->
                            DropdownMenuItem(
                                text = { Text("${country.name} (${country.code})", color = OverdriveTheme.colors.textPrimary) },
                                onClick = {
                                    selectedCountry = country
                                    countryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "Sunucu Bölgesi: ${selectedCountry.region.uppercase()} (Ülkeye göre otomatik eşleşti)",
                    fontSize = 11.sp,
                    color = OverdriveTheme.colors.textSecondary,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 12.dp)
                )

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-posta Adresi") },
                    placeholder = { Text("ornek@byd.com") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OverdriveTheme.colors.accentGreen,
                        unfocusedBorderColor = OverdriveTheme.colors.cardBorder,
                        focusedTextColor = OverdriveTheme.colors.textPrimary,
                        unfocusedTextColor = OverdriveTheme.colors.textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Şifre") },
                    placeholder = {
                        Text(if (state.isVerified) "Mevcut şifreyi koru" else "BYD şifreniz")
                    },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = OverdriveTheme.colors.textSecondary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OverdriveTheme.colors.accentGreen,
                        unfocusedBorderColor = OverdriveTheme.colors.cardBorder,
                        focusedTextColor = OverdriveTheme.colors.textPrimary,
                        unfocusedTextColor = OverdriveTheme.colors.textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Control PIN
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6) pin = it },
                    label = { Text("Kontrol PIN (4–6 Haneli)") },
                    placeholder = {
                        Text(if (state.isVerified) "Mevcut PIN'i koru" else "123456")
                    },
                    singleLine = true,
                    visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    trailingIcon = {
                        IconButton(onClick = { showPin = !showPin }) {
                            Icon(
                                imageVector = if (showPin) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = OverdriveTheme.colors.textSecondary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OverdriveTheme.colors.accentGreen,
                        unfocusedBorderColor = OverdriveTheme.colors.cardBorder,
                        focusedTextColor = OverdriveTheme.colors.textPrimary,
                        unfocusedTextColor = OverdriveTheme.colors.textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            onSaveCredentials(email, password, pin, selectedCountry.code, selectedCountry.region)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OverdriveTheme.colors.primary,
                            contentColor = OverdriveTheme.colors.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !state.isLoading
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = OverdriveTheme.colors.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.isVerified) "Bilgileri Güncelle" else "Giriş Yap ve Kaydet",
                            color = OverdriveTheme.colors.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = onTestConnection,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OverdriveTheme.colors.surfaceContainerHigh),
                        shape = RoundedCornerShape(8.dp),
                        enabled = state.isVerified && !state.isLoading
                    ) {
                        Icon(imageVector = Icons.Default.FlashOn, contentDescription = null, tint = OverdriveTheme.colors.textPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Işıkları Yak (Test)", color = OverdriveTheme.colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusTabContent(
    state: BydCloudUiState,
    onToggleCloudDataMerge: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Vehicle Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Bağlantı Bilgisi",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = OverdriveTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("VIN (Şasi No):", color = OverdriveTheme.colors.textSecondary, fontSize = 13.sp)
                    Text(
                        text = if (state.vin.isNotBlank()) state.vin else "—",
                        color = OverdriveTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Bağlı Hesap:", color = OverdriveTheme.colors.textSecondary, fontSize = 13.sp)
                    Text(
                        text = if (state.username.isNotBlank()) state.username else "—",
                        color = OverdriveTheme.colors.textPrimary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Cloud Push Telemetry
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = OverdriveTheme.colors.accentGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Canlı Bulut Telemetrisi (Push)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = OverdriveTheme.colors.textPrimary
                        )
                    }

                    val ageStr = when {
                        state.lastPushAgeSeconds < 0 -> "Veri bekleniyor"
                        state.lastPushAgeSeconds < 60 -> "${state.lastPushAgeSeconds} sn önce"
                        else -> "${state.lastPushAgeSeconds / 60} dk önce"
                    }
                    Text(
                        text = ageStr,
                        fontSize = 12.sp,
                        color = OverdriveTheme.colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Lock state
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Kilit Durumu", fontSize = 12.sp, color = OverdriveTheme.colors.textSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        val lockText = when (state.pushLockState.lowercase()) {
                            "locked" -> "🔒 Kilitli"
                            "unlocked" -> "🔓 Açık"
                            else -> "—"
                        }
                        Text(lockText, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OverdriveTheme.colors.textPrimary)
                    }

                    // Battery SOC
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Batarya (SOC)", fontSize = 12.sp, color = OverdriveTheme.colors.textSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        val socText = state.pushSocPercent?.let { "🔋 %$it" } ?: "—"
                        Text(socText, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OverdriveTheme.colors.textPrimary)
                    }

                    // Charging state
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Şarj Durumu", fontSize = 12.sp, color = OverdriveTheme.colors.textSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        val chgText = when (state.pushChargingState.lowercase()) {
                            "charging" -> "⚡ Şarj Oluyor"
                            "not_charging" -> "Şarj Olmuyor"
                            else -> "—"
                        }
                        Text(chgText, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OverdriveTheme.colors.textPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Data Merge Toggle Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Araç Verileri İçin Bulutu Kullan",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Text(
                        text = "Araç park halindeyken batarya SOC, şarj ve kabin sıcaklığı BYD bulutundan güncellenir.",
                        fontSize = 12.sp,
                        color = OverdriveTheme.colors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Switch(
                    checked = state.cloudDataMerge,
                    onCheckedChange = onToggleCloudDataMerge,
                    enabled = state.isVerified,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OverdriveTheme.colors.onPrimary,
                        checkedTrackColor = OverdriveTheme.colors.primary,
                        uncheckedThumbColor = OverdriveTheme.colors.outline,
                        uncheckedTrackColor = OverdriveTheme.colors.surfaceContainerHighest
                    )
                )
            }
        }
    }
}

@Composable
private fun AdvancedTabContent(
    state: BydCloudUiState,
    onOpenClearDialog: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.accentRed.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = OverdriveTheme.colors.accentRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tehlikeli Bölge",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = OverdriveTheme.colors.accentRed
                    )
                }

                Text(
                    text = "Bu cihazda kayıtlı olan tüm BYD oturum bilgilerini ve anahtar özetlerini siler. Silindikten sonra uzaktan kontrol ve caydırıcı güvenlik özellikleri durdurulur.",
                    fontSize = 13.sp,
                    color = OverdriveTheme.colors.textSecondary,
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                Button(
                    onClick = onOpenClearDialog,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OverdriveTheme.colors.accentRed),
                    shape = RoundedCornerShape(8.dp),
                    enabled = state.isConfigured || state.isVerified
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = OverdriveTheme.colors.onError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kimlik Bilgilerini Temizle", color = OverdriveTheme.colors.onError, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
