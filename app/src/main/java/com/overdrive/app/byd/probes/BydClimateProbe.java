package com.overdrive.app.byd.probes;

import com.overdrive.app.byd.BydDeviceHelper;
import com.overdrive.app.byd.BydVehicleData;
import com.overdrive.app.logging.DaemonLogger;

import java.lang.reflect.Method;

/**
 * Hardware probe responsible for BYD HVAC, Cabin Air Conditioning, and PM2.5 Air Quality.
 * Encapsulates reflection-based telemetry reads and actuator commands to BYDAutoAcDevice
 * and BYDAutoPM2p5Device.
 */
public class BydClimateProbe {

    private static final DaemonLogger logger = DaemonLogger.getInstance("BydClimateProbe");

    private volatile Object acDevice;
    private volatile Object pm25Device;

    public BydClimateProbe() {}

    public synchronized void setDevices(Object acDev, Object pm25Dev) {
        this.acDevice = acDev;
        this.pm25Device = pm25Dev;
    }

    public synchronized Object getAcDevice() { return acDevice; }
    public synchronized Object getPm25Device() { return pm25Device; }

    /**
     * Reads climate and air quality telemetry into snapshot builder.
     */
    public void collectClimate(BydVehicleData.Builder b, Object acDev, Object pm25Dev) {
        if (acDev != null) {
            try {
                Object acState = BydDeviceHelper.callGetter(acDev, "getAcStartState");
                if (acState instanceof Number) {
                    b.acStartState(((Number) acState).intValue());
                }
            } catch (Exception e) {
                logger.debug("Failed reading AC start state: " + e.getMessage());
            }

            try {
                Object fanLevel = BydDeviceHelper.callGetter(acDev, "getAcFanLevel");
                if (fanLevel instanceof Number) {
                    b.acFanLevel(((Number) fanLevel).intValue());
                }
            } catch (Exception e) {
                logger.debug("Failed reading AC fan level: " + e.getMessage());
            }
        }

        if (pm25Dev != null) {
            try {
                Object pm25Val = BydDeviceHelper.callGetter(pm25Dev, "getPM2p5Value");
                if (pm25Val instanceof Number) {
                    int p = ((Number) pm25Val).intValue();
                    if (p >= 0) b.pm25Inside(p);
                }
            } catch (Exception e) {
                logger.debug("Failed reading PM2.5 value: " + e.getMessage());
            }
        }
    }

    /**
     * Turn AC power on or off.
     */
    public boolean setAcPower(Object acDev, boolean on) {
        if (acDev == null) {
            logger.warn("setAcPower: acDevice is null");
            return false;
        }
        try {
            Method m = acDev.getClass().getMethod("setAcStartState", int.class);
            Object res = m.invoke(acDev, on ? 1 : 0);
            logger.info("setAcPower(" + on + ") result=" + res);
            return res != null && !Boolean.FALSE.equals(res);
        } catch (Exception e) {
            logger.warn("setAcPower failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Set cabin target temperature in degrees Celsius.
     */
    public boolean setTemperature(Object acDev, int zone, double celsius) {
        if (acDev == null) {
            logger.warn("setTemperature: acDevice is null");
            return false;
        }
        try {
            Method m = acDev.getClass().getMethod("setAcTemperature", int.class, double.class);
            Object res = m.invoke(acDev, zone, celsius);
            logger.info("setTemperature(zone=" + zone + ", " + celsius + "C) result=" + res);
            return res != null && !Boolean.FALSE.equals(res);
        } catch (NoSuchMethodException e) {
            try {
                Method m2 = acDev.getClass().getMethod("setAcTemperature", int.class, int.class);
                Object res2 = m2.invoke(acDev, zone, (int) Math.round(celsius));
                return res2 != null && !Boolean.FALSE.equals(res2);
            } catch (Exception e2) {
                logger.warn("setTemperature fallback failed: " + e2.getMessage());
                return false;
            }
        } catch (Exception e) {
            logger.warn("setTemperature failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Set blower fan speed level.
     */
    public boolean setFanLevel(Object acDev, int level) {
        if (acDev == null) {
            logger.warn("setFanLevel: acDevice is null");
            return false;
        }
        try {
            Method m = acDev.getClass().getMethod("setAcFanLevel", int.class);
            Object res = m.invoke(acDev, level);
            logger.info("setFanLevel(" + level + ") result=" + res);
            return res != null && !Boolean.FALSE.equals(res);
        } catch (Exception e) {
            logger.warn("setFanLevel failed: " + e.getMessage());
            return false;
        }
    }
}
