package com.overdrive.app.ui.telegram

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
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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

enum class TelegramTab(val title: String, val icon: ImageVector) {
    BOT("Bot Ayarları", Icons.Default.Send),
    PAIR("Eşleştirme", Icons.Default.Link),
    PREFERENCES("Bildirim Tercihleri", Icons.Default.Notifications)
}

data class TelegramUiState(
    val selectedTab: TelegramTab = TelegramTab.BOT,

    // Bot Tab
    val isConfigured: Boolean = false,
    val isPaired: Boolean = false,
    val botTokenInput: String = "",
    val botUsername: String = "",
    val botFirstName: String = "",

    // Pair Tab
    val pendingPin: String? = null,
    val pinExpiresSeconds: Int = 0,
    val ownerFirstName: String = "",
    val ownerUsername: String = "",
    val ownerChatId: Long = -1L,

    // Preferences Tab
    val autoStartAccOff: Boolean = false,
    val videoUploads: Boolean = false,
    val criticalAlerts: Boolean = true,
    val tyreAlerts: Boolean = true,
    val parkingMessages: Boolean = true,
    val motionText: Boolean = true,
    val tierNotices: Boolean = false,
    val tierAlerts: Boolean = true,
    val tierCritical: Boolean = true,

    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val isError: Boolean = false
)

@Composable
fun TelegramScreen(
    state: TelegramUiState,
    onTabSelected: (TelegramTab) -> Unit,
    onTokenInputChange: (String) -> Unit,
    onConnectToken: () -> Unit,
    onClearToken: () -> Unit,
    onGeneratePin: () -> Unit,
    onUnpair: () -> Unit,
    onToggleAutoStart: (Boolean) -> Unit,
    onToggleVideoUploads: (Boolean) -> Unit,
    onToggleCriticalAlerts: (Boolean) -> Unit,
    onToggleTyreAlerts: (Boolean) -> Unit,
    onToggleParkingMessages: (Boolean) -> Unit,
    onToggleMotionText: (Boolean) -> Unit,
    onToggleTierNotices: (Boolean) -> Unit,
    onToggleTierAlerts: (Boolean) -> Unit,
    onToggleTierCritical: (Boolean) -> Unit,
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
                        text = "Telegram Entegrasyonu",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    OverdriveStatusPill(
                        label = when {
                            state.isPaired -> "EŞLEŞTİRİLDİ (PAIRED)"
                            state.isConfigured -> "EŞLEŞME BEKLENİYOR"
                            else -> "YAPILANDIRILMADI"
                        },
                        status = when {
                            state.isPaired -> OverdrivePillStatus.SUCCESS
                            state.isConfigured -> OverdrivePillStatus.WARNING
                            else -> OverdrivePillStatus.INFO
                        }
                    )
                }
                Text(
                    text = "Gözetim alarmları, anlık fotoğraf ve araç telemetri bildirimlerini Telegram'dan alın.",
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
        TelegramSubTabRow(
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
                TelegramTab.BOT -> BotTabContent(
                    state = state,
                    onTokenInputChange = onTokenInputChange,
                    onConnectToken = onConnectToken,
                    onClearToken = onClearToken
                )
                TelegramTab.PAIR -> PairTabContent(
                    state = state,
                    onGeneratePin = onGeneratePin,
                    onUnpair = onUnpair
                )
                TelegramTab.PREFERENCES -> PreferencesTabContent(
                    state = state,
                    onToggleAutoStart = onToggleAutoStart,
                    onToggleVideoUploads = onToggleVideoUploads,
                    onToggleCriticalAlerts = onToggleCriticalAlerts,
                    onToggleTyreAlerts = onToggleTyreAlerts,
                    onToggleParkingMessages = onToggleParkingMessages,
                    onToggleMotionText = onToggleMotionText,
                    onToggleTierNotices = onToggleTierNotices,
                    onToggleTierAlerts = onToggleTierAlerts,
                    onToggleTierCritical = onToggleTierCritical
                )
            }
        }
    }
}

