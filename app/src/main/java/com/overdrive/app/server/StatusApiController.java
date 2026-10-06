package com.overdrive.app.server;

import com.overdrive.app.daemon.CameraDaemon;
import com.overdrive.app.genai.GenAiConfig;
import com.overdrive.app.monitor.AccMonitor;
import com.overdrive.app.monitor.BatteryMonitor;
import com.overdrive.app.monitor.BatterySocData;
import com.overdrive.app.monitor.ChargingStateData;
import com.overdrive.app.monitor.DrivingRangeData;
import com.overdrive.app.monitor.GpsMonitor;
import com.overdrive.app.monitor.NetworkMonitor;
import com.overdrive.app.monitor.SocHistoryDatabase;
import com.overdrive.app.monitor.VehicleDataMonitor;
import com.overdrive.app.surveillance.GpuSurveillancePipeline;
import com.overdrive.app.surveillance.H264ByteRingBuffer;
import com.overdrive.app.surveillance.HardwareEventRecorderGpu;
import com.overdrive.app.surveillance.SafeLocationManager;
import com.overdrive.app.updater.AppUpdater;

import org.json.JSONObject;

import java.io.OutputStream;

/**
 * Controller for /status endpoint and vehicle status JSON assembly.
 * Extracted from HttpServer as part of Phase 3 (God Class Refactoring).
 */
public final class StatusApiController {

    private StatusApiController() {}

    /**
     * Builds and sends the full status JSON response over the provided OutputStream.
     */
    public static void sendStatus(OutputStream out) throws Exception {
        JSONObject status = buildStatusJson();
        HttpResponse.sendJson(out, status.toString());
    }

