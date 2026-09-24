package com.overdrive.app.byd.diagnostics;

import android.content.Context;
import android.os.SystemClock;

import com.overdrive.app.byd.BydDeviceHelper;
import com.overdrive.app.daemon.CameraDaemon;
import com.overdrive.app.logging.DaemonLogger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Vehicle Health Diagnostics Engine.
 * Ported and adapted from navion_byd RealBydHealthDiagnosticAdapter.
 * Queries 9 core ECU modules for warning indicators, faults, and status flags
 * safely via BydDeviceHelper reflection.
 */
public class VehicleHealthDiagnostics {

    private static final String TAG = "VehicleHealthDiagnostics";
    private static final DaemonLogger logger = DaemonLogger.getInstance(TAG);

    private static final long CACHE_TTL_MS = 5000L; // 5 seconds cache
    private static volatile JSONObject sCachedHealth = null;
    private static volatile long sLastCollectionElapsedMs = 0L;
    private static final Object sLock = new Object();

    public static class HealthItem {
        public final String id;
        public final String titleTr;
        public final String titleEn;
        public final boolean isNormal;
        public final String statusTextTr;
        public final String statusTextEn;
        public final int rawCode;

        public HealthItem(String id, String titleTr, String titleEn, boolean isNormal,
                          String statusTextTr, String statusTextEn, int rawCode) {
            this.id = id;
            this.titleTr = titleTr;
            this.titleEn = titleEn;
            this.isNormal = isNormal;
            this.statusTextTr = statusTextTr;
            this.statusTextEn = statusTextEn;
            this.rawCode = rawCode;
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("id", id);
                json.put("title", titleTr);
                json.put("titleEn", titleEn);
                json.put("isNormal", isNormal);
                json.put("statusText", statusTextTr);
                json.put("statusTextEn", statusTextEn);
                json.put("rawCode", rawCode);
            } catch (Exception ignored) {}
            return json;
        }
    }

    /**
     * Get or refresh vehicle health report.
     * @param forceRefresh if true, bypasses the 5-second cache
     * @return JSONObject containing overall status and all 9 ECU items
     */
    public static JSONObject getHealthReport(boolean forceRefresh) {
        long nowElapsed = SystemClock.elapsedRealtime();
        synchronized (sLock) {
            if (!forceRefresh && sCachedHealth != null && (nowElapsed - sLastCollectionElapsedMs < CACHE_TTL_MS)) {
                return sCachedHealth;
            }
            sCachedHealth = collectLiveHealth();
            sLastCollectionElapsedMs = nowElapsed;
            return sCachedHealth;
        }
    }

    /**
     * Collect live health data across all 9 ECU modules.
     */
    private static JSONObject collectLiveHealth() {
        JSONObject result = new JSONObject();
        Context context = CameraDaemon.getAppContext();

        Object instrumentDevice = null;
        Object tyreDevice = null;
        Object chargingDevice = null;
        Object gearboxDevice = null;

        if (context != null) {
            try {
                instrumentDevice = BydDeviceHelper.getDevice("android.hardware.bydauto.instrument.BYDAutoInstrumentDevice", context);
                tyreDevice = BydDeviceHelper.getDevice("android.hardware.bydauto.tyre.BYDAutoTyreDevice", context);
                chargingDevice = BydDeviceHelper.getDevice("android.hardware.bydauto.charging.BYDAutoChargingDevice", context);
                gearboxDevice = BydDeviceHelper.getDevice("android.hardware.bydauto.gearbox.BYDAutoGearboxDevice", context);
            } catch (Throwable t) {
                logger.warn("Error acquiring BYD devices for diagnostics: " + t.getMessage());
            }
        }

        Map<String, HealthItem> items = new LinkedHashMap<>();

        // 1. Lastik Basıncı İzleme (TPMS)
        int tpmsSysWarn = getInt(instrumentDevice, "getTyrePressureSYSFailWarnLightState", 0);
        int pressureWarn = getInt(instrumentDevice, "getPressureWarnLightState", 0);
        boolean isTpmsNormal = (tpmsSysWarn == 0 && pressureWarn == 0);
        items.put("tpms", new HealthItem(
                "tpms",
                "Lastik Basıncı İzleme (TPMS)",
                "Tyre Pressure Monitoring (TPMS)",
                isTpmsNormal,
                isTpmsNormal ? "Normal" : (tpmsSysWarn != 0 ? "Sistem Arızası" : "Düşük/Yüksek Basınç"),
                isTpmsNormal ? "Normal" : (tpmsSysWarn != 0 ? "System Failure" : "Pressure Warning"),
                (tpmsSysWarn << 8) | pressureWarn
        ));

        // 2. Direksiyon Sistemi (EPS / Steering)
        int steeringSysWarn = getInt(instrumentDevice, "getSteeringSYSFailWarnLightState", 0);
        int epsFault = getInt(instrumentDevice, "getEPSFaultWarningLight", 0);
        boolean isSteeringNormal = (steeringSysWarn == 0 && epsFault == 0);
        items.put("steering", new HealthItem(
                "steering",
                "Direksiyon Sistemi (EPS)",
                "Electric Power Steering (EPS)",
                isSteeringNormal,
                isSteeringNormal ? "Normal" : "Arıza / İkaz",
                isSteeringNormal ? "Normal" : "Fault / Warning",
                (steeringSysWarn << 8) | epsFault
        ));

        // 3. SRS Hava Yastığı (SRS Airbag)
        int srsWarn = getInt(instrumentDevice, "getSRSFaultWarningLight", 0);
        boolean isSrsNormal = (srsWarn == 0);
        items.put("srsAirbag", new HealthItem(
                "srsAirbag",
                "SRS Hava Yastığı",
                "SRS Airbag System",
                isSrsNormal,
                isSrsNormal ? "Normal" : "Hava Yastığı Arızası",
                isSrsNormal ? "Normal" : "Airbag Fault",
                srsWarn
        ));

        // 4. Güç Sistemi (Powertrain / Motor)
        int pwrSysWarn = getInt(instrumentDevice, "getPowerSysFailWarnLightState", 0);
        int engineWarn = getInt(instrumentDevice, "getEngineFailWarnLightState", 0);
        boolean isPowerSysNormal = (pwrSysWarn == 0 && engineWarn == 0);
        items.put("powerSystem", new HealthItem(
                "powerSystem",
                "Güç Aktarımı & Motor",
                "Powertrain & Motor",
                isPowerSysNormal,
                isPowerSysNormal ? "Normal" : "Güç Sistemi Arızası",
                isPowerSysNormal ? "Normal" : "Powertrain Fault",
                (pwrSysWarn << 8) | engineWarn
        ));

        // 5. Güç Bataryası (Traction Battery)
        int pwrBatFail = getInt(instrumentDevice, "getPowerBatFailWarnLightState", 0);
        int pwrBatHeat = getInt(instrumentDevice, "getPowerBatteryHeatWarnLightState", 0);
        boolean isBatNormal = (pwrBatFail == 0 && pwrBatHeat == 0);
        String batTextTr = isBatNormal ? "Normal" : (pwrBatHeat != 0 ? "Aşırı Sıcaklık" : "Batarya Arızası");
        String batTextEn = isBatNormal ? "Normal" : (pwrBatHeat != 0 ? "Over Temperature" : "Battery Fault");
        items.put("tractionBattery", new HealthItem(
                "tractionBattery",
                "Çekiş Bataryası (HV)",
                "Traction Battery (HV)",
                isBatNormal,
                batTextTr,
                batTextEn,
                (pwrBatFail << 8) | pwrBatHeat
        ));

        // 6. Elektronik Denge (ESC / ESP)
        int espSysWarn = getInt(instrumentDevice, "getESPFailWarnLightState", 0);
        int espFault = getInt(instrumentDevice, "getESPFaultWarningLight", 0);
        boolean isEscNormal = (espSysWarn == 0 && espFault == 0);
        items.put("escStability", new HealthItem(
                "escStability",
                "Elektronik Denge (ESC/ESP)",
                "Electronic Stability (ESC/ESP)",
                isEscNormal,
                isEscNormal ? "Normal" : "Stabilite Arızası",
                isEscNormal ? "Normal" : "Stability Fault",
                (espSysWarn << 8) | espFault
        ));

        // 7. Şarj Sistemi (OBC / DC Şarj)
        int chgState = getInt(chargingDevice, "getChargerState", 0);
        boolean isChgNormal = (chgState != 255);
        items.put("chargingSystem", new HealthItem(
                "chargingSystem",
                "Şarj Sistemi (OBC)",
                "Charging System (OBC)",
                isChgNormal,
                isChgNormal ? "Normal" : "Şarj Modülü Hatası",
                isChgNormal ? "Normal" : "Charger Fault",
                chgState
        ));

        // 8. Park Freni Sistemi (EPB)
        int epbState = getInt(gearboxDevice, "getEPBState", 2);
        int epbFault = getInt(instrumentDevice, "getElectricParkingBrakeFaultWarningLight", 0);
        boolean isEpbNormal = (epbFault == 0 && epbState != 4);
        items.put("epbBrake", new HealthItem(
                "epbBrake",
                "Park Freni (EPB)",
                "Electric Parking Brake (EPB)",
                isEpbNormal,
                isEpbNormal ? "Normal" : "Park Freni Arızası",
                isEpbNormal ? "Normal" : "EPB Fault",
                (epbFault << 8) | epbState
        ));

        // 9. Fren Sistemi (ABS)
        int absFault = getInt(instrumentDevice, "getABSFaultWarningLight", 0);
        int pressSupply = getInt(instrumentDevice, "getPressureSupplySysFailWarnLightState", 0);
        boolean isAbsNormal = (absFault == 0 && pressSupply == 0);
        items.put("absBrake", new HealthItem(
                "absBrake",
                "Fren Sistemi (ABS)",
                "Anti-lock Braking System (ABS)",
                isAbsNormal,
                isAbsNormal ? "Normal" : "Fren / Basınç Arızası",
                isAbsNormal ? "Normal" : "Brake / Pressure Fault",
                (absFault << 8) | pressSupply
        ));

        int faultCount = 0;
        JSONObject itemsJson = new JSONObject();
        JSONArray itemsArray = new JSONArray();

        try {
            for (Map.Entry<String, HealthItem> entry : items.entrySet()) {
                HealthItem item = entry.getValue();
                if (!item.isNormal) faultCount++;
                itemsJson.put(entry.getKey(), item.toJson());
                itemsArray.put(item.toJson());
            }

            boolean allNormal = (faultCount == 0);

            result.put("success", true);
            result.put("timestamp", System.currentTimeMillis());
            result.put("isAllNormal", allNormal);
            result.put("faultCount", faultCount);
            result.put("summaryTextTr", allNormal ? "Tüm Sistemler Normal" : faultCount + " Sistemde Arıza/Uyarı Mevcut");
            result.put("summaryTextEn", allNormal ? "All Systems Normal" : faultCount + " System(s) Reporting Fault");
            result.put("summaryTr", allNormal ? "Tüm Sistemler Normal" : faultCount + " Sistemde Arıza/Uyarı Mevcut");
            result.put("summaryEn", allNormal ? "All Systems Normal" : faultCount + " System(s) Reporting Fault");
            result.put("modules", itemsJson);
            result.put("systems", itemsJson);
            result.put("modulesList", itemsArray);
        } catch (Exception e) {
            logger.error("Failed to build health JSON: " + e.getMessage(), e);
        }

        return result;
    }

    private static int getInt(Object device, String methodName, int defaultValue) {
        if (device == null) return defaultValue;
        try {
            Object val = BydDeviceHelper.callGetter(device, methodName);
            if (val instanceof Number) {
                return ((Number) val).intValue();
            }
        } catch (Throwable t) {
            logger.debug("Failed getter " + methodName + ": " + t.getMessage());
        }
        return defaultValue;
    }
}
