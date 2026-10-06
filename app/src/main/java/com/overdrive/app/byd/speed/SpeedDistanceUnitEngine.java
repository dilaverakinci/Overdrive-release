package com.overdrive.app.byd.speed;

import com.overdrive.app.byd.BydFeatureIds;

/**
 * Speed, distance, odometer, and telemetry unit calculation and validation engine.
 */
public final class SpeedDistanceUnitEngine {

    private SpeedDistanceUnitEngine() {}

    public static final double MILES_TO_KM = 1.60934;
    public static final double KM_TO_MILES = 1.0 / MILES_TO_KM;

    /**
     * A raw total-distance register at or above this is being reported in 0.1 units rather
     * than whole ones — no production odometer legitimately reaches 1,000,000.
     */
    public static final double RAW_TOTAL_MILEAGE_FINE_THRESHOLD = 1_000_000.0;

    public static double milesToKm(double miles) {
        return miles * MILES_TO_KM;
    }

    public static double kmToMiles(double km) {
        return km * KM_TO_MILES;
    }

    public static boolean isUsablePolledSoc(double soc, boolean diLink5) {
        return soc >= (diLink5 ? 1.0 : 0.0) && soc <= 100.0;
    }

    public static double statisticDistanceFactor(boolean diLink5, double legacyFactor) {
        return diLink5 ? 1.0 : legacyFactor;
    }

    public static boolean isUsableMileage(int value, boolean diLink5) {
        return value >= (diLink5 ? 0 : 1) && value <= 2_000_000;
    }

    /**
     * Rejects HAL not-available encodings, MIN_VALUE, 65535, and checks plausibility.
     */
    public static boolean isUsableManagerMileage(int raw, boolean diLink5) {
        return raw != BydFeatureIds.BMS_UNAVAILABLE
                && raw != BydFeatureIds.INVALID_VALUE
                && raw != BydFeatureIds.INVALID_VALUE_2
                && raw != 65535
                && raw != Integer.MIN_VALUE
                && isUsableMileage(raw, diLink5);
    }

    public static boolean isPlausibleTotalMileage(double value) {
        return Double.isFinite(value) && value > 0.0 && value <= 9_999_999.9;
    }

    /**
     * Normalize a raw total-distance register to WHOLE cluster units.
     * When raw >= 1,000,000, scales by 0.1.
     */
    public static double normalizeRawTotalMileage(double raw) {
        return raw >= RAW_TOTAL_MILEAGE_FINE_THRESHOLD ? raw / 10.0 : raw;
    }

    public static boolean isPlausibleElectricRange(int value) {
        return value >= 0 && value <= 2_000;
    }

    public static boolean isPlausibleWaterTemperature(int value, boolean diLink5) {
        return value >= (diLink5 ? 1 : 0) && value <= 200;
    }

    public static boolean isPlausibleTotalElectricConsumption(double value) {
        return Double.isFinite(value) && value >= -1_000.0 && value <= 1_676_721.4;
    }

    public static boolean isPlausibleTotalFuelConsumption(double value, boolean diLink5) {
        return Double.isFinite(value) && value >= 0.0
                && value <= (diLink5 ? 9_999.9 : 104_857.4);
    }
}