    /**
     * Assembles the complete vehicle, daemon, camera, and network status snapshot.
     */
    public static JSONObject buildStatusJson() {
        JSONObject status = new JSONObject();
        try {
            status.put("status", "ok");
            status.put("deviceId", CameraDaemon.getDeviceId());
            try {
                status.put(
                    "screenshotPrivacyMode",
                    com.overdrive.app.config.UnifiedConfigManager.isScreenshotPrivacyModeEnabled()
                );
            } catch (Exception ignored) {
                // A status response must remain available even if unified config
                // is temporarily unreadable during a cross-process replacement.
                status.put("screenshotPrivacyMode", false);
            }
            status.put("genAiDashboardEnabled",
                    GenAiConfig.isDashboardPresentationEnabled());

            // Vehicle-data readiness, surfaced explicitly so the web UI can render
            // a "waiting for vehicle…" state instead of silently leaving every
            // field blank when the BYD binders haven't bound yet.
            boolean vehicleReady = waitForVehicleDataReady(1500);
            status.put("vehicleDataReady", vehicleReady);

            // App version — the installed GitHub release label.
            status.put("appVersion", AppUpdater.getDisplayVersionFromFile());
            status.put("recording", TcpCommandServer.getRecordingCameras());
            status.put("viewing", TcpCommandServer.getViewOnlyCameras());
            status.put("active", TcpCommandServer.getActiveCameras());
            status.put("streaming", TcpCommandServer.getStreamingCameras());
            status.put("available", TcpCommandServer.getAvailableCameras());
            status.put("battery", BatteryMonitor.getBatteryInfo());
            status.put("acc", AccMonitor.isAccOn());
            
            // Safe zone status
            SafeLocationManager safeMgr = SafeLocationManager.getInstance();
            status.put("safeZoneSuppressed", CameraDaemon.isSafeZoneSuppressed());
            status.put("inSafeZone", safeMgr.isInSafeZone());
            if (safeMgr.getCurrentZoneName() != null) {
                status.put("safeZoneName", safeMgr.getCurrentZoneName());
            }
            
            // Vehicle data (charging state and power)
            com.overdrive.app.charging.ChargingApiHandler.LivePublication chargingPublication = null;
            status.put("charging", buildIdleChargingStatus());
            try {
                VehicleDataMonitor vehicleMonitor = VehicleDataMonitor.getInstance();
                chargingPublication =
                    com.overdrive.app.charging.ChargingApiHandler
                        .readLivePublication(SocHistoryDatabase.getInstance());
                status.put("charging", chargingPublication.toStatusJson());
                
                BatterySocData socData = vehicleMonitor.getBatterySoc();
                if (socData != null) {
                    JSONObject soc = new JSONObject();
                    soc.put("percent", socData.socPercent);
                    soc.put("isLow", socData.isLow);
                    soc.put("isCritical", socData.isCritical);
                    soc.put("status", socData.getStatus());
                    status.put("soc", soc);
                }
                
                DrivingRangeData rangeData = vehicleMonitor.getDrivingRange();
                if (rangeData != null) {
                    JSONObject range = new JSONObject();
                    range.put("elecRangeKm", rangeData.elecRangeKm);
                    range.put("fuelRangeKm", rangeData.fuelRangeKm);
                    range.put("totalRangeKm", rangeData.totalRangeKm);
                    range.put("isLow", rangeData.isLow);
                    range.put("isCritical", rangeData.isCritical);
                    range.put("status", rangeData.getStatus());
                    if (rangeData.hasFuelPercent()) {
                        range.put("fuelPercent", rangeData.fuelPercent);
                    }
                    range.put("isPhev", vehicleMonitor.isPhev());
                    status.put("range", range);
                }

                // Distance unit preference — "km" or "mi"
                try {
                    com.overdrive.app.byd.BydDataCollector collector =
                            com.overdrive.app.byd.BydDataCollector.getInstance();
                    status.put("distanceUnit", (collector != null && collector.isMilesMode()) ? "mi" : "km");
                } catch (Exception ignored) {
                    status.put("distanceUnit", "km");
                }

                // Tyre pressure display unit — "kpa" | "psi" | "bar"
                try {
                    status.put("pressureUnit",
                            com.overdrive.app.config.UnifiedConfigManager.getTyrePressureUnit());
                } catch (Exception ignored) {
                    status.put("pressureUnit", "psi");
                }

                // Active UI locale
                try {
                    status.put("locale", LocaleManager.get());
                } catch (Exception ignored) {
                    status.put("locale", "en");
                }
            } catch (Exception e) {
                CameraDaemon.log("status: vehicle data block failed: " + e);
                status.put("vehicleDataError", e.getClass().getSimpleName() + ": " + e.getMessage());
            }

            try {
                JSONObject soh = new JSONObject();
                boolean hasSoh = false;
                
                SocHistoryDatabase socDb = SocHistoryDatabase.getInstance();
                com.overdrive.app.abrp.SohEstimator sohEst = socDb != null ? socDb.getSohEstimator() : null;
                double canonicalSoh = sohEst != null ? sohEst.getDisplaySoh() : -1;
                if (canonicalSoh > 0) {
                    soh.put("percent", Math.round(canonicalSoh * 10) / 10.0);
                    soh.put("estimatedCapacityKwh", Math.round(sohEst.getEstimatedCapacityKwh() * 10) / 10.0);
                    soh.put("nominalCapacityKwh", sohEst.getNominalCapacityKwh());
                    hasSoh = true;
                }
                
                if (hasSoh) status.put("soh", soh);
            } catch (Exception e) {
                // SOH not available
            }
            
            // GPU surveillance status
            GpuSurveillancePipeline pipeline = CameraDaemon.getGpuPipeline();
            status.put("gpuSurveillance", pipeline != null && pipeline.isSurveillanceMode());
            
            // Recording mode details
            try {
                JSONObject recordingStatus = new JSONObject();
                com.overdrive.app.recording.RecordingModeManager rmm = CameraDaemon.getRecordingModeManager();
                if (rmm != null) {
                    recordingStatus.put("configuredMode", rmm.getCurrentMode().name());
                    recordingStatus.put("isRecording", pipeline != null && pipeline.isRecording());
                    recordingStatus.put("pipelineRunning", pipeline != null && pipeline.isRunning());
                    recordingStatus.put("gear", com.overdrive.app.recording.RecordingModeManager.gearToString(rmm.getCurrentGear()));
                    recordingStatus.put("accOn", rmm.isAccOn());
                    recordingStatus.put("modeActive", rmm.isModeActive());
                    recordingStatus.put("wedged", rmm.isRecordingWedged());
                    int gmRetries = rmm.getGearMonitorRetryFailures();
                    if (gmRetries > 0) {
                        recordingStatus.put("gearMonitorRetryFailures", gmRetries);
                    }
                } else {
                    recordingStatus.put("configuredMode", "UNKNOWN");
                    recordingStatus.put("isRecording", false);
                    recordingStatus.put("pipelineRunning", false);
                    recordingStatus.put("modeActive", false);
                }

                if (pipeline != null) {
                    recordingStatus.put("camViewActive", pipeline.isCamViewActive());
                    recordingStatus.put("camViewTarget", pipeline.getCamViewTargetString());
                    GpuSurveillancePipeline.CloseLabels closeLabels = pipeline.getCloseLabels();
                    recordingStatus.put("bsCardShowing", closeLabels.bsCardShowing);
                    recordingStatus.put("bsCardTarget", pipeline.getBsTargetString());
                    recordingStatus.put("laneTransitioning", closeLabels.laneTransitioning);
                    int[] laneRect = pipeline.getLaneGeomRect();
                    if (laneRect != null) {
                        JSONObject lr = new JSONObject();
                        lr.put("x", laneRect[0]); lr.put("y", laneRect[1]);
                        lr.put("w", laneRect[2]); lr.put("h", laneRect[3]);
                        recordingStatus.put("laneRect", lr);
                    }
                }
                status.put("recordingStatus", recordingStatus);
            } catch (Exception e) {
                // Recording status not available
            }

            // Instant-replay lifecycle
            try {
                status.put("replay",
                        com.overdrive.app.recording.ManualClipService.getInstance().statusJson());
            } catch (Exception e) {
                // Replay status not available
            }

            // Trip analytics status
            try {
                JSONObject tripStatus = new JSONObject();
                com.overdrive.app.trips.TripAnalyticsManager tam = CameraDaemon.getTripAnalyticsManager();
                if (tam != null) {
                    tripStatus.put("enabled", tam.isEnabled());
                    tripStatus.put("tripActive", tam.isTripActive());
                    com.overdrive.app.trips.TripRecord activeTrip = tam.getActiveTrip();
                    if (activeTrip != null) {
                        tripStatus.put("tripStartTime", activeTrip.startTime);
                        tripStatus.put("tripDurationSec", (System.currentTimeMillis() - activeTrip.startTime) / 1000);
                    }
                } else {
                    tripStatus.put("enabled", false);
                    tripStatus.put("tripActive", false);
                }
                status.put("tripStatus", tripStatus);
            } catch (Exception e) {
                // Trip status not available
            }
            
            // GPS location
            GpsMonitor gps = GpsMonitor.getInstance();
            status.put("gps", gps.getLocationJson());
            
            // Network info
            status.put("network", NetworkMonitor.getNetworkInfo());

            // Pre-record buffer health
            try {
                GpuSurveillancePipeline preRecPipeline = CameraDaemon.getGpuPipeline();
                if (preRecPipeline != null) {
                    HardwareEventRecorderGpu enc = preRecPipeline.getEncoder();
                    if (enc != null) {
                        JSONObject preRec = new JSONObject();
                        boolean preRecOn = enc.isPreRecordEnabled();
                        preRec.put("preRecordEnabled", preRecOn);
                        H264ByteRingBuffer ring = enc.getPreRecordBuffer();
                        if (ring != null) {
                            preRec.put("currentSeconds", Math.round(ring.getDurationSeconds() * 10) / 10.0);
                            preRec.put("currentMB", Math.round(ring.storedBytes() / (1024.0 * 1024.0) * 10) / 10.0);
                            preRec.put("maxSeconds", ring.getMaxDurationUs() / 1_000_000L);
                            preRec.put("packetCount", ring.size());
                            preRec.put("totalAdds", ring.getTotalAdds());
                            preRec.put("totalEvictions", ring.getTotalEvictions());
                            preRec.put("totalKeyDrops", ring.getTotalKeyDrops());
                            preRec.put("totalPDrops", ring.getTotalPDrops());
                            preRec.put("stats", ring.getStats());
                        } else {
                            boolean ooMd = enc.isPreRecordAllocFailed();
                            preRec.put("degraded", ooMd);
                            preRec.put("reason", ooMd
                                ? "byte-ring allocation failed at boot — pre-record disabled this session"
                                : "stream-only encoder (no pre-record by design)");
                        }
                        status.put("preRecord", preRec);
                    }
                }
            } catch (Exception e) {
                // Pre-record stats are diagnostic
            }

            // Revalidate charging publication
            if (chargingPublication != null
                    && chargingPublication.hasPositivePresentation()
                    && !chargingPublication.isStillCurrent()) {
                chargingPublication =
                    com.overdrive.app.charging.ChargingApiHandler
                        .readLivePublication(SocHistoryDatabase.getInstance());
                status.put("charging", chargingPublication.toStatusJson());
            }

        } catch (Exception e) {
            CameraDaemon.log("StatusApiController.buildStatusJson failed: " + e.getMessage());
        }
        return status;
    }

