package com.overdrive.app.ui.charging

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/**
 * Custom chart view rendering cell temperature curves (High, Avg, Low) in °C.
 */
class TemperatureCurveView @JvmOverloads constructor(
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

    private val highPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF5252") // Red
        strokeWidth = 3.5f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val avgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFB74D") // Orange
        strokeWidth = 3f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val lowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#40C4FF") // Blue
        strokeWidth = 3.5f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val highPath = Path()
    private val avgPath = Path()
    private val lowPath = Path()

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
        val paddingRight = 40f
        val paddingTop = 40f
        val paddingBottom = 40f

        val plotW = w - paddingLeft - paddingRight
        val plotH = h - paddingTop - paddingBottom
        if (plotW <= 0 || plotH <= 0) return

        // 4 grid lines
        for (i in 0..3) {
            val y = paddingTop + plotH * (i / 3f)
            canvas.drawLine(paddingLeft, y, paddingLeft + plotW, y, gridPaint)
        }

        if (samples.isEmpty()) {
            val emptyText = "No temperature samples recorded"
            val textW = textPaint.measureText(emptyText)
            canvas.drawText(emptyText, (w - textW) / 2f, h / 2f, textPaint)
            return
        }

        var minTemp = 15.0
        var maxTemp = 45.0
        for (s in samples) {
            s.tempHigh?.let { if (it > maxTemp) maxTemp = it }
            s.tempLow?.let { if (it < minTemp) minTemp = it }
            s.temp?.let {
                if (it > maxTemp) maxTemp = it
                if (it < minTemp) minTemp = it
            }
        }
        maxTemp = (Math.ceil(maxTemp / 5.0) * 5.0).coerceAtLeast(40.0)
        minTemp = (Math.floor(minTemp / 5.0) * 5.0).coerceAtMost(20.0)
        val tempSpan = (maxTemp - minTemp).coerceAtLeast(1.0).toFloat()

        // Labels
        canvas.drawText("${maxTemp.toInt()}°C", 10f, paddingTop + 20f, textPaint)
        canvas.drawText("${minTemp.toInt()}°C", 10f, paddingTop + plotH, textPaint)

        val minTime = samples.firstOrNull()?.t ?: 0L
        val maxTime = samples.lastOrNull()?.t ?: minTime
        val timeSpan = (maxTime - minTime).coerceAtLeast(1L).toFloat()

        highPath.reset()
        avgPath.reset()
        lowPath.reset()

        for (s in samples) {
            val fractionX = ((s.t - minTime).toFloat() / timeSpan).coerceIn(0f, 1f)
            val px = paddingLeft + fractionX * plotW

            s.tempHigh?.let {
                val fy = ((it - minTemp).toFloat() / tempSpan).coerceIn(0f, 1f)
                val py = paddingTop + plotH * (1f - fy)
                if (highPath.isEmpty) highPath.moveTo(px, py) else highPath.lineTo(px, py)
            }

            s.temp?.let {
                val fy = ((it - minTemp).toFloat() / tempSpan).coerceIn(0f, 1f)
                val py = paddingTop + plotH * (1f - fy)
                if (avgPath.isEmpty) avgPath.moveTo(px, py) else avgPath.lineTo(px, py)
            }

            s.tempLow?.let {
                val fy = ((it - minTemp).toFloat() / tempSpan).coerceIn(0f, 1f)
                val py = paddingTop + plotH * (1f - fy)
                if (lowPath.isEmpty) lowPath.moveTo(px, py) else lowPath.lineTo(px, py)
            }
        }

        if (!highPath.isEmpty) canvas.drawPath(highPath, highPaint)
        if (!avgPath.isEmpty) canvas.drawPath(avgPath, avgPaint)
        if (!lowPath.isEmpty) canvas.drawPath(lowPath, lowPaint)
    }
}
