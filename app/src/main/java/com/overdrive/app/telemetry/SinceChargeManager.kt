package com.overdrive.app.telemetry

import com.overdrive.app.byd.BydDataCollector
import com.overdrive.app.byd.BydVehicleData
import com.overdrive.app.logging.DaemonLogger
import com.overdrive.app.monitor.SocHistoryDatabase
import com.overdrive.app.monitor.VehicleDataMonitor
import com.overdrive.app.trips.OdometerReader
import com.overdrive.app.trips.TripAnalyticsManager
import org.json.JSONObject
import java.io.File
import java.util.Locale

/**
 * Manages "Son Şarjdan İtibaren" (Since Last Charge) telemetry (distance, energy, and consumption rate)
 * and realistic dynamic range estimation ("Gerçekçi Menzil").
 *
 * Requirements:
 * 1. Computes distance (km) and energy consumed (kWh) since the vehicle was last charged.
 * 2. Realistic range is calculated dynamically from this energy consumption rate:
 *    Realistic Range = (Current Usable kWh * 100) / Since Last Charge Consumption Rate
 * 3. Can be reset by the user at any time (manual baseline reset).
 * 4. Automatically resets baseline when a new charging session completes.
 */
class SinceChargeManager private constructor() {

    companion object {
        private const val TAG = "SinceChargeManager"
        private val logger = DaemonLogger.getInstance(TAG)
        private const val PERSIST_PATH = "/data/local/tmp/overdrive_since_charge.json"

        @Volatile
        private var instance: SinceChargeManager? = null

        @JvmStatic
        fun getInstance(): SinceChargeManager {
            return instance ?: synchronized(this) {
                instance ?: SinceChargeManager().also { instance = it }
            }
        }
    }

    private var baselineOdometerKm: Double = -1.0
    private var baselineTotalElecConKwh: Double = -1.0
    private var baselineSoc: Double = -1.0
    private var baselineTimestampMs: Long = 0L
    private var isUserReset: Boolean = false

    private var cachedSinceLastChargeKm: Double = 0.0
    private var cachedSinceLastChargeAvgKwh: Double = 0.0
    private var cachedRealisticRangeKm: Int = 0

    private var lastWasCharging: Boolean = false
    private var initialized: Boolean = false

    init {
        loadPersistedBaseline()
    }

    /**
     * Loads saved baseline from storage. If absent, queries the most recent charging session
     * from SocHistoryDatabase or initializes with current vehicle readings.
     */
    @Synchronized
    private fun loadPersistedBaseline() {
        try {
            val file = File(PERSIST_PATH)
            if (file.exists() && file.length() > 0) {
                val jsonStr = file.readText(Charsets.UTF_8)
                val obj = JSONObject(jsonStr)
                baselineTimestampMs = obj.optLong("baselineTimestampMs", 0L)
                baselineOdometerKm = obj.optDouble("baselineOdometerKm", -1.0)
                baselineTotalElecConKwh = obj.optDouble("baselineTotalElecConKwh", -1.0)
                baselineSoc = obj.optDouble("baselineSoc", -1.0)
                isUserReset = obj.optBoolean("isUserReset", false)
                if (baselineTimestampMs > 0 && baselineOdometerKm > 0) {
                    initialized = true
                    logger.info("Loaded since-charge baseline from file: ts=$baselineTimestampMs, odo=$baselineOdometerKm, soc=$baselineSoc, userReset=$isUserReset")
                    return
                }
            }
        } catch (t: Throwable) {
            logger.debug("Failed loading since-charge file: ${t.message}")
        }

        // Fallback: Query SocHistoryDatabase for the most recent completed charging session
        try {
            val socDb = SocHistoryDatabase.getInstance()
            if (socDb != null) {
                val lastCharge = socDb.getMostRecentCompletedChargingSession(8760)
                if (lastCharge != null) {
                    val endMs = lastCharge.optLong("endTime", 0L)
                    val endSoc = lastCharge.optDouble("endSoc", -1.0)
                    val startOdo = lastCharge.optDouble("startOdometer", -1.0)
                    if (endMs > 0) {
                        baselineTimestampMs = endMs
                        baselineSoc = endSoc
                        baselineOdometerKm = startOdo
                        isUserReset = false
                        initialized = true
                        logger.info("Initialized since-charge baseline from SocHistoryDatabase: endMs=$endMs, soc=$endSoc, odo=$startOdo")
                        persistBaseline()
                        return
                    }
                }
            }
        } catch (t: Throwable) {
            logger.debug("Failed checking SocHistoryDatabase for charge baseline: ${t.message}")
        }

        // Last resort: snapshot current vehicle readings if available
        captureCurrentBaseline(userReset = false)
    }

    @Synchronized
    private fun persistBaseline() {
        try {
            val obj = JSONObject().apply {
                put("baselineTimestampMs", baselineTimestampMs)
                put("baselineOdometerKm", baselineOdometerKm)
                put("baselineTotalElecConKwh", baselineTotalElecConKwh)
                put("baselineSoc", baselineSoc)
                put("isUserReset", isUserReset)
            }
            val file = File(PERSIST_PATH)
            file.parentFile?.mkdirs()
            file.writeText(obj.toString(), Charsets.UTF_8)
            try { file.setReadable(true, false); file.setWritable(true, false) } catch (_: Throwable) {}
        } catch (t: Throwable) {
            logger.debug("Failed persisting since-charge baseline: ${t.message}")
        }
    }

