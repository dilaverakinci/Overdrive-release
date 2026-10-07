package com.overdrive.app.ui.trips

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class DnaRadarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var scores: DnaScoresItem? = null

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#30808080")
        strokeWidth = 2f
    }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#40808080")
        strokeWidth = 2f
    }

    private val polygonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#3300D4AA")
    }

    private val polygonStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#00D4AA")
        strokeWidth = 5f
        strokeJoin = Paint.Join.ROUND
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#00D4AA")
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9E9E9E")
        textSize = 28f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val valueTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF")
        textSize = 32f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val axisLabels = arrayOf(
        "Anticipation",
        "Smoothness",
        "Speed",
        "Efficiency",
        "Consistency"
    )

    fun setScores(scores: DnaScoresItem?) {
        this.scores = scores
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val centerX = w / 2f
        val centerY = h / 2f
        val radius = (Math.min(w, h) / 2f) - 60f
        if (radius <= 0f) return

        val numAxes = 5
        val angleStep = (2 * Math.PI / numAxes).toFloat()
        val startAngle = (-Math.PI / 2).toFloat() // top vertex

        // Draw concentric web rings (25, 50, 75, 100)
        val rings = 4
        for (r in 1..rings) {
            val ringRadius = radius * (r.toFloat() / rings)
            val path = Path()
            for (i in 0 until numAxes) {
                val angle = startAngle + i * angleStep
                val x = centerX + ringRadius * cos(angle.toDouble()).toFloat()
                val y = centerY + ringRadius * sin(angle.toDouble()).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            canvas.drawPath(path, gridPaint)
        }

        // Draw radial spokes & labels
        for (i in 0 until numAxes) {
            val angle = startAngle + i * angleStep
            val cosA = cos(angle.toDouble()).toFloat()
            val sinA = sin(angle.toDouble()).toFloat()
            val spokeX = centerX + radius * cosA
            val spokeY = centerY + radius * sinA
            canvas.drawLine(centerX, centerY, spokeX, spokeY, axisPaint)

            val labelDist = radius + 36f
            val labelX = centerX + labelDist * cosA
            val labelY = centerY + labelDist * sinA + 10f
            canvas.drawText(axisLabels[i], labelX, labelY, textPaint)
        }

        val s = scores
        if (s != null) {
            val values = floatArrayOf(
                s.anticipation.toFloat().coerceIn(0f, 100f),
                s.smoothness.toFloat().coerceIn(0f, 100f),
                s.speedDiscipline.toFloat().coerceIn(0f, 100f),
                s.efficiency.toFloat().coerceIn(0f, 100f),
                s.consistency.toFloat().coerceIn(0f, 100f)
            )

            val dataPath = Path()
            val points = mutableListOf<PointF>()

            for (i in 0 until numAxes) {
                val angle = startAngle + i * angleStep
                val dist = radius * (values[i] / 100f)
                val x = centerX + dist * cos(angle.toDouble()).toFloat()
                val y = centerY + dist * sin(angle.toDouble()).toFloat()
                points.add(PointF(x, y))
                if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
            }
            dataPath.close()

            canvas.drawPath(dataPath, polygonPaint)
            canvas.drawPath(dataPath, polygonStrokePaint)

            for (p in points) {
                canvas.drawCircle(p.x, p.y, 8f, pointPaint)
            }
        }
    }
}
