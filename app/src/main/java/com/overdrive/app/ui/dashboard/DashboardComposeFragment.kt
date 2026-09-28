package com.overdrive.app.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.overdrive.app.R
import com.overdrive.app.auth.AuthManager
import com.overdrive.app.ui.component.OverdriveComposeContainer
import com.overdrive.app.ui.model.DaemonStatus
import com.overdrive.app.ui.theme.OverdriveTheme
import com.overdrive.app.ui.util.QrCodeGenerator
import com.overdrive.app.ui.viewmodel.DaemonsViewModel
import com.overdrive.app.ui.viewmodel.MainViewModel
import com.overdrive.app.ui.viewmodel.RecordingViewModel
import com.overdrive.app.util.DeviceIdGenerator

/**
 * Modern 100% Jetpack Compose Native implementation of Dashboard Fragment.
 * Backed by identical ViewModel contracts and StateFlow/LiveData sources.
 */
class DashboardComposeFragment : Fragment() {

    private val mainViewModel: MainViewModel by activityViewModels()
    private val daemonsViewModel: DaemonsViewModel by activityViewModels()
    private val recordingViewModel: RecordingViewModel by activityViewModels()

    private var uiState by mutableStateOf(DashboardUiState())
    private var heroState by mutableStateOf(DashboardHeroState())
    private var remoteState by mutableStateOf(DashboardRemoteState())

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
                    DashboardScreen(
                        uiState = uiState,
                        heroState = heroState,
                        remoteState = remoteState,
                        onVehicleCardClick = {
                            // Navigate or show vehicle config
                        },
                        onRecordingsClick = {
                            findNavController().navigate(R.id.recordingsFragment)
                        },
                        onLiveClick = {
                            findNavController().navigate(R.id.liveViewFragment)
                        },
                        onDaemonsClick = {
                            findNavController().navigate(R.id.daemonsFragment)
                        },
                        onTripsClick = {
                            findNavController().navigate(R.id.tripsFragment)
                        },
                        onVehicleControlClick = {
                            findNavController().navigate(R.id.vehicleControlFragment)
                        },
                        onToggleRemoteExpanded = {
                            remoteState = remoteState.copy(isExpanded = !remoteState.isExpanded)
                        },
                        onToggleTokenMask = {
                            remoteState = remoteState.copy(isTokenMasked = !remoteState.isTokenMasked)
                        },
                        onCopyToken = {
                            copyToClipboard(getString(R.string.clip_label_access_code), remoteState.deviceToken)
                            Toast.makeText(requireContext(), R.string.toast_access_code_copied, Toast.LENGTH_SHORT).show()
                        },
                        onCopyUrl = { url ->
                            copyToClipboard(getString(R.string.dashboard_metric_tunnel), url)
                            Toast.makeText(requireContext(), R.string.toast_url_copied_short, Toast.LENGTH_SHORT).show()
                        },
                        onRegenerateToken = {
                            val newToken = AuthManager.regenerateToken() ?: ""
                            remoteState = remoteState.copy(deviceToken = newToken)
                            Toast.makeText(requireContext(), R.string.toast_token_regenerated, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDeviceIdAndAuth()
        observeViewModels()
    }

    private fun initDeviceIdAndAuth() {
        val deviceId = DeviceIdGenerator.generateDeviceId(requireContext())
        val token = AuthManager.getState()?.deviceSecret ?: AuthManager.initialize()?.deviceSecret ?: ""
        remoteState = remoteState.copy(
            deviceId = deviceId,
            deviceToken = token
        )
    }

    private fun observeViewModels() {
        // Observe Daemons
        daemonsViewModel.daemonStates.observe(viewLifecycleOwner) { states ->
            val running = states.values.count { it.status == DaemonStatus.RUNNING }
            val total = states.size
            val daemonsText = getString(R.string.dashboard_daemons_running, running, total)
            heroState = heroState.copy(
                daemonsChipText = daemonsText,
                areDaemonsRunning = running > 0
            )
        }

        // Observe Tunnels
        val updateTunnel: (String?) -> Unit = { _ ->
            val activeUrl = daemonsViewModel.zrokController.tunnelUrl.value
                ?: daemonsViewModel.cloudflaredController.tunnelUrl.value
                ?: daemonsViewModel.tailscaleController.tunnelUrl.value

            val isOnline = !activeUrl.isNullOrEmpty()
            val statusText = if (isOnline) activeUrl!! else getString(R.string.dashboard_tunnel_offline)

            val qrBitmap = if (isOnline) {
                QrCodeGenerator.generate(activeUrl!!, 256)
            } else {
                null
            }

            remoteState = remoteState.copy(
                isOnline = isOnline,
                statusText = statusText,
                activeUrl = activeUrl,
                qrBitmap = qrBitmap
            )

            heroState = heroState.copy(
                tunnelChipText = if (isOnline) getString(R.string.dashboard_tunnel_online) else getString(R.string.dashboard_tunnel_offline),
                isTunnelOnline = isOnline
            )
        }

        daemonsViewModel.zrokController.tunnelUrl.observe(viewLifecycleOwner, updateTunnel)
        daemonsViewModel.cloudflaredController.tunnelUrl.observe(viewLifecycleOwner, updateTunnel)
        daemonsViewModel.tailscaleController.tunnelUrl.observe(viewLifecycleOwner, updateTunnel)

        // Observe Recordings
        recordingViewModel.isRecording.observe(viewLifecycleOwner) { isRec ->
            heroState = heroState.copy(
                isRecordingActive = isRec == true,
                recordingChipText = if (isRec == true) {
                    getString(R.string.dashboard_chip_recording_active)
                } else {
                    getString(R.string.dashboard_chip_recording_idle)
                }
            )
        }

        recordingViewModel.storageInfo.observe(viewLifecycleOwner) { info ->
            val summary = info?.takeIf { it.totalBytes > 0L }?.let {
                DashboardUiState.StorageSummary(
                    usedBytes = it.usedBytes.coerceAtLeast(0L),
                    availableBytes = it.availableBytes.coerceAtLeast(0L),
                    totalBytes = it.totalBytes
                )
            }
            uiState = uiState.copy(
                recordings = DashboardUiState.RecordingState.Ready(
                    todayClipCount = null,
                    storage = summary
                )
            )
        }
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }
}
