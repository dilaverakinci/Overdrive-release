package com.overdrive.app.navmap.nav

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Client for global and European EV charging points using the Open Charge Map
 * API v3 ([api.openchargemap.io]).
 *
 * <p>Serves as the secondary EV charging source when browsing outside Turkey or
 * when global/European coverage is desired. Fully non-throwing and graceful:
 * returns an empty list if no key is provided, if rate limited, or on network error,
 * allowing instant fallback to OpenStreetMap / Overpass.
 */
object OpenChargeMapClient {

    private const val TAG = "OpenChargeMapClient"
    private const val OCM_ENDPOINT = "https://api.openchargemap.io/v3/poi/"
    private const val USER_AGENT = "OverDrive/1.0 (RoadSense navigation)"

    // Optional default or user-provided key. If blank, OCM may return 403.
    var apiKey: String = ""

    private val http: OkHttpClient by lazy {
        MapNetworking.builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(4, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
    }

    /**
     * Query Open Charge Map for EV stations inside the given bounding box.
     *
     * @param minLat south edge (decimal degrees)
     * @param minLng west edge (decimal degrees)
     * @param maxLat north edge (decimal degrees)
     * @param maxLng east edge (decimal degrees)
     * @param limit max results
     * @return list of [RoutePoi]s with charging station details
     */
    fun poisNearBbox(
        minLat: Double,
        minLng: Double,
        maxLat: Double,
        maxLng: Double,
        limit: Int = 50
    ): List<RoutePoi> {
        val south = minOf(minLat, maxLat)
        val north = maxOf(minLat, maxLat)
        val west = minOf(minLng, maxLng)
        val east = maxOf(minLng, maxLng)
        if (south == north || west == east) return emptyList()

        return try {
            val urlBuilder = StringBuilder(OCM_ENDPOINT)
                .append("?output=json")
                .append("&boundingbox=(").append(south).append(',').append(west).append("),(")
                .append(north).append(',').append(east).append(')')
                .append("&maxresults=").append(limit)
                .append("&compact=true&verbose=false")

            val reqBuilder = Request.Builder()
                .url(urlBuilder.toString())
                .header("User-Agent", USER_AGENT)

            if (apiKey.isNotBlank()) {
                reqBuilder.header("X-API-Key", apiKey)
            }

            http.newCall(reqBuilder.build()).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.d(TAG, "OCM query returned HTTP ${resp.code}")
                    return emptyList()
                }
                val bodyStr = resp.body?.string() ?: return emptyList()
                parseOcm(bodyStr)
            }
        } catch (t: Throwable) {
            Log.d(TAG, "OCM request failed: ${t.message}")
            emptyList()
        }
    }

    /**
     * Parse Open Charge Map JSON response into [RoutePoi]s.
     */
    internal fun parseOcm(json: String): List<RoutePoi> {
        val out = ArrayList<RoutePoi>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val addr = item.optJSONObject("AddressInfo") ?: continue

                val lat = addr.optDouble("Latitude", Double.NaN)
                val lng = addr.optDouble("Longitude", Double.NaN)
                if (lat.isNaN() || lng.isNaN()) continue

                val title = addr.optString("Title", "").trim()
                val line1 = addr.optString("AddressLine1", "").trim()
                val town = addr.optString("Town", "").trim()
                val fullAddress = if (line1.isNotBlank() && town.isNotBlank()) "$line1, $town" else line1.ifBlank { town }

                val opObj = item.optJSONObject("OperatorInfo")
                val operator = opObj?.optString("Title", "")?.trim() ?: ""

                var maxPower = 0.0
                var socketCount = 0
                var hasDc = false
                var hasAc = false

                val connections = item.optJSONArray("Connections")
                if (connections != null) {
                    for (c in 0 until connections.length()) {
                        val conn = connections.optJSONObject(c) ?: continue
                        val power = conn.optDouble("PowerKW", 0.0)
                        val qty = conn.optInt("Quantity", 1).coerceAtLeast(1)
                        socketCount += qty
                        if (power > maxPower) maxPower = power

                        val connType = conn.optJSONObject("ConnectionType")?.optString("Title", "") ?: ""
                        val level = conn.optJSONObject("Level")
                        val isFast = level?.optBoolean("IsFastCharge", false) ?: false

                        val isType2Only = connType.contains("Type 2", ignoreCase = true) && !connType.contains("CCS", ignoreCase = true)
                        if (isType2Only || connType.contains("Type 1", ignoreCase = true) || connType.contains("Schuko", ignoreCase = true)) {
                            hasAc = true
                        } else if (isFast || power >= 40.0 || connType.contains("CCS", ignoreCase = true) || connType.contains("CHAdeMO", ignoreCase = true)) {
                            hasDc = true
                        } else if (power > 0.0) {
                            hasAc = true
                        }
                    }
                }

                val chargingType = when {
                    hasDc && hasAc -> "DC/AC"
                    hasDc -> "DC"
                    hasAc -> "AC"
                    else -> ""
                }

                out.add(
                    RoutePoi(
                        kind = PoiKind.CHARGING,
                        name = title.ifBlank { operator.ifBlank { "EV Charging" } },
                        lat = lat,
                        lng = lng,
                        operator = operator,
                        powerKw = maxPower,
                        socketCount = socketCount,
                        acPrice = 0.0,
                        dcPrice = 0.0,
                        chargingType = chargingType,
                        address = fullAddress
                    )
                )
            }
        } catch (t: Throwable) {
            try {
                Log.d(TAG, "parseOcm failed: ${t.message}")
            } catch (_: Throwable) {}
        }
        return out
    }
}