    public static boolean resolveChargingPlugged(
            boolean charging,
            ChargingStateData.ChargingStatus status,
            int gunState,
            boolean vtolCharging) {
        return com.overdrive.app.charging.ChargingApiHandler.resolvePlugged(
                charging, status, gunState, vtolCharging);
    }

    public static boolean resolveChargingActive(
            boolean fusedCharging, boolean taperCharging,
            ChargingStateData.ChargingStatus status,
            int gunState, boolean vtolCharging) {
        if (gunState == 1 || gunState == 5 || vtolCharging
                || status == ChargingStateData.ChargingStatus.DISCHARGING) {
            return false;
        }
        if (status == ChargingStateData.ChargingStatus.READY
                || status == ChargingStateData.ChargingStatus.FINISHED
                || status == ChargingStateData.ChargingStatus.TERMINATED
                || status == ChargingStateData.ChargingStatus.TIMEOUT
                || status == ChargingStateData.ChargingStatus.ERROR) {
            return status == ChargingStateData.ChargingStatus.FINISHED && taperCharging;
        }
        return fusedCharging;
    }

    public static boolean resolveChargingFull(
            ChargingStateData.ChargingStatus status,
            boolean taperCharging, int gunState, boolean vtolCharging) {
        if (gunState == 1 || gunState == 5
                || vtolCharging || taperCharging
                || status == ChargingStateData.ChargingStatus.DISCHARGING) {
            return false;
        }
        return status == ChargingStateData.ChargingStatus.FINISHED;
    }

