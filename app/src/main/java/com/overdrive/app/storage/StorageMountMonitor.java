package com.overdrive.app.storage;

import android.os.StatFs;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * StorageMountMonitor - Manages external volume mounting state, lease publication,
 * and path-to-storage-type classification for Overdrive.
 * Extracted from StorageManager as part of Phase 3 (God Class Refactoring).
 */
public final class StorageMountMonitor {
    private static final String TAG = "StorageMountMonitor";

    public static final long SD_MOUNTED_LEASE_MS = 60_000L;

    private StorageMountMonitor() {}

    /**
     * Reads the lease deadline timestamp from a lease file.
     * Returns Long.MIN_VALUE on failure or missing file.
     */
    public static long readSdMountedLeaseDeadline(File lease) {
        if (lease == null || !lease.isFile()) return Long.MIN_VALUE;
        try {
            byte[] raw = Files.readAllBytes(lease.toPath());
            return Long.parseLong(new String(raw, StandardCharsets.US_ASCII).trim());
        } catch (Throwable ignored) {
            return Long.MIN_VALUE;
        }
    }

    /**
     * Writes the lease deadline timestamp atomically with world-readable permissions.
     */
    public static boolean writeSdMountedLease(File leaseFile, long deadlineMs) {
        if (leaseFile == null) return false;
        try {
            byte[] payload = Long.toString(deadlineMs).getBytes(StandardCharsets.US_ASCII);
            File tmp = new File(leaseFile.getAbsolutePath() + ".tmp");
            try (FileOutputStream output = new FileOutputStream(tmp)) {
                output.write(payload);
            }
            if (!tmp.renameTo(leaseFile)) {
                try (FileOutputStream output = new FileOutputStream(leaseFile)) {
                    output.write(payload);
                }
                tmp.delete();
            }
            leaseFile.setReadable(true, false);
            leaseFile.setWritable(true, false);
            return true;
        } catch (Throwable failure) {
            Log.w(TAG, "SD mounted lease publish failed: " + failure.getMessage());
            return false;
        }
    }

    /**
     * Cheap, fork-free mount health check (StatFs + canRead).
     */
    public static boolean isMountLikelyAvailable(String mountPath) {
        if (mountPath == null || mountPath.isEmpty()) return false;
        try {
            File dir = new File(mountPath);
            if (!dir.exists() || !dir.isDirectory() || !dir.canRead()) {
                return false;
            }
            StatFs stat = new StatFs(mountPath);
            return stat.getAvailableBytes() > 0 || stat.getTotalBytes() > 0;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Classifies a file path into "INTERNAL", "SD_CARD", "USB", or null.
     */
    public static String classifyPath(
            String absPath,
            String internalBaseDir,
            String legacyAppFilesDir,
            String sdCardPath,
            String usbPath) {
        if (absPath == null || absPath.isEmpty()) return null;

        if ((internalBaseDir != null && absPath.startsWith(internalBaseDir))
                || (legacyAppFilesDir != null && absPath.startsWith(legacyAppFilesDir))) {
            return "INTERNAL";
        }

        if (sdCardPath != null && !sdCardPath.isEmpty() && absPath.startsWith(sdCardPath)) {
            return "SD_CARD";
        }
        if (usbPath != null && !usbPath.isEmpty() && absPath.startsWith(usbPath)) {
            return "USB";
        }

        if (absPath.startsWith("/storage/emulated")) {
            return "INTERNAL";
        }
        if (absPath.startsWith("/storage/") || absPath.startsWith("/mnt/")) {
            return "SD_CARD";
        }
        return null;
    }
}
