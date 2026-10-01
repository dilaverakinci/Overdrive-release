package com.overdrive.app.ui.vehicle

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

data class VehicleControlUiState(
    val security: VehicleSecurityState = VehicleSecurityState(),
    val tyres: VehicleTyresState = VehicleTyresState(),
    val doors: VehicleDoorsState = VehicleDoorsState(),
    val windows: VehicleWindowsState = VehicleWindowsState(),
    val climate: VehicleClimateState = VehicleClimateState(),
    val comfort: VehicleComfortState = VehicleComfortState(),
    val vehicleModelName: String = "",
    val isActionInProgress: Boolean = false,
    val lastActionMessage: String? = null,
)
