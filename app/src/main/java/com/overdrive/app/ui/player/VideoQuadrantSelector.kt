package com.overdrive.app.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.overdrive.app.R
import com.overdrive.app.ui.theme.OverdriveTheme
import com.overdrive.app.ui.view.ZoomableVideoView

/**
 * 2x2 grid position selector pill for AVM video playback.
 * Matches the classic DiLink/Overdrive grid selection UI:
 * [ALL (4 tiles)] [Top-Left] [Top-Right] [Bottom-Left] [Bottom-Right]
 *
 * Tapping an active quadrant toggles back to ALL.
 */
@Composable
fun VideoQuadrantSelector(
    selectedQuadrant: ZoomableVideoView.Quadrant,
    onQuadrantSelected: (ZoomableVideoView.Quadrant) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xCC000000), // 80% black, matching bg_quadrant_bar.xml
        border = BorderStroke(1.dp, Color(0x33FFFFFF)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                Triple(ZoomableVideoView.Quadrant.ALL, R.drawable.ic_quadrant_all, R.string.cd_player_quadrant_all),
                Triple(ZoomableVideoView.Quadrant.FRONT, R.drawable.ic_quadrant_front, R.string.cd_player_quadrant_front),
                Triple(ZoomableVideoView.Quadrant.RIGHT, R.drawable.ic_quadrant_right, R.string.cd_player_quadrant_right),
                Triple(ZoomableVideoView.Quadrant.REAR, R.drawable.ic_quadrant_rear, R.string.cd_player_quadrant_rear),
                Triple(ZoomableVideoView.Quadrant.LEFT, R.drawable.ic_quadrant_left, R.string.cd_player_quadrant_left),
            )

            items.forEach { (quad, iconRes, cdRes) ->
                val isSelected = selectedQuadrant == quad
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            color = if (isSelected) OverdriveTheme.colors.primary.copy(alpha = 0.35f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    width = 1.5.dp,
                                    color = OverdriveTheme.colors.primary,
                                    shape = RoundedCornerShape(8.dp)
                                )
                            } else Modifier
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                val next = if (selectedQuadrant == quad && quad != ZoomableVideoView.Quadrant.ALL) {
                                    ZoomableVideoView.Quadrant.ALL
                                } else {
                                    quad
                                }
                                onQuadrantSelected(next)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = stringResource(id = cdRes),
                        tint = Color.Unspecified, // CRITICAL: preserve vector's lit vs dim cells!
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
