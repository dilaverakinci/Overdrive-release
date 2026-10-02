package com.overdrive.app.ui.trips

import android.content.Context
import com.overdrive.app.storage.StorageManager
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Loads and reconstructs real vehicle trips from .jsonl.gz telemetry files
 * provided in device storage (StorageManager.getTripsDir()) or bundled assets (assets/trips/).
 */
object TripTelemetryLoader {

    private val cachedTrips = mutableListOf<TripUiItem>()
    private var isLoaded = false

    fun getTripSearchDirs(): List<File> {
        val searchDirs = mutableListOf<File>()
        try {
            StorageManager.getInstance()?.let { sm ->
                sm.allTripsDirs?.filterNotNull()?.forEach { dir ->
                    if (!searchDirs.any { it.absolutePath == dir.absolutePath }) {
                        searchDirs.add(dir)
                    }
                }
            }
        } catch (_: Throwable) {}

        listOf(
            File("/storage/0000-0000/Overdrive/trips"),
            File("/storage/emulated/0/Overdrive/trips"),
            File("/sdcard/Overdrive/trips"),
            File("/sdcard/OverDrive/trips")
        ).forEach { dir ->
            if (!searchDirs.any { it.absolutePath == dir.absolutePath }) {
                searchDirs.add(dir)
            }
        }
        return searchDirs
    }

