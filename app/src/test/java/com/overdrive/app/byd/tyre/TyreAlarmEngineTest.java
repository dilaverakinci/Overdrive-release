package com.overdrive.app.byd.tyre;

import static org.junit.Assert.assertEquals;

import com.overdrive.app.byd.BydFeatureIds;
import com.overdrive.app.byd.BydVehicleData;

import org.junit.Test;

public class TyreAlarmEngineTest {

    @Test
    public void normalizeDiLink5TyreLeakStateSwapsSlowAndFast() {
        assertEquals(0, TyreAlarmEngine.normalizeDiLink5TyreLeakState(0));
        assertEquals(2, TyreAlarmEngine.normalizeDiLink5TyreLeakState(1));
        assertEquals(1, TyreAlarmEngine.normalizeDiLink5TyreLeakState(2));
        assertEquals(BydVehicleData.UNAVAILABLE, TyreAlarmEngine.normalizeDiLink5TyreLeakState(-1));
        assertEquals(BydVehicleData.UNAVAILABLE, TyreAlarmEngine.normalizeDiLink5TyreLeakState(3));
    }

    @Test
    public void normalizeDiLink5TyreStatusFiltersSentinels() {
        assertEquals(0, TyreAlarmEngine.normalizeDiLink5TyreStatus(0));
        assertEquals(1, TyreAlarmEngine.normalizeDiLink5TyreStatus(1));
        assertEquals(2, TyreAlarmEngine.normalizeDiLink5TyreStatus(2));

        assertEquals(BydVehicleData.UNAVAILABLE,
                TyreAlarmEngine.normalizeDiLink5TyreStatus(BydFeatureIds.BMS_UNAVAILABLE));
        assertEquals(BydVehicleData.UNAVAILABLE,
                TyreAlarmEngine.normalizeDiLink5TyreStatus(BydFeatureIds.INVALID_VALUE));
        assertEquals(BydVehicleData.UNAVAILABLE,
                TyreAlarmEngine.normalizeDiLink5TyreStatus(65534));
        assertEquals(BydVehicleData.UNAVAILABLE,
                TyreAlarmEngine.normalizeDiLink5TyreStatus(65535));
        assertEquals(BydVehicleData.UNAVAILABLE,
                TyreAlarmEngine.normalizeDiLink5TyreStatus(Integer.MIN_VALUE));
    }

    @Test
    public void formatTyrePressureRendersUnits() {
        assertEquals("250 kPa",
                TyreAlarmEngine.formatTyrePressure(250, "kpa", "kPa", "bar", "psi"));
        assertEquals("2.50 bar",
                TyreAlarmEngine.formatTyrePressure(250, "bar", "kPa", "bar", "psi"));
        assertEquals("36.3 psi",
                TyreAlarmEngine.formatTyrePressure(250, "psi", "kPa", "bar", "psi"));
    }

    @Test
    public void evaluateCornerPressureSeverityDetectsCriticalAndWarn() {
        // Critical low (e.g. 150 kPa when critical threshold is 152)
        assertEquals(TyreAlarmEngine.SEVERITY_CRITICAL,
                TyreAlarmEngine.evaluateCornerPressureSeverity(150, 0, 234, 310, 152));

        // Normal pressure
        assertEquals(TyreAlarmEngine.SEVERITY_NORMAL,
                TyreAlarmEngine.evaluateCornerPressureSeverity(250, 0, 234, 310, 152));

        // Low pressure (WARN)
        assertEquals(TyreAlarmEngine.SEVERITY_WARN,
                TyreAlarmEngine.evaluateCornerPressureSeverity(210, 0, 234, 310, 152));

        // High pressure (WARN)
        assertEquals(TyreAlarmEngine.SEVERITY_WARN,
                TyreAlarmEngine.evaluateCornerPressureSeverity(320, 0, 234, 310, 152));

        // Firmware warning flag even when kPa appears normal
        assertEquals(TyreAlarmEngine.SEVERITY_WARN,
                TyreAlarmEngine.evaluateCornerPressureSeverity(250, 1, 234, 310, 152));

        // No data / dropout
        assertEquals(TyreAlarmEngine.SEVERITY_NO_DATA,
                TyreAlarmEngine.evaluateCornerPressureSeverity(-1, -1, 234, 310, 152));
    }

    @Test
    public void evaluateCornerLeakSeverityMapsStates() {
        assertEquals(TyreAlarmEngine.SEVERITY_NORMAL, TyreAlarmEngine.evaluateCornerLeakSeverity(0));
        assertEquals(TyreAlarmEngine.SEVERITY_WARN, TyreAlarmEngine.evaluateCornerLeakSeverity(1));
        assertEquals(TyreAlarmEngine.SEVERITY_CRITICAL, TyreAlarmEngine.evaluateCornerLeakSeverity(2));
        assertEquals(TyreAlarmEngine.SEVERITY_NO_DATA, TyreAlarmEngine.evaluateCornerLeakSeverity(-1));
    }
}
