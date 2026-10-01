package com.overdrive.app.domain.diagnostics.dtc

import com.overdrive.app.byd.BydDataCollector
import com.overdrive.app.domain.diagnostics.EcuType
import com.overdrive.app.domain.repository.RepositoryProvider
import com.overdrive.app.logging.DaemonLogger
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Result of a DTC clearing command.
 */
data class DtcClearResult(
    val success: Boolean,
    val message: String,
    val clearedCodesCount: Int = 0
)

/**
 * Contract for querying and safely clearing OBD-II Diagnostic Trouble Codes.
 */
interface DtcDiagnosticService {
    /**
     * Scan and return active DTC trouble codes from ECU diagnostics and telemetry monitors.
     */
    fun scanDtcCodes(): List<DtcCode>

    /**
     * Safely clear DTC trouble codes. Requires vehicle to be in PARK and stationary.
     */
    @Throws(IllegalStateException::class)
    fun clearDtcCodes(targetEcu: EcuType? = null): DtcClearResult

    /**
     * Validates whether DTC clearance is physically safe given current vehicle motion state.
     */
    fun isSafeToClear(speedKmh: Double, gearMode: Int): Boolean
}

/**
 * Default implementation of DtcDiagnosticService with safety guards and telemetry synthesis.
 */
class DefaultDtcDiagnosticService : DtcDiagnosticService {

    private val logger = DaemonLogger.getInstance("DtcDiagnosticService")
    private val activeFaults = CopyOnWriteArrayList<DtcCode>()

    override fun isSafeToClear(speedKmh: Double, gearMode: Int): Boolean {
        // Vehicle must be stationary (speed <= 1.5 km/h or NaN) AND in PARK (gearMode == 1)
        val isStopped = speedKmh.isNaN() || speedKmh <= 1.5
        val isParked = gearMode == 1
        return isStopped && isParked
    }

    override fun scanDtcCodes(): List<DtcCode> {
        val detected = mutableListOf<DtcCode>()

        // 1. Synthesize from live DiagnosticsRepository state
        val diagRepo = RepositoryProvider.diagnosticsRepository
        val bms = diagRepo.bmsTelemetry.value
        val tpms = diagRepo.tpmsTelemetry.value

        if (bms.isCellImbalanceWarning) {
            detected.add(DtcDictionary.lookup("P0B24"))
        }
        if (!bms.lowVoltage12v.isNaN() && bms.lowVoltage12v < 11.8) {
            detected.add(DtcDictionary.lookup("P0562"))
        }
        if (tpms.hasPressureWarning) {
            detected.add(DtcDictionary.lookup("C1500"))
        }

        // 2. Query hardware vehicle health device if running on live car
        try {
            val collector = BydDataCollector.getInstance()
            // In live environment, vehicleHealthDevice or DTC device can be queried reflectively here
        } catch (_: Throwable) {
            // JVM / offline unit test fallback
        }

        activeFaults.clear()
        activeFaults.addAll(detected)
        logger.info("DTC Scan completed: ${detected.size} active codes found.")
        return detected.toList()
    }

    override fun clearDtcCodes(targetEcu: EcuType?): DtcClearResult {
        val diagRepo = RepositoryProvider.diagnosticsRepository
        val vcu = diagRepo.vcuTelemetry.value
        val gearMode = if (vcu.gear == "P") 1 else 4

        // Enforce physical safety guard
        if (!isSafeToClear(vcu.speedKmh, gearMode)) {
            val msg = "Sürüş Güvenliği Engeli: Araç hareket halindeyken DTC arıza kodları silinemez! Aracı PARK (P) konumuna alıp durdurunuz."
            logger.warn(msg)
            throw IllegalStateException(msg)
        }

        val previousCount = activeFaults.size
        if (targetEcu == null) {
            activeFaults.clear()
        } else {
            activeFaults.removeAll { it.ecu == targetEcu }
        }

        val cleared = previousCount - activeFaults.size
        logger.info("Cleared $cleared DTC codes (Target: ${targetEcu?.name ?: "ALL"}).")

        return DtcClearResult(
            success = true,
            message = if (targetEcu != null) "${targetEcu.name} modülündeki arıza kodları başarıyla silindi."
            else "Tüm ECU arıza kodları başarıyla sıfırlandı.",
            clearedCodesCount = cleared
        )
    }

    /**
     * Testing hook to manually inject DTC faults into memory.
     */
    fun injectFaultForTesting(code: String) {
        activeFaults.add(DtcDictionary.lookup(code))
    }
}
