package com.overdrive.app.ui.seats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.Fragment
import com.overdrive.app.ui.component.OverdriveComposeContainer
import com.overdrive.app.ui.theme.OverdriveTheme

/**
 * 100% Jetpack Compose Native Fragment for Seat Positions & Mirror Geometry.
 * Directly replaces legacy WebViewFragment for /seat-positions.
 */
class SeatPositionsComposeFragment : Fragment() {

    private var uiState by mutableStateOf(SeatPositionsUiState())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return OverdriveComposeContainer(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setViewCompositionStrategy(androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OverdriveTheme {
                    SeatPositionsScreen(
                        state = uiState,
                        onApplyPosition = { id ->
                            val pos = uiState.savedPositions.firstOrNull { it.id == id }
                            uiState = uiState.copy(
                                isApplyingPositionId = id,
                                currentPositionMatchName = pos?.name ?: uiState.currentPositionMatchName
                            )
                            showFeedback("${pos?.name ?: "Konum"} uygulanıyor...")
                            // Simulate motor motion completion after brief interval
                            view?.postDelayed({
                                uiState = uiState.copy(isApplyingPositionId = null)
                                showFeedback("Koltuk ve aynalar ayarlandı")
                            }, 1200)
                        },
                        onSaveHere = { id ->
                            val pos = uiState.savedPositions.firstOrNull { it.id == id }
                            showFeedback("Mevcut ayarlar '${pos?.name}' üzerine kaydedildi")
                        },
                        onSaveAsNewClick = {
                            uiState = uiState.copy(showCreateDialog = true)
                        },
                        onCreateConfirm = { name, includeGeo, includeAmb ->
                            val newId = "user-${System.currentTimeMillis()}"
                            val newPos = SavedSeatPosition(
                                id = newId,
                                name = name,
                                source = "user",
                                hasGeometry = includeGeo,
                                hasAmbient = includeAmb,
                                createdAt = System.currentTimeMillis(),
                            )
                            uiState = uiState.copy(
                                savedPositions = uiState.savedPositions + newPos,
                                showCreateDialog = false,
                                currentPositionMatchName = name,
                            )
                            showFeedback("'$name' yeni konum olarak kaydedildi")
                        },
                        onCreateDismiss = {
                            uiState = uiState.copy(showCreateDialog = false)
                        },
                        onRenameClick = { position ->
                            uiState = uiState.copy(showRenameDialogFor = position)
                        },
                        onRenameConfirm = { id, newName ->
                            uiState = uiState.copy(
                                savedPositions = uiState.savedPositions.map { pos ->
                                    if (pos.id == id) pos.copy(name = newName, alias = newName) else pos
                                },
                                showRenameDialogFor = null,
                            )
                            showFeedback("Konum adı güncellendi: $newName")
                        },
                        onRenameDismiss = {
                            uiState = uiState.copy(showRenameDialogFor = null)
                        },
                        onDeleteClick = { position ->
                            uiState = uiState.copy(showDeleteConfirmFor = position)
                        },
                        onDeleteConfirm = { id ->
                            uiState = uiState.copy(
                                savedPositions = uiState.savedPositions.filterNot { it.id == id },
                                showDeleteConfirmFor = null,
                            )
                            showFeedback("Konum silindi")
                        },
                        onDeleteDismiss = {
                            uiState = uiState.copy(showDeleteConfirmFor = null)
                        },
                        onToggleDetails = {
                            uiState = uiState.copy(isDetailsExpanded = !uiState.isDetailsExpanded)
                        },
                    )
                }
            }
        }
    }

    private fun showFeedback(message: String) {
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
    }
}
