package com.overdrive.app.ui.adb

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying state mutations and command execution invariants for AdbConsole Compose Native.
 */
class AdbConsoleComposeParityTest {

    @Test
    fun initialState_hasDefaultAdbConsoleState() {
        val state = AdbConsoleUiState()
        assertEquals("", state.currentCommand)
        assertEquals("$ Ready for commands…", state.outputLog)
        assertFalse(state.isExecuting)
    }

    @Test
    fun commandInput_updatesCurrentCommand() {
        val state = AdbConsoleUiState()
        val updated = state.copy(currentCommand = "dumpsys media.camera")
        assertEquals("dumpsys media.camera", updated.currentCommand)
    }

    @Test
    fun executingState_updatesCorrectly() {
        val state = AdbConsoleUiState()
        val executing = state.copy(isExecuting = true)
        assertTrue(executing.isExecuting)

        val finished = executing.copy(
            isExecuting = false,
            outputLog = "$ dumpsys media.camera\nCamera service operational"
        )
        assertFalse(finished.isExecuting)
        assertTrue(finished.outputLog.contains("Camera service operational"))
    }
}
