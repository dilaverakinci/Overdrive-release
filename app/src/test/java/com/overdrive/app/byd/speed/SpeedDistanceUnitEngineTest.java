package com.overdrive.app.byd.speed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.overdrive.app.byd.BydFeatureIds;

import org.junit.Test;

public class SpeedDistanceUnitEngineTest {

    @Test
    public void unitConversionsAreAccurate() {
        assertEquals(16.0934, SpeedDistanceUnitEngine.milesToKm(10.0), 0.001);
        assertEquals(10.0, SpeedDistanceUnitEngine.kmToMiles(16.0934), 0.001);
    }

    @Test
    public void usablePolledSocValidatesRange() {
        assertTrue(SpeedDistanceUnitEngine.isUsablePolledSoc(50.0, true));
        assertTrue(SpeedDistanceUnitEngine.isUsablePolledSoc(1.0, true));
        assertTrue(SpeedDistanceUnitEngine.isUsablePolledSoc(100.0, true));
        assertFalse(SpeedDistanceUnitEngine.isUsablePolledSoc(0.5, true));
        assertTrue(SpeedDistanceUnitEngine.isUsablePolledSoc(0.0, false));
        assertFalse(SpeedDistanceUnitEngine.isUsablePolledSoc(-1.0, false));
        assertFalse(SpeedDistanceUnitEngine.isUsablePolledSoc(101.0, false));
    }

    @Test
    public void statisticDistanceFactorPinsDiLink5ToOne() {
        assertEquals(1.0, SpeedDistanceUnitEngine.statisticDistanceFactor(true, 1.60934), 0.0001);
        assertEquals(1.60934, SpeedDistanceUnitEngine.statisticDistanceFactor(false, 1.60934), 0.0001);
    }

    @Test
    public void managerMileageRejectsSentinelsAndAcceptsRealNumbers() {
        assertTrue(SpeedDistanceUnitEngine.isUsableManagerMileage(8373, false));
        assertTrue(SpeedDistanceUnitEngine.isUsableManagerMileage(2141, false));
        assertTrue(SpeedDistanceUnitEngine.isUsableManagerMileage(1, false));
        assertTrue(SpeedDistanceUnitEngine.isUsableManagerMileage(2_000_000, false));

        int[] sentinels = {
                BydFeatureIds.BMS_UNAVAILABLE,   // -10011
                BydFeatureIds.INVALID_VALUE,     // -2147482645
                BydFeatureIds.INVALID_VALUE_2,   // -2147482648
                Integer.MIN_VALUE,
                65535,
                -1,
        };
        for (int s : sentinels) {
            assertFalse(SpeedDistanceUnitEngine.isUsableManagerMileage(s, false));
            assertFalse(SpeedDistanceUnitEngine.isUsableManagerMileage(s, true));
        }

        // Zero allowed only on DiLink 5
        assertFalse(SpeedDistanceUnitEngine.isUsableManagerMileage(0, false));
        assertTrue(SpeedDistanceUnitEngine.isUsableManagerMileage(0, true));

        // Upper bound
        assertFalse(SpeedDistanceUnitEngine.isUsableManagerMileage(2_000_001, false));
    }

    @Test
    public void normalizeRawTotalMileageScalesFineRegisters() {
        assertEquals(52345.0, SpeedDistanceUnitEngine.normalizeRawTotalMileage(52345.0), 0.001);
        assertEquals(123456.7, SpeedDistanceUnitEngine.normalizeRawTotalMileage(1_234_567.0), 0.001);
    }

    @Test
    public void plausibilityGatesCheckRanges() {
        assertTrue(SpeedDistanceUnitEngine.isPlausibleTotalMileage(100.0));
        assertFalse(SpeedDistanceUnitEngine.isPlausibleTotalMileage(0.0));
        assertFalse(SpeedDistanceUnitEngine.isPlausibleTotalMileage(-5.0));
        assertFalse(SpeedDistanceUnitEngine.isPlausibleTotalMileage(Double.NaN));

        assertTrue(SpeedDistanceUnitEngine.isPlausibleElectricRange(500));
        assertFalse(SpeedDistanceUnitEngine.isPlausibleElectricRange(-1));
        assertFalse(SpeedDistanceUnitEngine.isPlausibleElectricRange(2001));

        assertTrue(SpeedDistanceUnitEngine.isPlausibleWaterTemperature(90, true));
        assertFalse(SpeedDistanceUnitEngine.isPlausibleWaterTemperature(0, true));
        assertTrue(SpeedDistanceUnitEngine.isPlausibleWaterTemperature(0, false));
        assertFalse(SpeedDistanceUnitEngine.isPlausibleWaterTemperature(201, false));
    }
}
