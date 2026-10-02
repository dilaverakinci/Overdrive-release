package com.overdrive.app.ui.component

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.overdrive.app.R
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

/**
 * Realistic Chassis Energy & Power Flow View.
 *
 * - Base Layer: 90° cutaway render of vehicle chassis (AWD or RWD).
 * - Live Telemetry:
 *   - powerKw > 0: Battery -> Motor -> Wheels (Cyan/Turquoise Power Acceleration Flow)
 *   - powerKw < 0: Wheels -> Motor -> Battery (Emerald/Lime Regeneration Flow)
 *   - powerKw == 0: Stationary (0 FPS Sleep Mode)
 */
class ChassisEnergyFlowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var rwdBitmap: Bitmap? = null
    private var awdBitmap: Bitmap? = null

    private var isAwd: Boolean = false
    private var powerKw: Double = 0.0
    private var socPercent: Int = 80

    private val carBounds = RectF()
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    // Paint brushes
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val strokeGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    // Animation
    private var flowAnimator: ValueAnimator? = null
    private var animPhase: Float = 0f

    // Colors
    private val colPowerGlow = Color.parseColor("#00D2FF") // Cyan
    private val colPowerCore = Color.parseColor("#FFFFFF") // White core
    private val colPowerBattery = Color.parseColor("#2600D2FF") // Transparent Cyan

    private val colRegenGlow = Color.parseColor("#28FF70") // Neon Emerald
    private val colRegenCore = Color.parseColor("#F0FFF4") // Light Mint
    private val colRegenBattery = Color.parseColor("#2B28FF70") // Transparent Green

    init {
        loadBitmaps()
    }

    private fun loadBitmaps() {
        try {
            rwdBitmap = decodeDrawableBitmap(R.drawable.img_sealion7_rwd_cutaway)
            awdBitmap = decodeDrawableBitmap(R.drawable.img_sealion7_awd_cutaway)
        } catch (_: Throwable) {}
    }

    private fun decodeDrawableBitmap(drawableResId: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, drawableResId) ?: return null
        val w = drawable.intrinsicWidth.coerceAtLeast(100)
        val h = drawable.intrinsicHeight.coerceAtLeast(100)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bmp
    }

    fun setAwd(awd: Boolean) {
        if (isAwd != awd) {
            isAwd = awd
            invalidate()
        }
    }

    fun isAwd(): Boolean = isAwd

    fun setSoc(soc: Int) {
        val clamped = soc.coerceIn(0, 100)
        if (socPercent != clamped) {
            socPercent = clamped
            invalidate()
        }
    }

    fun setPowerKw(kw: Double) {
        if (abs(powerKw - kw) < 0.2) return
        powerKw = kw
        manageAnimationState()
        invalidate()
    }

    fun getPowerKw(): Double = powerKw

    private fun manageAnimationState() {
        val isActive = abs(powerKw) >= 0.5 && isAttachedToWindow && visibility == VISIBLE
        if (isActive) {
            val powerMag = abs(powerKw).coerceIn(5.0, 250.0)
            val speedFactor = (powerMag - 5.0) / 245.0
            val durationMs = (1400L - (800.0 * speedFactor).toLong()).coerceIn(500L, 1400L)

            if (flowAnimator == null) {
                flowAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                    interpolator = LinearInterpolator()
                    repeatCount = ValueAnimator.INFINITE
                    duration = durationMs
                    addUpdateListener { va ->
                        animPhase = va.animatedValue as Float
                        invalidate()
                    }
                    start()
                }
            } else {
                if (flowAnimator?.duration != durationMs) {
                    flowAnimator?.duration = durationMs
                }
                if (!flowAnimator!!.isRunning) {
                    flowAnimator!!.start()
                }
            }
        } else {
            flowAnimator?.cancel()
            flowAnimator = null
            animPhase = 0f
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        manageAnimationState()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        flowAnimator?.cancel()
        flowAnimator = null
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        manageAnimationState()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val bmp = (if (isAwd) awdBitmap else rwdBitmap) ?: return
        val bmpW = bmp.width.toFloat()
        val bmpH = bmp.height.toFloat()

        val scale = min(w / bmpW, h / bmpH) * 0.96f
        val targetW = bmpW * scale
        val targetH = bmpH * scale

        val left = (w - targetW) / 2f
        val top = (h - targetH) / 2f
        carBounds.set(left, top, left + targetW, top + targetH)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = if (isAwd) awdBitmap else rwdBitmap
        if (bmp == null || carBounds.isEmpty) return

        // 1. Cutaway chassis bitmap
        canvas.drawBitmap(bmp, null, carBounds, bitmapPaint)

        val isRegen = powerKw < -0.5
        val isPower = powerKw > 0.5

        if (!isRegen && !isPower) {
            return
        }

        val glowColor = if (isRegen) colRegenGlow else colPowerGlow
        val coreColor = if (isRegen) colRegenCore else colPowerCore
        val batFillColor = if (isRegen) colRegenBattery else colPowerBattery

        val bw = carBounds.width()
        val bh = carBounds.height()
        val bx = carBounds.left
        val by = carBounds.top

        val batLeft = if (isAwd) (bx + bw * 0.22f) else (bx + bw * 0.20f)
        val batRight = if (isAwd) (bx + bw * 0.78f) else (bx + bw * 0.80f)
        val batTop = if (isAwd) (by + bh * 0.35f) else (by + bh * 0.41f)
        val batBottom = if (isAwd) (by + bh * 0.69f) else (by + bh * 0.70f)
        val batCenterX = (batLeft + batRight) / 2f
        val batCenterY = (batTop + batBottom) / 2f

        val rearMotorX = batCenterX
        val rearMotorY = if (isAwd) (by + bh * 0.765f) else (by + bh * 0.795f)
        val frontMotorX = batCenterX
        val frontMotorY = by + bh * 0.165f

        val wheelFLX = bx + bw * 0.11f
        val wheelFLY = if (isAwd) (by + bh * 0.260f) else (by + bh * 0.275f)

        val wheelFRX = bx + bw * 0.89f
        val wheelFRY = wheelFLY

        val wheelRLX = bx + bw * 0.11f
        val wheelRLY = if (isAwd) (by + bh * 0.755f) else (by + bh * 0.780f)

        val wheelRRX = bx + bw * 0.89f
        val wheelRRY = wheelRLY

        // 2. Battery Glow & Pulse
        drawBatteryGlow(canvas, batLeft, batTop, batRight, batBottom, batCenterX, batCenterY, batFillColor, glowColor, isRegen)

        // 3. Rear Axle & Motor Flow
        drawEnergyStream(canvas, batCenterX, batBottom, rearMotorX, rearMotorY - dp(6f), isRegen, glowColor, coreColor)
        drawMotorPulse(canvas, rearMotorX, rearMotorY, glowColor, coreColor)
        drawEnergyStream(canvas, rearMotorX - dp(12f), rearMotorY, wheelRLX + dp(10f), wheelRLY, isRegen, glowColor, coreColor)
        drawEnergyStream(canvas, rearMotorX + dp(12f), rearMotorY, wheelRRX - dp(10f), wheelRRY, isRegen, glowColor, coreColor)
        drawWheelTractionAura(canvas, wheelRLX, wheelRLY, glowColor, isRegen)
        drawWheelTractionAura(canvas, wheelRRX, wheelRRY, glowColor, isRegen)

        // 4. If AWD, Front Axle & Motor Flow
        if (isAwd) {
            drawEnergyStream(canvas, batCenterX, batTop, frontMotorX, frontMotorY + dp(6f), isRegen, glowColor, coreColor)
            drawMotorPulse(canvas, frontMotorX, frontMotorY, glowColor, coreColor)
            drawEnergyStream(canvas, frontMotorX - dp(12f), frontMotorY, wheelFLX + dp(10f), wheelFLY, isRegen, glowColor, coreColor)
            drawEnergyStream(canvas, frontMotorX + dp(12f), frontMotorY, wheelFRX - dp(10f), wheelFRY, isRegen, glowColor, coreColor)
            drawWheelTractionAura(canvas, wheelFLX, wheelFLY, glowColor, isRegen)
            drawWheelTractionAura(canvas, wheelFRX, wheelFRY, glowColor, isRegen)
        }
    }

    private fun drawBatteryGlow(
        canvas: Canvas,
        left: Float, top: Float, right: Float, bottom: Float,
        cx: Float, cy: Float,
        fillColor: Int, glowColor: Int, isRegen: Boolean
    ) {
        val pulse = 0.5f + 0.5f * sin(2.0 * Math.PI * animPhase).toFloat()
        glowPaint.color = fillColor
        val rect = RectF(left, top, right, bottom)
        canvas.drawRoundRect(rect, dp(6f), dp(6f), glowPaint)

        strokeGlowPaint.color = glowColor
        strokeGlowPaint.strokeWidth = dp(1.5f)
        strokeGlowPaint.alpha = (70 + 60 * pulse).toInt().coerceIn(0, 255)
        canvas.drawRoundRect(rect, dp(6f), dp(6f), strokeGlowPaint)

        val maxR = (right - left) * 0.35f
        val rippleR = if (isRegen) maxR * (1f - (animPhase % 1f)) else maxR * (animPhase % 1f)
        val ripAlpha = (120 * (1f - abs((animPhase % 1f) - 0.5f) * 2f)).toInt().coerceIn(0, 255)
        strokeGlowPaint.alpha = ripAlpha
        strokeGlowPaint.strokeWidth = dp(1.2f)
        canvas.drawCircle(cx, cy, rippleR, strokeGlowPaint)
    }

    private fun drawEnergyStream(
        canvas: Canvas,
        x1: Float, y1: Float, x2: Float, y2: Float,
        isReverse: Boolean, glowColor: Int, coreColor: Int
    ) {
        val numParticles = 3
        for (i in 0 until numParticles) {
            var t = (animPhase + i.toFloat() / numParticles) % 1.0f
            if (isReverse) {
                t = 1.0f - t
            }

            val px = x1 + t * (x2 - x1)
            val py = y1 + t * (y2 - y1)

            glowPaint.color = glowColor
            glowPaint.alpha = 180
            canvas.drawCircle(px, py, dp(4.5f), glowPaint)

            corePaint.color = coreColor
            corePaint.alpha = 255
            canvas.drawCircle(px, py, dp(2f), corePaint)

            val tailLen = dp(9f) * (if (isReverse) 1f else -1f)
            val dx = (x2 - x1)
            val dy = (y2 - y1)
            val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
            if (dist > 1f) {
                val ndx = dx / dist
                val ndy = dy / dist
                strokeGlowPaint.color = glowColor
                strokeGlowPaint.strokeWidth = dp(1.8f)
                strokeGlowPaint.alpha = 120
                canvas.drawLine(px, py, px + ndx * tailLen, py + ndy * tailLen, strokeGlowPaint)
            }
        }
    }

    private fun drawMotorPulse(canvas: Canvas, mx: Float, my: Float, glowColor: Int, coreColor: Int) {
        val pulse = 0.5f + 0.5f * sin(2.0 * Math.PI * animPhase + 1.0).toFloat()
        val r = dp(13f + 3f * pulse)

        glowPaint.color = glowColor
        glowPaint.alpha = (50 + 40 * pulse).toInt().coerceIn(0, 255)
        canvas.drawCircle(mx, my, r, glowPaint)

        corePaint.color = coreColor
        corePaint.alpha = (120 + 80 * pulse).toInt().coerceIn(0, 255)
        canvas.drawCircle(mx, my, dp(4f), corePaint)
    }

    private fun drawWheelTractionAura(canvas: Canvas, wx: Float, wy: Float, glowColor: Int, isRegen: Boolean) {
        val pulse = 0.5f + 0.5f * sin(2.0 * Math.PI * animPhase + 2.0).toFloat()
        strokeGlowPaint.color = glowColor
        strokeGlowPaint.strokeWidth = dp(1.8f)
        strokeGlowPaint.alpha = (60 + 50 * pulse).toInt().coerceIn(0, 255)

        val rw = dp(8f)
        val rh = dp(16f)
        canvas.drawOval(RectF(wx - rw, wy - rh, wx + rw, wy + rh), strokeGlowPaint)
    }

    private fun dp(v: Float): Float = v * context.resources.displayMetrics.density
}