    public static JSONObject buildIdleChargingStatus() {
        JSONObject charging = new JSONObject();
        try {
            charging.put("stateName", "Unavailable");
            charging.put("status", "UNKNOWN");
            charging.put("chargingPowerKW", 0);
            charging.put("isDischarging", false);
            charging.put("isError", false);
            charging.put("isEstimated", false);
            charging.put("powerSource", "none");
            charging.put("powerObservedAtMs", 0);
            charging.put("powerQuality", ChargingStateData.PowerQuality.UNKNOWN.name());
            charging.put("powerConfidence", 0);
            charging.put("charging", false);
            charging.put("plugged", false);
            charging.put("full", false);
            charging.put("fault", false);
            charging.put("powerKw", 0);
        } catch (Exception ignored) {}
        return charging;
    }

    public static boolean waitForVehicleDataReady(long maxWaitMs) {
        try {
            com.overdrive.app.byd.BydDataCollector collector =
                com.overdrive.app.byd.BydDataCollector.getInstance();
            if (collector != null && collector.isInitialized()) {
                return true;
            }
            long deadline = System.currentTimeMillis() + maxWaitMs;
            while (System.currentTimeMillis() < deadline) {
                Thread.sleep(50);
                if (collector != null && collector.isInitialized()) {
                    return true;
                }
            }
            return false;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
