package com.overdrive.app.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.overdrive.app.ui.component.OverdriveComposeContainer
import com.overdrive.app.ui.player.VideoPlayerScreen
import com.overdrive.app.ui.theme.OverdriveTheme

/**
 * 100% Jetpack Compose Native video player with event timeline overlay & 4-quadrant mosaic zoom.
 * Built with [VideoPlayerScreen] on top of hardware-accelerated video decoding.
 */
class VideoPlayerFragment : Fragment() {

    companion object {
        const val ARG_VIDEO_PATH = "video_path"
        const val ARG_VIDEO_TITLE = "video_title"
        const val ARG_INLINE = "inline"
        const val ARG_PLAYLIST_PATHS = "playlist_paths"
        const val ARG_PLAYLIST_TITLES = "playlist_titles"
        const val ARG_PLAYLIST_INDEX = "playlist_index"
    }

    var onFullscreenToggle: ((Boolean) -> Unit)? = null
    var isFullscreen: Boolean = false
    var onPlaylistItemChanged: ((String) -> Unit)? = null
    var onDurationResolved: ((String, Long) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val currentPath = arguments?.getString(ARG_VIDEO_PATH) ?: ""
        val currentTitle = arguments?.getString(ARG_VIDEO_TITLE) ?: ""
        val playlistPaths = arguments?.getStringArray(ARG_PLAYLIST_PATHS) ?: emptyArray()
        val playlistTitles = arguments?.getStringArray(ARG_PLAYLIST_TITLES) ?: emptyArray()
        val playlistIndex = arguments?.getInt(ARG_PLAYLIST_INDEX, 0) ?: 0
        val inlineMode = arguments?.getBoolean(ARG_INLINE, false) ?: false

        return OverdriveComposeContainer(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OverdriveTheme {
                    VideoPlayerScreen(
                        videoPath = currentPath,
                        videoTitle = currentTitle,
                        playlistPaths = playlistPaths.toList(),
                        playlistTitles = playlistTitles.toList(),
                        initialPlaylistIndex = playlistIndex.coerceAtLeast(0),
                        onBack = {
                            if (!inlineMode) {
                                findNavController().popBackStack()
                            }
                        }
                    )
                }
            }
        }
    }
}
