package com.overdrive.app.ui.live

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.overdrive.app.R
import com.overdrive.app.ui.component.OverdriveButton
import com.overdrive.app.ui.component.OverdriveButtonVariant
import com.overdrive.app.ui.component.OverdriveCard
import com.overdrive.app.ui.component.OverdriveDialog
import com.overdrive.app.ui.component.OverdrivePillStatus
import com.overdrive.app.ui.component.OverdriveStatusPill
import com.overdrive.app.ui.theme.OverdriveTheme

@Composable
fun LiveViewScreen(
    viewModel: LiveViewViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (state.isFullscreen) {
            // Fullscreen camera stage
            Box(modifier = Modifier.fillMaxSize()) {
                LiveCameraViewport(
                    state = state,
                    onSelectCamera = { viewModel.selectCamera(it) },
                    modifier = Modifier.fillMaxSize()
                )

                // Exit Fullscreen Floating Button
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    IconButton(onClick = { viewModel.toggleFullscreen() }) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = stringResource(R.string.live_exit_fullscreen),
                            tint = Color.White
                        )
                    }
                }
            }
        } else {
            // Standard OverDrive Dashboard Two-Pane Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = OverdriveTheme.dimensions.pagePaddingHorizontal,
                        vertical = OverdriveTheme.dimensions.pagePaddingTop
                    )
            ) {
                // Header Bar
                LiveViewHeader(
                    state = state,
                    onSelectQuality = { viewModel.setQuality(it) },
                    onToggleCabin = { viewModel.toggleCabinListening() },
                    onToggleFullscreen = { viewModel.toggleFullscreen() }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Main Stage & Utility Rail
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isWide = maxWidth >= 840.dp

                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Left Column: Camera Video Stage
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                LiveCameraStageCard(
                                    state = state,
                                    onSelectCamera = { viewModel.selectCamera(it) },
                                    onRequestDeterrent = { viewModel.requestDeterrent(it) },
                                    onToggleFullscreen = { viewModel.toggleFullscreen() }
                                )
                            }

                            // Right Column: Utility Rail
                            Column(
                                modifier = Modifier
                                    .width(360.dp)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Camera Selector Card
                                CameraSelectorCard(
                                    activeCamera = state.activeCamera,
                                    hasOemDashcam = state.hasOemDashcam,
                                    onSelectCamera = { viewModel.selectCamera(it) }
                                )

                                // Vehicle Location Card
                                VehicleLocationCard(
                                    state = state,
                                    onOpenDirections = {
                                        val geoUri = Uri.parse("google.navigation:q=${state.vehicleLatitude},${state.vehicleLongitude}")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                                            context.startActivity(mapIntent)
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        // Narrow / Portrait Layout
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            LiveCameraStageCard(
                                state = state,
                                onSelectCamera = { viewModel.selectCamera(it) },
                                onRequestDeterrent = { viewModel.requestDeterrent(it) },
                                onToggleFullscreen = { viewModel.toggleFullscreen() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                            )

                            CameraSelectorCard(
                                activeCamera = state.activeCamera,
                                hasOemDashcam = state.hasOemDashcam,
                                onSelectCamera = { viewModel.selectCamera(it) }
                            )

                            VehicleLocationCard(
                                state = state,
                                onOpenDirections = {
                                    val geoUri = Uri.parse("google.navigation:q=${state.vehicleLatitude},${state.vehicleLongitude}")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                                    if (mapIntent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(mapIntent)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Deterrent Confirmation Dialog
        if (state.confirmDeterrentKind != null) {
            val isHorn = state.confirmDeterrentKind == "horn"
            OverdriveDialog(
                onDismissRequest = { viewModel.dismissDeterrentDialog() },
                title = stringResource(
                    if (isHorn) R.string.live_deterrent_horn_confirm_title
                    else R.string.live_deterrent_flash_confirm_title
                ),
                positiveButtonText = stringResource(R.string.live_deterrent_confirm_btn),
                onPositiveClick = { viewModel.confirmDeterrent() },
                negativeButtonText = stringResource(R.string.live_deterrent_cancel_btn),
                onNegativeClick = { viewModel.dismissDeterrentDialog() }
            ) {
                Text(
                    text = stringResource(
                        if (isHorn) R.string.live_deterrent_horn_confirm_desc
                        else R.string.live_deterrent_flash_confirm_desc
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LiveViewHeader(
    state: LiveViewUiState,
    onSelectQuality: (StreamQuality) -> Unit,
    onToggleCabin: () -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showQualityMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(R.string.live_view_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.live_view_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Connection Status Pill
            val (statusText, statusPill) = when (state.connectionState) {
                StreamConnectionState.LIVE -> stringResource(R.string.live_stream_status_live) to OverdrivePillStatus.SUCCESS
                StreamConnectionState.CONNECTING -> stringResource(R.string.live_stream_status_connecting) to OverdrivePillStatus.WARNING
                StreamConnectionState.IDLE -> stringResource(R.string.live_stream_status_idle) to OverdrivePillStatus.INFO
                StreamConnectionState.ERROR -> stringResource(R.string.live_stream_status_error) to OverdrivePillStatus.DANGER
            }
            OverdriveStatusPill(label = statusText, status = statusPill)

            // Quality Dropdown Menu
            Box {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { showQualityMenu = true }
                ) {
                    Text(
                        text = state.selectedQuality.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }

                DropdownMenu(
                    expanded = showQualityMenu,
                    onDismissRequest = { showQualityMenu = false }
                ) {
                    StreamQuality.entries.forEach { quality ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = quality.label,
                                    fontWeight = if (quality == state.selectedQuality) FontWeight.Bold else FontWeight.Normal,
                                    color = if (quality == state.selectedQuality) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                onSelectQuality(quality)
                                showQualityMenu = false
                            }
                        )
                    }
                }
            }

            // Cabin Audio Toggle
            IconButton(onClick = onToggleCabin) {
                Icon(
                    imageVector = if (state.isCabinListening) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = stringResource(R.string.live_cabin_listen),
                    tint = if (state.isCabinListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Fullscreen Toggle
            IconButton(onClick = onToggleFullscreen) {
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = stringResource(R.string.live_fullscreen),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LiveCameraStageCard(
    state: LiveViewUiState,
    onSelectCamera: (LiveCameraMode) -> Unit,
    onRequestDeterrent: (String) -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OverdriveCard(
        modifier = modifier.fillMaxSize(),
        backgroundColor = Color.Black,
        contentPadding = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Live Stream Video Viewport
            LiveCameraViewport(
                state = state,
                onSelectCamera = onSelectCamera,
                modifier = Modifier.fillMaxSize()
            )

            // Top Badges Overlay: Camera Label & Recording Dot
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Active Camera Label Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = OverdriveTheme.colors.statusSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(state.activeCamera.labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Recording Indicator Badge
                if (state.isRecording) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = stringResource(R.string.live_recording_active),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Bottom Deterrent Bar (Horn & Flash)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.7f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Horn Deterrent Button
                    Surface(
                        onClick = { onRequestDeterrent("horn") },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = OverdriveTheme.colors.statusWarning,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.live_deterrent_horn),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Flash Deterrent Button
                    Surface(
                        onClick = { onRequestDeterrent("flash") },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = OverdriveTheme.colors.statusInfo,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.live_deterrent_flash),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun LiveCameraViewport(
    state: LiveViewUiState,
    onSelectCamera: (LiveCameraMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(state.activeCamera) {
        webViewRef?.evaluateJavascript(
            "if (window.BYD && window.BYD.stream && window.BYD.stream.selectCamera) { window.BYD.stream.selectCamera(${state.activeCamera.id}); }",
            null
        )
    }

    LaunchedEffect(state.selectedQuality) {
        webViewRef?.evaluateJavascript(
            "if (window.BYD && window.BYD.stream && window.BYD.stream.setQuality) { window.BYD.stream.setQuality('${state.selectedQuality.key}'); }",
            null
        )
    }

    AndroidView(
        factory = { ctx ->
            object : WebView(ctx) {
                override fun onTouchEvent(event: MotionEvent?): Boolean = false
            }.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(android.graphics.Color.BLACK)

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    cacheMode = WebSettings.LOAD_NO_CACHE
                    loadWithOverviewMode = true
                    useWideViewPort = true
                }

                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Inject seamless viewport style: remove headers, sidebar, duplicate cards, deterrents
                        val js = """
                            (function() {
                                document.documentElement.classList.add('is-app-webview');
                                var hideIds = ['mobileHeader', 'app-shell-mount', 'liveUtilityRail', 'deterrentControls', 'expandMapBtn'];
                                hideIds.forEach(function(id) {
                                    var el = document.getElementById(id);
                                    if (el) el.style.setProperty('display', 'none', 'important');
                                });
                                var topBar = document.querySelector('.camera-top-bar');
                                if (topBar) topBar.style.setProperty('display', 'none', 'important');
                                var deterrents = document.querySelectorAll('.deterrent-controls, .deterrent-btn');
                                deterrents.forEach(function(el) {
                                    el.style.setProperty('display', 'none', 'important');
                                });
                                var seamless = document.querySelector('.seamless-camera-view');
                                if (seamless) {
                                    seamless.style.setProperty('padding', '0', 'important');
                                    seamless.style.setProperty('margin', '0', 'important');
                                    seamless.style.setProperty('height', '100%', 'important');
                                }
                                var stage = document.querySelector('.camera-stage');
                                if (stage) stage.style.setProperty('height', '100%', 'important');
                                var area = document.querySelector('.video-display-area');
                                if (area) area.style.setProperty('height', '100%', 'important');
                                if (window.BYD && window.BYD.stream && window.BYD.stream.selectCamera) {
                                    window.BYD.stream.selectCamera(${state.activeCamera.id});
                                }
                            })();
                        """.trimIndent()
                        view?.evaluateJavascript(js, null)
                    }
                }
                loadUrl("http://127.0.0.1:8080/live-view.html")
                webViewRef = this
            }
        },
        modifier = modifier
    )
}

@Composable
private fun CameraSelectorCard(
    activeCamera: LiveCameraMode,
    hasOemDashcam: Boolean,
    onSelectCamera: (LiveCameraMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    OverdriveCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OverdriveTheme.dimensions.cardPaddingStandard),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.live_selector_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Car Top-down Hotspot Visual
            CarHotspotDiagram(
                activeCamera = activeCamera,
                hasOemDashcam = hasOemDashcam,
                onSelectCamera = onSelectCamera,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )

            Text(
                text = stringResource(R.string.live_tap_camera_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Quick Camera Mode Chips Grid
            val row1 = listOf(LiveCameraMode.ALL, LiveCameraMode.FRONT, LiveCameraMode.RIGHT)
            val row2 = if (hasOemDashcam) {
                listOf(LiveCameraMode.REAR, LiveCameraMode.LEFT, LiveCameraMode.DVR)
            } else {
                listOf(LiveCameraMode.REAR, LiveCameraMode.LEFT)
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row1.forEach { cam ->
                        CameraGridItem(
                            cam = cam,
                            isSelected = cam == activeCamera,
                            onClick = { onSelectCamera(cam) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row2.forEach { cam ->
                        CameraGridItem(
                            cam = cam,
                            isSelected = cam == activeCamera,
                            onClick = { onSelectCamera(cam) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row2.size < 3) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraGridItem(
    cam: LiveCameraMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant
    )
    val contentColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = cam.shortCode,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

@Composable
private fun CarHotspotDiagram(
    activeCamera: LiveCameraMode,
    hasOemDashcam: Boolean,
    onSelectCamera: (LiveCameraMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val w = maxWidth
        val h = maxHeight

        // Draw Stylized Car Silhouette
        Canvas(modifier = Modifier.fillMaxSize()) {
            val carWidth = size.width * 0.32f
            val carHeight = size.height * 0.85f
            val left = (size.width - carWidth) / 2f
            val top = (size.height - carHeight) / 2f

            // Car body
            drawRoundRect(
                color = surfaceColor,
                topLeft = Offset(left, top),
                size = Size(carWidth, carHeight),
                cornerRadius = CornerRadius(28f, 28f)
            )
            drawRoundRect(
                color = outlineColor,
                topLeft = Offset(left, top),
                size = Size(carWidth, carHeight),
                cornerRadius = CornerRadius(28f, 28f),
                style = Stroke(width = 2.dp.toPx())
            )

            // Windshield outline
            val windshieldTop = top + carHeight * 0.22f
            val windshieldHeight = carHeight * 0.18f
            drawRoundRect(
                color = outlineColor.copy(alpha = 0.5f),
                topLeft = Offset(left + 8.dp.toPx(), windshieldTop),
                size = Size(carWidth - 16.dp.toPx(), windshieldHeight),
                cornerRadius = CornerRadius(12f, 12f)
            )

            // Rear window outline
            val rearWindowTop = top + carHeight * 0.62f
            drawRoundRect(
                color = outlineColor.copy(alpha = 0.5f),
                topLeft = Offset(left + 8.dp.toPx(), rearWindowTop),
                size = Size(carWidth - 16.dp.toPx(), windshieldHeight * 0.8f),
                cornerRadius = CornerRadius(12f, 12f)
            )
        }

        // Camera Hotspots positioned on the car
        // Front (Mode 1)
        CameraHotspotBadge(
            label = "ÖN",
            isSelected = activeCamera == LiveCameraMode.FRONT,
            onClick = { onSelectCamera(LiveCameraMode.FRONT) },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp)
        )

        // DVR (Mode 6)
        if (hasOemDashcam) {
            CameraHotspotBadge(
                label = "DVR",
                isSelected = activeCamera == LiveCameraMode.DVR,
                onClick = { onSelectCamera(LiveCameraMode.DVR) },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 36.dp)
            )
        }

        // Center 360° (Mode 0)
        CameraHotspotBadge(
            label = "360°",
            isSelected = activeCamera == LiveCameraMode.ALL,
            onClick = { onSelectCamera(LiveCameraMode.ALL) },
            modifier = Modifier.align(Alignment.Center)
        )

        // Left (Mode 4)
        CameraHotspotBadge(
            label = "SOL",
            isSelected = activeCamera == LiveCameraMode.LEFT,
            onClick = { onSelectCamera(LiveCameraMode.LEFT) },
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)
        )

        // Right (Mode 2)
        CameraHotspotBadge(
            label = "SAĞ",
            isSelected = activeCamera == LiveCameraMode.RIGHT,
            onClick = { onSelectCamera(LiveCameraMode.RIGHT) },
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)
        )

        // Rear (Mode 3)
        CameraHotspotBadge(
            label = "ARKA",
            isSelected = activeCamera == LiveCameraMode.REAR,
            onClick = { onSelectCamera(LiveCameraMode.REAR) },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
        )
    }
}

@Composable
private fun CameraHotspotBadge(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val containerColor = if (isSelected) primaryColor else MaterialTheme.colorScheme.surface
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isSelected) Color.White.copy(alpha = 0.8f) else primaryColor.copy(alpha = 0.5f)
        ),
        shadowElevation = if (isSelected) 4.dp else 1.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun VehicleLocationCard(
    state: LiveViewUiState,
    onOpenDirections: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OverdriveCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OverdriveTheme.dimensions.cardPaddingStandard),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.live_vehicle_location),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // GPS Freshness Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(OverdriveTheme.colors.statusSuccess)
                    )
                    Text(
                        text = state.lastGpsUpdateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // GPS Coordinates Display
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format("%.5f, %.5f", state.vehicleLatitude, state.vehicleLongitude),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "GPS FIX",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = OverdriveTheme.colors.statusSuccess
                    )
                }
            }

            // Directions Action Button
            OverdriveButton(
                text = stringResource(R.string.live_directions),
                variant = OverdriveButtonVariant.OUTLINED,
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenDirections
            )
        }
    }
}
