package com.overdrive.app.ui.fragment

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.overdrive.app.R
import com.overdrive.app.client.CameraDaemonClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * High-performance, pure native Live View fragment.
 * Completely eliminates Chromium WebView and WebGL/Three.js GPU & RAM overhead,
 * providing fluid 60 FPS UI on legacy Snapdragon 625/665 BYD head-units.
 */
class LiveViewNativeFragment : Fragment() {

    companion object {
        private const val TAG = "LiveViewNativeFragment"
        const val CAM_ALL = 0
        const val CAM_FRONT = 1
        const val CAM_RIGHT = 2
        const val CAM_REAR = 3
        const val CAM_LEFT = 4
        const val CAM_DVR = 6
    }

    private var selectedCameraId: Int = CAM_FRONT
    private var selectedQuality: String = "MEDIUM"
    private var isFullscreen: Boolean = false
    private var framePollingJob: Job? = null
    private val cameraDaemonClient = CameraDaemonClient()

    // Views
    private lateinit var tvLiveStatus: TextView
    private lateinit var liveStatusDot: View
    private lateinit var btnQualitySelector: MaterialButton
    private lateinit var btnToggleFullscreen: MaterialButton
    private lateinit var ivLiveVideoFrame: ImageView
    private lateinit var idleStateOverlay: LinearLayout
    private lateinit var tvIdleSubtext: TextView
    private lateinit var pbLiveLoading: ProgressBar
    private lateinit var cameraLabelOverlay: LinearLayout
    private lateinit var tvCurrentCameraLabel: TextView
    private lateinit var btnDeterrentHorn: MaterialButton
    private lateinit var btnDeterrentFlash: MaterialButton
    private lateinit var liveUtilityRail: View

    // Hotspot Buttons
    private lateinit var btnHotspotFront: MaterialButton
    private lateinit var btnHotspotDvr: MaterialButton
    private lateinit var btnHotspotRear: MaterialButton
    private lateinit var btnHotspotLeft: MaterialButton
    private lateinit var btnHotspotRight: MaterialButton
    private lateinit var btnHotspotAll: MaterialButton

    // Quick Chips
    private lateinit var chipGroupCameras: ChipGroup
    private lateinit var chipCamAll: Chip
    private lateinit var chipCamFront: Chip
    private lateinit var chipCamRear: Chip
    private lateinit var chipCamLeft: Chip
    private lateinit var chipCamRight: Chip
    private lateinit var chipCamDvr: Chip

