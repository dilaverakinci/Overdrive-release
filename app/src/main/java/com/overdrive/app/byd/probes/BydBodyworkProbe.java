package com.overdrive.app.byd.probes;

import com.overdrive.app.byd.BydDeviceHelper;
import com.overdrive.app.byd.BydFeatureIds;
import com.overdrive.app.byd.BydVehicleData;
import com.overdrive.app.logging.DaemonLogger;

import java.lang.reflect.Method;

/**
 * Hardware probe responsible for BYD Bodywork, Door Locks, Windows, Lights, Wipers,
 * and Central Screen (Pad) Rotation.
 * Encapsulates reflection-based calls to BYDAutoBodyworkDevice, BYDAutoDoorLockDevice,
 * BYDAutoLightDevice, BYDAutoWiperDevice, and BYDAutoSettingDevice.
 */
public class BydBodyworkProbe {

    private static final DaemonLogger logger = DaemonLogger.getInstance("BydBodyworkProbe");

    public static final int PAD_ROTATION_HORIZONTAL = 1;
    public static final int PAD_ROTATION_VERTICAL = 2;

    private volatile Object bodyworkDevice;
    private volatile Object doorLockDevice;
    private volatile Object lightDevice;
    private volatile Object wiperDevice;
    private volatile Object settingDevice;

    public BydBodyworkProbe() {}

    public synchronized void setDevices(
            Object bodyworkDev,
            Object doorLockDev,
            Object lightDev,
            Object wiperDev,
            Object settingDev) {
        this.bodyworkDevice = bodyworkDev;
        this.doorLockDevice = doorLockDev;
        this.lightDevice = lightDev;
        this.wiperDevice = wiperDev;
        this.settingDevice = settingDev;
    }

    public synchronized Object getBodyworkDevice() { return bodyworkDevice; }
    public synchronized Object getDoorLockDevice() { return doorLockDevice; }
    public synchronized Object getLightDevice() { return lightDevice; }
    public synchronized Object getWiperDevice() { return wiperDevice; }
    public synchronized Object getSettingDevice() { return settingDevice; }

    /**
     * Reads bodywork, lights, and wipers telemetry into snapshot builder.
     */
    public void collectBodywork(
            BydVehicleData.Builder b,
            Object bodyworkDev,
            Object lightDev,
            Object wiperDev) {
        if (lightDev != null) {
            try {
                Object low = BydDeviceHelper.callGetter(lightDev, "getLowBeamState");
                if (low instanceof Number) {
                    b.lowBeam(((Number) low).intValue() == 1);
                }
            } catch (Exception e) {
                logger.debug("Failed reading lowBeam: " + e.getMessage());
            }

            try {
                Object high = BydDeviceHelper.callGetter(lightDev, "getHighBeamState");
                if (high instanceof Number) {
                    b.highBeam(((Number) high).intValue() == 1);
                }
            } catch (Exception e) {
                logger.debug("Failed reading highBeam: " + e.getMessage());
            }
        }

        if (wiperDev != null) {
            try {
                Object wiper = BydDeviceHelper.callGetter(wiperDev, "getFrontWiperStatus");
                if (wiper instanceof Number) {
                    b.wiperState(((Number) wiper).intValue());
                }
            } catch (Exception e) {
                logger.debug("Failed reading wiperState: " + e.getMessage());
            }
        }
    }

    /**
     * Set central infotainment Pad rotation orientation.
     * @param rotation 1 for horizontal (landscape), 2 for vertical (portrait).
     */
    public boolean setPadRotation(Object settingDev, int rotation) {
        if (rotation != PAD_ROTATION_HORIZONTAL && rotation != PAD_ROTATION_VERTICAL) {
            logger.warn("setPadRotation: invalid rotation " + rotation);
            return false;
        }
        if (settingDev == null) {
            logger.warn("setPadRotation: settingDevice is null");
            return false;
        }
        try {
            Method method = settingDev.getClass().getMethod("setPadRotation", int.class);
            Object result = method.invoke(settingDev, rotation);
            boolean accepted = false;
            if (result instanceof Number) {
                accepted = ((Number) result).intValue() >= 0;
            } else if (result instanceof Boolean) {
                accepted = (Boolean) result;
            } else if (result != null) {
                accepted = true;
            }
            logger.info("setPadRotation(" + rotation + ") result=" + result + " accepted=" + accepted);
            return accepted;
        } catch (NoSuchMethodException e) {
            return BydDeviceHelper.sendSetCommand(
                    settingDev, BydFeatureIds.SETTING_PAD_ROTATION_SET, rotation);
        } catch (Exception e) {
            logger.warn("setPadRotation(" + rotation + ") failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lock or unlock vehicle doors via door lock actuator.
     */
    public boolean setDoorLock(Object doorLockDev, boolean lock) {
        if (doorLockDev == null) {
            logger.warn("setDoorLock: doorLockDevice is null");
            return false;
        }
        try {
            Method m = doorLockDev.getClass().getMethod("setDoorLockStatus", int.class, int.class);
            // area 0 (all doors), 1=lock, 2=unlock
            int action = lock ? 1 : 2;
            Object res = m.invoke(doorLockDev, 0, action);
            logger.info("setDoorLock(" + lock + ") result=" + res);
            return res != null && !Boolean.FALSE.equals(res);
        } catch (Exception e) {
            logger.warn("setDoorLock failed: " + e.getMessage());
            return false;
        }
    }
}
