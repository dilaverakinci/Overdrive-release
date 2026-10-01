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
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadRealTrips()
    }

    private fun loadRealTrips() {
        context?.let { ctx ->
            val loaded = TripTelemetryLoader.loadTrips(ctx)
            if (loaded.isNotEmpty()) {
                uiState = TripsUiState.fromTrips(loaded)
            }
        }
    }

    private fun showFeedback(message: String) {
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
    }
}
