package com.overdrive.app.ui.trips

import android.content.Context
import com.overdrive.app.logging.DaemonLogger
import com.overdrive.app.util.DaemonHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class TripsRepository(private val context: Context) {

    private val logger = DaemonLogger.getInstance("TripsRepository")

    suspend fun getBootstrap(days: Int = 7, limit: Int = 50, offset: Int = 0): Result<TripsBootstrapResult> = withContext(Dispatchers.IO) {
        try {
            val response = executeHttp("/api/trips/bootstrap?days=$days&limit=$limit&offset=$offset", "GET")
            if (response == null) return@withContext Result.failure(Exception("Empty response from server"))
            val json = JSONObject(response)
            if (!json.optBoolean("success", false)) {
                return@withContext Result.failure(Exception(json.optString("error", "Failed to fetch bootstrap")))
            }
            val bootstrap = json.optJSONObject("bootstrap") ?: JSONObject()
            
            val config = bootstrap.optJSONObject("config")?.let { TripConfigItem.fromJson(it) }
            val storage = bootstrap.optJSONObject("storage")?.let { TripStorageItem.fromJson(it) }
            val dna = bootstrap.optJSONObject("dna")?.let { DnaScoresItem.fromJson(it) }
            val range = bootstrap.optJSONObject("range")?.let { RangeEstimateItem.fromJson(it) }
            
            val summaryArray = bootstrap.optJSONArray("summary")
            val weeklyRollups = mutableListOf<WeeklyRollupItem>()
            if (summaryArray != null) {
                for (i in 0 until summaryArray.length()) {
                    val item = summaryArray.optJSONObject(i)
                    if (item != null) weeklyRollups.add(WeeklyRollupItem.fromJson(item))
                }
            }

            val tripsObj = bootstrap.optJSONObject("trips")
            val tripsArray = tripsObj?.optJSONArray("trips")
            val trips = mutableListOf<TripRecordItem>()
            if (tripsArray != null) {
                for (i in 0 until tripsArray.length()) {
                    val item = tripsArray.optJSONObject(i)
                    if (item != null) trips.add(TripRecordItem.fromJson(item))
                }
            }

            Result.success(
                TripsBootstrapResult(
                    config = config,
                    storage = storage,
                    dna = dna,
                    range = range,
                    weeklyRollups = weeklyRollups,
                    trips = trips
                )
            )
        } catch (e: Exception) {
            logger.error("Error in getBootstrap: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getTrips(days: Int = 7, limit: Int = 50, offset: Int = 0): Result<List<TripRecordItem>> = withContext(Dispatchers.IO) {
        try {
            val response = executeHttp("/api/trips?days=$days&limit=$limit&offset=$offset", "GET")
            if (response == null) return@withContext Result.failure(Exception("Empty response from server"))
            val json = JSONObject(response)
            if (!json.optBoolean("success", false)) {
                return@withContext Result.failure(Exception(json.optString("error", "Failed to fetch trips")))
            }
            val array = json.optJSONArray("trips") ?: JSONArray()
            val list = mutableListOf<TripRecordItem>()
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i)
                if (item != null) list.add(TripRecordItem.fromJson(item))
            }
            Result.success(list)
        } catch (e: Exception) {
            logger.error("Error in getTrips: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getTripDetail(tripId: Long): Result<TripRecordItem> = withContext(Dispatchers.IO) {
        try {
            val response = executeHttp("/api/trips/$tripId", "GET")
            if (response == null) return@withContext Result.failure(Exception("Empty response from server"))
            val json = JSONObject(response)
            if (!json.optBoolean("success", false)) {
                return@withContext Result.failure(Exception(json.optString("error", "Trip not found")))
            }
            val tripJson = json.optJSONObject("trip") ?: return@withContext Result.failure(Exception("Trip object missing"))
            Result.success(TripRecordItem.fromJson(tripJson))
        } catch (e: Exception) {
            logger.error("Error in getTripDetail: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getTelemetry(tripId: Long): Result<List<TelemetrySampleItem>> = withContext(Dispatchers.IO) {
        try {
            val response = executeHttp("/api/trips/$tripId/telemetry", "GET")
            if (response == null) return@withContext Result.failure(Exception("Empty response from server"))
            val json = JSONObject(response)
            if (!json.optBoolean("success", false)) {
                return@withContext Result.failure(Exception(json.optString("error", "Telemetry not found")))
            }
            val array = json.optJSONArray("telemetry") ?: JSONArray()
            val samples = mutableListOf<TelemetrySampleItem>()
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i)
                if (item != null) samples.add(TelemetrySampleItem.fromJson(item))
            }
            Result.success(samples)
        } catch (e: Exception) {
            logger.error("Error in getTelemetry: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteTrip(tripId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = executeHttp("/api/trips/$tripId", "DELETE")
            if (response == null) return@withContext Result.failure(Exception("Empty response from server"))
            val json = JSONObject(response)
            if (!json.optBoolean("success", false)) {
                return@withContext Result.failure(Exception(json.optString("error", "Failed to delete trip")))
            }
            Result.success(true)
        } catch (e: Exception) {
            logger.error("Error in deleteTrip: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun rescoreTrip(tripId: Long): Result<TripRecordItem> = withContext(Dispatchers.IO) {
        try {
            val response = executeHttp("/api/trips/$tripId/rescore", "POST")
            if (response == null) return@withContext Result.failure(Exception("Empty response from server"))
            val json = JSONObject(response)
            if (!json.optBoolean("success", false)) {
                return@withContext Result.failure(Exception(json.optString("error", "Failed to rescore trip")))
            }
            val tripJson = json.optJSONObject("trip") ?: return@withContext Result.failure(Exception("Missing rescored trip"))
            Result.success(TripRecordItem.fromJson(tripJson))
        } catch (e: Exception) {
            logger.error("Error in rescoreTrip: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun saveConfig(
        enabled: Boolean? = null,
        electricityRate: Double? = null,
        currency: String? = null,
        distanceUnit: String? = null,
        tankCapacityL: Double? = null,
        fuelPricePerL: Double? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject()
            if (enabled != null) body.put("enabled", enabled)
            if (electricityRate != null) body.put("electricityRate", electricityRate)
            if (currency != null) body.put("currency", currency)
            if (distanceUnit != null) body.put("distanceUnit", distanceUnit)
            if (tankCapacityL != null) body.put("tankCapacityL", tankCapacityL)
            if (fuelPricePerL != null) body.put("fuelPricePerL", fuelPricePerL)

            val response = executeHttp("/api/trips/config", "POST", body.toString())
            if (response == null) return@withContext Result.failure(Exception("Empty response from server"))
            val json = JSONObject(response)
            if (!json.optBoolean("success", false)) {
                return@withContext Result.failure(Exception(json.optString("error", "Failed to save config")))
            }
            Result.success(true)
        } catch (e: Exception) {
            logger.error("Error in saveConfig: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun saveStorage(
        storageType: String? = null,
        limitMb: Long? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject()
            if (storageType != null) body.put("storageType", storageType)
            if (limitMb != null) body.put("limitMb", limitMb)

            val response = executeHttp("/api/trips/storage", "POST", body.toString())
            if (response == null) return@withContext Result.failure(Exception("Empty response from server"))
            val json = JSONObject(response)
            if (!json.optBoolean("success", false)) {
                return@withContext Result.failure(Exception(json.optString("error", "Failed to save storage")))
            }
            Result.success(true)
        } catch (e: Exception) {
            logger.error("Error in saveStorage: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun startRecovery(): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val response = executeHttp("/api/trips/recover", "POST")
            if (response == null) return@withContext Result.failure(Exception("Empty response from server"))
            val json = JSONObject(response)
            Result.success(json)
        } catch (e: Exception) {
            logger.error("Error in startRecovery: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getRecoveryStatus(): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val response = executeHttp("/api/trips/recover/status", "GET")
            if (response == null) return@withContext Result.failure(Exception("Empty response from server"))
            val json = JSONObject(response)
            Result.success(json)
        } catch (e: Exception) {
            logger.error("Error in getRecoveryStatus: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun executeHttp(path: String, method: String, body: String? = null): String? {
        val conn = DaemonHttpClient.open(path, method, 4000, 5000)
        if (body != null) {
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.bufferedWriter().use { it.write(body) }
        }
        return try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream ?: conn.inputStream
            BufferedReader(InputStreamReader(stream)).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}

data class TripsBootstrapResult(
    val config: TripConfigItem?,
    val storage: TripStorageItem?,
    val dna: DnaScoresItem?,
    val range: RangeEstimateItem?,
    val weeklyRollups: List<WeeklyRollupItem>,
    val trips: List<TripRecordItem>
)
