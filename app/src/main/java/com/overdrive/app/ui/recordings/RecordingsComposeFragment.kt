package com.overdrive.app.ui.recordings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.overdrive.app.R
import com.overdrive.app.ui.component.OverdriveComposeContainer
import com.overdrive.app.ui.fragment.VideoPlayerFragment
import com.overdrive.app.ui.theme.OverdriveTheme

/**
 * 100% Jetpack Compose Native Fragment for OverDrive Recordings Library & Player.
 * Directly replaces legacy XML RecordingsFragment / RecordingLibraryFragment for /recordings.
 */
class RecordingsComposeFragment : Fragment() {

    private val viewModel: RecordingsViewModel by viewModels()

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
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OverdriveTheme {
                    RecordingsScreen(
                        viewModel = viewModel,
                        onFullscreen = { recording ->
                            val bundle = Bundle().apply {
                                putString(VideoPlayerFragment.ARG_VIDEO_PATH, recording.playbackSource)
                                putString(VideoPlayerFragment.ARG_VIDEO_TITLE, recording.name)
                                putBoolean(VideoPlayerFragment.ARG_INLINE, false)
                            }
                            try {
                                findNavController().navigate(R.id.action_global_videoPlayer, bundle)
                            } catch (e: Throwable) {
                                android.util.Log.e("RecordingsCompose", "Fullscreen nav failed", e)
                            }
                        }
                    )
                }
            }
        }
    }
}
