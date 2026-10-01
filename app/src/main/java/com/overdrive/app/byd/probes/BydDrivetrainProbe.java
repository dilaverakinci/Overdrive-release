package com.overdrive.app.byd.probes;

import com.overdrive.app.byd.BydDeviceHelper;
import com.overdrive.app.byd.BydVehicleData;
import com.overdrive.app.logging.DaemonLogger;
import com.overdrive.app.monitor.GearMonitor;

/**
 * Hardware probe responsible for BYD Powertrain, Speed, Gearbox, and Motor subsystems.
 * Encapsulates reflection-based calls to BYDAutoSpeedDevice, BYDAutoEngineDevice,
 * and BYDAutoGearboxDevice.
 */
public class BydDrivetrainProbe {

    private static final DaemonLogger logger = DaemonLogger.getInstance("BydDrivetrainProbe");

    private volatile Object speedDevice;
    private volatile Object engineDevice;
    private volatile Object gearboxDevice;
    private volatile Object statisticDevice;

    public BydDrivetrainProbe() {}

    public synchronized void setDevices(Object speedDev, Object engineDev, Object gearboxDev, Object statDev) {
        this.speedDevice = speedDev;
        this.engineDevice = engineDev;
        this.gearboxDevice = gearboxDev;
        this.statisticDevice = statDev;
    }

    public synchronized Object getSpeedDevice() { return speedDevice; }
    public synchronized Object getEngineDevice() { return engineDevice; }
    public synchronized Object getGearboxDevice() { return gearboxDevice; }
    public synchronized Object getStatisticDevice() { return statisticDevice; }

    /**
     * Reads speed and powertrain telemetry from devices into snapshot builder.
     */
    public void collectDrivetrain(BydVehicleData.Builder b, Object speedDev, Object engineDev, Object gearboxDev) {
        if (speedDev != null) {
            try {
                Object speed = BydDeviceHelper.callGetter(speedDev, "getVehicleSpeed");
                if (speed instanceof Number) {
                    double val = ((Number) speed).doubleValue();
                    if (val >= 0 && val <= 350) b.speedKmh(val);
                }
            } catch (Exception e) {
                logger.debug("Failed reading vehicle speed: " + e.getMessage());
            }
        }

        if (gearboxDev != null) {
            try {
                Object gear = BydDeviceHelper.callGetter(gearboxDev, "getGearboxAutoModeType");
                if (gear instanceof Number) {
                    int g = ((Number) gear).intValue();
                    if (g >= GearMonitor.GEAR_P && g <= GearMonitor.GEAR_S) {
                        b.gearMode(g);
                    }
                }
            } catch (Exception e) {
                logger.debug("Failed reading gear: " + e.getMessage());
            }
        }

        if (engineDev != null) {
            try {
                Object rpm = BydDeviceHelper.callGetter(engineDev, "getEngineSpeed");
                if (rpm instanceof Number) {
                    int r = ((Number) rpm).intValue();
                    if (r >= 0) b.engineSpeedRpm(r);
                }
            } catch (Exception e) {
                logger.debug("Failed reading engine RPM: " + e.getMessage());
            }
        }
    }
}