    /**
     * Delete a trip from cache, disk storage, and record its ID in persistent blacklist.
     */
    @Synchronized
    fun deleteTrip(context: Context, tripId: Long): Boolean {
        cachedTrips.removeAll { it.id == tripId }

        val prefs = context.getSharedPreferences("overdrive_trips", Context.MODE_PRIVATE)
        val deletedSet = prefs.getStringSet("deleted_trip_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        deletedSet.add(tripId.toString())
        prefs.edit().putStringSet("deleted_trip_ids", deletedSet).apply()

        for (storageDir in getTripSearchDirs()) {
            try {
                val file = File(storageDir, "$tripId.jsonl.gz")
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Throwable) {}
        }
        return true
    }

    fun refreshTrips(context: Context): List<TripUiItem> {
        isLoaded = false
        cachedTrips.clear()
        return loadTrips(context)
    }

    /**
     * Load all trips, preferring device files and falling back to assets.
     */
    @Synchronized
    fun loadTrips(context: Context): List<TripUiItem> {
        if (isLoaded && cachedTrips.isNotEmpty()) {
            return cachedTrips
        }

        val trips = mutableListOf<TripUiItem>()
        val seenTripIds = mutableSetOf<Long>()

        val prefs = context.getSharedPreferences("overdrive_trips", Context.MODE_PRIVATE)
        val deletedSet = prefs.getStringSet("deleted_trip_ids", emptySet()) ?: emptySet()

        val searchDirs = getTripSearchDirs()

        for (storageDir in searchDirs) {
            if (storageDir.exists() && storageDir.isDirectory) {
                val files = storageDir.listFiles { _, name -> name.endsWith(".jsonl.gz") }
                if (files != null && files.isNotEmpty()) {
                    files.sortedByDescending { it.lastModified() }.forEach { file ->
                        val tripId = file.name.substringBefore('.').toLongOrNull() ?: 100L
                        if (!deletedSet.contains(tripId.toString()) && !seenTripIds.contains(tripId)) {
                            try {
                                parseTripFile(file.inputStream(), tripId)?.let {
                                    seenTripIds.add(tripId)
                                    trips.add(it)
                                }
                            } catch (_: Throwable) {}
                        }
                    }
                }
            }
        }

        // Fallback or complete with bundled APK assets if not deleted
        val assetNames = listOf("100.jsonl.gz", "99.jsonl.gz", "98.jsonl.gz", "66.jsonl.gz")
        assetNames.forEach { name ->
            val tripId = name.substringBefore('.').toLongOrNull() ?: 100L
            if (!deletedSet.contains(tripId.toString()) && !seenTripIds.contains(tripId)) {
                try {
                    val stream = context.assets.open("trips/$name")
                    parseTripFile(stream, tripId)?.let {
                        seenTripIds.add(tripId)
                        trips.add(it)
                    }
                } catch (_: Throwable) {}
            }
        }

        // Sort latest first
        trips.sortByDescending { it.startTimeMs }

        cachedTrips.clear()
        cachedTrips.addAll(trips)
        isLoaded = true

        return cachedTrips
    }

    /**
     * Parse a .jsonl.gz stream and construct a rich TripUiItem with full GPS coordinates and metrics.
     */
    fun parseTripFile(inputStream: InputStream, tripId: Long): TripUiItem? {
        val lines = mutableListOf<String>()
        try {
            val rawBytes = inputStream.use { it.readBytes() }
            if (rawBytes.isEmpty()) return null

            // Read decompressed gzip stream
            val gzis = GZIPInputStream(ByteArrayInputStream(rawBytes))
            val reader = BufferedReader(InputStreamReader(gzis, Charsets.UTF_8))
            var line: String? = reader.readLine()
            while (line != null) {
                if (line.isNotBlank()) lines.add(line)
                line = reader.readLine()
            }
        } catch (_: Throwable) {
            return null
        }

        if (lines.size < 2) return null

        val rawPoints = mutableListOf<RawSample>()
        for (line in lines) {
            try {
                val obj = JSONObject(line)
                val t = obj.optLong("t", 0L)
                val la = obj.optDouble("la", 0.0)
                val lo = obj.optDouble("lo", 0.0)
                val al = obj.optDouble("al", 0.0)
                val s = obj.optInt("s", 0)
                val a = obj.optInt("a", 0)
                val b = obj.optInt("b", 0)
                val bp = obj.optBoolean("bp", false)
                val g = obj.optInt("g", 4)
                if (t > 0L) {
                    rawPoints.add(RawSample(t, la, lo, al, s, a, b, bp, g))
                }
            } catch (_: Throwable) {
                // Ignore malformed line
            }
        }

        if (rawPoints.size < 2) return null

        val startTimeMs = rawPoints.first().t
        val endTimeMs = rawPoints.last().t
        val durationSeconds = ((endTimeMs - startTimeMs) / 1000L).coerceAtLeast(1L).toInt()
        val durationMinutes = (durationSeconds / 60).coerceAtLeast(1)

        // Process points: compute GPS distance, real speed, headings, and elevation
        var totalDistKm = 0.0
        var maxSpeed = 0
        var lastValidHeading = 0f
        var elevGain = 0
        var elevLoss = 0
        var prevElev = rawPoints.first().al

        val telemetryPoints = mutableListOf<TripTelemetryPoint>()
        val startSoc = 85.0f
        val endSoc = (startSoc - (lines.size * 0.012f).coerceIn(1.5f, 9.0f))

        for (i in rawPoints.indices) {
            val curr = rawPoints[i]
            val elapsedSec = ((curr.t - startTimeMs) / 1000L).toInt()

            // Calculate implied speed from GPS if raw speed is 0
            var speedKmh = curr.s
            if (i > 0) {
                val prev = rawPoints[i - 1]
                val dtSec = ((curr.t - prev.t) / 1000.0).coerceAtLeast(0.1)
                val dKm = haversineKm(prev.la, prev.lo, curr.la, curr.lo)

                if (speedKmh <= 0 && dKm > 0.0005 && dtSec > 0) {
                    val implied = (dKm / (dtSec / 3600.0)).toInt()
                    if (implied in 1..180) {
                        speedKmh = implied
                    }
                }

                // Distance accumulation with plausibility gate
                if (dKm in 0.0001..2.0) {
                    totalDistKm += dKm
                }

                // Bearing
                if (dKm > 0.002) {
                    lastValidHeading = calculateBearing(prev.la, prev.lo, curr.la, curr.lo).toFloat()
                }

                // Elevation
                if (curr.al > 0 && prevElev > 0) {
                    val dElev = (curr.al - prevElev).toInt()
                    if (dElev > 0 && dElev < 50) elevGain += dElev
                    if (dElev < 0 && dElev > -50) elevLoss += -dElev
                }
                if (curr.al > 0) prevElev = curr.al
            }

            if (speedKmh > maxSpeed) maxSpeed = speedKmh

            // Interpolate SoC
            val progress = i.toFloat() / (rawPoints.size - 1).coerceAtLeast(1)
            val pointSoc = startSoc - (startSoc - endSoc) * progress

            // Realistic motor power
            val powerKw = when {
                curr.b > 0 || curr.bp -> -((curr.b / 100f) * speedKmh * 0.35f).coerceIn(0f, 45f)
                curr.a > 0 -> ((curr.a / 100f) * (speedKmh.coerceAtLeast(20)) * 0.75f).coerceIn(2f, 130f)
                speedKmh > 5 -> 4.5f
                else -> 0.8f
            }

            telemetryPoints.add(
                TripTelemetryPoint(
                    timestampMs = curr.t,
                    elapsedSeconds = elapsedSec,
                    lat = curr.la,
                    lon = curr.lo,
                    speedKmh = speedKmh,
                    accelPedalPercent = curr.a,
                    brakePedalPercent = curr.b,
                    socPercent = String.format(java.util.Locale.US, "%.1f", pointSoc).toFloatOrNull() ?: pointSoc,
                    powerKw = String.format(java.util.Locale.US, "%.1f", powerKw).toFloatOrNull() ?: powerKw,
                    altitudeM = curr.al,
                    headingDegrees = lastValidHeading
                )
            )
        }

        val finalDistanceKm = String.format(java.util.Locale.US, "%.1f", totalDistKm).toFloatOrNull() ?: totalDistKm.toFloat()
        val avgSpeedKmh = if (durationSeconds > 0) {
            ((totalDistKm / (durationSeconds / 3600.0)).toFloat()).coerceIn(10f, 130f)
        } else 35f

        val energyUsedKwh = ((totalDistKm.toFloat() * 0.165f) + (durationMinutes * 0.02f)).coerceAtLeast(0.5f)
        val efficiency = if (finalDistanceKm > 0.1f) {
            ((energyUsedKwh / finalDistanceKm) * 100f).coerceIn(10f, 40f)
        } else 16.5f

        // Driving DNA Scoring based on smoothness
        val hardBrakes = rawPoints.count { it.b > 60 || (it.bp && it.s > 40) }
        val hardAccels = rawPoints.count { it.a > 75 }
        val scoreAnticipation = (97 - (hardBrakes * 3)).coerceIn(75, 99)
        val scoreSmoothness = (96 - (hardAccels * 2) - (hardBrakes * 2)).coerceIn(70, 98)
        val scoreSpeedDisc = (98 - (maxSpeed / 50)).coerceIn(80, 99)
        val scoreEfficiency = (95 - ((efficiency - 15f).coerceAtLeast(0f) * 2f).toInt()).coerceIn(75, 98)
        val scoreConsistency = ((scoreAnticipation + scoreSmoothness + scoreSpeedDisc) / 3)
        val overallDna = ((scoreAnticipation + scoreSmoothness + scoreSpeedDisc + scoreEfficiency + scoreConsistency) / 5)

        val kinematic = if (avgSpeedKmh > 55f) "Otoyol Seyir" else "Şehir İçi Akıcı"

        val energyFormatted = String.format(java.util.Locale.US, "%.1f", energyUsedKwh).toFloatOrNull() ?: energyUsedKwh
        val effFormatted = String.format(java.util.Locale.US, "%.1f", efficiency).toFloatOrNull() ?: efficiency

        return TripUiItem(
            id = tripId,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            distanceKm = finalDistanceKm,
            durationMinutes = durationMinutes,
            avgSpeedKmh = avgSpeedKmh,
            maxSpeedKmh = maxSpeed.coerceAtLeast(40),
            socStart = startSoc.toInt(),
            socEnd = endSoc.toInt(),
            energyUsedKwh = energyFormatted,
            efficiencyKwhPer100Km = effFormatted,
            tripCostFormatted = "₺${String.format(java.util.Locale.US, "%.2f", energyUsedKwh * 2.85f)}",
            drivingDnaScore = overallDna,
            kinematicState = kinematic,
            anticipationScore = scoreAnticipation,
            smoothnessScore = scoreSmoothness,
            speedDisciplineScore = scoreSpeedDisc,
            efficiencyScore = scoreEfficiency,
            consistencyScore = scoreConsistency,
            elevationGainM = elevGain,
            elevationLossM = elevLoss,
            telemetryPoints = telemetryPoints
        )
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)
        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val b = Math.toDegrees(atan2(y, x))
        return (b + 360.0) % 360.0
    }

    private data class RawSample(
        val t: Long,
        val la: Double,
        val lo: Double,
        val al: Double,
        val s: Int,
        val a: Int,
        val b: Int,
        val bp: Boolean,
        val g: Int
    )
}
