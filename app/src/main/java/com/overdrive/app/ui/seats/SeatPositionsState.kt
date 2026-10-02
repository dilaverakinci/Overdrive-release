package com.overdrive.app.ui.seats

/**
 * Axis data for seat and mirrors geometry.
 */
data class SeatAxesState(
    val horizontal: Float? = null,
    val backrest: Float? = null,
    val height: Float? = null,
    val sitpoint: Float? = null,
    val legholder: Float? = null,
    val headrestH: Float? = null,
    val headrestV: Float? = null,
    val leftMirrorH: Float? = null,
    val leftMirrorV: Float? = null,
    val rightMirrorH: Float? = null,
    val rightMirrorV: Float? = null,
    val steeringH: Float? = null,
    val steeringV: Float? = null,
)

/**
 * Represents ambient lighting configuration saved in a profile.
 */
data class AmbientProfileState(
    val frontColour: Int = 1,
    val frontBrightness: Int = 5,
    val rearColour: Int = 1,
    val rearBrightness: Int = 5,
    val musicMode: Boolean = false,
)

/**
 * Single saved seat position entry (captured from native slot or user created).
 */
data class SavedSeatPosition(
    val id: String,
    val name: String,
    val alias: String? = null,
    val slot: Int? = null, // 1..3 for DiLink native slots, null for custom user slots
    val source: String = "user", // "captured" or "user"
    val hasGeometry: Boolean = true,
    val hasAmbient: Boolean = false,
    val axes: SeatAxesState? = null,
    val ambient: AmbientProfileState? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDependedByAutomation: Boolean = false,
)

/**
 * Complete UI state for SeatPositionsScreen.
 */
data class SeatPositionsUiState(
    val isCarAccOn: Boolean = true,
    val isMovementBlocked: Boolean = false,
    val movementBlockReason: String? = null,
    val isPositioningBlocked: Boolean = false,
    val currentPositionMatchName: String? = null,
    val currentAxes: SeatAxesState = SeatAxesState(
        horizontal = 52f,
        backrest = 56f,
        height = 48f,
        sitpoint = 50f,
        leftMirrorH = 32f,
        leftMirrorV = 45f,
        rightMirrorH = 35f,
        rightMirrorV = 42f,
    ),
    val currentAmbient: AmbientProfileState? = null,
    val savedPositions: List<SavedSeatPosition> = emptyList(),
    val isApplyingPositionId: String? = null,
    val isDetailsExpanded: Boolean = false,
    val showCreateDialog: Boolean = false,
    val showRenameDialogFor: SavedSeatPosition? = null,
    val showDeleteConfirmFor: SavedSeatPosition? = null,
)
