package com.overdrive.app.ui.fragment.settings

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
import com.overdrive.app.R
import com.overdrive.app.auth.PinManager
import com.overdrive.app.ui.component.OverdriveComposeContainer
import com.overdrive.app.ui.security.AutoLockOption
import com.overdrive.app.ui.security.SecurityDialogMode
import com.overdrive.app.ui.security.SettingsSecurityScreen
import com.overdrive.app.ui.security.SettingsSecurityUiState
import com.overdrive.app.ui.theme.OverdriveTheme
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Settings → Security & PIN lock (100% Jetpack Compose Native).
 *
 * Hosts the PIN-lock controls backed by [PinManager].
 * Guards [com.overdrive.app.ui.MainActivity] only.
 */
class SettingsSecurityFragment : Fragment() {

    private var executorService: ExecutorService? = null
    private val executor: ExecutorService
        get() = executorService?.takeUnless { it.isShutdown } ?: Executors.newSingleThreadExecutor { r ->
            Thread(r, "SettingsPinIO").apply {
                isDaemon = true
                priority = Thread.NORM_PRIORITY - 1
            }
        }.also { executorService = it }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var uiState by mutableStateOf(SettingsSecurityUiState())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return OverdriveComposeContainer(requireContext()).apply {
            setContent {
                OverdriveTheme {
                    SettingsSecurityScreen(
                        state = uiState,
                        onToggleClick = { onToggleRowTapped() },
                        onChangePinClick = { onChangePinTapped() },
                        onAutoLockClick = { onAutoLockTapped() },
                        onDismissDialog = {
                            uiState = uiState.copy(dialogMode = null, dialogError = null)
                        },
                        onSetPinSubmit = { pin, confirm ->
                            handleSetPinSubmit(pin, confirm)
                        },
                        onDisablePinSubmit = { pin ->
                            handleDisablePinSubmit(pin)
                        },
                        onChangeStep1Submit = { currentPin ->
                            handleChangeStep1Submit(currentPin)
                        },
                        onChangeStep2Submit = { newPin, confirmPin ->
                            handleChangeStep2Submit(newPin, confirmPin)
                        },
                        onSelectAutoLock = { ms ->
                            handleSelectAutoLock(ms)
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAutoLockOptions()
        renderState()
    }

    override fun onResume() {
        super.onResume()
        renderState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        executorService?.shutdownNow()
        executorService = null
    }

    private fun initAutoLockOptions() {
        val options = AUTO_LOCK_OPTIONS.map { (ms, resId) ->
            AutoLockOption(ms, getString(resId))
        }
        uiState = uiState.copy(autoLockOptions = options)
    }

    private fun renderState() {
        executor.execute {
            val enabled = try { PinManager.isEnabled() } catch (t: Throwable) { false }
            val autoLockMs = try { PinManager.getAutoLockMs() } catch (t: Throwable) {
                PinManager.DEFAULT_AUTOLOCK_MS
            }
            mainHandler.post {
                if (!isAdded) return@post
                uiState = uiState.copy(
                    isEnabled = enabled,
                    autoLockMs = autoLockMs,
                    autoLockLabel = autoLockLabel(autoLockMs)
                )
            }
        }
    }

    private fun onToggleRowTapped() {
        executor.execute {
            val enabled = try { PinManager.isEnabled() } catch (t: Throwable) { false }
            mainHandler.post {
                if (!isAdded) return@post
                uiState = if (enabled) {
                    uiState.copy(dialogMode = SecurityDialogMode.DISABLE_PIN, dialogError = null)
                } else {
                    uiState.copy(dialogMode = SecurityDialogMode.SET_PIN, dialogError = null)
                }
            }
        }
    }

    private fun onChangePinTapped() {
        uiState = uiState.copy(dialogMode = SecurityDialogMode.CHANGE_PIN_STEP1, dialogError = null)
    }

    private fun onAutoLockTapped() {
        uiState = uiState.copy(dialogMode = SecurityDialogMode.AUTO_LOCK, dialogError = null)
    }

    private fun handleSetPinSubmit(pin: String, confirm: String) {
        val err = validatePinPair(pin, confirm)
        if (err != null) {
            uiState = uiState.copy(dialogError = getString(err))
            return
        }

        uiState = uiState.copy(isLoading = true, dialogError = null)
        executor.execute {
            val result = PinManager.setPin(pin)
            mainHandler.post {
                if (!isAdded) return@post
                uiState = uiState.copy(isLoading = false)
                when (result) {
                    PinManager.SetResult.OK -> {
                        context?.let {
                            Toast.makeText(it, R.string.settings_security_toast_set_ok, Toast.LENGTH_SHORT).show()
                        }
                        uiState = uiState.copy(dialogMode = null, dialogError = null)
                        renderState()
                    }
                    else -> {
                        uiState = uiState.copy(dialogError = getString(R.string.settings_security_error_persist))
                    }
                }
            }
        }
    }

    private fun handleDisablePinSubmit(pin: String) {
        if (pin.isBlank()) {
            uiState = uiState.copy(dialogError = getString(R.string.settings_security_error_wrong))
            return
        }

        uiState = uiState.copy(isLoading = true, dialogError = null)
        executor.execute {
            val verify = PinManager.verify(pin)
            val disabled = if (verify == PinManager.VerifyResult.OK) PinManager.disable() else false
            mainHandler.post {
                if (!isAdded) return@post
                uiState = uiState.copy(isLoading = false)
                when (verify) {
                    PinManager.VerifyResult.OK -> {
                        if (disabled) {
                            context?.let {
                                Toast.makeText(it, R.string.settings_security_toast_disable_ok, Toast.LENGTH_SHORT).show()
                            }
                            uiState = uiState.copy(dialogMode = null, dialogError = null)
                            renderState()
                        } else {
                            uiState = uiState.copy(dialogError = getString(R.string.settings_security_error_persist))
                        }
                    }
                    PinManager.VerifyResult.WRONG -> {
                        uiState = uiState.copy(dialogError = getString(R.string.settings_security_error_wrong))
                    }
                    PinManager.VerifyResult.LOCKED_OUT -> {
                        uiState = uiState.copy(dialogError = getString(R.string.settings_security_error_locked_out))
                    }
                    PinManager.VerifyResult.NOT_ENABLED -> {
                        uiState = uiState.copy(dialogMode = null, dialogError = null)
                        renderState()
                    }
                }
            }
        }
    }

    private fun handleChangeStep1Submit(currentPin: String) {
        if (currentPin.isBlank()) {
            uiState = uiState.copy(dialogError = getString(R.string.settings_security_error_wrong))
            return
        }

        uiState = uiState.copy(isLoading = true, dialogError = null)
        executor.execute {
            val result = PinManager.verify(currentPin)
            mainHandler.post {
                if (!isAdded) return@post
                uiState = uiState.copy(isLoading = false)
                when (result) {
                    PinManager.VerifyResult.OK -> {
                        uiState = uiState.copy(dialogMode = SecurityDialogMode.CHANGE_PIN_STEP2, dialogError = null)
                    }
                    PinManager.VerifyResult.WRONG -> {
                        uiState = uiState.copy(dialogError = getString(R.string.settings_security_error_wrong))
                    }
                    PinManager.VerifyResult.LOCKED_OUT -> {
                        uiState = uiState.copy(dialogError = getString(R.string.settings_security_error_locked_out))
                    }
                    PinManager.VerifyResult.NOT_ENABLED -> {
                        uiState = uiState.copy(dialogMode = null, dialogError = null)
                        renderState()
                    }
                }
            }
        }
    }

    private fun handleChangeStep2Submit(newPin: String, confirmPin: String) {
        val err = validatePinPair(newPin, confirmPin)
        if (err != null) {
            uiState = uiState.copy(dialogError = getString(err))
            return
        }

        uiState = uiState.copy(isLoading = true, dialogError = null)
        executor.execute {
            val result = PinManager.setPin(newPin)
            mainHandler.post {
                if (!isAdded) return@post
                uiState = uiState.copy(isLoading = false)
                when (result) {
                    PinManager.SetResult.OK -> {
                        context?.let {
                            Toast.makeText(it, R.string.settings_security_toast_set_ok, Toast.LENGTH_SHORT).show()
                        }
                        uiState = uiState.copy(dialogMode = null, dialogError = null)
                        renderState()
                    }
                    else -> {
                        uiState = uiState.copy(dialogError = getString(R.string.settings_security_error_persist))
                    }
                }
            }
        }
    }

    private fun handleSelectAutoLock(ms: Long) {
        uiState = uiState.copy(autoLockMs = ms, autoLockLabel = autoLockLabel(ms), dialogMode = null)
        executor.execute {
            PinManager.setAutoLockMs(ms)
        }
    }

    private fun autoLockLabel(ms: Long): String {
        val match = AUTO_LOCK_OPTIONS.firstOrNull { it.first == ms }
        return getString(match?.second ?: R.string.settings_security_autolock_5min)
    }

    private fun validatePinPair(pin: String, confirm: String): Int? {
        if (pin.length < PinManager.MIN_PIN_LEN || pin.length > PinManager.MAX_PIN_LEN) {
            return R.string.settings_security_error_length
        }
        if (!pin.all(Char::isDigit)) {
            return R.string.settings_security_error_numeric
        }
        if (pin != confirm) {
            return R.string.settings_security_error_mismatch
        }
        return null
    }

    private companion object {
        private val AUTO_LOCK_OPTIONS = listOf(
            0L to R.string.settings_security_autolock_immediate,
            60_000L to R.string.settings_security_autolock_1min,
            300_000L to R.string.settings_security_autolock_5min,
            900_000L to R.string.settings_security_autolock_15min,
            -1L to R.string.settings_security_autolock_never
        )
    }
}
