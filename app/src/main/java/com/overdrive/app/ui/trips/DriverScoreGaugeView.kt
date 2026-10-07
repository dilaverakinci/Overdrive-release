package com.overdrive.app.ui.trips

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class DriverScoreGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var score: Int = -1 // -1 = no data

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#20808080")
        strokeWidth = 14f
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 14f
        strokeCap = Paint.Cap.ROUND
    }

    private val scoreTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 72f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val labelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 24f
        textAlign = Paint.Align.CENTER
        color = Color.parseColor("#9E9E9E")
        typeface = Typeface.DEFAULT_BOLD
    }

    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 36f
        textAlign = Paint.Align.CENTER
    }

    private val arcBounds = RectF()

    fun setScore(score: Int) {
        this.score = score
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val padding = 20f
        val diameter = Math.min(w, h) - padding * 2
        val left = (w - diameter) / 2f
        val top = (h - diameter) / 2f
        arcBounds.set(left, top, left + diameter, top + diameter)

        // Arc from 135 deg to 405 deg (270 degree span)
        val startAngle = 135f
        val sweepAngle = 270f

        canvas.drawArc(arcBounds, startAngle, sweepAngle, false, trackPaint)

        val s = score
        if (s >= 0) {
            val progressFraction = (s.coerceIn(0, 100) / 100f)
            val currentSweep = sweepAngle * progressFraction

            val color = when {
                s >= 80 -> Color.parseColor("#00D4AA") // teal/green
                s >= 60 -> Color.parseColor("#FFB020") // amber
                else -> Color.parseColor("#FF5252") // red
            }
            progressPaint.color = color
            scoreTextPaint.color = color

            canvas.drawArc(arcBounds, startAngle, currentSweep, false, progressPaint)

            val centerX = arcBounds.centerX()
            val centerY = arcBounds.centerY()

            // Draw star icon
            canvas.drawText("★", centerX, centerY - 36f, starPaint.apply { this.color = color })
            // Draw score
            canvas.drawText("$s", centerX, centerY + 24f, scoreTextPaint)
            // Draw "/100"
            canvas.drawText("/ 100", centerX, centerY + 62f, labelTextPaint)
        } else {
            val centerX = arcBounds.centerX()
            val centerY = arcBounds.centerY()
            scoreTextPaint.color = Color.parseColor("#9E9E9E")
            canvas.drawText("★", centerX, centerY - 36f, starPaint.apply { this.color = Color.parseColor("#9E9E9E") })
            canvas.drawText("--", centerX, centerY + 24f, scoreTextPaint)
            canvas.drawText("/ 100", centerX, centerY + 62f, labelTextPaint)
        }
    }
}
