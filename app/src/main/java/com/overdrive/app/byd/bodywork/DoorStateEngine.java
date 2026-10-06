package com.overdrive.app.byd.bodywork;

import com.overdrive.app.byd.BydFeatureIds;

/**
 * Door and closure state normalization and physical mapping engine.
 */
public final class DoorStateEngine {

    private DoorStateEngine() {}

    /**
     * Check if a raw state integer represents a confirmed open or closed door/lid state.
     */
    public static boolean isValidDoorOpenState(int state) {
        return state == BodyworkConstants.STATE_OPEN
                || state == BodyworkConstants.STATE_CLOSED;
    }

    /**
     * Map logical door areas (driver/passenger) to physical BYD feature IDs based on drive side.
     * On RHD: area 1 (driver) is physical RF and area 2 (passenger) is physical LF.
     * On LHD: area 1 (driver) is physical LF and area 2 (passenger) is physical RF.
     * Rear doors and lids remain fixed physical identifiers.
     */
    public static int doorFeatureForArea(int area, boolean rightHandDrive) {
        switch (area) {
            case BodyworkConstants.AREA_FRONT_DRIVER:
                return rightHandDrive
                        ? BydFeatureIds.BODYWORK_DOOR_RF
                        : BydFeatureIds.BODYWORK_DOOR_LF;
            case BodyworkConstants.AREA_FRONT_PASSENGER:
                return rightHandDrive
                        ? BydFeatureIds.BODYWORK_DOOR_LF
                        : BydFeatureIds.BODYWORK_DOOR_RF;
            case BodyworkConstants.AREA_REAR_LEFT:
                return BydFeatureIds.BODYWORK_DOOR_LR;
            case BodyworkConstants.AREA_REAR_RIGHT:
                return BydFeatureIds.BODYWORK_DOOR_RR;
            case BodyworkConstants.AREA_HOOD:
                return BydFeatureIds.BODYWORK_HOOD;
            case BodyworkConstants.AREA_TRUNK:
                return BydFeatureIds.BODYWORK_TRUNK;
            case BodyworkConstants.AREA_FUEL_CAP:
                return BydFeatureIds.BODYWORK_FUEL_CAP;
            default:
                return BydFeatureIds.UNRESOLVED_ID;
        }
    }

    /**
     * Map raw door read (which may use sentinels or Integer.MIN_VALUE for unavailable)
     * to API contract: 1=open, 0=closed, -1=unknown.
     */
    public static int normalizeDoorOpen(int raw) {
        return (raw == BodyworkConstants.STATE_OPEN || raw == BodyworkConstants.STATE_CLOSED)
                ? raw : -1;
    }
}
