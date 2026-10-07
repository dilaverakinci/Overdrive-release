package com.overdrive.app.ui.cockpit

import android.content.Context
import com.overdrive.app.ui.dashboard.DashboardStatusParser
import com.overdrive.app.ui.dashboard.DashboardStatusResult
import com.overdrive.app.ui.util.RecordingScanner
import com.overdrive.app.ui.util.RecordingsApiClient
import com.overdrive.app.util.DaemonHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.util.Calendar

/**
 * Data repository for in-cockpit vehicle metrics, trip estimation, and storage probes.
 * Executes strictly on [Dispatchers.IO] to eliminate main-thread I/O stalls on
 * constrained in-vehicle hardware (Snapdragon 625/665, 3-4 GB RAM).
 */
open class CockpitVehicleRepository(
    private val appContext: Context,
) {
    companion object {
        private const val STATUS_CONNECT_TIMEOUT_MS = 2_000
        private const val STATUS_READ_TIMEOUT_MS = 3_000
        private const val RANGE_CONNECT_TIMEOUT_MS = 1_500
        private const val RANGE_READ_TIMEOUT_MS = 2_000
    }

    open suspend fun fetchVehicleStatus(): DashboardStatusResult = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = DaemonHttpClient.open(
                "/status",
                "GET",
                STATUS_CONNECT_TIMEOUT_MS,
                STATUS_READ_TIMEOUT_MS,
            )
            if (conn.responseCode != 200) {
                DashboardStatusResult.Unavailable(
                    DashboardStatusResult.Reason.SERVICE_UNAVAILABLE
                )
            } else {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val personalizedRangeBody = fetchPersonalizedRangeInternal()
                DashboardStatusParser.parse(body, personalizedRangeBody)
            }
        } catch (_: Throwable) {
            DashboardStatusResult.Unavailable(
                DashboardStatusResult.Reason.SERVICE_UNAVAILABLE
            )
        } finally {
            try {
                conn?.disconnect()
            } catch (_: Throwable) {
                // Connection already dropped or closed
            }
        }
    }

    private fun fetchPersonalizedRangeInternal(): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = DaemonHttpClient.open(
                "/api/trips/range",
                "GET",
                RANGE_CONNECT_TIMEOUT_MS,
                RANGE_READ_TIMEOUT_MS,
            )
            if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        } finally {
            try {
                conn?.disconnect()
            } catch (_: Throwable) {
                // Connection already dropped or closed
            }
        }
    }

    open suspend fun fetchTodayClipCount(): Int? = withContext(Dispatchers.IO) {
        val stats = try {
            RecordingsApiClient.fetchStats()
        } catch (_: Throwable) {
            null
        }

        when {
            stats?.warming == true || stats?.indexUnavailable == true -> null
            stats != null -> stats.totalToday
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
            else -> try {
                val startOfDayMs = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                RecordingScanner.scanRecordings(appContext)
                    .count { it.timestamp >= startOfDayMs }
            } catch (_: Throwable) {
                null
            }
        }
    }
}
