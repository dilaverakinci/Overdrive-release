package com.overdrive.app.charging.station

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.location.LocationManager
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Service managing Turkey EV Charging Stations Database (22,000+ stations) and Custom User Stations.
 * Compatible with Navion's ev_stations.db SQLite format.
 *
 * Capabilities:
 * - 100% Offline operation (22,000+ stations stored in local SQLite).
 * - High-speed geographic bounding-box and Haversine distance calculations.
 * - Manages custom user-defined charging locations (Home Wallbox, Office, etc.) in 'custom_ev_stations.json'.
 * - Instant proximity matching (find charging station within 400m of current GPS).
 */
class EvStationRepository private constructor(private val context: Context) {

    private val dbName = "ev_stations.db"
    private val legacyDbName = "turkey_ev_stations.db"
    private var database: SQLiteDatabase? = null

    private val customStationsFile by lazy { File(context.filesDir, "custom_ev_stations.json") }
    private val customStations = mutableListOf<EvStation>()

    init {
        ensureDatabaseExtracted()
        openDatabase()
        loadCustomStations()
    }

    private fun ensureDatabaseExtracted() {
        try {
            val dbFile = context.getDatabasePath(dbName)
            val legacyDbFile = context.getDatabasePath(legacyDbName)

            if (legacyDbFile.exists() && !dbFile.exists()) {
                legacyDbFile.renameTo(dbFile)
                Log.i(TAG, "Legacy $legacyDbName migrated to $dbName")
            }

            val assetName = try {
                val assetsList = context.assets.list("") ?: emptyArray()
                if (assetsList.contains(dbName)) dbName
                else if (assetsList.contains(legacyDbName)) legacyDbName
                else dbName
            } catch (_: Throwable) {
                dbName
            }

            val assetSize = try {
                context.assets.open(assetName).use { it.available().toLong() }
            } catch (_: Throwable) {
                0L
            }

            val shouldExtract = !dbFile.exists() || dbFile.length() < 9_000_000L ||
                    (assetSize > 0 && dbFile.length() != assetSize)

            if (shouldExtract) {
                dbFile.parentFile?.mkdirs()
                if (dbFile.exists()) {
                    dbFile.delete()
                }
                context.assets.open(assetName).use { input ->
                    FileOutputStream(dbFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.i(TAG, "$dbName extracted successfully (${dbFile.length()} bytes)")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Database extraction error: ${e.message}", e)
        }
    }

    private fun openDatabase() {
        try {
            val dbFile = context.getDatabasePath(dbName)
            if (dbFile.exists()) {
                database = SQLiteDatabase.openDatabase(
                    dbFile.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
                )
                Log.i(TAG, "Opened $dbName successfully")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Database open error: ${e.message}", e)
        }
    }

    // =========================================================================
    // CUSTOM USER STATIONS (Home Wallbox, Office, etc.)
    // =========================================================================

    @Synchronized
    private fun loadCustomStations() {
        customStations.clear()
        if (!customStationsFile.exists()) return

        try {
            val jsonStr = customStationsFile.readText(Charsets.UTF_8)
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                customStations.add(
                    EvStation(
                        id = obj.optString("id", "custom_${System.currentTimeMillis()}_$i"),
                        operator = obj.optString("operator", "Özel"),
                        name = obj.optString("name", "Özel Şarj"),
                        city = obj.optString("city", ""),
                        district = obj.optString("district", ""),
                        address = obj.optString("address", ""),
                        latitude = obj.optDouble("latitude", 0.0),
                        longitude = obj.optDouble("longitude", 0.0),
                        chargingType = obj.optString("chargingType", "AC"),
                        maxPowerKw = obj.optDouble("maxPowerKw", 11.0),
                        socketCount = obj.optInt("socketCount", 1),
                        acPrice = obj.optDouble("acPrice", 0.0),
                        dcPrice = obj.optDouble("dcPrice", 0.0),
                        logoUrl = obj.optString("logoUrl", ""),
                        connectorsJson = obj.optString("connectorsJson", ""),
                        isCustom = true
                    )
                )
            }
            Log.i(TAG, "Loaded ${customStations.size} custom stations")
        } catch (e: Throwable) {
            Log.e(TAG, "Error loading custom stations: ${e.message}")
        }
    }

    @Synchronized
    private fun saveCustomStations() {
        try {
            val jsonArray = JSONArray()
            for (st in customStations) {
                val obj = JSONObject().apply {
                    put("id", st.id)
                    put("operator", st.operator)
                    put("name", st.name)
                    put("city", st.city)
                    put("district", st.district)
                    put("address", st.address)
                    put("latitude", st.latitude)
                    put("longitude", st.longitude)
                    put("chargingType", st.chargingType)
                    put("maxPowerKw", st.maxPowerKw)
                    put("socketCount", st.socketCount)
                    put("acPrice", st.acPrice)
                    put("dcPrice", st.dcPrice)
                    put("logoUrl", st.logoUrl)
                    put("connectorsJson", st.connectorsJson)
                    put("isCustom", true)
                }
                jsonArray.put(obj)
            }
            customStationsFile.writeText(jsonArray.toString(2), Charsets.UTF_8)
            Log.i(TAG, "Saved ${customStations.size} custom stations")
        } catch (e: Throwable) {
            Log.e(TAG, "Error saving custom stations: ${e.message}")
        }
    }

    @Synchronized
    fun addCustomStation(station: EvStation) {
        val safeStation = if (!station.isCustom) station.copy(isCustom = true) else station
        customStations.removeAll { it.id == safeStation.id || (it.name.equals(safeStation.name, ignoreCase = true) && it.city.equals(safeStation.city, ignoreCase = true)) }
        customStations.add(0, safeStation)
        saveCustomStations()
    }

    @Synchronized
    fun deleteCustomStation(stationId: String) {
        customStations.removeAll { it.id == stationId }
        saveCustomStations()
    }

    @Synchronized
    fun getCustomStations(): List<EvStation> {
        return customStations.toList()
    }

    // =========================================================================
    // SEARCH & GEOGRAPHIC PROXIMITY QUERIES
    // =========================================================================

    /**
     * Find nearest charging stations ordered by distance from given GPS coordinates.
     * Searches both 22,000+ local SQLite stations and user's custom stations.
     */
    fun findNearestStations(
        lat: Double,
        lng: Double,
        radiusKm: Double = 30.0,
        limit: Int = 50
    ): List<EvStation> {
        val list = mutableListOf<EvStation>()

        // 1. Custom Stations with distance
        synchronized(this) {
            for (cs in customStations) {
                val dist = if (cs.latitude != 0.0 && cs.longitude != 0.0) {
                    calculateHaversineDistanceKm(lat, lng, cs.latitude, cs.longitude)
                } else 0.0

                if (dist <= radiusKm || (cs.latitude == 0.0 && cs.longitude == 0.0)) {
                    list.add(cs.copy(distanceKm = dist))
                }
            }
        }

        // 2. Query SQLite via fast bounding-box
        val db = database
        if (db != null) {
            val latDelta = radiusKm / 111.0
            val lngDelta = radiusKm / (111.0 * cos(Math.toRadians(lat)).coerceAtLeast(0.1))

            val minLat = lat - latDelta
            val maxLat = lat + latDelta
            val minLng = lng - lngDelta
            val maxLng = lng + lngDelta

            val sql = """
                SELECT id, operator, name, city, district, address, latitude, longitude, charging_type, max_power_kw, socket_count, ac_price, dc_price, logo_url, connectors_json
                FROM stations
                WHERE latitude BETWEEN ? AND ? AND longitude BETWEEN ? AND ?
            """.trimIndent()

            try {
                db.rawQuery(sql, arrayOf(minLat.toString(), maxLat.toString(), minLng.toString(), maxLng.toString())).use { cursor ->
                    while (cursor.moveToNext()) {
                        val stLat = cursor.getDouble(6)
                        val stLng = cursor.getDouble(7)
                        val dist = calculateHaversineDistanceKm(lat, lng, stLat, stLng)

                        if (dist <= radiusKm) {
                            list.add(
                                EvStation(
                                    id = cursor.getString(0) ?: "",
                                    operator = cursor.getString(1) ?: "",
                                    name = cursor.getString(2) ?: "",
                                    city = cursor.getString(3) ?: "",
                                    district = cursor.getString(4) ?: "",
                                    address = cursor.getString(5) ?: "",
                                    latitude = stLat,
                                    longitude = stLng,
                                    chargingType = cursor.getString(8) ?: "AC",
                                    maxPowerKw = cursor.getDouble(9),
                                    socketCount = cursor.getInt(10),
                                    acPrice = cursor.getDouble(11),
                                    dcPrice = cursor.getDouble(12),
                                    logoUrl = cursor.getString(13) ?: "",
                                    connectorsJson = cursor.getString(14) ?: "",
                                    distanceKm = dist,
                                    isCustom = false
                                )
                            )
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "findNearestStations error: ${e.message}")
            }
        }

        list.sortBy { it.distanceKm }
        return list.take(limit)
    }

    /**
     * Finds the closest station within tolerance meters (e.g. 400m).
     * Useful for automatic station identification when the car stops or plugs in.
     */
    fun findStationClosestTo(lat: Double, lng: Double, toleranceMeters: Double = 400.0): EvStation? {
        val nearest = findNearestStations(lat, lng, radiusKm = (toleranceMeters / 1000.0) + 0.1, limit = 5)
        return nearest.firstOrNull { it.distanceKm * 1000.0 <= toleranceMeters }
    }

    /**
     * Search stations across name, operator, city, district, or address.
     * If user coordinates are provided, results are sorted by distance.
     */
    fun searchStations(
        query: String,
        userLat: Double? = null,
        userLng: Double? = null,
        limit: Int = 50
    ): List<EvStation> {
        val list = mutableListOf<EvStation>()
        val cleanQuery = query.trim().lowercase()

        // 1. Search in custom stations
        synchronized(this) {
            for (cs in customStations) {
                if (cleanQuery.isEmpty() ||
                    cs.name.lowercase().contains(cleanQuery) ||
                    cs.operator.lowercase().contains(cleanQuery) ||
                    cs.city.lowercase().contains(cleanQuery) ||
                    cs.district.lowercase().contains(cleanQuery) ||
                    cs.address.lowercase().contains(cleanQuery)
                ) {
                    val dist = if (userLat != null && userLng != null && cs.latitude != 0.0 && cs.longitude != 0.0) {
                        calculateHaversineDistanceKm(userLat, userLng, cs.latitude, cs.longitude)
                    } else 0.0
                    list.add(cs.copy(distanceKm = dist))
                }
            }
        }

        // 2. Search in SQLite database
        val db = database
        if (db != null) {
            val searchPattern = "%${query.trim()}%"
            val sql = """
                SELECT id, operator, name, city, district, address, latitude, longitude, charging_type, max_power_kw, socket_count, ac_price, dc_price, logo_url
                FROM stations
                WHERE name LIKE ? OR operator LIKE ? OR city LIKE ? OR district LIKE ? OR address LIKE ?
                LIMIT ?
            """.trimIndent()

            try {
                db.rawQuery(sql, arrayOf(searchPattern, searchPattern, searchPattern, searchPattern, searchPattern, limit.toString())).use { cursor ->
                    while (cursor.moveToNext()) {
                        val stLat = cursor.getDouble(6)
                        val stLng = cursor.getDouble(7)
                        val dist = if (userLat != null && userLng != null) calculateHaversineDistanceKm(userLat, userLng, stLat, stLng) else 0.0

                        list.add(
                            EvStation(
                                id = cursor.getString(0) ?: "",
                                operator = cursor.getString(1) ?: "",
                                name = cursor.getString(2) ?: "",
                                city = cursor.getString(3) ?: "",
                                district = cursor.getString(4) ?: "",
                                address = cursor.getString(5) ?: "",
                                latitude = stLat,
                                longitude = stLng,
                                chargingType = cursor.getString(8) ?: "AC",
                                maxPowerKw = cursor.getDouble(9),
                                socketCount = cursor.getInt(10),
                                acPrice = cursor.getDouble(11),
                                dcPrice = cursor.getDouble(12),
                                logoUrl = cursor.getString(13) ?: "",
                                distanceKm = dist,
                                isCustom = false
                            )
                        )
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "searchStations error: ${e.message}")
            }
        }

        if (userLat != null && userLng != null) {
            list.sortBy { it.distanceKm }
        }
        return list.take(limit)
    }

    /**
     * Map viewport bounding box query.
     */
    fun getStationsInBounds(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double,
        limit: Int = 100
    ): List<EvStation> {
        val list = mutableListOf<EvStation>()

        synchronized(this) {
            for (cs in customStations) {
                if (cs.latitude in minLat..maxLat && cs.longitude in minLng..maxLng) {
                    list.add(cs)
                }
            }
        }

        val db = database ?: return list
        val sql = """
            SELECT id, operator, name, city, district, address, latitude, longitude, charging_type, max_power_kw, socket_count, ac_price, dc_price, logo_url
            FROM stations
            WHERE latitude BETWEEN ? AND ? AND longitude BETWEEN ? AND ?
            LIMIT ?
        """.trimIndent()

        try {
            db.rawQuery(sql, arrayOf(minLat.toString(), maxLat.toString(), minLng.toString(), maxLng.toString(), limit.toString())).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(
                        EvStation(
                            id = cursor.getString(0) ?: "",
                            operator = cursor.getString(1) ?: "",
                            name = cursor.getString(2) ?: "",
                            city = cursor.getString(3) ?: "",
                            district = cursor.getString(4) ?: "",
                            address = cursor.getString(5) ?: "",
                            latitude = cursor.getDouble(6),
                            longitude = cursor.getDouble(7),
                            chargingType = cursor.getString(8) ?: "AC",
                            maxPowerKw = cursor.getDouble(9),
                            socketCount = cursor.getInt(10),
                            acPrice = cursor.getDouble(11),
                            dcPrice = cursor.getDouble(12),
                            logoUrl = cursor.getString(13) ?: "",
                            isCustom = false
                        )
                    )
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "getStationsInBounds error: ${e.message}")
        }
        return list
    }

    fun calculateHaversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    companion object {
        private const val TAG = "EvStationRepo"

        @Volatile
        private var INSTANCE: EvStationRepository? = null

        fun getInstance(context: Context): EvStationRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EvStationRepository(context.applicationContext).also { INSTANCE = it }
            }
        }

        @SuppressLint("MissingPermission")
        fun getLastKnownLocation(context: Context): Pair<Double, Double>? {
            return try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
                val gps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (gps != null && gps.latitude != 0.0 && gps.longitude != 0.0) {
                    return gps.latitude to gps.longitude
                }
                val net = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (net != null && net.latitude != 0.0 && net.longitude != 0.0) {
                    return net.latitude to net.longitude
                }
                val passive = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                if (passive != null && passive.latitude != 0.0 && passive.longitude != 0.0) {
                    return passive.latitude to passive.longitude
                }
                null
            } catch (_: Throwable) {
                null
            }
        }
    }
}
