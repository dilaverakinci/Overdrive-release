package com.overdrive.app.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.Fragment
import com.overdrive.app.BuildConfig
import com.overdrive.app.R
import com.overdrive.app.config.UnifiedConfigManager
import com.overdrive.app.server.DaemonIpcClient
import com.overdrive.app.ui.MainActivity
import com.overdrive.app.ui.component.OverdriveComposeContainer
import com.overdrive.app.ui.theme.OverdriveTheme
import com.overdrive.app.updater.AppUpdater
import org.json.JSONObject
import java.util.concurrent.Executors

class AboutComposeFragment : Fragment() {

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var uiState by mutableStateOf(AboutUiState())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return OverdriveComposeContainer(requireContext()).apply {
            setContent {
                OverdriveTheme {
                    AboutScreen(
                        state = uiState,
                        onCheckForUpdates = {
                            (activity as? MainActivity)?.invokeCheckForUpdates()
                        },
                        onSelectChannel = { channel ->
                            setChannel(channel)
                        },
                        onToggleVinVisibility = {
                            uiState = uiState.copy(isVinRevealed = !uiState.isVinRevealed)
                        },
                        onExportBackup = {
                            exportBackup()
                        },
                        onImportBackup = {
                            importBackup()
                        },
                        onOpenLicense = {
                            openExternal(getString(R.string.settings_about_license_url))
                        },
                        onOpenSource = {
                            openExternal(getString(R.string.settings_about_source_url))
                        },
                        onOpenStar = {
                            openExternal(getString(R.string.settings_about_star_url))
                        },
                        onShare = {
                            shareOverdrive()
                        },
                        onSupport = {
                            openExternal(getString(R.string.settings_about_support_kofi_url))
                        },
                        onRefresh = {
                            loadAllInfo()
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadAllInfo()
    }

    override fun onResume() {
        super.onResume()
        loadAllInfo()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        executor.shutdown()
    }

    private fun loadAllInfo() {
        val installed = AppUpdater.getInstalledVersion()
        uiState = uiState.copy(
            installedVersion = installed,
            buildId = BuildConfig.APPLICATION_ID
        )

        executor.execute {
            // Display version from AppUpdater
            val resolvedVer = AppUpdater.getDisplayVersion(requireContext().applicationContext)

            // Update channel
            UnifiedConfigManager.forceReload()
            val currentChannel = UnifiedConfigManager.getUpdateChannel()

            // Vehicle version info
            val response = DaemonIpcClient.send(
                JSONObject().put("command", "GET_VEHICLE_IDENTITY"),
                2500
            )
            val rawVin = response
                ?.takeIf { it.optBoolean("success", false) }
                ?.optString("vin", "")
                ?.takeIf { it.isNotBlank() }
            val vInfoObj = VehicleVersionInfoProvider.read(rawVin)
            val vInfo = VehicleDisplayInfo(
                vin = vInfoObj.vin,
                firmware = vInfoObj.firmware,
                dsp = vInfoObj.dsp,
                mcu = vInfoObj.mcu,
                android = vInfoObj.android,
                securityPatch = vInfoObj.securityPatch,
                headUnit = vInfoObj.headUnit
            )

            mainHandler.post {
                if (!isAdded) return@post
                uiState = uiState.copy(
                    installedVersion = resolvedVer,
                    updateChannel = currentChannel,
                    vehicleInfo = vInfo
                )
            }
        }
    }

    private fun setChannel(channel: String) {
        uiState = uiState.copy(updateChannel = channel)
        executor.execute {
            val ok = UnifiedConfigManager.setUpdateChannel(channel)
            mainHandler.post {
                if (!isAdded) return@post
                if (ok) {
                    Toast.makeText(requireContext(), "Kanal $channel olarak ayarlandı", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Kanal kaydedilemedi", Toast.LENGTH_SHORT).show()
                    loadAllInfo()
                }
            }
        }
    }

    private fun exportBackup() {
        executor.execute {
            val payload = JSONObject().apply {
                put("command", "EXPORT_CONFIG")
                put("includeTrips", false)
            }
            val res = DaemonIpcClient.send(payload, 5000)
            val success = res?.optBoolean("success", false) ?: false
            mainHandler.post {
                if (!isAdded) return@post
                if (success) {
                    val path = res?.optString("path", "Yedekler klasörü")
                    Toast.makeText(requireContext(), "Yedek başarıyla alındı: $path", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "Yedekleme başlatılamadı (Kamera servisi çalışıyor mu?)", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun importBackup() {
        Toast.makeText(requireContext(), "Yedekten geri yüklemek için lütfen yedek dosyasını Overdrive klasörüne yerleştirin.", Toast.LENGTH_LONG).show()
    }

    private fun openExternal(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (_: Throwable) {
            Toast.makeText(requireContext(), "Bağlantı açılamadı", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareOverdrive() {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, getString(R.string.settings_about_support_share_message))
                type = "text/plain"
            }
            startActivity(Intent.createChooser(sendIntent, getString(R.string.settings_about_support_share_chooser)))
        } catch (_: Throwable) {}
    }
}
