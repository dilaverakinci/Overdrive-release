package com.overdrive.app.telemetry

import android.content.Context
import com.overdrive.app.byd.BydDataCollector
import com.overdrive.app.byd.BydDeviceHelper
import com.overdrive.app.domain.model.Gear
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round

/**
 * Araç Canlı Güç ve Tüketim Hesaplama Motoru (Vehicle Power Estimator).
 *
 * 3 Katmanlı Doğrulanmış Hibrit Mimari (Navion / BYD Engine):
 * 1. Şarj istasyonunda veya V2L modunda net DC şarj/deşarj gücü.
 * 2. Donanımsal Motor MCU doğrudan güç okuması (getEnginePower / getMotorPower).
 * 3. Donanım verisi gürültülü veya sıfır olduğunda Aerodinamik + Yuvarlanma Direnci + İvmelenme Fizik Modeli.
 *
 * Sıfır Hız Rejen Filtresi (Zero-Speed Regen Filter):
 * - Hız <= 1.5 km/s veya araç Park/Boş vitesteyken ASLA negatif rejen gücü üretilemez.
 * - 3 km/s altında mekanik disk frenler devreye girdiği için rejen 0'a iner (low-speed taper).
 */
object VehiclePowerEstimator {

    private var cachedEngineDevice: Any? = null
    private var cachedMotorDevice: Any? = null
    private var engineDeviceChecked = false
    private var motorDeviceChecked = false

    // Sabit Hız / İvme Takip Değişkenleri
    @Volatile
    private var lastObservedSpeedKmh: Double = 0.0

    @Volatile
    private var lastSpeedTimestampMs: Long = 0L

    /**
     * BYD Motor Kontrol Ünitesinden (MCU) canlı donanımsal net güç okuması yapar.
     */
    fun getLiveEnginePowerFromHw(context: Context?): Double? {
        // Öncelik 1: BydDataCollector tarafından toplanmış ve ölçeklenmiş motor gücü
        try {
            val collector = BydDataCollector.getInstance()
            if (collector != null && collector.isInitialized) {
                val data = collector.data
                if (data != null && !data.enginePowerKw.isNaN() && abs(data.enginePowerKw) > 0.05) {
                    if (data.enginePowerKw in -200.0..400.0) {
                        return data.enginePowerKw
                    }
                }
            }
        } catch (_: Throwable) {}

        // Öncelik 2: Doğrudan yansıma (reflection) ile BYDAutoEngineDevice veya BYDAutoMotorDevice
        if (context != null) {
            if (!engineDeviceChecked && cachedEngineDevice == null) {
                try {
                    val cls = Class.forName("android.hardware.bydauto.engine.BYDAutoEngineDevice")
                    val m = cls.getMethod("getInstance", Context::class.java)
                    cachedEngineDevice = m.invoke(null, context)
                } catch (_: Throwable) {}
                engineDeviceChecked = true
            }

            if (cachedEngineDevice != null) {
                try {
                    val p = BydDeviceHelper.callGetter(cachedEngineDevice, "getEnginePower")
                    if (p is Number) {
                        val d = p.toDouble()
                        if (d in -200.0..400.0 && abs(d) > 0.05) {
                            return d
                        }
                    }
                } catch (_: Throwable) {}
            }

            if (!motorDeviceChecked && cachedMotorDevice == null) {
                try {
                    val cls = Class.forName("android.hardware.bydauto.motor.BYDAutoMotorDevice")
                    val m = cls.getMethod("getInstance", Context::class.java)
                    cachedMotorDevice = m.invoke(null, context)
                } catch (_: Throwable) {}
                motorDeviceChecked = true
            }

            if (cachedMotorDevice != null) {
                try {
                    val p = BydDeviceHelper.callGetter(cachedMotorDevice, "getMotorPower")
                    if (p is Number) {
                        val d = p.toDouble()
                        if (d in -200.0..400.0 && abs(d) > 0.05) {
                            return d
                        }
                    }
                } catch (_: Throwable) {}
            }
        }

        return null
    }

