package com.overdrive.app.ui.charging

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
 * 100% Jetpack Compose Native Fragment for Charging and Battery Management.
 * Directly replaces legacy WebViewFragment for /charging.
 */
class ChargingComposeFragment : Fragment() {

    private var uiState by mutableStateOf(ChargingUiState())

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
                    ChargingScreen(
                        state = uiState,
                        onTabSelected = { tab ->
                            uiState = uiState.copy(selectedTab = tab)
                        },
                        onTargetSocChange = { targetSoc ->
                            uiState = uiState.copy(targetSocLimit = targetSoc)
                            showFeedback("Hedef şarj sınırı: %$targetSoc")
                        },
                        onCurrentLimitChange = { amp ->
                            uiState = uiState.copy(targetCurrentLimitA = amp)
                            showFeedback("Maksimum şarj akımı: ${amp}A")
                        },
                        onTogglePortLock = {
                            val next = !uiState.isPortUnlocked
                            uiState = uiState.copy(isPortUnlocked = next)
                            showFeedback(if (next) "Şarj tabanca kilidi çözüldü" else "Şarj tabancası kilitlendi")
                        },
                        onToggleBatteryPreHeat = {
                            val next = !uiState.isBatteryPreHeating
                            uiState = uiState.copy(isBatteryPreHeating = next)
                            showFeedback(if (next) "Batarya ön ısıtma başlatıldı" else "Batarya ön ısıtma durduruldu")
                        },
                    )
                }
            }
        }
    }

    private fun showFeedback(message: String) {
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
    }
}
