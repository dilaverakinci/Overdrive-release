package com.overdrive.app.ui.mqtt

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
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

enum class MqttTab(val title: String, val icon: ImageVector) {
    CONNECTIONS("Bağlantılar", Icons.Default.Hub),
    ADD_EDIT("Broker Yapılandırma", Icons.Default.Add),
    TELEMETRY("Canlı Telemetri", Icons.Default.ShowChart)
}

data class MqttConnectionItem(
    val id: String,
    val name: String,
    val brokerUrl: String,
    val port: Int,
    val topic: String,
    val enabled: Boolean,
    val isConnected: Boolean,
    val stateText: String,
    val publishedCount: Long = 0L,
    val receivedCount: Long = 0L,
    val errorCount: Long = 0L,
    val lastSeenText: String = "—"
)

data class MqttFormState(
    val id: String? = null,
    val name: String = "Home Assistant",
    val brokerUrl: String = "",
    val port: String = "1883",
    val topic: String = "overdrive/vehicle/telemetry",
    val clientId: String = "",
    val username: String = "",
    val password: String = "",
    val useTls: Boolean = false,
    val trustAllCerts: Boolean = true,
    val qos: Int = 0,
    val minIntervalSeconds: Int = 2,
    val maxIntervalSeconds: Int = 60,
    val changeOnly: Boolean = true
)

data class MqttUiState(
    val selectedTab: MqttTab = MqttTab.CONNECTIONS,
    val connections: List<MqttConnectionItem> = emptyList(),
    val formState: MqttFormState = MqttFormState(),
    val isEditing: Boolean = false,
    val liveTelemetry: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val isError: Boolean = false
)

@Composable
fun MqttScreen(
    state: MqttUiState,
    onTabSelected: (MqttTab) -> Unit,
    onToggleConnectionEnabled: (id: String, enabled: Boolean) -> Unit,
    onEditConnection: (MqttConnectionItem) -> Unit,
    onDeleteConnection: (id: String) -> Unit,
    onTestConnection: (MqttFormState) -> Unit,
    onSaveForm: (MqttFormState) -> Unit,
    onFormChange: (MqttFormState) -> Unit,
    onCancelEdit: () -> Unit,
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
                        text = "MQTT Entegrasyonu",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    val connectedCount = state.connections.count { it.isConnected }
                    OverdriveStatusPill(
                        label = when {
                            state.connections.isEmpty() -> "BROKER YOK"
                            connectedCount > 0 -> "$connectedCount BAĞLI"
                            else -> "BAĞLANTI YOK"
                        },
                        status = when {
                            connectedCount > 0 -> OverdrivePillStatus.SUCCESS
                            state.connections.isNotEmpty() -> OverdrivePillStatus.WARNING
                            else -> OverdrivePillStatus.INFO
                        }
                    )
                }
                Text(
                    text = "Home Assistant veya özel MQTT brokerlarına canlı CAN ve telemetri yayını.",
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
        MqttSubTabRow(
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
                MqttTab.CONNECTIONS -> ConnectionsTabContent(
                    state = state,
                    onToggleEnabled = onToggleConnectionEnabled,
                    onEdit = onEditConnection,
                    onDelete = onDeleteConnection,
                    onAddClick = { onTabSelected(MqttTab.ADD_EDIT) }
                )
                MqttTab.ADD_EDIT -> AddEditTabContent(
                    state = state,
                    onFormChange = onFormChange,
                    onSave = { onSaveForm(state.formState) },
                    onTest = { onTestConnection(state.formState) },
                    onCancel = onCancelEdit
                )
                MqttTab.TELEMETRY -> TelemetryTabContent(
                    telemetry = state.liveTelemetry
                )
            }
        }
    }
}

