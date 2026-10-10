package com.overdrive.app.ui.roadsense

import android.content.Context
import android.util.Log
import com.overdrive.app.config.UnifiedConfigManager
import com.overdrive.app.navmap.ClusterMapProjector
import com.overdrive.app.navmap.NavMapConfig
import com.overdrive.app.roadsense.config.RoadSenseConfig
import com.overdrive.app.roadsense.overlay.BlindSpotControl
import com.overdrive.app.roadsense.overlay.RoadSenseOverlayService
import com.overdrive.app.util.DaemonHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter

open class RoadSenseRepository {

    companion object {
        private const val TAG = "RoadSenseRepository"
        private const val SECTION_ROADSENSE = "roadSense"
        private const val SECTION_NAVMAP = "navMap"
        private const val SECTION_BLINDSPOT = "blindspot"
    }

    open suspend fun loadConfig(): RoadSenseUiState = withContext(Dispatchers.IO) {
        try {
            val rsSnapshot = RoadSenseConfig.snapshot(forceReload = true)
            val navMapConfig = NavMapConfig.fromUnifiedConfig()
            val navMapJson = UnifiedConfigManager.loadConfig().optJSONObject(SECTION_NAVMAP) ?: JSONObject()
            val bsJson = UnifiedConfigManager.loadConfig().optJSONObject(SECTION_BLINDSPOT) ?: JSONObject()

            val isProjecting = try {
                ClusterMapProjector.isActive()
            } catch (e: Throwable) {
                false
            }

            val general = GeneralConfig(
                enabled = rsSnapshot.enabled,
                detectionSensitivityMult = rsSnapshot.detectionSensitivity,
                detectionSensitivityPct = RoadSenseSensitivityUtils.multToPct(rsSnapshot.detectionSensitivity),
                calibrationMode = rsSnapshot.calibrationMode,
                overlayVisible = rsSnapshot.overlayVisible
            )

            val map = MapConfig(
                routingConfigured = navMapConfig.isRoutingConfigured,
                routingEndpoint = navMapConfig.routingEndpoint,
                hasRoutingKey = navMapConfig.routingApiKey.isNotBlank(),
                clusterProjecting = isProjecting,
                autoProjectCluster = navMapJson.optBoolean("autoProjectCluster", false),
                clusterLayout = bsJson.optInt("clusterSizeProfile", 31)
            )

            val warnings = WarningsConfig(
                warnEnabled = rsSnapshot.warnEnabled,
                warnMode = when (rsSnapshot.warnMode) {
                    RoadSenseConfig.WarnMode.VISUAL -> WarnMode.VISUAL
                    RoadSenseConfig.WarnMode.AUDIO -> WarnMode.AUDIO
                    RoadSenseConfig.WarnMode.BOTH -> WarnMode.BOTH
                },
                warnAudioChannel = SoundChannel.fromWire(rsSnapshot.warnAudioChannel),
                warnAudioVolume = rsSnapshot.warnAudioVolume,
                warnLeadSeconds = rsSnapshot.warnLeadSeconds.toInt().coerceIn(2, 8),
                warnConfidenceThreshold = Math.round(rsSnapshot.warnConfidenceThreshold * 100).coerceIn(0, 100),
                severityMinor = rsSnapshot.severityMinor,
                severityModerate = rsSnapshot.severityModerate,
                severitySevere = rsSnapshot.severitySevere
            )

            val data = DataConfig(
                crowdUpload = rsSnapshot.crowdUpload,
                crowdDownload = rsSnapshot.crowdDownload,
                syncWorkerUrl = rsSnapshot.syncWorkerUrl ?: RoadSenseConfig.DEFAULT_WORKER_URL
            )

            val blindSpot = BlindSpotConfig(
                enabled = bsJson.optBoolean("enabled", false),
                mergeMode = BsMergeMode.fromWire(bsJson.optString("mergeMode", "both")),
                rotationLeft = bsJson.optString("rotationLeft", "0"),
                rotationRight = bsJson.optString("rotationRight", "0"),
                rectifyStrength = bsJson.optInt("rectifyStrength", 0),
                minSpeedKmh = bsJson.optInt("minSpeedKmh", 0),
                maxSpeedKmh = bsJson.optInt("maxSpeedKmh", 0),
                suppressInReverse = bsJson.optBoolean("suppressInReverse", false),
                target = BsDisplayTarget.fromWire(bsJson.optString("target", "head_unit")),
                clusterLayout = bsJson.optInt("clusterSizeProfile", 31),
                sizePct = bsJson.optInt("sizePct", 40),
                cornerLeft = bsJson.optString("cornerLeft", "tr"),
                cornerRight = bsJson.optString("cornerRight", "tr"),
                rearFov = bsJson.optDouble("rearFov", 1.66).toFloat(),
                sideFov = bsJson.optDouble("sideFov", 1.98).toFloat(),
                yaw = bsJson.optDouble("yaw", 1.23).toFloat(),
                roll = bsJson.optDouble("roll", 0.25).toFloat(),
                pitch = bsJson.optDouble("pitch", -0.275).toFloat(),
                feather = bsJson.optDouble("feather", 0.38).toFloat(),
                projExp = bsJson.optDouble("projExp", 1.0).toFloat(),
                rearRoll = bsJson.optDouble("rearRoll", 0.0).toFloat(),
                rearPitch = bsJson.optDouble("rearPitch", 0.0).toFloat(),
                debugPreviewActive = bsJson.optBoolean("debugPreview", false)
            )

            RoadSenseUiState(
                general = general,
                map = map,
                warnings = warnings,
                data = data,
                blindSpot = blindSpot
            )
        } catch (e: Exception) {
            Log.w(TAG, "loadConfig failed: ${e.message}", e)
            RoadSenseUiState(
                bannerMessage = "Ayarlar yüklenirken hata oluştu: ${e.message}",
                isBannerError = true
            )
        }
    }

