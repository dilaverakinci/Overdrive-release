package com.overdrive.app.domain.diagnostics

import com.overdrive.app.byd.BydVehicleData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Default implementation of DiagnosticsRepository computing real-time telemetry snapshots
 * and health evaluation for all 9 automotive ECUs.
 */
class DefaultDiagnosticsRepository : DiagnosticsRepository {

    private val _bmsTelemetry = MutableStateFlow(BmsTelemetry())
    override val bmsTelemetry: StateFlow<BmsTelemetry> = _bmsTelemetry.asStateFlow()

    private val _mcuTelemetry = MutableStateFlow(McuTelemetry())
    override val mcuTelemetry: StateFlow<McuTelemetry> = _mcuTelemetry.asStateFlow()

    private val _vcuTelemetry = MutableStateFlow(VcuTelemetry())
    override val vcuTelemetry: StateFlow<VcuTelemetry> = _vcuTelemetry.asStateFlow()

    private val _espTelemetry = MutableStateFlow(EspTelemetry())
    override val espTelemetry: StateFlow<EspTelemetry> = _espTelemetry.asStateFlow()

    private val _bcmTelemetry = MutableStateFlow(BcmTelemetry())
    override val bcmTelemetry: StateFlow<BcmTelemetry> = _bcmTelemetry.asStateFlow()

    private val _hvacTelemetry = MutableStateFlow(HvacTelemetry())
    override val hvacTelemetry: StateFlow<HvacTelemetry> = _hvacTelemetry.asStateFlow()

    private val _tpmsTelemetry = MutableStateFlow(TpmsTelemetry())
    override val tpmsTelemetry: StateFlow<TpmsTelemetry> = _tpmsTelemetry.asStateFlow()

    private val _epsTelemetry = MutableStateFlow(EpsTelemetry())
    override val epsTelemetry: StateFlow<EpsTelemetry> = _epsTelemetry.asStateFlow()

    private val _epbTelemetry = MutableStateFlow(EpbTelemetry())
    override val epbTelemetry: StateFlow<EpbTelemetry> = _epbTelemetry.asStateFlow()

    private val _ecuSnapshots = MutableStateFlow<Map<EcuType, EcuTelemetrySnapshot>>(emptyMap())
    override val ecuSnapshots: StateFlow<Map<EcuType, EcuTelemetrySnapshot>> = _ecuSnapshots.asStateFlow()

    private val _overallHealth = MutableStateFlow(EcuHealthStatus.OFFLINE)
    override val overallHealth: StateFlow<EcuHealthStatus> = _overallHealth.asStateFlow()

