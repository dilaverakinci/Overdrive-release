package com.overdrive.app.ui.vehicle

import com.overdrive.app.domain.model.Gear
import com.overdrive.app.domain.model.OperationMode

/**
 * Vehicle Control Categories for the bottom dock bar.
 */
enum class VehicleControlTab {
    SECURITY,
    TRUNK,
    CLIMATE,
    SEATS,
    WINDOWS,
    LIGHTS,
    ADAS,
    CHARGING,
    SOUND,
    SYSTEM
}

/**
 * State data models for 100% Compose Native Vehicle Control Screen.
 * Fully compatible with VehicleControlApiHandler and BYD vehicle telemetry.
 */
data class VehicleTyresState(
    val flPsi: Float? = null,
    val frPsi: Float? = null,
    val rlPsi: Float? = null,
    val rrPsi: Float? = null,
    val flTemp: Int? = null,
    val frTemp: Int? = null,
    val rlTemp: Int? = null,
    val rrTemp: Int? = null,
    val flKpa: Int? = null,
    val frKpa: Int? = null,
    val rlKpa: Int? = null,
    val rrKpa: Int? = null,
)

data class VehicleDoorsState(
    val frontLeftOpen: Boolean = false,
    val frontRightOpen: Boolean = false,
    val rearLeftOpen: Boolean = false,
    val rearRightOpen: Boolean = false,
    val trunkOpen: Boolean = false,
    val hoodOpen: Boolean = false,
)

data class VehicleWindowsState(
    val frontLeftOpen: Boolean = false,
    val frontRightOpen: Boolean = false,
    val rearLeftOpen: Boolean = false,
    val rearRightOpen: Boolean = false,
    val sunroofOpen: Boolean = false,
    val sunshadeOpen: Boolean = false,
    val isVentMode: Boolean = false,
)

data class VehicleClimateState(
    val isAcOn: Boolean = false,
    val targetTemp: Int = 22,
    val fanLevel: Int = 3,
    val isBatteryHeatOn: Boolean = false,
)

data class VehicleComfortState(
    val driverSeatHeat: Int = 0,      // 0=off, 1=low, 2=med, 3=high
    val passengerSeatHeat: Int = 0,
    val driverSeatVent: Int = 0,      // 0=off, 1=low, 2=med, 3=high
    val passengerSeatVent: Int = 0,
    val steeringHeatOn: Boolean = false,
)

data class VehicleSecurityState(
    val isLocked: Boolean? = null,
    val isCloudConfigured: Boolean = false,
    val isCloudConnected: Boolean = false,
    val cloudStatusText: String = "",
    val mirrorsFolded: Boolean = true,
)

data class VehiclePowertrainUiState(
    val speedKmh: Double = 0.0,
    val powerKw: Double = 0.0,
    val gear: Gear = Gear.P,
    val operationMode: OperationMode = OperationMode.NORMAL,
    val accelPedalPercent: Int = 0,
    val brakePedalPercent: Int = 0,
)

data class VehicleBatteryUiState(
    val socPercent: Int = 0,
    val elecRangeKm: Int = 0,
    val realisticRangeKm: Int = 0,
    val batteryCapacityKwh: Double = 82.5,
    val batteryTempC: Int = 25,
    val sohPercent: Double = 100.0,
    val isCharging: Boolean = false,
    val chargingPowerKw: Double = 0.0,
    val voltage12v: Double = 12.8,
    val avg50KmKwh: Double = 0.0,
    val avgLifetimeKwh: Double = 0.0,
    val sinceLastChargeKm: Double = 0.0,
    val sinceLastChargeAvgKwh: Double = 0.0,
    val activeTripKm: Double = 0.0,
    val activeTripMinutes: Int = 0,
    val regenKwh: Double = 0.0,
)

data class HealthCheckItem(
    val title: String = "",
    val isNormal: Boolean = true,
    val statusText: String = "Normal",
    val detailMessage: String = "",
)

data class VehicleHealthUiState(
    val timestamp: Long = System.currentTimeMillis(),
    val isAllNormal: Boolean = true,
    val tpms: HealthCheckItem = HealthCheckItem("Lastik Basıncı İzleme (TPMS)", true, "Normal"),
    val steering: HealthCheckItem = HealthCheckItem("Direksiyon Sistemi", true, "Normal"),
    val srsAirbag: HealthCheckItem = HealthCheckItem("SRS Hava Yastığı", true, "Normal"),
    val powerSystem: HealthCheckItem = HealthCheckItem("Güç Sistemi", true, "Normal"),
    val tractionBattery: HealthCheckItem = HealthCheckItem("Güç Bataryası", true, "Normal"),
    val escStability: HealthCheckItem = HealthCheckItem("Elektronik Stabilite (ESC)", true, "Normal"),
    val chargingSystem: HealthCheckItem = HealthCheckItem("Şarj Sistemi", true, "Normal"),
    val epbBrake: HealthCheckItem = HealthCheckItem("Park Freni Sistemi (EPB)", true, "Normal"),
    val absBrake: HealthCheckItem = HealthCheckItem("Fren Sistemi (ABS)", true, "Normal"),
)

data class VehicleControlUiState(
    val security: VehicleSecurityState = VehicleSecurityState(),
    val tyres: VehicleTyresState = VehicleTyresState(),
    val doors: VehicleDoorsState = VehicleDoorsState(),
    val windows: VehicleWindowsState = VehicleWindowsState(),
    val climate: VehicleClimateState = VehicleClimateState(),
    val comfort: VehicleComfortState = VehicleComfortState(),
    val powertrain: VehiclePowertrainUiState = VehiclePowertrainUiState(),
    val battery: VehicleBatteryUiState = VehicleBatteryUiState(),
    val health: VehicleHealthUiState = VehicleHealthUiState(),
    val selectedModelId: String? = null,
    val vehicleModelName: String = "",
    val is3DMode: Boolean = false,
    val isAwd: Boolean = false,
    val selectedTab: VehicleControlTab = VehicleControlTab.SECURITY,
    val isActionInProgress: Boolean = false,
    val lastActionMessage: String? = null,
    val isDrlOn: Boolean = false,
    val ambientColorPreset: Int = 1,
    val slwEnabled: Boolean = false,
    val cpdEnabled: Boolean = false,
    val smartChargeEnabled: Boolean = false,
    val chargeCapPercent: Int = 100,
    val acCurrentLimit: Int = 16,
    val activeAvasTone: Int? = null,
    val isEngineSoundOn: Boolean = false,
)
