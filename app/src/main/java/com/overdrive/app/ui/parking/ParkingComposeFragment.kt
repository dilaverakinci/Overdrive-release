package com.overdrive.app.ui.parking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.overdrive.app.R
import com.overdrive.app.ui.component.OverdriveComposeContainer
import com.overdrive.app.ui.theme.OverdriveTheme

/**
 * 100% Jetpack Compose Native Fragment for Parking Intelligence (Sessions, Stills, OCR & Sentry).
 * Directly replaces legacy WebView /parking for parkingFragment in nav_graph.
 */
class ParkingComposeFragment : Fragment() {

    private val viewModel: ParkingViewModel by viewModels()

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
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OverdriveTheme {
                    ParkingScreen(
                        viewModel = viewModel,
                        onNavigateToRecordings = {
                            try {
                                findNavController().navigate(R.id.recordingsFragment)
                            } catch (_: Throwable) {}
                        }
                    )
                }
            }
        }
    }
}