    open suspend fun saveRoadSenseSection(delta: JSONObject, context: Context?): Boolean = withContext(Dispatchers.IO) {
        try {
            val ok = UnifiedConfigManager.updateSection(SECTION_ROADSENSE, delta)
            if (ok && context != null) {
                try {
                    val snapshot = RoadSenseConfig.snapshot(forceReload = true)
                    context.mainExecutor.execute {
                        RoadSenseOverlayService.syncWithConfig(context, snapshot.overlayShouldShow())
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Failed to sync RoadSense overlay service: ${e.message}")
                }
            }
            ok
        } catch (e: Exception) {
            Log.w(TAG, "saveRoadSenseSection failed: ${e.message}", e)
            false
        }
    }

    open suspend fun saveBlindSpotSection(delta: JSONObject, context: Context?): Boolean = withContext(Dispatchers.IO) {
        try {
            val ok = UnifiedConfigManager.updateSection(SECTION_BLINDSPOT, delta)
            if (ok && context != null) {
                try {
                    BlindSpotControl.sync(context)
                } catch (e: Throwable) {
                    Log.w(TAG, "Failed to sync BlindSpotControl: ${e.message}")
                }
            }
            ok
        } catch (e: Exception) {
            Log.w(TAG, "saveBlindSpotSection failed: ${e.message}", e)
            false
        }
    }

    open suspend fun saveNavMapSection(delta: JSONObject): Boolean = withContext(Dispatchers.IO) {
        try {
            UnifiedConfigManager.updateSection(SECTION_NAVMAP, delta)
        } catch (e: Exception) {
            Log.w(TAG, "saveNavMapSection failed: ${e.message}", e)
            false
        }
    }

    open suspend fun saveRouting(endpoint: String, apiKey: String): Boolean = withContext(Dispatchers.IO) {
        try {
            NavMapConfig.saveRouting(endpoint, apiKey)
            true
        } catch (e: Exception) {
            Log.w(TAG, "saveRouting failed: ${e.message}", e)
            false
        }
    }

    open suspend fun clearRouting(): Boolean = withContext(Dispatchers.IO) {
        try {
            NavMapConfig.clearRouting()
            true
        } catch (e: Exception) {
            Log.w(TAG, "clearRouting failed: ${e.message}", e)
            false
        }
    }

    open suspend fun setClusterProjection(start: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (start) {
                ClusterMapProjector.start()
            } else {
                ClusterMapProjector.stop()
            }
            Result.success(ClusterMapProjector.isActive())
        } catch (e: Exception) {
            Log.w(TAG, "setClusterProjection failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    open suspend fun testChime(channel: String, volumePercent: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("severity", "severe")
                put("channel", channel)
                put("volumePercent", volumePercent)
            }.toString()

            val conn = DaemonHttpClient.open("/api/roadsense/test-chime", "POST", 3000, 3000)
            conn.doOutput = true
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
            val code = conn.responseCode
            val resp = if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }
            conn.disconnect()

            val json = if (resp.isNotBlank()) JSONObject(resp) else JSONObject()
            if (json.optBoolean("success", false) || code == 200) {
                Result.success(true)
            } else {
                Result.failure(Exception(json.optString("error", "HTTP $code")))
            }
        } catch (e: Exception) {
            Log.w(TAG, "testChime error: ${e.message}")
            Result.failure(e)
        }
    }

    open suspend fun deleteLocalCalibrations(): Result<Pair<Long, Int>> = withContext(Dispatchers.IO) {
        try {
            val conn = DaemonHttpClient.open("/api/roadsense/delete-local", "POST", 5000, 5000)
            val code = conn.responseCode
            val resp = if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }
            conn.disconnect()

            val json = JSONObject(resp)
            if (json.optBoolean("success", false) || code == 200) {
                val hazards = json.optLong("hazardsDeleted", 0)
                val labels = json.optInt("labelsDeleted", 0)
                Result.success(Pair(hazards, labels))
            } else {
                Result.failure(Exception(json.optString("error", "HTTP $code")))
            }
        } catch (e: Exception) {
            Log.w(TAG, "deleteLocalCalibrations error: ${e.message}")
            Result.failure(e)
        }
    }

    open suspend fun deleteCloudCalibrations(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val conn = DaemonHttpClient.open("/api/roadsense/delete-cloud", "POST", 5000, 5000)
            val code = conn.responseCode
            val resp = if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }
            conn.disconnect()

            val json = JSONObject(resp)
            if (json.optBoolean("success", false) || code == 200) {
                Result.success(true)
            } else {
                Result.failure(Exception(json.optString("error", "HTTP $code")))
            }
        } catch (e: Exception) {
            Log.w(TAG, "deleteCloudCalibrations error: ${e.message}")
            Result.failure(e)
        }
    }
}
