package com.overdrive.app.od

import android.content.Context
import java.util.Arrays

/**
 * Pure Kotlin binding for blind-spot lens projection coefficients.
 *
 * <p>Migrated from the legacy closed-source {@code libod.so} library to pure Kotlin.
 * Eliminates JNI overhead, native load issues, and the SHA-256 certificate signature gate.
 */
object Od {

    @Volatile
    private var ready = true

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
        ready = true
        return true
    }

    /**
     * Resolves the coefficient set (in[11] → out[20]) via [PureOdDewarp].
     * Zero-allocates on calls, thread-safe and non-blocking.
     */
    @JvmStatic
    fun resolve(input: FloatArray?, output: FloatArray?) {
        if (output == null) return
        if (input == null || input.size < 11 || output.size < 20) {
            Arrays.fill(output, 0f)
            return
        }
        val success = PureOdDewarp.derive(input, output)
        if (!success) {
            Arrays.fill(output, 0f)
        }
    }

    val isReady: Boolean
        get() = ready
}
