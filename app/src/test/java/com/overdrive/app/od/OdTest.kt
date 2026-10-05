package com.overdrive.app.od

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

class OdTest {

    private val eps = 1e-5f

    @Test
    fun testStandardDeriveMathParity() {
        val input = floatArrayOf(
            0.5f,   // a
            0.5f,   // b
            0.1f,   // c
            0.05f,  // d (roll)
            0.02f,  // e (lift)
            -0.1f,  // f (k1)
            0.8f,   // g (vg)
            0.5f,   // blend
            0.01f,  // rRoll
            -0.02f, // rPitch
            1.0f    // sign
        )
        val output = FloatArray(20)

        Od.resolve(input, output)

        // Hand-calculated ground truth from C++ od_core.h
        val h0 = 0.25f
        val h1 = 0.25f
        val ctr = 0.1f
        val lo = -0.25f
        val hi = 0.35f
        val span = 0.6f
        val bMid = 0.05f
        val bHalf = 0.1f
        val t0 = tan(0.25f)
        val t1 = tan(0.25f)
        val k1 = -0.1f
        val k2 = -0.1f * 0.33333f
        val rc = cos(0.05f)
        val rs = sin(0.05f)
        val lift = 0.02f
        val vg = 0.8f
        val rearRc = cos(0.01f)
        val rearRs = sin(0.01f)
        val rearPitch = -0.02f
        val rpad = 0.0f

        assertEquals(lo, output[0], eps)
        assertEquals(hi, output[1], eps)
        assertEquals(span, output[2], eps)
        assertEquals(h0, output[3], eps)
        assertEquals(h1, output[4], eps)
        assertEquals(ctr, output[5], eps)
        assertEquals(bMid, output[6], eps)
        assertEquals(bHalf, output[7], eps)
        assertEquals(t0, output[8], eps)
        assertEquals(t1, output[9], eps)
        assertEquals(k1, output[10], eps)
        assertEquals(k2, output[11], eps)
        assertEquals(rc, output[12], eps)
        assertEquals(rs, output[13], eps)
        assertEquals(lift, output[14], eps)
        assertEquals(vg, output[15], eps)
        assertEquals(rearRc, output[16], eps)
        assertEquals(rearRs, output[17], eps)
        assertEquals(rearPitch, output[18], eps)
        assertEquals(rpad, output[19], eps)
    }

    @Test
    fun testSignFlipForLeftAndRightCamera() {
        val inputRight = floatArrayOf(0.4f, 0.4f, 0.15f, 0.08f, 0.01f, -0.05f, 0.9f, 0.4f, 0.02f, 0.01f, 1.0f)
        val inputLeft = floatArrayOf(0.4f, 0.4f, 0.15f, 0.08f, 0.01f, -0.05f, 0.9f, 0.4f, 0.02f, 0.01f, -1.0f)

        val outRight = FloatArray(20)
        val outLeft = FloatArray(20)

        Od.resolve(inputRight, outRight)
        Od.resolve(inputLeft, outLeft)

        // ctr and roll sin flip sign
        assertEquals(0.15f, outRight[5], eps)
        assertEquals(-0.15f, outLeft[5], eps)

        // roll cos is even (symmetric)
        assertEquals(outRight[12], outLeft[12], eps)
        // roll sin is odd (anti-symmetric)
        assertEquals(outRight[13], -outLeft[13], eps)
    }

    @Test
    fun testBufferSafetyAndNullHandling() {
        val validIn = FloatArray(11)
        val smallOut = FloatArray(10)
        Od.resolve(validIn, smallOut) // Should safely do nothing and not crash

        val odOut = FloatArray(20)
        odOut[0] = 999f
        Od.resolve(null, odOut)
        assertEquals(0f, odOut[0], 0f)

        val shortIn = FloatArray(5)
        odOut[0] = 999f
        Od.resolve(shortIn, odOut)
        assertEquals(0f, odOut[0], 0f)
    }

    @Test
    fun testOdCompatibilityFacadeAlwaysReady() {
        assertTrue(Od.isReady)
        assertTrue(Od.tryLoadLibrary("/system/lib64"))
        assertTrue(Od.authorize(null))

        val input = floatArrayOf(0.5f, 0.5f, 0.1f, 0.05f, 0.02f, -0.1f, 0.8f, 0.5f, 0.01f, -0.02f, 1.0f)
        val output = FloatArray(20)
        Od.resolve(input, output)

        // Output should be fully computed, not zeroed
        assertTrue(output[2] > 0f) // span > 0
        assertEquals(0.6f, output[2], eps)
    }
}
