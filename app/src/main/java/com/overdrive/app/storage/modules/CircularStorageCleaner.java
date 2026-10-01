package com.overdrive.app.storage.modules;

import com.overdrive.app.logging.DaemonLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Modular component implementing the Circular Storage Retention Policy for Dashcam and Sentry modes.
 * Safely prunes the oldest unlocked video segments when storage limits are approached.
 */
public class CircularStorageCleaner {

    private static final DaemonLogger logger = DaemonLogger.getInstance("CircularStorageCleaner");

    public static class CleanResult {
        public final int deletedCount;
        public final long freedBytes;
        public final boolean targetReached;

        public CleanResult(int deletedCount, long freedBytes, boolean targetReached) {
            this.deletedCount = deletedCount;
            this.freedBytes = freedBytes;
            this.targetReached = targetReached;
        }
    }

    /**
     * Determines whether a recording file is locked or protected from circular cleanup.
     */
    public boolean isLockedFile(File file) {
        if (file == null) return false;
        String name = file.getName().toLowerCase();
        return name.contains("lock") || name.contains("event") || name.contains("emergency")
                || name.contains("sentry_event") || name.endsWith(".tmp");
    }

    /**
     * Calculates total bytes consumed by all files in the given directory (non-recursive).
     */
    public long calculateDirectoryBytes(File dir) {
        if (dir == null || !dir.isDirectory()) return 0L;
        File[] files = dir.listFiles();
        if (files == null) return 0L;
        long total = 0L;
        for (File f : files) {
            if (f.isFile()) {
                total += f.length();
            }
        }
        return total;
    }

    /**
     * Prunes oldest unlocked media files until total directory size <= targetMaxBytes.
     */
    public CleanResult cleanToQuota(File dir, long targetMaxBytes) {
        if (dir == null || !dir.isDirectory()) {
            return new CleanResult(0, 0L, true);
        }

        long currentSize = calculateDirectoryBytes(dir);
        if (currentSize <= targetMaxBytes) {
            return new CleanResult(0, 0L, true);
        }

        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return new CleanResult(0, 0L, true);
        }

        List<File> candidates = new ArrayList<>();
        for (File f : files) {
            if (f.isFile() && !isLockedFile(f)) {
                candidates.add(f);
            }
        }

        // Sort by last modified ascending (oldest first)
        Collections.sort(candidates, Comparator.comparingLong(File::lastModified));

        int deletedCount = 0;
        long freedBytes = 0L;

        for (File candidate : candidates) {
            long len = candidate.length();
            if (candidate.delete()) {
                deletedCount++;
                freedBytes += len;
                currentSize -= len;
                logger.debug("Deleted oldest file: " + candidate.getName() + " (" + len + " bytes)");
                if (currentSize <= targetMaxBytes) {
                    break;
                }
            }
        }

        return new CleanResult(deletedCount, freedBytes, currentSize <= targetMaxBytes);
    }
}
