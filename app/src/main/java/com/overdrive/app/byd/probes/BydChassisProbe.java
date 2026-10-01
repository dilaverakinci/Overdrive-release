package com.overdrive.app.byd.probes;

import com.overdrive.app.byd.BydDeviceHelper;
import com.overdrive.app.byd.BydVehicleData;
import com.overdrive.app.logging.DaemonLogger;

/**
 * Hardware probe responsible for BYD Chassis, TPMS Tyre Pressure Monitoring,
 * Radar Proximity Sensors, and Incline/Slope Sensors.
 * Encapsulates reflection-based calls to BYDAutoTyreDevice, BYDAutoRadarDevice,
 * and BYDAutoSensorDevice.
 */
public class BydChassisProbe {

    private static final DaemonLogger logger = DaemonLogger.getInstance("BydChassisProbe");

    private volatile Object tyreDevice;
    private volatile Object sensorDevice;
    private volatile Object radarDevice;

    public BydChassisProbe() {}

    public synchronized void setDevices(Object tyreDev, Object sensorDev, Object radarDev) {
        this.tyreDevice = tyreDev;
        this.sensorDevice = sensorDev;
        this.radarDevice = radarDev;
    }

    public synchronized Object getTyreDevice() { return tyreDevice; }
    public synchronized Object getSensorDevice() { return sensorDevice; }
    public synchronized Object getRadarDevice() { return radarDevice; }

    /**
     * Reads chassis sensors (slope, incline) into snapshot builder.
     */
    public void collectSensor(BydVehicleData.Builder b, Object sensorDev) {
        if (sensorDev == null) return;
        try {
            Object slope = BydDeviceHelper.callGetter(sensorDev, "getSlope");
            if (slope instanceof Number) {
                int raw = ((Number) slope).intValue();
                double degrees = Math.toDegrees(Math.atan(raw / 100.0));
                if (degrees >= -60 && degrees <= 60) b.slopeDegrees(degrees);
            }
        } catch (Exception e) {
            logger.debug("Failed reading slope: " + e.getMessage());
        }
    }

    /**
     * Reads TPMS tyre pressures and temperatures into snapshot builder.
     */
    public void collectTyres(BydVehicleData.Builder b, Object tyreDev) {
        if (tyreDev == null) return;
        try {
            Object flPressure = BydDeviceHelper.callGetter(tyreDev, "getTyrePressureValue", 1);
            Object frPressure = BydDeviceHelper.callGetter(tyreDev, "getTyrePressureValue", 2);
            Object rlPressure = BydDeviceHelper.callGetter(tyreDev, "getTyrePressureValue", 3);
            Object rrPressure = BydDeviceHelper.callGetter(tyreDev, "getTyrePressureValue", 4);

            if (flPressure instanceof Number && frPressure instanceof Number &&
                    rlPressure instanceof Number && rrPressure instanceof Number) {
                int[] pressures = new int[] {
                        ((Number) flPressure).intValue(),
                        ((Number) frPressure).intValue(),
                        ((Number) rlPressure).intValue(),
                        ((Number) rrPressure).intValue()
                };
                b.tyrePressure(pressures);
            }
        } catch (Exception e) {
            logger.debug("Failed reading tyre pressures: " + e.getMessage());
        }
    }
}
