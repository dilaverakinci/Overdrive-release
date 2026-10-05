package com.overdrive.app.od

import android.content.Context
import java.util.Arrays
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan

/**
 * Pure Kotlin implementation of lens projection and blind-spot dewarping.
 *
 * <p>Replaces the legacy closed-source {@code libod.so} native library, eliminates
 * JNI marshalling latency, and removes the private signing certificate SHA-256 DRM gate
 * so custom builds can render blind-spot camera cards without screen blackout.
 *
 * <p>Calculates the 20-element coefficient vector from an 11-element tuning parameter vector
 * for consumption by the OpenGL ES fragment shaders (uniforms uOd0..uOd4).
 */
object Od {

    const val K_CLAMP: Float = 1.55334303f

    @Volatile
    var isReady: Boolean = true
        private set

    /**
     * Preserved for backward compatibility with the app_process daemon (UID 2000)
     * and CameraDaemon. No-op since native library loading is no longer required.
     */
    @JvmStatic
    fun tryLoadLibrary(nativeLibDir: String): Boolean = true

    /**
     * Preserved for backward compatibility with callers.
     * Host signature authorization check is permanently bypassed.
     */
    @JvmStatic
    fun authorize(context: Context?): Boolean {
        isReady = true
        return true
    }

    /**
     * Resolves the 11-element input parameter vector into a 20-element output
     * coefficient vector passed to the OpenGL ES fragment shader (uOd0..uOd4).
     *
     * @param input  11 floats: [a, b, c, d, e, f, g, blend, rRoll, rPitch, sign]
     * @param output 20 floats: [lo, hi, span, h0, h1, ctr, bMid, bHalf, t0, t1,
     *                          k1, k2, rc, rs, lift, vg, rearRc, rearRs, rearPitch, rpad]
     */
    @JvmStatic
    fun resolve(input: FloatArray?, output: FloatArray?) {
        if (output == null) return
        if (input == null || input.size < 11 || output.size < 20) {
            Arrays.fill(output, 0f)
            return
        }

        val a = input[0]
        val b = input[1]
        val c = input[2]
        val d = input[3]
        val e = input[4]
        val f = input[5]
        val g = input[6]
        val blend = input[7]
        val rRoll = input[8]
        val rPitch = input[9]
        val sign = input[10]

        val h0 = max(a, 0.05f) * 0.5f
        val raw = if (b > 0.001f) b else a
        val h1 = max(raw, 0.05f) * 0.5f
        val ctr = sign * max(c, 0.0f)

        val lo = min(-h0, ctr - h1)
        val hi = max(h0, ctr + h1)
        val span = max(hi - lo, 1.0e-4f)

        val ovLo = max(-h0, ctr - h1)
        val ovHi = min(h0, ctr + h1)
        val bMid = 0.5f * (ovLo + ovHi)
        val ovHalf = max(0.5f * (ovHi - ovLo), 1.0e-4f)
        val clampedBlend = blend.coerceIn(0.0f, 1.0f)
        val bHalf = max(clampedBlend * ovHalf, 1.0e-4f)

        val t0 = max(tan(max(h0, 0.025f)), 0.001f)
        val t1 = max(tan(max(h1, 0.025f)), 0.001f)

        val k1 = f
        val k2 = f * 0.33333f

        val roll = d * sign
        val rc = cos(roll)
        val rs = sin(roll)

        val lift = e
        val vg = g

        // Rear-tap rotation. rRoll is an absolute camera level (NOT *sign); the
        // shader's flip-parity handles the per-quadrant mirror.
        val rearRc = cos(rRoll)
        val rearRs = sin(rRoll)
        val rearPitch = rPitch
        val rpad = 0.0f

        output[0] = lo
        output[1] = hi
        output[2] = span
        output[3] = h0
        output[4] = h1
        output[5] = ctr
        output[6] = bMid
        output[7] = bHalf
        output[8] = t0
        output[9] = t1
        output[10] = k1
        output[11] = k2
        output[12] = rc
        output[13] = rs
        output[14] = lift
        output[15] = vg
        output[16] = rearRc
        output[17] = rearRs
        output[18] = rearPitch
        output[19] = rpad
    }
}
