package com.overdrive.app.storage;

import android.os.StatFs;
import android.util.Log;

import java.io.File;
import java.util.Locale;

/**
 * StorageQuotaEngine - Handles StatFs calculations, storage quotas, limit clamping,
 * and byte formatting for Overdrive storage management.
 * Extracted from StorageManager as part of Phase 3 (God Class Refactoring).
 */
public final class StorageQuotaEngine {
    private static final String TAG = "StorageQuotaEngine";

    public static final long VOLUME_HEADROOM_MB = 256;
    public static final long MIN_LIMIT_MB = 100;
    public static final long MAX_LIMIT_MB_FALLBACK = 100_000; // 100GB

    private StorageQuotaEngine() {}

    /**
     * Formats bytes into a human-readable string (B, KB, MB, GB).
     */
    public static String formatSize(long bytes) {
        if (bytes >= 1_000_000_000) {
            return String.format(Locale.US, "%.1f GB", bytes / 1_000_000_000.0);
        } else if (bytes >= 1_000_000) {
            return String.format(Locale.US, "%.1f MB", bytes / 1_000_000.0);
        } else if (bytes >= 1_000) {
            return String.format(Locale.US, "%.1f KB", bytes / 1_000.0);
        }
        return bytes + " B";
    }

    /**
     * Query available space in bytes on the given path using StatFs.
     * Returns 0 if path is invalid, inaccessible, or throws.
     */
    public static long getAvailableBytes(String path) {
        if (path == null) return 0;
        try {
            File dir = new File(path);
            if (!dir.exists() || !dir.isDirectory()) {
                return 0;
            }
            StatFs stat = new StatFs(path);
            return stat.getAvailableBytes();
        } catch (Exception e) {
            Log.w(TAG, "Could not get available bytes for " + path + ": " + e.getMessage());
            return 0;
        }
    }

    /**
     * Query total space in bytes on the given path using StatFs.
     * Returns 0 if path is invalid, inaccessible, or throws.
     */
    public static long getTotalBytes(String path) {
        if (path == null) return 0;
        try {
            File dir = new File(path);
            if (!dir.exists() || !dir.isDirectory()) {
                return 0;
            }
            StatFs stat = new StatFs(path);
            return stat.getTotalBytes();
        } catch (Exception e) {
            Log.w(TAG, "Could not get total bytes for " + path + ": " + e.getMessage());
            return 0;
        }
    }

    /**
     * Calculates usable ceiling in MB given total bytes on a volume and headroom.
     */
    public static long computeVolumeCeilingMb(long totalBytes, long headroomMb, long minLimitMb) {
        if (totalBytes <= 0) return 0;
        long totalMb = totalBytes / (1024L * 1024L);
        return Math.max(minLimitMb, totalMb - headroomMb);
    }

    /**
     * Re-clamps a configured limit down to the ceiling. Shrink-or-hold only.
     */
    public static long reclampTargetMb(long ceilingMb, long currentLimitMb, long minLimitMb) {
        if (ceilingMb <= 0) return currentLimitMb;
        return Math.max(minLimitMb, Math.min(ceilingMb, currentLimitMb));
    }

    /**
     * Calculates runtime effective limit in MB for active storage.
     */
    public static long clampEffectiveLimitMb(long configuredLimitMb, long activeVolumeCeilingMb, long minLimitMb) {
        if (activeVolumeCeilingMb <= 0) return Math.max(minLimitMb, configuredLimitMb);
        return Math.max(minLimitMb, Math.min(configuredLimitMb, activeVolumeCeilingMb));
    }
}
