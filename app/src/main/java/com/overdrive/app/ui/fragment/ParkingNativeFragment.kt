package com.overdrive.app.ui.fragment

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.overdrive.app.R
import com.overdrive.app.ui.parking.GeocodingConfig
import com.overdrive.app.ui.parking.ParkingConfig
import com.overdrive.app.ui.parking.ParkingDetail
import com.overdrive.app.ui.parking.ParkingSession
import com.overdrive.app.ui.parking.ParkingSessionAdapter
import com.overdrive.app.ui.parking.ParkingStatus
import com.overdrive.app.ui.parking.ParkingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pure native Parking Intelligence fragment.
 * Complete 1:1 visual and functional parity with legacy parking.html/js,
 * using 60 FPS Material 3 native components without Chromium WebView overhead.
 */
class ParkingNativeFragment : Fragment() {

    private val viewModel: ParkingViewModel by viewModels()
    private lateinit var adapter: ParkingSessionAdapter

    // Main / Detail containers
    private lateinit var layoutMainParkingContent: View
    private lateinit var layoutDetailContainer: View

    // Top Header
    private lateinit var tvParkingTitle: TextView
    private lateinit var parkingStatusBadge: View
    private lateinit var parkingStatusDot: View
    private lateinit var tvParkingStatus: TextView

    // Tabs
    private lateinit var tabButtonSessions: TextView
    private lateinit var tabButtonSettings: TextView
    private lateinit var layoutSessionsContainer: View
    private lateinit var layoutSettingsContainer: View

    // Hero Card
    private lateinit var cardParkedNowHero: MaterialCardView
    private lateinit var tvHeroPlace: TextView
    private lateinit var tvHeroSub: TextView
    private lateinit var tvHeroDuration: TextView
    private lateinit var tvHeroSentryEvents: TextView
    private lateinit var tvHeroNeighbours: TextView
    private lateinit var tvHeroGpsStatus: TextView

    // Filter Buttons
    private lateinit var btnFilter7Days: TextView
    private lateinit var btnFilter30Days: TextView
    private lateinit var btnFilter90Days: TextView
    private lateinit var btnFilterAll: TextView

    // Sessions & Empty State
    private lateinit var rvParkingSessions: RecyclerView
    private lateinit var emptyStateParking: LinearLayout
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptyText: TextView
    private lateinit var btnEnableFromEmpty: MaterialButton
    private lateinit var pbParkingLoading: ProgressBar

    // Settings Controls
    private lateinit var switchParkingEnabled: MaterialSwitch
    private lateinit var layoutSubSettings: LinearLayout
    private lateinit var spinnerEndTrigger: Spinner
    private lateinit var switchSnapshots: MaterialSwitch
    private lateinit var switchNeighbours: MaterialSwitch
    private lateinit var switchSignage: MaterialSwitch
    private lateinit var switchGeocodingEnabled: MaterialSwitch
    private lateinit var switchGeocodingOnline: MaterialSwitch
    private lateinit var etRetentionDays: EditText
    private lateinit var etStorageCapMb: EditText

    // Detail Drill-in Views
    private lateinit var btnDetailBack: TextView
    private lateinit var btnDetailMap: TextView
    private lateinit var btnDetailRecordings: TextView
    private lateinit var tvDetailPlace: TextView
    private lateinit var tvDetailSubtitle: TextView
    private lateinit var tvDetailDuration: TextView
    private lateinit var tvDetailEnergy: TextView
    private lateinit var tvDetailEvents: TextView
    private lateinit var tvDetailNeighbours: TextView
    private lateinit var cardDetailStills: View
    private lateinit var tvNoStillsHint: TextView
    private lateinit var layoutStillsColumns: View
    private lateinit var ivArrivedMosaic: ImageView
    private lateinit var ivReturnedMosaic: ImageView
    private lateinit var cardDetailSignage: View
    private lateinit var tvSignageStatusTag: TextView
    private lateinit var tvSignageValue: TextView
    private lateinit var tvSignageConfidence: TextView
    private lateinit var btnRequeueSignage: MaterialButton
    private lateinit var btnDeleteSession: MaterialButton

    // Lightbox Overlay
    private lateinit var layoutLightboxOverlay: FrameLayout
    private lateinit var ivLightboxImage: ImageView
    private lateinit var btnLightboxClose: ImageView

