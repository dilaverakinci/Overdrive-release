package com.overdrive.app.ui.keymapping

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.overdrive.app.ui.fragment.WebViewFragment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

open class KeyMappingViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: KeyMappingRepository = KeyMappingRepository(application)
) : AndroidViewModel(application) {

    private val _config = MutableStateFlow<KeymapConfig?>(null)
    val config: StateFlow<KeymapConfig?> = _config.asStateFlow()

    private val _activeTab = MutableStateFlow(KeyMappingTab.BINDINGS)
    val activeTab: StateFlow<KeyMappingTab> = _activeTab.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _capturedKeycode = MutableStateFlow<Int?>(null)
    val capturedKeycode: StateFlow<Int?> = _capturedKeycode.asStateFlow()

    private val _clusterSizeProfile = MutableStateFlow(31)
    val clusterSizeProfile: StateFlow<Int> = _clusterSizeProfile.asStateFlow()

    init {
        loadConfig()
        loadApps()
    }

    fun selectTab(tab: KeyMappingTab) {
        _activeTab.value = tab
    }

    fun loadConfig() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _clusterSizeProfile.value = repository.getClusterSizeProfile()
            repository.getConfig().fold(
                onSuccess = {
                    _config.value = it
                    _isLoading.value = false
                },
                onFailure = {
                    _errorMessage.value = "Failed to load key mappings: ${it.message}"
                    _isLoading.value = false
                }
            )
        }
    }

    fun updateClusterSizeProfile(profile: Int) {
        if (profile !in listOf(29, 30, 31)) return
        _clusterSizeProfile.value = profile
        viewModelScope.launch {
            repository.setClusterSizeProfile(profile)
        }
    }

    fun loadApps() {
        viewModelScope.launch {
            repository.getInstalledApps().fold(
                onSuccess = {
                    _installedApps.value = it
                },
                onFailure = {
                    // Non-fatal, app picker can retry
                }
            )
        }
    }

    fun toggleMasterEnabled(enabled: Boolean) {
        val current = _config.value ?: return
        val updated = current.copy(enabled = enabled)
        persist(updated)
    }

    fun toggleAllowAdvanced(enabled: Boolean) {
        val current = _config.value ?: return
        val updated = current.copy(allowAdvanced = enabled)
        persist(updated)
    }

    fun updateDoubleTapWindowMs(ms: Long) {
        val current = _config.value ?: return
        val updated = current.copy(doubleTapWindowMs = ms)
        persist(updated)
    }

    fun toggleBindingEnabled(binding: KeyBinding, enabled: Boolean) {
        val current = _config.value ?: return
        val updatedList = current.bindings.map {
            if (it.keycode == binding.keycode && it.pressType == binding.pressType) {
                it.copy(enabled = enabled)
            } else {
                it
            }
        }
        persist(current.copy(bindings = updatedList))
    }

    fun deleteBinding(binding: KeyBinding) {
        val current = _config.value ?: return
        val updatedList = current.bindings.filterNot {
            it.keycode == binding.keycode && it.pressType == binding.pressType
        }
        persist(current.copy(bindings = updatedList))
    }

    fun saveBinding(binding: KeyBinding, onComplete: ((Boolean) -> Unit)? = null) {
        val current = _config.value ?: KeymapConfig()
        // Replace existing binding for same keycode + pressType, or append
        val exists = current.bindings.any { it.keycode == binding.keycode && it.pressType == binding.pressType }
        val updatedList = if (exists) {
            current.bindings.map {
                if (it.keycode == binding.keycode && it.pressType == binding.pressType) binding else it
            }
        } else {
            current.bindings + binding
        }

        viewModelScope.launch {
            _isSaving.value = true
            repository.saveConfig(current.copy(bindings = updatedList)).fold(
                onSuccess = {
                    _config.value = it
                    _isSaving.value = false
                    _successMessage.value = "Binding saved successfully"
                    _activeTab.value = KeyMappingTab.BINDINGS
                    onComplete?.invoke(true)
                },
                onFailure = {
                    _isSaving.value = false
                    _errorMessage.value = "Failed to save binding: ${it.message}"
                    onComplete?.invoke(false)
                }
            )
        }
    }

    fun testAction(action: KeyAction) {
        viewModelScope.launch {
            repository.fireAction(action).fold(
                onSuccess = {
                    _successMessage.value = "Action triggered successfully"
                },
                onFailure = {
                    _errorMessage.value = "Action execution failed: ${it.message}"
                }
            )
        }
    }

    fun startCapture() {
        _isCapturing.value = true
        _capturedKeycode.value = null
        WebViewFragment.setNativeCapture(true) { keyCode ->
            onKeyCaptured(keyCode)
        }
    }

    fun stopCapture() {
        _isCapturing.value = false
        WebViewFragment.setNativeCapture(false, null)
    }

    fun onKeyCaptured(keyCode: Int) {
        viewModelScope.launch {
            _capturedKeycode.value = keyCode
            _isCapturing.value = false
            WebViewFragment.setNativeCapture(false, null)
            _successMessage.value = "Key captured: Keycode $keyCode"
        }
    }

    fun setManualKeycode(keyCode: Int?) {
        _capturedKeycode.value = keyCode
    }

    private fun persist(updated: KeymapConfig) {
        viewModelScope.launch {
            _isSaving.value = true
            repository.saveConfig(updated).fold(
                onSuccess = {
                    _config.value = it
                    _isSaving.value = false
                },
                onFailure = {
                    _isSaving.value = false
                    _errorMessage.value = "Failed to update configuration: ${it.message}"
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (_isCapturing.value) {
            WebViewFragment.setNativeCapture(false, null)
        }
    }
}
