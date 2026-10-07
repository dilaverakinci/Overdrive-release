package com.overdrive.app.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.overdrive.app.R
import com.overdrive.app.ui.parking.ParkingConfig
import com.overdrive.app.ui.parking.ParkingSession
import com.overdrive.app.ui.parking.ParkingSessionAdapter
import com.overdrive.app.ui.parking.ParkingViewModel
import kotlinx.coroutines.launch

/**
 * Pure native Parking Intelligence fragment.
 * Eliminates Chromium WebView footprint, rendering sessions, hero cards,
 * and settings with 60 FPS Material 3 native components.
 */
class ParkingNativeFragment : Fragment() {

    private val viewModel: ParkingViewModel by viewModels()
    private lateinit var adapter: ParkingSessionAdapter

    // Header & Tabs
    private lateinit var tvParkingStatus: TextView
    private lateinit var parkingStatusDot: View
    private lateinit var toggleGroupParkingTabs: MaterialButtonToggleGroup
    private lateinit var layoutSessionsContainer: View
    private lateinit var layoutSettingsContainer: View

    // Hero Card
    private lateinit var cardParkedNowHero: MaterialCardView
    private lateinit var tvHeroPlace: TextView
    private lateinit var tvHeroDuration: TextView
    private lateinit var tvHeroSentryEvents: TextView
    private lateinit var tvHeroNeighbours: TextView
    private lateinit var tvHeroGpsStatus: TextView

    // Filters & List
    private lateinit var chipGroupParkingFilter: ChipGroup
    private lateinit var rvParkingSessions: RecyclerView
    private lateinit var emptyStateParking: LinearLayout
    private lateinit var pbParkingLoading: ProgressBar