    override fun updateFromSnapshot(data: BydVehicleData) {
        // 1. BMS Telemetry
        val minCellV = data.lowCellVoltage
        val maxCellV = data.highCellVoltage
        val deltaMv = if (!minCellV.isNaN() && !maxCellV.isNaN()) {
            kotlin.math.round((maxCellV - minCellV) * 1000.0).toInt().coerceAtLeast(0)
        } else {
            0
        }
        val isImbalance = deltaMv >= 50 // Cell imbalance alert threshold: 50mV
        val bmsStatus = when {
            isImbalance -> EcuHealthStatus.WARNING
            !data.voltage12v.isNaN() && data.voltage12v < 11.8 -> EcuHealthStatus.WARNING
            !data.socPercent.isNaN() -> EcuHealthStatus.NORMAL
            else -> EcuHealthStatus.OFFLINE
        }
        val bms = BmsTelemetry(
            cellVoltageMinV = minCellV,
            cellVoltageMaxV = maxCellV,
            cellVoltageDeltaMv = deltaMv,
            cellTempMinC = data.lowCellTempC,
            cellTempMaxC = data.highCellTempC,
            cellTempAvgC = data.avgCellTempC,
            packVoltageV = if (!data.hvPackVoltage.isNaN()) data.hvPackVoltage else data.voltage12v,
            packCurrentA = data.hvPackCurrentAmps,
            socPercent = data.socPercent,
            sohPercent = data.sohPercent,
            lowVoltage12v = data.voltage12v,
            isCellImbalanceWarning = isImbalance,
            status = bmsStatus
        )
        _bmsTelemetry.value = bms

        // 2. MCU Telemetry
        val hasMcuData = !data.frontMotorTorque.isNaN() || data.frontMotorSpeed != BydVehicleData.UNAVAILABLE
        val mcuStatus = if (hasMcuData) EcuHealthStatus.NORMAL else EcuHealthStatus.OFFLINE
        val mcu = McuTelemetry(
            frontMotorRpm = if (data.frontMotorSpeed != BydVehicleData.UNAVAILABLE) data.frontMotorSpeed else 0,
            rearMotorRpm = if (data.rearMotorSpeed != BydVehicleData.UNAVAILABLE) data.rearMotorSpeed else 0,
            frontMotorTorqueNm = data.frontMotorTorque,
            rearMotorTorqueNm = Double.NaN,
            motorTempC = Double.NaN,
            inverterTempC = Double.NaN,
            status = mcuStatus
        )
        _mcuTelemetry.value = mcu

        // 3. VCU Telemetry
        val gearName = when (data.gearMode) {
            1 -> "P"
            2 -> "R"
            3 -> "N"
            4 -> "D"
            5 -> "M"
            6 -> "S"
            else -> "P"
        }
        val vcuStatus = if (data.gearMode != BydVehicleData.UNAVAILABLE) EcuHealthStatus.NORMAL else EcuHealthStatus.OFFLINE
        val vcu = VcuTelemetry(
            powerState = if (data.voltageLevelRaw != BydVehicleData.UNAVAILABLE) "LEVEL_${data.voltageLevelRaw}" else "OK",
            gear = gearName,
            driveMode = if (data.operationMode == 1) "ECO" else if (data.operationMode == 3) "SPORT" else "NORMAL",
            speedKmh = if (!data.speedKmh.isNaN()) data.speedKmh else 0.0,
            acceleratorPedalPercent = if (data.accelPercent != BydVehicleData.UNAVAILABLE) data.accelPercent.toDouble() else 0.0,
            isBrakePressed = data.brakePercent > 0,
            status = vcuStatus
        )
        _vcuTelemetry.value = vcu

        // 4. ESP / Chassis Telemetry
        val hasEsp = !data.speedKmh.isNaN() || !data.steeringAngleDegrees.isNaN()
        val espStatus = if (hasEsp) EcuHealthStatus.NORMAL else EcuHealthStatus.OFFLINE
        val esp = EspTelemetry(
            flWheelSpeedKmh = data.speedKmh,
            frWheelSpeedKmh = data.speedKmh,
            rlWheelSpeedKmh = data.speedKmh,
            rrWheelSpeedKmh = data.speedKmh,
            steeringAngleDeg = data.steeringAngleDegrees,
            isAbsActive = false,
            isEspActive = false,
            status = espStatus
        )
        _espTelemetry.value = esp

        // 5. BCM Telemetry
        val isLocked = if (data.doorLockStatus != null && data.doorLockStatus.isNotEmpty()) {
            data.doorLockStatus.all { it != 0 && it != -1 }
        } else null

        val bcm = BcmTelemetry(
            doorsOpenCount = 0,
            isLocked = isLocked,
            isHeadlightOn = data.lowBeam,
            isHighBeamOn = data.highBeam,
            isHazardLightOn = data.hazard,
            seatbeltsFastenedCount = 1,
            status = EcuHealthStatus.NORMAL
        )
        _bcmTelemetry.value = bcm

        // 6. HVAC Telemetry
        val hasHvac = !data.insideTempC.isNaN() || !data.outsideTempC.isNaN()
        val hvac = HvacTelemetry(
            cabinTempC = data.insideTempC,
            targetTempC = if (data.acSetpointDriver != BydVehicleData.UNAVAILABLE) data.acSetpointDriver.toDouble() else Double.NaN,
            isAcOn = data.acStartState == 1,
            fanLevel = if (data.acFanLevel != BydVehicleData.UNAVAILABLE) data.acFanLevel else 0,
            pm25Inside = if (data.pm25Inside != BydVehicleData.UNAVAILABLE) data.pm25Inside else -1,
            pm25Outside = if (data.pm25Outside != BydVehicleData.UNAVAILABLE) data.pm25Outside else -1,
            status = if (hasHvac) EcuHealthStatus.NORMAL else EcuHealthStatus.OFFLINE
        )
        _hvacTelemetry.value = hvac

        // 7. TPMS Telemetry
        val pressuresKpa = data.tyrePressure
        val flBar = if (pressuresKpa != null && pressuresKpa.isNotEmpty() && pressuresKpa[0] != BydVehicleData.UNAVAILABLE) pressuresKpa[0] / 100.0 else Double.NaN
        val frBar = if (pressuresKpa != null && pressuresKpa.size > 1 && pressuresKpa[1] != BydVehicleData.UNAVAILABLE) pressuresKpa[1] / 100.0 else Double.NaN
        val rlBar = if (pressuresKpa != null && pressuresKpa.size > 2 && pressuresKpa[2] != BydVehicleData.UNAVAILABLE) pressuresKpa[2] / 100.0 else Double.NaN
        val rrBar = if (pressuresKpa != null && pressuresKpa.size > 3 && pressuresKpa[3] != BydVehicleData.UNAVAILABLE) pressuresKpa[3] / 100.0 else Double.NaN

        val hasPressureWarn = (flBar in 0.1..2.0) || (frBar in 0.1..2.0) || (rlBar in 0.1..2.0) || (rrBar in 0.1..2.0)
        val tpmsStatus = when {
            hasPressureWarn -> EcuHealthStatus.WARNING
            !flBar.isNaN() && flBar > 0.0 -> EcuHealthStatus.NORMAL
            else -> EcuHealthStatus.OFFLINE
        }
        val tpms = TpmsTelemetry(
            flPressureBar = flBar,
            frPressureBar = frBar,
            rlPressureBar = rlBar,
            rrPressureBar = rrBar,
            flTempC = data.tyreTemperature?.getOrNull(0) ?: -1,
            frTempC = data.tyreTemperature?.getOrNull(1) ?: -1,
            rlTempC = data.tyreTemperature?.getOrNull(2) ?: -1,
            rrTempC = data.tyreTemperature?.getOrNull(3) ?: -1,
            hasPressureWarning = hasPressureWarn,
            status = tpmsStatus
        )
        _tpmsTelemetry.value = tpms

        // 8. EPS Telemetry
        val eps = EpsTelemetry(
            steeringAngleDeg = data.steeringAngleDegrees,
            assistMode = "COMFORT",
            status = if (!data.steeringAngleDegrees.isNaN()) EcuHealthStatus.NORMAL else EcuHealthStatus.OFFLINE
        )
        _epsTelemetry.value = eps

        // 9. EPB Telemetry
        val epb = EpbTelemetry(
            isParkBrakeEngaged = gearName == "P",
            isAutoHoldActive = false,
            status = EcuHealthStatus.NORMAL
        )
        _epbTelemetry.value = epb

        // Build 9 Snapshots map
        val snapshots = mapOf(
            EcuType.BMS to EcuTelemetrySnapshot(
                ecu = EcuType.BMS,
                status = bms.status,
                primaryMetric = if (!bms.socPercent.isNaN()) "%${bms.socPercent.toInt()} SOC" else "--",
                secondaryMetric = "Δ ${bms.cellVoltageDeltaMv} mV",
                details = mapOf(
                    "Min Hücre" to formatV(bms.cellVoltageMinV),
                    "Max Hücre" to formatV(bms.cellVoltageMaxV),
                    "Paket Voltajı" to formatV(bms.packVoltageV),
                    "Paket Akımı" to formatA(bms.packCurrentA),
                    "12V Akü" to formatV(bms.lowVoltage12v),
                    "SOH Sağlık" to if (!bms.sohPercent.isNaN()) "%${bms.sohPercent.toInt()}" else "--"
                ),
                errorNotes = if (bms.isCellImbalanceWarning) "Hücre voltaj dengesizliği (Δ > 50mV)" else null
            ),
            EcuType.MCU to EcuTelemetrySnapshot(
                ecu = EcuType.MCU,
                status = mcu.status,
                primaryMetric = "${mcu.frontMotorRpm} RPM",
                secondaryMetric = if (!mcu.frontMotorTorqueNm.isNaN()) "${mcu.frontMotorTorqueNm.toInt()} Nm" else "--",
                details = mapOf(
                    "Ön Devir" to "${mcu.frontMotorRpm} RPM",
                    "Arka Devir" to "${mcu.rearMotorRpm} RPM",
                    "Ön Tork" to if (!mcu.frontMotorTorqueNm.isNaN()) "${mcu.frontMotorTorqueNm.toInt()} Nm" else "--",
                    "Motor Sıcaklığı" to formatTemp(mcu.motorTempC),
                    "İnverter Sıcaklığı" to formatTemp(mcu.inverterTempC)
                )
            ),
            EcuType.VCU to EcuTelemetrySnapshot(
                ecu = EcuType.VCU,
                status = vcu.status,
                primaryMetric = "Vites: ${vcu.gear}",
                secondaryMetric = "${vcu.speedKmh.toInt()} km/s",
                details = mapOf(
                    "Sürüş Modu" to vcu.driveMode,
                    "Pedal Gaz" to "%${vcu.acceleratorPedalPercent.toInt()}",
                    "Fren Basılı" to if (vcu.isBrakePressed) "Evet" else "Hayır"
                )
            ),
            EcuType.ESP to EcuTelemetrySnapshot(
                ecu = EcuType.ESP,
                status = esp.status,
                primaryMetric = "Denge Aktif",
                secondaryMetric = if (!esp.steeringAngleDeg.isNaN()) "Açı: ${esp.steeringAngleDeg.toInt()}°" else "--",
                details = mapOf(
                    "ABS Durumu" to if (esp.isAbsActive) "Aktif Müdahale" else "Devrede",
                    "ESP Durumu" to if (esp.isEspActive) "Aktif Müdahale" else "Devrede"
                )
            ),
            EcuType.BCM to EcuTelemetrySnapshot(
                ecu = EcuType.BCM,
                status = bcm.status,
                primaryMetric = if (bcm.doorsOpenCount > 0) "${bcm.doorsOpenCount} Kapak Açık" else "Tüm Kapılar Kapalı",
                secondaryMetric = when (bcm.isLocked) {
                    true -> "Kilitli"
                    false -> "Kilit Açık"
                    else -> "Bilinmiyor"
                },
                details = mapOf(
                    "Farlar" to if (bcm.isHeadlightOn) "Açık" else "Kapalı",
                    "Uzun Farlar" to if (bcm.isHighBeamOn) "Açık" else "Kapalı",
                    "Dörtlü Flaşör" to if (bcm.isHazardLightOn) "Açık" else "Kapalı"
                )
            ),
            EcuType.HVAC to EcuTelemetrySnapshot(
                ecu = EcuType.HVAC,
                status = hvac.status,
                primaryMetric = if (!hvac.cabinTempC.isNaN()) "${String.format(Locale.US, "%.1f", hvac.cabinTempC)}°C" else "--",
                secondaryMetric = if (hvac.isAcOn) "A/C Açık" else "A/C Kapalı",
                details = mapOf(
                    "Hedef Sıcaklık" to if (!hvac.targetTempC.isNaN()) "${hvac.targetTempC.toInt()}°C" else "--",
                    "Fan Kademesi" to "${hvac.fanLevel}",
                    "Kabin İçi PM2.5" to if (hvac.pm25Inside >= 0) "${hvac.pm25Inside} µg/m³" else "--"
                )
            ),
            EcuType.TPMS to EcuTelemetrySnapshot(
                ecu = EcuType.TPMS,
                status = tpms.status,
                primaryMetric = if (!tpms.flPressureBar.isNaN()) "${String.format(Locale.US, "%.1f", tpms.flPressureBar)} Bar" else "--",
                secondaryMetric = if (tpms.hasPressureWarning) "Düşük Basınç" else "Basınç Uygun",
                details = mapOf(
                    "Sol Ön (FL)" to formatBar(tpms.flPressureBar),
                    "Sağ Ön (FR)" to formatBar(tpms.frPressureBar),
                    "Sol Arka (RL)" to formatBar(tpms.rlPressureBar),
                    "Sağ Arka (RR)" to formatBar(tpms.rrPressureBar)
                ),
                errorNotes = if (tpms.hasPressureWarning) "Lastik basıncı 2.0 Bar altında!" else null
            ),
            EcuType.EPS to EcuTelemetrySnapshot(
                ecu = EcuType.EPS,
                status = eps.status,
                primaryMetric = if (!eps.steeringAngleDeg.isNaN()) "${eps.steeringAngleDeg.toInt()}°" else "--",
                secondaryMetric = "Mod: ${eps.assistMode}",
                details = mapOf(
                    "Direksiyon Açısı" to if (!eps.steeringAngleDeg.isNaN()) "${eps.steeringAngleDeg.toInt()}°" else "--",
                    "Destek Modu" to eps.assistMode
                )
            ),
            EcuType.EPB to EcuTelemetrySnapshot(
                ecu = EcuType.EPB,
                status = epb.status,
                primaryMetric = if (epb.isParkBrakeEngaged) "Park Freni Devrede" else "Park Freni Açık",
                secondaryMetric = if (epb.isAutoHoldActive) "Auto-Hold Aktif" else "Beklemede",
                details = mapOf(
                    "El Freni" to if (epb.isParkBrakeEngaged) "Devrede" else "Çözülmüş",
                    "Auto Hold" to if (epb.isAutoHoldActive) "Aktif" else "Pasif"
                )
            )
        )
        _ecuSnapshots.value = snapshots

        // Calculate overall health
        val allStatuses = snapshots.values.map { it.status }
        _overallHealth.value = when {
            allStatuses.any { it == EcuHealthStatus.FAULT } -> EcuHealthStatus.FAULT
            allStatuses.any { it == EcuHealthStatus.WARNING } -> EcuHealthStatus.WARNING
            allStatuses.any { it == EcuHealthStatus.NORMAL } -> EcuHealthStatus.NORMAL
            else -> EcuHealthStatus.OFFLINE
        }
    }

    private fun formatV(v: Double): String {
        return if (v.isNaN()) "--" else String.format(Locale.US, "%.2f V", v)
    }

    private fun formatA(a: Double): String {
        return if (a.isNaN()) "--" else String.format(Locale.US, "%.1f A", a)
    }

    private fun formatBar(b: Double): String {
        return if (b.isNaN()) "--" else String.format(Locale.US, "%.2f Bar", b)
    }

    private fun formatTemp(t: Double): String {
        return if (t.isNaN()) "--" else String.format(Locale.US, "%.1f °C", t)
    }
}