    private fun readCurrentOdometer(vd: BydVehicleData?): Double {
        var odo = -1.0
        try {
            odo = OdometerReader.getInstance().readOdometerKm()
        } catch (_: Throwable) {}
        if (odo <= 0 && vd != null && vd.totalMileageKm != BydVehicleData.UNAVAILABLE && vd.totalMileageKm > 0) {
            odo = vd.totalMileageKm.toDouble()
        }
        return odo
    }

    private fun readCurrentTotalElecCon(vd: BydVehicleData?): Double {
        var elecCon = Double.NaN
        try {
            elecCon = VehicleDataMonitor.getInstance().totalElecCon
        } catch (_: Throwable) {}
        if (elecCon.isNaN() && vd != null && !vd.totalElecCon.isNaN()) {
            elecCon = vd.totalElecCon
        }
        return if (!elecCon.isNaN() && elecCon >= 0) elecCon else -1.0
    }

    private fun readCurrentSoc(vd: BydVehicleData?): Double {
        return if (vd != null && !vd.socPercent.isNaN() && vd.socPercent >= 0) vd.socPercent else -1.0
    }

    @Synchronized
    private fun captureCurrentBaseline(userReset: Boolean) {
        val vd = VehicleDataMonitor.getInstance().vd
        val odo = readCurrentOdometer(vd)
        val elecCon = readCurrentTotalElecCon(vd)
        val soc = readCurrentSoc(vd)

        if (odo > 0 || soc > 0) {
            baselineTimestampMs = System.currentTimeMillis()
            baselineOdometerKm = odo
            baselineTotalElecConKwh = elecCon
            baselineSoc = soc
            isUserReset = userReset
            initialized = true
            cachedSinceLastChargeKm = 0.0
            persistBaseline()
            logger.info("Captured baseline (userReset=$userReset): ts=$baselineTimestampMs, odo=$odo, elecCon=$elecCon, soc=$soc")
        }
    }

    /**
     * User-requested reset. Resets the baseline to the current moment and odometer/energy readings.
     */
    @Synchronized
    fun reset() {
        val vd = VehicleDataMonitor.getInstance().vd
        val odo = readCurrentOdometer(vd)
        val elecCon = readCurrentTotalElecCon(vd)
        val soc = readCurrentSoc(vd)

        baselineTimestampMs = System.currentTimeMillis()
        baselineOdometerKm = if (odo > 0) odo else baselineOdometerKm
        baselineTotalElecConKwh = if (elecCon > 0) elecCon else baselineTotalElecConKwh
        baselineSoc = if (soc > 0) soc else baselineSoc
        isUserReset = true
        initialized = true

        cachedSinceLastChargeKm = 0.0
        val last50 = if (vd != null && !vd.last50KmConsumption.isNaN() && vd.last50KmConsumption > 0) vd.last50KmConsumption else 16.5
        cachedSinceLastChargeAvgKwh = Math.round(last50 * 10.0) / 10.0

        val packCap = if (vd != null && !vd.remainKwh.isNaN() && vd.remainKwh > 0) vd.remainKwh else 82.5
        val curSoc = if (soc > 0) soc else 50.0
        val usableKwh = (curSoc / 100.0) * packCap
        cachedRealisticRangeKm = Math.round((usableKwh * 100.0) / cachedSinceLastChargeAvgKwh).toInt()

        persistBaseline()
        logger.info("User reset since-charge baseline successfully: odo=$baselineOdometerKm, elecCon=$baselineTotalElecConKwh, soc=$baselineSoc")
    }

    /**
     * Called when a charging session completes (from ChargingDetector or SocHistoryDatabase).
     */
    @Synchronized
    fun onChargingSessionFinished(chargeEndTimeMs: Long, odoKm: Double, totalElecConKwh: Double, endSoc: Double) {
        baselineTimestampMs = if (chargeEndTimeMs > 0) chargeEndTimeMs else System.currentTimeMillis()
        if (odoKm > 0) baselineOdometerKm = odoKm
        if (totalElecConKwh > 0) baselineTotalElecConKwh = totalElecConKwh
        if (endSoc > 0) baselineSoc = endSoc
        isUserReset = false
        initialized = true
        cachedSinceLastChargeKm = 0.0
        persistBaseline()
        logger.info("Charging finished: new baseline set at odo=$baselineOdometerKm, elecCon=$baselineTotalElecConKwh, endSoc=$baselineSoc")
    }

