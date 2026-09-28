package com.overdrive.app.ui.parking

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.overdrive.app.R
import com.overdrive.app.ui.theme.OverdriveTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ParkingScreen(
    viewModel: ParkingViewModel,
    onNavigateToRecordings: (String?) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDeleteConfirmDialog by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val savedStr = stringResource(R.string.parking_toast_saved)
    val deletedStr = stringResource(R.string.parking_toast_deleted)
    val deleteFailedStr = stringResource(R.string.parking_toast_delete_failed)
    val queuedStr = stringResource(R.string.parking_toast_signage_queued)

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msgKey ->
            val text = when (msgKey) {
                "saved" -> savedStr
                "deleted" -> deletedStr
                "delete_failed" -> deleteFailedStr
                "signage_queued" -> queuedStr
                else -> msgKey
            }
            snackbarHostState.showSnackbar(text)
            viewModel.clearToast()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Top App Bar
            ParkingTopBar(
                activeTab = uiState.activeTab,
                isRunning = uiState.isRunning,
                isLoading = uiState.isLoading,
                onTabSelected = { viewModel.setTab(it) },
                onRefresh = { viewModel.loadData() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Main Content Area
            Crossfade(targetState = uiState.activeTab, label = "ParkingTabCrossfade") { tab ->
                when (tab) {
                    ParkingTab.SESSIONS -> {
                        if (uiState.selectedSessionId != null) {
                            ParkingDetailView(
                                detailData = uiState.detailData,
                                isLoading = uiState.isDetailLoading,
                                viewModel = viewModel,
                                onBack = { viewModel.selectSession(null) },
                                onDelete = { showDeleteConfirmDialog = it },
                                onRequeueSignage = { viewModel.requeueSignage(it) },
                                onOpenImage = { viewModel.setLightboxUrl(it) },
                                onNavigateToRecordings = onNavigateToRecordings
                            )
                        } else {
                            ParkingSessionsListView(
                                uiState = uiState,
                                viewModel = viewModel,
                                onSessionClick = { viewModel.selectSession(it.sessionId) },
                                onFilterChange = { viewModel.setFilterRange(it) },
                                onEnableMaster = {
                                    viewModel.updateConfig(uiState.config.copy(enabled = true))
                                }
                            )
                        }
                    }
                    ParkingTab.SETTINGS -> {
                        ParkingSettingsView(
                            config = uiState.config,
                            geocodingEnabled = uiState.geocodingEnabled,
                            geocodingOnline = uiState.geocodingOnline,
                            onUpdateConfig = { viewModel.updateConfig(it) },
                            onUpdateGeocoding = { enabled, online ->
                                viewModel.updateGeocoding(enabled, online)
                            }
                        )
                    }
                }
            }
        }

        // Floating Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )

        // Delete Confirmation Dialog
        showDeleteConfirmDialog?.let { sessionId ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = null },
                title = {
                    Text(
                        stringResource(R.string.parking_delete_confirm_title),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    Text(
                        stringResource(R.string.parking_delete_confirm_msg),
                        color = Color(0xFFCBD5E1)
                    )
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        onClick = {
                            val id = showDeleteConfirmDialog ?: return@Button
                            showDeleteConfirmDialog = null
                            viewModel.deleteSession(id)
                        }
                    ) {
                        Text(stringResource(R.string.parking_delete_session_btn), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = null }) {
                        Text(stringResource(R.string.recordings_cancel_select_btn), color = Color(0xFF94A3B8))
                    }
                },
                containerColor = Color(0xFF1E293B)
            )
        }

        // Image Lightbox Dialog
        uiState.lightboxUrl?.let { url ->
            ParkingLightboxDialog(
                url = url,
                viewModel = viewModel,
                onDismiss = { viewModel.setLightboxUrl(null) }
            )
        }
    }
}

// ==================== TOP BAR ====================

