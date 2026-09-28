package com.overdrive.app.ui.recordings

import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.overdrive.app.R
import com.overdrive.app.ui.component.OverdriveDialog
import com.overdrive.app.ui.model.RecordingFile
import com.overdrive.app.ui.theme.LocalOverdriveColors
import com.overdrive.app.ui.view.ZoomableVideoView
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun RecordingsScreen(
    viewModel: RecordingsViewModel,
    onFullscreen: (RecordingFile) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalOverdriveColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // 1. Header Bar: Title, Storage Subtitle & Top Actions
        RecordingsHeader(
            summaryText = uiState.stats.formattedSummary,
            isSelectionMode = uiState.isSelectionMode,
            onToggleSelectionMode = { viewModel.toggleSelectionMode() },
            onRefresh = { viewModel.loadRecordings() }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Segmented Tabs: Tümü · Dashcam · Tekrarlar · Gözetim
        RecordingsSegmentedTabs(
            selectedTab = uiState.selectedTab,
            stats = uiState.stats,
            onTabSelected = { viewModel.selectTab(it) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Filter Bar: Search Box + Filter Chips (Date, Actor, Storage)
        RecordingsFilterBar(
            searchQuery = uiState.searchQuery,
            onSearchQueryChange = { viewModel.setSearchQuery(it) },
            selectedDate = uiState.selectedDateFilter,
            onDateSelected = { viewModel.setDateFilter(it) },
            selectedActor = uiState.selectedActorFilter,
            onActorSelected = { viewModel.setActorFilter(it) },
            selectedStorage = uiState.selectedStorageFilter,
            onStorageSelected = { viewModel.setStorageFilter(it) },
            onClearFilters = { viewModel.clearFilters() }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Two-Pane Content Area (Left: Library, Right: Player/Telemetry)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Pane: Library & List
            Box(
                modifier = Modifier
                    .weight(1.05f)
                    .fillMaxHeight()
            ) {
                RecordingsLibraryPane(
                    isLoading = uiState.isLoading,
                    isSelectionMode = uiState.isSelectionMode,
                    groupedRecordings = uiState.groupedRecordings,
                    selectedRecording = uiState.selectedRecording,
                    selectedClipIds = uiState.selectedClipIds,
                    onSelectRecording = { viewModel.selectRecording(it) },
                    onToggleClipSelection = { viewModel.toggleClipSelection(it) },
                    onSelectAll = { viewModel.selectAllClips() },
                    onClearSelection = { viewModel.clearClipSelection() },
                    onPromptBatchDelete = { viewModel.promptBatchDelete() },
                    onLoadThumbnail = { viewModel.getThumbnail(it) },
                    onClearFilters = { viewModel.clearFilters() }
                )
            }

            // Right Pane: Player Stage & Telemetry Inspector
            Box(
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight()
            ) {
                RecordingsInspectorPane(
                    selectedRecording = uiState.selectedRecording,
                    stats = uiState.stats,
                    selectedQuadrant = uiState.selectedQuadrant,
                    onQuadrantSelected = { viewModel.setQuadrant(it) },
                    onFullscreen = onFullscreen,
                    onDeleteClip = { viewModel.promptDeleteClip(it) }
                )
            }
        }
    }

    // Single Clip Delete Confirmation Dialog
    if (uiState.showDeleteConfirmDialog && uiState.clipToDelete != null) {
        val target = uiState.clipToDelete!!
        OverdriveDialog(
            onDismissRequest = { viewModel.dismissDeleteClip() },
            title = stringResource(R.string.recordings_delete_clip_confirm_title),
            positiveButtonText = stringResource(R.string.action_delete),
            onPositiveClick = { viewModel.executeDeleteClip() },
            negativeButtonText = stringResource(R.string.action_cancel),
            onNegativeClick = { viewModel.dismissDeleteClip() }
        ) {
            Text(
                text = stringResource(R.string.recordings_delete_clip_confirm_msg, target.name),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface
            )
        }
    }

    // Batch Delete Confirmation Dialog
    if (uiState.showBatchDeleteConfirmDialog) {
        val count = uiState.selectedClipIds.size
        OverdriveDialog(
            onDismissRequest = { viewModel.dismissBatchDelete() },
            title = stringResource(R.string.recordings_batch_delete_dialog_title),
            positiveButtonText = stringResource(R.string.action_delete),
            onPositiveClick = { viewModel.executeBatchDelete() },
            negativeButtonText = stringResource(R.string.action_cancel),
            onNegativeClick = { viewModel.dismissBatchDelete() }
        ) {
            Text(
                text = stringResource(R.string.recordings_batch_delete_dialog_msg, count),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface
            )
        }
    }
}

@Composable
private fun RecordingsHeader(
    summaryText: String,
    isSelectionMode: Boolean,
    onToggleSelectionMode: () -> Unit,
    onRefresh: () -> Unit
) {
    val colors = LocalOverdriveColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.VideoLibrary,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.recordings_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = colors.onBackground
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Select Mode Toggle Button
            Surface(
                onClick = onToggleSelectionMode,
                shape = RoundedCornerShape(20.dp),
                color = if (isSelectionMode) colors.primaryContainer else colors.surfaceContainer,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelectionMode) colors.primary else colors.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSelectionMode) Icons.Filled.Close else Icons.Filled.Check,
                        contentDescription = null,
                        tint = if (isSelectionMode) colors.onPrimaryContainer else colors.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSelectionMode) {
                            stringResource(R.string.recordings_cancel_select_btn)
                        } else {
                            stringResource(R.string.recordings_select_mode_btn)
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isSelectionMode) colors.onPrimaryContainer else colors.onSurface
                    )
                }
            }

            // Refresh Button
            Surface(
                onClick = onRefresh,
                shape = CircleShape,
                color = colors.surfaceContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant)
            ) {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.recordings_refresh_btn),
                        tint = colors.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingsSegmentedTabs(
    selectedTab: RecordingTab,
    stats: StorageStats,
    onTabSelected: (RecordingTab) -> Unit
) {
    val colors = LocalOverdriveColors.current

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf(
                RecordingTab.ALL to stringResource(R.string.recordings_segment_all_count, stats.totalCount),
                RecordingTab.DASHCAM to stringResource(R.string.recordings_segment_dashcam_count, stats.dashcamCount),
                RecordingTab.REPLAYS to stringResource(R.string.recordings_segment_replays_count, stats.replaysCount),
                RecordingTab.SURVEILLANCE to stringResource(R.string.recordings_segment_surveillance_count, stats.surveillanceCount)
            )

            for ((tab, label) in tabs) {
                val isSelected = selectedTab == tab
                Surface(
                    onClick = { onTabSelected(tab) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) colors.primary else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) colors.onPrimary else colors.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingsFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedDate: DateFilter,
    onDateSelected: (DateFilter) -> Unit,
    selectedActor: ActorFilter,
    onActorSelected: (ActorFilter) -> Unit,
    selectedStorage: StorageFilter,
    onStorageSelected: (StorageFilter) -> Unit,
    onClearFilters: () -> Unit
) {
    val colors = LocalOverdriveColors.current
    val hasActiveFilter = selectedDate != DateFilter.ALL ||
            selectedActor != ActorFilter.ALL ||
            selectedStorage != StorageFilter.ALL ||
            searchQuery.isNotEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search Input Field
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.surfaceContainer,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant),
            modifier = Modifier.width(220.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.recording_lib_place_search_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = colors.primary
                    ),
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = colors.onSurface)
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Date Filter Chips
        FilterChipPill(
            label = stringResource(R.string.recordings_filter_all),
            isSelected = selectedDate == DateFilter.ALL,
            onClick = { onDateSelected(DateFilter.ALL) }
        )
        FilterChipPill(
            label = stringResource(R.string.recordings_filter_today),
            isSelected = selectedDate == DateFilter.TODAY,
            onClick = { onDateSelected(DateFilter.TODAY) }
        )
        FilterChipPill(
            label = stringResource(R.string.recordings_filter_yesterday),
            isSelected = selectedDate == DateFilter.YESTERDAY,
            onClick = { onDateSelected(DateFilter.YESTERDAY) }
        )

        // Actor Chips
        FilterChipPill(
            label = stringResource(R.string.recordings_filter_person),
            isSelected = selectedActor == ActorFilter.PERSON,
            onClick = { onActorSelected(if (selectedActor == ActorFilter.PERSON) ActorFilter.ALL else ActorFilter.PERSON) }
        )
        FilterChipPill(
            label = stringResource(R.string.recordings_filter_vehicle),
            isSelected = selectedActor == ActorFilter.VEHICLE,
            onClick = { onActorSelected(if (selectedActor == ActorFilter.VEHICLE) ActorFilter.ALL else ActorFilter.VEHICLE) }
        )

        // Storage Chips
        FilterChipPill(
            label = stringResource(R.string.recordings_filter_internal),
            isSelected = selectedStorage == StorageFilter.INTERNAL,
            onClick = { onStorageSelected(if (selectedStorage == StorageFilter.INTERNAL) StorageFilter.ALL else StorageFilter.INTERNAL) }
        )
        FilterChipPill(
            label = stringResource(R.string.recordings_filter_sd),
            isSelected = selectedStorage == StorageFilter.SD_CARD,
            onClick = { onStorageSelected(if (selectedStorage == StorageFilter.SD_CARD) StorageFilter.ALL else StorageFilter.SD_CARD) }
        )

        // Clear Filters button
        if (hasActiveFilter) {
            Surface(
                onClick = onClearFilters,
                shape = RoundedCornerShape(12.dp),
                color = colors.errorContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.error)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = colors.onErrorContainer,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.recordings_filter_clear),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalOverdriveColors.current

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) colors.primaryContainer else colors.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) colors.primary else colors.outlineVariant
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isSelected) colors.onPrimaryContainer else colors.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun RecordingsLibraryPane(
    isLoading: Boolean,
    isSelectionMode: Boolean,
    groupedRecordings: Map<String, List<RecordingFile>>,
    selectedRecording: RecordingFile?,
    selectedClipIds: Set<String>,
    onSelectRecording: (RecordingFile) -> Unit,
    onToggleClipSelection: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onPromptBatchDelete: () -> Unit,
    onLoadThumbnail: suspend (RecordingFile) -> Bitmap?,
    onClearFilters: () -> Unit
) {
    val colors = LocalOverdriveColors.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Multi-Select Action Bar (Visible when Selection Mode is on)
            AnimatedVisibility(visible = isSelectionMode) {
                Surface(
                    color = colors.surfaceContainerHigh,
                    border = androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.recording_lib_selected_count, selectedClipIds.size),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                onClick = onSelectAll,
                                shape = RoundedCornerShape(8.dp),
                                color = colors.surfaceContainerHighest
                            ) {
                                Text(
                                    text = stringResource(R.string.recordings_select_all_btn),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            if (selectedClipIds.isNotEmpty()) {
                                Surface(
                                    onClick = onPromptBatchDelete,
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.errorContainer
                                ) {
                                    Text(
                                        text = stringResource(R.string.recordings_delete_selected_btn, selectedClipIds.size),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colors.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Body: Loading, Empty, or Grouped LazyColumn
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = colors.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = stringResource(R.string.recording_library_loading),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                }

                groupedRecordings.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colors.surfaceContainer,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Folder,
                                        contentDescription = null,
                                        tint = colors.onSurfaceVariant,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.recording_lib_empty_disk_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.onSurface
                            )
                            Text(
                                text = stringResource(R.string.recording_lib_empty_disk_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                onClick = onClearFilters,
                                shape = RoundedCornerShape(12.dp),
                                color = colors.primaryContainer
                            ) {
                                Text(
                                    text = stringResource(R.string.recording_lib_empty_clear_filters),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colors.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for ((bucketLabel, clips) in groupedRecordings) {
                            item(key = "header_$bucketLabel") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = bucketLabel,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = colors.primary
                                    )
                                    Text(
                                        text = "${clips.size} klip",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                            }

                            items(clips, key = { it.recordingId ?: it.path }) { clip ->
                                val isSelectedForPlay = selectedRecording?.let {
                                    (it.recordingId ?: it.path) == (clip.recordingId ?: clip.path)
                                } ?: false
                                val isChecked = (clip.recordingId ?: clip.path) in selectedClipIds

                                RecordingItemCard(
                                    recording = clip,
                                    isSelectedForPlay = isSelectedForPlay,
                                    isSelectionMode = isSelectionMode,
                                    isChecked = isChecked,
                                    onClick = {
                                        if (isSelectionMode) {
                                            onToggleClipSelection(clip.recordingId ?: clip.path)
                                        } else {
                                            onSelectRecording(clip)
                                        }
                                    },
                                    onCheckChange = {
                                        onToggleClipSelection(clip.recordingId ?: clip.path)
                                    },
                                    onLoadThumbnail = onLoadThumbnail
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingItemCard(
    recording: RecordingFile,
    isSelectedForPlay: Boolean,
    isSelectionMode: Boolean,
    isChecked: Boolean,
    onClick: () -> Unit,
    onCheckChange: () -> Unit,
    onLoadThumbnail: suspend (RecordingFile) -> Bitmap?
) {
    val colors = LocalOverdriveColors.current

    val thumbnailBitmap by produceState<Bitmap?>(initialValue = null, key1 = recording) {
        value = onLoadThumbnail(recording)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelectedForPlay) colors.surfaceContainerHigh else colors.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelectedForPlay) 2.dp else 1.dp,
            if (isSelectedForPlay) colors.primary else colors.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox in selection mode
            if (isSelectionMode) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { onCheckChange() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = colors.primary,
                        uncheckedColor = colors.outline
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Thumbnail Preview Container
            Box(
                modifier = Modifier
                    .size(width = 110.dp, height = 66.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceContainerLowest)
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap!!.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Videocam,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Camera Badge (Top-Left)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                ) {
                    Text(
                        text = when (recording.cameraId) {
                            1 -> "ÖN"
                            2 -> "SAĞ"
                            3 -> "ARKA"
                            4 -> "SOL"
                            5 -> "DVR"
                            else -> "360°"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                // Duration Badge (Bottom-Right)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        text = recording.formattedDuration,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Metadata Info Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // Row 1: Time + Type Tag + Storage Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = recording.formattedTime,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.onSurface
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Type Tag
                        val (typeText, typeBg, typeFg) = when (recording.type) {
                            RecordingFile.RecordingType.NORMAL -> Triple("CAM", colors.primaryContainer, colors.onPrimaryContainer)
                            RecordingFile.RecordingType.SENTRY -> Triple("GÖZETİM", colors.errorContainer, colors.onErrorContainer)
                            RecordingFile.RecordingType.PROXIMITY -> Triple("PROX", colors.tertiaryContainer, colors.onTertiaryContainer)
                            RecordingFile.RecordingType.OEM_DASHCAM -> Triple("OEM", colors.secondaryContainer, colors.onSecondaryContainer)
                            RecordingFile.RecordingType.REPLAY -> Triple("TEKRAR", colors.primaryContainer, colors.onPrimaryContainer)
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = typeBg
                        ) {
                            Text(
                                text = typeText,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = typeFg,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }

                        // Storage Badge
                        val storageLabel = when (recording.storageType?.uppercase(Locale.ROOT)) {
                            "SD_CARD" -> "SD"
                            "USB" -> "USB"
                            else -> "INT"
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = colors.surfaceContainerHighest
                        ) {
                            Text(
                                text = storageLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                // Row 2: File size & Location / detected actors
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = recording.formattedSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )

                    recording.placeShortLabel?.takeIf { it.isNotBlank() }?.let { place ->
                        Text(
                            text = "• $place",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Row 3: Severity / Actor badges if present
                if (recording.peakSeverity.equals("ALERT", true) || recording.peakSeverity.equals("CRITICAL", true)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = colors.error,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (recording.peakSeverity.equals("CRITICAL", true)) "Kritik Olay" else "Uyarı",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingsInspectorPane(
    selectedRecording: RecordingFile?,
    stats: StorageStats,
    selectedQuadrant: ZoomableVideoView.Quadrant,
    onQuadrantSelected: (ZoomableVideoView.Quadrant) -> Unit,
    onFullscreen: (RecordingFile) -> Unit,
    onDeleteClip: (RecordingFile) -> Unit
) {
    val colors = LocalOverdriveColors.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxSize()
    ) {
        if (selectedRecording == null) {
            // Placeholder Stage when no clip is selected
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colors.surfaceContainer,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.recordings_preview_placeholder_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.onSurface
                    )
                    Text(
                        text = stringResource(R.string.recordings_preview_placeholder_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Storage Overview Card
                    StorageOverviewCard(stats = stats)
                }
            }
        } else {
            // Selected Recording: Video Player + Telemetry Inspector
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Interactive Video Player Stage
                VideoPlayerStage(
                    recording = selectedRecording,
                    selectedQuadrant = selectedQuadrant,
                    onQuadrantSelected = onQuadrantSelected,
                    onFullscreen = { onFullscreen(selectedRecording) }
                )

                // Telemetry & Inspector Details Card
                RecordingTelemetryCard(
                    recording = selectedRecording,
                    onFullscreen = { onFullscreen(selectedRecording) },
                    onDelete = { onDeleteClip(selectedRecording) }
                )
            }
        }
    }
}

@Composable
private fun VideoPlayerStage(
    recording: RecordingFile,
    selectedQuadrant: ZoomableVideoView.Quadrant,
    onQuadrantSelected: (ZoomableVideoView.Quadrant) -> Unit,
    onFullscreen: () -> Unit
) {
    val colors = LocalOverdriveColors.current
    var videoViewRef by remember { mutableStateOf<ZoomableVideoView?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isMuted by remember { mutableStateOf(false) }

    DisposableEffect(recording) {
        onDispose {
            videoViewRef?.stopPlayback()
        }
    }

    // Polling ticker for playhead progress
    LaunchedEffect(recording, isPlaying) {
        while (isPlaying) {
            videoViewRef?.let { vv ->
                currentPosMs = vv.currentPosition.toLong()
                durationMs = vv.duration.toLong().coerceAtLeast(recording.durationMs)
            }
            delay(250)
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.Black,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // TextureView Video Player Engine
            AndroidView(
                factory = { ctx ->
                    ZoomableVideoView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            start()
                            isPlaying = true
                            durationMs = duration.toLong()
                        }
                        videoViewRef = this
                    }
                },
                update = { view ->
                    videoViewRef = view
                    val uri = when {
                        recording.videoUrl != null -> Uri.parse(recording.videoUrl)
                        recording.contentUri != null -> recording.contentUri
                        else -> Uri.fromFile(recording.file)
                    }
                    view.setVideoURI(uri)
                    view.setQuadrant(selectedQuadrant, true)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Quadrant Switching Bar (Top Overlay)
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val quadrants = listOf(
                            ZoomableVideoView.Quadrant.ALL to stringResource(R.string.recordings_camera_view_all),
                            ZoomableVideoView.Quadrant.FRONT to stringResource(R.string.recordings_camera_view_front),
                            ZoomableVideoView.Quadrant.REAR to stringResource(R.string.recordings_camera_view_rear),
                            ZoomableVideoView.Quadrant.LEFT to stringResource(R.string.recordings_camera_view_left),
                            ZoomableVideoView.Quadrant.RIGHT to stringResource(R.string.recordings_camera_view_right)
                        )
                        for ((quadrant, label) in quadrants) {
                            val active = selectedQuadrant == quadrant
                            Surface(
                                onClick = {
                                    onQuadrantSelected(quadrant)
                                    videoViewRef?.setQuadrant(quadrant, true)
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = if (active) colors.primary else Color.Transparent
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (active) colors.onPrimary else Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Transport Control Overlay (Bottom Overlay)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                // Seekbar slider
                val maxDuration = if (durationMs > 0) durationMs.toFloat() else 1f
                Slider(
                    value = currentPosMs.toFloat().coerceIn(0f, maxDuration),
                    onValueChange = { newPos ->
                        currentPosMs = newPos.toLong()
                        videoViewRef?.seekTo(newPos.toInt())
                    },
                    valueRange = 0f..maxDuration,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.primary,
                        activeTrackColor = colors.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                )

                // Controls row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play/Pause button
                        IconButton(
                            onClick = {
                                videoViewRef?.let { vv ->
                                    if (isPlaying) {
                                        vv.pause()
                                        isPlaying = false
                                    } else {
                                        vv.start()
                                        isPlaying = true
                                    }
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }

                        // Time label
                        Text(
                            text = "${formatTime(currentPosMs)} / ${formatTime(durationMs.coerceAtLeast(recording.durationMs))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Fullscreen button
                        IconButton(
                            onClick = onFullscreen,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Fullscreen,
                                contentDescription = stringResource(R.string.recordings_fullscreen_btn),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingTelemetryCard(
    recording: RecordingFile,
    onFullscreen: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalOverdriveColors.current

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Title & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recording.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${recording.formattedDate} • ${recording.formattedTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Fullscreen Button
                    Surface(
                        onClick = onFullscreen,
                        shape = RoundedCornerShape(8.dp),
                        color = colors.surfaceContainerHighest
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Fullscreen,
                                contentDescription = null,
                                tint = colors.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.recordings_fullscreen_btn),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.onSurface
                            )
                        }
                    }

                    // Delete Button
                    Surface(
                        onClick = onDelete,
                        shape = RoundedCornerShape(8.dp),
                        color = colors.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                tint = colors.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.action_delete),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Telemetry Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryStatItem(
                    label = stringResource(R.string.recording_preview_duration),
                    value = recording.formattedDuration,
                    modifier = Modifier.weight(1f)
                )
                TelemetryStatItem(
                    label = stringResource(R.string.recording_preview_file_size),
                    value = recording.formattedSize,
                    modifier = Modifier.weight(1f)
                )
                TelemetryStatItem(
                    label = stringResource(R.string.recording_preview_storage),
                    value = when (recording.storageType?.uppercase(Locale.ROOT)) {
                        "SD_CARD" -> stringResource(R.string.recording_preview_storage_sd)
                        "USB" -> stringResource(R.string.recording_preview_storage_usb)
                        else -> stringResource(R.string.recording_preview_storage_internal)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryStatItem(
                    label = stringResource(R.string.recordings_location_label),
                    value = recording.placeDisplayName ?: recording.placeShortLabel ?: "Bilinmiyor",
                    modifier = Modifier.weight(1f)
                )
                TelemetryStatItem(
                    label = stringResource(R.string.recordings_detected_label),
                    value = if (recording.personCount > 0 || recording.vehicleCount > 0) {
                        "Kişi: ${recording.personCount} • Araç: ${recording.vehicleCount}"
                    } else {
                        stringResource(R.string.recording_preview_no_detection)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TelemetryStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalOverdriveColors.current

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = colors.surfaceContainerHighest,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StorageOverviewCard(stats: StorageStats) {
    val colors = LocalOverdriveColors.current

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Storage,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.recordings_storage_overview),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.onSurface
                )
            }

            // Internal Storage bar
            if (stats.internalTotalBytes > 0) {
                val internalFraction = (stats.internalUsedBytes.toFloat() / stats.internalTotalBytes.toFloat()).coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.recordings_internal_storage),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.onSurface
                        )
                        Text(
                            text = "${formatBytes(stats.internalUsedBytes)} / ${formatBytes(stats.internalTotalBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = { internalFraction },
                        color = colors.primary,
                        trackColor = colors.surfaceContainerHighest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }

            // SD Storage bar
            if (stats.sdTotalBytes > 0) {
                val sdFraction = (stats.sdUsedBytes.toFloat() / stats.sdTotalBytes.toFloat()).coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.recordings_sd_storage),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.onSurface
                        )
                        Text(
                            text = "${formatBytes(stats.sdUsedBytes)} / ${formatBytes(stats.sdTotalBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = { sdFraction },
                        color = colors.tertiary,
                        trackColor = colors.surfaceContainerHighest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000L -> String.format(Locale.getDefault(), "%.1f GB", bytes / 1_000_000_000.0)
        bytes >= 1_000_000L -> String.format(Locale.getDefault(), "%.1f MB", bytes / 1_000_000.0)
        bytes >= 1_000L -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1_000.0)
        else -> "$bytes B"
    }
}
