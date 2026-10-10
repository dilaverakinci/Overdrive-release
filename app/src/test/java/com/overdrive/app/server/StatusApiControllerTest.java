package com.overdrive.app.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.overdrive.app.byd.BydVehicleData;
import com.overdrive.app.monitor.ChargingStateData;

import org.json.JSONObject;
import org.junit.Test;

public class StatusApiControllerTest {

    @Test
    public void idleChargingStatusHasExpectedDefaultContract() throws Exception {
        JSONObject status = StatusApiController.buildIdleChargingStatus();
        assertNotNull(status);
        assertEquals("Unavailable", status.getString("stateName"));
        assertEquals("UNKNOWN", status.getString("status"));
        assertEquals(0, status.getInt("chargingPowerKW"));
        assertFalse(status.getBoolean("isDischarging"));
        assertFalse(status.getBoolean("charging"));
        assertFalse(status.getBoolean("plugged"));
        assertFalse(status.getBoolean("full"));
    }

    @Test
    public void chargingResolversFollowExpectedRules() {
        assertFalse(StatusApiController.resolveChargingPlugged(
                false, ChargingStateData.ChargingStatus.FINISHED, 1, false));
        assertFalse(StatusApiController.resolveChargingPlugged(
                true, ChargingStateData.ChargingStatus.CHARGING, 2, true));
        assertTrue(StatusApiController.resolveChargingPlugged(
                false, ChargingStateData.ChargingStatus.READY,
                BydVehicleData.UNAVAILABLE, false));

        assertFalse(StatusApiController.resolveChargingActive(
                true, false,
                ChargingStateData.ChargingStatus.FINISHED,
                1, false));
        assertFalse(StatusApiController.resolveChargingFull(
                ChargingStateData.ChargingStatus.FINISHED,
                false, 1, false));
    }
}
