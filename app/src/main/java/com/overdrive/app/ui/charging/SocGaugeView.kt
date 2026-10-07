package com.overdrive.app.ui.charging

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Circular arc gauge view for State of Charge (SoC).
 */
class SocGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var soc: Double = 0.0

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1FFFFFFF")
        strokeWidth = 14f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00D4AA")
        strokeWidth = 14f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val textValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val textLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#90FFFFFF")
        textSize = 15f
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.05f
    }

    private val arcBounds = RectF()

    fun setSoc(percent: Double) {
        soc = percent.coerceIn(0.0, 100.0)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val size = Math.min(w, h)
        val stroke = 14f
        val padding = stroke / 2f + 8f

        arcBounds.set(
            (w - size) / 2f + padding,
            (h - size) / 2f + padding,
            (w + size) / 2f - padding,
            (h + size) / 2f - padding
        )

        // Arc starts at bottom-left (135°) and sweeps 270° to bottom-right
        val startAngle = 135f
        val sweepMax = 270f
        val sweepAngle = ((soc / 100.0) * sweepMax).toFloat()

        // Background track
        canvas.drawArc(arcBounds, startAngle, sweepMax, false, trackPaint)

        // Progress
        if (sweepAngle > 0f) {
            canvas.drawArc(arcBounds, startAngle, sweepAngle, false, progressPaint)
        }

        // Center text
        val cx = w / 2f
        val cy = h / 2f
        val socText = if (soc > 0) "${Math.round(soc)}%" else "--"
        canvas.drawText(socText, cx, cy + 4f, textValuePaint)
        canvas.drawText("SoC", cx, cy + 30f, textLabelPaint)
    }
}