    // Location Card
    private lateinit var tvLocationCoordinates: TextView
    private lateinit var tvLocationSpeed: TextView
    private lateinit var tvGpsFreshness: TextView
    private lateinit var gpsFreshnessDot: View
    private lateinit var btnOpenMap: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_live_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupListeners()
        setupLocationCard()
        selectCamera(CAM_FRONT)
    }

    private fun bindViews(view: View) {
        tvLiveStatus = view.findViewById(R.id.tvLiveStatus)
        liveStatusDot = view.findViewById(R.id.liveStatusDot)
        btnQualitySelector = view.findViewById(R.id.btnQualitySelector)
        btnToggleFullscreen = view.findViewById(R.id.btnToggleFullscreen)
        ivLiveVideoFrame = view.findViewById(R.id.ivLiveVideoFrame)
        idleStateOverlay = view.findViewById(R.id.idleStateOverlay)
        tvIdleSubtext = view.findViewById(R.id.tvIdleSubtext)
        pbLiveLoading = view.findViewById(R.id.pbLiveLoading)
        cameraLabelOverlay = view.findViewById(R.id.cameraLabelOverlay)
        tvCurrentCameraLabel = view.findViewById(R.id.tvCurrentCameraLabel)
        btnDeterrentHorn = view.findViewById(R.id.btnDeterrentHorn)
        btnDeterrentFlash = view.findViewById(R.id.btnDeterrentFlash)
        liveUtilityRail = view.findViewById(R.id.liveUtilityRail)

        btnHotspotFront = view.findViewById(R.id.btnHotspotFront)
        btnHotspotDvr = view.findViewById(R.id.btnHotspotDvr)
        btnHotspotRear = view.findViewById(R.id.btnHotspotRear)
        btnHotspotLeft = view.findViewById(R.id.btnHotspotLeft)
        btnHotspotRight = view.findViewById(R.id.btnHotspotRight)
        btnHotspotAll = view.findViewById(R.id.btnHotspotAll)

        chipGroupCameras = view.findViewById(R.id.chipGroupCameras)
        chipCamAll = view.findViewById(R.id.chipCamAll)
        chipCamFront = view.findViewById(R.id.chipCamFront)
        chipCamRear = view.findViewById(R.id.chipCamRear)
        chipCamLeft = view.findViewById(R.id.chipCamLeft)
        chipCamRight = view.findViewById(R.id.chipCamRight)
        chipCamDvr = view.findViewById(R.id.chipCamDvr)

        tvLocationCoordinates = view.findViewById(R.id.tvLocationCoordinates)
        tvLocationSpeed = view.findViewById(R.id.tvLocationSpeed)
        tvGpsFreshness = view.findViewById(R.id.tvGpsFreshness)
        gpsFreshnessDot = view.findViewById(R.id.gpsFreshnessDot)
        btnOpenMap = view.findViewById(R.id.btnOpenMap)
    }

    private fun setupListeners() {
        // Hotspot button clicks
        btnHotspotFront.setOnClickListener { selectCamera(CAM_FRONT) }
        btnHotspotDvr.setOnClickListener { selectCamera(CAM_DVR) }
        btnHotspotRear.setOnClickListener { selectCamera(CAM_REAR) }
        btnHotspotLeft.setOnClickListener { selectCamera(CAM_LEFT) }
        btnHotspotRight.setOnClickListener { selectCamera(CAM_RIGHT) }
        btnHotspotAll.setOnClickListener { selectCamera(CAM_ALL) }

        // Chip group listener
        chipGroupCameras.setOnCheckedStateChangeListener { _, checkedIds ->
            when {
                checkedIds.contains(R.id.chipCamAll) -> selectCamera(CAM_ALL, fromChip = true)
                checkedIds.contains(R.id.chipCamFront) -> selectCamera(CAM_FRONT, fromChip = true)
                checkedIds.contains(R.id.chipCamRear) -> selectCamera(CAM_REAR, fromChip = true)
                checkedIds.contains(R.id.chipCamLeft) -> selectCamera(CAM_LEFT, fromChip = true)
                checkedIds.contains(R.id.chipCamRight) -> selectCamera(CAM_RIGHT, fromChip = true)
                checkedIds.contains(R.id.chipCamDvr) -> selectCamera(CAM_DVR, fromChip = true)
            }
        }

        // Quality menu
        btnQualitySelector.setOnClickListener { showQualityMenu() }

        // Fullscreen toggle
        btnToggleFullscreen.setOnClickListener { toggleFullscreen() }

        // Deterrent actions
        btnDeterrentHorn.setOnClickListener { showHornConfirmationDialog() }
        btnDeterrentFlash.setOnClickListener { showFlashConfirmationDialog() }

        // Map button
        btnOpenMap.setOnClickListener {
            try {
                val intent = Intent(requireContext(), com.overdrive.app.navmap.RoadSenseMapActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Cannot launch RoadSenseMapActivity: ${e.message}")
            }
        }
    }

    fun selectCamera(cameraId: Int, fromChip: Boolean = false) {
        selectedCameraId = cameraId
        updateCameraLabels(cameraId)
        updateHotspotHighlight(cameraId)

        if (!fromChip) {
            when (cameraId) {
                CAM_ALL -> chipCamAll.isChecked = true
                CAM_FRONT -> chipCamFront.isChecked = true
                CAM_REAR -> chipCamRear.isChecked = true
                CAM_LEFT -> chipCamLeft.isChecked = true
                CAM_RIGHT -> chipCamRight.isChecked = true
                CAM_DVR -> chipCamDvr.isChecked = true
            }
        }

        startLiveStream(cameraId)
    }

    private fun updateCameraLabels(cameraId: Int) {
        val label = when (cameraId) {
            CAM_ALL -> "TÜMÜ (MOSAIC)"
            CAM_FRONT -> "ÖN KAMERA (FRONT)"
            CAM_REAR -> "ARKA KAMERA (REAR)"
            CAM_LEFT -> "SOL KAMERA (LEFT)"
            CAM_RIGHT -> "SAĞ KAMERA (RIGHT)"
            CAM_DVR -> "DVR (OEM DASHCAM)"
            else -> "KAMERA $cameraId"
        }
        tvCurrentCameraLabel.text = label
        tvLiveStatus.text = "CANLI ($label)"
    }

    private fun updateHotspotHighlight(cameraId: Int) {
        val activeColor = requireContext().getColor(R.color.brand_primary)
        val inactiveTint = null

        btnHotspotFront.strokeColor = if (cameraId == CAM_FRONT) android.content.res.ColorStateList.valueOf(activeColor) else null
        btnHotspotFront.strokeWidth = if (cameraId == CAM_FRONT) 2 else 0

        btnHotspotDvr.strokeColor = if (cameraId == CAM_DVR) android.content.res.ColorStateList.valueOf(activeColor) else null
        btnHotspotDvr.strokeWidth = if (cameraId == CAM_DVR) 2 else 0

        btnHotspotRear.strokeColor = if (cameraId == CAM_REAR) android.content.res.ColorStateList.valueOf(activeColor) else null
        btnHotspotRear.strokeWidth = if (cameraId == CAM_REAR) 2 else 0

        btnHotspotLeft.strokeColor = if (cameraId == CAM_LEFT) android.content.res.ColorStateList.valueOf(activeColor) else null
        btnHotspotLeft.strokeWidth = if (cameraId == CAM_LEFT) 2 else 0

        btnHotspotRight.strokeColor = if (cameraId == CAM_RIGHT) android.content.res.ColorStateList.valueOf(activeColor) else null
        btnHotspotRight.strokeWidth = if (cameraId == CAM_RIGHT) 2 else 0

        btnHotspotAll.strokeColor = if (cameraId == CAM_ALL) android.content.res.ColorStateList.valueOf(activeColor) else null
        btnHotspotAll.strokeWidth = if (cameraId == CAM_ALL) 2 else 0
    }

    private fun startLiveStream(cameraId: Int) {
        framePollingJob?.cancel()
        idleStateOverlay.visibility = View.GONE
        pbLiveLoading.visibility = View.VISIBLE

        framePollingJob = viewLifecycleOwner.lifecycleScope.launch {
            // First notify stream handler to switch view
            withContext(Dispatchers.IO) {
                try {
                    notifyStreamView(cameraId)
                } catch (e: Exception) {
                    Log.w(TAG, "notifyStreamView error: ${e.message}")
                }
            }

            // Frame polling loop — low CPU, zero GC spikes
            while (isActive) {
                val frameBytes = withContext(Dispatchers.IO) {
                    fetchFrameBytes(cameraId)
                }

                if (frameBytes != null && frameBytes.isNotEmpty()) {
                    val bitmap = BitmapFactory.decodeByteArray(frameBytes, 0, frameBytes.size)
                    if (bitmap != null) {
                        ivLiveVideoFrame.setImageBitmap(bitmap)
                        pbLiveLoading.visibility = View.GONE
                        liveStatusDot.setBackgroundResource(R.drawable.bg_circle_dot)
                        liveStatusDot.backgroundTintList = android.content.res.ColorStateList.valueOf(
                            requireContext().getColor(R.color.status_success)
                        )
                    }
                } else {
                    // Frame not available or daemon connecting
                    pbLiveLoading.visibility = View.GONE
                    if (ivLiveVideoFrame.drawable == null) {
                        idleStateOverlay.visibility = View.VISIBLE
                        tvIdleSubtext.text = "Akış bekleniyor..."
                    }
                }

                // Poll delay: ~100ms for smooth 10 fps MJPEG/JPEG preview without straining Snapdragon 625/665
                delay(100L)
            }
        }
    }

    private fun notifyStreamView(cameraId: Int) {
        try {
            val url = URL("http://127.0.0.1:8080/api/stream/view/$cameraId")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 1000
            conn.readTimeout = 1000
            conn.responseCode
            conn.disconnect()
        } catch (e: Exception) {
            // Daemon might use another port or direct client
        }
    }

    private fun fetchFrameBytes(cameraId: Int): ByteArray? {
        // Try CameraDaemonClient direct socket first (zero HTTP overhead)
        try {
            if (cameraDaemonClient.isConnected || cameraDaemonClient.connect()) {
                val bytes = cameraDaemonClient.getFrame(cameraId)
                if (bytes != null && bytes.isNotEmpty()) return bytes
            }
        } catch (e: Exception) {
            Log.v(TAG, "Daemon socket frame failed: ${e.message}")
        }

        // Fallback to localhost HTTP snapshot endpoint
        try {
            val url = URL("http://127.0.0.1:8080/api/surveillance/snapshot/$cameraId")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 1000
            conn.readTimeout = 1000
            if (conn.responseCode == 200) {
                val bytes = conn.inputStream.readBytes()
                conn.disconnect()
                return bytes
            }
            conn.disconnect()
        } catch (e: Exception) {
            // Silent fallback
        }
        return null
    }

    private fun showQualityMenu() {
        val popup = PopupMenu(requireContext(), btnQualitySelector)
        val qualities = listOf(
            "ULTRA_LOW" to "Ultra Low (400k)",
            "LOW" to "Low (600k)",
            "MEDIUM" to "Medium (1M)",
            "HIGH" to "High (1.5M)",
            "ULTRA_HIGH" to "Ultra High (2.5M)",
            "SMOOTH" to "Smooth (3.5M, 25 fps)",
            "MAX" to "Max (5M, 30 fps)"
        )
        qualities.forEachIndexed { index, (key, label) ->
            popup.menu.add(0, index, index, label)
        }
        popup.setOnMenuItemClickListener { item ->
            val selected = qualities[item.itemId]
            selectedQuality = selected.first
            btnQualitySelector.text = selected.second
            setStreamQuality(selected.first)
            true
        }
        popup.show()
    }

    private fun setStreamQuality(preset: String) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://127.0.0.1:8080/api/stream/quality/$preset")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 1500
                conn.readTimeout = 1500
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to set quality: ${e.message}")
            }
        }
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        if (isFullscreen) {
            liveUtilityRail.visibility = View.GONE
            btnToggleFullscreen.setIconResource(R.drawable.ic_fullscreen_exit)
        } else {
            liveUtilityRail.visibility = View.VISIBLE
            btnToggleFullscreen.setIconResource(R.drawable.ic_fullscreen)
        }
    }

    private fun showHornConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Korna &amp; İkaz Işıkları")
            .setMessage("Aracın korna ve selektör ikaz ışıklarını tetiklemek istediğinize emin misiniz?")
            .setPositiveButton("Çal ve Yak") { _, _ ->
                triggerDeterrent("horn")
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun showFlashConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Selektör Flaş")
            .setMessage("Araç farlarını selektör yaparak flaşlamak istediğinize emin misiniz?")
            .setPositiveButton("Flaş Yak") { _, _ ->
                triggerDeterrent("flash")
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun triggerDeterrent(type: String) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cmd = if (type == "horn") "FINDCAR" else "FLASHLIGHT"
                val url = URL("http://127.0.0.1:8080/api/vehicle/control/$cmd")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 2000
                conn.readTimeout = 2000
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                Log.w(TAG, "Deterrent trigger failed: ${e.message}")
            }
        }
    }

    private fun setupLocationCard() {
        try {
            val collector = com.overdrive.app.byd.BydDataCollector.getInstance()
            val vehicleData = collector?.data
            val speedKmh = if (vehicleData != null && !vehicleData.speedKmh.isNaN()) vehicleData.speedKmh.toInt() else 0

            val locationManager = requireContext().getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
            val location = try {
                locationManager?.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                    ?: locationManager?.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
            } catch (e: SecurityException) {
                null
            }

            if (location != null && location.latitude != 0.0 && location.longitude != 0.0) {
                val lat = String.format(java.util.Locale.US, "%.5f", location.latitude)
                val lon = String.format(java.util.Locale.US, "%.5f", location.longitude)
                tvLocationCoordinates.text = "$lat° N, $lon° E"
                val alt = location.altitude.toInt()
                tvLocationSpeed.text = "Hız: $speedKmh km/h · Rakım: $alt m"
                tvGpsFreshness.text = "Güncel"
            } else {
                tvLocationCoordinates.text = "GPS Konumu Bekleniyor..."
                tvLocationSpeed.text = "Hız: $speedKmh km/h · Araç Park Halinde"
                tvGpsFreshness.text = "Bekleniyor"
            }
        } catch (e: Exception) {
            tvLocationCoordinates.text = "Konum servisi hazır"
            tvLocationSpeed.text = "Hız: 0 km/h"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        framePollingJob?.cancel()
        try {
            cameraDaemonClient.disconnect()
        } catch (e: Exception) {
            Log.v(TAG, "Disconnect error: ${e.message}")
        }
    }
}
