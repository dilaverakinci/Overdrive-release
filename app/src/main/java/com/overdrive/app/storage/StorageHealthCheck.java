package com.overdrive.app.storage;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * StorageHealthCheck - Handles bounded subprocess execution, directory writability probes,
 * and storage health diagnostics for Overdrive.
 * Extracted from StorageManager as part of Phase 3 (God Class Refactoring).
 */
public final class StorageHealthCheck {
    private static final String TAG = "StorageHealthCheck";

    private StorageHealthCheck() {}

    /**
     * Result of bounded process output drain.
     */
    public static final class ProcessLines {
        public final List<String> lines;
        public final boolean complete;
        public final int exitCode;

        public ProcessLines(List<String> lines, boolean complete, int exitCode) {
            this.lines = lines;
            this.complete = complete;
            this.exitCode = exitCode;
        }
    }

    /**
     * Bounded Process.waitFor() - kills the child if it doesn't exit within timeoutMs.
     */
    public static int waitForBounded(Process p, long timeoutMs, String label) {
        if (p == null) return -1;
        try {
            if (p.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
                return p.exitValue();
            }
            Log.w(TAG, label + ": timed out after " + timeoutMs + "ms - killing child");
            p.destroyForcibly();
            try { p.waitFor(500, TimeUnit.MILLISECONDS); } catch (InterruptedException ignored) {}
            return -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            try { p.destroyForcibly(); } catch (Exception ignored) {}
            return -1;
        }
    }

    /**
     * Drain a child process's stdout (and optionally stderr) with a HARD deadline on the whole read,
     * then reap the child.
     */
    public static ProcessLines readProcessLinesBounded(
            Process p, boolean includeStderr, long drainTimeoutMs, String label) {
        if (p == null) {
            return new ProcessLines(Collections.emptyList(), false, -1);
        }
        final List<String> lines = Collections.synchronizedList(new ArrayList<>());
        Thread drain = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) lines.add(line);
            } catch (Exception ignored) {
                // Stream closed by destroyForcibly on timeout, or read error.
            }
            if (includeStderr) {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) lines.add("ERR: " + line);
                } catch (Exception ignored) {}
            }
        }, label + "-drain");
        drain.setDaemon(true);
        drain.start();
        boolean complete = false;
        try {
            drain.join(drainTimeoutMs);
            complete = !drain.isAlive();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        int exit;
        if (!complete) {
            Log.w(TAG, label + ": output drain exceeded " + drainTimeoutMs
                    + "ms - killing child (partial output, " + lines.size() + " lines)");
            try { p.destroyForcibly(); } catch (Exception ignored) {}
            try { drain.join(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            exit = -1;
        } else {
            exit = waitForBounded(p, 2_000, label);
        }
        return new ProcessLines(new ArrayList<>(lines), complete, exit);
    }

    /**
     * Checks whether a directory is genuinely writable by creating and unlinking a probe file.
     */
    public static boolean canWriteToDirectory(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory() || !dir.canWrite()) {
            return false;
        }
        File probe = new File(dir, ".probe_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(probe)) {
            out.write(1);
            out.flush();
            return true;
        } catch (Throwable t) {
            return false;
        } finally {
            try {
                if (probe.exists()) probe.delete();
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Checks if a volume path is accessible and readable as a directory.
     */
    public static boolean isVolumeAccessible(String path) {
        if (path == null) return false;
        try {
            File dir = new File(path);
            return dir.exists() && dir.isDirectory() && dir.canRead();
        } catch (Throwable t) {
            return false;
        }
    }
}
