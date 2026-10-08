package com.overdrive.app.ui.trips

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.overdrive.app.logging.DaemonLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TripsViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: TripsRepository = TripsRepository(application)
) : AndroidViewModel(application) {

    private val logger = DaemonLogger.getInstance("TripsViewModel")

    private val _uiState = MutableStateFlow(TripsUiState())
    val uiState: StateFlow<TripsUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var recoveryPollJob: Job? = null

    init {
        loadBootstrap()
    }

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(10000)
                refreshData()
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun selectTab(tab: TripsTab) {
        _uiState.update { it.copy(activeTab = tab, error = null, infoMessage = null) }
    }

    fun setPeriodFilter(filter: PeriodFilter) {
        if (_uiState.value.periodFilter == filter) return
        _uiState.update { it.copy(periodFilter = filter, isLoading = true) }
        loadTripsForFilter(filter)
    }

    fun loadBootstrap() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val currentFilter = _uiState.value.periodFilter
            val result = repository.getBootstrap(days = currentFilter.days, limit = 50, offset = 0)
            result.onSuccess { data ->
                val summary = computeSummary(data.trips, data.weeklyRollups)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        config = data.config ?: it.config,
                        storage = data.storage ?: it.storage,
                        dnaScores = data.dna ?: it.dnaScores,
                        rangeEstimate = data.range ?: it.rangeEstimate,
                        weeklyRollups = data.weeklyRollups,
                        trips = data.trips,
                        summary = summary,
                        error = null
                    )
                }
            }.onFailure { err ->
                logger.error("Failed to load bootstrap: ${err.message}", err)
                _uiState.update { it.copy(isLoading = false, error = err.message) }
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            val currentFilter = _uiState.value.periodFilter
            val result = repository.getTrips(days = currentFilter.days, limit = 50, offset = 0)
            result.onSuccess { tripList ->
                val summary = computeSummary(tripList)
                _uiState.update {
                    it.copy(
                        trips = tripList,
                        summary = summary
                    )
                }
            }
        }
    }

    private fun loadTripsForFilter(filter: PeriodFilter) {
        viewModelScope.launch {
            val result = repository.getTrips(days = filter.days, limit = 50, offset = 0)
            result.onSuccess { tripList ->
                val summary = computeSummary(tripList)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        trips = tripList,
                        summary = summary,
                        error = null
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message) }
            }
        }
    }

    private fun computeSummary(trips: List<TripRecordItem>, rollups: List<WeeklyRollupItem> = emptyList()): TripsSummaryPeriod {
        if (trips.isEmpty() && rollups.isNotEmpty()) {
            val s = rollups.first()
            val overall = ((s.avgAnticipation + s.avgSmoothness + s.avgSpeedDiscipline + s.avgEfficiencyScore + s.avgConsistency) / 5)
            val cons = if (s.totalDistanceKm > 0.5 && s.totalEnergyKwh > 0.0) (s.totalEnergyKwh / s.totalDistanceKm) * 100.0 else null
            val eff = if (s.totalDistanceKm > 0.5 && s.totalEnergyKwh > 0.0) s.totalDistanceKm / s.totalEnergyKwh else null
            return TripsSummaryPeriod(
                tripCount = s.tripCount,
                totalDistanceKm = s.totalDistanceKm,
                totalDurationSeconds = s.totalDurationSeconds,
                totalEnergyKwh = s.totalEnergyKwh,
                totalCost = s.totalCost,
                avgScore = if (overall > 0) overall else null,
                avgConsumptionKwhPer100Km = cons,
                avgEfficiencyKmPerKwh = eff
            )
        }

        val count = trips.size
        val dist = trips.sumOf { it.distanceKm }
        val dur = trips.sumOf { it.durationSeconds }
        val energy = trips.sumOf { it.energyUsedKwh }
        val cost = trips.sumOf { it.tripCost }

        val scoredTrips = trips.filter {
            it.overallScore > 0 || it.anticipationScore > 0 || it.smoothnessScore > 0 ||
                it.speedDisciplineScore > 0 || it.efficiencyScore > 0 || it.consistencyScore > 0
        }
        val avgScore = if (scoredTrips.isNotEmpty()) {
            val totalScore = scoredTrips.sumOf {
                if (it.overallScore > 0) it.overallScore
                else (it.anticipationScore + it.smoothnessScore + it.speedDisciplineScore + it.efficiencyScore + it.consistencyScore) / 5
            }
            totalScore / scoredTrips.size
        } else null

        val avgConsumption = if (dist > 0.5 && energy > 0.0) {
            (energy / dist) * 100.0
        } else if (dist > 0.5) {
            val totalSocDelta = trips.sumOf { (it.socStart - it.socEnd).coerceAtLeast(0.0) }
            if (totalSocDelta > 0.0) (totalSocDelta / dist) * 100.0 else null
        } else null

        val avgEfficiency = if (dist > 0.5 && energy > 0.0) {
            dist / energy
        } else null

        return TripsSummaryPeriod(
            tripCount = count,
            totalDistanceKm = dist,
            totalDurationSeconds = dur,
            totalEnergyKwh = energy,
            totalCost = cost,
            avgScore = avgScore,
            avgConsumptionKwhPer100Km = avgConsumption,
            avgEfficiencyKmPerKwh = avgEfficiency
        )
    }

    fun openTripDetail(trip: TripRecordItem) {
        _uiState.update { it.copy(activeTripDetail = trip, telemetrySamples = emptyList()) }
        loadTelemetry(trip.id)
    }

    fun closeTripDetail() {
        _uiState.update { it.copy(activeTripDetail = null, telemetrySamples = emptyList()) }
    }

    private fun loadTelemetry(tripId: Long) {
        viewModelScope.launch {
            val result = repository.getTelemetry(tripId)
            result.onSuccess { samples ->
                _uiState.update { it.copy(telemetrySamples = samples) }
            }.onFailure {
                logger.warn("Could not load telemetry for trip $tripId: ${it.message}")
            }
        }
    }

    fun deleteTrip(tripId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.deleteTrip(tripId)
            result.onSuccess {
                _uiState.update { state ->
                    val updatedTrips = state.trips.filter { it.id != tripId }
                    val isCurrentDetailDeleted = state.activeTripDetail?.id == tripId
                    state.copy(
                        isLoading = false,
                        trips = updatedTrips,
                        summary = computeSummary(updatedTrips),
                        activeTripDetail = if (isCurrentDetailDeleted) null else state.activeTripDetail,
                        infoMessage = "Trip deleted successfully"
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message) }
            }
        }
    }

    fun rescoreTrip(tripId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.rescoreTrip(tripId)
            result.onSuccess { updatedTrip ->
                _uiState.update { state ->
                    val updatedList = state.trips.map { if (it.id == tripId) updatedTrip else it }
                    state.copy(
                        isLoading = false,
                        trips = updatedList,
                        activeTripDetail = updatedTrip,
                        infoMessage = "Trip rescored successfully"
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message) }
            }
        }
    }

    fun updateAnalyticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val result = repository.saveConfig(enabled = enabled)
            result.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        config = state.config?.copy(enabled = enabled) ?: TripConfigItem(
                            enabled = enabled,
                            electricityRate = 0.0,
                            currency = "₺",
                            tankCapacityL = 0.0,
                            fuelPricePerL = 0.0,
                            fuelUnit = "L",
                            distanceUnit = "km",
                            isPhev = false,
                            nominalKwh = 82.5
                        )
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(error = err.message) }
            }
        }
    }

    fun saveConfigSettings(
        electricityRate: Double,
        currency: String,
        distanceUnit: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.saveConfig(
                electricityRate = electricityRate,
                currency = currency,
                distanceUnit = distanceUnit
            )
            result.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        config = state.config?.copy(
                            electricityRate = electricityRate,
                            currency = currency,
                            distanceUnit = distanceUnit
                        ),
                        infoMessage = "Settings saved"
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message) }
            }
        }
    }

    fun saveStorageSettings(storageType: String, limitMb: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.saveStorage(storageType = storageType, limitMb = limitMb)
            result.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        storage = state.storage?.copy(
                            storageType = storageType,
                            limitMb = limitMb
                        ),
                        infoMessage = "Storage settings updated"
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message) }
            }
        }
    }

    fun startTripRecovery() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRecovering = true, recoveryMessage = "Scanning storage for missing trips…") }
            val startRes = repository.startRecovery()
            startRes.onSuccess {
                pollRecovery()
            }.onFailure { err ->
                _uiState.update { it.copy(isRecovering = false, error = err.message) }
            }
        }
    }

    private fun pollRecovery() {
        recoveryPollJob?.cancel()
        recoveryPollJob = viewModelScope.launch {
            while (isActive) {
                delay(2000)
                val statusRes = repository.getRecoveryStatus()
                statusRes.onSuccess { json ->
                    val done = json.optBoolean("done", false)
                    val msg = json.optString("message", "")
                    if (done) {
                        _uiState.update {
                            it.copy(
                                isRecovering = false,
                                recoveryMessage = msg.ifEmpty { "Recovery finished" }
                            )
                        }
                        // Refresh data after recovery
                        loadBootstrap()
                        return@launch
                    } else {
                        _uiState.update {
                            it.copy(recoveryMessage = msg.ifEmpty { "Rebuilding trips…" })
                        }
                    }
                }.onFailure {
                    _uiState.update { state -> state.copy(isRecovering = false) }
                    return@launch
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, infoMessage = null) }
    }
}
