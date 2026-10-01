package com.overdrive.app.byd.probes;

import com.overdrive.app.byd.BydDeviceHelper;
import com.overdrive.app.byd.BydFeatureIds;
import com.overdrive.app.byd.BydVehicleData;
import com.overdrive.app.logging.DaemonLogger;

import java.lang.reflect.Method;

/**
 * Hardware probe responsible for BYD High-Voltage Battery, 12V Battery, and Charging subsystems.
 * Encapsulates reflection-based calls to BYDAutoEnergyDevice and BYDAutoChargingDevice.
 */
public class BydBatteryProbe {

    private static final DaemonLogger logger = DaemonLogger.getInstance("BydBatteryProbe");

    private volatile Object energyDevice;
    private volatile Object chargingDevice;
    private volatile Boolean acChargingCurrentLimitSupported;

    public BydBatteryProbe() {}

    public synchronized void setDevices(Object energyDevice, Object chargingDevice) {
        this.energyDevice = energyDevice;
        this.chargingDevice = chargingDevice;
    }

    public synchronized Object getEnergyDevice() {
        return energyDevice;
    }

    public synchronized Object getChargingDevice() {
        return chargingDevice;
    }

    /**
     * Reads battery telemetry from the supplied energy and charging devices into the snapshot builder.
     */
    public void collectBattery(BydVehicleData.Builder b, Object energyDev, Object chargingDev) {
        if (energyDev != null) {
            try {
                Object soc = BydDeviceHelper.callGetter(energyDev, "getElectricityPercentage");
                if (soc instanceof Number) {
                    double val = ((Number) soc).doubleValue();
                    if (val >= 0 && val <= 100) b.socPercent(val);
                }
            } catch (Exception e) {
                logger.debug("Failed reading SOC from energyDevice: " + e.getMessage());
            }

            try {
                Object remainKwh = BydDeviceHelper.callGetter(energyDev, "getRemainingElectricCapacity");
                if (remainKwh instanceof Number) {
                    double val = ((Number) remainKwh).doubleValue();
                    if (val >= 0) b.remainKwh(val);
                }
            } catch (Exception e) {
                logger.debug("Failed reading remaining kWh from energyDevice: " + e.getMessage());
            }

            try {
                Object v12 = BydDeviceHelper.callGetter(energyDev, "getLowVoltageBatteryVoltage");
                if (v12 instanceof Number) {
                    double val = ((Number) v12).doubleValue();
                    if (val > 0) {
                        b.voltage12v(val);
                        b.voltage12vAtMs(System.currentTimeMillis());
                    }
                }
            } catch (Exception e) {
                logger.debug("Failed reading 12V voltage: " + e.getMessage());
            }
        }

        if (chargingDev != null) {
            try {
                Object gunState = BydDeviceHelper.callGetter(chargingDev, "getChargingGunState");
                if (gunState instanceof Number) {
                    b.chargingGunState(((Number) gunState).intValue());
                }
            } catch (Exception e) {
                logger.debug("Failed reading charging gun state: " + e.getMessage());
            }

            try {
                Object pwr = BydDeviceHelper.callGetter(chargingDev, "getChargingPower");
                if (pwr instanceof Number) {
                    double val = ((Number) pwr).doubleValue();
                    if (!Double.isNaN(val) && val >= 0) {
                        b.chargingPowerKw(val);
                        b.chargingPowerAtMs(System.currentTimeMillis());
                    }
                }
            } catch (Exception e) {
                logger.debug("Failed reading charging power: " + e.getMessage());
            }
        }
    }

    /**
     * Check if vehicle trim supports AC charging current limiting.
     */
    public boolean isAcChargingCurrentLimitSupported(Object chargingDev) {
        if (acChargingCurrentLimitSupported != null) {
            return acChargingCurrentLimitSupported;
        }
        if (chargingDev == null) return false;
        try {
            Method m = chargingDev.getClass().getMethod("getAcChargingCurrentLimitState");
            acChargingCurrentLimitSupported = (m != null);
            return acChargingCurrentLimitSupported;
        } catch (NoSuchMethodException e) {
            acChargingCurrentLimitSupported = false;
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Send AC charging current limit setting command.
     */
    public boolean setAcChargingCurrentLimit(Object chargingDev, int amps) {
        if (chargingDev == null) {
            logger.warn("setAcChargingCurrentLimit: chargingDevice is null");
            return false;
        }
        try {
            Method m = chargingDev.getClass().getMethod("setAcChargingCurrentLimitValue", int.class);
            Object res = m.invoke(chargingDev, amps);
            logger.info("setAcChargingCurrentLimit(" + amps + "A) result=" + res);
            return res != null && !Boolean.FALSE.equals(res);
        } catch (NoSuchMethodException e) {
            logger.debug("setAcChargingCurrentLimitValue method not found on device");
            return false;
        } catch (Exception e) {
            logger.warn("setAcChargingCurrentLimit failed: " + e.getMessage());
            return false;
        }
    }
}
