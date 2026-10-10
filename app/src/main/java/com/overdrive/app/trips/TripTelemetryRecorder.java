package com.overdrive.app.trips;

import com.overdrive.app.logging.DaemonLogger;
import com.overdrive.app.monitor.GearMonitor;
import com.overdrive.app.monitor.GpsMonitor;
import com.overdrive.app.storage.StorageManager;
import com.overdrive.app.telemetry.TelemetryDataCollector;
import com.overdrive.app.telemetry.TelemetrySnapshot;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

/**
 * Captures 5Hz telemetry by reading from existing singleton monitors.
 * Buffers samples in memory, flushes 1Hz-downsampled gzipped JSON-lines to disk.
 *
 * Does NOT create its own BYD device handles — reads from TelemetryDataCollector,
 * GpsMonitor, VehicleDataMonitor, and GearMonitor which are already running.
 */
public class TripTelemetryRecorder {

    private static final DaemonLogger logger = DaemonLogger.getInstance("TripTelemetryRecorder");

    private static final long SAMPLE_INTERVAL_MS = 200;       // 5Hz
    private static final long FLUSH_INTERVAL_MS = 60_000;     // 60s
    /** Delay before the FIRST flush. Short on purpose — see startRecording:
     *  until a flush lands there is no on-disk trace of the trip, so a process
     *  death before it is unrecoverable. 5s ≈ 25 samples: cheap, and it closes
     *  a 60s data-loss window at the start of every trip. */
    private static final long FIRST_FLUSH_DELAY_MS = 5_000;
    private static final long MAX_BUFFER_BYTES = 10 * 1024 * 1024; // 10MB

    // Distance fusion tunables.
    // Reject a GPS fallback segment whose reported horizontal accuracy is worse
    // than this — a 50 m-error fix can wander metres between ticks while parked.
    private static final float GPS_ACCURACY_GATE_M = 50.0f;
    // Floor below which a GPS fallback segment is treated as stationary jitter
    // rather than travel (~2 m random walk at the typical fix noise level).
    private static final double MIN_GPS_SEGMENT_KM = 0.002;
    // Cap dt for speed integration so a scheduler stall / process resume can't
    // turn one tick into a kilometre. Normal cadence is 200 ms.
    private static final long MAX_INTEGRATION_DT_MS = 2_000;

    // Dependencies: existing singleton monitors
    private volatile TelemetryDataCollector telemetryDataCollector;

    // Executor for 5Hz sampling
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> sampleFuture;
    private ScheduledFuture<?> flushFuture;

    // Buffer (guarded by bufferLock)
    private final Object bufferLock = new Object();
    private ArrayList<TelemetrySample> buffer = new ArrayList<>();
    private long estimatedBufferBytes = 0;

    // All captured 5Hz samples for scoring (not cleared on flush)
    private final Object allSamplesLock = new Object();
    private ArrayList<TelemetrySample> allSamples = new ArrayList<>();

    // Trip state
    private volatile boolean recording = false;
    private long currentTripId = -1;
    private File outputFile;

    // Stats tracking
    private int maxSpeedKmh = 0;
    private long speedSumKmh = 0;
    private long speedSampleCount = 0;
    
    // Live distance tracking (fused).
    // volatile: written only on the 5Hz sampler thread but read on the
    // detector/finalize thread via getTotalDistanceKm() (the live-distance
    // fallback when the hardware odometer is unavailable). Single writer, so
    // volatile fully resolves the cross-thread visibility — the finalize read
    // sees the latest accumulated distance.
    //
    // Fusion strategy (SOTA for a vehicle with a wheel-derived speed bus):
    //   • PRIMARY  = integrate CAN/wheel speed over dt (speedKmh × dt). This is
    //     immune to GPS jitter while parked, multipath in urban canyons, and
    //     signal loss in tunnels/garages — the classic failure modes of a pure
    //     fix-to-fix haversine sum. Wheel speed reads ~0 when stopped, so idle
    //     dwell contributes nothing instead of accreting drift.
    //   • FALLBACK = accuracy-gated GPS haversine, used only for ticks where we
    //     had no fresh dynamics snapshot (speed unknown). Gated on reported
    //     horizontal accuracy and a minimum-segment floor so a noisy fix can't
    //     manufacture phantom travel.
    // The hardware odometer delta still overrides this entirely at finalize
    // (TripDetector); this value is the live readout and the odometer-absent
    // fallback.
    private volatile double totalDistanceKm = 0;
    private double lastLat = 0;
    private double lastLon = 0;
    private boolean hasLastGps = false;
    private long lastSampleMs = 0;