    /**
     * Anlık net elektriksel gücü (+ kW: Tüketim, - kW: Rejenerasyon / Şarj) hesaplar.
     */
    fun calculateLivePowerKw(
        context: Context?,
        speedKmh: Double,
        gear: Gear,
        accelPercent: Int,
        brakePercent: Int,
        isAcOn: Boolean,
        fanLevel: Int = 1,
        isCharging: Boolean = false,
        chargingPowerKw: Double = 0.0,
        deltaTimeSec: Double = 0.1
    ): Double {
        // 1. Şarj İstasyonunda Şarj Olurken (- kW Bataryaya Dolum)
        if (isCharging && chargingPowerKw > 0.1 && chargingPowerKw < 350.0) {
            return -round(chargingPowerKw * 10.0) / 10.0
        }

        val speed = speedKmh.coerceAtLeast(0.0)
        val accel = accelPercent.coerceIn(0, 100)
        val brake = brakePercent.coerceIn(0, 100)

        // 2. Doğrudan Donanım Motor MCU Net Güç Okuma (Varsa)
        val hwPower = getLiveEnginePowerFromHw(context)
        if (hwPower != null) {
            // SIFIR HIZ REJEN FİLTRESİ:
            // Araç dururken veya ışıkta beklerken motor asla rejen üretemez!
            val filteredHwPower = if (speed <= 1.5 && hwPower < 0.0) 0.0 else hwPower
            return round(filteredHwPower * 10.0) / 10.0
        }

        // 3. Baz Araç & Klima / Aksesuar Tüketimi (Auxiliary Load)
        var auxLoadKw = 0.35
        if (isAcOn) {
            val fanKw = (fanLevel - 1).coerceAtLeast(0) * 0.09
            val compKw = 1.6
            auxLoadKw += (compKw + fanKw)
        }

        // 4. Park (P), Boş (N) veya Araç Hareketsizken (Hız <= 1.5 km/s)
        // EV Fizik Kuralı: Hız 0 iken veya dururken frene basılsa dahi ASLA rejen üretilemez.
        if (gear == Gear.P || gear == Gear.N || speed <= 1.5) {
            lastObservedSpeedKmh = speed
            lastSpeedTimestampMs = System.currentTimeMillis()
            return round(auxLoadKw * 10.0) / 10.0
        }

        // 5. Sürüş Esnasında (D, R, S, M ve Hız > 1.5 km/s)
        if (gear == Gear.D || gear == Gear.R || gear == Gear.S || gear == Gear.M) {
            val now = System.currentTimeMillis()
            val dt = if (deltaTimeSec in 0.05..5.0) {
                deltaTimeSec
            } else if (lastSpeedTimestampMs > 0 && now > lastSpeedTimestampMs) {
                (now - lastSpeedTimestampMs) / 1000.0
            } else {
                0.1
            }
            val speedDeltaRate = if (dt > 0.05) (speed - lastObservedSpeedKmh) / dt else 0.0
            lastObservedSpeedKmh = speed
            lastSpeedTimestampMs = now

            // A. Gaza Basılıyorsa -> Anlık Hızlanma & Çekiş Gücü (+ kW)
            if (accel > 0) {
                val throttleRatio = accel / 100.0
                val baseCruisingKw = 2.8 + (speed * 0.16)
                val accelDemandKw = (throttleRatio * 20.0) + (throttleRatio.pow(1.85) * 160.0)

                val drivingKw = if (throttleRatio <= 0.25) {
                    (baseCruisingKw * (0.65 + 1.2 * throttleRatio)) + (throttleRatio * 16.0)
                } else {
                    (baseCruisingKw * 0.9) + accelDemandKw
                }

                val totalPowerKw = drivingKw + auxLoadKw
                return max(1.0, round(totalPowerKw * 10.0) / 10.0)
            }

            // B. Frene Basılıyorsa -> Rejeneratif Frenleme (- kW)
            // EV motor dinamiği: 3 km/s altında rejen kesilir (Back-EMF biter), mekanik disk frenler tutar.
            if (brake > 0) {
                if (speed <= 3.0) {
                    return round(auxLoadKw * 10.0) / 10.0
                }

                // 3 km/s ile 10 km/s arasında rejen pürüzsüzce 0'a iner (taper-out)
                val lowSpeedTaper = ((speed - 3.0) / 7.0).coerceIn(0.0, 1.0)
                val speedFactor = min(1.0, speed / 65.0) * lowSpeedTaper
                val baseCoastingRegen = (2.8 + (speedFactor * 5.5)) * lowSpeedTaper
                val brakeRatio = brake / 100.0
                val maxBrakeRegen = 38.0
                val brakeAddedRegen = brakeRatio.pow(0.9) * maxBrakeRegen * lowSpeedTaper

                val grossRegenKw = min(50.0, baseCoastingRegen + brakeAddedRegen)
                val netRegenIntoBattery = -(grossRegenKw - (auxLoadKw * 0.5))
                return min(-0.2, round(netRegenIntoBattery * 10.0) / 10.0)
            }

            // C. Pedallar Serbest (Gaz = 0, Fren = 0) ve Araç Hareket Halinde (speed > 3.0 km/s)
            if (speed > 3.0) {
                // Sürücü hız sabitleyici (ACC / Cruise Control) ile mi gidiyor yoksa süzülüyor mu?
                val isCruising = speedDeltaRate >= -0.7

                if (isCruising) {
                    // Sabit Hızda Seyir Gücü (~2100 kg araç aerodinamik + yuvarlanma direnci)
                    val vMs = speed / 3.6
                    val pRollingKw = (260.0 * vMs) / 1000.0
                    val pAeroKw = (0.42 * vMs.pow(3.0)) / 1000.0
                    val cruisingKw = pRollingKw + pAeroKw + auxLoadKw
                    return max(1.0, round(cruisingKw * 10.0) / 10.0)
                } else {
                    // Ayak gazdan çekilmiş, araç yavaşlıyor -> Rejeneratif Süzülme (- kW)
                    val lowSpeedTaper = ((speed - 3.0) / 7.0).coerceIn(0.0, 1.0)
                    val speedFactor = min(1.0, speed / 65.0) * lowSpeedTaper
                    val grossRegenKw = (2.5 + (speedFactor * 5.0)) * lowSpeedTaper
                    val netRegenIntoBattery = -(grossRegenKw - (auxLoadKw * 0.5))
                    return min(-0.2, round(netRegenIntoBattery * 10.0) / 10.0)
                }
            }
        }

        return round(auxLoadKw * 10.0) / 10.0
    }

    /**
     * UI gösterge çubuğu için yumuşatma filtresi (Exponential Moving Average & Deadband).
     * Göstergenin titremesini ve sıçramasını önler.
     */
    fun smoothPowerForDisplay(targetPowerKw: Double, currentDisplayPowerKw: Double): Double {
        if (abs(targetPowerKw - currentDisplayPowerKw) < 0.15) {
            return currentDisplayPowerKw
        }
        val alpha = 0.35 // %35 yeni değer, %65 geçmiş değer ile yumuşak geçiş
        val smoothed = (alpha * targetPowerKw) + ((1.0 - alpha) * currentDisplayPowerKw)
        return round(smoothed * 10.0) / 10.0
    }
}
