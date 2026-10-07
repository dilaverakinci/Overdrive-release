package com.overdrive.app.ui.trips

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class TripTimelineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var samples: List<TelemetrySampleItem> = emptyList()

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
        color = Color.parseColor("#330EA5E9")
        strokeWidth = 3f
    }

    private val brakePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#44EF4444")
        strokeWidth = 3f
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#1FFFFFFF")
        strokeWidth = 1.5f
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80FFFFFF")
        textSize = 24f
        textAlign = Paint.Align.LEFT
    }

    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#60FFFFFF")
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }

    fun setSamples(samples: List<TelemetrySampleItem>) {
        this.samples = samples
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        if (samples.isEmpty()) {
            canvas.drawText("No telemetry samples recorded for this trip", w / 2f, h / 2f, emptyPaint)
            return
        }

        val paddingLeft = 50f
        val paddingRight = 30f
        val paddingTop = 20f
        val paddingBottom = 40f

        val plotW = w - paddingLeft - paddingRight
        val plotH = h - paddingTop - paddingBottom
        if (plotW <= 0f || plotH <= 0f) return

        val maxSpeed = (samples.maxOfOrNull { it.speedKmh } ?: 100).coerceAtLeast(60).toFloat()

        // Draw horizontal grid lines (0, 50%, 100% of max speed)
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = paddingTop + plotH - (i.toFloat() / gridLines) * plotH
            canvas.drawLine(paddingLeft, y, paddingLeft + plotW, y, gridPaint)
            val speedLabel = "${((i.toFloat() / gridLines) * maxSpeed).toInt()} km/h"
            canvas.drawText(speedLabel, 8f, y + 8f, labelPaint)
        }

        val n = samples.size
        val dx = plotW / (n - 1).coerceAtLeast(1)

        val speedPath = Path()
        val speedFillPath = Path()
        val accelPath = Path()
        val brakePath = Path()

        speedFillPaint.shader = LinearGradient(
            0f, paddingTop, 0f, paddingTop + plotH,
            Color.parseColor("#4000D4AA"),
            Color.parseColor("#0000D4AA"),
            Shader.TileMode.CLAMP
        )

        for (i in 0 until n) {
            val sample = samples[i]
            val x = paddingLeft + i * dx
            val speedY = paddingTop + plotH - (sample.speedKmh.toFloat() / maxSpeed) * plotH
            val accelY = paddingTop + plotH - (sample.accelPedalPercent.toFloat() / 100f) * plotH
            val brakeY = paddingTop + plotH - (sample.brakePedalPercent.toFloat() / 100f) * plotH

            if (i == 0) {
                speedPath.moveTo(x, speedY)
                speedFillPath.moveTo(x, paddingTop + plotH)
                speedFillPath.lineTo(x, speedY)
                accelPath.moveTo(x, accelY)
                brakePath.moveTo(x, brakeY)
            } else {
                speedPath.lineTo(x, speedY)
                speedFillPath.lineTo(x, speedY)
                accelPath.lineTo(x, accelY)
                brakePath.lineTo(x, brakeY)
            }
        }

        speedFillPath.lineTo(paddingLeft + (n - 1) * dx, paddingTop + plotH)
        speedFillPath.close()

        canvas.drawPath(speedFillPath, speedFillPaint)
        canvas.drawPath(accelPath, accelPaint)
        canvas.drawPath(brakePath, brakePaint)
        canvas.drawPath(speedPath, speedPaint)
    }
}
