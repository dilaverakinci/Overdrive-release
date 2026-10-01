package com.overdrive.app.domain.repository

import com.overdrive.app.byd.BydVehicleData

/**
 * Global provider providing singleton access to vehicle domain repositories.
 * Supports runtime dependency injection and unit test overrides.
 */
object RepositoryProvider {

    @Volatile
    private var defaultBatteryRepo = DefaultBatteryRepository()
    @Volatile
    private var defaultPowertrainRepo = DefaultPowertrainRepository()
    @Volatile
    private var defaultHvacRepo = DefaultHvacRepository()
    @Volatile
    private var defaultBodyworkRepo = DefaultBodyworkRepository()
    @Volatile
    private var defaultChassisRepo = DefaultChassisRepository()
    @Volatile
    private var defaultDiagnosticsRepo = com.overdrive.app.domain.diagnostics.DefaultDiagnosticsRepository()

    @Volatile
    private var customBatteryRepo: BatteryRepository? = null

    @Volatile
    private var customPowertrainRepo: PowertrainRepository? = null

    @Volatile
    private var customHvacRepo: HvacRepository? = null

    @Volatile
    private var customBodyworkRepo: BodyworkRepository? = null

    @Volatile
    private var customChassisRepo: ChassisRepository? = null

    @Volatile
    private var customDiagnosticsRepo: com.overdrive.app.domain.diagnostics.DiagnosticsRepository? = null

    val batteryRepository: BatteryRepository
        get() = customBatteryRepo ?: defaultBatteryRepo

    val powertrainRepository: PowertrainRepository
        get() = customPowertrainRepo ?: defaultPowertrainRepo

    val hvacRepository: HvacRepository
        get() = customHvacRepo ?: defaultHvacRepo

    val bodyworkRepository: BodyworkRepository
        get() = customBodyworkRepo ?: defaultBodyworkRepo

    val chassisRepository: ChassisRepository
        get() = customChassisRepo ?: defaultChassisRepo

    val diagnosticsRepository: com.overdrive.app.domain.diagnostics.DiagnosticsRepository
        get() = customDiagnosticsRepo ?: defaultDiagnosticsRepo

    /**
     * Updates all default reactive repositories with the incoming vehicle snapshot.
     */
    @JvmStatic
    fun updateFromVehicleData(data: BydVehicleData) {
        defaultBatteryRepo.updateFromSnapshot(data)
        defaultPowertrainRepo.updateFromSnapshot(data)
        defaultHvacRepo.updateFromSnapshot(data)
        defaultBodyworkRepo.updateFromSnapshot(data)
        defaultChassisRepo.updateFromSnapshot(data)
        defaultDiagnosticsRepo.updateFromSnapshot(data)
    }

    // Dependency injection helpers for testing
    fun setBatteryRepository(repo: BatteryRepository?) { customBatteryRepo = repo }
    fun setPowertrainRepository(repo: PowertrainRepository?) { customPowertrainRepo = repo }
    fun setHvacRepository(repo: HvacRepository?) { customHvacRepo = repo }
    fun setBodyworkRepository(repo: BodyworkRepository?) { customBodyworkRepo = repo }
    fun setChassisRepository(repo: ChassisRepository?) { customChassisRepo = repo }
    fun setDiagnosticsRepository(repo: com.overdrive.app.domain.diagnostics.DiagnosticsRepository?) { customDiagnosticsRepo = repo }

    fun resetToDefaults() {
        customBatteryRepo = null
        customPowertrainRepo = null
        customHvacRepo = null
        customBodyworkRepo = null
        customChassisRepo = null
        customDiagnosticsRepo = null
        defaultBatteryRepo = DefaultBatteryRepository()
        defaultPowertrainRepo = DefaultPowertrainRepository()
        defaultHvacRepo = DefaultHvacRepository()
        defaultBodyworkRepo = DefaultBodyworkRepository()
        defaultChassisRepo = DefaultChassisRepository()
        defaultDiagnosticsRepo = com.overdrive.app.domain.diagnostics.DefaultDiagnosticsRepository()
    }
}
