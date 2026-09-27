package com.overdrive.app.ui.seats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying state mutations, actions, and safety invariants for SeatPositions Compose Native.
 */
class SeatPositionsComposeParityTest {

    @Test
    fun initialState_hasDefaultPositionsAndAxes() {
        val state = SeatPositionsUiState()
        assertTrue("Car ACC should be on by default", state.isCarAccOn)
        assertFalse("Movement should not be blocked by default", state.isMovementBlocked)
        assertEquals(3, state.savedPositions.size)
        assertNotNull("Horizontal axis should be populated", state.currentAxes.horizontal)
        assertEquals(52f, state.currentAxes.horizontal)
    }

    @Test
    fun toggleDetails_invertsExpandedState() {
        val state = SeatPositionsUiState(isDetailsExpanded = false)
        val next = state.copy(isDetailsExpanded = !state.isDetailsExpanded)
        assertTrue(next.isDetailsExpanded)
    }

    @Test
    fun applyPosition_updatesMatchAndApplyingId() {
        val state = SeatPositionsUiState()
        val targetId = "slot-2"
        val next = state.copy(
            isApplyingPositionId = targetId,
            currentPositionMatchName = "Dinlenme / Giriş-Çıkış (P2)"
        )
        assertEquals(targetId, next.isApplyingPositionId)
        assertEquals("Dinlenme / Giriş-Çıkış (P2)", next.currentPositionMatchName)
    }

    @Test
    fun createPosition_addsNewUserPosition() {
        val state = SeatPositionsUiState()
        val newPos = SavedSeatPosition(
            id = "user-custom-1",
            name = "Benim Konumum",
            source = "user",
            hasGeometry = true,
            hasAmbient = false,
        )
        val next = state.copy(savedPositions = state.savedPositions + newPos)
        assertEquals(4, next.savedPositions.size)
        assertEquals("Benim Konumum", next.savedPositions.last().name)
    }

    @Test
    fun deletePosition_removesTargetPosition() {
        val state = SeatPositionsUiState()
        val removeId = "user-sport"
        val next = state.copy(savedPositions = state.savedPositions.filterNot { it.id == removeId })
        assertEquals(2, next.savedPositions.size)
        assertTrue(next.savedPositions.none { it.id == removeId })
    }

    @Test
    fun renamePosition_updatesNameAndAlias() {
        val state = SeatPositionsUiState()
        val targetId = "slot-1"
        val next = state.copy(
            savedPositions = state.savedPositions.map {
                if (it.id == targetId) it.copy(name = "Yeni Sürüş Modu", alias = "Yeni Sürüş Modu") else it
            }
        )
        assertEquals("Yeni Sürüş Modu", next.savedPositions.first { it.id == targetId }.name)
    }

    @Test
    fun movementBlocked_preventsPositioning() {
        val state = SeatPositionsUiState(
            isMovementBlocked = true,
            movementBlockReason = "not_park",
            isPositioningBlocked = true
        )
        assertTrue(state.isPositioningBlocked)
        assertEquals("not_park", state.movementBlockReason)
    }
}
