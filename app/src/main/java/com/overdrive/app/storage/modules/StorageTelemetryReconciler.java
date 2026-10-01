package com.overdrive.app.storage.modules;

import com.overdrive.app.logging.DaemonLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Modular component for reconciling storage files against database indices,
 * identifying orphaned files or ghost database records, and verifying multi-UID file permissions.
 */
public class StorageTelemetryReconciler {

    private static final DaemonLogger logger = DaemonLogger.getInstance("StorageTelemetryReconciler");

    /**
     * Identifies database records that refer to files that no longer exist on physical storage.
     */
    public List<String> findOrphanedRecords(File storageDir, Set<String> registeredFileNames) {
        List<String> orphaned = new ArrayList<>();
        if (registeredFileNames == null || registeredFileNames.isEmpty()) {
            return orphaned;
        }

        Set<String> actualFiles = new HashSet<>();
        if (storageDir != null && storageDir.isDirectory()) {
            File[] files = storageDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    actualFiles.add(f.getName());
                }
            }
        }

        for (String registered : registeredFileNames) {
            if (!actualFiles.contains(registered)) {
                orphaned.add(registered);
            }
        }

        return orphaned;
    }

    /**
     * Ensures shared read/write access across daemon (UID 2000) and app (UID 10xxx).
     */
    public boolean ensurePermissions(File fileOrDir) {
        if (fileOrDir == null || !fileOrDir.exists()) return false;
        try {
            boolean r = fileOrDir.setReadable(true, false);
            boolean w = fileOrDir.setWritable(true, false);
            boolean x = fileOrDir.isDirectory() ? fileOrDir.setExecutable(true, false) : true;
            return r && w && x;
        } catch (Exception e) {
            logger.debug("Failed setting world permissions: " + e.getMessage());
            return false;
        }
    }
}
