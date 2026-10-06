package com.overdrive.app.byd.tyre;

import com.overdrive.app.byd.BydVehicleData;

import java.util.Locale;

/**
 * Tire pressure, leak severity evaluation, unit formatting, and status normalization engine.
 */
public final class TyreAlarmEngine {

    private TyreAlarmEngine() {}

    public static final int SEVERITY_NO_DATA = -1;
    public static final int SEVERITY_NORMAL = 0;
    public static final int SEVERITY_WARN = 1;
    public static final int SEVERITY_CRITICAL = 2;

    /**
     * Check if a DiLink 5 register value represents an unavailable rail (negative, 65534, or 65535).
     */
    public static boolean isDiLink5UnavailableRail(int value) {
        return value < 0 || value == 65534 || value == 65535;
    }

    /**
     * Map DiLink 5 leak state (0=normal, 1=fast, 2=slow) to canonical Overdrive values (0=normal, 1=slow, 2=fast).
     */
    public static int normalizeDiLink5TyreLeakState(int raw) {
        switch (raw) {
            case 0: return 0;
            case 1: return 2;
            case 2: return 1;
            default: return BydVehicleData.UNAVAILABLE;
        }
    }

    /**
     * Normalize DiLink 5 tyre status rail sentinels.
     */
    public static int normalizeDiLink5TyreStatus(int raw) {
        return !isDiLink5UnavailableRail(raw) ? raw : BydVehicleData.UNAVAILABLE;
    }

    /**
     * Format a tyre pressure reading in the given unit ("kpa", "bar", "psi").
     */
    public static String formatTyrePressure(int kPa, String unit, String kpaLabel, String barLabel, String psiLabel) {
        if (unit == null) unit = "psi";
        switch (unit.toLowerCase(Locale.US)) {
            case "kpa":
                return kPa + " " + kpaLabel;
            case "bar":
                return String.format(Locale.US, "%.2f", kPa / 100.0) + " " + barLabel;
            case "psi":
            default:
                return String.format(Locale.US, "%.1f", kPa * 0.1450377) + " " + psiLabel;
        }
    }

    /**
     * Evaluate pressure severity for a single corner.
     * Returns: 2 (CRITICAL), 1 (WARN), 0 (NORMAL), or -1 (NO DATA).
     */
    public static int evaluateCornerPressureSeverity(
            int kPa, int pState, int lowKpa, int highKpa, int critLowKpa) {
        if (kPa <= 0 && (pState == BydVehicleData.UNAVAILABLE || pState == -1)) {
            return SEVERITY_NO_DATA;
        }
        if (kPa > 0 && kPa <= critLowKpa) {
            return SEVERITY_CRITICAL;
        }
        if ((pState != 0 && pState != BydVehicleData.UNAVAILABLE && pState != -1)
                || (kPa > 0 && (kPa < lowKpa || kPa > highKpa))) {
            return SEVERITY_WARN;
        }
        return SEVERITY_NORMAL;
    }

    /**
     * Evaluate air leak severity for a single corner.
     * Returns: 2 (CRITICAL/FAST), 1 (WARN/SLOW), 0 (NORMAL), or -1 (NO DATA).
     */
    public static int evaluateCornerLeakSeverity(int leak) {
        switch (leak) {
            case 2: return SEVERITY_CRITICAL;
            case 1: return SEVERITY_WARN;
            case 0: return SEVERITY_NORMAL;
            default: return SEVERITY_NO_DATA;
        }
    }
}
