package com.overdrive.app.storage.modules;

import android.os.StatFs;
import com.overdrive.app.logging.DaemonLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Modular component responsible for discovering storage volumes, mount points,
 * FUSE emulated storage, SD cards, and USB OTG drives.
 */
public class VolumeMountDetector {

    private static final DaemonLogger logger = DaemonLogger.getInstance("VolumeMountDetector");

    public static final String INTERNAL_PATH = "/storage/emulated/0";

    public static class StorageVolumeInfo {
        public final String path;
        public final String description;
        public final boolean isRemovable;
        public final boolean isMounted;
        public final long totalBytes;
        public final long freeBytes;

        public StorageVolumeInfo(
                String path,
                String description,
                boolean isRemovable,
                boolean isMounted,
                long totalBytes,
                long freeBytes) {
            this.path = path;
            this.description = description;
            this.isRemovable = isRemovable;
            this.isMounted = isMounted;
            this.totalBytes = totalBytes;
            this.freeBytes = freeBytes;
        }
    }

    /**
     * Detects mounted external storage paths (SD card or USB) from /proc/mounts.
     */
    public List<String> detectMountedVolumes() {
        List<String> paths = new ArrayList<>();
        File mounts = new File("/proc/mounts");
        if (!mounts.exists() || !mounts.canRead()) {
            return paths;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(mounts))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    String mountPoint = parts[1];
                    if (isExternalStorageMount(mountPoint)) {
                        paths.add(mountPoint);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Failed reading /proc/mounts: " + e.getMessage());
        }
        return paths;
    }

    public boolean isExternalStorageMount(String mountPoint) {
        if (mountPoint == null) return false;
        return (mountPoint.startsWith("/storage/") && !mountPoint.contains("emulated") && !mountPoint.contains("self"))
                || mountPoint.startsWith("/mnt/media_rw/");
    }

    public long getAvailableBytes(File dir) {
        if (dir == null || !dir.exists()) return 0L;
        try {
            StatFs stat = new StatFs(dir.getAbsolutePath());
            return stat.getAvailableBytes();
        } catch (Exception e) {
            return 0L;
        }
    }

    public long getTotalBytes(File dir) {
        if (dir == null || !dir.exists()) return 0L;
        try {
            StatFs stat = new StatFs(dir.getAbsolutePath());
            return stat.getTotalBytes();
        } catch (Exception e) {
            return 0L;
        }
    }

    public boolean isWritable(File dir) {
        if (dir == null) return false;
        try {
            if (!dir.exists()) {
                dir.mkdirs();
            }
            return dir.canWrite();
        } catch (Exception e) {
            return false;
        }
    }
}
