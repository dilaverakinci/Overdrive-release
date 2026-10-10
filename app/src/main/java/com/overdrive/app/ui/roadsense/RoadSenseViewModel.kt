package com.overdrive.app.ui.roadsense

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.overdrive.app.roadsense.config.RoadSenseConfig

open class RoadSenseViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: RoadSenseRepository = RoadSenseRepository()
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "RoadSenseViewModel"
    }

    private val _uiState = MutableStateFlow(RoadSenseUiState())
    val uiState: StateFlow<RoadSenseUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun selectTab(tab: RoadSenseTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val loadedState = repository.loadConfig()
            _uiState.update { current ->
                loadedState.copy(
                    activeTab = current.activeTab,
                    isLoading = false
                )
            }
        }
    }

    fun toggleRoadSenseEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val delta = JSONObject().put("enabled", enabled)
            val ok = repository.saveRoadSenseSection(delta, getApplication())
            if (ok) {
                _uiState.update { it.copy(general = it.general.copy(enabled = enabled)) }
                showBanner(if (enabled) "RoadSense etkinleştirildi" else "RoadSense kapatıldı", false)
            } else {
                showBanner("RoadSense durumu kaydedilemedi", true)
            }
        }
    }

    fun updateDetectionSensitivity(pct: Int) {
        val clampedPct = pct.coerceIn(0, 100)
        val mult = RoadSenseSensitivityUtils.pctToMult(clampedPct)
        _uiState.update {
            it.copy(general = it.general.copy(
                detectionSensitivityPct = clampedPct,
                detectionSensitivityMult = mult
            ))
        }
        viewModelScope.launch {
            val delta = JSONObject().put("detectionSensitivity", mult.toDouble())
            repository.saveRoadSenseSection(delta, getApplication())
        }
    }

    fun toggleCalibrationMode(enabled: Boolean) {
        viewModelScope.launch {
            val delta = JSONObject().put("calibrationMode", enabled)
            val ok = repository.saveRoadSenseSection(delta, getApplication())
            if (ok) {
                _uiState.update { it.copy(general = it.general.copy(calibrationMode = enabled)) }
            } else {
                showBanner("Kalibrasyon modu güncellenemedi", true)
            }
        }
    }

    fun toggleOverlayVisible(visible: Boolean) {
        viewModelScope.launch {
            val delta = JSONObject().put("overlayVisible", visible)
            val ok = repository.saveRoadSenseSection(delta, getApplication())
            if (ok) {
                _uiState.update { it.copy(general = it.general.copy(overlayVisible = visible)) }
                showBanner(if (visible) "Ekran yer paylaşımı açıldı" else "Ekran yer paylaşımı gizlendi", false)
            } else {
                showBanner("Yer paylaşımı görünürlüğü güncellenemedi", true)
            }
        }
    }

    fun saveRouting(endpoint: String, apiKey: String) {
        viewModelScope.launch {
            val cleanEndpoint = endpoint.trim().ifEmpty { "https://api.stadiamaps.com/route/v1" }
            val cleanKey = apiKey.trim()
            if (cleanKey.isEmpty()) {
                showBanner("Lütfen geçerli bir API anahtarı girin", true)
                return@launch
            }
            val ok = repository.saveRouting(cleanEndpoint, cleanKey)
            if (ok) {
                _uiState.update {
                    it.copy(map = it.map.copy(
                        routingConfigured = true,
                        routingEndpoint = cleanEndpoint,
                        hasRoutingKey = true
                    ))
                }
                showBanner("Yönlendirme anahtarı şifrelenerek kaydedildi", false)
            } else {
                showBanner("Yönlendirme kaydedilemedi", true)
            }
        }
    }

    fun clearRouting() {
        viewModelScope.launch {
            val ok = repository.clearRouting()
            if (ok) {
                _uiState.update {
                    it.copy(map = it.map.copy(
                        routingConfigured = false,
                        hasRoutingKey = false
                    ))
                }
                showBanner("Yönlendirme anahtarı silindi", false)
            } else {
                showBanner("Yönlendirme silinemedi", true)
            }
        }
    }

    fun toggleClusterProject(start: Boolean) {
        viewModelScope.launch {
            val result = repository.setClusterProjection(start)
            result.onSuccess { active ->
                _uiState.update { it.copy(map = it.map.copy(clusterProjecting = active)) }
                showBanner(if (active) "Harita göstergeye yansıtılıyor" else "Gösterge yansıtması durduruldu", false)
            }.onFailure { err ->
                showBanner("Gösterge projeksiyonu hatası: ${err.message}", true)
            }
        }
    }

    fun toggleClusterAuto(auto: Boolean) {
        viewModelScope.launch {
            val delta = JSONObject().put("autoProjectCluster", auto)
            val ok = repository.saveNavMapSection(delta)
            if (ok) {
                _uiState.update { it.copy(map = it.map.copy(autoProjectCluster = auto)) }
            } else {
                showBanner("Otomatik projeksiyon ayarı kaydedilemedi", true)
            }
        }
    }

    fun setClusterLayout(layoutCode: Int) {
        viewModelScope.launch {
            val delta = JSONObject().put("clusterSizeProfile", layoutCode)
            val ok = repository.saveBlindSpotSection(delta, getApplication())
            if (ok) {
                _uiState.update {
                    it.copy(
                        map = it.map.copy(clusterLayout = layoutCode),
                        blindSpot = it.blindSpot.copy(clusterLayout = layoutCode)
                    )
                }
            }
        }
    }

    fun toggleWarnEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val delta = JSONObject().put("warnEnabled", enabled)
            val ok = repository.saveRoadSenseSection(delta, getApplication())
            if (ok) {
                _uiState.update { it.copy(warnings = it.warnings.copy(warnEnabled = enabled)) }
                showBanner(if (enabled) "Tehlike uyarıları açıldı" else "Tehlike uyarıları kapatıldı", false)
            }
        }
    }

    fun setWarnMode(mode: WarnMode) {
        viewModelScope.launch {
            val delta = JSONObject().put("warnMode", mode.wireValue)
            val ok = repository.saveRoadSenseSection(delta, getApplication())
            if (ok) {
                _uiState.update { it.copy(warnings = it.warnings.copy(warnMode = mode)) }
            }
        }
    }

    fun setWarnAudioChannel(channel: SoundChannel) {
        viewModelScope.launch {
            val delta = JSONObject().put("warnAudioChannel", channel.wireValue)
            val ok = repository.saveRoadSenseSection(delta, getApplication())
            if (ok) {
                _uiState.update { it.copy(warnings = it.warnings.copy(warnAudioChannel = channel)) }
            }
        }
    }

    fun setWarnAudioVolume(volumePercent: Int) {
        val vol = volumePercent.coerceIn(10, 100)
        _uiState.update { it.copy(warnings = it.warnings.copy(warnAudioVolume = vol)) }
        viewModelScope.launch {
            val delta = JSONObject().put("warnAudioVolume", vol)
            repository.saveRoadSenseSection(delta, getApplication())
        }
    }

    fun testChime() {
        viewModelScope.launch {
            val current = _uiState.value.warnings
            val res = repository.testChime(current.warnAudioChannel.wireValue, current.warnAudioVolume)
            res.onSuccess {
                showBanner("Test zil sesi çalındı (${current.warnAudioChannel.wireValue})", false)
            }.onFailure { err ->
                showBanner("Zil sesi testi başarısız: ${err.message}", true)
            }
        }
    }

    fun setWarnLeadSeconds(seconds: Int) {
        val sec = seconds.coerceIn(2, 8)
        _uiState.update { it.copy(warnings = it.warnings.copy(warnLeadSeconds = sec)) }
        viewModelScope.launch {
            val delta = JSONObject().put("warnLeadSeconds", sec)
            repository.saveRoadSenseSection(delta, getApplication())
        }
    }

    fun setWarnConfidenceThreshold(thresholdPct: Int) {
        val pct = thresholdPct.coerceIn(0, 100)
        val confFloat = pct / 100f
        _uiState.update { it.copy(warnings = it.warnings.copy(warnConfidenceThreshold = pct)) }
        viewModelScope.launch {
            val delta = JSONObject().put("warnConfidenceThreshold", confFloat.toDouble())
            repository.saveRoadSenseSection(delta, getApplication())
        }
    }

    fun toggleSeverityMinor(enabled: Boolean) {
        viewModelScope.launch {
            val delta = JSONObject().put("warnSeverityMinor", enabled)
            val ok = repository.saveRoadSenseSection(delta, getApplication())
            if (ok) {
                _uiState.update { it.copy(warnings = it.warnings.copy(severityMinor = enabled)) }
            }
        }
    }

    fun toggleSeverityModerate(enabled: Boolean) {
        viewModelScope.launch {
            val delta = JSONObject().put("warnSeverityModerate", enabled)
            val ok = repository.saveRoadSenseSection(delta, getApplication())
            if (ok) {
                _uiState.update { it.copy(warnings = it.warnings.copy(severityModerate = enabled)) }
            }
        }
    }

    fun toggleSeveritySevere(enabled: Boolean) {
        viewModelScope.launch {
            val delta = JSONObject().put("warnSeveritySevere", enabled)
            val ok = repository.saveRoadSenseSection(delta, getApplication())
            if (ok) {
                _uiState.update { it.copy(warnings = it.warnings.copy(severitySevere = enabled)) }
            }
        }
    }

    fun toggleCrowdUpload(enabled: Boolean) {
        viewModelScope.launch {
            val delta = JSONObject().put("crowdUpload", enabled)
            val ok = repository.saveRoadSenseSection(delta, getApplication())
            if (ok) {
                _uiState.update { it.copy(data = it.data.copy(crowdUpload = enabled)) }
                showBanner(if (enabled) "Topluluk yüklemesi açıldı" else "Topluluk yüklemesi kapatıldı", false)
            }
        }
    }

    fun toggleCrowdDownload(enabled: Boolean) {
        viewModelScope.launch {
            val delta = JSONObject().put("crowdDownload", enabled)
            val ok = repository.saveRoadSenseSection(delta, getApplication())
            if (ok) {
                _uiState.update { it.copy(data = it.data.copy(crowdDownload = enabled)) }
                showBanner(if (enabled) "Topluluk indirmesi açıldı" else "Topluluk indirmesi kapatıldı", false)
            }
        }
    }

    fun setSyncWorkerUrl(url: String) {
        val clean = url.trim().ifEmpty { RoadSenseConfig.DEFAULT_WORKER_URL }
        viewModelScope.launch {
            val delta = JSONObject().put("syncWorkerUrl", clean)
            val ok = repository.saveRoadSenseSection(delta, getApplication())
            if (ok) {
                _uiState.update { it.copy(data = it.data.copy(syncWorkerUrl = clean)) }
                showBanner("Senkronizasyon adresi kaydedildi", false)
            }
        }
    }

    fun deleteLocalCalibrations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = repository.deleteLocalCalibrations()
            _uiState.update { it.copy(isLoading = false) }
            res.onSuccess { (hazards, labels) ->
                showBanner("Yerel veriler temizlendi: $hazards tehlike, $labels etiket silindi", false)
            }.onFailure { err ->
                showBanner("Yerel veriler silinirken hata: ${err.message}", true)
            }
        }
    }

    fun deleteCloudCalibrations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = repository.deleteCloudCalibrations()
            _uiState.update { it.copy(isLoading = false) }
            res.onSuccess {
                showBanner("Bulut verileriniz başarıyla silindi", false)
            }.onFailure { err ->
                showBanner("Bulut verileri silinirken hata: ${err.message}", true)
            }
        }
    }

    fun toggleBlindSpotEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val delta = JSONObject().put("enabled", enabled)
            val ok = repository.saveBlindSpotSection(delta, getApplication())
            if (ok) {
                _uiState.update { it.copy(blindSpot = it.blindSpot.copy(enabled = enabled)) }
                showBanner(if (enabled) "Kör nokta asistanı açıldı" else "Kör nokta asistanı kapatıldı", false)
            } else {
                showBanner("Kör nokta durumu kaydedilemedi", true)
            }
        }
    }

    fun setBsMergeMode(mode: BsMergeMode) {
        viewModelScope.launch {
            val delta = JSONObject().put("mergeMode", mode.wireValue)
            val ok = repository.saveBlindSpotSection(delta, getApplication())
            if (ok) {
                _uiState.update { it.copy(blindSpot = it.blindSpot.copy(mergeMode = mode)) }
            }
        }
    }

    fun setBsRotation(side: String, rotation: String) {
        viewModelScope.launch {
            val key = if (side.equals("left", true)) "rotationLeft" else "rotationRight"
            val delta = JSONObject().put(key, rotation)
            val ok = repository.saveBlindSpotSection(delta, getApplication())
            if (ok) {
                _uiState.update {
                    if (side.equals("left", true)) {
                        it.copy(blindSpot = it.blindSpot.copy(rotationLeft = rotation))
                    } else {
                        it.copy(blindSpot = it.blindSpot.copy(rotationRight = rotation))
                    }
                }
            }
        }
    }

    fun setBsRectifyStrength(strength: Int) {
        val s = strength.coerceIn(0, 100)
        _uiState.update { it.copy(blindSpot = it.blindSpot.copy(rectifyStrength = s)) }
        viewModelScope.launch {
            val delta = JSONObject().put("rectifyStrength", s)
            repository.saveBlindSpotSection(delta, getApplication())
        }
    }

    fun setBsSpeedRange(minKmh: Int, maxKmh: Int) {
        viewModelScope.launch {
            val lo = minKmh.coerceAtLeast(0)
            val hi = maxKmh.coerceAtLeast(0)
            if (hi in 1 until lo) {
                showBanner("Maksimum hız minimum hızdan küçük olamaz", true)
                return@launch
            }
            val delta = JSONObject().apply {
                put("minSpeedKmh", lo)
                put("maxSpeedKmh", hi)
            }
            val ok = repository.saveBlindSpotSection(delta, getApplication())
            if (ok) {
                _uiState.update {
                    it.copy(blindSpot = it.blindSpot.copy(minSpeedKmh = lo, maxSpeedKmh = hi))
                }
                showBanner("Hız aralığı uygulandı ($lo - $hi km/h)", false)
            } else {
                showBanner("Hız aralığı kaydedilemedi", true)
            }
        }
    }

    fun toggleBsSuppressReverse(suppress: Boolean) {
        viewModelScope.launch {
            val delta = JSONObject().put("suppressInReverse", suppress)
            val ok = repository.saveBlindSpotSection(delta, getApplication())
            if (ok) {
                _uiState.update { it.copy(blindSpot = it.blindSpot.copy(suppressInReverse = suppress)) }
            }
        }
    }

    fun setBsDisplayTarget(target: BsDisplayTarget) {
        viewModelScope.launch {
            val delta = JSONObject().put("target", target.wireValue)
            val ok = repository.saveBlindSpotSection(delta, getApplication())
            if (ok) {
                _uiState.update { it.copy(blindSpot = it.blindSpot.copy(target = target)) }
            }
        }
    }

    fun setBsSizePct(sizePct: Int) {
        val s = sizePct.coerceIn(15, 90)
        _uiState.update { it.copy(blindSpot = it.blindSpot.copy(sizePct = s)) }
        viewModelScope.launch {
            val delta = JSONObject().put("sizePct", s)
            repository.saveBlindSpotSection(delta, getApplication())
        }
    }

    fun setBsCorner(side: String, corner: String) {
        viewModelScope.launch {
            val key = if (side.equals("left", true)) "cornerLeft" else "cornerRight"
            val delta = JSONObject().put(key, corner)
            val ok = repository.saveBlindSpotSection(delta, getApplication())
            if (ok) {
                _uiState.update {
                    if (side.equals("left", true)) {
                        it.copy(blindSpot = it.blindSpot.copy(cornerLeft = corner))
                    } else {
                        it.copy(blindSpot = it.blindSpot.copy(cornerRight = corner))
                    }
                }
            }
        }
    }

    fun setBsAlignment(
        rearFov: Float,
        sideFov: Float,
        yaw: Float,
        roll: Float,
        pitch: Float,
        feather: Float,
        projExp: Float,
        rearRoll: Float,
        rearPitch: Float
    ) {
        viewModelScope.launch {
            val delta = JSONObject().apply {
                put("rearFov", rearFov.toDouble())
                put("sideFov", sideFov.toDouble())
                put("yaw", yaw.toDouble())
                put("roll", roll.toDouble())
                put("pitch", pitch.toDouble())
                put("feather", feather.toDouble())
                put("projExp", projExp.toDouble())
                put("rearRoll", rearRoll.toDouble())
                put("rearPitch", rearPitch.toDouble())
            }
            val ok = repository.saveBlindSpotSection(delta, getApplication())
            if (ok) {
                _uiState.update {
                    it.copy(blindSpot = it.blindSpot.copy(
                        rearFov = rearFov,
                        sideFov = sideFov,
                        yaw = yaw,
                        roll = roll,
                        pitch = pitch,
                        feather = feather,
                        projExp = projExp,
                        rearRoll = rearRoll,
                        rearPitch = rearPitch
                    ))
                }
                showBanner("Hizalama ayarları başarıyla uygulandı", false)
            } else {
                showBanner("Hizalama ayarları kaydedilemedi", true)
            }
        }
    }

    fun resetBsAlignmentDefaults() {
        setBsAlignment(
            rearFov = 1.66f,
            sideFov = 1.98f,
            yaw = 1.23f,
            roll = 0.25f,
            pitch = -0.275f,
            feather = 0.38f,
            projExp = 1.0f,
            rearRoll = 0.0f,
            rearPitch = 0.0f
        )
    }

    fun setBsPreview(view: Int, active: Boolean) {
        viewModelScope.launch {
            val delta = JSONObject().apply {
                put("debugPreview", active)
                if (active) put("debugView", view)
            }
            val ok = repository.saveBlindSpotSection(delta, getApplication())
            if (ok) {
                _uiState.update { it.copy(blindSpot = it.blindSpot.copy(debugPreviewActive = active)) }
            }
        }
    }

    private fun showBanner(msg: String, isError: Boolean) {
        _uiState.update { it.copy(bannerMessage = msg, isBannerError = isError) }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(bannerMessage = null) }
    }
}
