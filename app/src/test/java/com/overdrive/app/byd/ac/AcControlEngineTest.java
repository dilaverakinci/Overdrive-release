package com.overdrive.app.byd.ac;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.overdrive.app.byd.BydVehicleData;

import org.junit.Test;

public class AcControlEngineTest {

    @Test
    public void clampSetpointRespectsCelsiusAndFahrenheitLimits() {
        // Celsius: 17 .. 33
        assertEquals(17, AcControlEngine.clampSetpoint(15, AcControlEngine.TEMP_UNIT_CELSIUS));
        assertEquals(22, AcControlEngine.clampSetpoint(22, AcControlEngine.TEMP_UNIT_CELSIUS));
        assertEquals(33, AcControlEngine.clampSetpoint(35, AcControlEngine.TEMP_UNIT_CELSIUS));

        // Fahrenheit: 64 .. 91
        assertEquals(64, AcControlEngine.clampSetpoint(50, AcControlEngine.TEMP_UNIT_FAHRENHEIT));
        assertEquals(72, AcControlEngine.clampSetpoint(72, AcControlEngine.TEMP_UNIT_FAHRENHEIT));
        assertEquals(91, AcControlEngine.clampSetpoint(95, AcControlEngine.TEMP_UNIT_FAHRENHEIT));
    }

    @Test
    public void inferUnitFromSetpointDetectsDisjointBands() {
        assertEquals(AcControlEngine.TEMP_UNIT_CELSIUS, AcControlEngine.inferUnitFromSetpoint(22));
        assertEquals(AcControlEngine.TEMP_UNIT_CELSIUS, AcControlEngine.inferUnitFromSetpoint(17));
        assertEquals(AcControlEngine.TEMP_UNIT_CELSIUS, AcControlEngine.inferUnitFromSetpoint(33));

        assertEquals(AcControlEngine.TEMP_UNIT_FAHRENHEIT, AcControlEngine.inferUnitFromSetpoint(72));
        assertEquals(AcControlEngine.TEMP_UNIT_FAHRENHEIT, AcControlEngine.inferUnitFromSetpoint(64));
        assertEquals(AcControlEngine.TEMP_UNIT_FAHRENHEIT, AcControlEngine.inferUnitFromSetpoint(91));

        assertEquals(BydVehicleData.UNAVAILABLE, AcControlEngine.inferUnitFromSetpoint(10));
        assertEquals(BydVehicleData.UNAVAILABLE, AcControlEngine.inferUnitFromSetpoint(50));
        assertEquals(BydVehicleData.UNAVAILABLE, AcControlEngine.inferUnitFromSetpoint(100));
    }

    @Test
    public void resolveAcTemperatureUnitPrioritizesReportedUnit() {
        assertEquals(AcControlEngine.TEMP_UNIT_CELSIUS,
                AcControlEngine.resolveAcTemperatureUnit(AcControlEngine.TEMP_UNIT_CELSIUS, 72, false));
        assertEquals(AcControlEngine.TEMP_UNIT_FAHRENHEIT,
                AcControlEngine.resolveAcTemperatureUnit(AcControlEngine.TEMP_UNIT_FAHRENHEIT, 22, false));

        // When unit unavailable, infers from setpoint
        assertEquals(AcControlEngine.TEMP_UNIT_FAHRENHEIT,
                AcControlEngine.resolveAcTemperatureUnit(BydVehicleData.UNAVAILABLE, 72, true));
        assertEquals(AcControlEngine.TEMP_UNIT_CELSIUS,
                AcControlEngine.resolveAcTemperatureUnit(BydVehicleData.UNAVAILABLE, 22, true));
    }

    @Test
    public void readbackConfirmationChecksZonesCorrectly() {
        // Zone 0: both must match
        assertTrue(AcControlEngine.isAcSetpointReadbackConfirmed(0, 22, 22, 22));
        assertFalse(AcControlEngine.isAcSetpointReadbackConfirmed(0, 22, 22, 23));

        // Passenger zone (2)
        assertTrue(AcControlEngine.isAcSetpointReadbackConfirmed(AcControlEngine.AC_TEMP_AREA_PASSENGER, 24, 20, 24));
        assertFalse(AcControlEngine.isAcSetpointReadbackConfirmed(AcControlEngine.AC_TEMP_AREA_PASSENGER, 24, 24, 20));

        // Driver zone (1)
        assertTrue(AcControlEngine.isAcSetpointReadbackConfirmed(AcControlEngine.AC_TEMP_AREA_DRIVER, 21, 21, 25));
        assertFalse(AcControlEngine.isAcSetpointReadbackConfirmed(AcControlEngine.AC_TEMP_AREA_DRIVER, 21, 20, 21));
    }

    @Test
    public void normalizeAcTemperatureUnitHandlesDiLink5Rails() {
        assertEquals(BydVehicleData.UNAVAILABLE, AcControlEngine.normalizeAcTemperatureUnit(-10011, true));
        assertEquals(BydVehicleData.UNAVAILABLE, AcControlEngine.normalizeAcTemperatureUnit(65534, true));
        assertEquals(BydVehicleData.UNAVAILABLE, AcControlEngine.normalizeAcTemperatureUnit(65535, true));
        assertEquals(-10011, AcControlEngine.normalizeAcTemperatureUnit(-10011, false));
        assertEquals(65535, AcControlEngine.normalizeAcTemperatureUnit(65535, false));
    }
}
