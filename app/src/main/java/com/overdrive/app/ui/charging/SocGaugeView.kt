package com.overdrive.app.ui.charging

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Circular arc gauge view for State of Charge (SoC).
 * Automatically adapts track and text colors to light/dark themes.
 */
class SocGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var soc: Double = 0.0

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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
        textSize = 48f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val textLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 20f
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.08f
        isFakeBoldText = true
    }

    private val arcBounds = RectF()

    init {
        updateColors()
    }

    private fun isNightMode(): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }

    private fun updateColors() {
        val night = isNightMode()
        if (night) {
            trackPaint.color = Color.parseColor("#2E3036")
            textValuePaint.color = Color.WHITE
            textLabelPaint.color = Color.parseColor("#9E9E9E")
        } else {
            trackPaint.color = Color.parseColor("#E0E2EC")
            textValuePaint.color = Color.parseColor("#1A1C1E")
            textLabelPaint.color = Color.parseColor("#74777F")
        }
    }

    fun setSoc(percent: Double) {
        soc = percent.coerceIn(0.0, 100.0)
        invalidate()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        updateColors()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        updateColors()

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
        canvas.drawText(socText, cx, cy + 6f, textValuePaint)
        canvas.drawText("STATE OF CHARGE", cx, cy + 34f, textLabelPaint)
    }
}
