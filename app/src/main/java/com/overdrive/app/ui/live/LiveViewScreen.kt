package com.overdrive.app.ui.live

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.overdrive.app.R
import com.overdrive.app.navmap.nav.MapNetworking
import com.overdrive.app.ui.component.OverdriveButton
import com.overdrive.app.ui.component.OverdriveButtonVariant
import com.overdrive.app.ui.component.OverdriveCard
import com.overdrive.app.ui.component.OverdriveDialog
import com.overdrive.app.ui.component.OverdrivePillStatus
import com.overdrive.app.ui.component.OverdriveStatusPill
import com.overdrive.app.ui.theme.OverdriveTheme
import com.overdrive.app.ui.vehicle.VehicleTopDownArt
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Point
import java.util.Locale

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
            // Standard OverDrive Two-Pane Layout
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
                            // Left Column: Live Camera Video Stage
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

                            // Right Column: Cameras Selector & Vehicle Location Map
                            Column(
                                modifier = Modifier
                                    .width(360.dp)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Camera Selector Card (Top-Down Car Visual with Hotspots)
                                CameraSelectorCard(
                                    activeCamera = state.activeCamera,
                                    hasOemDashcam = state.hasOemDashcam,
                                    vehicleModelId = state.vehicleModelId,
                                    vehicleModelName = state.vehicleModelName,
                                    onSelectCamera = { viewModel.selectCamera(it) }
                                )

                                // Vehicle Location Card (Mini MapLibre Preview)
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
                                vehicleModelId = state.vehicleModelId,
                                vehicleModelName = state.vehicleModelName,
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

        // Deterrent Confirmation Dialog (Horn / Flash)
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
            // Live Stream Video Viewport (No flash glitch)
            LiveCameraViewport(
                state = state,
                onSelectCamera = onSelectCamera,
                modifier = Modifier.fillMaxSize()
            )

            // Top Badges Overlay: Active Camera Label & Recording Dot
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

/**
 * Live Camera Viewport: renders the H.264/WebRTC stream without any visual flash glitch.
 * The WebView stays invisible until the utility rail hiding CSS is confirmed.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun LiveCameraViewport(
    state: LiveViewUiState,
    onSelectCamera: (LiveCameraMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isWebViewReady by remember { mutableStateOf(false) }

    val hideRailCssJs = remember {
        """
        (function() {
            var s = document.getElementById('od-hide-rail-style');
            if (!s) {
                s = document.createElement('style');
                s.id = 'od-hide-rail-style';
                s.innerHTML = '#liveUtilityRail, .camera-top-bar, #mobileHeader, #app-shell-mount, .deterrent-controls, #expandMapBtn, .location-expand-button, .location-map-frame { display: none !important; } .seamless-camera-view, .camera-stage, .video-display-area { height: 100% !important; width: 100% !important; padding: 0 !important; margin: 0 !important; }';
                document.head.appendChild(s);
            }
            var hideIds = ['mobileHeader', 'app-shell-mount', 'liveUtilityRail', 'deterrentControls', 'expandMapBtn'];
            hideIds.forEach(function(id) {
                var el = document.getElementById(id);
                if (el) el.style.setProperty('display', 'none', 'important');
            });
        })();
        """.trimIndent()
    }

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

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Native WebView Feed
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
                    visibility = android.view.View.INVISIBLE

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
                        override fun onPageCommitVisible(view: WebView?, url: String?) {
                            super.onPageCommitVisible(view, url)
                            view?.evaluateJavascript(hideRailCssJs, null)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            view?.evaluateJavascript(hideRailCssJs) {
                                view.visibility = android.view.View.VISIBLE
                                isWebViewReady = true
                                view.evaluateJavascript(
                                    "if (window.BYD && window.BYD.stream && window.BYD.stream.selectCamera) { window.BYD.stream.selectCamera(${state.activeCamera.id}); }",
                                    null
                                )
                            }
                        }
                    }
                    loadUrl(state.streamUrl)
                    webViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading indicator until video view is ready
        if (!isWebViewReady) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = OverdriveTheme.colors.statusSuccess,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.live_stream_status_connecting),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Camera Selector Card: Displays the user's selected vehicle top-down view
 * with interactive pulsing camera angle hotspots placed directly on/around the car.
 * (Redundant bottom button grid removed per user feedback).
 */
@Composable
private fun CameraSelectorCard(
    activeCamera: LiveCameraMode,
    hasOemDashcam: Boolean,
    vehicleModelId: String,
    vehicleModelName: String,
    onSelectCamera: (LiveCameraMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    OverdriveCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.live_selector_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Vehicle Model Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = vehicleModelName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Top-down Car Visual with Camera Hotspots
            CarHotspotDiagram(
                activeCamera = activeCamera,
                hasOemDashcam = hasOemDashcam,
                vehicleModelId = vehicleModelId,
                vehicleModelName = vehicleModelName,
                onSelectCamera = onSelectCamera,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(185.dp)
            )

            // Hint Text
            Text(
                text = stringResource(R.string.live_tap_camera_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Interactive Car Diagram with Top-Down Vehicle Silhouette & Pulsing Hotspots.
 */
@Composable
private fun CarHotspotDiagram(
    activeCamera: LiveCameraMode,
    hasOemDashcam: Boolean,
    vehicleModelId: String,
    vehicleModelName: String,
    onSelectCamera: (LiveCameraMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val topDownDrawableRes = remember(vehicleModelId) {
        VehicleTopDownArt.drawableFor(vehicleModelId)
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Vehicle Top-Down Silhouette
        Image(
            painter = painterResource(topDownDrawableRes),
            contentDescription = vehicleModelName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height(165.dp)
                .width(82.dp)
                .align(Alignment.Center)
        )

        // 1. FRONT (ÖN) — Bumper top center
        CameraHotspotBadge(
            shortLabel = "ÖN",
            isSelected = activeCamera == LiveCameraMode.FRONT,
            onClick = { onSelectCamera(LiveCameraMode.FRONT) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 0.dp)
        )

        // 2. DVR (OEM Dashcam) — Hood right-of-center
        if (hasOemDashcam) {
            CameraHotspotBadge(
                shortLabel = "DVR",
                isSelected = activeCamera == LiveCameraMode.DVR,
                onClick = { onSelectCamera(LiveCameraMode.DVR) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 34.dp, start = 32.dp)
            )
        }

        // 3. ALL (360°) — Roof center
        CameraHotspotBadge(
            shortLabel = "360°",
            isSelected = activeCamera == LiveCameraMode.ALL,
            onClick = { onSelectCamera(LiveCameraMode.ALL) },
            modifier = Modifier.align(Alignment.Center)
        )

        // 4. LEFT (SOL) — Left mirror side
        CameraHotspotBadge(
            shortLabel = "SOL",
            isSelected = activeCamera == LiveCameraMode.LEFT,
            onClick = { onSelectCamera(LiveCameraMode.LEFT) },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp)
        )

        // 5. RIGHT (SAĞ) — Right mirror side
        CameraHotspotBadge(
            shortLabel = "SAĞ",
            isSelected = activeCamera == LiveCameraMode.RIGHT,
            onClick = { onSelectCamera(LiveCameraMode.RIGHT) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
        )

        // 6. REAR (ARKA) — Rear bumper bottom center
        CameraHotspotBadge(
            shortLabel = "ARKA",
            isSelected = activeCamera == LiveCameraMode.REAR,
            onClick = { onSelectCamera(LiveCameraMode.REAR) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 0.dp)
        )
    }
}

/**
 * Camera Hotspot Badge with pulsating animated ring when selected.
 */
@Composable
private fun CameraHotspotBadge(
    shortLabel: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    // Pulsing halo animation for selected hotspot
    val infiniteTransition = rememberInfiniteTransition(label = "hotspotPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Pulsing halo when active
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = pulseAlpha
                    }
                    .clip(RoundedCornerShape(14.dp))
                    .background(primaryColor)
            )
        }

        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) primaryColor else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            border = androidx.compose.foundation.BorderStroke(
                width = if (isSelected) 2.dp else 1.5.dp,
                color = if (isSelected) Color.White else primaryColor.copy(alpha = 0.5f)
            ),
            shadowElevation = if (isSelected) 6.dp else 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
                Text(
                    text = shortLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Vehicle Location Card: Displays real-time vehicle GPS coordinates and
 * a compact native MapLibre vector map showing the vehicle's position.
 */
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
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Title + GPS Freshness
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
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
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

            // Compact Native MapLibre Map Preview
            LiveLocationMiniMap(
                latitude = state.vehicleLatitude,
                longitude = state.vehicleLongitude,
                heading = state.vehicleHeading,
                onOpenDirections = onOpenDirections,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            // Coordinates Display & Quick Directions Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.5f°, %.5f°", state.vehicleLatitude, state.vehicleLongitude),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                OverdriveButton(
                    text = stringResource(R.string.live_directions),
                    variant = OverdriveButtonVariant.PRIMARY,
                    onClick = onOpenDirections
                )
            }
        }
    }
}

private const val MINI_MAP_PUCK_SOURCE = "live-mini-puck-source"
private const val MINI_MAP_PUCK_HALO = "live-mini-puck-halo"
private const val MINI_MAP_PUCK_SYMBOL = "live-mini-puck-symbol"
private const val MINI_MAP_ARROW_IMG = "live-mini-arrow-img"

/**
 * Compact MapLibre vector map showing real road network basemap and vehicle location.
 */
@Composable
private fun LiveLocationMiniMap(
    latitude: Double,
    longitude: Double,
    heading: Float,
    onOpenDirections: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isDark = isSystemInDarkTheme()
    val brandTeal = MaterialTheme.colorScheme.primary.toArgb()

    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    var puckSourceRef by remember { mutableStateOf<GeoJsonSource?>(null) }

    val mapView = remember {
        try {
            MapLibre.getInstance(context)
            MapNetworking.installMapLibreHttpClient()
            MapView(context).apply {
                onCreate(null)
            }
        } catch (_: Throwable) {
            null
        }
    }

    // Forward Android lifecycle events to MapView
    if (mapView != null) {
        DisposableEffect(lifecycleOwner, mapView) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                mapView.onDestroy()
            }
        }
    }

    // Update puck geometry and camera on coordinates change
    LaunchedEffect(latitude, longitude, heading, mapInstance, puckSourceRef) {
        if (latitude != 0.0 && longitude != 0.0) {
            puckSourceRef?.setGeoJson(Point.fromLngLat(longitude, latitude))
            mapInstance?.easeCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 15.2),
                600
            )
        }
    }

    // Initialize MapLibre Style
    if (mapView != null) {
        LaunchedEffect(mapView, isDark) {
            mapView.getMapAsync { map ->
                mapInstance = map
                map.uiSettings.isAttributionEnabled = false
                map.uiSettings.isLogoEnabled = false
                map.uiSettings.isCompassEnabled = false

                val styleAsset = if (isDark) "maps/dark_style.json" else "maps/liberty_style.json"
                val json = try {
                    context.assets.open(styleAsset).bufferedReader(Charsets.UTF_8).use { it.readText() }
                } catch (_: Throwable) {
                    null
                }

                val styleBuilder = if (json != null) {
                    Style.Builder().fromJson(json)
                } else {
                    Style.Builder().fromUri(
                        if (isDark) "https://tiles.openfreemap.org/styles/dark"
                        else "https://tiles.openfreemap.org/styles/liberty"
                    )
                }

                map.setStyle(styleBuilder) { style ->
                    val arrowBmp = createMiniPuckArrow(brandTeal)
                    style.addImage(MINI_MAP_ARROW_IMG, arrowBmp)

                    val initialPoint = Point.fromLngLat(longitude, latitude)
                    val puckSource = GeoJsonSource(MINI_MAP_PUCK_SOURCE, initialPoint)
                    style.addSource(puckSource)
                    puckSourceRef = puckSource

                    // Outer Halo
                    style.addLayer(
                        CircleLayer(MINI_MAP_PUCK_HALO, MINI_MAP_PUCK_SOURCE).withProperties(
                            PropertyFactory.circleColor(brandTeal),
                            PropertyFactory.circleRadius(16f),
                            PropertyFactory.circleOpacity(0.25f),
                            PropertyFactory.circleStrokeColor(0xFFFFFFFF.toInt()),
                            PropertyFactory.circleStrokeWidth(1.5f),
                            PropertyFactory.circleStrokeOpacity(0.6f)
                        )
                    )

                    // Vehicle Heading Arrow
                    style.addLayer(
                        SymbolLayer(MINI_MAP_PUCK_SYMBOL, MINI_MAP_PUCK_SOURCE).withProperties(
                            PropertyFactory.iconImage(MINI_MAP_ARROW_IMG),
                            PropertyFactory.iconRotate(heading),
                            PropertyFactory.iconAllowOverlap(true),
                            PropertyFactory.iconIgnorePlacement(true)
                        )
                    )

                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 15.2))
                }
            }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (mapView != null) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Elegant Vector Fallback Grid
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = String.format(Locale.US, "%.5f°, %.5f°", latitude, longitude),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Overlay Action Buttons: Recenter on car & Directions
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                IconButton(
                    onClick = {
                        mapInstance?.easeCamera(
                            CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 15.2),
                            500
                        )
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = stringResource(R.string.live_my_location),
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                IconButton(
                    onClick = onOpenDirections,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription = stringResource(R.string.live_directions),
                        tint = OverdriveTheme.colors.statusSuccess,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Creates a navigation arrow bitmap pointing North for the mini map puck.
 */
private fun createMiniPuckArrow(accentColor: Int): Bitmap {
    val s = 56
    val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = s / 2f
    val cy = s / 2f
    val r = s * 0.40f

    fun arrowPath(scale: Float, dy: Float) = Path().apply {
        moveTo(cx, cy - r * 0.78f * scale + dy)
        lineTo(cx + r * 0.62f * scale, cy + r * 0.66f * scale + dy)
        lineTo(cx, cy + r * 0.30f * scale + dy)
        lineTo(cx - r * 0.62f * scale, cy + r * 0.66f * scale + dy)
        close()
    }

    val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.FILL
    }
    c.drawPath(arrowPath(1.22f, 0f), outline)

    val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = accentColor
    }
    c.drawPath(arrowPath(1.0f, 0f), body)
    return bmp
}
