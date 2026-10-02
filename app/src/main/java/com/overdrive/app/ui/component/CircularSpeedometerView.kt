package com.overdrive.app.ui.component

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.overdrive.app.domain.model.OperationMode

/**
 * Modern Circular Speedometer View (Canlı Hız Kadranı).
 * - Sürüş Moduna Göre Değişen Dinamik Ambiyans Renkleri:
 *   - ECO: Zümrüt Yeşili (#10B981) / Nane (#34D399)
 *   - NORMAL: BYD Okyanus Mavisi (#06B6D4) / Kobalt & Camgöbeği (#0284C7)
 *   - SPORT: Alev Kırmızısı (#EF4444) / Kehribar & Turuncu (#F97316)
 *   - SNOW: Buzul & Gök Mavisi (#38BDF8) / Kristal Beyaz (#E0F2FE)
 */
class CircularSpeedometerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var isDarkTheme: Boolean = true
    private var currentOperationMode: OperationMode = OperationMode.NORMAL

    private var targetSpeedKmh: Float = 0f
    private var displayedSpeedKmh: Float = 0f

    private val maxSpeedKmh = 220f

    // Açılar:
    private val trackStartAngle = 305f
    private val trackSweepAngle = 235f

    // Aktif Hız Yayı Başlangıcı: Üst tepe (285° / ~12 o'clock)
    private val progressStartAngle = 285f
    private val maxProgressSweep = 215f

    // Boya Nesneleri
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val speedTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val unitTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.08f
        isFakeBoldText = true
    }

    private val arcBounds = RectF()
    private var speedAnimator: ValueAnimator? = null
    private var colorAnimator: ValueAnimator? = null

    private var currentProgressColor: Int = Color.parseColor("#06B6D4")
    private var currentUnitColor: Int = Color.parseColor("#0284C7")

    init {
        updateThemeColors(animate = false)
    }

    fun setDarkTheme(isDark: Boolean) {
        if (this.isDarkTheme == isDark) return
        this.isDarkTheme = isDark
        updateThemeColors(animate = false)
        invalidate()
    }

    /**
     * Sürüş modunu günceller ve renkleri akıcı geçişle değiştirir.
     */
    fun setDriveMode(mode: OperationMode) {
        if (this.currentOperationMode == mode) return
        this.currentOperationMode = mode
        updateThemeColors(animate = true)
    }

    private fun getDriveModeColors(mode: OperationMode): Pair<Int, Int> {
        return when (mode) {
            OperationMode.ECO -> Pair(Color.parseColor("#10B981"), Color.parseColor("#34D399"))
            OperationMode.NORMAL -> Pair(Color.parseColor("#06B6D4"), Color.parseColor("#0284C7"))
            OperationMode.SPORT -> Pair(Color.parseColor("#EF4444"), Color.parseColor("#F97316"))
            OperationMode.SNOW -> Pair(Color.parseColor("#38BDF8"), if (!isDarkTheme) Color.parseColor("#0284C7") else Color.parseColor("#E0F2FE"))
            else -> Pair(Color.parseColor("#06B6D4"), Color.parseColor("#0284C7"))
        }
    }

    private fun updateThemeColors(animate: Boolean = true) {
        val density = resources.displayMetrics.density
        if (!isDarkTheme) {
            trackPaint.color = Color.parseColor("#E2E8F0") // Slate 200
            speedTextPaint.color = Color.parseColor("#0F172A") // Slate 900
        } else {
            trackPaint.color = Color.parseColor("#1E293B") // Slate 800
            speedTextPaint.color = Color.parseColor("#F8FAFC") // Slate 50
        }

        val (targetProgress, targetUnit) = getDriveModeColors(currentOperationMode)

        if (animate) {
            val startProg = currentProgressColor
            val startUnit = currentUnitColor
            val evaluator = ArgbEvaluator()

            colorAnimator?.cancel()
            colorAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 300
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    val frac = it.animatedValue as Float
                    currentProgressColor = evaluator.evaluate(frac, startProg, targetProgress) as Int
                    currentUnitColor = evaluator.evaluate(frac, startUnit, targetUnit) as Int
                    progressPaint.color = currentProgressColor
                    unitTextPaint.color = currentUnitColor
                    invalidate()
                }
                start()
            }
        } else {
            currentProgressColor = targetProgress
            currentUnitColor = targetUnit
            progressPaint.color = currentProgressColor
            unitTextPaint.color = currentUnitColor
        }
    }

    /**
     * Anlık Hız Değerini Günceller (km/h).
     */
    fun setSpeedKmh(speed: Float) {
        val deadbandSpeed = if (speed < 1.5f) 0f else speed
        val clamped = deadbandSpeed.coerceIn(0f, maxSpeedKmh)
        if (targetSpeedKmh == clamped) return
        targetSpeedKmh = clamped

        speedAnimator?.cancel()
        speedAnimator = ValueAnimator.ofFloat(displayedSpeedKmh, targetSpeedKmh).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                displayedSpeedKmh = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val size = minOf(w, h).toFloat()
        if (size <= 0) return
        val density = resources.displayMetrics.density
        val stroke = (size * 0.075f).coerceIn(6f * density, 12f * density)
        trackPaint.strokeWidth = stroke
        progressPaint.strokeWidth = stroke
        speedTextPaint.textSize = (size * 0.28f).coerceIn(20f * density, 44f * density)
        unitTextPaint.textSize = (size * 0.082f).coerceIn(7.5f * density, 13f * density)

        val pad = stroke / 2f + 4f * density
        val left = (w - size) / 2f + pad
        val top = (h - size) / 2f + pad
        arcBounds.set(left, top, left + size - 2 * pad, top + size - 2 * pad)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Taban Yolu (Track Arc)
        canvas.drawArc(arcBounds, trackStartAngle, trackSweepAngle, false, trackPaint)

        // 2. Aktif Hız Yayı (Progress Arc)
        val ratio = (displayedSpeedKmh / maxSpeedKmh).coerceIn(0f, 1f)
        if (ratio > 0.005f) {
            val sweep = (ratio * maxProgressSweep).coerceAtLeast(6f)
            canvas.drawArc(arcBounds, progressStartAngle, sweep, false, progressPaint)
        }

        // 3. Merkez Yazıları
        val centerX = arcBounds.centerX()
        val centerY = arcBounds.centerY()

        // Hız Rakamı
        val speedStr = if (displayedSpeedKmh < 1.0f) "0" else displayedSpeedKmh.toInt().toString()
        val speedY = centerY + speedTextPaint.textSize * 0.16f
        canvas.drawText(speedStr, centerX, speedY, speedTextPaint)

        // KM/H Birimi
        val unitY = speedY + unitTextPaint.textSize * 1.5f
        canvas.drawText("KM/H", centerX, unitY, unitTextPaint)
    }
}