    private var currentActiveTab: String = "sessions"
    private var isUpdatingSettingsUI = false
    private var arrivedBitmap: Bitmap? = null
    private var returnedBitmap: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_parking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupTabs()
        setupFilters()
        setupRecyclerView()
        setupSettingsControls()
        setupDetailViews()
        setupLightbox()
        observeViewModel()
        viewModel.loadData()
    }

    private fun bindViews(view: View) {
        layoutMainParkingContent = view.findViewById(R.id.layoutMainParkingContent)
        layoutDetailContainer = view.findViewById(R.id.layoutDetailContainer)

        tvParkingTitle = view.findViewById(R.id.tvParkingTitle)
        parkingStatusBadge = view.findViewById(R.id.parkingStatusBadge)
        parkingStatusDot = view.findViewById(R.id.parkingStatusDot)
        tvParkingStatus = view.findViewById(R.id.tvParkingStatus)

        tabButtonSessions = view.findViewById(R.id.tabButtonSessions)
        tabButtonSettings = view.findViewById(R.id.tabButtonSettings)
        layoutSessionsContainer = view.findViewById(R.id.layoutSessionsContainer)
        layoutSettingsContainer = view.findViewById(R.id.layoutSettingsContainer)

        cardParkedNowHero = view.findViewById(R.id.cardParkedNowHero)
        tvHeroPlace = view.findViewById(R.id.tvHeroPlace)
        tvHeroSub = view.findViewById(R.id.tvHeroSub)
        tvHeroDuration = view.findViewById(R.id.tvHeroDuration)
        tvHeroSentryEvents = view.findViewById(R.id.tvHeroSentryEvents)
        tvHeroNeighbours = view.findViewById(R.id.tvHeroNeighbours)
        tvHeroGpsStatus = view.findViewById(R.id.tvHeroGpsStatus)

        btnFilter7Days = view.findViewById(R.id.btnFilter7Days)
        btnFilter30Days = view.findViewById(R.id.btnFilter30Days)
        btnFilter90Days = view.findViewById(R.id.btnFilter90Days)
        btnFilterAll = view.findViewById(R.id.btnFilterAll)

        rvParkingSessions = view.findViewById(R.id.rvParkingSessions)
        emptyStateParking = view.findViewById(R.id.emptyStateParking)
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle)
        tvEmptyText = view.findViewById(R.id.tvEmptyText)
        btnEnableFromEmpty = view.findViewById(R.id.btnEnableFromEmpty)
        pbParkingLoading = view.findViewById(R.id.pbParkingLoading)

        switchParkingEnabled = view.findViewById(R.id.switchParkingEnabled)
        layoutSubSettings = view.findViewById(R.id.layoutSubSettings)
        spinnerEndTrigger = view.findViewById(R.id.spinnerEndTrigger)
        switchSnapshots = view.findViewById(R.id.switchSnapshots)
        switchNeighbours = view.findViewById(R.id.switchNeighbours)
        switchSignage = view.findViewById(R.id.switchSignage)
        switchGeocodingEnabled = view.findViewById(R.id.switchGeocodingEnabled)
        switchGeocodingOnline = view.findViewById(R.id.switchGeocodingOnline)
        etRetentionDays = view.findViewById(R.id.etRetentionDays)
        etStorageCapMb = view.findViewById(R.id.etStorageCapMb)

        btnDetailBack = view.findViewById(R.id.btnDetailBack)
        btnDetailMap = view.findViewById(R.id.btnDetailMap)
        btnDetailRecordings = view.findViewById(R.id.btnDetailRecordings)
        tvDetailPlace = view.findViewById(R.id.tvDetailPlace)
        tvDetailSubtitle = view.findViewById(R.id.tvDetailSubtitle)
        tvDetailDuration = view.findViewById(R.id.tvDetailDuration)
        tvDetailEnergy = view.findViewById(R.id.tvDetailEnergy)
        tvDetailEvents = view.findViewById(R.id.tvDetailEvents)
        tvDetailNeighbours = view.findViewById(R.id.tvDetailNeighbours)
        cardDetailStills = view.findViewById(R.id.cardDetailStills)
        tvNoStillsHint = view.findViewById(R.id.tvNoStillsHint)
        layoutStillsColumns = view.findViewById(R.id.layoutStillsColumns)
        ivArrivedMosaic = view.findViewById(R.id.ivArrivedMosaic)
        ivReturnedMosaic = view.findViewById(R.id.ivReturnedMosaic)
        cardDetailSignage = view.findViewById(R.id.cardDetailSignage)
        tvSignageStatusTag = view.findViewById(R.id.tvSignageStatusTag)
        tvSignageValue = view.findViewById(R.id.tvSignageValue)
        tvSignageConfidence = view.findViewById(R.id.tvSignageConfidence)
        btnRequeueSignage = view.findViewById(R.id.btnRequeueSignage)
        btnDeleteSession = view.findViewById(R.id.btnDeleteSession)

        layoutLightboxOverlay = view.findViewById(R.id.layoutLightboxOverlay)
        ivLightboxImage = view.findViewById(R.id.ivLightboxImage)
        btnLightboxClose = view.findViewById(R.id.btnLightboxClose)
    }

    private fun setupTabs() {
        tabButtonSessions.setOnClickListener { switchTab("sessions") }
        tabButtonSettings.setOnClickListener { switchTab("settings") }
    }

    private fun switchTab(tab: String) {
        currentActiveTab = tab
        if (tab == "sessions") {
            tabButtonSessions.setBackgroundResource(R.drawable.bg_parking_tab_left_active)
            tabButtonSessions.setTextColor(Color.parseColor("#0F172A"))
            tabButtonSettings.setBackgroundColor(Color.TRANSPARENT)
            tabButtonSettings.setTextColor(Color.parseColor("#64748B"))
            layoutSessionsContainer.visibility = View.VISIBLE
            layoutSettingsContainer.visibility = View.GONE
        } else {
            tabButtonSettings.setBackgroundResource(R.drawable.bg_parking_tab_right_active)
            tabButtonSettings.setTextColor(Color.parseColor("#0F172A"))
            tabButtonSessions.setBackgroundColor(Color.TRANSPARENT)
            tabButtonSessions.setTextColor(Color.parseColor("#64748B"))
            layoutSessionsContainer.visibility = View.GONE
            layoutSettingsContainer.visibility = View.VISIBLE
        }
    }

    private fun setupFilters() {
        btnFilter7Days.setOnClickListener { applyFilter(7) }
        btnFilter30Days.setOnClickListener { applyFilter(30) }
        btnFilter90Days.setOnClickListener { applyFilter(90) }
        btnFilterAll.setOnClickListener { applyFilter(0) }
    }

    private fun applyFilter(days: Int) {
        val normalBg = R.drawable.bg_parking_filter_normal
        val activeBg = R.drawable.bg_parking_filter_active
        val normalColor = Color.parseColor("#64748B")
        val activeColor = Color.parseColor("#064E3B")

        btnFilter7Days.setBackgroundResource(if (days == 7) activeBg else normalBg)
        btnFilter7Days.text = if (days == 7) "✓ 7 Gün" else "7 Gün"
        btnFilter7Days.setTextColor(if (days == 7) activeColor else normalColor)

        btnFilter30Days.setBackgroundResource(if (days == 30) activeBg else normalBg)
        btnFilter30Days.text = if (days == 30) "✓ 30 Gün" else "30 Gün"
        btnFilter30Days.setTextColor(if (days == 30) activeColor else normalColor)

        btnFilter90Days.setBackgroundResource(if (days == 90) activeBg else normalBg)
        btnFilter90Days.text = if (days == 90) "✓ 90 Gün" else "90 Gün"
        btnFilter90Days.setTextColor(if (days == 90) activeColor else normalColor)

        btnFilterAll.setBackgroundResource(if (days == 0) activeBg else normalBg)
        btnFilterAll.text = if (days == 0) "✓ Tümü" else "Tümü"
        btnFilterAll.setTextColor(if (days == 0) activeColor else normalColor)

        viewModel.filterByDays(days)
    }

    private fun setupRecyclerView() {
        adapter = ParkingSessionAdapter { session ->
            openSessionDetail(session.id)
        }

        val gridLayoutManager = GridLayoutManager(requireContext(), 3)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (adapter.getItemViewType(position) == ParkingSessionAdapter.VIEW_TYPE_HEADER) 3 else 1
            }
        }
        rvParkingSessions.layoutManager = gridLayoutManager
        rvParkingSessions.adapter = adapter

        btnEnableFromEmpty.setOnClickListener {
            val current = viewModel.status.value?.config ?: ParkingConfig()
            viewModel.saveConfig(current.copy(enabled = true)) { ok ->
                if (ok) switchTab("settings")
            }
        }
    }

    private fun setupSettingsControls() {
        // EndTrigger spinner options
        val triggerLabels = arrayOf("Geri döndüğümde", "Araç çalıştığında", "Sürüşe başladığımda")
        val triggerValues = arrayOf("return", "power_on", "drive_away")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, triggerLabels)
        spinnerEndTrigger.adapter = spinnerAdapter

        spinnerEndTrigger.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isUpdatingSettingsUI) return
                val current = viewModel.status.value?.config ?: return
                val selected = triggerValues.getOrElse(position) { "return" }
                if (current.endTrigger != selected) {
                    viewModel.saveConfig(current.copy(endTrigger = selected))
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val onConfigSwitchChanged = {
            if (!isUpdatingSettingsUI) {
                val current = viewModel.status.value?.config ?: ParkingConfig()
                val updated = current.copy(
                    enabled = switchParkingEnabled.isChecked,
                    snapshots = switchSnapshots.isChecked,
                    neighbours = switchNeighbours.isChecked,
                    signage = switchSignage.isChecked
                )
                viewModel.saveConfig(updated)
                updateSubSettingsEnabled(updated.enabled)
            }
        }

        switchParkingEnabled.setOnCheckedChangeListener { _, _ -> onConfigSwitchChanged() }
        switchSnapshots.setOnCheckedChangeListener { _, _ -> onConfigSwitchChanged() }
        switchNeighbours.setOnCheckedChangeListener { _, _ -> onConfigSwitchChanged() }
        switchSignage.setOnCheckedChangeListener { _, _ -> onConfigSwitchChanged() }

        val onGeocodingSwitchChanged = {
            if (!isUpdatingSettingsUI) {
                viewModel.saveGeocoding(
                    enabled = switchGeocodingEnabled.isChecked,
                    allowOnline = switchGeocodingOnline.isChecked
                )
                switchGeocodingOnline.isEnabled = switchGeocodingEnabled.isChecked
            }
        }

        switchGeocodingEnabled.setOnCheckedChangeListener { _, _ -> onGeocodingSwitchChanged() }
        switchGeocodingOnline.setOnCheckedChangeListener { _, _ -> onGeocodingSwitchChanged() }

        etRetentionDays.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val days = etRetentionDays.text.toString().toIntOrNull()?.coerceIn(7, 730) ?: 90
                etRetentionDays.setText(days.toString())
                val current = viewModel.status.value?.config ?: ParkingConfig()
                if (current.retentionDays != days) {
                    viewModel.saveConfig(current.copy(retentionDays = days))
                }
            }
            false
        }

        etStorageCapMb.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val cap = etStorageCapMb.text.toString().toIntOrNull()?.coerceIn(50, 4096) ?: 300
                etStorageCapMb.setText(cap.toString())
                val current = viewModel.status.value?.config ?: ParkingConfig()
                if (current.storageCapMb != cap) {
                    viewModel.saveConfig(current.copy(storageCapMb = cap))
                }
            }
            false
        }
    }

    private fun updateSubSettingsEnabled(enabled: Boolean) {
        layoutSubSettings.alpha = if (enabled) 1.0f else 0.55f
        for (i in 0 until layoutSubSettings.childCount) {
            val child = layoutSubSettings.getChildAt(i)
            child.isEnabled = enabled
        }
        spinnerEndTrigger.isEnabled = enabled
        switchSnapshots.isEnabled = enabled
        switchNeighbours.isEnabled = enabled
        switchSignage.isEnabled = enabled
        switchGeocodingEnabled.isEnabled = enabled
        switchGeocodingOnline.isEnabled = enabled && switchGeocodingEnabled.isChecked
        etRetentionDays.isEnabled = enabled
        etStorageCapMb.isEnabled = enabled
    }

    private fun setupDetailViews() {
        btnDetailBack.setOnClickListener {
            viewModel.closeSessionDetail()
            layoutDetailContainer.visibility = View.GONE
            layoutMainParkingContent.visibility = View.VISIBLE
        }

        btnDetailMap.setOnClickListener {
            val detail = viewModel.detail.value ?: return@setOnClickListener
            val s = detail.session
            if (s.lat != null && s.lng != null) {
                val uri = Uri.parse("geo:${s.lat},${s.lng}?q=${s.lat},${s.lng}(${Uri.encode(s.place)})")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Harita uygulaması bulunamadı", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "GPS konumu mevcut değil", Toast.LENGTH_SHORT).show()
            }
        }

        btnDetailRecordings.setOnClickListener {
            try {
                findNavController().navigate(R.id.recordingsFragment)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Kayıtlar sekmesine geçilemedi", Toast.LENGTH_SHORT).show()
            }
        }

        btnRequeueSignage.setOnClickListener {
            val detail = viewModel.detail.value ?: return@setOnClickListener
            viewModel.requeueSignage(detail.session.id) { ok ->
                Toast.makeText(
                    requireContext(),
                    if (ok) "Tabela okuma kuyruğa alındı" else "Park Zekası çalışmıyor",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        btnDeleteSession.setOnClickListener {
            val detail = viewModel.detail.value ?: return@setOnClickListener
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Oturumu Sil")
                .setMessage("Bu park oturumunu, fotoğraflarını ve karelerini silmek istediğinize emin misiniz?")
                .setPositiveButton("Sil") { _, _ ->
                    viewModel.deleteSession(detail.session.id) { ok ->
                        if (ok) {
                            Toast.makeText(requireContext(), "Oturum silindi", Toast.LENGTH_SHORT).show()
                            layoutDetailContainer.visibility = View.GONE
                            layoutMainParkingContent.visibility = View.VISIBLE
                        } else {
                            Toast.makeText(requireContext(), "Oturum silinemedi", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("İptal", null)
                .show()
        }

        cardParkedNowHero.setOnClickListener {
            val current = viewModel.status.value?.current
            if (current != null) {
                openSessionDetail(current.id)
            }
        }
    }

    private fun setupLightbox() {
        ivArrivedMosaic.setOnClickListener {
            if (arrivedBitmap != null) {
                ivLightboxImage.setImageBitmap(arrivedBitmap)
                layoutLightboxOverlay.visibility = View.VISIBLE
            }
        }

        ivReturnedMosaic.setOnClickListener {
            if (returnedBitmap != null) {
                ivLightboxImage.setImageBitmap(returnedBitmap)
                layoutLightboxOverlay.visibility = View.VISIBLE
            }
        }

        btnLightboxClose.setOnClickListener {
            layoutLightboxOverlay.visibility = View.GONE
        }

        layoutLightboxOverlay.setOnClickListener {
            layoutLightboxOverlay.visibility = View.GONE
        }
    }

    private fun openSessionDetail(id: String) {
        layoutMainParkingContent.visibility = View.GONE
        layoutDetailContainer.visibility = View.VISIBLE
        viewModel.openSessionDetail(id)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.status.collect { status ->
                        if (status != null) {
                            updateStatusUI(status)
                        }
                    }
                }

                launch {
                    viewModel.geocoding.collect { geo ->
                        if (geo != null) {
                            updateGeocodingUI(geo)
                        }
                    }
                }

                launch {
                    viewModel.sessions.collect { sessions ->
                        adapter.submitSessions(sessions)
                        val isEnabled = viewModel.status.value?.enabled ?: false
                        val filterDays = viewModel.selectedDays.value
                        updateEmptyState(sessions.isEmpty(), isEnabled, filterDays)
                    }
                }

                launch {
                    viewModel.isLoading.collect { loading ->
                        pbParkingLoading.visibility = if (loading) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.detail.collect { detail ->
                        if (detail != null) {
                            populateDetail(detail)
                        }
                    }
                }
            }
        }
    }

    private fun updateStatusUI(status: ParkingStatus) {
        val context = requireContext()
        if (status.enabled) {
            tvParkingStatus.text = if (status.running) "AÇIK" else "DURAKLATILDI"
            tvParkingStatus.setTextColor(ContextCompat.getColor(context, R.color.brand_primary))
            parkingStatusDot.backgroundTintList = ContextCompat.getColorStateList(context, R.color.status_success)
        } else {
            tvParkingStatus.text = "KAPALI"
            tvParkingStatus.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
            parkingStatusDot.backgroundTintList = ContextCompat.getColorStateList(context, R.color.status_stopped)
        }

        // Hero Card
        val cur = status.current
        if (cur != null) {
            cardParkedNowHero.visibility = View.VISIBLE
            tvHeroPlace.text = if (!cur.signageLabel.isNullOrBlank()) "${cur.place} · ${cur.signageLabel}" else cur.place
            val daySdf = SimpleDateFormat("EEEE", Locale("tr"))
            val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dayStr = daySdf.format(Date(cur.start)).replaceFirstChar { it.uppercase() }
            val timeStr = timeSdf.format(Date(cur.start))
            val gpsStr = when (cur.gpsQuality.uppercase()) {
                "FRESH" -> "GPS güncel"
                "RECENT" -> "GPS yakın"
                "STALE" -> "GPS eski"
                else -> "GPS yok"
            }
            val sentryStr = if (cur.sentryState == "armed") "Nöbetçi devrede" else "Nöbetçi beklemede"
            tvHeroSub.text = "$dayStr · $timeStr · $gpsStr · $sentryStr"

            val mins = cur.durationMs / 60000
            val hours = mins / 60
            val remMins = mins % 60
            tvHeroDuration.text = if (hours > 0) "${hours}s ${remMins}dk" else "${remMins}dk"
            tvHeroSentryEvents.text = cur.eventsCount.toString()
            tvHeroNeighbours.text = cur.neighboursCount.toString()
            tvHeroGpsStatus.text = gpsStr
        } else {
            cardParkedNowHero.visibility = View.GONE
        }

        // Sync Settings values
        isUpdatingSettingsUI = true
        val cfg = status.config
        if (switchParkingEnabled.isChecked != cfg.enabled) {
            switchParkingEnabled.isChecked = cfg.enabled
        }
        val triggerIndex = when (cfg.endTrigger) {
            "power_on" -> 1
            "drive_away" -> 2
            else -> 0
        }
        if (spinnerEndTrigger.selectedItemPosition != triggerIndex) {
            spinnerEndTrigger.setSelection(triggerIndex)
        }
        if (switchSnapshots.isChecked != cfg.snapshots) {
            switchSnapshots.isChecked = cfg.snapshots
        }
        if (switchNeighbours.isChecked != cfg.neighbours) {
            switchNeighbours.isChecked = cfg.neighbours
        }
        if (switchSignage.isChecked != cfg.signage) {
            switchSignage.isChecked = cfg.signage
        }
        if (etRetentionDays.text.toString() != cfg.retentionDays.toString()) {
            etRetentionDays.setText(cfg.retentionDays.toString())
        }
        if (etStorageCapMb.text.toString() != cfg.storageCapMb.toString()) {
            etStorageCapMb.setText(cfg.storageCapMb.toString())
        }
        updateSubSettingsEnabled(cfg.enabled)
        isUpdatingSettingsUI = false
    }

    private fun updateGeocodingUI(geo: GeocodingConfig) {
        isUpdatingSettingsUI = true
        if (switchGeocodingEnabled.isChecked != geo.enabled) {
            switchGeocodingEnabled.isChecked = geo.enabled
        }
        if (switchGeocodingOnline.isChecked != geo.allowOnline) {
            switchGeocodingOnline.isChecked = geo.allowOnline
        }
        switchGeocodingOnline.isEnabled = geo.enabled && (viewModel.status.value?.enabled ?: false)
        isUpdatingSettingsUI = false
    }

    private fun updateEmptyState(isEmpty: Boolean, isEnabled: Boolean, filterDays: Int) {
        if (!isEmpty) {
            emptyStateParking.visibility = View.GONE
            return
        }
        emptyStateParking.visibility = View.VISIBLE
        if (!isEnabled) {
            tvEmptyTitle.text = "Park Zekası kapalı"
            tvEmptyText.text = "Açıldığında, her stop ediş bir park oturumu açar: aracın nerede olduğu, uzaklaştığınızda 4 kamera fotoğrafı, nöbetçinin izlediği sırada yanınıza gelen veya ayrılan araçlar ve döndüğünüzde bir 'Araca dönüş' özeti."
            btnEnableFromEmpty.visibility = View.VISIBLE
        } else {
            if (filterDays > 0) {
                tvEmptyTitle.text = "Bu aralıkta park oturumu yok"
                tvEmptyText.text = "Seçilen tarih aralığında kaydedilmiş park oturumu bulunamadı."
            } else {
                tvEmptyTitle.text = "Henüz park oturumu bulunmuyor"
                tvEmptyText.text = "Araç stop edildiğinde ilk oturum otomatik başlatılır."
            }
            btnEnableFromEmpty.visibility = View.GONE
        }
    }

    private fun populateDetail(d: ParkingDetail) {
        val s = d.session
        val place = if (!s.signageLabel.isNullOrBlank()) "${s.place} · ${s.signageLabel}" else s.place
        tvDetailPlace.text = place

        val dateSdf = SimpleDateFormat("EEEE, d MMMM", Locale("tr"))
        val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateStr = dateSdf.format(Date(s.start)).replaceFirstChar { it.uppercase() }
        val startStr = timeSdf.format(Date(s.start))
        val endStr = if (s.end != null && s.end > 0) timeSdf.format(Date(s.end)) else "Şu an park halinde"
        tvDetailSubtitle.text = "$dateStr · $startStr – $endStr"

        val mins = s.durationMs / 60000
        val hours = mins / 60
        val remMins = mins % 60
        tvDetailDuration.text = if (hours > 0) "${hours}s ${remMins}dk" else "${remMins}dk"

        tvDetailEvents.text = s.eventsCount.toString()
        tvDetailNeighbours.text = d.neighbours.size.toString()

        if (s.energyUsedKwh != null) {
            val prefix = if (s.energyUsedKwh > 0) "+" else ""
            tvDetailEnergy.text = String.format(Locale.getDefault(), "%s%.2f kWh", prefix, s.energyUsedKwh)
        } else if (s.socDelta != null) {
            val prefix = if (s.socDelta > 0) "+" else ""
            tvDetailEnergy.text = String.format(Locale.getDefault(), "%s%.1f%%", prefix, s.socDelta)
        } else {
            tvDetailEnergy.text = "--"
        }

        // Stills
        val hasStills = !d.assets.arrivedMosaic.isNullOrEmpty() || !d.assets.returnedMosaic.isNullOrEmpty()
        tvNoStillsHint.visibility = if (hasStills) View.GONE else View.VISIBLE
        layoutStillsColumns.visibility = if (hasStills) View.VISIBLE else View.GONE

        arrivedBitmap = null
        returnedBitmap = null
        ivArrivedMosaic.setImageDrawable(null)
        ivReturnedMosaic.setImageDrawable(null)

        if (!d.assets.arrivedMosaic.isNullOrEmpty()) {
            loadAssetBitmap(d.assets.arrivedMosaic) { bmp ->
                arrivedBitmap = bmp
                ivArrivedMosaic.setImageBitmap(bmp)
            }
        }
        if (!d.assets.returnedMosaic.isNullOrEmpty()) {
            loadAssetBitmap(d.assets.returnedMosaic) { bmp ->
                returnedBitmap = bmp
                ivReturnedMosaic.setImageBitmap(bmp)
            }
        }

        // Signage
        val hasSignage = !s.signageLabel.isNullOrBlank()
        cardDetailSignage.visibility = View.VISIBLE
        if (hasSignage) {
            tvSignageValue.text = s.signageLabel
            tvSignageStatusTag.text = "Okundu"
            val conf = s.signageConfidence?.let { (it * 100).toInt() } ?: 90
            tvSignageConfidence.text = "Güven skoru: %$conf"
        } else {
            tvSignageValue.text = "Tabela bulunamadı"
            tvSignageStatusTag.text = "Beklemede"
            tvSignageConfidence.text = "Kat, blok veya park yeri numarası algılanamadı."
        }
    }

    private fun loadAssetBitmap(urlPath: String, onLoaded: (Bitmap) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fullUrl = if (urlPath.startsWith("http")) urlPath else "http://127.0.0.1:8080$urlPath"
                val conn = URL(fullUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                if (conn.responseCode == 200) {
                    val bmp = BitmapFactory.decodeStream(conn.inputStream)
                    conn.disconnect()
                    if (bmp != null) {
                        withContext(Dispatchers.Main) {
                            onLoaded(bmp)
                        }
                    }
                } else {
                    conn.disconnect()
                }
            } catch (ignored: Exception) {}
        }
    }
}