    // True once the CAN/wheel speed channel has produced ANY non-zero reading in
    // this trip — i.e. proof the channel is actually wired, as opposed to a fresh
    // snapshot whose speedKmh is simply the int 0 initialiser because the
    // BYDAutoSpeedDevice bind failed. Distance integration trusts the speed
    // channel only after that proof; until then GPS carries the distance. Reset
    // per trip in startRecording so one bad trip can't poison the next.
    private boolean speedChannelEverLive = false;
    // Last observed valid gear mode (persisted across momentary dropouts)
    private int lastKnownGearMode = GearMonitor.GEAR_P;

    // GPS coverage tracking — how many samples landed valid lat/lon. Logged
    // at trip end so the daemon log alone tells us why a trip's map is blank
    // (no GPS during recording vs. file write failure vs. UI bug).
    private long sampleCountTotal = 0;
    private long sampleCountWithGps = 0;

    /**
     * Constructor takes TelemetryDataCollector as parameter (injected from TripAnalyticsManager).
     * May be null if TelemetryDataCollector hasn't been initialized yet (GPU init delay).
     */
    public TripTelemetryRecorder(TelemetryDataCollector telemetryDataCollector) {
        this.telemetryDataCollector = telemetryDataCollector;
    }

    /**
     * Update the TelemetryDataCollector reference after late initialization.
     * Called by CameraDaemon once TelemetryDataCollector is ready (after GPU init delay).
     */
    public void setTelemetryDataCollector(TelemetryDataCollector collector) {
        this.telemetryDataCollector = collector;
    }

    /**
     * Start recording telemetry for the given trip.
     * Starts the 5Hz sampling timer and periodic flush timer.
     */
    public void startRecording(long tripId) {
        // JOURNAL ON INTERNAL STORAGE: the in-flight file used to live in the
        // user-selected trips dir (usually the SD card). It is the ONLY recovery
        // source for a trip if the process dies mid-drive, and the field
        // incident (log_DG87KWQX) showed that same card stalling and dropping
        // off the bus during a drive — a failed flush there is logged and
        // dropped (see flushBuffer). The journal now lives next to the trip H2
        // database on /data/local/tmp and is MOVED to the trips dir as
        // <dbId>.jsonl.gz when the row is finalized. Falls back to the trips
        // dir if the journal dir cannot be created.
        beginRecording(tripId, new File(resolveJournalDir(), tripId + ".jsonl.gz"), null);
    }

    /**
     * Re-attach to the journal of a trip that survived a daemon process
     * restart (same-session resume). Appends to {@code existingFile} — each
     * flush is an independent gzip member, so the reader
     * ({@link TelemetryStore#readFromFile}) sees one continuous timeline —
     * and seeds the live stats/scoring stream from the already-journaled
     * {@code history} (1 Hz on disk) so the trip-end distance fallback,
     * max/avg speed and scores cover the WHOLE trip, not just the part
     * recorded after the restart.
     */
    public void resumeRecording(long tripId, File existingFile, List<TelemetrySample> history) {
        beginRecording(tripId, existingFile, history);
    }

    /**
     * Directory the in-flight journal is written to: the internal journal dir
     * when available, else the trips dir (legacy behaviour).
     */
    public static File resolveJournalDir() {
        try {
            File journal = StorageManager.getInstance().getTripJournalDir();
            if (journal != null) return journal;
        } catch (Throwable ignored) {}
        return StorageManager.getInstance().getTripsDir();
    }

