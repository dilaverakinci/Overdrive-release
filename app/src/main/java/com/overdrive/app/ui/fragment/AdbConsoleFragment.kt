package com.overdrive.app.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.overdrive.app.launcher.AdbDaemonLauncher
import com.overdrive.app.ui.adb.AdbConsoleScreen
import com.overdrive.app.ui.adb.AdbConsoleUiState
import com.overdrive.app.ui.component.OverdriveComposeContainer
import com.overdrive.app.ui.theme.OverdriveTheme

/**
 * 100% Jetpack Compose Native Fragment for ADB shell console.
 */
class AdbConsoleFragment : Fragment() {

    private var uiState by mutableStateOf(AdbConsoleUiState())
    private var adbLauncher: AdbDaemonLauncher? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        adbLauncher = AdbDaemonLauncher(requireContext())
        return OverdriveComposeContainer(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OverdriveTheme {
                    AdbConsoleScreen(
                        state = uiState,
                        onCommandChange = { cmd ->
                            uiState = uiState.copy(currentCommand = cmd)
                        },
                        onExecuteClick = { cmd ->
                            executeCommand(cmd)
                        },
                        onClearOutputClick = {
                            clearOutput()
                        },
                        onPresetClick = { preset ->
                            uiState = uiState.copy(currentCommand = preset.command)
                        },
                        onEnableBydAdbClick = {
                            appendOutput("$ ⚡ BYD ADB Aç komutu yürütülüyor...")
                            com.overdrive.app.byd.adb.BydAdbManager.enableWirelessAdbAsync(requireContext()) { success, msg ->
                                activity?.runOnUiThread {
                                    appendOutput(if (success) "✓ $msg" else "⚠ $msg")
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun executeCommand(command: String) {
        appendOutput("$ $command")
        uiState = uiState.copy(isExecuting = true)

        adbLauncher?.executeShellCommand(command, object : AdbDaemonLauncher.LaunchCallback {
            override fun onLog(message: String) {
                activity?.runOnUiThread {
                    appendOutput(message)
                }
            }

            override fun onLaunched() {
                activity?.runOnUiThread {
                    uiState = uiState.copy(isExecuting = false, currentCommand = "")
                }
            }

            override fun onError(error: String) {
                activity?.runOnUiThread {
                    appendOutput("Error: $error")
                    uiState = uiState.copy(isExecuting = false)
                }
            }
        })
    }

    private fun appendOutput(text: String) {
        val updated = "${uiState.outputLog}\n$text"
        uiState = uiState.copy(outputLog = updated)
    }

    private fun clearOutput() {
        uiState = uiState.copy(outputLog = "$ Ready for commands…")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adbLauncher?.releasePerInstanceResources()
        adbLauncher = null
    }
}
