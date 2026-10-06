package com.overdrive.app.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class StorageMountMonitorTest {

    @Test
    public void classifyPathResolvesExactAndFallbackVolumes() {
        String internalBase = "/storage/emulated/0/Overdrive";
        String legacyAppFiles = "/storage/emulated/0/Android/data/com.overdrive.app/files";
        String sdPath = "/storage/1234-5678";
        String usbPath = "/storage/ABCD-EF01";

        assertEquals("INTERNAL", StorageMountMonitor.classifyPath(
                internalBase + "/recordings/rec.mp4", internalBase, legacyAppFiles, sdPath, usbPath));
        assertEquals("INTERNAL", StorageMountMonitor.classifyPath(
                legacyAppFiles + "/recordings/rec.mp4", internalBase, legacyAppFiles, sdPath, usbPath));
        assertEquals("SD_CARD", StorageMountMonitor.classifyPath(
                sdPath + "/recordings/rec.mp4", internalBase, legacyAppFiles, sdPath, usbPath));
        assertEquals("USB", StorageMountMonitor.classifyPath(
                usbPath + "/recordings/rec.mp4", internalBase, legacyAppFiles, sdPath, usbPath));
        assertEquals("INTERNAL", StorageMountMonitor.classifyPath(
                "/storage/emulated/0/Other/rec.mp4", internalBase, legacyAppFiles, sdPath, usbPath));
        assertEquals("SD_CARD", StorageMountMonitor.classifyPath(
                "/storage/9999-9999/recordings/rec.mp4", internalBase, legacyAppFiles, sdPath, usbPath));
        assertNull(StorageMountMonitor.classifyPath(
                "/data/local/tmp/somefile", internalBase, legacyAppFiles, sdPath, usbPath));
    }

    @Test
    public void leaseReadAndWriteCycleWorks() throws Exception {
        Path tempDir = Files.createTempDirectory("lease-test");
        File leaseFile = new File(tempDir.toFile(), "test_lease");

        long deadline = System.currentTimeMillis() + 60_000L;
        boolean ok = StorageMountMonitor.writeSdMountedLease(leaseFile, deadline);
        assertTrue(ok);

        long readDeadline = StorageMountMonitor.readSdMountedLeaseDeadline(leaseFile);
        assertEquals(deadline, readDeadline);
    }
}
