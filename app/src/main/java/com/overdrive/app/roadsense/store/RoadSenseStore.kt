package com.overdrive.app.roadsense.store

import android.database.Cursor
import com.overdrive.app.database.OverdriveSqliteMaster
import com.overdrive.app.logging.DaemonLogger
import com.overdrive.app.roadsense.detect.ALTITUDE_UNKNOWN
import com.overdrive.app.roadsense.detect.HazardType
import com.overdrive.app.roadsense.detect.RoadSenseHazard
import com.overdrive.app.roadsense.detect.Severity
import com.overdrive.app.roadsense.detect.StoredHazard
import com.overdrive.app.roadsense.detect.altitudeKnown
import com.overdrive.app.roadsense.detect.altitudeMatches
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Local-first hazard store for RoadSense.
 *
 * Backed by [OverdriveSqliteMaster] in WAL mode. Zero H2 dependencies,
 * zero .lock.db file leakages, zero cross-process locking overhead.
 */
class RoadSenseStore(
    private val master: OverdriveSqliteMaster = OverdriveSqliteMaster.getInstance()
) {

    // ──────────────────────────── Tunables ──────────────────────────────────

    companion object {
        private const val TAG = "RoadSenseStore"
        private val logger = DaemonLogger.getInstance(TAG)

        val DB_PATH = OverdriveSqliteMaster.DEFAULT_DB_PATH

        private const val TABLE = "roadsense_hazards"

        private const val TIGHT_MERGE_RADIUS_M = 4.0
        private const val REPEAT_MERGE_RADIUS_M = 8.0
        private const val REPEAT_PASS_MIN_GAP_MS = 60_000L
        private const val LOCAL_CONFIRM_OBSERVATIONS = 2

        private const val STATUS_CANDIDATE = 0
        private const val STATUS_LOCALLY_CONFIRMED = 1

        const val SOURCE_LOCAL = 0
        const val SOURCE_CLOUD = 1

        private const val CANDIDATE_RETENTION_DAYS = 30L
        private const val PRUNE_INTERVAL_HOURS = 24L

        @Volatile
        private var instance: RoadSenseStore? = null
        private val singletonLock = Any()

        @JvmStatic
        fun getInstance(): RoadSenseStore {
            instance?.let { return it }
            synchronized(singletonLock) {
                instance?.let { return it }
                return RoadSenseStore().also { instance = it }
            }
        }

        @JvmStatic
        fun setInstanceForTesting(testStore: RoadSenseStore?) {
            synchronized(singletonLock) {
                instance = testStore
            }
        }
    }

    // ──────────────────────────── State ─────────────────────────────────────

    private val lock = Any()
    private var scheduler: ScheduledExecutorService? = null

    @Volatile
    private var initialized = false

    @Volatile
    private var running = false

    // ──────────────────────────── Lifecycle ─────────────────────────────────

    fun init() {
        if (initialized) return
        synchronized(lock) {
            if (initialized) return
            logger.info("Initializing RoadSense SQLite store via OverdriveSqliteMaster")
            if (master.open()) {
                initialized = true
                logger.info("RoadSense store initialized successfully")
            } else {
                logger.error("Failed to init RoadSense store via OverdriveSqliteMaster")
            }
        }
    }

    fun start() {
        if (running) return
        if (!initialized) init()
        if (!initialized) {
            logger.error("Cannot start RoadSense prune — store init failed")
            return
        }
        running = true

        scheduler = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "RoadSenseStore").apply {
                priority = Thread.MIN_PRIORITY
                setUncaughtExceptionHandler { _, ex ->
                    logger.error("Uncaught exception in RoadSenseStore thread: " + ex.message, ex)
                }
            }
        }

        scheduler!!.scheduleAtFixedRate({
            try {
                pruneStaleCandidates()
            } catch (t: Throwable) {
                logger.error("Critical error in RoadSense prune task: " + t.message, t)
            }
        }, PRUNE_INTERVAL_HOURS, PRUNE_INTERVAL_HOURS, TimeUnit.HOURS)

        logger.info("RoadSense retention prune started (interval ${PRUNE_INTERVAL_HOURS}h, candidate TTL ${CANDIDATE_RETENTION_DAYS}d)")
    }

    fun stop() {
        running = false
        scheduler?.let { s ->
            s.shutdown()
            try {
                if (!s.awaitTermination(2, TimeUnit.SECONDS)) s.shutdownNow()
            } catch (ie: InterruptedException) {
                s.shutdownNow()
                Thread.currentThread().interrupt()
            }
        }
        scheduler = null
        initialized = false
        logger.info("RoadSense store stopped")
    }

    // ──────────────────────────── Writes ────────────────────────────────────

    @JvmOverloads
    fun upsertDetection(hazard: RoadSenseHazard, nowMs: Long, source: Int = SOURCE_LOCAL): String {
        synchronized(lock) {
            if (!ensureOpen()) return ""
            try {
                val tile = SpatialIndex.tileKey(hazard.lat, hazard.lng)
                val typeOrdinal = hazard.type.ordinal
                val neighbours = SpatialIndex.neighborTiles(hazard.lat, hazard.lng)

                val existing = findMergeTarget(neighbours, typeOrdinal, hazard.lat, hazard.lng, hazard.altitudeM, nowMs)
                if (existing != null) {
                    return mergeInto(existing, hazard, nowMs)
                }

                val id = UUID.randomUUID().toString()
                val sql =
                    "INSERT INTO $TABLE (id, lat, lng, tile, type, severity, heading, confidence, " +
                        "speed_kmh, a_vert_peak, altitude, observations, status, human_verified, source, " +
                        "device_id, created_ms, updated_ms) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
                val args: Array<Any?> = arrayOf(
                    id,
                    hazard.lat,
                    hazard.lng,
                    tile,
                    typeOrdinal,
                    hazard.severity.level,
                    hazard.headingDeg.toDouble(),
                    hazard.confidence.toDouble(),
                    hazard.speedKmh.toDouble(),
                    hazard.aVertPeak.toDouble(),
                    if (altitudeKnown(hazard.altitudeM)) hazard.altitudeM else null,
                    1,
                    STATUS_CANDIDATE,
                    0,
                    source,
                    null,
                    nowMs,
                    nowMs
                )
                master.execSQL(sql, args)
                logger.debug("Inserted candidate $id type=${hazard.type} sev=${hazard.severity.level} conf=${hazard.confidence}")
                return id
            } catch (e: Exception) {
                logger.error("upsertDetection failed: " + e.message, e)
                return ""
            }
        }
    }

    fun upsertCloudHazard(hazard: RoadSenseHazard, nowMs: Long): String =
        upsertDetection(hazard, nowMs, SOURCE_CLOUD)

    private data class MergeTarget(
        val id: String,
        val observations: Int,
        val confidence: Double,
        val severity: Int,
        val aVertPeak: Double,
        val humanVerified: Int,
        val heading: Double,
    )

    private fun findMergeTarget(
        tiles: LongArray,
        typeOrdinal: Int,
        lat: Double,
        lng: Double,
        altitudeM: Double,
        nowMs: Long,
    ): MergeTarget? {
        val placeholders = tiles.joinToString(",") { "?" }
        val sql =
            "SELECT id, lat, lng, observations, confidence, severity, a_vert_peak, " +
                "human_verified, heading, altitude, updated_ms FROM $TABLE WHERE type = ? AND tile IN ($placeholders);"
        val selectionArgs = Array(1 + tiles.size) { i ->
            if (i == 0) typeOrdinal.toString() else tiles[i - 1].toString()
        }
        val rs = master.rawQuery(sql, selectionArgs) ?: return null
        rs.use { cursor ->
            var best: MergeTarget? = null
            var bestDist = REPEAT_MERGE_RADIUS_M
            while (cursor.moveToNext()) {
                val rLat = cursor.getDouble(cursor.getColumnIndexOrThrow("lat"))
                val rLng = cursor.getDouble(cursor.getColumnIndexOrThrow("lng"))
                val d = GeoMath.haversineMeters(lat, lng, rLat, rLng)
                if (d > bestDist) continue

                val altIdx = cursor.getColumnIndexOrThrow("altitude")
                val rAlt = if (cursor.isNull(altIdx)) ALTITUDE_UNKNOWN else cursor.getDouble(altIdx)
                if (!altitudeMatches(altitudeM, rAlt)) continue

                val ageMs = nowMs - cursor.getLong(cursor.getColumnIndexOrThrow("updated_ms"))
                val mergeable = d <= TIGHT_MERGE_RADIUS_M || ageMs >= REPEAT_PASS_MIN_GAP_MS
                if (!mergeable) continue
                bestDist = d
                best = MergeTarget(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    observations = cursor.getInt(cursor.getColumnIndexOrThrow("observations")),
                    confidence = cursor.getDouble(cursor.getColumnIndexOrThrow("confidence")),
                    severity = cursor.getInt(cursor.getColumnIndexOrThrow("severity")),
                    aVertPeak = cursor.getDouble(cursor.getColumnIndexOrThrow("a_vert_peak")),
                    humanVerified = cursor.getInt(cursor.getColumnIndexOrThrow("human_verified")),
                    heading = cursor.getDouble(cursor.getColumnIndexOrThrow("heading")),
                )
            }
            return best
        }
    }

    private fun mergeInto(target: MergeTarget, hazard: RoadSenseHazard, nowMs: Long): String {
        val newObservations = target.observations + 1
        val newConfidence = maxOf(target.confidence, hazard.confidence.toDouble())
        val newSeverity = maxOf(target.severity, hazard.severity.level)
        val newPeak = maxOf(target.aVertPeak, hazard.aVertPeak.toDouble())
        val incomingHeading = hazard.headingDeg.toDouble()
        val newHeading =
            if (target.heading < 0.0 && incomingHeading >= 0.0) incomingHeading
            else target.heading
        val newStatus =
            if (newObservations >= LOCAL_CONFIRM_OBSERVATIONS || target.humanVerified == 1)
                STATUS_LOCALLY_CONFIRMED
            else STATUS_CANDIDATE

        val sql =
            "UPDATE $TABLE SET observations = ?, confidence = ?, severity = ?, " +
                "a_vert_peak = ?, status = ?, heading = ?, updated_ms = ? WHERE id = ?;"
        val args: Array<Any?> = arrayOf(
            newObservations,
            newConfidence,
            newSeverity,
            newPeak,
            newStatus,
            newHeading,
            nowMs,
            target.id
        )
        master.execSQL(sql, args)
        logger.debug("Merged into ${target.id}: obs=$newObservations conf=$newConfidence status=$newStatus")
        return target.id
    }

    fun markHumanVerified(
        id: String,
        confirmed: Boolean,
        correctedSeverity: Int?,
        correctedType: Int?,
        nowMs: Long,
    ) {
        synchronized(lock) {
            if (!ensureOpen()) return
            try {
                if (!confirmed) {
                    val n = master.executeUpdateDelete("DELETE FROM $TABLE WHERE id = ?;", arrayOf(id))
                    logger.info("Rejected hazard $id via Calibration Mode (deleted $n)")
                    return
                }

                val sets = StringBuilder("human_verified = 1, status = ?, updated_ms = ?")
                val argsList = mutableListOf<Any?>()
                argsList.add(STATUS_LOCALLY_CONFIRMED)
                argsList.add(nowMs)
                if (correctedSeverity != null) {
                    sets.append(", severity = ?")
                    argsList.add(correctedSeverity)
                }
                if (correctedType != null) {
                    sets.append(", type = ?")
                    argsList.add(correctedType)
                }
                argsList.add(id)

                val sql = "UPDATE $TABLE SET $sets WHERE id = ?;"
                val n = master.executeUpdateDelete(sql, argsList.toTypedArray())
                logger.info("Human-verified hazard $id (sevΔ=$correctedSeverity typeΔ=$correctedType, updated $n)")
            } catch (e: Exception) {
                logger.error("markHumanVerified failed: " + e.message, e)
            }
        }
    }

    // ──────────────────────────── Reads ─────────────────────────────────────

    fun queryAhead(
        lat: Double,
        lng: Double,
        @Suppress("UNUSED_PARAMETER") bearingDeg: Double,
        maxResults: Int,
    ): List<StoredHazard> {
        synchronized(lock) {
            if (!ensureOpen()) return emptyList()
            try {
                val tiles = SpatialIndex.neighborTiles(lat, lng)
                val placeholders = tiles.joinToString(",") { "?" }
                val sql =
                    "SELECT * FROM $TABLE WHERE tile IN ($placeholders) " +
                        "ORDER BY updated_ms DESC LIMIT ?;"
                val selectionArgs = Array(tiles.size + 1) { i ->
                    if (i < tiles.size) tiles[i].toString() else maxResults.toString()
                }
                val rs = master.rawQuery(sql, selectionArgs) ?: return emptyList()
                rs.use { cursor -> return readHazards(cursor) }
            } catch (e: Exception) {
                logger.error("queryAhead failed: " + e.message, e)
                return emptyList()
            }
        }
    }

    fun queryForUpload(minConfidence: Double, sinceMs: Long): List<StoredHazard> {
        synchronized(lock) {
            if (!ensureOpen()) return emptyList()
            try {
                val sql =
                    "SELECT * FROM $TABLE WHERE confidence >= ? AND updated_ms > ? " +
                        "AND source = ? ORDER BY updated_ms ASC;"
                val selectionArgs = arrayOf(minConfidence.toString(), sinceMs.toString(), SOURCE_LOCAL.toString())
                val rs = master.rawQuery(sql, selectionArgs) ?: return emptyList()
                rs.use { cursor -> return readHazards(cursor) }
            } catch (e: Exception) {
                logger.error("queryForUpload failed: " + e.message, e)
                return emptyList()
            }
        }
    }

    fun queryByBbox(
        minLat: Double,
        minLng: Double,
        maxLat: Double,
        maxLng: Double,
        maxResults: Int,
    ): List<StoredHazard> {
        synchronized(lock) {
            if (!ensureOpen()) return emptyList()
            try {
                val sql =
                    "SELECT * FROM $TABLE WHERE lat >= ? AND lat <= ? AND lng >= ? AND lng <= ? " +
                        "ORDER BY updated_ms DESC LIMIT ?;"
                val selectionArgs = arrayOf(
                    minLat.toString(),
                    maxLat.toString(),
                    minLng.toString(),
                    maxLng.toString(),
                    maxResults.toString()
                )
                val rs = master.rawQuery(sql, selectionArgs) ?: return emptyList()
                rs.use { cursor -> return readHazards(cursor) }
            } catch (e: Exception) {
                logger.error("queryByBbox failed: " + e.message, e)
                return emptyList()
            }
        }
    }

    private fun readHazards(cursor: Cursor): List<StoredHazard> {
        val out = ArrayList<StoredHazard>()
        val idIdx = cursor.getColumnIndexOrThrow("id")
        val latIdx = cursor.getColumnIndexOrThrow("lat")
        val lngIdx = cursor.getColumnIndexOrThrow("lng")
        val typeIdx = cursor.getColumnIndexOrThrow("type")
        val sevIdx = cursor.getColumnIndexOrThrow("severity")
        val headIdx = cursor.getColumnIndexOrThrow("heading")
        val confIdx = cursor.getColumnIndexOrThrow("confidence")
        val spdIdx = cursor.getColumnIndexOrThrow("speed_kmh")
        val peakIdx = cursor.getColumnIndexOrThrow("a_vert_peak")
        val altIdx = cursor.getColumnIndexOrThrow("altitude")
        val statIdx = cursor.getColumnIndexOrThrow("status")
        val obsIdx = cursor.getColumnIndexOrThrow("observations")
        val humIdx = cursor.getColumnIndexOrThrow("human_verified")
        val creIdx = cursor.getColumnIndexOrThrow("created_ms")
        val updIdx = cursor.getColumnIndexOrThrow("updated_ms")

        while (cursor.moveToNext()) {
            val createdMs = cursor.getLong(creIdx)
            val altitudeM = if (cursor.isNull(altIdx)) ALTITUDE_UNKNOWN else cursor.getDouble(altIdx)
            out.add(
                StoredHazard(
                    id = cursor.getString(idIdx),
                    hazard = RoadSenseHazard(
                        lat = cursor.getDouble(latIdx),
                        lng = cursor.getDouble(lngIdx),
                        type = hazardTypeFromOrdinal(cursor.getInt(typeIdx)),
                        severity = severityFromLevel(cursor.getInt(sevIdx)),
                        headingDeg = cursor.getDouble(headIdx).toFloat(),
                        confidence = cursor.getDouble(confIdx).toFloat(),
                        speedKmh = cursor.getDouble(spdIdx).toFloat(),
                        aVertPeak = cursor.getDouble(peakIdx).toFloat(),
                        tMs = createdMs,
                        altitudeM = altitudeM,
                    ),
                    status = cursor.getInt(statIdx),
                    observations = cursor.getInt(obsIdx),
                    humanVerified = cursor.getInt(humIdx) != 0,
                    createdMs = createdMs,
                    updatedMs = cursor.getLong(updIdx),
                )
            )
        }
        return out
    }

    // ──────────────────────────── Maintenance ───────────────────────────────

    fun deleteAllLocal(): Long {
        synchronized(lock) {
            if (!ensureOpen()) return -1
            try {
                val n = master.executeUpdateDelete("DELETE FROM $TABLE;", null)
                logger.info("deleteAllLocal: cleared $n hazards")
                return n.toLong()
            } catch (e: Exception) {
                logger.error("deleteAllLocal failed: " + e.message, e)
                return -1
            }
        }
    }

    private fun pruneStaleCandidates() {
        synchronized(lock) {
            if (!ensureOpen()) return
            try {
                val cutoff = System.currentTimeMillis() - CANDIDATE_RETENTION_DAYS * 24 * 60 * 60 * 1000L
                val sql =
                    "DELETE FROM $TABLE WHERE status = ? AND human_verified = 0 AND updated_ms < ?;"
                val n = master.executeUpdateDelete(sql, arrayOf(STATUS_CANDIDATE, cutoff))
                if (n > 0) logger.info("Pruned $n stale candidate hazards (older than ${CANDIDATE_RETENTION_DAYS}d)")
            } catch (e: Exception) {
                logger.error("pruneStaleCandidates failed: " + e.message, e)
            }
        }
    }

    // ──────────────────────────── Helpers ───────────────────────────────────

    private fun ensureOpen(): Boolean {
        if (!initialized) {
            init()
        }
        return master.isOpen
    }

    private fun hazardTypeFromOrdinal(ordinal: Int): HazardType {
        val values = HazardType.values()
        return if (ordinal in values.indices) values[ordinal] else HazardType.UNKNOWN
    }

    private fun severityFromLevel(level: Int): Severity =
        when (level) {
            Severity.MINOR.level -> Severity.MINOR
            Severity.MODERATE.level -> Severity.MODERATE
            Severity.SEVERE.level -> Severity.SEVERE
            else -> if (level >= Severity.SEVERE.level) Severity.SEVERE else Severity.MINOR
        }
}
