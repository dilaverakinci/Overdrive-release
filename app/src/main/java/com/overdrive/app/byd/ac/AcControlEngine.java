package com.overdrive.app.byd.ac;

import com.overdrive.app.byd.BydVehicleData;

/**
 * Climate control setpoint calculation, clamping, unit inference, and readback confirmation engine.
 */
public final class AcControlEngine {

    private AcControlEngine() {}

    public static final int TEMP_UNIT_FAHRENHEIT = 0;
    public static final int TEMP_UNIT_CELSIUS = 1;

    public static final int AC_SETPOINT_MIN_C = 17;
    public static final int AC_SETPOINT_MAX_C = 33;
    public static final int AC_SETPOINT_MIN_F = 64;
    public static final int AC_SETPOINT_MAX_F = 91;

    public static final int AC_TEMP_AREA_DRIVER = 1;
    public static final int AC_TEMP_AREA_PASSENGER = 2;

    /**
     * Check if a DiLink 5 register value represents an unavailable rail (negative, 65534, or 65535).
     */
    public static boolean isDiLink5UnavailableRail(int value) {
        return value < 0 || value == 65534 || value == 65535;
    }

    /**
     * Clamp setpoint to safe dial limits for given temperature unit.
     */
    public static int clampSetpoint(int value, int unit) {
        boolean f = unit == TEMP_UNIT_FAHRENHEIT;
        int min = f ? AC_SETPOINT_MIN_F : AC_SETPOINT_MIN_C;
        int max = f ? AC_SETPOINT_MAX_F : AC_SETPOINT_MAX_C;
        return value < min ? min : value > max ? max : value;
    }

    /**
     * Infer temperature unit from an existing numeric setpoint reading.
     * Returns 1 (Celsius) if in 17..33 range, 0 (Fahrenheit) if in 64..91 range, else UNAVAILABLE.
     */
    public static int inferUnitFromSetpoint(int setpoint) {
        if (setpoint >= AC_SETPOINT_MIN_C && setpoint <= AC_SETPOINT_MAX_C) {
            return TEMP_UNIT_CELSIUS;
        }
        if (setpoint >= AC_SETPOINT_MIN_F && setpoint <= AC_SETPOINT_MAX_F) {
            return TEMP_UNIT_FAHRENHEIT;
        }
        return BydVehicleData.UNAVAILABLE;
    }

    /**
     * Resolve effective temperature unit from reported unit and current dial setpoint.
     */
    public static int resolveAcTemperatureUnit(
            int reportedUnit, int currentSetpoint, boolean diLink5) {
        reportedUnit = normalizeAcTemperatureUnit(reportedUnit, diLink5);
        if (reportedUnit != BydVehicleData.UNAVAILABLE) return reportedUnit;
        return diLink5 ? inferUnitFromSetpoint(currentSetpoint) : TEMP_UNIT_CELSIUS;
    }

    /**
     * Normalize DiLink 5 temperature unit sentinel values.
     */
    public static int normalizeAcTemperatureUnit(int unit, boolean diLink5) {
        if (diLink5 && isDiLink5UnavailableRail(unit)) {
            return BydVehicleData.UNAVAILABLE;
        }
        return unit;
    }

    /**
     * Verify whether setpoint readback matches target.
     * Zone 0 expects both driver and passenger to match; otherwise checks the specific zone.
     */
    public static boolean isAcSetpointReadbackConfirmed(
            int zone, int target, int driver, int passenger) {
        if (zone == 0) return driver == target && passenger == target;
        return zone == AC_TEMP_AREA_PASSENGER
                ? passenger == target : driver == target;
    }
}