@Composable
private fun TelegramSubTabRow(
    selectedTab: TelegramTab,
    onTabSelected: (TelegramTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OverdriveTheme.colors.surfaceContainerLow, RoundedCornerShape(8.dp))
            .border(1.dp, OverdriveTheme.colors.cardBorder, RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TelegramTab.values().forEach { tab ->
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
// TAB 1: BOT
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BotTabContent(
    state: TelegramUiState,
    onTokenInputChange: (String) -> Unit,
    onConnectToken: () -> Unit,
    onClearToken: () -> Unit
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
                    text = "Telegram Bot Tokeni",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OverdriveTheme.colors.textPrimary
                )
                Text(
                    text = "Telegram'da @BotFather üzerinden yeni bir bot oluşturun, verilen API tokenini buraya girin. Token yalnızca cihazınızda şifreli saklanır.",
                    fontSize = 13.sp,
                    color = OverdriveTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                OutlinedTextField(
                    value = state.botTokenInput,
                    onValueChange = onTokenInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Bot API Tokeni (ör: 123456789:ABCdef...)") },
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onConnectToken,
                        modifier = Modifier
                            .weight(1f)
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
                            text = "Bağlan ve Test Et",
                            fontWeight = FontWeight.SemiBold,
                            color = OverdriveTheme.colors.onPrimary
                        )
                    }

                    if (state.isConfigured) {
                        Button(
                            onClick = onClearToken,
                            modifier = Modifier.height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OverdriveTheme.colors.accentRed.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, OverdriveTheme.colors.accentRed),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = OverdriveTheme.colors.accentRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tokeni Sil",
                                color = OverdriveTheme.colors.accentRed
                            )
                        }
                    }
                }

                // Connected Bot Details
                if (state.isConfigured && (state.botUsername.isNotBlank() || state.botFirstName.isNotBlank())) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(OverdriveTheme.colors.surfaceContainerLow, RoundedCornerShape(8.dp))
                            .border(1.dp, OverdriveTheme.colors.accentGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                tint = OverdriveTheme.colors.accentGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = state.botFirstName.ifBlank { state.botUsername },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OverdriveTheme.colors.textPrimary
                                )
                                Text(
                                    text = if (state.botUsername.isNotBlank()) "@${state.botUsername}" else "",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = OverdriveTheme.colors.accentGreen
                                )
                            }
                        }
                    }
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
// TAB 2: PAIR
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PairTabContent(
    state: TelegramUiState,
    onGeneratePin: () -> Unit,
    onUnpair: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!state.isConfigured) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
                border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = OverdriveTheme.colors.textSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Önce Bot Tokenini Yapılandırın",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Text(
                        text = "Aracınızı Telegram hesabınızla eşleştirebilmek için lütfen önce 'Bot Ayarları' sekmesinden bot tokeninizi kaydedin.",
                        fontSize = 13.sp,
                        color = OverdriveTheme.colors.textSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else if (state.isPaired) {
            // Already Paired Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
                border = BorderStroke(1.dp, OverdriveTheme.colors.accentGreen.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Eşleştirilmiş Sahip Hesabı",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = OverdriveTheme.colors.textPrimary
                        )
                        OverdriveStatusPill(label = "BAĞLI", status = OverdrivePillStatus.SUCCESS)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(label = "Sahip İsmi", value = state.ownerFirstName.ifBlank { "Bilinmiyor" })
                    InfoRow(label = "Telegram Kullanıcı Adı", value = if (state.ownerUsername.isNotBlank()) "@${state.ownerUsername}" else "—")
                    InfoRow(label = "Telegram Chat ID", value = if (state.ownerChatId > 0) state.ownerChatId.toString() else "—")

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onUnpair,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OverdriveTheme.colors.accentRed.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, OverdriveTheme.colors.accentRed),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Eşleşmeyi Kaldır (Unpair)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OverdriveTheme.colors.accentRed
                        )
                    }
                }
            }
        } else {
            // Pairing in Progress (PIN card)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
                border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Telegram Hesabı ile Eşleştirme",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Text(
                        text = "Aşağıdaki butona basarak tek kullanımlık 6 haneli bir PIN kodu üretin. Ardından Telegram'da botunuza '/pair <PIN>' mesajını gönderin.",
                        fontSize = 13.sp,
                        color = OverdriveTheme.colors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                    )

                    if (state.pendingPin != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(OverdriveTheme.colors.primary.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                .border(1.dp, OverdriveTheme.colors.primary, RoundedCornerShape(10.dp))
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = state.pendingPin,
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 8.sp,
                                    color = OverdriveTheme.colors.primary
                                )
                                if (state.pinExpiresSeconds > 0) {
                                    val mm = state.pinExpiresSeconds / 60
                                    val ss = state.pinExpiresSeconds % 60
                                    Text(
                                        text = "Kalan Süre: %02d:%02d".format(mm, ss),
                                        fontSize = 12.sp,
                                        color = OverdriveTheme.colors.textSecondary,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    Button(
                        onClick = onGeneratePin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OverdriveTheme.colors.primary),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = OverdriveTheme.colors.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.pendingPin != null) "Yeni PIN Üret" else "Eşleştirme PIN Kodu Üret",
                            fontWeight = FontWeight.SemiBold,
                            color = OverdriveTheme.colors.onPrimary
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 3: PREFERENCES
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PreferencesTabContent(
    state: TelegramUiState,
    onToggleAutoStart: (Boolean) -> Unit,
    onToggleVideoUploads: (Boolean) -> Unit,
    onToggleCriticalAlerts: (Boolean) -> Unit,
    onToggleTyreAlerts: (Boolean) -> Unit,
    onToggleParkingMessages: (Boolean) -> Unit,
    onToggleMotionText: (Boolean) -> Unit,
    onToggleTierNotices: (Boolean) -> Unit,
    onToggleTierAlerts: (Boolean) -> Unit,
    onToggleTierCritical: (Boolean) -> Unit
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
                    text = "Arka Plan ve Medya Ayarları",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OverdriveTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                SettingToggleRow(
                    title = "Park Halinde Telegram Botunu Çalıştır (ACC OFF)",
                    subtitle = "Araç kontak kapalıyken de Telegram botunun gelen komutları dinlemesini sağlar.",
                    checked = state.autoStartAccOff,
                    onCheckedChange = onToggleAutoStart
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingToggleRow(
                    title = "Olay Kliplerini Telegram'a Yükle (Video Uploads)",
                    subtitle = "Gözetim alarmı veya yakınlık uyarısında kaydedilen MP4 videosunu doğrudan Telegram sohbetinize gönderir.",
                    checked = state.videoUploads,
                    onCheckedChange = onToggleVideoUploads
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Bildirim Kategorileri",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OverdriveTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                SettingToggleRow(
                    title = "Kritik Güvenlik Alarmları",
                    subtitle = "Şarj arızası, yetkisiz hareket, çekilme veya 12V akü kritik seviyesi alarmları.",
                    checked = state.criticalAlerts,
                    onCheckedChange = onToggleCriticalAlerts
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingToggleRow(
                    title = "Lastik Basınç ve Kaçak Uyarıları (TPMS)",
                    subtitle = "Lastik basıncı düştüğünde veya ani hava kaçağında Telegram uyarısı.",
                    checked = state.tyreAlerts,
                    onCheckedChange = onToggleTyreAlerts
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingToggleRow(
                    title = "Park ve Çevre Raporu Mesajları",
                    subtitle = "Araç park edildiğinde ve araca dönüldüğünde 4-kamera panoramik fotoğraf ve özet gönderir.",
                    checked = state.parkingMessages,
                    onCheckedChange = onToggleParkingMessages
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingToggleRow(
                    title = "Hareket Metin Bildirimleri",
                    subtitle = "Gözetim kamerası hareket yakaladığında anlık metin bildirimi.",
                    checked = state.motionText,
                    onCheckedChange = onToggleMotionText
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Bildirim Şiddet Seviyeleri (Tiers)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OverdriveTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                SettingToggleRow(
                    title = "Bilgilendirme / Rutin Notlar (Notices)",
                    subtitle = "Düşük öncelikli rutin durum ve bağlantı bildirimleri.",
                    checked = state.tierNotices,
                    onCheckedChange = onToggleTierNotices
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingToggleRow(
                    title = "Alarmlar (Alerts)",
                    subtitle = "Standart gözetim hareket ve yakınlık alarmları.",
                    checked = state.tierAlerts,
                    onCheckedChange = onToggleTierAlerts
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingToggleRow(
                    title = "Kritik Seviye (Critical)",
                    subtitle = "En yüksek öncelikli acil durum uyarıları.",
                    checked = state.tierCritical,
                    onCheckedChange = onToggleTierCritical
                )
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
            color = OverdriveTheme.colors.textPrimary
        )
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
