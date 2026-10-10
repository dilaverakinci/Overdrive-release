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
 * Circular 360-degree gauge view for State of Charge (SoC).
 * Matches the legacy OverDrive `socCircleCanvas` and `dashboard-soc-gauge` 1:1.
 * Automatically adapts track, brand, and text colors to light/dark themes.
 */
class SocGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var soc: Double? = null
    private val density = context.resources.displayMetrics.density
    private val scaledDensity = context.resources.displayMetrics.scaledDensity

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val textValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val textLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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
        val strokePx = 8f * density
        trackPaint.strokeWidth = strokePx
        progressPaint.strokeWidth = strokePx

        textValuePaint.textSize = 24f * scaledDensity
        textLabelPaint.textSize = 7.5f * scaledDensity

        if (night) {
            trackPaint.color = Color.parseColor("#1FFFFFFF") // subtle arc track
            progressPaint.color = Color.parseColor("#00D4AA")
            textValuePaint.color = Color.WHITE
            textLabelPaint.color = Color.parseColor("#8AFFFFFF")
        } else {
            trackPaint.color = Color.parseColor("#1A000000") // subtle light track
            progressPaint.color = Color.parseColor("#007A62")
            textValuePaint.color = Color.parseColor("#1A1C1E")
            textLabelPaint.color = Color.parseColor("#74777F")
        }
    }

    fun setSoc(percent: Double?) {
        soc = percent?.coerceIn(0.0, 100.0)
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
        val radius = 48f * density * (size / (120f * density))
        val cx = w / 2f
        val cy = h / 2f

        arcBounds.set(cx - radius, cy - radius, cx + radius, cy + radius)

        // 1. Full 360-degree background track
        canvas.drawCircle(cx, cy, radius, trackPaint)

        // 2. Active progress arc (starts at -90 deg / 12 o'clock, sweeps clockwise)
        val socVal = soc
        if (socVal != null && socVal > 0.0) {
            val sweepAngle = ((Math.min(socVal, 100.0) / 100.0) * 360.0).toFloat()
            canvas.drawArc(arcBounds, -90f, sweepAngle, false, progressPaint)
        }

        // 3. Center value and uppercase label
        val socText = if (socVal != null && socVal >= 0.0) "${Math.round(socVal)}%" else "--"
        val labelText = "STATE OF CHARGE"

        val valAscent = textValuePaint.ascent()
        val valDescent = textValuePaint.descent()
        val valHeight = valDescent - valAscent

        val labelAscent = textLabelPaint.ascent()
        val labelDescent = textLabelPaint.descent()
        val labelHeight = labelDescent - labelAscent

        val gap = 3f * density
        val totalBlockHeight = valHeight + gap + labelHeight

        val blockTop = cy - (totalBlockHeight / 2f)
        val valueBaseline = blockTop - valAscent
        val labelBaseline = valueBaseline + valDescent + gap - labelAscent

        canvas.drawText(socText, cx, valueBaseline, textValuePaint)
        canvas.drawText(labelText, cx, labelBaseline, textLabelPaint)
    }
}