@Composable
private fun ParkingTopBar(
    activeTab: ParkingTab,
    isRunning: Boolean,
    isLoading: Boolean,
    onTabSelected: (ParkingTab) -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title & Status
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF00D4AA).copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalParking,
                    contentDescription = null,
                    tint = Color(0xFF00D4AA),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.parking_page_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                if (isRunning) Color(0xFF10B981) else Color(0xFF64748B),
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isRunning) stringResource(R.string.status_on) else stringResource(R.string.status_off),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isRunning) Color(0xFF10B981) else Color(0xFF94A3B8)
                    )
                }
            }
        }

        // Tab Selector Pills & Refresh
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Row {
                    ParkingTabButton(
                        text = stringResource(R.string.parking_tab_sessions),
                        icon = Icons.Outlined.History,
                        selected = activeTab == ParkingTab.SESSIONS,
                        onClick = { onTabSelected(ParkingTab.SESSIONS) }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    ParkingTabButton(
                        text = stringResource(R.string.parking_tab_settings),
                        icon = Icons.Outlined.Settings,
                        selected = activeTab == ParkingTab.SETTINGS,
                        onClick = { onTabSelected(ParkingTab.SETTINGS) }
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(38.dp)
                    .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF00D4AA)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ParkingTabButton(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF00D4AA) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color(0xFF0F172A) else Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color(0xFF0F172A) else Color(0xFF94A3B8)
            )
        }
    }
}

// ==================== SESSIONS LIST VIEW ====================

@Composable
private fun ParkingSessionsListView(
    uiState: ParkingUiState,
    viewModel: ParkingViewModel,
    onSessionClick: (ParkingSessionModel) -> Unit,
    onFilterChange: (ParkingFilterRange) -> Unit,
    onEnableMaster: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Hero Card (if currently parked)
        uiState.currentSession?.let { current ->
            ParkedNowHeroCard(
                session = current,
                viewModel = viewModel,
                onClick = { onSessionClick(current) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Range Filter Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ParkingFilterRange.values().forEach { range ->
                val selected = uiState.filterRange == range
                val label = when (range) {
                    ParkingFilterRange.DAYS_7 -> stringResource(R.string.parking_filter_7_days)
                    ParkingFilterRange.DAYS_30 -> stringResource(R.string.parking_filter_30_days)
                    ParkingFilterRange.DAYS_90 -> stringResource(R.string.parking_filter_90_days)
                    ParkingFilterRange.ALL -> stringResource(R.string.parking_filter_all)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) Color(0xFF334155) else Color(0xFF1E293B))
                        .border(
                            1.dp,
                            if (selected) Color(0xFF00D4AA).copy(alpha = 0.5f) else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onFilterChange(range) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) Color(0xFF00D4AA) else Color(0xFF94A3B8)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Session Cards Grid or Empty State
        if (uiState.sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalParking,
                        contentDescription = null,
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.parking_no_sessions_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.parking_no_sessions),
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 320.dp)
                    )
                    if (!uiState.config.enabled) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = onEnableMaster,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D4AA))
                        ) {
                            Text(
                                stringResource(R.string.parking_enable_btn),
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            val sessions = uiState.sessions
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 175.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sessions, key = { it.sessionId }) { session ->
                    ParkingSessionCard(
                        session = session,
                        onClick = { onSessionClick(session) }
                    )
                }
            }
        }
    }
}

// ==================== PARKED NOW HERO CARD ====================

