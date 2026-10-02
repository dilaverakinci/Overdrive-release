package com.overdrive.app.ui.charging

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.overdrive.app.R
import com.overdrive.app.charging.station.EvStation
import com.overdrive.app.charging.station.EvStationRepository
import com.overdrive.app.ui.component.OverdriveButton
import com.overdrive.app.ui.component.OverdriveButtonVariant
import com.overdrive.app.ui.component.OverdriveCard
import com.overdrive.app.ui.component.OverdriveDialog
import com.overdrive.app.ui.theme.OverdriveTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * 100% Jetpack Compose Native Turkey EV Charging Stations Picker Dialog.
 * Direct parity with Navion's 22,000+ EV charging database and custom charger persistence.
 */
@Composable
fun EvStationPickerDialog(
    onDismissRequest: () -> Unit,
    onStationSelected: (EvStation) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repository = remember { EvStationRepository.getInstance(context) }

    val userCoords = remember { EvStationRepository.getLastKnownLocation(context) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Tümü") } // "Tümü", "DC", "AC", "Özel", "Trugo", "ZES", "Eşarj", "Astor"

    var stations by remember { mutableStateOf<List<EvStation>>(emptyList()) }
    var nearestDetectedStation by remember { mutableStateOf<EvStation?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var showAddCustomDialog by remember { mutableStateOf(false) }

    // Fetch stations when query or filter changes
    LaunchedEffect(searchQuery, selectedFilter) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val userLat = userCoords?.first
            val userLng = userCoords?.second

            val rawList = if (searchQuery.isNotBlank()) {
                repository.searchStations(searchQuery, userLat, userLng, limit = 80)
            } else if (userLat != null && userLng != null) {
                repository.findNearestStations(userLat, userLng, radiusKm = 60.0, limit = 80)
            } else {
                repository.searchStations("Trugo", null, null, limit = 80)
            }

            // Check if user is closely located to any station (<= 500m)
            if (userLat != null && userLng != null && nearestDetectedStation == null) {
                nearestDetectedStation = repository.findStationClosestTo(userLat, userLng, 500.0)
            }

            // Filter results
            val filtered = rawList.filter { st ->
                when (selectedFilter) {
                    "DC" -> st.isDc
                    "AC" -> !st.isDc
                    "Özel" -> st.isCustom
                    "Trugo" -> st.operator.contains("Trugo", ignoreCase = true) || st.name.contains("Trugo", ignoreCase = true)
                    "ZES" -> st.operator.contains("ZES", ignoreCase = true) || st.name.contains("ZES", ignoreCase = true)
                    "Eşarj" -> st.operator.contains("Eşarj", ignoreCase = true) || st.name.contains("Eşarj", ignoreCase = true)
                    "Astor" -> st.operator.contains("Astor", ignoreCase = true) || st.name.contains("Astor", ignoreCase = true)
                    else -> true
                }
            }

            withContext(Dispatchers.Main) {
                stations = filtered
                isLoading = false
            }
        }
    }

    if (showAddCustomDialog) {
        AddCustomEvStationDialog(
            currentCoords = userCoords,
            onDismiss = { showAddCustomDialog = false },
            onStationCreated = { newStation ->
                repository.addCustomStation(newStation)
                showAddCustomDialog = false
                onStationSelected(newStation)
            }
        )
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .fillMaxSize(0.90f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_poi_charging),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Şarj İstasyonu Rehberi",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Türkiye Geneli 22.000+ İstasyon & Özel Şarj Noktaları",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OverdriveButton(
                            text = "+ Özel İstasyon Ekle",
                            variant = OverdriveButtonVariant.OUTLINED,
                            onClick = { showAddCustomDialog = true }
                        )

                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                painter = painterResource(R.drawable.ic_clear),
                                contentDescription = "Kapat",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("İstasyon adı, operatör, il veya ilçe ara (örn: Trugo, ZES, Ankara, Bolu)...") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_nav_search),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_clear),
                                    contentDescription = "Temizle",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filterOptions = listOf(
                        "Tümü" to "Tümü",
                        "DC" to "⚡ DC Hızlı",
                        "AC" to "🔌 AC Yavaş",
                        "Özel" to "⭐ Özel İstasyonlarım",
                        "Trugo" to "Trugo",
                        "ZES" to "ZES",
                        "Eşarj" to "Eşarj",
                        "Astor" to "Astor"
                    )

                    filterOptions.forEach { (key, label) ->
                        val isSelected = selectedFilter == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedFilter = key }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // If nearby station detected at current vehicle location
                nearestDetectedStation?.let { detected ->
                    OverdriveCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = 10.dp,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "📍",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Column {
                                    Text(
                                        text = "Araç Şu An Şarj İstasyonunda Algılandı",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${detected.displayTitle} (${(detected.distanceKm * 1000).toInt()}m)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            OverdriveButton(
                                text = "Bu İstasyonu Seç",
                                variant = OverdriveButtonVariant.PRIMARY,
                                onClick = { onStationSelected(detected) }
                            )
                        }
                    }
                }

                // Results Count & Distance Label
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isLoading) "İstasyonlar aranıyor..." else "${stations.size} istasyon bulundu",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (userCoords != null) {
                        Text(
                            text = "📍 Canlı GPS Konumuna Göre Sıralı",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Station List
                if (stations.isEmpty() && !isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Aramanıza uygun şarj istasyonu bulunamadı",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OverdriveButton(
                                text = "Bu Konumu Özel İstasyon Olarak Ekle",
                                variant = OverdriveButtonVariant.OUTLINED,
                                onClick = { showAddCustomDialog = true }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(stations, key = { it.id }) { station ->
                            StationListItem(
                                station = station,
                                onSelect = { onStationSelected(station) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StationListItem(
    station: EvStation,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OverdriveCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        contentPadding = 10.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // Title and Badges Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // DC / AC Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (station.isDc) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            )
                            .border(
                                1.dp,
                                if (station.isDc) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.secondary,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (station.isDc) "DC ${station.maxPowerKw.toInt()} kW" else "AC ${station.maxPowerKw.toInt()} kW",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (station.isDc) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }

                    if (station.socketCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${station.socketCount} Soket",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = station.displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Location / Address
                val locationText = when {
                    station.district.isNotBlank() && station.city.isNotBlank() -> "${station.city}, ${station.district}"
                    station.city.isNotBlank() -> station.city
                    else -> station.address
                }
                Text(
                    text = if (station.address.isNotBlank() && station.address != locationText) "$locationText · ${station.address}" else locationText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right side: Price & Distance
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (station.bestPricePerKwh > 0.0) {
                    Text(
                        text = String.format(Locale.getDefault(), "₺%.2f/kWh", station.bestPricePerKwh),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (station.distanceKm > 0.0) {
                    Text(
                        text = if (station.distanceKm < 1.0) "${(station.distanceKm * 1000).toInt()} m"
                        else String.format(Locale.getDefault(), "%.1f km", station.distanceKm),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AddCustomEvStationDialog(
    currentCoords: Pair<Double, Double>?,
    onDismiss: () -> Unit,
    onStationCreated: (EvStation) -> Unit,
) {
    var name by remember { mutableStateOf("Evim - Wallbox") }
    var operator by remember { mutableStateOf("Kişisel / Ev") }
    var city by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var isDc by remember { mutableStateOf(false) }
    var powerKw by remember { mutableStateOf("11") }
    var priceText by remember { mutableStateOf("2.60") }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    )

    OverdriveDialog(
        onDismissRequest = onDismiss,
        title = "Yeni Özel Şarj İstasyonu Ekle",
        positiveButtonText = "Kaydet ve Seç",
        onPositiveClick = {
            val pKw = powerKw.replace(',', '.').toDoubleOrNull() ?: (if (isDc) 60.0 else 11.0)
            val price = priceText.replace(',', '.').toDoubleOrNull() ?: 0.0
            val st = EvStation(
                id = "custom_${System.currentTimeMillis()}",
                operator = operator.trim().ifEmpty { "Özel" },
                name = name.trim().ifEmpty { "Özel İstasyon" },
                city = city.trim(),
                district = district.trim(),
                address = address.trim(),
                latitude = currentCoords?.first ?: 0.0,
                longitude = currentCoords?.second ?: 0.0,
                chargingType = if (isDc) "DC" else "AC",
                maxPowerKw = pKw,
                socketCount = 1,
                acPrice = if (!isDc) price else 0.0,
                dcPrice = if (isDc) price else 0.0,
                isCustom = true
            )
            onStationCreated(st)
        },
        negativeButtonText = "Vazgeç",
        onNegativeClick = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Quick Template Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val templates = listOf(
                    Triple("Ev Wallbox", "Kişisel / Ev", false to "2.60"),
                    Triple("İşyeri Şarjı", "İşyeri", false to "0.00"),
                    Triple("Yazlık AC", "Kişisel", false to "2.60"),
                    Triple("Özel DC İstasyon", "Özel", true to "8.50")
                )

                templates.forEach { (tmplName, tmplOp, typeAndPrice) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable {
                                name = tmplName
                                operator = tmplOp
                                isDc = typeAndPrice.first
                                priceText = typeAndPrice.second
                                powerKw = if (typeAndPrice.first) "60" else "11"
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tmplName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Name & Operator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("İstasyon Adı") },
                    singleLine = true,
                    colors = textFieldColors,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = operator,
                    onValueChange = { operator = it },
                    label = { Text("Sağlayıcı / Operatör") },
                    singleLine = true,
                    colors = textFieldColors,
                    modifier = Modifier.weight(1f)
                )
            }

            // AC / DC Toggle & Power
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // AC / DC Toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!isDc) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                            .clickable { isDc = false }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "AC",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (!isDc) FontWeight.Bold else FontWeight.Normal,
                            color = if (!isDc) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDc) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { isDc = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "DC",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isDc) FontWeight.Bold else FontWeight.Normal,
                            color = if (isDc) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = powerKw,
                    onValueChange = { powerKw = it },
                    label = { Text("Güç (kW)") },
                    singleLine = true,
                    colors = textFieldColors,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Fiyat (₺/kWh)") },
                    singleLine = true,
                    colors = textFieldColors,
                    modifier = Modifier.weight(1f)
                )
            }

            // City & District
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Şehir") },
                    singleLine = true,
                    colors = textFieldColors,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = district,
                    onValueChange = { district = it },
                    label = { Text("İlçe") },
                    singleLine = true,
                    colors = textFieldColors,
                    modifier = Modifier.weight(1f)
                )
            }

            // Address
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Adres / Konum Tarifi") },
                singleLine = true,
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
