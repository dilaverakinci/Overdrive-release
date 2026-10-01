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
    private var customBatteryRepo: BatteryRepository? = null

    @Volatile
    private var customPowertrainRepo: PowertrainRepository? = null

    @Volatile
    private var customHvacRepo: HvacRepository? = null

    @Volatile
    private var customBodyworkRepo: BodyworkRepository? = null

    @Volatile
    private var customChassisRepo: ChassisRepository? = null

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
    }

    // Dependency injection helpers for testing
    fun setBatteryRepository(repo: BatteryRepository?) { customBatteryRepo = repo }
    fun setPowertrainRepository(repo: PowertrainRepository?) { customPowertrainRepo = repo }
    fun setHvacRepository(repo: HvacRepository?) { customHvacRepo = repo }
    fun setBodyworkRepository(repo: BodyworkRepository?) { customBodyworkRepo = repo }
    fun setChassisRepository(repo: ChassisRepository?) { customChassisRepo = repo }

    fun resetToDefaults() {
        customBatteryRepo = null
        customPowertrainRepo = null
        customHvacRepo = null
        customBodyworkRepo = null
        customChassisRepo = null
        defaultBatteryRepo = DefaultBatteryRepository()
        defaultPowertrainRepo = DefaultPowertrainRepository()
        defaultHvacRepo = DefaultHvacRepository()
        defaultBodyworkRepo = DefaultBodyworkRepository()
        defaultChassisRepo = DefaultChassisRepository()
    }
}
