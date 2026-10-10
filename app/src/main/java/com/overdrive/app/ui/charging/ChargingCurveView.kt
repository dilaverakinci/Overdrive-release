package com.overdrive.app.ui.charging

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

/**
 * Custom chart view rendering Power (kW) and SoC (%) curves for a charging session.
 */
class ChargingCurveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var samples: List<ChargingSample> = emptyList()

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1FFFFFFF")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80FFFFFF")
        textSize = 24f
    }

    private val powerLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#A46BFF") // Purple
        strokeWidth = 4.5f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val powerFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val socLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00D4AA") // Mint
        strokeWidth = 3.5f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val powerPath = Path()
    private val powerFillPath = Path()
    private val socPath = Path()

    fun setSamples(newSamples: List<ChargingSample>) {
        samples = newSamples
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val paddingLeft = 60f
        val paddingRight = 60f
        val paddingTop = 40f
        val paddingBottom = 40f

        val plotW = w - paddingLeft - paddingRight
        val plotH = h - paddingTop - paddingBottom
        if (plotW <= 0 || plotH <= 0) return

        val night = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        gridPaint.color = if (night) Color.parseColor("#1FFFFFFF") else Color.parseColor("#1F000000")
        textPaint.color = if (night) Color.parseColor("#80FFFFFF") else Color.parseColor("#757575")

        // Draw horizontal grid lines (4 lines: 0%, 33%, 66%, 100%)
        for (i in 0..3) {
            val y = paddingTop + plotH * (i / 3f)
            canvas.drawLine(paddingLeft, y, paddingLeft + plotW, y, gridPaint)
        }

        if (samples.isEmpty()) {
            val emptyText = "No curve samples recorded"
            val textW = textPaint.measureText(emptyText)
            canvas.drawText(emptyText, (w - textW) / 2f, h / 2f, textPaint)
            return
        }

        val minTime = samples.firstOrNull()?.t ?: 0L
        val maxTime = samples.lastOrNull()?.t ?: minTime
        val timeSpan = (maxTime - minTime).coerceAtLeast(1L).toFloat()

        var maxPower = 10.0
        for (s in samples) {
            val p = s.powerKw
            if (p != null && p > maxPower) maxPower = p
        }
        maxPower = Math.ceil(maxPower / 10.0) * 10.0

        // Y-axis labels: Power (left in purple), SoC (right in mint)
        textPaint.color = Color.parseColor("#A46BFF")
        canvas.drawText("${maxPower.toInt()} kW", 10f, paddingTop + 20f, textPaint)
        canvas.drawText("0 kW", 10f, paddingTop + plotH, textPaint)

        textPaint.color = Color.parseColor("#00D4AA")
        val rightLabelMax = "100%"
        canvas.drawText(rightLabelMax, w - paddingRight + 8f, paddingTop + 20f, textPaint)
        val rightLabelMin = "0%"
        canvas.drawText(rightLabelMin, w - paddingRight + 8f, paddingTop + plotH, textPaint)

        // Build Paths
        powerPath.reset()
        powerFillPath.reset()
        socPath.reset()

        var powerStarted = false
        var firstX = paddingLeft
        var lastX = paddingLeft

        for (s in samples) {
            val fractionX = ((s.t - minTime).toFloat() / timeSpan).coerceIn(0f, 1f)
            val px = paddingLeft + fractionX * plotW

            // Power
            val p = s.powerKw
            if (p != null && p >= 0) {
                val fractionY = (p.toFloat() / maxPower.toFloat()).coerceIn(0f, 1f)
                val py = paddingTop + plotH * (1f - fractionY)
                if (!powerStarted) {
                    powerPath.moveTo(px, py)
                    powerFillPath.moveTo(px, paddingTop + plotH)
                    powerFillPath.lineTo(px, py)
                    firstX = px
                    powerStarted = true
                } else {
                    powerPath.lineTo(px, py)
                    powerFillPath.lineTo(px, py)
                }
                lastX = px
            }

            // SoC
            val soc = s.soc
            if (soc != null && soc >= 0) {
                val fractionY = (soc.toFloat() / 100f).coerceIn(0f, 1f)
                val py = paddingTop + plotH * (1f - fractionY)
                if (socPath.isEmpty) {
                    socPath.moveTo(px, py)
                } else {
                    socPath.lineTo(px, py)
                }
            }
        }

        if (powerStarted) {
            powerFillPath.lineTo(lastX, paddingTop + plotH)
            powerFillPath.close()

            powerFillPaint.shader = LinearGradient(
                0f, paddingTop, 0f, paddingTop + plotH,
                Color.parseColor("#44A46BFF"),
                Color.parseColor("#05A46BFF"),
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(powerFillPath, powerFillPaint)
            canvas.drawPath(powerPath, powerLinePaint)
        }

        if (!socPath.isEmpty) {
            canvas.drawPath(socPath, socLinePaint)
        }
    }
}