@Composable
private fun ParkedNowHeroCard(
    session: ParkingSessionModel,
    viewModel: ParkingViewModel,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF00D4AA).copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF00D4AA).copy(alpha = 0.15f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF00D4AA),
                            letterSpacing = 0.8.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.parking_now_parked),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = session.place.ifBlank { stringResource(R.string.live_vehicle_location) },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    session.level?.let { lvl ->
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "· $lvl",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00D4AA)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stat Summary
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    HeroStatItem(
                        value = formatDuration(session.durationMs),
                        label = stringResource(R.string.parking_stat_parked_for)
                    )
                    HeroStatItem(
                        value = session.eventCount.toString(),
                        label = stringResource(R.string.parking_stat_events)
                    )
                    HeroStatItem(
                        value = session.neighbourCount.toString(),
                        label = stringResource(R.string.parking_stat_neighbours)
                    )
                    session.kwh?.let { kwh ->
                        HeroStatItem(
                            value = "${if (kwh > 0) "+" else ""}${String.format(Locale.US, "%.2f", kwh)} kWh",
                            label = stringResource(R.string.parking_energy_used)
                        )
                    }
                }
            }

            // Thumbnail snapshot preview if arrived mosaic is ready
            var thumbBitmap by remember { mutableStateOf<Bitmap?>(null) }
            LaunchedEffect(session.sessionId) {
                if (session.arrivedOk) {
                    viewModel.loadBitmap("/parking/asset/${session.sessionId}/arrived_mosaic.jpg") { bmp ->
                        thumbBitmap = bmp
                    }
                }
            }

            thumbBitmap?.let { bmp ->
                Spacer(modifier = Modifier.width(16.dp))
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 120.dp, height = 75.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                )
            }
        }
    }
}

@Composable
private fun HeroStatItem(value: String, label: String) {
    Column {
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFF64748B)
        )
    }
}

// ==================== SESSION CARD ====================

@Composable
private fun ParkingSessionCard(
    session: ParkingSessionModel,
    onClick: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(
            1.dp,
            if (session.isOpen) Color(0xFF00D4AA).copy(alpha = 0.5f) else Color(0xFF334155)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Time + Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeFormat.format(Date(session.startedMs)),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateFormat.format(Date(session.startedMs)),
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                    if (session.isOpen) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF00D4AA).copy(alpha = 0.15f), CircleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "LIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00D4AA)
                            )
                        }
                    }
                }

                // Event Badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            when {
                                session.eventCount >= 5 -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                session.eventCount >= 2 -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                session.eventCount > 0 -> Color(0xFF00D4AA).copy(alpha = 0.15f)
                                else -> Color(0xFF334155)
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (session.eventCount == 0) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = session.eventCount.toString(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = when {
                                session.eventCount >= 5 -> Color(0xFFEF4444)
                                session.eventCount >= 2 -> Color(0xFFF59E0B)
                                else -> Color(0xFF00D4AA)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Location & Level
            Text(
                text = session.place.ifBlank { stringResource(R.string.parking_unknown_place) },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE2E8F0),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            session.level?.let { lvl ->
                Text(
                    text = "· $lvl",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00D4AA)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metadata Capsules
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CapsuleRow(
                    icon = Icons.Outlined.Schedule,
                    text = formatDuration(session.durationMs)
                )
                session.kwh?.let { kwh ->
                    CapsuleRow(
                        icon = Icons.Outlined.BatteryChargingFull,
                        text = "${if (kwh > 0) "+" else ""}${String.format(Locale.US, "%.2f", kwh)} kWh",
                        tint = if (kwh > 0) Color(0xFF10B981) else Color(0xFF94A3B8)
                    )
                }
                if (session.neighbourCount > 0) {
                    CapsuleRow(
                        icon = Icons.Outlined.DirectionsCar,
                        text = "${session.neighbourCount} ${stringResource(R.string.parking_stat_neighbours)}"
                    )
                }
                CapsuleRow(
                    icon = Icons.Outlined.Security,
                    text = session.sentryState.capitalize(Locale.ROOT)
                )
            }
        }
    }
}

@Composable
private fun CapsuleRow(
    icon: ImageVector,
    text: String,
    tint: Color = Color(0xFF94A3B8)
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            color = tint
        )
    }
}

// ==================== DETAIL VIEW ====================

