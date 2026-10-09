package com.overdrive.app.ui.trips

import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class ElevationProfileMetrics(
    val minAltitudeM: Double,
    val maxAltitudeM: Double,
    val totalGainM: Double,
    val totalLossM: Double,
    val uphillDistanceKm: Double,
    val downhillDistanceKm: Double,
    val flatDistanceKm: Double,
    val totalDistanceKm: Double
) {
    val uphillPercent: Int
        get() = if (totalDistanceKm > 0.05) Math.round((uphillDistanceKm / totalDistanceKm) * 100).toInt() else 0
    val downhillPercent: Int
        get() = if (totalDistanceKm > 0.05) Math.round((downhillDistanceKm / totalDistanceKm) * 100).toInt() else 0
    val flatPercent: Int
        get() = if (totalDistanceKm > 0.05) Math.round((flatDistanceKm / totalDistanceKm) * 100).toInt() else 0
}

class TripElevationChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var rawSamples: List<TelemetrySampleItem> = emptyList()
    private var smoothedAltitudes: FloatArray = FloatArray(0)
    private var cumulativeDistancesKm: FloatArray = FloatArray(0)
    private var minAltDisplay: Float = 0f
    private var maxAltDisplay: Float = 100f
    private var scrubberIndex: Int? = null

    var onScrubListener: ((Int) -> Unit)? = null
    var onMetricsCalculated: ((ElevationProfileMetrics) -> Unit)? = null

    private val elevLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#00D4AA")
        strokeWidth = 3.5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val elevFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
        pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 20f
        textAlign = Paint.Align.LEFT
    }

    private val distanceLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 20f
        textAlign = Paint.Align.CENTER
    }

    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 26f
        textAlign = Paint.Align.CENTER
    }

    // Scrubber elements
    private val scrubberLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#B300D4AA")
        strokeWidth = 2f
        pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
    }

    private val scrubberGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#3300D4AA")
    }

    private val scrubberDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#00D4AA")
    }

    private val scrubberDotStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 3f
    }

    private val tooltipBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#CC111827")
    }

    private val tooltipTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 20f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    fun setSamples(samples: List<TelemetrySampleItem>) {
        this.rawSamples = samples
        this.scrubberIndex = null

        if (samples.size < 2) {
            smoothedAltitudes = FloatArray(0)
            cumulativeDistancesKm = FloatArray(0)
            invalidate()
            return
        }

        // 1. Extract raw altitudes and handle missing points
        val alts = FloatArray(samples.size)
        var lastValidAlt = 0f
        for (i in samples.indices) {
            val a = samples[i].altitude.toFloat()
            if (a > 0f && a.isFinite()) {
                lastValidAlt = a
                alts[i] = a
            } else if (lastValidAlt > 0f) {
                alts[i] = lastValidAlt
            } else {
                // Look ahead for first valid altitude
                val firstValid = samples.drop(i).firstOrNull { it.altitude > 0 && it.altitude.isFinite() }?.altitude?.toFloat() ?: 0f
                lastValidAlt = firstValid
                alts[i] = firstValid
            }
        }

        val hasAnyAltitude = alts.any { it > 0f }
        if (!hasAnyAltitude) {
            smoothedAltitudes = FloatArray(0)
            cumulativeDistancesKm = FloatArray(0)
            invalidate()
            return
        }

        // 2. Smooth altitudes with 5-point moving median/average
        smoothedAltitudes = FloatArray(samples.size)
        for (i in samples.indices) {
            val start = (i - 2).coerceAtLeast(0)
            val end = (i + 2).coerceAtMost(samples.size - 1)
            var sum = 0f
            var count = 0
            for (j in start..end) {
                sum += alts[j]
                count++
            }
            smoothedAltitudes[i] = if (count > 0) sum / count else alts[i]
        }

        // 3. Compute cumulative distances along the route
        cumulativeDistancesKm = FloatArray(samples.size)
        cumulativeDistancesKm[0] = 0f
        var runningDistKm = 0.0

        var uphillKm = 0.0
        var downhillKm = 0.0
        var flatKm = 0.0
        var totalGainM = 0.0
        var totalLossM = 0.0

        for (i in 1 until samples.size) {
            val s0 = samples[i - 1]
            val s1 = samples[i]
            val segDistKm = if (s0.lat != 0.0 && s0.lon != 0.0 && s1.lat != 0.0 && s1.lon != 0.0) {
                haversineKm(s0.lat, s0.lon, s1.lat, s1.lon)
            } else {
                val dtHours = (s1.timestampMs - s0.timestampMs).coerceAtLeast(0L) / 3_600_000.0
                (s1.speedKmh * dtHours)
            }

            runningDistKm += segDistKm
            cumulativeDistancesKm[i] = runningDistKm.toFloat()

            val dh = (smoothedAltitudes[i] - smoothedAltitudes[i - 1]).toDouble()
            if (dh > 0) totalGainM += dh
            else if (dh < 0) totalLossM += abs(dh)

            val gradientPct = if (segDistKm > 0.001) (dh / (segDistKm * 1000.0)) * 100.0 else 0.0
            when {
                gradientPct > 0.75 -> uphillKm += segDistKm
                gradientPct < -0.75 -> downhillKm += segDistKm
                else -> flatKm += segDistKm
            }
        }

        // 4. Calculate display bounds
        val minA = smoothedAltitudes.minOrNull() ?: 0f
        val maxA = smoothedAltitudes.maxOrNull() ?: 100f
        val paddingM = ((maxA - minA) * 0.15f).coerceAtLeast(4f)
        minAltDisplay = (minA - paddingM).coerceAtLeast(0f)
        maxAltDisplay = maxA + paddingM
        if (maxAltDisplay - minAltDisplay < 10f) {
            maxAltDisplay = minAltDisplay + 10f
        }

        val totalDist = runningDistKm
        val metrics = ElevationProfileMetrics(
            minAltitudeM = minA.toDouble(),
            maxAltitudeM = maxA.toDouble(),
            totalGainM = totalGainM,
            totalLossM = totalLossM,
            uphillDistanceKm = uphillKm,
            downhillDistanceKm = downhillKm,
            flatDistanceKm = flatKm,
            totalDistanceKm = totalDist
        )
        onMetricsCalculated?.invoke(metrics)

        invalidate()
    }

    fun setScrubberIndex(index: Int?) {
        if (this.scrubberIndex != index) {
            this.scrubberIndex = index
            invalidate()
        }
    }

    private fun isNightMode(): Boolean {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (rawSamples.size < 2 || smoothedAltitudes.isEmpty()) return super.onTouchEvent(event)

        val paddingLeft = 55f
        val paddingRight = 35f
        val plotW = width.toFloat() - paddingLeft - paddingRight
        if (plotW <= 0f) return super.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val clampedX = (event.x - paddingLeft).coerceIn(0f, plotW)
                val pct = clampedX / plotW
                val idx = (pct * (rawSamples.size - 1)).toInt().coerceIn(0, rawSamples.size - 1)
                scrubberIndex = idx
                invalidate()
                onScrubListener?.invoke(idx)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val isNight = isNightMode()
        gridPaint.color = if (isNight) Color.parseColor("#26FFFFFF") else Color.parseColor("#18000000")
        labelPaint.color = if (isNight) Color.parseColor("#99FFFFFF") else Color.parseColor("#757575")
        distanceLabelPaint.color = if (isNight) Color.parseColor("#99FFFFFF") else Color.parseColor("#757575")
        emptyPaint.color = if (isNight) Color.parseColor("#80FFFFFF") else Color.parseColor("#9E9E9E")

        if (smoothedAltitudes.size < 2) {
            canvas.drawText("Elevation profile not available for this trip", w / 2f, h / 2f, emptyPaint)
            return
        }

        val paddingLeft = 55f
        val paddingRight = 35f
        val paddingTop = 26f
        val paddingBottom = 34f

        val plotW = w - paddingLeft - paddingRight
        val plotH = h - paddingTop - paddingBottom
        if (plotW <= 0f || plotH <= 0f) return

        val altSpan = (maxAltDisplay - minAltDisplay).coerceAtLeast(1f)

        // Draw horizontal altitude grid lines (3 lines: min, mid, max)
        val gridLines = 3
        for (i in 0..gridLines) {
            val y = paddingTop + plotH - (i.toFloat() / gridLines) * plotH
            canvas.drawLine(paddingLeft, y, paddingLeft + plotW, y, gridPaint)
            val altVal = (minAltDisplay + (i.toFloat() / gridLines) * altSpan).toInt()
            canvas.drawText("${altVal}m", 6f, y + 6f, labelPaint)
        }

        // Draw vertical distance grid lines (3 intervals)
        val totalDistKm = cumulativeDistancesKm.lastOrNull() ?: 0f
        val distSteps = 3
        for (i in 0..distSteps) {
            val x = paddingLeft + (i.toFloat() / distSteps) * plotW
            canvas.drawLine(x, paddingTop, x, paddingTop + plotH, gridPaint)
            val dVal = (i.toFloat() / distSteps) * totalDistKm
            val label = String.format(Locale.US, "%.1f km", dVal)
            canvas.drawText(label, x, h - 8f, distanceLabelPaint)
        }

        val n = smoothedAltitudes.size
        val dx = plotW / (n - 1).coerceAtLeast(1)

        val elevPath = Path()
        val elevFillPath = Path()

        elevFillPaint.shader = LinearGradient(
            0f, paddingTop, 0f, paddingTop + plotH,
            Color.parseColor("#3800D4AA"),
            Color.parseColor("#0500D4AA"),
            Shader.TileMode.CLAMP
        )

        for (i in 0 until n) {
            val x = paddingLeft + i * dx
            val altNorm = ((smoothedAltitudes[i] - minAltDisplay) / altSpan).coerceIn(0f, 1f)
            val y = paddingTop + plotH - altNorm * plotH

            if (i == 0) {
                elevPath.moveTo(x, y)
                elevFillPath.moveTo(x, paddingTop + plotH)
                elevFillPath.lineTo(x, y)
            } else {
                elevPath.lineTo(x, y)
                elevFillPath.lineTo(x, y)
            }
        }

        elevFillPath.lineTo(paddingLeft + (n - 1) * dx, paddingTop + plotH)
        elevFillPath.close()

        // Draw gradient area and line
        canvas.drawPath(elevFillPath, elevFillPaint)
        canvas.drawPath(elevPath, elevLinePaint)

        // Draw Scrubber cursor if active
        val scrubIdx = scrubberIndex
        if (scrubIdx != null && scrubIdx in smoothedAltitudes.indices) {
            val scrubX = paddingLeft + scrubIdx * dx
            val altNorm = ((smoothedAltitudes[scrubIdx] - minAltDisplay) / altSpan).coerceIn(0f, 1f)
            val scrubY = paddingTop + plotH - altNorm * plotH

            // Vertical dashed line
            canvas.drawLine(scrubX, paddingTop, scrubX, paddingTop + plotH, scrubberLinePaint)

            // Outer glow and inner dot at altitude position
            canvas.drawCircle(scrubX, scrubY, 13f, scrubberGlowPaint)
            canvas.drawCircle(scrubX, scrubY, 5.5f, scrubberDotPaint)
            canvas.drawCircle(scrubX, scrubY, 5.5f, scrubberDotStrokePaint)

            // Altitude tooltip badge above point
            val distAtScrub = cumulativeDistancesKm.getOrNull(scrubIdx) ?: 0f
            val tooltipText = String.format(Locale.US, "%.0fm (%.1f km)", smoothedAltitudes[scrubIdx], distAtScrub)
            val textWidth = tooltipTextPaint.measureText(tooltipText)
            val badgePadH = 10f
            val badgeH = 26f
            val badgeW = textWidth + badgePadH * 2
            val badgeX = (scrubX - badgeW / 2f).coerceIn(paddingLeft, paddingLeft + plotW - badgeW)
            val badgeY = (scrubY - badgeH - 10f).coerceAtLeast(paddingTop)

            val badgeRect = RectF(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH)
            canvas.drawRoundRect(badgeRect, 8f, 8f, tooltipBgPaint)
            canvas.drawText(tooltipText, badgeRect.centerX(), badgeRect.centerY() + 6f, tooltipTextPaint)
        }
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
}
