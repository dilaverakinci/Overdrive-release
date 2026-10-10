package com.overdrive.app.byd.bodywork;

/**
 * Window motion calculation, command planning, and tolerance evaluation engine.
 */
public final class WindowMotionEngine {

    private WindowMotionEngine() {}

    /**
     * Verify that all four side windows (LF, RF, LR, RR) report a valid percentage (0..100).
     */
    public static boolean hasCompleteSideWindowPositionFeedback(int[] positions) {
        if (positions == null || positions.length < 4) return false;
        for (int i = 0; i < 4; i++) {
            if (positions[i] < 0 || positions[i] > 100) return false;
        }
        return true;
    }

    /**
     * Compute directional command (0=STOP, 1=OPEN, 2=CLOSE) toward target percentage.
     * Returns -1 if inputs are out of bounds.
     */
    public static int sideWindowCommandTowardTarget(
            int currentPercent, int targetPercent, int tolerance) {
        if (currentPercent < 0 || currentPercent > 100
                || targetPercent < 0 || targetPercent > 100
                || tolerance < 0) {
            return -1;
        }
        if (Math.abs(currentPercent - targetPercent) <= tolerance) return 0;
        return targetPercent > currentPercent ? 1 : 2;
    }

    /**
     * Plan 4-window command vector toward target percent.
     * Returns null if positions array is invalid or target/tolerance are out of bounds.
     */
    public static int[] planSideWindowCommands(
            int[] positions, int targetPercent, int tolerance) {
        if (!hasCompleteSideWindowPositionFeedback(positions)
                || targetPercent < 0 || targetPercent > 100
                || tolerance < 0) {
            return null;
        }
        int[] commands = new int[4];
        for (int i = 0; i < commands.length; i++) {
            commands[i] = sideWindowCommandTowardTarget(
                    positions[i], targetPercent, tolerance);
        }
        return commands;
    }

    /**
     * Check if a window has reached its target position given movement direction and tolerance.
     */
    public static boolean hasReachedSideWindowTarget(
            int currentPercent, int targetPercent, int direction, int tolerance) {
        if (currentPercent < 0 || currentPercent > 100
                || targetPercent < 0 || targetPercent > 100
                || tolerance < 0) {
            return false;
        }
        if (direction == 1) {
            return currentPercent >= targetPercent - tolerance;
        }
        if (direction == 2) {
            return currentPercent <= targetPercent + tolerance;
        }
        return Math.abs(currentPercent - targetPercent) <= tolerance;
    }
}
