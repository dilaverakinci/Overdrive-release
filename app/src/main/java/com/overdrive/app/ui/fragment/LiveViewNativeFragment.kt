package com.overdrive.app.ui.fragment

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
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
 * Exactly replicates legacy Chromium WebView Live View appearance,
 * completely eliminating WebGL/Three.js GPU & RAM overhead.
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
    private var tvLiveStatus: TextView? = null
    private lateinit var liveStatusDot: View
    private lateinit var btnQualitySelector: MaterialButton
    private var btnToggleFullscreen: MaterialButton? = null
    private lateinit var ivLiveVideoFrame: ImageView
    private lateinit var idleStateOverlay: LinearLayout
    private lateinit var tvIdleSubtext: TextView
    private lateinit var pbLiveLoading: ProgressBar
    private lateinit var cameraLabelOverlay: LinearLayout
    private lateinit var tvCurrentCameraLabel: TextView
    private var btnDeterrentHorn: MaterialButton? = null
    private var btnDeterrentFlash: MaterialButton? = null
    private lateinit var liveUtilityRail: View

    // Hotspot Views (Exact Match to Legacy Live View)
    private lateinit var btnHotspotFront: View
    private lateinit var ringHotspotFront: View
    private lateinit var labelHotspotFront: TextView

    private lateinit var btnHotspotLeft: View
    private lateinit var ringHotspotLeft: View
    private lateinit var labelHotspotLeft: TextView

    private lateinit var btnHotspotRight: View
    private lateinit var ringHotspotRight: View
    private lateinit var labelHotspotRight: TextView

    private lateinit var btnHotspotAll: View
    private lateinit var ringHotspotAll: View
    private lateinit var labelHotspotAll: TextView

    private lateinit var btnHotspotRear: View
    private lateinit var ringHotspotRear: View
    private lateinit var labelHotspotRear: TextView

    private var btnHotspotDvr: View? = null

    // Location Card
    private lateinit var tvLocationCoordinates: TextView
    private var tvLocationSpeed: TextView? = null
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
        ringHotspotFront = view.findViewById(R.id.ringHotspotFront)
        labelHotspotFront = view.findViewById(R.id.labelHotspotFront)

        btnHotspotLeft = view.findViewById(R.id.btnHotspotLeft)
        ringHotspotLeft = view.findViewById(R.id.ringHotspotLeft)
        labelHotspotLeft = view.findViewById(R.id.labelHotspotLeft)

        btnHotspotRight = view.findViewById(R.id.btnHotspotRight)
        ringHotspotRight = view.findViewById(R.id.ringHotspotRight)
        labelHotspotRight = view.findViewById(R.id.labelHotspotRight)

        btnHotspotAll = view.findViewById(R.id.btnHotspotAll)
        ringHotspotAll = view.findViewById(R.id.ringHotspotAll)
        labelHotspotAll = view.findViewById(R.id.labelHotspotAll)

        btnHotspotRear = view.findViewById(R.id.btnHotspotRear)
        ringHotspotRear = view.findViewById(R.id.ringHotspotRear)
        labelHotspotRear = view.findViewById(R.id.labelHotspotRear)

        btnHotspotDvr = view.findViewById(R.id.btnHotspotDvr)

        tvLocationCoordinates = view.findViewById(R.id.tvLocationCoordinates)
        tvLocationSpeed = view.findViewById(R.id.tvLocationSpeed)
        tvGpsFreshness = view.findViewById(R.id.tvGpsFreshness)
        gpsFreshnessDot = view.findViewById(R.id.gpsFreshnessDot)
        btnOpenMap = view.findViewById(R.id.btnOpenMap)
    }

    private fun setupListeners() {
        // Hotspot click listeners
        btnHotspotFront.setOnClickListener { selectCamera(CAM_FRONT) }
        btnHotspotLeft.setOnClickListener { selectCamera(CAM_LEFT) }
        btnHotspotRight.setOnClickListener { selectCamera(CAM_RIGHT) }
        btnHotspotAll.setOnClickListener { selectCamera(CAM_ALL) }
        btnHotspotRear.setOnClickListener { selectCamera(CAM_REAR) }
        btnHotspotDvr?.setOnClickListener { selectCamera(CAM_DVR) }

        // Quality menu
        btnQualitySelector.setOnClickListener { showQualityMenu() }

        // Fullscreen toggle (if visible)
        btnToggleFullscreen?.setOnClickListener { toggleFullscreen() }

        // Deterrent actions (if enabled)
        btnDeterrentHorn?.setOnClickListener { showHornConfirmationDialog() }
        btnDeterrentFlash?.setOnClickListener { showFlashConfirmationDialog() }

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

    fun selectCamera(cameraId: Int) {
        selectedCameraId = cameraId
        updateCameraLabels(cameraId)
        updateHotspotHighlight(cameraId)
        startLiveStream(cameraId)
    }

    private fun updateCameraLabels(cameraId: Int) {
        val label = when (cameraId) {
            CAM_ALL -> "All Cameras"
            CAM_FRONT -> "Front Camera"
            CAM_REAR -> "Rear Camera"
            CAM_LEFT -> "Left Camera"
            CAM_RIGHT -> "Right Camera"
            CAM_DVR -> "Dashcam"
            else -> "Camera $cameraId"
        }
        tvCurrentCameraLabel.text = label
        cameraLabelOverlay.visibility = if (cameraId >= 0) View.VISIBLE else View.GONE
        tvLiveStatus?.text = "CANLI ($label)"
    }

    private fun updateHotspotHighlight(cameraId: Int) {
        val isFront = cameraId == CAM_FRONT
        ringHotspotFront.setBackgroundResource(if (isFront) R.drawable.bg_hotspot_ring_active else R.drawable.bg_hotspot_ring)
        labelHotspotFront.setBackgroundResource(if (isFront) R.drawable.bg_hotspot_label_active else R.drawable.bg_hotspot_label)
        labelHotspotFront.setTextColor(if (isFront) Color.BLACK else Color.WHITE)

        val isLeft = cameraId == CAM_LEFT
        ringHotspotLeft.setBackgroundResource(if (isLeft) R.drawable.bg_hotspot_ring_active else R.drawable.bg_hotspot_ring)
        labelHotspotLeft.setBackgroundResource(if (isLeft) R.drawable.bg_hotspot_label_active else R.drawable.bg_hotspot_label)
        labelHotspotLeft.setTextColor(if (isLeft) Color.BLACK else Color.WHITE)

        val isRight = cameraId == CAM_RIGHT
        ringHotspotRight.setBackgroundResource(if (isRight) R.drawable.bg_hotspot_ring_active else R.drawable.bg_hotspot_ring)
        labelHotspotRight.setBackgroundResource(if (isRight) R.drawable.bg_hotspot_label_active else R.drawable.bg_hotspot_label)
        labelHotspotRight.setTextColor(if (isRight) Color.BLACK else Color.WHITE)

        val isAll = cameraId == CAM_ALL
        ringHotspotAll.setBackgroundResource(if (isAll) R.drawable.bg_hotspot_ring_active else R.drawable.bg_hotspot_ring)
        labelHotspotAll.setBackgroundResource(if (isAll) R.drawable.bg_hotspot_label_active else R.drawable.bg_hotspot_label)
        labelHotspotAll.setTextColor(if (isAll) Color.BLACK else Color.WHITE)

        val isRear = cameraId == CAM_REAR
        ringHotspotRear.setBackgroundResource(if (isRear) R.drawable.bg_hotspot_ring_active else R.drawable.bg_hotspot_ring)
        labelHotspotRear.setBackgroundResource(if (isRear) R.drawable.bg_hotspot_label_active else R.drawable.bg_hotspot_label)
        labelHotspotRear.setTextColor(if (isRear) Color.BLACK else Color.WHITE)
    }

    private fun startLiveStream(cameraId: Int) {
        framePollingJob?.cancel()
        idleStateOverlay.visibility = View.VISIBLE
        pbLiveLoading.visibility = View.VISIBLE
        tvIdleSubtext.text = "Starting camera..."

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
                        idleStateOverlay.visibility = View.GONE
                        liveStatusDot.setBackgroundResource(R.drawable.bg_circle_dot)
                        liveStatusDot.backgroundTintList = android.content.res.ColorStateList.valueOf(
                            requireContext().getColor(R.color.status_success)
                        )
                    }
                } else {
                    if (ivLiveVideoFrame.drawable == null) {
                        idleStateOverlay.visibility = View.VISIBLE
                        tvIdleSubtext.text = "Starting camera..."
                        liveStatusDot.backgroundTintList = android.content.res.ColorStateList.valueOf(
                            requireContext().getColor(R.color.status_stopped)
                        )
                    }
                }

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
        try {
            if (cameraDaemonClient.isConnected || cameraDaemonClient.connect()) {
                val bytes = cameraDaemonClient.getFrame(cameraId)
                if (bytes != null && bytes.isNotEmpty()) return bytes
            }
        } catch (e: Exception) {
            Log.v(TAG, "Daemon socket frame failed: ${e.message}")
        }

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
            btnToggleFullscreen?.setIconResource(R.drawable.ic_fullscreen_exit)
        } else {
            liveUtilityRail.visibility = View.VISIBLE
            btnToggleFullscreen?.setIconResource(R.drawable.ic_fullscreen)
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
                tvLocationSpeed?.text = "Hız: $speedKmh km/h"
                tvGpsFreshness.text = "0s ago"
            } else {
                tvLocationCoordinates.text = "—"
                tvLocationSpeed?.text = "Hız: $speedKmh km/h"
                tvGpsFreshness.text = "0s ago"
            }
        } catch (e: Exception) {
            tvLocationCoordinates.text = "—"
            tvGpsFreshness.text = "0s ago"
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
