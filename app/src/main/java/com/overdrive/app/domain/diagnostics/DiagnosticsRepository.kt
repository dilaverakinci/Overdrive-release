package com.overdrive.app.domain.diagnostics

import com.overdrive.app.byd.BydVehicleData
import kotlinx.coroutines.flow.StateFlow

/**
 * Reactive repository contract for 9-ECU vehicle health and live diagnostics.
 */
interface DiagnosticsRepository {
    /** Map of all 9 ECUs with their latest diagnostic telemetry snapshot. */
    val ecuSnapshots: StateFlow<Map<EcuType, EcuTelemetrySnapshot>>

    /** High-level overall vehicle health status. */
    val overallHealth: StateFlow<EcuHealthStatus>

    val bmsTelemetry: StateFlow<BmsTelemetry>
    val mcuTelemetry: StateFlow<McuTelemetry>
    val vcuTelemetry: StateFlow<VcuTelemetry>
    val espTelemetry: StateFlow<EspTelemetry>
    val bcmTelemetry: StateFlow<BcmTelemetry>
    val hvacTelemetry: StateFlow<HvacTelemetry>
    val tpmsTelemetry: StateFlow<TpmsTelemetry>
    val epsTelemetry: StateFlow<EpsTelemetry>
    val epbTelemetry: StateFlow<EpbTelemetry>

    /**
     * Ingest raw vehicle data snapshot and update all 9 ECU telemetries reactively.
     */
    fun updateFromSnapshot(data: BydVehicleData)
}
