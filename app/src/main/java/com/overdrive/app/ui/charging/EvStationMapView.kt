package com.overdrive.app.ui.charging

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.overdrive.app.R
import com.overdrive.app.charging.station.EvStation
import com.overdrive.app.charging.station.EvStationRepository
import com.overdrive.app.ui.component.OverdriveButton
import com.overdrive.app.ui.component.OverdriveButtonVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Interactive Leaflet Map for visual EV charging station selection across Turkey.
 * Directly integrates Navion's 22,784 SQLite offline database, live center pin targeting,
 * station markers with DC/AC styling, geocoding, vehicle GPS recentering, and search.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EvStationMapView(
    onStationConfirmed: (EvStation) -> Unit,
    initialCoords: Pair<Double, Double>? = null,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { EvStationRepository.getInstance(context) }
    val scope = rememberCoroutineScope()

    // Determine initial center
    val vehicleCoords = remember { EvStationRepository.getLastKnownLocation(context) }
    val defaultLat = initialCoords?.first ?: vehicleCoords?.first ?: 39.9255 // Default Ankara / Turkey
    val defaultLng = initialCoords?.second ?: vehicleCoords?.second ?: 32.8662

    var currentLat by remember { mutableDoubleStateOf(defaultLat) }
    var currentLng by remember { mutableDoubleStateOf(defaultLng) }

    var matchedStation by remember { mutableStateOf<EvStation?>(null) }
    var resolvedAddress by remember { mutableStateOf("") }
    var isGeocoding by remember { mutableStateOf(false) }

    var selectedFilter by remember { mutableStateOf("Tümü") } // "Tümü", "DC", "AC"
    var searchQuery by remember { mutableStateOf("") }
    var searchSuggestions by remember { mutableStateOf<List<EvStation>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapReady by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()

    // Job for debounced map movement and lookup
    var moveJob by remember { mutableStateOf<Job?>(null) }

    fun refreshStationMarkers(lat: Double, lng: Double) {
        val wv = webViewRef ?: return
        if (!isMapReady) return

        scope.launch(Dispatchers.IO) {
            val nearby = repository.findNearestStations(lat, lng, radiusKm = 25.0, limit = 60)
            val filtered = nearby.filter { st ->
                when (selectedFilter) {
                    "DC" -> st.isDc
                    "AC" -> !st.isDc
                    else -> true
                }
            }

            val jsonArray = JSONArray()
            filtered.forEach { st ->
                val obj = JSONObject()
                obj.put("id", st.id)
                obj.put("name", st.displayTitle)
                obj.put("lat", st.latitude)
                obj.put("lng", st.longitude)
                obj.put("op", st.operator)
                obj.put("type", st.chargingType)
                obj.put("power", st.maxPowerKw)
                obj.put("price", st.bestPricePerKwh)
                obj.put("isCustom", st.isCustom)
                jsonArray.put(obj)
            }

            withContext(Dispatchers.Main) {
                wv.evaluateJavascript("setStationMarkers($jsonArray);", null)
            }
        }
    }

    fun focusOn(lat: Double, lng: Double, zoom: Int = 16) {
        val wv = webViewRef ?: return
        currentLat = lat
        currentLng = lng
        wv.evaluateJavascript("setLocation($lat, $lng, $zoom);", null)
    }

    // When filter changes, update markers
    LaunchedEffect(selectedFilter) {
        if (isMapReady) {
            refreshStationMarkers(currentLat, currentLng)
        }
    }

    // Search query autocomplete
    LaunchedEffect(searchQuery) {
        if (searchQuery.trim().length >= 2) {
            isSearching = true
            withContext(Dispatchers.IO) {
                val results = repository.searchStations(
                    query = searchQuery,
                    userLat = currentLat,
                    userLng = currentLng,
                    limit = 6
                )
                withContext(Dispatchers.Main) {
                    searchSuggestions = results
                    isSearching = false
                }
            }
        } else {
            searchSuggestions = emptyList()
            isSearching = false
        }
    }

    val mapBridge = remember {
        object {
            @JavascriptInterface
            fun onMapMoved(lat: Double, lng: Double) {
                scope.launch {
                    currentLat = lat
                    currentLng = lng
                    isGeocoding = true

                    moveJob?.cancel()
                    moveJob = scope.launch {
                        delay(300) // Debounce

                        val (st, addr) = withContext(Dispatchers.IO) {
                            // 1. Check EV stations database within 400m
                            val foundSt = repository.findStationClosestTo(lat, lng, toleranceMeters = 400.0)

                            // 2. Reverse geocode via Android Geocoder
                            val geocoderAddr = try {
                                val geocoder = Geocoder(context, Locale("tr", "TR"))
                                val list = geocoder.getFromLocation(lat, lng, 1)
                                if (!list.isNullOrEmpty()) {
                                    val a = list[0]
                                    val street = a.thoroughfare ?: a.subLocality ?: ""
                                    val district = a.subAdminArea ?: a.locality ?: ""
                                    val city = a.adminArea ?: ""
                                    when {
                                        street.isNotBlank() && district.isNotBlank() -> "$street, $district, $city"
                                        district.isNotBlank() && city.isNotBlank() -> "$district, $city"
                                        city.isNotBlank() -> city
                                        else -> a.getAddressLine(0) ?: String.format(Locale.US, "%.5f, %.5f", lat, lng)
                                    }
                                } else {
                                    String.format(Locale.US, "Konum (%.5f, %.5f)", lat, lng)
                                }
                            } catch (_: Throwable) {
                                String.format(Locale.US, "GPS: %.5f, %.5f", lat, lng)
                            }

                            Pair(foundSt, geocoderAddr)
                        }

                        matchedStation = st
                        resolvedAddress = addr
                        isGeocoding = false

                        // Update markers around new center
                        refreshStationMarkers(lat, lng)
                    }
                }
            }

            @JavascriptInterface
            fun onStationMarkerClicked(lat: Double, lng: Double, stationId: String) {
                scope.launch {
                    focusOn(lat, lng, 17)
                    if (stationId.isNotBlank()) {
                        withContext(Dispatchers.IO) {
                            val st = repository.findNearestStations(lat, lng, radiusKm = 0.5, limit = 1).firstOrNull()
                            if (st != null) {
                                withContext(Dispatchers.Main) {
                                    matchedStation = st
                                    resolvedAddress = if (st.address.isNotBlank()) "${st.address}, ${st.district}, ${st.city}" else "${st.district}, ${st.city}"
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Web Harita Katmanı
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }
                    addJavascriptInterface(mapBridge, "AndroidBridge")
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isMapReady = true
                            focusOn(currentLat, currentLng, 16)
                            refreshStationMarkers(currentLat, currentLng)
                        }
                    }

                    val html = buildLeafletHtml(currentLat, currentLng, isDark)
                    loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
                    webViewRef = this
                }
            }
        )

        // 2. Harita Merkezindeki Sabit Pin Göstergesi
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-20).dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_location_pin),
                contentDescription = "Hedef Pin",
                tint = if (matchedStation != null) Color(0xFF10B981) else Color(0xFFEF4444),
                modifier = Modifier.size(42.dp)
            )
        }
        // Pin ucu yerleşim noktası (zeminde küçük hedef halkası)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(7.dp)
                .clip(CircleShape)
                .background(if (matchedStation != null) Color(0xFF10B981) else Color(0xFFEF4444))
                .border(1.5.dp, Color.White, CircleShape)
        )

        // 3. Üst Arama ve Seçili İstasyon Kartı
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Arama Çubuğu
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Haritada ara (İstasyon adı, şehir, ilçe, operatör)...") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_nav_search),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_clear),
                                        contentDescription = "Temizle",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Arama Önerileri Dropdown
                    AnimatedVisibility(visible = searchSuggestions.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(vertical = 4.dp)
                        ) {
                            searchSuggestions.forEach { st ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            searchQuery = ""
                                            searchSuggestions = emptyList()
                                            focusOn(st.latitude, st.longitude, 17)
                                            matchedStation = st
                                            resolvedAddress = if (st.address.isNotBlank()) "${st.address}, ${st.city}" else st.city
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = if (st.isDc) "⚡ DC" else "🔌 AC",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (st.isDc) Color(0xFF0284C7) else Color(0xFF10B981)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = st.displayTitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${st.district}, ${st.city} • ${st.maxPowerKw.toInt()} kW",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Seçili İstasyon / Konum Bilgi Kartı
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (matchedStation != null) {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)
                },
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (matchedStation != null) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (matchedStation != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.18f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "⚡ ${matchedStation?.operator?.uppercase(Locale.US)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF38BDF8).copy(alpha = 0.18f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "📍 SEÇİLEN KONUM",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            }

                            if (isGeocoding) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = matchedStation?.displayTitle ?: resolvedAddress.ifBlank { "Konum belirleniyor..." },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = if (matchedStation != null) {
                                "${matchedStation?.address} • ${matchedStation?.chargingType} (${matchedStation?.maxPowerKw?.toInt()} kW) • ${String.format(Locale.US, "%.2f ₺/kWh", matchedStation?.bestPricePerKwh ?: 0.0)}"
                            } else {
                                "GPS: ${String.format(Locale.US, "%.5f, %.5f", currentLat, currentLng)}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Hızlı Onay Butonu
                    OverdriveButton(
                        text = "✓ Bu İstasyonu Seç",
                        variant = OverdriveButtonVariant.PRIMARY,
                        onClick = {
                            val st = matchedStation ?: EvStation(
                                id = "custom_${System.currentTimeMillis()}",
                                operator = "Özel / Diğer",
                                name = resolvedAddress.substringBefore(",").ifBlank { "Harita Konumu" },
                                city = resolvedAddress.substringAfterLast(",").trim(),
                                district = "",
                                address = resolvedAddress,
                                latitude = currentLat,
                                longitude = currentLng,
                                chargingType = "DC",
                                maxPowerKw = 120.0,
                                socketCount = 1,
                                acPrice = 7.50,
                                dcPrice = 9.50,
                                isCustom = true
                            )
                            onStationConfirmed(st)
                        }
                    )
                }
            }
        }

        // 4. Sağ Tarafta Yüzen Hızlı Aksiyon Kontrolleri
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Araca Git (GPS) Butonu
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shadowElevation = 8.dp,
                tonalElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .size(46.dp)
                    .clickable {
                        val coords = EvStationRepository.getLastKnownLocation(context)
                        if (coords != null) {
                            focusOn(coords.first, coords.second, 17)
                            Toast.makeText(context, "🎯 Araç GPS konumuna odaklandı", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Araç GPS koordinatı bulunamadı", Toast.LENGTH_SHORT).show()
                        }
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_my_location),
                        contentDescription = "Araca Git",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Zoom In (+)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shadowElevation = 8.dp,
                tonalElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .size(42.dp)
                    .clickable {
                        webViewRef?.evaluateJavascript("map.zoomIn();", null)
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "+",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Zoom Out (-)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shadowElevation = 8.dp,
                tonalElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .size(42.dp)
                    .clickable {
                        webViewRef?.evaluateJavascript("map.zoomOut();", null)
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "−",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 5. Sol Altta Hızlı Filtre Çipleri
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Tümü", "DC", "AC").forEach { filter ->
                val isSelected = selectedFilter == filter
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shadowElevation = 6.dp,
                    tonalElevation = 4.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.clickable { selectedFilter = filter }
                ) {
                    Text(
                        text = filter,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 6. Alt Bilgi & İptal Çubuğu (Varsa)
        if (onCancel != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OverdriveButton(
                    text = "Vazgeç",
                    variant = OverdriveButtonVariant.OUTLINED,
                    onClick = onCancel
                )

                OverdriveButton(
                    text = "✓ Bu Konumu Onayla",
                    variant = OverdriveButtonVariant.PRIMARY,
                    onClick = {
                        val st = matchedStation ?: EvStation(
                            id = "custom_${System.currentTimeMillis()}",
                            operator = "Özel / Diğer",
                            name = resolvedAddress.substringBefore(",").ifBlank { "Harita Konumu" },
                            city = resolvedAddress.substringAfterLast(",").trim(),
                            district = "",
                            address = resolvedAddress,
                            latitude = currentLat,
                            longitude = currentLng,
                            chargingType = "DC",
                            maxPowerKw = 120.0,
                            socketCount = 1,
                            acPrice = 7.50,
                            dcPrice = 9.50,
                            isCustom = true
                        )
                        onStationConfirmed(st)
                    }
                )
            }
        }
    }
}

private fun buildLeafletHtml(initLat: Double, initLng: Double, isDark: Boolean): String {
    val tileUrl = if (isDark) {
        "https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png"
    } else {
        "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
    }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="web/shared/leaflet.css" />
            <script src="web/shared/leaflet.js"></script>
            <script>
                if (typeof L === 'undefined') {
                    document.write('<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />');
                    document.write('<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"><\/script>');
                }
            </script>
            <style>
                html, body, #map {
                    height: 100%;
                    width: 100%;
                    margin: 0;
                    padding: 0;
                    background: ${if (isDark) "#0f172a" else "#f8fafc"};
                }
                .leaflet-control-zoom, .leaflet-control-attribution {
                    display: none !important;
                }
                .ev-station-icon {
                    background: #10b981;
                    border: 2px solid #ffffff;
                    border-radius: 50%;
                    color: white;
                    font-weight: bold;
                    text-align: center;
                    line-height: 24px;
                    font-size: 13px;
                    box-shadow: 0 3px 8px rgba(0,0,0,0.5);
                    cursor: pointer;
                    transition: transform 0.15s ease-out;
                }
                .ev-station-icon:hover, .ev-station-icon:active {
                    transform: scale(1.22);
                }
                .ev-station-dc {
                    background: #0284c7;
                }
                .ev-station-custom {
                    background: #8b5cf6;
                }
                .leaflet-tooltip {
                    background: rgba(15, 23, 42, 0.94) !important;
                    border: 1px solid #38bdf8 !important;
                    color: #ffffff !important;
                    font-size: 11.5px !important;
                    font-weight: 600 !important;
                    border-radius: 6px !important;
                    padding: 4px 8px !important;
                    box-shadow: 0 4px 12px rgba(0,0,0,0.5) !important;
                }
                .leaflet-tooltip-top:before {
                    border-top-color: #38bdf8 !important;
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', {
                    center: [$initLat, $initLng],
                    zoom: 16,
                    zoomControl: false,
                    attributionControl: false
                });

                L.tileLayer('$tileUrl', {
                    maxZoom: 19,
                    subdomains: 'abcd'
                }).addTo(map);

                var stationLayer = L.layerGroup().addTo(map);

                map.on('moveend', function() {
                    var c = map.getCenter();
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onMapMoved(c.lat, c.lng);
                    }
                });

                map.on('click', function(e) {
                    map.setView(e.latlng, map.getZoom());
                });

                function setLocation(lat, lng, zoom) {
                    map.setView([lat, lng], zoom || map.getZoom());
                }

                function setStationMarkers(stations) {
                    stationLayer.clearLayers();
                    if (!stations || !stations.length) return;

                    stations.forEach(function(st) {
                        var isDc = st.type && st.type.indexOf('DC') !== -1;
                        var isCustom = st.isCustom === true;
                        var cls = 'ev-station-icon';
                        if (isCustom) cls += ' ev-station-custom';
                        else if (isDc) cls += ' ev-station-dc';

                        var customIcon = L.divIcon({
                            className: cls,
                            html: '⚡',
                            iconSize: [26, 26],
                            iconAnchor: [13, 13]
                        });

                        var marker = L.marker([st.lat, st.lng], { icon: customIcon });
                        var opText = st.op ? st.op + ' · ' : '';
                        var pwrText = st.power ? ' (' + Math.round(st.power) + ' kW)' : '';
                        marker.bindTooltip(opText + st.name + pwrText, { direction: 'top', offset: [0, -10] });

                        marker.on('click', function() {
                            if (window.AndroidBridge) {
                                window.AndroidBridge.onStationMarkerClicked(st.lat, st.lng, st.id ? String(st.id) : '');
                            }
                        });
                        stationLayer.addLayer(marker);
                    });
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}