@Composable
private fun MqttSubTabRow(
    selectedTab: MqttTab,
    onTabSelected: (MqttTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OverdriveTheme.colors.surfaceContainerLow, RoundedCornerShape(8.dp))
            .border(1.dp, OverdriveTheme.colors.cardBorder, RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MqttTab.values().forEach { tab ->
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
// TAB 1: CONNECTIONS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ConnectionsTabContent(
    state: MqttUiState,
    onToggleEnabled: (id: String, enabled: Boolean) -> Unit,
    onEdit: (MqttConnectionItem) -> Unit,
    onDelete: (id: String) -> Unit,
    onAddClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    if (state.connections.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Hub,
                    contentDescription = null,
                    tint = OverdriveTheme.colors.textSecondary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Henüz Bir MQTT Brokerı Eklenmedi",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OverdriveTheme.colors.textPrimary
                )
                Text(
                    text = "Home Assistant veya bulut sunucunuza telemetri aktarmak için yeni bir broker bağlantısı tanımlayın.",
                    fontSize = 13.sp,
                    color = OverdriveTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )
                Button(
                    onClick = onAddClick,
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OverdriveTheme.colors.primary),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = OverdriveTheme.colors.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Yeni Broker Ekle", fontWeight = FontWeight.SemiBold, color = OverdriveTheme.colors.onPrimary)
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.connections.forEach { conn ->
                ConnectionCard(
                    conn = conn,
                    onToggleEnabled = { enabled -> onToggleEnabled(conn.id, enabled) },
                    onEdit = { onEdit(conn) },
                    onDelete = { onDelete(conn.id) }
                )
            }
        }
    }
}

