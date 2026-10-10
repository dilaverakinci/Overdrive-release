package com.overdrive.app.trips;

import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

/**
 * Ensures TripTelemetryRecorder reliably consumes BydDataCollector dynamics,
 * resolves gear safely without UNAVAILABLE sentinels, and falls back to GPS Doppler
 * speed when CAN wheel speed is dead or unavailable.
 */
public class TripTelemetryRecorderDynamicsLinkageContractTest {

    private static final String RECORDER_PATH =
            "app/src/main/java/com/overdrive/app/trips/TripTelemetryRecorder.java";

    @Test
    public void recorderQueriesBydDataCollectorFirst() throws Exception {
        String source = readRepositoryFile(RECORDER_PATH);

        assertTrue("Recorder must consult BydDataCollector.getInstance()",
                source.contains("BydDataCollector.getInstance()"));
        assertTrue("Recorder must read fast dynamics tuple",
                source.contains("byd.getFastDynamics()"));
        assertTrue("Recorder must fall back to live speed read",
                source.contains("byd.readSpeedNowKmh()"));
        assertTrue("Recorder must fall back to live accel read",
                source.contains("byd.readAccelNow()"));
        assertTrue("Recorder must fall back to live brake read",
                source.contains("byd.readBrakeNow()"));
        assertTrue("Recorder must fall back to live gear read",
                source.contains("byd.readGearNow()"));
    }

    @Test
    public void recorderFallsBackToGpsSpeed() throws Exception {
        String source = readRepositoryFile(RECORDER_PATH);

        assertTrue("Recorder must check GPS fix snapshot",
                source.contains("gps.getFixSnapshot()"));
        assertTrue("Recorder must convert fix.speed to km/h",
                source.contains("fix.speed * 3.6f"));
        assertTrue("Recorder must filter stationary GPS drift",
                source.contains("candidateSpeed < 2"));
    }

    @Test
    public void recorderGuardsAgainstUnavailableGear() throws Exception {
        String source = readRepositoryFile(RECORDER_PATH);

        assertTrue("Recorder must retain last known valid gear mode",
                source.contains("lastKnownGearMode"));
        assertTrue("Recorder must fall back to GearMonitor.GEAR_P rather than UNAVAILABLE",
                source.contains("gearMode = GearMonitor.GEAR_P;"));
    }

    private static String readRepositoryFile(String relativePath) throws Exception {
        Path path = Paths.get(relativePath);
        if (!Files.exists(path)) {
            path = Paths.get("..", relativePath);
        }
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