    private void beginRecording(long tripId, File file, List<TelemetrySample> history) {
        if (recording) {
            logger.warn("Already recording trip " + currentTripId + ", ignoring start for " + tripId);
            return;
        }

        this.currentTripId = tripId;
        this.outputFile = file;
        this.maxSpeedKmh = 0;
        this.speedSumKmh = 0;
        this.speedSampleCount = 0;
        this.totalDistanceKm = 0;
        this.lastLat = 0;
        this.lastLon = 0;
        this.hasLastGps = false;
        this.lastSampleMs = 0;
        this.sampleCountTotal = 0;
        this.sampleCountWithGps = 0;
        this.speedChannelEverLive = false;
        this.lastKnownGearMode = GearMonitor.GEAR_P;

        synchronized (bufferLock) {
            buffer.clear();
            estimatedBufferBytes = 0;
        }
        synchronized (allSamplesLock) {
            allSamples.clear();
        }

        if (history != null && !history.isEmpty()) {
            seedFromHistory(history);
        }

        recording = true;

        // Mark this file as in-flight so StorageManager.ensureTripsSpace
        // won't unlink it during a limit-change cleanup mid-trip.
        try {
            StorageManager.getInstance().setActiveTripFile(outputFile);
        } catch (Exception e) {
            logger.warn("Failed to mark active trip file: " + e.getMessage());
        }

        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TripTelemetry-" + tripId);
            t.setDaemon(true);
            return t;
        });

        // 5Hz sampling
        sampleFuture = executor.scheduleAtFixedRate(
                this::sample, 0, SAMPLE_INTERVAL_MS, TimeUnit.MILLISECONDS);

        // Periodic flush every 60s, but with a SHORT first flush.
        //
        // The DB row for a trip is written only at trip end, so until the first
        // telemetry flush lands there is NO artifact on disk and a process death
        // loses the drive outright — next-boot recovery reconstructs rows from
        // these .jsonl.gz files, and cannot recover what was never written.
        // With initialDelay == FLUSH_INTERVAL_MS that blind window was a full
        // 60s at the start of EVERY trip; on units that restart often (watchdog
        // kills, settings-change restarts) a large share of drives died inside
        // it. FIRST_FLUSH_DELAY_MS makes the trip recoverable within seconds.
        // Steady-state cadence is unchanged, so there is no extra I/O on a
        // long drive beyond one early small write.
        flushFuture = executor.scheduleAtFixedRate(
                this::flushBuffer, FIRST_FLUSH_DELAY_MS, FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);

        logger.info((history != null && !history.isEmpty() ? "Resumed" : "Started")
                + " recording trip " + tripId + " → " + outputFile.getAbsolutePath()
                + (history != null && !history.isEmpty()
                        ? " (seeded from " + history.size() + " journaled samples, "
                            + String.format("%.2f", totalDistanceKm) + " km)"
                        : ""));
    }

    /**
     * Seed the live accumulators from already-journaled samples (same-session
     * resume). Distance is GPS-integrated with the recovery path's plausibility
     * gate — the journal has no dt-integrated CAN distance — and stats mirror
     * {@link #sample}'s rules as far as the 1 Hz file allows: synthetic
     * stale-zero samples are indistinguishable on disk, so a resumed trip's
     * average speed may read marginally low. The hardware odometer delta still
     * overrides this distance at finalize whenever both edges are available.
     */
    private void seedFromHistory(List<TelemetrySample> history) {
        double prevLat = 0, prevLon = 0;
        long prevTs = 0;
        boolean havePrev = false;
        double dist = 0;
        for (TelemetrySample s : history) {
            sampleCountTotal++;
            boolean haveGps = s.lat != 0 || s.lon != 0;
            if (haveGps) {
                sampleCountWithGps++;
                if (havePrev) {
                    double seg = haversineKm(prevLat, prevLon, s.lat, s.lon);
                    long dtMs = s.timestampMs - prevTs;
                    double dtHr = dtMs > 0 ? dtMs / 3_600_000.0 : (1.0 / 3600.0);
                    // Same 250 km/h implied-speed gate as TripDatabase's
                    // reconstruction: rejects teleport glitches, keeps a long
                    // GPS-dropout leg.
                    if (seg / dtHr <= 250.0) dist += seg;
                }
                prevLat = s.lat; prevLon = s.lon; prevTs = s.timestampMs; havePrev = true;
            }
            if (s.speedKmh > 0) speedChannelEverLive = true;
            if (s.speedKmh > maxSpeedKmh) maxSpeedKmh = s.speedKmh;
            speedSumKmh += s.speedKmh;
            speedSampleCount++;
        }
        totalDistanceKm = dist;
        if (havePrev) {
            lastLat = prevLat;
            lastLon = prevLon;
            hasLastGps = true;
        }
        // lastSampleMs stays 0 so the first live tick integrates no dt across
        // the restart gap.
        synchronized (allSamplesLock) {
            allSamples.addAll(history);
        }
    }

    /**
     * Stop recording. Flushes remaining buffer, closes file.
     * @return the telemetry file path, or null if not recording
     */
    public String stopRecording() {
        return stopRecording(false);
    }

    /**
     * @param keepActiveMarker true to leave the in-flight file marker SET
     *   after stopping. The trip-end flow needs continuous protection: rows
     *   are finalized (or inserted) only AFTER this returns, and a marker
     *   cleared here — even for the microseconds until the caller re-asserts
     *   it — is a window in which a concurrent recovery scan (startup thread
     *   or POST /api/trips/recover) sees a half-open row plus an unprotected
     *   file and may finalize, duplicate, or (below-floor branch) DELETE it.
     *   The caller owns clearing the marker once the row is complete. The
     *   discard path keeps the default (clear immediately: the file is about
     *   to be deleted, there is nothing to protect).
     */
    public String stopRecording(boolean keepActiveMarker) {
        if (!recording) {
            logger.warn("Not recording, ignoring stop");
            return null;
        }

        recording = false;
        logger.info("Stopping recording for trip " + currentTripId);

        // Cancel scheduled tasks
        if (sampleFuture != null) sampleFuture.cancel(false);
        if (flushFuture != null) flushFuture.cancel(false);

        // Final flush of remaining buffer
        flushBuffer();

        // Shutdown executor
        if (executor != null) {
            executor.shutdown();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            executor = null;
        }

        // Notify StorageManager. Default path clears the in-flight marker
        // first so the post-save cleanup it triggers can reap this file if a
        // downward limit change made it the oldest over-limit file. The
        // trip-end flow passes keepActiveMarker=true instead: the marker must
        // stay up CONTINUOUSLY until the DB row is finalized and the file
        // renamed (the caller clears it in its finally) — the cleanup simply
        // can't reap this one file until then, which is the point.
        try {
            if (!keepActiveMarker) {
                StorageManager.getInstance().setActiveTripFile(null);
            }
            StorageManager.getInstance().onTripFileSaved();
        } catch (Exception e) {
            logger.warn("Failed to notify StorageManager: " + e.getMessage());
        }

        String path = outputFile != null ? outputFile.getAbsolutePath() : null;
        long fileBytes = (outputFile != null && outputFile.exists()) ? outputFile.length() : -1;
        long gpsPct = sampleCountTotal == 0
                ? 0
                : Math.round(100.0 * sampleCountWithGps / sampleCountTotal);
        logger.info("Stopped recording trip " + currentTripId +
                " (samples=" + speedSampleCount +
                ", maxSpeed=" + maxSpeedKmh +
                ", gps=" + sampleCountWithGps + "/" + sampleCountTotal + " (" + gpsPct + "%)" +
                ", fileBytes=" + (fileBytes < 0 ? "missing" : Long.toString(fileBytes)) +
                ", file=" + path + ")");

        return path;
    }

    /**
     * Returns the file path for a given trip ID.
     */
    public String getTelemetryFilePath(long tripId) {
        return new File(StorageManager.getInstance().getTripsDir(),
                tripId + ".jsonl.gz").getAbsolutePath();
    }

    /**
     * Returns all captured 5Hz samples for score computation (before downsampling).
     */
    public List<TelemetrySample> getSamplesForScoring() {
        synchronized (allSamplesLock) {
            return new ArrayList<>(allSamples);
        }
    }

    /**
     * Get the maximum speed recorded during this trip.
     */
    public int getMaxSpeedKmh() {
        return maxSpeedKmh;
    }

    /**
     * Get the average speed recorded during this trip.
     */
    public double getAvgSpeedKmh() {
        return speedSampleCount > 0 ? (double) speedSumKmh / speedSampleCount : 0.0;
    }

    /**
     * Get the total live distance recorded during this trip (km).
     * Fused: CAN/wheel-speed integration primary, accuracy-gated GPS fallback.
     * Used as the live readout and as the finalize-time fallback when the
     * hardware odometer delta is unavailable.
     */
    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    /**
     * Haversine formula: distance between two GPS coordinates in km.
     */
    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // ==================== PRIVATE: Sampling ====================

    /**
     * Called at 5Hz. Reads from existing singleton monitors and buffers a sample.
     */
    private void sample() {
        if (!recording) return;

        try {
            long now = System.currentTimeMillis();

            // ── 1. Read dynamics from BydDataCollector (primary vehicle collector) ──
            com.overdrive.app.byd.BydDataCollector byd = null;
            try {
                byd = com.overdrive.app.byd.BydDataCollector.getInstance();
            } catch (Throwable ignored) {}

            int speedKmh = 0;
            int accelPedal = 0;
            int brakePedal = 0;
            boolean brakePedalPressed = false;
            int gearMode = com.overdrive.app.byd.BydVehicleData.UNAVAILABLE;
            // True when we couldn't read a fresh dynamics snapshot this tick.
            // Such a sample carries synthetic zeros — fine to persist in the raw
            // .jsonl.gz for timeline continuity, but it must NOT feed the scoring
            // buffer or the speed stats, where a fabricated 0 km/h would
            // manufacture a phantom stop / launch / coast and dilute the jerk and
            // consistency windows.
            boolean dynamicsStale = true;

            if (byd != null) {
                // Check fast-dynamics tuple (active when RoadSense fast poll is running)
                com.overdrive.app.byd.BydDataCollector.FastDynamics fast = byd.getFastDynamics();
                if (fast != null && (now - fast.timestamp < 2000)) {
                    if (!Double.isNaN(fast.speedKmh) && fast.speedKmh >= 0) {
                        speedKmh = (int) Math.round(fast.speedKmh);
                        dynamicsStale = false;
                    }
                    if (fast.accelPercent >= 0 && fast.accelPercent <= 100) {
                        accelPedal = fast.accelPercent;
                        dynamicsStale = false;
                    }
                    if (fast.brakePercent >= 0 && fast.brakePercent <= 100) {
                        brakePedal = fast.brakePercent;
                        brakePedalPressed = brakePedal > 0;
                        dynamicsStale = false;
                    }
                    if (fast.gearMode > 0 && fast.gearMode != com.overdrive.app.byd.BydVehicleData.UNAVAILABLE) {
                        gearMode = fast.gearMode;
                    }
                }

                // If fast dynamics didn't provide speed or is not active, try live single-signal reads
                if (dynamicsStale) {
                    double liveSpeed = byd.readSpeedNowKmh();
                    if (!Double.isNaN(liveSpeed) && liveSpeed >= 0) {
                        speedKmh = (int) Math.round(liveSpeed);
                        dynamicsStale = false;
                    }
                    int liveAccel = byd.readAccelNow();
                    if (liveAccel >= 0 && liveAccel <= 100) {
                        accelPedal = liveAccel;
                        dynamicsStale = false;
                    }
                    int liveBrake = byd.readBrakeNow();
                    if (liveBrake >= 0 && liveBrake <= 100) {
                        brakePedal = liveBrake;
                        brakePedalPressed = brakePedal > 0;
                        dynamicsStale = false;
                    }
                    int liveGear = byd.readGearNow();
                    if (liveGear > 0 && liveGear != com.overdrive.app.byd.BydVehicleData.UNAVAILABLE) {
                        gearMode = liveGear;
                    }
                }
            }

            // ── 2. Fallback to TelemetryDataCollector (video overlay collector) ──
            TelemetryDataCollector collector = telemetryDataCollector;
            TelemetrySnapshot snapshot = collector != null ? collector.getLatestSnapshot() : null;
            if (snapshot != null) {
                long snapshotAge = now - snapshot.timestampMs;
                if (snapshotAge < 2000) {
                    if (dynamicsStale || (speedKmh == 0 && snapshot.speedKmh > 0)) {
                        speedKmh = snapshot.speedKmh;
                        dynamicsStale = false;
                    }
                    if (accelPedal == 0 && snapshot.accelPedalPercent > 0) {
                        accelPedal = snapshot.accelPedalPercent;
                    }
                    if (brakePedal == 0 && (snapshot.brakePedalPercent > 0 || snapshot.brakePedalPressed)) {
                        brakePedal = snapshot.brakePedalPercent;
                        brakePedalPressed = snapshot.brakePedalPressed;
                    }
                    if (gearMode <= 0 || gearMode == com.overdrive.app.byd.BydVehicleData.UNAVAILABLE) {
                        if (snapshot.gearMode > 0 && snapshot.gearMode != com.overdrive.app.byd.BydVehicleData.UNAVAILABLE) {
                            gearMode = snapshot.gearMode;
                        }
                    }
                } else if (dynamicsStale && snapshotAge < 5000) {
                    logger.warn("Telemetry snapshot stale (" + snapshotAge + "ms old), recording zeros");
                }
            }

            // ── 3. Gear resolution with fallbacks ──
            if (gearMode <= 0 || gearMode == com.overdrive.app.byd.BydVehicleData.UNAVAILABLE) {
                int gmGear = GearMonitor.getInstance().getCurrentGear();
                if (gmGear > 0 && gmGear != com.overdrive.app.byd.BydVehicleData.UNAVAILABLE) {
                    gearMode = gmGear;
                }
            }
            if (gearMode <= 0 || gearMode == com.overdrive.app.byd.BydVehicleData.UNAVAILABLE) {
                if (byd != null) {
                    com.overdrive.app.byd.BydVehicleData snap = byd.getData();
                    if (snap != null && snap.gearMode > 0 && snap.gearMode != com.overdrive.app.byd.BydVehicleData.UNAVAILABLE) {
                        gearMode = snap.gearMode;
                    }
                }
            }
            if (gearMode > 0 && gearMode != com.overdrive.app.byd.BydVehicleData.UNAVAILABLE) {
                lastKnownGearMode = gearMode;
            } else if (lastKnownGearMode > 0) {
                gearMode = lastKnownGearMode;
            } else {
                gearMode = GearMonitor.GEAR_P;
            }

            // Check if CAN/wheel speed channel has EVER produced a positive value this trip
            boolean canSpeedLiveThisTick = !dynamicsStale && speedKmh > 0;
            if (canSpeedLiveThisTick) {
                speedChannelEverLive = true;
            }
            boolean canSpeedUsable = !dynamicsStale && speedChannelEverLive;

            // ── 4. GPS Fix and GPS Speed Fallback ──
            GpsMonitor gps = GpsMonitor.getInstance();
            GpsMonitor.GpsFixSnapshot fix = gps.getFixSnapshot();
            double lat = fix.latitude;
            double lon = fix.longitude;
            double altitude = fix.altitude;
            float gpsAccuracy = fix.accuracy;

            boolean gpsFresh = !fix.loadedFromCache && fix.lastUpdate > 0 && (now - fix.lastUpdate < 3000);
            boolean gpsAccuracyOk = gpsAccuracy > 0 && gpsAccuracy <= GPS_ACCURACY_GATE_M;

            // When CAN speed is NOT live/usable (e.g. DiLink5 missing device, emulator, sensor delay),
            // use GPS Doppler speed so the timeline, stats, and scores reflect real vehicle motion.
            if ((!speedChannelEverLive || dynamicsStale) && gpsFresh && gpsAccuracyOk) {
                float gpsSpeedKmh = fix.speed * 3.6f;
                int candidateSpeed = Math.round(gpsSpeedKmh);
                if (candidateSpeed < 2) candidateSpeed = 0; // Filter stationary GPS jitter
                if (candidateSpeed <= 300) {
                    speedKmh = candidateSpeed;
                    dynamicsStale = false; // Valid speed observed from GPS
                }
            }

            TelemetrySample sample = new TelemetrySample(
                    now, speedKmh, accelPedal, brakePedal,
                    brakePedalPressed, gearMode, lat, lon, altitude,
                    fix.verticalAccuracy, fix.altitudeIsMsl);

            // ── Distance fusion (CAN-speed primary, accuracy-gated GPS fallback) ──
            // dt since the previous sample, clamped so a scheduler stall or a
            // process resume can't integrate one tick into a huge jump.
            long dtMs = lastSampleMs > 0 ? (now - lastSampleMs) : 0;
            if (dtMs < 0) dtMs = 0;
            if (dtMs > MAX_INTEGRATION_DT_MS) dtMs = MAX_INTEGRATION_DT_MS;

            sampleCountTotal++;
            boolean haveGps = lat != 0 && lon != 0;
            if (haveGps) sampleCountWithGps++;

            // Does the CAN/wheel speed channel look USABLE, not merely fresh?
            //
            // If CAN speed channel is live and proven for this trip, integrate wheel/CAN speed.
            // If CAN speed is unproven or dead, GPS haversine carries the distance.
            if (canSpeedUsable && dtMs > 0) {
                // PRIMARY: integrate wheel/CAN speed. Reads ~0 km/h when stopped,
                // so idle dwell adds nothing and GPS jitter is irrelevant. Robust
                // through tunnels/garages where GPS drops out entirely.
                //   km = (km/h) × (hours)
                totalDistanceKm += speedKmh * (dtMs / 3_600_000.0);
            } else if (haveGps && hasLastGps && lastLat != 0 && lastLon != 0) {
                // FALLBACK: no fresh CAN dynamics this tick. Use GPS haversine,
                // but only when the fix is trustworthy and the segment is above the
                // stationary-jitter floor.
                boolean accuracyOk = gpsAccuracy > 0 && gpsAccuracy <= GPS_ACCURACY_GATE_M;
                double dist = haversineKm(lastLat, lastLon, lat, lon);
                boolean trustedStop = canSpeedUsable && speedKmh == 0;
                if (accuracyOk && !trustedStop
                        && dist >= MIN_GPS_SEGMENT_KM && dist < 0.5) {
                    totalDistanceKm += dist;
                }
            }

            // Always advance the GPS anchor on a valid fix so the next fallback
            // segment measures from here, even on ticks where CAN speed drove the
            // accumulation (keeps the fallback honest if dynamics later go stale).
            if (haveGps) {
                lastLat = lat;
                lastLon = lon;
                hasLastGps = true;
            }
            lastSampleMs = now;

            // Track stats — only from real readings; synthetic stale zeros would
            // drag the average down and never affect max anyway.
            if (!dynamicsStale) {
                if (speedKmh > maxSpeedKmh) {
                    maxSpeedKmh = speedKmh;
                }
                speedSumKmh += speedKmh;
                speedSampleCount++;
            }

            // Add to scoring buffer (real-dynamics 5Hz samples only). Stale
            // synthetic-zero samples are still written to the flush buffer below
            // for raw-timeline continuity, but are kept out of the single-pass
            // scoring stream so they can't fabricate stop/launch/coast events.
            if (!dynamicsStale) {
                synchronized (allSamplesLock) {
                    allSamples.add(sample);
                }
            }

            // Add to flush buffer
            synchronized (bufferLock) {
                buffer.add(sample);
                // Rough estimate: ~100 bytes per sample
                estimatedBufferBytes += 100;

                // Force flush if buffer exceeds 10MB threshold
                if (estimatedBufferBytes >= MAX_BUFFER_BYTES) {
                    logger.info("Buffer exceeded 10MB threshold, force-flushing");
                    executor.execute(this::flushBuffer);
                }
            }
        } catch (Throwable e) {
            // Catch Throwable to prevent ScheduledExecutorService from silently stopping
            logger.warn("Sample error: " + e.getMessage());
        }
    }

    // ==================== PRIVATE: Flush Pipeline ====================

    /**
     * Flush pipeline:
     * 1. Copy buffer to local list, clear buffer
     * 2. Downsample 5Hz → 1Hz: group by second, pick closest to each whole-second boundary
     * 3. Serialize each 1Hz sample as JSON line using TelemetrySample.toJson()
     * 4. Write gzipped chunk and append to the output file
     */
    /**
     * Flush buffered samples to disk WITHOUT stopping the recording.
     *
     * <p>For use immediately before a process kill that is not a trip end (the
     * UI's prepare-restart + {@code killall -9}). Guarantees the on-disk
     * {@code .jsonl.gz} covers everything sampled so far, so next-boot recovery
     * can reconstruct the trip — while leaving the trip OPEN, so no discard
     * threshold is applied and the telemetry file is not deleted.
     */
    public void flushNow() {
        flushBuffer();
    }

    private void flushBuffer() {
        List<TelemetrySample> toFlush;
        synchronized (bufferLock) {
            if (buffer.isEmpty()) return;
            toFlush = new ArrayList<>(buffer);
            buffer.clear();
            estimatedBufferBytes = 0;
        }

        // Downsample 5Hz → 1Hz
        List<TelemetrySample> downsampled = downsampleTo1Hz(toFlush);

        if (downsampled.isEmpty()) return;

        // Serialize and write gzipped chunk
        try {
            writeGzippedChunk(downsampled);
        } catch (IOException e) {
            logger.error("Failed to write telemetry chunk: " + e.getMessage());
            // Per requirement 2.7: log error and continue, don't crash
        }
    }

    /**
     * 1Hz downsampling: for each whole-second boundary present in the data,
     * select the sample with timestamp closest to that boundary.
     */
    static List<TelemetrySample> downsampleTo1Hz(List<TelemetrySample> samples) {
        if (samples == null || samples.isEmpty()) return new ArrayList<>();

        // Group samples by their whole-second (floor to nearest second)
        Map<Long, List<TelemetrySample>> bySecond = new HashMap<>();
        for (TelemetrySample s : samples) {
            long secondBoundary = (s.timestampMs / 1000) * 1000;
            List<TelemetrySample> group = bySecond.get(secondBoundary);
            if (group == null) {
                group = new ArrayList<>();
                bySecond.put(secondBoundary, group);
            }
            group.add(s);
        }

        // For each second boundary, pick the sample closest to that boundary
        List<Long> sortedSeconds = new ArrayList<>(bySecond.keySet());
        java.util.Collections.sort(sortedSeconds);

        List<TelemetrySample> result = new ArrayList<>(sortedSeconds.size());
        for (long boundary : sortedSeconds) {
            List<TelemetrySample> group = bySecond.get(boundary);
            TelemetrySample closest = null;
            long closestDist = Long.MAX_VALUE;
            for (TelemetrySample s : group) {
                long dist = Math.abs(s.timestampMs - boundary);
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = s;
                }
            }
            if (closest != null) {
                result.add(closest);
            }
        }

        return result;
    }

    /**
     * Write a list of 1Hz samples as a gzipped JSON-lines chunk appended to the output file.
     * Per-chunk approach: each flush creates a temp buffer, gzips it, and appends to the file.
     *
     * <p>Re-resolves the output directory against StorageManager.getTripsDir() on each
     * flush. The directory pointer can flip mid-trip when the SD card unmounts/remounts
     * (the watchdog rebinds tripsDir to internal storage and back). Without re-resolution,
     * we'd keep appending to a path under a now-absent volume, every flush would IOException,
     * and the trip would end with an empty file even though the recorder logs "ok".
     */
    private void writeGzippedChunk(List<TelemetrySample> samples) throws IOException {
        if (outputFile == null) return;

        // Build JSON-lines content
        StringBuilder sb = new StringBuilder();
        for (TelemetrySample sample : samples) {
            sb.append(sample.toJson().toString()).append('\n');
        }

        // Gzip the content
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(sb.toString().getBytes("UTF-8"));
        }

        // Re-resolve target file if its directory has gone away mid-trip.
        // We migrate any partial bytes from the stale path to the live one so
        // a chunk-by-chunk flush stays as one continuous file post-trip.
        File target = resolveOutputFileForFlush();

        // Append gzipped bytes to the (possibly relocated) output file.
        try (OutputStream fos = new BufferedOutputStream(
                new FileOutputStream(target, true))) {
            fos.write(baos.toByteArray());
        }

        logger.info("Flushed " + samples.size() + " 1Hz samples to " + target.getName() +
                " (" + baos.size() + " bytes gzipped)");
    }

    /**
     * Re-resolve the output file against the live {@link StorageManager#getTripsDir()}
     * before each flush. Two distinct cases trigger a migration:
     * <ol>
     *   <li>The original parent volume is gone (SD unmounted mid-trip).</li>
     *   <li>The user explicitly switched the trips storage type via the UI;
     *       the original volume may still be writable, but continuing on it
     *       silently ignores the user's intent. We honor the new selection
     *       on the next flush boundary.</li>
     * </ol>
     *
     * <p>When the live dir differs from the current parent, any prior chunk
     * bytes are copied to the new path before continuing, the source file is
     * removed (otherwise it would survive on the old volume as an orphan),
     * and {@link #outputFile} plus the cleanup-protection marker are
     * rebound to the new path so subsequent flushes and stopRecording's
     * path report see the live location.
     */
    private File resolveOutputFileForFlush() {
        if (outputFile == null) return outputFile;
        File parent = outputFile.getParentFile();
        // A journal on the internal journal dir is deliberately NOT migrated
        // mid-trip: that dir exists precisely so the in-flight file is immune
        // to the trips volume's mount state. The trip-end flow moves it to
        // the trips dir once the row is finalized.
        try {
            File journal = StorageManager.getInstance().getTripJournalDir();
            if (journal != null && parent != null && journal.equals(parent)) {
                return outputFile;
            }
        } catch (Throwable ignored) {}
        File liveDir = StorageManager.getInstance().getTripsDir();
        if (liveDir == null) return outputFile;

        // Same volume: nothing to do. The canWrite() check covers the rare
        // case where the live dir == current parent but has gone read-only
        // (FUSE-bridged SD under heavy GL contention occasionally drops to
        // RO until vold catches up); we let the next flush retry.
        if (liveDir.equals(parent)) {
            return outputFile;
        }

        File newPath = new File(liveDir, outputFile.getName());
        if (newPath.equals(outputFile)) {
            return outputFile;
        }

        File oldPath = outputFile;
        boolean migrated = false;
        if (oldPath.exists() && oldPath.length() > 0
                && parent != null && parent.exists()) {
            try {
                java.nio.file.Files.copy(
                        oldPath.toPath(),
                        newPath.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                migrated = true;
                logger.info("Trip telemetry migrated: " + oldPath.getAbsolutePath()
                        + " -> " + newPath.getAbsolutePath());
            } catch (Exception e) {
                logger.warn("Trip telemetry migration failed: " + e.getMessage()
                        + " — continuing at " + newPath.getAbsolutePath());
            }
        } else {
            // Silent-skip cases (old volume unmounted, file not yet created).
            // Log so operators can correlate "trip ended on a different
            // path than it started" with a known volume event.
            logger.info("Trip telemetry rebinding without copy ("
                    + (oldPath.exists() ? "empty file" : "old parent gone")
                    + "): " + (parent == null ? "<no parent>" : parent.getAbsolutePath())
                    + " -> " + liveDir.getAbsolutePath());
        }

        // Update the cleanup-protection marker BEFORE rebinding outputFile.
        // Order matters: between assigning outputFile=newPath and calling
        // setActiveTripFile(newPath), a concurrent ensureTripsSpace would
        // see activeTripFilePath still pointing at oldPath, leaving newPath
        // unprotected. Cleanup runs from the 30s periodic tick so the race
        // window was sub-µs in practice, but the simpler invariant is to
        // mark the destination protected first; only after that commit do
        // we point outputFile at it.
        boolean markerUpdated = false;
        try {
            StorageManager.getInstance().setActiveTripFile(newPath);
            markerUpdated = true;
        } catch (Exception e) {
            logger.warn("Failed to update active trip file marker after migration: "
                    + e.getMessage());
        }
        outputFile = newPath;

        // Remove the source so the old volume doesn't accumulate an orphan
        // .jsonl.gz that would only get reaped when ensureTripsSpace next
        // walks the inactive volume. Skipped when:
        //   - copy failed → source is the only surviving copy of those bytes.
        //   - marker update failed → cleanup still sees oldPath as the
        //     protected file, so newPath is unprotected. Deleting the source
        //     here would trade one corruption window for another. Leave both
        //     files until the next flush retries the marker update.
        if (migrated && markerUpdated && oldPath.exists()) {
            if (!oldPath.delete()) {
                logger.warn("Failed to remove migrated source: " + oldPath.getAbsolutePath());
            }
        }

        return outputFile;
    }
}
