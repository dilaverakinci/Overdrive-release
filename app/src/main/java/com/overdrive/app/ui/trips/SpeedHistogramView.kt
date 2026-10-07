package com.overdrive.app.ui.trips

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class SpeedHistogramView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var lowPct: Float = 0f
    private var normalPct: Float = 0f
    private var highPct: Float = 0f

    private val lowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFB020") // Amber for low speed
        style = Paint.Style.FILL
    }

    private val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00D4AA") // Teal for normal speed
        style = Paint.Style.FILL
    }

    private val highPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EF4444") // Red for high speed
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF")
        textSize = 26f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9E9E9E")
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }

    fun setDistribution(low: Float, normal: Float, high: Float) {
        val total = low + normal + high
        if (total > 0f) {
            lowPct = (low / total) * 100f
            normalPct = (normal / total) * 100f
            highPct = (high / total) * 100f
        } else {
            lowPct = 0f
            normalPct = 0f
            highPct = 0f
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val barWidth = 80f
        val maxBarH = h - 60f
        val gap = (w - (barWidth * 3)) / 4f

        val categories = listOf(
            Triple("Low (<40)", lowPct, lowPaint),
            Triple("Normal (40-80)", normalPct, normalPaint),
            Triple("High (>80)", highPct, highPaint)
        )

        for (i in categories.indices) {
            val (label, pct, paint) = categories[i]
            val x = gap + i * (barWidth + gap)
            val barH = (pct / 100f) * maxBarH
            val y = (h - 40f) - barH

            val rect = RectF(x, y, x + barWidth, h - 40f)
            canvas.drawRoundRect(rect, 8f, 8f, paint)

            if (pct > 0f) {
                canvas.drawText("${pct.toInt()}%", x + barWidth / 2f, (y - 8f).coerceAtLeast(24f), textPaint)
            }
            canvas.drawText(label, x + barWidth / 2f, h - 10f, labelPaint)
        }
    }
}
