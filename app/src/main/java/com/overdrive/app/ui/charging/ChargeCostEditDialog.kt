package com.overdrive.app.ui.charging

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.overdrive.app.R
import com.overdrive.app.charging.station.EvStation
import com.overdrive.app.charging.station.EvStationRepository
import com.overdrive.app.domain.repository.RepositoryProvider
import com.overdrive.app.ui.component.OverdriveButton
import com.overdrive.app.ui.component.OverdriveButtonVariant
import com.overdrive.app.ui.component.OverdriveCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Advanced Charging Session Entry & Cost Edit Dialog.
 * Direct 100% Jetpack Compose parity with Navion's ChargeCostEditDialog.
 *
 * Features:
 * - Visual Dual-Handle Battery Range Indicator (SoC %20 ➔ %80)
 * - Bidirectional live calculation: Unit Price (₺/kWh), Total Cost (₺), Energy (kWh)
 * - Free Charge Switch (Ücretsiz şarj)
 * - Real vehicle odometer and battery SoC telemetry auto-population
 * - Modification warning badges (⚠️ Değiştirildi) when sensor data is edited
 * - Integration with 22,000+ stations guide and interactive Leaflet map picker
 * - Supports both creating new sessions and editing existing historical records
 */
@Composable
fun ChargeCostEditDialog(
    batteryCapacityKwh: Float,
    sessionToEdit: ChargingSession? = null,
    initialStation: EvStation? = null,
    onDismiss: () -> Unit,
    onSave: (ChargingSession) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { EvStationRepository.getInstance(context) }
    val effectiveCapacity = if (batteryCapacityKwh > 30f) batteryCapacityKwh else 60.48f

    // Read car telemetry for auto-fill defaults
    val livePowertrain = remember {
        try {
            RepositoryProvider.powertrainRepository.powertrainState.value
        } catch (_: Throwable) {
            null
        }
    }
    val defaultOdo = sessionToEdit?.odometerKm
        ?: livePowertrain?.totalMileageKm?.takeIf { it > 0 }
        ?: 0

    // Original baseline values for change detection
    val originalOdo = remember { defaultOdo }
    val originalStartSoc = remember { sessionToEdit?.startSoc?.toFloat() ?: 20f }
    val originalEndSoc = remember { sessionToEdit?.endSoc?.toFloat() ?: 80f }
    val originalEnergy = remember { sessionToEdit?.energyKwh ?: (((originalEndSoc - originalStartSoc).coerceAtLeast(0f) * effectiveCapacity) / 100f) }

    // Dialog form state
    val calendar = remember {
        Calendar.getInstance().apply {
            if (sessionToEdit != null && sessionToEdit.timestamp > 0) {
                timeInMillis = sessionToEdit.timestamp
            }
        }
    }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    var dateText by remember { mutableStateOf(dateFormat.format(calendar.time)) }
    var timeText by remember { mutableStateOf(timeFormat.format(calendar.time)) }

    var odoText by remember { mutableStateOf(if (defaultOdo > 0) defaultOdo.toString() else "") }
    var selectedChargeType by remember {
        mutableStateOf(sessionToEdit?.chargeType ?: if (sessionToEdit?.isDc == true) "DC" else if (initialStation?.isDc == true) "DC" else "DC")
    }

    var isFreeCharge by remember {
        mutableStateOf(sessionToEdit != null && sessionToEdit.totalCost == 0f && sessionToEdit.unitPrice == 0f)
    }

    // Battery SoC Range state
    var startSoc by remember { mutableFloatStateOf(originalStartSoc) }
    var endSoc by remember { mutableFloatStateOf(originalEndSoc) }
    var startSocInput by remember { mutableStateOf(startSoc.toInt().toString()) }
    var endSocInput by remember { mutableStateOf(endSoc.toInt().toString()) }

    // Energy & Financial state
    var energyText by remember {
        mutableStateOf(
            if (sessionToEdit != null) String.format(Locale.US, "%.1f", sessionToEdit.energyKwh)
            else String.format(Locale.US, "%.1f", originalEnergy)
        )
    }

    var unitPriceText by remember {
        mutableStateOf(
            when {
                isFreeCharge -> "0.00"
                sessionToEdit?.unitPrice != null && sessionToEdit.unitPrice > 0f -> String.format(Locale.US, "%.2f", sessionToEdit.unitPrice)
                initialStation != null && initialStation.bestPricePerKwh > 0.0 -> String.format(Locale.US, "%.2f", initialStation.bestPricePerKwh)
                selectedChargeType == "DC" -> "9.50"
                selectedChargeType == "AC" -> "6.50"
                else -> "8.90"
            }
        )
    }

    var totalCostText by remember {
        mutableStateOf(
            when {
                isFreeCharge -> "0.00"
                sessionToEdit?.totalCost != null -> String.format(Locale.US, "%.2f", sessionToEdit.totalCost)
                else -> {
                    val kwh = energyText.replace(',', '.').toFloatOrNull() ?: originalEnergy
                    val price = unitPriceText.replace(',', '.').toFloatOrNull() ?: 9.50f
                    String.format(Locale.US, "%.2f", kwh * price)
                }
            }
        )
    }

    var durationText by remember {
        mutableStateOf(
            (sessionToEdit?.durationMinutes ?: 35).toString()
        )
    }

    var stationName by remember {
        mutableStateOf(
            sessionToEdit?.location ?: initialStation?.displayTitle ?: "Trugo DC Hızlı Şarj"
        )
    }

    // Modals for station picker and map
    var showStationGuide by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }
    var showSensorWarningDialog by remember { mutableStateOf(false) }

    // Detected nearby station (<= 500m)
    val detectedStation = remember {
        EvStationRepository.getLastKnownLocation(context)?.let {
            repository.findStationClosestTo(it.first, it.second, 500.0)
        }
    }

    // Recalculate helper functions
    fun onRangeUpdated(newStart: Float, newEnd: Float) {
        startSoc = newStart.coerceIn(0f, 100f)
        endSoc = newEnd.coerceIn(0f, 100f)
        startSocInput = startSoc.toInt().toString()
        endSocInput = endSoc.toInt().toString()

        val delta = max(0f, endSoc - startSoc)
        val kwh = (delta * effectiveCapacity) / 100f
        energyText = String.format(Locale.US, "%.1f", kwh)

        if (!isFreeCharge) {
            val rate = unitPriceText.replace(',', '.').toFloatOrNull() ?: 0f
            if (rate > 0f) {
                totalCostText = String.format(Locale.US, "%.2f", kwh * rate)
            }
        }
    }

    fun applyUnitPrice(rate: Float) {
        isFreeCharge = false
        unitPriceText = String.format(Locale.US, "%.2f", rate)
        val kwh = energyText.replace(',', '.').toFloatOrNull() ?: 0f
        if (kwh > 0f) {
            totalCostText = String.format(Locale.US, "%.2f", kwh * rate)
        }
    }

    fun performSave(isManualEdit: Boolean) {
        val sSoc = startSoc.toInt()
        val eSoc = endSoc.toInt()
        val dur = durationText.toIntOrNull() ?: 35
        val kwh = energyText.replace(',', '.').toFloatOrNull() ?: (((eSoc - sSoc).coerceAtLeast(0) * effectiveCapacity) / 100f)
        val unitPriceVal = if (isFreeCharge) 0f else unitPriceText.replace(',', '.').toFloatOrNull()
        val costVal = if (isFreeCharge) 0f else (totalCostText.replace(',', '.').toFloatOrNull() ?: (unitPriceVal?.let { it * kwh }))
        val odoVal = odoText.toIntOrNull()

        val costStr = if (costVal != null && costVal > 0f) String.format(Locale.getDefault(), "₺%.2f", costVal) else if (isFreeCharge) "Ücretsiz" else null

        val finalSession = ChargingSession(
            id = sessionToEdit?.id ?: System.currentTimeMillis().toString(),
            timestamp = calendar.timeInMillis,
            location = stationName.ifBlank { if (selectedChargeType == "DC") "DC Hızlı Şarj" else "AC Şarj" },
            startSoc = sSoc,
            endSoc = eSoc,
            energyKwh = kwh,
            durationMinutes = dur,
            peakPowerKw = sessionToEdit?.peakPowerKw ?: if (selectedChargeType == "DC") 120f else 11f,
            costEstimate = costStr,
            odometerKm = odoVal,
            unitPrice = unitPriceVal,
            totalCost = costVal,
            isDc = (selectedChargeType == "DC"),
            chargeType = selectedChargeType,
            isManualEdit = isManualEdit
        )

        onSave(finalSession)
        Toast.makeText(context, "✓ Şarj kaydı başarıyla kaydedildi", Toast.LENGTH_SHORT).show()
        onDismiss()
    }

    // Change detection for sensor alert
    val curOdoVal = odoText.toIntOrNull() ?: 0
    val isOdoChanged = originalOdo > 0 && abs(curOdoVal - originalOdo) > 1
    val isSocChanged = abs(startSoc - originalStartSoc) > 1f || abs(endSoc - originalEndSoc) > 1f
    val hasSensorModification = isOdoChanged || isSocChanged

    // Station Guide & Map Picker Dialogs
    if (showStationGuide) {
        EvStationPickerDialog(
            onDismissRequest = { showStationGuide = false },
            onStationSelected = { st ->
                stationName = st.displayTitle
                selectedChargeType = if (st.isDc) "DC" else "AC"
                if (st.bestPricePerKwh > 0.0) {
                    applyUnitPrice(st.bestPricePerKwh.toFloat())
                }
                showStationGuide = false
            }
        )
    }

    if (showMapPicker) {
        EvStationMapPickerDialog(
            onDismissRequest = { showMapPicker = false },
            onStationSelected = { st ->
                stationName = st.displayTitle
                selectedChargeType = if (st.isDc) "DC" else "AC"
                if (st.bestPricePerKwh > 0.0) {
                    applyUnitPrice(st.bestPricePerKwh.toFloat())
                }
                showMapPicker = false
            }
        )
    }

    // Sensor modification warning dialog
    if (showSensorWarningDialog) {
        AlertDialog(
            onDismissRequest = { showSensorWarningDialog = false },
            title = {
                Text(
                    text = "⚠️ Araç Sensör Verisi Değiştirildi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF59E0B)
                )
            },
            text = {
                Text(
                    text = "Kilometre veya Batarya Seviyesi (SoC) verileri araç ECU sensöründen otomatik okunmuştur.\n\nManuel düzenlemenizi onaylayıp kaydetmek istiyor musunuz?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSensorWarningDialog = false
                    performSave(isManualEdit = true)
                }) {
                    Text("Evet, Kaydet", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSensorWarningDialog = false }) {
                    Text("Gözden Geçir", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth(0.88f)
                .fillMaxSize(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. HEADER BAR (Turuncu / Temalı Üst Bar)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF59E0B))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = "Kapat",
                                tint = Color.White
                            )
                        }

                        Text(
                            text = if (sessionToEdit != null) "Şarj Seansını Düzenle" else "Şarj",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    TextButton(
                        onClick = {
                            if (hasSensorModification) {
                                showSensorWarningDialog = true
                            } else {
                                performSave(isManualEdit = sessionToEdit?.isManualEdit ?: false)
                            }
                        }
                    ) {
                        Text(
                            text = "KAYDET",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }

                // 2. KAYDIRILABİLİR FORM GÖVDESİ
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // SATIR 1: TARİH & SAAT
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_calendar_clock),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )

                        // Tarih
                        OutlinedTextField(
                            value = dateText,
                            onValueChange = { dateText = it },
                            label = { Text("Tarih") },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            calendar.set(Calendar.YEAR, y)
                                            calendar.set(Calendar.MONTH, m)
                                            calendar.set(Calendar.DAY_OF_MONTH, d)
                                            dateText = dateFormat.format(calendar.time)
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_calendar_clock),
                                        contentDescription = "Tarih Seç",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1.3f)
                        )

                        // Saat
                        OutlinedTextField(
                            value = timeText,
                            onValueChange = { timeText = it },
                            label = { Text("Saat") },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    TimePickerDialog(
                                        context,
                                        { _, h, m ->
                                            calendar.set(Calendar.HOUR_OF_DAY, h)
                                            calendar.set(Calendar.MINUTE, m)
                                            timeText = timeFormat.format(calendar.time)
                                        },
                                        calendar.get(Calendar.HOUR_OF_DAY),
                                        calendar.get(Calendar.MINUTE),
                                        true
                                    ).show()
                                }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_calendar_clock),
                                        contentDescription = "Saat Seç",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // SATIR 2: KİLOMETRE SAYACI
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_odometer),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = odoText,
                                onValueChange = { odoText = it },
                                label = { Text("Kilometre sayacı") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (originalOdo > 0) "Son kilometre sayacı: $originalOdo km (Araçtan Okundu)" else "Araç kilometre sayacı",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (isOdoChanged) {
                                    Text(
                                        text = "⚠️ Değiştirildi",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFF59E0B),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // SATIR 3: ŞARJ TİPİ SEÇİCİ
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_charging),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Şarj tipi",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val types = listOf(
                                    Triple("DC", "⚡ DC Hızlı Şarj", Color(0xFF38BDF8)),
                                    Triple("AC", "🔌 AC Tip-2", Color(0xFF10B981)),
                                    Triple("V2L", "🔋 V2L Deşarj", Color(0xFFF59E0B))
                                )

                                types.forEach { (typeKey, label, accentColor) ->
                                    val isSelected = selectedChargeType == typeKey
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) accentColor.copy(alpha = 0.2f)
                                                else MaterialTheme.colorScheme.surfaceContainerHigh
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                selectedChargeType = typeKey
                                                if (typeKey == "DC" && unitPriceText == "6.50") {
                                                    applyUnitPrice(9.50f)
                                                } else if (typeKey == "AC" && unitPriceText == "9.50") {
                                                    applyUnitPrice(6.50f)
                                                }
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // SATIR 4: ÜCRETSİZ ŞARJ TOGGLE
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 36.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Ücretsiz şarj",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Switch(
                            checked = isFreeCharge,
                            onCheckedChange = { checked ->
                                isFreeCharge = checked
                                if (checked) {
                                    unitPriceText = "0.00"
                                    totalCostText = "0.00"
                                } else {
                                    applyUnitPrice(if (selectedChargeType == "DC") 9.50f else 6.50f)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF10B981),
                                checkedTrackColor = Color(0xFF10B981).copy(alpha = 0.5f)
                            )
                        )
                    }

                    // SATIR 5: FİYAT / KWH & TOPLAM MALİYET & KWH
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_payment_card),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )

                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Fiyat / kWh
                            OutlinedTextField(
                                value = unitPriceText,
                                onValueChange = { newVal ->
                                    unitPriceText = newVal
                                    if (!isFreeCharge) {
                                        val unit = newVal.replace(',', '.').toFloatOrNull() ?: 0f
                                        val kwh = energyText.replace(',', '.').toFloatOrNull() ?: 0f
                                        if (kwh > 0f) {
                                            totalCostText = String.format(Locale.US, "%.2f", unit * kwh)
                                        }
                                    }
                                },
                                label = { Text("Fiyat / kWh") },
                                enabled = !isFreeCharge,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF38BDF8),
                                    unfocusedTextColor = Color(0xFF38BDF8)
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            // Toplam Maliyet
                            OutlinedTextField(
                                value = totalCostText,
                                onValueChange = { newVal ->
                                    totalCostText = newVal
                                    if (!isFreeCharge) {
                                        val cost = newVal.replace(',', '.').toFloatOrNull() ?: 0f
                                        val kwh = energyText.replace(',', '.').toFloatOrNull() ?: 0f
                                        if (kwh > 0.1f && cost > 0f) {
                                            unitPriceText = String.format(Locale.US, "%.2f", cost / kwh)
                                        }
                                    }
                                },
                                label = { Text("Toplam maliyet") },
                                enabled = !isFreeCharge,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFFF59E0B),
                                    unfocusedTextColor = Color(0xFFF59E0B)
                                ),
                                modifier = Modifier.weight(1.2f)
                            )

                            // kWh
                            OutlinedTextField(
                                value = energyText,
                                onValueChange = { newVal ->
                                    energyText = newVal
                                    val kwh = newVal.replace(',', '.').toFloatOrNull() ?: 0f
                                    if (!isFreeCharge) {
                                        val unit = unitPriceText.replace(',', '.').toFloatOrNull() ?: 0f
                                        if (unit > 0f) {
                                            totalCostText = String.format(Locale.US, "%.2f", unit * kwh)
                                        }
                                    }
                                },
                                label = { Text("kWh") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF10B981),
                                    unfocusedTextColor = Color(0xFF10B981)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Hızlı Tarife Şablonları
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 36.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val tariffPresets = listOf(
                            Triple("🏠 Ev (2.60 ₺)", 2.60f, Color(0xFF10B981)),
                            Triple("🔌 AC (6.50 ₺)", 6.50f, Color(0xFF38BDF8)),
                            Triple("⚡ DC (9.50 ₺)", 9.50f, Color(0xFFF59E0B)),
                            Triple("🚀 Trugo (8.90 ₺)", 8.90f, Color(0xFF0284C7)),
                            Triple("⚡ ZES (9.40 ₺)", 9.40f, Color(0xFFF59E0B)),
                            Triple("⚡ Eşarj (8.80 ₺)", 8.80f, Color(0xFF10B981))
                        )

                        tariffPresets.forEach { (label, rate, col) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .border(1.dp, col.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .clickable { applyUnitPrice(rate) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = col
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // SATIR 6: BATARYA SEVİYESİ & GÖRSEL RANGE ÇUBUĞU
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_fuel_battery),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(top = 4.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val diff = (endSoc - startSoc).toInt()
                                val sign = if (diff >= 0) "+%$diff" else "%$diff"
                                Text(
                                    text = "Batarya seviyesi: %${startSoc.toInt()} ➔ %${endSoc.toInt()} ($sign)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (isSocChanged) {
                                    Text(
                                        text = "⚠️ Değiştirildi",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFF59E0B),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Görsel Çift Kollu Battery Range Bar
                            BatteryRangeIndicator(
                                startSoc = startSoc,
                                endSoc = endSoc,
                                onRangeChanged = { s, e -> onRangeUpdated(s, e) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Giriş & Çıkış SoC Manuel Giriş Kutuları ve Hızlı Çipler
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Giriş: %",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedTextField(
                                        value = startSocInput,
                                        onValueChange = {
                                            startSocInput = it
                                            val v = it.toFloatOrNull()
                                            if (v != null) {
                                                onRangeUpdated(v, endSoc)
                                            }
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color(0xFF38BDF8),
                                            unfocusedTextColor = Color(0xFF38BDF8)
                                        ),
                                        modifier = Modifier.width(64.dp)
                                    )

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Text(
                                        text = "Çıkış: %",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedTextField(
                                        value = endSocInput,
                                        onValueChange = {
                                            endSocInput = it
                                            val v = it.toFloatOrNull()
                                            if (v != null) {
                                                onRangeUpdated(startSoc, v)
                                            }
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color(0xFF10B981),
                                            unfocusedTextColor = Color(0xFF10B981)
                                        ),
                                        modifier = Modifier.width(64.dp)
                                    )
                                }

                                // Hızlı SoC Şablonları
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(
                                        "20-80" to Pair(20f, 80f),
                                        "10-90" to Pair(10f, 90f),
                                        "20-100" to Pair(20f, 100f)
                                    ).forEach { (lbl, range) ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                                .clickable { onRangeUpdated(range.first, range.second) }
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "%$lbl",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // SATIR 7: SÜRE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_calendar_clock),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )

                        OutlinedTextField(
                            value = durationText,
                            onValueChange = { durationText = it },
                            label = { Text("Şarj Süresi (dakika)") },
                            trailingIcon = { Text("dk", modifier = Modifier.padding(end = 12.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // SATIR 8: ŞARJ KONUMU & İSTASYON
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_location_pin),
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier
                                .size(26.dp)
                                .padding(top = 4.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            // Yakındaki istasyon tespit edildiyse bildirim kartı
                            if (detectedStation != null && stationName != detectedStation.displayTitle) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.12f))
                                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            stationName = detectedStation.displayTitle
                                            selectedChargeType = if (detectedStation.isDc) "DC" else "AC"
                                            if (detectedStation.bestPricePerKwh > 0.0) {
                                                applyUnitPrice(detectedStation.bestPricePerKwh.toFloat())
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "📍 Yakındaki İstasyon: ${detectedStation.displayTitle}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF10B981)
                                            )
                                            Text(
                                                text = "${(detectedStation.distanceKm * 1000).toInt()}m • ₺${String.format(Locale.US, "%.2f", detectedStation.bestPricePerKwh)}/kWh",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "Doldur ➔",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = stationName,
                                onValueChange = { stationName = it },
                                label = { Text("Şarj konumu / İstasyon adı") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Butonlar: Rehber ve Harita
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OverdriveButton(
                                    text = "📍 22.000+ İstasyon Rehberi",
                                    variant = OverdriveButtonVariant.OUTLINED,
                                    onClick = { showStationGuide = true },
                                    modifier = Modifier.weight(1f)
                                )

                                OverdriveButton(
                                    text = "🗺️ Haritadan Seç",
                                    variant = OverdriveButtonVariant.OUTLINED,
                                    onClick = { showMapPicker = true },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // 3. ALT AKSİYON (KAYDET BUTONU)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OverdriveButton(
                        text = "Vazgeç",
                        variant = OverdriveButtonVariant.OUTLINED,
                        onClick = onDismiss
                    )

                    OverdriveButton(
                        text = if (sessionToEdit != null) "💾 GÜNCELLE" else "💾 KAYDET",
                        variant = OverdriveButtonVariant.PRIMARY,
                        onClick = {
                            if (hasSensorModification) {
                                showSensorWarningDialog = true
                            } else {
                                performSave(isManualEdit = sessionToEdit?.isManualEdit ?: false)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Visual Battery Range Indicator Bar.
 * Parity with Navion's BatteryRangeIndicatorView.
 */
@Composable
fun BatteryRangeIndicator(
    startSoc: Float,
    endSoc: Float,
    onRangeChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val frac = (offset.x / size.width).coerceIn(0f, 1f) * 100f
                    val dStart = abs(frac - startSoc)
                    val dEnd = abs(frac - endSoc)
                    if (dStart < dEnd) {
                        onRangeChanged(min(frac, endSoc), endSoc)
                    } else {
                        onRangeChanged(startSoc, max(frac, startSoc))
                    }
                }
            }
            .pointerInput(Unit) {
                var activeThumb = 0 // 1: start, 2: end
                detectDragGestures(
                    onDragStart = { offset ->
                        val frac = (offset.x / size.width).coerceIn(0f, 1f) * 100f
                        val dStart = abs(frac - startSoc)
                        val dEnd = abs(frac - endSoc)
                        activeThumb = if (dStart < dEnd) 1 else 2
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val frac = (change.position.x / size.width).coerceIn(0f, 1f) * 100f
                        if (activeThumb == 1) {
                            onRangeChanged(min(frac, endSoc), endSoc)
                        } else {
                            onRangeChanged(startSoc, max(frac, startSoc))
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val trackHeight = 16.dp.toPx()
            val trackTop = (h - trackHeight) / 2f
            val cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)

            // 1. Arka plan rayı (Dark / Light uyumlu)
            drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(0f, trackTop),
                size = Size(w, trackHeight),
                cornerRadius = cornerRadius
            )

            // 2. Aktif dolum aralığı (StartSoc -> EndSoc)
            val minS = min(startSoc, endSoc).coerceIn(0f, 100f)
            val maxS = max(startSoc, endSoc).coerceIn(0f, 100f)
            val leftX = (minS / 100f) * w
            val rightX = (maxS / 100f) * w

            if (rightX > leftX) {
                drawRoundRect(
                    color = Color(0xFFF59E0B),
                    topLeft = Offset(leftX, trackTop),
                    size = Size(rightX - leftX, trackHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }

            // 3. Noktacıklar / Kademe çizgileri (%20, %40, %60, %80)
            for (step in 20..80 step 20) {
                val dotX = (step / 100f) * w
                val isInside = step in minS.toInt()..maxS.toInt()
                drawCircle(
                    color = if (isInside) Color.White.copy(alpha = 0.9f) else Color(0xFF64748B).copy(alpha = 0.6f),
                    radius = 2.dp.toPx(),
                    center = Offset(dotX, h / 2f)
                )
            }

            // 4. Başlangıç Tutamacı (Start Handle)
            drawCircle(
                color = Color(0xFF0284C7),
                radius = 10.dp.toPx(),
                center = Offset(leftX, h / 2f)
            )
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = Offset(leftX, h / 2f)
            )

            // 5. Bitiş Tutamacı (End Handle)
            drawCircle(
                color = Color(0xFF10B981),
                radius = 10.dp.toPx(),
                center = Offset(rightX, h / 2f)
            )
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = Offset(rightX, h / 2f)
            )
        }
    }
}