    // Settings Switches
    private lateinit var switchParkingEnabled: MaterialSwitch
    private lateinit var switchSnapshots: MaterialSwitch
    private lateinit var switchNeighbours: MaterialSwitch
    private lateinit var switchSignage: MaterialSwitch

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
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        viewModel.loadData()
    }

    private fun bindViews(view: View) {
        tvParkingStatus = view.findViewById(R.id.tvParkingStatus)
        parkingStatusDot = view.findViewById(R.id.parkingStatusDot)
        toggleGroupParkingTabs = view.findViewById(R.id.toggleGroupParkingTabs)
        layoutSessionsContainer = view.findViewById(R.id.layoutSessionsContainer)
        layoutSettingsContainer = view.findViewById(R.id.layoutSettingsContainer)

        cardParkedNowHero = view.findViewById(R.id.cardParkedNowHero)
        tvHeroPlace = view.findViewById(R.id.tvHeroPlace)
        tvHeroDuration = view.findViewById(R.id.tvHeroDuration)
        tvHeroSentryEvents = view.findViewById(R.id.tvHeroSentryEvents)
        tvHeroNeighbours = view.findViewById(R.id.tvHeroNeighbours)
        tvHeroGpsStatus = view.findViewById(R.id.tvHeroGpsStatus)

        chipGroupParkingFilter = view.findViewById(R.id.chipGroupParkingFilter)
        rvParkingSessions = view.findViewById(R.id.rvParkingSessions)
        emptyStateParking = view.findViewById(R.id.emptyStateParking)
        pbParkingLoading = view.findViewById(R.id.pbParkingLoading)

        switchParkingEnabled = view.findViewById(R.id.switchParkingEnabled)
        switchSnapshots = view.findViewById(R.id.switchSnapshots)
        switchNeighbours = view.findViewById(R.id.switchNeighbours)
        switchSignage = view.findViewById(R.id.switchSignage)
    }

    private fun setupRecyclerView() {
        adapter = ParkingSessionAdapter { session ->
            showSessionDetailDialog(session)
        }
        rvParkingSessions.layoutManager = LinearLayoutManager(requireContext())
        rvParkingSessions.adapter = adapter
    }

    private fun setupListeners() {
        // Tab switching
        toggleGroupParkingTabs.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnTabSessions -> {
                        layoutSessionsContainer.visibility = View.VISIBLE
                        layoutSettingsContainer.visibility = View.GONE
                    }
                    R.id.btnTabSettings -> {
                        layoutSessionsContainer.visibility = View.GONE
                        layoutSettingsContainer.visibility = View.VISIBLE
                    }
                }
            }
        }

        // Quick filter chips
        chipGroupParkingFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            when {
                checkedIds.contains(R.id.chipFilter7Days) -> viewModel.filterByDays(7)
                checkedIds.contains(R.id.chipFilter30Days) -> viewModel.filterByDays(30)
                checkedIds.contains(R.id.chipFilter90Days) -> viewModel.filterByDays(90)
                checkedIds.contains(R.id.chipFilterAll) -> viewModel.filterByDays(0)
            }
        }

        // Settings switches
        val onConfigChange = {
            val current = viewModel.status.value?.config ?: ParkingConfig()
            val updated = current.copy(
                enabled = switchParkingEnabled.isChecked,
                snapshots = switchSnapshots.isChecked,
                neighbours = switchNeighbours.isChecked,
                signage = switchSignage.isChecked
            )
            viewModel.saveConfig(updated)
        }

        switchParkingEnabled.setOnCheckedChangeListener { _, _ -> onConfigChange() }
        switchSnapshots.setOnCheckedChangeListener { _, _ -> onConfigChange() }
        switchNeighbours.setOnCheckedChangeListener { _, _ -> onConfigChange() }
        switchSignage.setOnCheckedChangeListener { _, _ -> onConfigChange() }
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
                    viewModel.sessions.collect { sessions ->
                        adapter.submitList(sessions)
                        emptyStateParking.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.isLoading.collect { loading ->
                        pbParkingLoading.visibility = if (loading) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    private fun updateStatusUI(status: com.overdrive.app.ui.parking.ParkingStatus) {
        val context = requireContext()
        if (status.enabled) {
            tvParkingStatus.text = "AKTİF"
            tvParkingStatus.setTextColor(ContextCompat.getColor(context, R.color.brand_primary))
            parkingStatusDot.backgroundTintList = ContextCompat.getColorStateList(context, R.color.status_success)
        } else {
            tvParkingStatus.text = "KAPALI"
            tvParkingStatus.setTextColor(ContextCompat.getColor(context, R.color.status_stopped))
            parkingStatusDot.backgroundTintList = ContextCompat.getColorStateList(context, R.color.status_stopped)
        }

        // Current Hero Card
        val current = status.current
        if (current != null) {
            cardParkedNowHero.visibility = View.VISIBLE
            tvHeroPlace.text = current.place
            val mins = current.durationMs / 60000
            val hours = mins / 60
            val remMins = mins % 60
            tvHeroDuration.text = if (hours > 0) "${hours}s ${remMins}dk" else "${remMins}dk"
            tvHeroSentryEvents.text = current.eventsCount.toString()
            tvHeroNeighbours.text = current.neighboursCount.toString()
            tvHeroGpsStatus.text = "GPS ${current.gpsQuality}"
        } else {
            cardParkedNowHero.visibility = View.GONE
        }

        // Sync settings switches (avoid feedback loop by temporarily detaching listener or checking)
        if (switchParkingEnabled.isChecked != status.config.enabled) {
            switchParkingEnabled.isChecked = status.config.enabled
        }
        if (switchSnapshots.isChecked != status.config.snapshots) {
            switchSnapshots.isChecked = status.config.snapshots
        }
        if (switchNeighbours.isChecked != status.config.neighbours) {
            switchNeighbours.isChecked = status.config.neighbours
        }
        if (switchSignage.isChecked != status.config.signage) {
            switchSignage.isChecked = status.config.signage
        }
    }

    private fun showSessionDetailDialog(session: ParkingSession) {
        val sdf = java.text.SimpleDateFormat("dd MMMM yyyy, HH:mm", java.util.Locale.getDefault())
        val dateStr = sdf.format(java.util.Date(session.start))
        val mins = session.durationMs / 60000
        val durStr = if (mins >= 60) "${mins / 60} saat ${mins % 60} dakika" else "$mins dakika"

        val msg = StringBuilder()
            .append("Tarih: ").append(dateStr).append("\n")
            .append("Konum: ").append(session.place).append("\n")
            .append("Süre: ").append(durStr).append("\n")
            .append("Sentry Olay Sayısı: ").append(session.eventsCount).append("\n")
            .append("Komşu Araçlar: ").append(session.neighboursCount).append("\n")

        if (!session.signageLabel.isNullOrBlank()) {
            msg.append("Tabela / Kat OCR: ").append(session.signageLabel).append("\n")
        }
        if (session.energyUsedKwh != null) {
            msg.append("Enerji Tüketimi: ").append(String.format(java.util.Locale.getDefault(), "%.2f kWh", session.energyUsedKwh)).append("\n")
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Park Oturumu Detayı")
            .setMessage(msg.toString())
            .setPositiveButton("Tamam", null)
            .setNegativeButton("Oturumu Sil") { _, _ ->
                confirmDeleteSession(session)
            }
            .show()
    }

    private fun confirmDeleteSession(session: ParkingSession) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Oturumu Sil")
            .setMessage("Bu park oturumunu silmek istediğinize emin misiniz?")
            .setPositiveButton("Sil") { _, _ ->
                viewModel.deleteSession(session.id)
            }
            .setNegativeButton("İptal", null)
            .show()
    }
}
