package com.overdrive.app.ui.recordings

import com.overdrive.app.ui.model.RecordingFile
import com.overdrive.app.ui.view.ZoomableVideoView

enum class RecordingTab {
    ALL,
    DASHCAM,
    REPLAYS,
    SURVEILLANCE
}

enum class ActorFilter {
    ALL,
    PERSON,
    VEHICLE,
    BIKE,
    ANIMAL
}

enum class SeverityFilter {
    ALL,
    ALERT,
    CRITICAL
}

enum class StorageFilter {
    ALL,
    INTERNAL,
    SD_CARD,
    USB
}

enum class DateFilter {
    ALL,
    TODAY,
    YESTERDAY
}

data class StorageStats(
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val totalCount: Int = 0,
    val todayCount: Int = 0,
    val dashcamCount: Int = 0,
    val replaysCount: Int = 0,
    val surveillanceCount: Int = 0,
    val internalTotalBytes: Long = 0L,
    val internalUsedBytes: Long = 0L,
    val sdTotalBytes: Long = 0L,
    val sdUsedBytes: Long = 0L,
    val formattedSummary: String = "0 bugün · 0 toplam · 0 B"
)

data class RecordingsUiState(
    val isLoading: Boolean = true,
    val isIndexDown: Boolean = false,
    val allRecordings: List<RecordingFile> = emptyList(),
    val displayedRecordings: List<RecordingFile> = emptyList(),
    val groupedRecordings: Map<String, List<RecordingFile>> = emptyMap(),
    val selectedTab: RecordingTab = RecordingTab.ALL,
    val selectedActorFilter: ActorFilter = ActorFilter.ALL,
    val selectedSeverityFilter: SeverityFilter = SeverityFilter.ALL,
    val selectedStorageFilter: StorageFilter = StorageFilter.ALL,
    val selectedDateFilter: DateFilter = DateFilter.ALL,
    val searchQuery: String = "",
    val selectedRecording: RecordingFile? = null,
    val isSelectionMode: Boolean = false,
    val selectedClipIds: Set<String> = emptySet(),
    val stats: StorageStats = StorageStats(),
    val isPlaying: Boolean = false,
    val playbackPositionMs: Long = 0L,
    val playbackDurationMs: Long = 0L,
    val selectedQuadrant: ZoomableVideoView.Quadrant = ZoomableVideoView.Quadrant.ALL,
    val isMuted: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val clipToDelete: RecordingFile? = null,
    val showBatchDeleteConfirmDialog: Boolean = false,
    val actionMessage: String? = null
)
