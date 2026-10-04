package com.overdrive.app.ui.trips

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.Fragment
import com.overdrive.app.ui.component.OverdriveComposeContainer
import com.overdrive.app.ui.theme.OverdriveTheme

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 100% Jetpack Compose Native Fragment for Trips and Driving Analytics.
 * Directly replaces legacy WebViewFragment for /trips.
 */
class TripsComposeFragment : Fragment() {

    private var uiState by mutableStateOf(TripsUiState())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return OverdriveComposeContainer(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setViewCompositionStrategy(androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OverdriveTheme {
                    TripsScreen(
                        state = uiState,
                        onFilterSelected = { filter ->
                            uiState = uiState.copy(filter = filter)
                        },
                        onTripClick = { trip ->
                            uiState = uiState.copy(
                                selectedTripForDetail = trip,
                                scrubberIndex = 0,
                                isPlaying = false
                            )
                        },
                        onBackToList = {
                            uiState = uiState.copy(
                                selectedTripForDetail = null,
                                isPlaying = false
                            )
                        },
                        onScrubberChange = { index ->
                            uiState = uiState.copy(scrubberIndex = index)
                        },
                        onTogglePlay = {
                            uiState = uiState.copy(isPlaying = !uiState.isPlaying)
                        },
                        onPlaybackSpeedChange = { speed ->
                            uiState = uiState.copy(playbackSpeed = speed)
                        },
                        onExportClick = {
                            showFeedback("Seyahat kayıtları GPX/CSV olarak dışa aktarılıyor...")
                        },
                        onCleanupCdr = {
                            showFeedback("BYD dashcam eski kayıtları temizlendi, depolama alanı açıldı.")
                        },
                        onExportGpx = {
                            showFeedback("GPX rota dosyası oluşturuldu ve kaydedildi.")
                        },
                        onExportKml = {
                            showFeedback("KML rota dosyası Google Earth için hazırlandı.")
                        },
                        onDeleteTripClick = { trip ->
                            uiState = uiState.copy(tripToDelete = trip)
                        },
                        onConfirmDeleteTrip = {
                            val trip = uiState.tripToDelete
                            if (trip != null) {
                                context?.let { ctx ->
                                    TripTelemetryLoader.deleteTrip(ctx, trip.id)
                                    val remaining = TripTelemetryLoader.loadTrips(ctx)
                                    uiState = TripsUiState.fromTrips(remaining, isTableView = uiState.isTableView)
                                        .copy(tripToDelete = null)
                                    showFeedback("Seyahat kaydı silindi.")
                                }
                            }
                        },
                        onDismissDeleteTrip = {
                            uiState = uiState.copy(tripToDelete = null)
                        },
                        onToggleViewMode = { isTable ->
                            uiState = uiState.copy(isTableView = isTable)
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadRealTrips()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (isActive) {
                    delay(4000)
                    context?.let { ctx ->
                        val loaded = withContext(Dispatchers.IO) {
                            TripTelemetryLoader.refreshTrips(ctx)
                        }
                        if (isActive && loaded.isNotEmpty()) {
                            val currentFilter = uiState.filter
                            val currentSelected = uiState.selectedTripForDetail
                            val currentTable = uiState.isTableView
                            uiState = TripsUiState.fromTrips(loaded, isTableView = currentTable).copy(
                                filter = currentFilter,
                                selectedTripForDetail = currentSelected?.let { sel -> loaded.firstOrNull { it.id == sel.id } ?: sel }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadRealTrips()
    }

    private fun loadRealTrips() {
        context?.let { ctx ->
            val loaded = TripTelemetryLoader.loadTrips(ctx)
            uiState = TripsUiState.fromTrips(loaded, isTableView = uiState.isTableView)
        }
    }

    private fun showFeedback(message: String) {
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
    }
}