@Composable
private fun ConnectionCard(
    conn: MqttConnectionItem,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
        border = BorderStroke(
            1.dp,
            if (conn.isConnected) OverdriveTheme.colors.accentGreen.copy(alpha = 0.5f) else OverdriveTheme.colors.cardBorder
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                when {
                                    !conn.enabled -> OverdriveTheme.colors.outline
                                    conn.isConnected -> OverdriveTheme.colors.accentGreen
                                    else -> OverdriveTheme.colors.accentRed
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = conn.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OverdriveTheme.colors.textPrimary
                        )
                        Text(
                            text = "${conn.brokerUrl}:${conn.port}",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = OverdriveTheme.colors.textSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = conn.enabled,
                        onCheckedChange = onToggleEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OverdriveTheme.colors.onPrimary,
                            checkedTrackColor = OverdriveTheme.colors.primary,
                            uncheckedThumbColor = OverdriveTheme.colors.outline,
                            uncheckedTrackColor = OverdriveTheme.colors.surfaceContainerHighest
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Düzenle",
                            tint = OverdriveTheme.colors.textSecondary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Sil",
                            tint = OverdriveTheme.colors.accentRed
                        )
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OverdriveTheme.colors.surfaceContainerLow, RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        InfoRow(label = "Ana Konu (Topic)", value = conn.topic)
                        InfoRow(label = "Bağlantı Durumu", value = conn.stateText)
                        InfoRow(label = "Yayınlanan Mesaj", value = "${conn.publishedCount}")
                        InfoRow(label = "Alınan Mesaj", value = "${conn.receivedCount}")
                        InfoRow(label = "Hata Sayısı", value = "${conn.errorCount}")
                        InfoRow(label = "Son Sinyal", value = conn.lastSeenText)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 2: ADD / EDIT
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AddEditTabContent(
    state: MqttUiState,
    onFormChange: (MqttFormState) -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
    onCancel: () -> Unit
) {
    val f = state.formState
    val scrollState = rememberScrollState()
    var passwordVisible by remember { mutableStateOf(false) }

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
                    text = if (state.isEditing) "Broker Bağlantısını Düzenle" else "Yeni MQTT Brokerı Ekle",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = OverdriveTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = f.name,
                    onValueChange = { onFormChange(f.copy(name = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Bağlantı Adı (ör: Home Assistant)") },
                    singleLine = true,
                    colors = outlinedColors()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = f.brokerUrl,
                        onValueChange = { onFormChange(f.copy(brokerUrl = it)) },
                        modifier = Modifier.weight(2f),
                        label = { Text("Broker Sunucu IP / Host") },
                        placeholder = { Text("192.168.1.100 veya broker.com") },
                        singleLine = true,
                        colors = outlinedColors()
                    )
                    OutlinedTextField(
                        value = f.port,
                        onValueChange = { onFormChange(f.copy(port = it)) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Port") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = outlinedColors()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = f.username,
                        onValueChange = { onFormChange(f.copy(username = it)) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Kullanıcı Adı (Opsiyonel)") },
                        singleLine = true,
                        colors = outlinedColors()
                    )
                    OutlinedTextField(
                        value = f.password,
                        onValueChange = { onFormChange(f.copy(password = it)) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Parola (Opsiyonel)") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = OverdriveTheme.colors.textSecondary
                                )
                            }
                        },
                        colors = outlinedColors()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = f.topic,
                    onValueChange = { onFormChange(f.copy(topic = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ana Konu Başlığı (Topic Prefix)") },
                    singleLine = true,
                    colors = outlinedColors()
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingToggleRow(
                    title = "TLS / SSL Güvenli Bağlantı",
                    subtitle = "Port 8883 veya TLS sertifikalı brokerlar için gereklidir.",
                    checked = f.useTls,
                    onCheckedChange = { onFormChange(f.copy(useTls = it)) }
                )

                if (f.useTls) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingToggleRow(
                        title = "Tüm Sertifikalara Güven (Self-signed)",
                        subtitle = "Home Assistant gibi yerel ağ sertifikalarını doğrulamadan kabul eder.",
                        checked = f.trustAllCerts,
                        onCheckedChange = { onFormChange(f.copy(trustAllCerts = it)) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                SettingToggleRow(
                    title = "Yalnızca Değişiklik Olduğunda Yayınla",
                    subtitle = "Değerler değişmediğinde ağ trafiği oluşturmaz.",
                    checked = f.changeOnly,
                    onCheckedChange = { onFormChange(f.copy(changeOnly = it)) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Minimum Yayın Aralığı: ${f.minIntervalSeconds} sn",
                    fontSize = 14.sp,
                    color = OverdriveTheme.colors.textPrimary
                )
                Slider(
                    value = f.minIntervalSeconds.toFloat(),
                    onValueChange = { onFormChange(f.copy(minIntervalSeconds = it.toInt())) },
                    valueRange = 1f..30f,
                    colors = SliderDefaults.colors(
                        thumbColor = OverdriveTheme.colors.accentGreen,
                        activeTrackColor = OverdriveTheme.colors.accentGreen
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onSave,
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
                            text = if (state.isEditing) "Güncelle" else "Brokerı Kaydet",
                            fontWeight = FontWeight.SemiBold,
                            color = OverdriveTheme.colors.onPrimary
                        )
                    }

                    Button(
                        onClick = onTest,
                        modifier = Modifier.height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OverdriveTheme.colors.surfaceContainerHigh),
                        border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(text = "Test Et", color = OverdriveTheme.colors.textPrimary)
                    }

                    if (state.isEditing) {
                        Button(
                            onClick = onCancel,
                            modifier = Modifier.height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(text = "İptal", color = OverdriveTheme.colors.textSecondary)
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
// TAB 3: TELEMETRY
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TelemetryTabContent(
    telemetry: Map<String, String>
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
                    text = "Yayınlanan Canlı Telemetri Konuları",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OverdriveTheme.colors.textPrimary
                )
                Text(
                    text = "MQTT brokerına anlık olarak aktarılan konu ve yük (payload) eşleşmeleri.",
                    fontSize = 13.sp,
                    color = OverdriveTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                if (telemetry.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Canlı MQTT yayını bekleniyor...",
                            fontSize = 13.sp,
                            color = OverdriveTheme.colors.textSecondary
                        )
                    }
                } else {
                    telemetry.forEach { (topic, payload) ->
                        InfoRow(label = topic, value = payload)
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
private fun outlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = OverdriveTheme.colors.primary,
    unfocusedBorderColor = OverdriveTheme.colors.cardBorder,
    focusedLabelColor = OverdriveTheme.colors.primary,
    unfocusedLabelColor = OverdriveTheme.colors.textSecondary,
    focusedTextColor = OverdriveTheme.colors.textPrimary,
    unfocusedTextColor = OverdriveTheme.colors.textPrimary
)

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