@Composable
private fun ParkingDetailView(
    detailData: ParkingDetailData?,
    isLoading: Boolean,
    viewModel: ParkingViewModel,
    onBack: () -> Unit,
    onDelete: (String) -> Unit,
    onRequeueSignage: (String) -> Unit,
    onOpenImage: (String) -> Unit,
    onNavigateToRecordings: (String?) -> Unit
) {
    if (isLoading || detailData == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF00D4AA))
        }
        return
    }

    val session = detailData.session
    val context = LocalContext.current
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Detail Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint = Color(0xFFCBD5E1),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.parking_back_btn),
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                session.mapsUrl?.let { maps ->
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(maps))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Map,
                            contentDescription = null,
                            tint = Color(0xFF00D4AA),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.parking_map_btn),
                            color = Color(0xFFCBD5E1),
                            fontSize = 13.sp
                        )
                    }
                }

                Button(
                    onClick = { onNavigateToRecordings(session.sessionId) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Videocam,
                        contentDescription = null,
                        tint = Color(0xFF00D4AA),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.parking_recordings_btn),
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Detail Summary Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = session.place.ifBlank { stringResource(R.string.parking_unknown_place) } +
                            (session.level?.let { " · $it" } ?: ""),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(Date(session.startedMs)) + " · " +
                            timeFormat.format(Date(session.startedMs)) +
                            (session.endedMs?.let { " – " + timeFormat.format(Date(it)) } ?: " · " + stringResource(R.string.parking_now_parked)),
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Detail Stat Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DetailStatItem(
                        value = formatDuration(session.durationMs),
                        label = stringResource(R.string.parking_stat_parked_for)
                    )
                    session.kwh?.let { kwh ->
                        DetailStatItem(
                            value = "${if (kwh > 0) "+" else ""}${String.format(Locale.US, "%.2f", kwh)} kWh",
                            label = stringResource(R.string.parking_energy_used)
                        )
                    }
                    DetailStatItem(
                        value = session.eventCount.toString(),
                        label = stringResource(R.string.parking_stat_events)
                    )
                    DetailStatItem(
                        value = detailData.neighbours.size.toString(),
                        label = stringResource(R.string.parking_stat_neighbours)
                    )
                    DetailStatItem(
                        value = session.gpsQuality,
                        label = "GPS"
                    )
                    DetailStatItem(
                        value = session.sentryState.capitalize(Locale.ROOT),
                        label = "Sentry"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Stills Card (Arrived / Returned Mosaics & Quadrants)
        DetailStillsCard(
            session = session,
            assets = detailData.assets,
            viewModel = viewModel,
            onOpenImage = onOpenImage
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Garage Signage OCR Card
        DetailSignageCard(
            session = session,
            onRequeue = { onRequeueSignage(session.sessionId) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Neighbour Timeline Card
        DetailNeighboursCard(
            sessionId = session.sessionId,
            neighbours = detailData.neighbours,
            viewModel = viewModel,
            onOpenImage = onOpenImage
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Sentry Events Card
        DetailEventsCard(
            events = detailData.events,
            viewModel = viewModel,
            onEventClick = { onNavigateToRecordings(session.sessionId) }
        )

        // Delete Session Button (if closed)
        if (!session.isOpen) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { onDelete(session.sessionId) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.parking_delete_session_btn),
                    color = Color(0xFFEF4444),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun DetailStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF64748B)
        )
    }
}

// ==================== STILLS CARD ====================

@Composable
private fun DetailStillsCard(
    session: ParkingSessionModel,
    assets: Map<String, Map<String, String>>,
    viewModel: ParkingViewModel,
    onOpenImage: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = null,
                    tint = Color(0xFF00D4AA),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.parking_stills_title),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val arrivedAssets = assets["arrived"]
            val returnedAssets = assets["returned"]

            if (arrivedAssets == null && returnedAssets == null) {
                Text(
                    text = stringResource(R.string.parking_no_stills),
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    arrivedAssets?.let { arr ->
                        Box(modifier = Modifier.weight(1f)) {
                            StillGroup(
                                label = stringResource(R.string.parking_arrived_label),
                                timestamp = session.arrivedMs,
                                assetMap = arr,
                                viewModel = viewModel,
                                onOpenImage = onOpenImage
                            )
                        }
                    }
                    returnedAssets?.let { ret ->
                        Box(modifier = Modifier.weight(1f)) {
                            StillGroup(
                                label = stringResource(R.string.parking_returned_label),
                                timestamp = session.returnedMs,
                                assetMap = ret,
                                viewModel = viewModel,
                                onOpenImage = onOpenImage
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StillGroup(
    label: String,
    timestamp: Long?,
    assetMap: Map<String, String>,
    viewModel: ParkingViewModel,
    onOpenImage: (String) -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val mosaicPath = assetMap["mosaic"] ?: assetMap.values.firstOrNull() ?: ""

    var mainBmp by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(mosaicPath) {
        if (mosaicPath.isNotBlank()) {
            viewModel.loadBitmap(mosaicPath) { mainBmp = it }
        }
    }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0F172A))
                .clickable { onOpenImage(mosaicPath) }
        ) {
            mainBmp?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = label + (timestamp?.let { " · " + timeFormat.format(Date(it)) } ?: ""),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Quadrant Side Tiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val sides: List<Pair<String, Int>> = listOf(
                "front" to R.string.parking_side_front,
                "right" to R.string.parking_side_right,
                "rear" to R.string.parking_side_rear,
                "left" to R.string.parking_side_left
            )
            sides.forEach { (side, nameRes) ->
                val sideUrl = assetMap[side]
                var tileBmp by remember { mutableStateOf<Bitmap?>(null) }
                LaunchedEffect(sideUrl) {
                    sideUrl?.let { viewModel.loadBitmap(it) { bmp -> tileBmp = bmp } }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0F172A))
                        .clickable(enabled = sideUrl != null) { sideUrl?.let { onOpenImage(it) } },
                    contentAlignment = Alignment.Center
                ) {
                    tileBmp?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } ?: run {
                        Text(
                            text = stringResource(nameRes),
                            fontSize = 9.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}

// ==================== SIGNAGE OCR CARD ====================

@Composable
private fun DetailSignageCard(
    session: ParkingSessionModel,
    onRequeue: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Layers,
                        contentDescription = null,
                        tint = Color(0xFF00D4AA),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.parking_signage_title),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                session.signageLabel?.let { label ->
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF00D4AA).copy(alpha = 0.15f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00D4AA)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (session.signageLabel != null) {
                Text(
                    text = session.signageLabel,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF00D4AA)
                )
                session.signageConfidence?.let { conf ->
                    Text(
                        text = "%${(conf * 100).toInt()} güvenirlik",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                if (session.signageEvidence.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = session.signageEvidence.joinToString(", ") { "\"$it\"" },
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            } else {
                Text(
                    text = when (session.signageState) {
                        "pending" -> "Tabela okuması sıraya alındı…"
                        "skipped" -> "Okunabilecek uygun kare bulunamadı."
                        "done" -> "Kat / bölge metni tespit edilemedi."
                        else -> "Tabela tespiti beklemede."
                    },
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick = onRequeue,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color(0xFF00D4AA),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.parking_read_again_btn),
                    color = Color(0xFF00D4AA),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ==================== NEIGHBOURS CARD ====================

@Composable
private fun DetailNeighboursCard(
    sessionId: String,
    neighbours: List<ParkingNeighbourModel>,
    viewModel: ParkingViewModel,
    onOpenImage: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsCar,
                        contentDescription = null,
                        tint = Color(0xFF00D4AA),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.parking_neighbours_title),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (neighbours.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF00D4AA).copy(alpha = 0.15f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = neighbours.size.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00D4AA)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (neighbours.isEmpty()) {
                Text(
                    text = stringResource(R.string.parking_no_neighbours),
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    neighbours.forEach { n ->
                        NeighbourRowItem(
                            sessionId = sessionId,
                            neighbour = n,
                            viewModel = viewModel,
                            onOpenImage = onOpenImage
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NeighbourRowItem(
    sessionId: String,
    neighbour: ParkingNeighbourModel,
    viewModel: ParkingViewModel,
    onOpenImage: (String) -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${neighbour.sideName.capitalize(Locale.ROOT)}: ${neighbour.classGroup.capitalize(Locale.ROOT)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (!neighbour.confirmed) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(Onaysız)",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(
                            when (neighbour.status.lowercase()) {
                                "arrived" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                "departed" -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                else -> Color(0xFF334155)
                            },
                            CircleShape
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = neighbour.status.replace("_", " ").capitalize(Locale.ROOT),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when (neighbour.status.lowercase()) {
                            "arrived" -> Color(0xFF10B981)
                            "departed" -> Color(0xFFF59E0B)
                            else -> Color(0xFF94A3B8)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Timestamps
            val times = mutableListOf<String>()
            neighbour.arrivedMs?.let { times.add("Geliş: ${timeFormat.format(Date(it))}") }
            neighbour.departedMs?.let { times.add("Ayrılış: ${timeFormat.format(Date(it))}") }
            if (times.isEmpty() && neighbour.lastSeenMs != null) {
                times.add("Son görülme: ${timeFormat.format(Date(neighbour.lastSeenMs))}")
            }

            Text(
                text = times.joinToString(" · "),
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )

            // Crop frames row
            if (neighbour.frames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    neighbour.frames.forEach { frame ->
                        val frameUrl = "/parking/asset/$sessionId/${frame.name}"
                        var frameBmp by remember { mutableStateOf<Bitmap?>(null) }
                        LaunchedEffect(frameUrl) {
                            viewModel.loadBitmap(frameUrl) { bmp -> frameBmp = bmp }
                        }

                        Box(
                            modifier = Modifier
                                .size(width = 80.dp, height = 55.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E293B))
                                .clickable { onOpenImage(frameUrl) }
                        ) {
                            frameBmp?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== SENTRY EVENTS CARD ====================

@Composable
private fun DetailEventsCard(
    events: List<ParkingEventModel>,
    viewModel: ParkingViewModel,
    onEventClick: (ParkingEventModel) -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = Color(0xFF00D4AA),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.parking_events_title),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (events.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF00D4AA).copy(alpha = 0.15f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = events.size.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00D4AA)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (events.isEmpty()) {
                Text(
                    text = stringResource(R.string.parking_no_events),
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    events.forEach { ev ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                                .clickable { onEventClick(ev) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var evBmp by remember { mutableStateOf<Bitmap?>(null) }
                            val thumb = ev.heroThumbnailUrl ?: ev.thumbnailUrl
                            LaunchedEffect(thumb) {
                                thumb?.let { viewModel.loadBitmap(it) { bmp -> evBmp = bmp } }
                            }

                            Box(
                                modifier = Modifier
                                    .size(width = 68.dp, height = 48.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                evBmp?.let { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } ?: Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = timeFormat.format(Date(ev.timestamp)),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    ev.peakSeverity?.let { sev ->
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (sev == "CRITICAL") Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                                                    CircleShape
                                                )
                                                .padding(horizontal = 6.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = sev,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (sev == "CRITICAL") Color(0xFFEF4444) else Color(0xFFF59E0B)
                                            )
                                        }
                                    }
                                }

                                val actors = mutableListOf<String>()
                                if (ev.personCount > 0) actors.add("👤 ${ev.personCount}")
                                if (ev.vehicleCount > 0) actors.add("🚗 ${ev.vehicleCount}")
                                if (ev.bikeCount > 0) actors.add("🚲 ${ev.bikeCount}")
                                if (ev.animalCount > 0) actors.add("🐾 ${ev.animalCount}")

                                Text(
                                    text = actors.joinToString("  ") + (ev.cameras?.let { " · $it" } ?: ""),
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== SETTINGS VIEW ====================

@Composable
private fun ParkingSettingsView(
    config: ParkingConfigModel,
    geocodingEnabled: Boolean,
    geocodingOnline: Boolean,
    onUpdateConfig: (ParkingConfigModel) -> Unit,
    onUpdateGeocoding: (Boolean, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Master Switch Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, if (config.enabled) Color(0xFF00D4AA).copy(alpha = 0.4f) else Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.parking_settings_master_enable),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.parking_settings_master_desc),
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Switch(
                        checked = config.enabled,
                        onCheckedChange = { onUpdateConfig(config.copy(enabled = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF0F172A),
                            checkedTrackColor = Color(0xFF00D4AA)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sub Settings Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
            ) {
                // End Trigger
                Text(
                    text = stringResource(R.string.parking_settings_end_trigger),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.parking_settings_end_trigger_desc),
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "return" to R.string.parking_end_trigger_return,
                        "power_on" to R.string.parking_end_trigger_power_on,
                        "drive_away" to R.string.parking_end_trigger_drive_away
                    ).forEach { (trigger, nameRes) ->
                        val selected = config.endTrigger == trigger
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) Color(0xFF334155) else Color(0xFF0F172A))
                                .border(
                                    1.dp,
                                    if (selected) Color(0xFF00D4AA) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onUpdateConfig(config.copy(endTrigger = trigger)) }
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(nameRes),
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Color(0xFF00D4AA) else Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 14.dp))

                // Snapshots switch
                SettingToggleRow(
                    title = stringResource(R.string.parking_settings_snapshots),
                    desc = stringResource(R.string.parking_settings_snapshots_desc),
                    checked = config.snapshots,
                    onCheckedChange = { onUpdateConfig(config.copy(snapshots = it)) }
                )

                Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 14.dp))

                // Neighbours switch
                SettingToggleRow(
                    title = stringResource(R.string.parking_settings_neighbours),
                    desc = stringResource(R.string.parking_settings_neighbours_desc),
                    checked = config.neighbours,
                    onCheckedChange = { onUpdateConfig(config.copy(neighbours = it)) }
                )

                Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 14.dp))

                // Signage OCR switch
                SettingToggleRow(
                    title = stringResource(R.string.parking_settings_signage),
                    desc = stringResource(R.string.parking_settings_signage_desc),
                    checked = config.signage,
                    onCheckedChange = { onUpdateConfig(config.copy(signage = it)) }
                )

                Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 14.dp))

                // Geocoding switch
                SettingToggleRow(
                    title = stringResource(R.string.parking_settings_geocoding),
                    desc = stringResource(R.string.parking_settings_geocoding_desc),
                    checked = geocodingEnabled,
                    onCheckedChange = { onUpdateGeocoding(it, geocodingOnline) }
                )

                if (geocodingEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    SettingToggleRow(
                        title = stringResource(R.string.parking_settings_geocoding_online),
                        desc = stringResource(R.string.parking_settings_geocoding_online_desc),
                        checked = geocodingOnline,
                        onCheckedChange = { onUpdateGeocoding(geocodingEnabled, it) }
                    )
                }

                Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 14.dp))

                // Retention & Cap
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.parking_settings_retention),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "${config.retentionDays} gün",
                            fontSize = 12.sp,
                            color = Color(0xFF00D4AA)
                        )
                    }
                    Row {
                        listOf(30, 90, 180, 365).forEach { days ->
                            val isSel = config.retentionDays == days
                            Box(
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) Color(0xFF00D4AA) else Color(0xFF0F172A))
                                    .clickable { onUpdateConfig(config.copy(retentionDays = days)) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$days",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color(0xFF0F172A) else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF0F172A),
                checkedTrackColor = Color(0xFF00D4AA)
            )
        )
    }
}

// ==================== LIGHTBOX DIALOG ====================

@Composable
private fun ParkingLightboxDialog(
    url: String,
    viewModel: ParkingViewModel,
    onDismiss: () -> Unit
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(url) {
        viewModel.loadBitmap(url) { bitmap = it }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            bitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                )
            } ?: CircularProgressIndicator(color = Color(0xFF00D4AA))

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}

// ==================== UTILS ====================

private fun formatDuration(ms: Long): String {
    val totalMins = (ms / 60000L).coerceAtLeast(0L)
    val hours = totalMins / 60L
    val mins = totalMins % 60L
    val days = hours / 24L

    return when {
        days > 1 -> "${days}g ${hours % 24}s"
        hours > 0 -> "${hours}s ${if (mins < 10) "0" else ""}${mins}dk"
        else -> "${mins}dk"
    }
}
