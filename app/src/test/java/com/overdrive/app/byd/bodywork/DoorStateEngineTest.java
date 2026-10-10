package com.overdrive.app.byd.bodywork;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.overdrive.app.byd.BydFeatureIds;

import org.junit.Test;

public class DoorStateEngineTest {

    @Test
    public void validDoorOpenStateAcceptsOnlyOpenAndClosed() {
        assertTrue(DoorStateEngine.isValidDoorOpenState(BodyworkConstants.STATE_OPEN));
        assertTrue(DoorStateEngine.isValidDoorOpenState(BodyworkConstants.STATE_CLOSED));

        assertFalse(DoorStateEngine.isValidDoorOpenState(-1));
        assertFalse(DoorStateEngine.isValidDoorOpenState(2));
        assertFalse(DoorStateEngine.isValidDoorOpenState(255));
        assertFalse(DoorStateEngine.isValidDoorOpenState(65535));
        assertFalse(DoorStateEngine.isValidDoorOpenState(Integer.MIN_VALUE));
        assertFalse(DoorStateEngine.isValidDoorOpenState(BydFeatureIds.BMS_UNAVAILABLE));
    }

    @Test
    public void frontDoorMappingFollowsDriveSide() {
        // RHD: driver is right (RF), passenger is left (LF)
        assertEquals(BydFeatureIds.BODYWORK_DOOR_RF,
                DoorStateEngine.doorFeatureForArea(BodyworkConstants.AREA_FRONT_DRIVER, true));
        assertEquals(BydFeatureIds.BODYWORK_DOOR_LF,
                DoorStateEngine.doorFeatureForArea(BodyworkConstants.AREA_FRONT_PASSENGER, true));

        // LHD: driver is left (LF), passenger is right (RF)
        assertEquals(BydFeatureIds.BODYWORK_DOOR_LF,
                DoorStateEngine.doorFeatureForArea(BodyworkConstants.AREA_FRONT_DRIVER, false));
        assertEquals(BydFeatureIds.BODYWORK_DOOR_RF,
                DoorStateEngine.doorFeatureForArea(BodyworkConstants.AREA_FRONT_PASSENGER, false));
    }

    @Test
    public void rearAndLidMappingRemainsFixed() {
        assertEquals(BydFeatureIds.BODYWORK_DOOR_LR,
                DoorStateEngine.doorFeatureForArea(BodyworkConstants.AREA_REAR_LEFT, true));
        assertEquals(BydFeatureIds.BODYWORK_DOOR_LR,
                DoorStateEngine.doorFeatureForArea(BodyworkConstants.AREA_REAR_LEFT, false));

        assertEquals(BydFeatureIds.BODYWORK_DOOR_RR,
                DoorStateEngine.doorFeatureForArea(BodyworkConstants.AREA_REAR_RIGHT, true));
        assertEquals(BydFeatureIds.BODYWORK_DOOR_RR,
                DoorStateEngine.doorFeatureForArea(BodyworkConstants.AREA_REAR_RIGHT, false));

        assertEquals(BydFeatureIds.BODYWORK_HOOD,
                DoorStateEngine.doorFeatureForArea(BodyworkConstants.AREA_HOOD, true));
        assertEquals(BydFeatureIds.BODYWORK_TRUNK,
                DoorStateEngine.doorFeatureForArea(BodyworkConstants.AREA_TRUNK, true));
        assertEquals(BydFeatureIds.BODYWORK_FUEL_CAP,
                DoorStateEngine.doorFeatureForArea(BodyworkConstants.AREA_FUEL_CAP, true));

        assertEquals(BydFeatureIds.UNRESOLVED_ID,
                DoorStateEngine.doorFeatureForArea(999, true));
    }

    @Test
    public void normalizeDoorOpenMapsToExpectedValues() {
        assertEquals(BodyworkConstants.STATE_OPEN,
                DoorStateEngine.normalizeDoorOpen(BodyworkConstants.STATE_OPEN));
        assertEquals(BodyworkConstants.STATE_CLOSED,
                DoorStateEngine.normalizeDoorOpen(BodyworkConstants.STATE_CLOSED));
        assertEquals(-1, DoorStateEngine.normalizeDoorOpen(-1));
        assertEquals(-1, DoorStateEngine.normalizeDoorOpen(Integer.MIN_VALUE));
        assertEquals(-1, DoorStateEngine.normalizeDoorOpen(2));
    }
}
