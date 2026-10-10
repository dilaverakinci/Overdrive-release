package com.overdrive.app.storage;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * RetentionPolicyEngine - Handles file retention policies, candidate sorting,
 * name/stem matching, and reaping decisions for Overdrive storage management.
 * Extracted from StorageManager as part of Phase 3 (God Class Refactoring).
 */
public final class RetentionPolicyEngine {

    public static final int REAP_NONE = 0;
    public static final int REAP_DEFER = 1;
    public static final int REAP_BOUNDED = 2;
    public static final int REAP_FULL = 3;

    private RetentionPolicyEngine() {}

    /**
     * Checks if a filename matches either the primary category prefix or any auxiliary prefix.
     */
    public static boolean nameMatchesCategoryPrefix(String name, String primaryPrefix, String[] auxPrefixes) {
        if (name == null) return false;
        if (primaryPrefix != null && name.startsWith(primaryPrefix)) return true;
        if (auxPrefixes != null) {
            for (String aux : auxPrefixes) {
                if (name.startsWith(aux)) return true;
            }
        }
        return false;
    }

    /**
     * Strip the primary extension from a file name, leaving the stem used to match sidecars.
     * Handles compound extensions like ".jsonl.gz".
     */
    public static String stemForName(String fileName, String primaryExt) {
        if (fileName == null) return "";
        if (primaryExt != null && fileName.endsWith(primaryExt)) {
            return fileName.substring(0, fileName.length() - primaryExt.length());
        }
        return fileName;
    }

    /**
     * Find the last "_a<digit>" actor-id marker in a thumb filename.
     */
    public static int lastIndexOfActorMarker(String name, int from) {
        if (name == null) return -1;
        for (int i = name.length() - 2; i >= from; i--) {
            if (name.charAt(i) != '_') continue;
            if (i + 1 >= name.length() || name.charAt(i + 1) != 'a') continue;
            if (i + 2 < name.length() && Character.isDigit(name.charAt(i + 2))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Sorts files strictly oldest-first by lastModified timestamp.
     */
    public static void sortOldestFirst(List<File> files) {
        if (files == null || files.size() <= 1) return;
        Collections.sort(files, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
    }

    /**
     * Evaluates cleanup action when checking category limits.
     *
     * @param encoderWriting whether media encoder is currently active
     * @param currentBytes current category usage in bytes
     * @param limitBytes configured limit in bytes
     * @param diskCritical whether physical volume is in emergency space deficit
     * @return REAP_NONE, REAP_DEFER, REAP_BOUNDED, or REAP_FULL
     */
    public static int evaluateReapDecision(
            boolean encoderWriting, long currentBytes, long limitBytes, boolean diskCritical) {
        if (diskCritical) {
            return REAP_FULL;
        }
        if (limitBytes <= 0 || currentBytes <= limitBytes * 0.9) {
            return REAP_NONE;
        }
        if (!encoderWriting) {
            return REAP_FULL;
        }
        // Encoder writing: >5% over cap -> hard reap, otherwise bounded trim
        boolean hardOver = currentBytes > (limitBytes * 21 / 20);
        return hardOver ? REAP_FULL : REAP_BOUNDED;
    }
}
