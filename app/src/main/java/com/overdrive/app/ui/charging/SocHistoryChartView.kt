package com.overdrive.app.ui.charging

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * State of Charge (SoC) Over Time interactive chart matching Overdrive Web charging.js parity:
 * - 5 horizontal grid lines (0%, 25%, 50%, 75%, 100%) with labels
 * - Shaded mint charging bands indicating active charging periods
 * - Mint SoC curve with vertical linear gradient fill underneath
 * - Formatted X-axis time / date marks
 * - Interactive touch scrubbing with crosshair line, marker dot, and floating tooltip pill
 */
class SocHistoryChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var points: List<SocHistoryPoint> = emptyList()
    private var socHours: Int = 168
    private var scrubIndex: Int = -1

    private val density = context.resources.displayMetrics.density
    private val scaledDensity = context.resources.displayMetrics.scaledDensity

    private val timeFormatClock = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val timeFormatDate = SimpleDateFormat("d MMM", Locale.getDefault())
    private val tooltipDateFormat = SimpleDateFormat("d MMM HH:mm", Locale.getDefault())

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1f * density
        style = Paint.Style.STROKE
    }

    private val axisTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10f * scaledDensity
    }

    private val socLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00D4AA")
        strokeWidth = 2.5f * density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val socFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2200D4AA") // ~13% mint shading
        style = Paint.Style.FILL
    }

    private val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1.2f * density
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(4f * density, 4f * density), 0f)
    }

    private val dotOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00D4AA")
        style = Paint.Style.FILL
    }

    private val dotStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2f * density
        style = Paint.Style.STROKE
    }

    private val tooltipBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val tooltipStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1f * density
        style = Paint.Style.STROKE
    }

    private val tooltipPrimaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 12f * scaledDensity
        color = Color.parseColor("#00D4AA")
        isFakeBoldText = true
    }

    private val tooltipSecondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10f * scaledDensity
    }

    private val socPath = Path()
    private val socFillPath = Path()
    private val crosshairPath = Path()
    private val tooltipRect = RectF()

    private val hideScrubberRunnable = Runnable {
        scrubIndex = -1
        invalidate()
    }

    fun setPoints(newPoints: List<SocHistoryPoint>, hours: Int = 168) {
        points = newPoints.sortedBy { it.timestamp }
        socHours = hours
        scrubIndex = -1
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (points.size < 2) return super.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                removeCallbacks(hideScrubberRunnable)

                val paddingLeft = 38f * density
                val paddingRight = 12f * density
                val plotW = width.toFloat() - paddingLeft - paddingRight
                if (plotW <= 0) return true

                val tMin = points.first().timestamp
                val tMax = points.last().timestamp
                val tSpan = (tMax - tMin).coerceAtLeast(1L).toFloat()

                val touchX = event.x.coerceIn(paddingLeft, paddingLeft + plotW)

                // Find closest point by projected x
                var bestIdx = 0
                var bestDist = Float.MAX_VALUE
                for (i in points.indices) {
                    val ptX = paddingLeft + ((points[i].timestamp - tMin).toFloat() / tSpan) * plotW
                    val dist = Math.abs(ptX - touchX)
                    if (dist < bestDist) {
                        bestDist = dist
                        bestIdx = i
                    }
                }

                if (scrubIndex != bestIdx) {
                    scrubIndex = bestIdx
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                postDelayed(hideScrubberRunnable, 2500L)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val paddingLeft = 38f * density
        val paddingRight = 14f * density
        val paddingTop = 14f * density
        val paddingBottom = 26f * density

        val plotW = w - paddingLeft - paddingRight
        val plotH = h - paddingTop - paddingBottom
        if (plotW <= 0 || plotH <= 0) return

        val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // Setup theme-aware colors
        gridPaint.color = if (night) Color.parseColor("#1FFFFFFF") else Color.parseColor("#14000000")
        axisTextPaint.color = if (night) Color.parseColor("#80FFFFFF") else Color.parseColor("#757575")
        crosshairPaint.color = if (night) Color.parseColor("#66FFFFFF") else Color.parseColor("#66000000")
        dotStrokePaint.color = if (night) Color.parseColor("#1E1E1E") else Color.WHITE
        tooltipBgPaint.color = if (night) Color.parseColor("#E6202024") else Color.parseColor("#F5FFFFFF")
        tooltipStrokePaint.color = if (night) Color.parseColor("#33FFFFFF") else Color.parseColor("#20000000")
        tooltipSecondaryPaint.color = if (night) Color.parseColor("#A0FFFFFF") else Color.parseColor("#616161")

        // 1. Draw 5 horizontal grid lines & Y labels: 0%, 25%, 50%, 75%, 100%
        val yGridValues = intArrayOf(100, 75, 50, 25, 0)
        axisTextPaint.textAlign = Paint.Align.RIGHT
        for (v in yGridValues) {
            val y = paddingTop + (1f - (v / 100f)) * plotH
            canvas.drawLine(paddingLeft, y, paddingLeft + plotW, y, gridPaint)
            val labelY = y + (axisTextPaint.textSize / 3f)
            canvas.drawText("$v%", paddingLeft - (6f * density), labelY, axisTextPaint)
        }

        // Empty state check
        if (points.size < 2) {
            axisTextPaint.textAlign = Paint.Align.CENTER
            val emptyMsg = "No SoC history recorded"
            canvas.drawText(emptyMsg, paddingLeft + plotW / 2f, paddingTop + plotH / 2f, axisTextPaint)
            return
        }

        val tMin = points.first().timestamp
        val tMax = points.last().timestamp
        val tSpan = (tMax - tMin).coerceAtLeast(1L).toFloat()

        fun getX(t: Long): Float {
            return paddingLeft + ((t - tMin).toFloat() / tSpan) * plotW
        }

        fun getY(soc: Double): Float {
            val clamped = soc.coerceIn(0.0, 100.0).toFloat()
            return paddingTop + (1f - (clamped / 100f)) * plotH
        }

        // 2. Draw charging bands (shaded regions where vehicle was charging)
        var bandStartTimestamp: Long? = null
        for (i in points.indices) {
            val pt = points[i]
            if (pt.charging && bandStartTimestamp == null) {
                bandStartTimestamp = pt.timestamp
            }
            if ((!pt.charging || i == points.size - 1) && bandStartTimestamp != null) {
                val bx0 = getX(bandStartTimestamp)
                val bx1 = getX(pt.timestamp)
                val bandWidth = Math.max(bx1 - bx0, 3f * density)
                canvas.drawRect(bx0, paddingTop, bx0 + bandWidth, paddingTop + plotH, bandPaint)
                bandStartTimestamp = null
            }
        }

        // 3. Build & Draw SoC Area Fill and Line
        socPath.reset()
        socFillPath.reset()

        val firstPt = points.first()
        val firstX = getX(firstPt.timestamp)
        val firstY = getY(firstPt.soc)

        socPath.moveTo(firstX, firstY)
        socFillPath.moveTo(firstX, paddingTop + plotH)
        socFillPath.lineTo(firstX, firstY)

        for (i in 1 until points.size) {
            val pt = points[i]
            val px = getX(pt.timestamp)
            val py = getY(pt.soc)
            socPath.lineTo(px, py)
            socFillPath.lineTo(px, py)
        }

        val lastPt = points.last()
        val lastX = getX(lastPt.timestamp)
        socFillPath.lineTo(lastX, paddingTop + plotH)
        socFillPath.close()

        socFillPaint.shader = LinearGradient(
            0f, paddingTop,
            0f, paddingTop + plotH,
            Color.parseColor("#4800D4AA"),
            Color.parseColor("#0400D4AA"),
            Shader.TileMode.CLAMP
        )

        canvas.drawPath(socFillPath, socFillPaint)
        canvas.drawPath(socPath, socLinePaint)

        // 4. Draw X-axis time / date labels at bottom
        axisTextPaint.textAlign = Paint.Align.LEFT
        val tMinText = formatTimeLabel(tMin, socHours)
        canvas.drawText(tMinText, paddingLeft, paddingTop + plotH + (16f * density), axisTextPaint)

        axisTextPaint.textAlign = Paint.Align.RIGHT
        val tMaxText = formatTimeLabel(tMax, socHours)
        canvas.drawText(tMaxText, paddingLeft + plotW, paddingTop + plotH + (16f * density), axisTextPaint)

        if (plotW > 180f * density) {
            axisTextPaint.textAlign = Paint.Align.CENTER
            val tMid = tMin + (tMax - tMin) / 2
            val tMidText = formatTimeLabel(tMid, socHours)
            canvas.drawText(tMidText, paddingLeft + plotW / 2f, paddingTop + plotH + (16f * density), axisTextPaint)
        }

        // 5. Draw interactive scrubber when active
        if (scrubIndex in points.indices) {
            val scrubPt = points[scrubIndex]
            val sx = getX(scrubPt.timestamp)
            val sy = getY(scrubPt.soc)

            // Crosshair vertical line
            crosshairPath.reset()
            crosshairPath.moveTo(sx, paddingTop)
            crosshairPath.lineTo(sx, paddingTop + plotH)
            canvas.drawPath(crosshairPath, crosshairPaint)

            // Scrubber dot with border
            canvas.drawCircle(sx, sy, 5.5f * density, dotOuterPaint)
            canvas.drawCircle(sx, sy, 5.5f * density, dotStrokePaint)

            // Floating Tooltip Pill
            val primaryText = buildString {
                append("${Math.round(scrubPt.soc)}% SoC")
                if (scrubPt.charging) append(" ⚡")
                if (scrubPt.powerKw != null && scrubPt.powerKw > 0) {
                    append(String.format(Locale.US, " • %.1f kW", scrubPt.powerKw))
                }
            }

            val secondaryText = buildString {
                append(tooltipDateFormat.format(Date(scrubPt.timestamp)))
                if (scrubPt.range != null && scrubPt.range > 0) {
                    append(String.format(Locale.US, " • %d km", Math.round(scrubPt.range)))
                }
            }

            val pTextW = tooltipPrimaryPaint.measureText(primaryText)
            val sTextW = tooltipSecondaryPaint.measureText(secondaryText)
            val tipWidth = Math.max(pTextW, sTextW) + (20f * density)
            val tipHeight = 44f * density

            // Position tooltip smartly: offset to right unless too close to right edge
            var tipLeft = sx + (12f * density)
            if (tipLeft + tipWidth > w - (8f * density)) {
                tipLeft = sx - tipWidth - (12f * density)
            }
            if (tipLeft < 8f * density) {
                tipLeft = 8f * density
            }

            val tipTop = (paddingTop + 6f * density).coerceAtMost(h - tipHeight - 6f * density)
            tooltipRect.set(tipLeft, tipTop, tipLeft + tipWidth, tipTop + tipHeight)

            val cornerR = 8f * density
            canvas.drawRoundRect(tooltipRect, cornerR, cornerR, tooltipBgPaint)
            canvas.drawRoundRect(tooltipRect, cornerR, cornerR, tooltipStrokePaint)

            canvas.drawText(
                primaryText,
                tipLeft + (10f * density),
                tipTop + (18f * density),
                tooltipPrimaryPaint
            )

            canvas.drawText(
                secondaryText,
                tipLeft + (10f * density),
                tipTop + (34f * density),
                tooltipSecondaryPaint
            )
        }
    }

    private fun formatTimeLabel(timestamp: Long, hours: Int): String {
        return try {
            val date = Date(timestamp)
            if (hours <= 24) {
                timeFormatClock.format(date)
            } else {
                timeFormatDate.format(date)
            }
        } catch (_: Exception) {
            ""
        }
    }
}