    /**
     * Main update routine. Computes distance, energy, average consumption, and realistic range.
     */
    @Synchronized
    fun update(data: BydVehicleData?, tam: TripAnalyticsManager?) {
        if (data == null) return

        // 1. Detect charge completion transition
        val isChargingNow = data.chargingGunState == 1 || data.chargingState == 1 ||
                (!data.chargingPowerKw.isNaN() && data.chargingPowerKw > 0.1)

        if (lastWasCharging && !isChargingNow) {
            // Charging just finished!
            val curOdo = readCurrentOdometer(data)
            val curElec = readCurrentTotalElecCon(data)
            val curSoc = readCurrentSoc(data)
            onChargingSessionFinished(System.currentTimeMillis(), curOdo, curElec, curSoc)
        }
        lastWasCharging = isChargingNow

        if (!initialized || baselineOdometerKm <= 0) {
            captureCurrentBaseline(userReset = false)
        }

        val curOdo = readCurrentOdometer(data)
        val curElec = readCurrentTotalElecCon(data)
        val curSoc = readCurrentSoc(data)
        val packCap = if (!data.remainKwh.isNaN() && data.remainKwh > 0) data.remainKwh else 82.5

        // Fallback reference consumption rates from vehicle hardware
        val car50Km = if (!data.last50KmConsumption.isNaN() && data.last50KmConsumption > 0) data.last50KmConsumption else 0.0
        val carLifetime = if (!data.avgElecConPer100Km.isNaN() && data.avgElecConPer100Km > 0) data.avgElecConPer100Km else 0.0
        val refRate = if (car50Km > 0) car50Km else (if (carLifetime > 0) carLifetime else 16.5)

        // 2. Compute Distance since last charge
        var distKm = 0.0
        if (curOdo > 0 && baselineOdometerKm > 0 && curOdo >= baselineOdometerKm) {
            distKm = curOdo - baselineOdometerKm
        } else if (tam != null && tam.database != null && baselineTimestampMs > 0) {
            try {
                val trips = tam.database.getTripsBetween(baselineTimestampMs, System.currentTimeMillis(), 200, 0)
                var sum = 0.0
                if (trips != null) {
                    for (t in trips) {
                        if (t.distanceKm > 0) sum += t.distanceKm
                    }
                }
                val at = tam.activeTrip
                if (at != null && at.distanceKm > 0) sum += at.distanceKm
                distKm = sum
            } catch (_: Throwable) {}
        }
        cachedSinceLastChargeKm = Math.round(distKm * 10.0) / 10.0

        // 3. Compute Energy Consumed since last charge
        var energyKwh = 0.0
        if (curElec > 0 && baselineTotalElecConKwh > 0 && curElec >= baselineTotalElecConKwh) {
            energyKwh = curElec - baselineTotalElecConKwh
        } else if (baselineSoc > 0 && curSoc > 0 && baselineSoc >= curSoc) {
            energyKwh = ((baselineSoc - curSoc) / 100.0) * packCap
        } else if (tam != null && tam.database != null && baselineTimestampMs > 0) {
            try {
                val trips = tam.database.getTripsBetween(baselineTimestampMs, System.currentTimeMillis(), 200, 0)
                var sum = 0.0
                if (trips != null) {
                    for (t in trips) {
                        if (t.distanceKm > 0 && t.energyPerKm > 0) {
                            sum += t.distanceKm * t.energyPerKm
                        }
                    }
                }
                val at = tam.activeTrip
                if (at != null && at.distanceKm > 0) {
                    sum += at.distanceKm * (refRate / 100.0)
                }
                energyKwh = sum
            } catch (_: Throwable) {}
        }

        // 4. Compute Average Consumption Rate (kWh/100km)
        val avgKwhPer100Km: Double
        if (distKm >= 1.0 && energyKwh > 0.05) {
            val measuredRate = (energyKwh / distKm) * 100.0
            if (distKm < 5.0) {
                // Smooth progressive blend with car's 50km reference between 1 km and 5 km
                val weight = (distKm - 1.0) / 4.0
                avgKwhPer100Km = (measuredRate * weight) + (refRate * (1.0 - weight))
            } else {
                avgKwhPer100Km = measuredRate.coerceIn(8.0, 55.0)
            }
        } else {
            // Less than 1 km driven: Use the car hardware's last 50 km consumption rate!
            avgKwhPer100Km = refRate
        }
        cachedSinceLastChargeAvgKwh = Math.round(avgKwhPer100Km * 10.0) / 10.0

        // 5. Realistic Range (Gerçekçi Menzil)
        // Calculated based on energy consumption rate since last charge:
        // Realistic Range = (Current Usable kWh * 100) / Since Last Charge Rate
        val usableKwh = ((if (curSoc > 0) curSoc else 50.0) / 100.0) * packCap
        val safeRate = if (cachedSinceLastChargeAvgKwh in 6.0..55.0) cachedSinceLastChargeAvgKwh else refRate
        cachedRealisticRangeKm = Math.round((usableKwh * 100.0) / safeRate).toInt()
    }

    @Synchronized
    fun getSinceLastChargeKm(): Double = cachedSinceLastChargeKm

    @Synchronized
    fun getSinceLastChargeAvgKwh(): Double = cachedSinceLastChargeAvgKwh

    @Synchronized
    fun getRealisticRangeKm(): Int = cachedRealisticRangeKm
}
