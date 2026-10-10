package com.overdrive.app.ui.trips

import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class TripTimelineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var samples: List<TelemetrySampleItem> = emptyList()
    private var socStart: Double = 0.0
    private var socEnd: Double = 0.0
    private var scrubberIndex: Int? = null

    var onScrubListener: ((Int) -> Unit)? = null

    private val speedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#00D4AA")
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val speedFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val accelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#0EA5E9")
        strokeWidth = 2.5f
    }

    private val accelFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1F0EA5E9")
    }

    private val brakePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#EF4444")
        strokeWidth = 2.5f
    }

    private val brakeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#26EF4444")
    }

    private val socPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#F59E0B")
        strokeWidth = 2f
        pathEffect = DashPathEffect(floatArrayOf(10f, 6f), 0f)
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 22f
        textAlign = Paint.Align.LEFT
    }

    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
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
        textSize = 22f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    fun setSamples(samples: List<TelemetrySampleItem>, socStart: Double = 0.0, socEnd: Double = 0.0) {
        this.samples = samples
        this.socStart = socStart
        this.socEnd = socEnd
        this.scrubberIndex = null
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
        if (samples.size < 2) return super.onTouchEvent(event)

        val paddingLeft = 55f
        val paddingRight = 35f
        val plotW = width.toFloat() - paddingLeft - paddingRight
        if (plotW <= 0f) return super.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val clampedX = (event.x - paddingLeft).coerceIn(0f, plotW)
                val pct = clampedX / plotW
                val idx = (pct * (samples.size - 1)).toInt().coerceIn(0, samples.size - 1)
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
        emptyPaint.color = if (isNight) Color.parseColor("#80FFFFFF") else Color.parseColor("#9E9E9E")

        if (samples.isEmpty()) {
            canvas.drawText("No telemetry samples recorded for this trip", w / 2f, h / 2f, emptyPaint)
            return
        }

        val paddingLeft = 55f
        val paddingRight = 35f
        val paddingTop = 24f
        val paddingBottom = 40f

        val plotW = w - paddingLeft - paddingRight
        val plotH = h - paddingTop - paddingBottom
        if (plotW <= 0f || plotH <= 0f) return

        val maxSpeed = (samples.maxOfOrNull { it.speedKmh } ?: 100).coerceAtLeast(60).toFloat()

        // Draw horizontal grid lines (0, 25%, 50%, 75%, 100% of max speed)
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = paddingTop + plotH - (i.toFloat() / gridLines) * plotH
            canvas.drawLine(paddingLeft, y, paddingLeft + plotW, y, gridPaint)
            val speedLabel = "${((i.toFloat() / gridLines) * maxSpeed).toInt()} km/h"
            canvas.drawText(speedLabel, 4f, y + 7f, labelPaint)
        }

        val n = samples.size
        val dx = plotW / (n - 1).coerceAtLeast(1)

        val speedPath = Path()
        val speedFillPath = Path()
        val accelPath = Path()
        val accelFillPath = Path()
        val brakePath = Path()
        val brakeFillPath = Path()
        val socPath = Path()

        speedFillPaint.shader = LinearGradient(
            0f, paddingTop, 0f, paddingTop + plotH,
            Color.parseColor("#3800D4AA"),
            Color.parseColor("#0000D4AA"),
            Shader.TileMode.CLAMP
        )

        for (i in 0 until n) {
            val sample = samples[i]
            val x = paddingLeft + i * dx
            val speedY = paddingTop + plotH - (sample.speedKmh.toFloat() / maxSpeed).coerceIn(0f, 1f) * plotH
            val accelY = paddingTop + plotH - (sample.accelPedalPercent.toFloat() / 100f).coerceIn(0f, 1f) * plotH
            val brakeY = paddingTop + plotH - (sample.brakePedalPercent.toFloat() / 100f).coerceIn(0f, 1f) * plotH

            // SoC interpolation
            val currentSoc = if (socStart > 0 && socEnd > 0) {
                socStart + (socEnd - socStart) * (i.toDouble() / (n - 1).coerceAtLeast(1))
            } else 0.0
            val socY = paddingTop + plotH - (currentSoc.toFloat() / 100f).coerceIn(0f, 1f) * plotH

            if (i == 0) {
                speedPath.moveTo(x, speedY)
                speedFillPath.moveTo(x, paddingTop + plotH)
                speedFillPath.lineTo(x, speedY)

                accelPath.moveTo(x, accelY)
                accelFillPath.moveTo(x, paddingTop + plotH)
                accelFillPath.lineTo(x, accelY)

                brakePath.moveTo(x, brakeY)
                brakeFillPath.moveTo(x, paddingTop + plotH)
                brakeFillPath.lineTo(x, brakeY)

                if (currentSoc > 0) socPath.moveTo(x, socY)
            } else {
                speedPath.lineTo(x, speedY)
                speedFillPath.lineTo(x, speedY)

                accelPath.lineTo(x, accelY)
                accelFillPath.lineTo(x, accelY)

                brakePath.lineTo(x, brakeY)
                brakeFillPath.lineTo(x, brakeY)

                if (currentSoc > 0) socPath.lineTo(x, socY)
            }
        }

        speedFillPath.lineTo(paddingLeft + (n - 1) * dx, paddingTop + plotH)
        speedFillPath.close()

        accelFillPath.lineTo(paddingLeft + (n - 1) * dx, paddingTop + plotH)
        accelFillPath.close()

        brakeFillPath.lineTo(paddingLeft + (n - 1) * dx, paddingTop + plotH)
        brakeFillPath.close()

        // Draw areas
        canvas.drawPath(speedFillPath, speedFillPaint)
        canvas.drawPath(accelFillPath, accelFillPaint)
        canvas.drawPath(brakeFillPath, brakeFillPaint)

        // Draw curves
        canvas.drawPath(accelPath, accelPaint)
        canvas.drawPath(brakePath, brakePaint)
        if (socStart > 0 && socEnd > 0) {
            canvas.drawPath(socPath, socPaint)
        }
        canvas.drawPath(speedPath, speedPaint)

        // Draw Scrubber cursor if active
        val scrubIdx = scrubberIndex
        if (scrubIdx != null && scrubIdx in samples.indices) {
            val sample = samples[scrubIdx]
            val scrubX = paddingLeft + scrubIdx * dx
            val scrubSpeedY = paddingTop + plotH - (sample.speedKmh.toFloat() / maxSpeed).coerceIn(0f, 1f) * plotH

            // Vertical dashed line
            canvas.drawLine(scrubX, paddingTop, scrubX, paddingTop + plotH, scrubberLinePaint)

            // Outer glow and inner dot at speed position
            canvas.drawCircle(scrubX, scrubSpeedY, 14f, scrubberGlowPaint)
            canvas.drawCircle(scrubX, scrubSpeedY, 6f, scrubberDotPaint)
            canvas.drawCircle(scrubX, scrubSpeedY, 6f, scrubberDotStrokePaint)

            // Speed tooltip badge above point
            val tooltipText = "${sample.speedKmh} km/h"
            val textWidth = tooltipTextPaint.measureText(tooltipText)
            val badgePadH = 12f
            val badgeH = 30f
            val badgeW = textWidth + badgePadH * 2
            val badgeX = (scrubX - badgeW / 2f).coerceIn(paddingLeft, paddingLeft + plotW - badgeW)
            val badgeY = (scrubSpeedY - badgeH - 12f).coerceAtLeast(paddingTop)

            val badgeRect = RectF(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH)
            canvas.drawRoundRect(badgeRect, 10f, 10f, tooltipBgPaint)
            canvas.drawText(tooltipText, badgeRect.centerX(), badgeRect.centerY() + 7f, tooltipTextPaint)
        }
    }
}
