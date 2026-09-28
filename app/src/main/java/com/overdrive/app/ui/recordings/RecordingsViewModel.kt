package com.overdrive.app.ui.recordings

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.os.StatFs
import android.util.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.overdrive.app.R
import com.overdrive.app.ui.model.RecordingFile
import com.overdrive.app.ui.util.RecordingScanner
import com.overdrive.app.ui.util.RecordingsApiClient
import com.overdrive.app.ui.view.ZoomableVideoView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RecordingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(RecordingsUiState())
    val uiState: StateFlow<RecordingsUiState> = _uiState.asStateFlow()

    // Thumbnail memory cache (bounded to 16 MB)
    private val thumbnailCache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    init {
        loadRecordings()
    }

    fun loadRecordings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val app = getApplication<Application>()
            
            withContext(Dispatchers.IO) {
                val scanned = RecordingScanner.scanRecordings(app)
                val statsPayload = RecordingsApiClient.fetchStats()
                
                // Read physical storage space
                val (internalTotal, internalUsed) = getInternalStorageSpace()
                val (sdTotal, sdUsed) = getSdStorageSpace()

                val now = Calendar.getInstance()
                val todayYear = now.get(Calendar.YEAR)
                val todayDay = now.get(Calendar.DAY_OF_YEAR)

                var todayCount = 0
                var dashcamCount = 0
                var replaysCount = 0
                var surveillanceCount = 0
                var totalBytes = 0L

                val cal = Calendar.getInstance()
                for (rec in scanned) {
                    totalBytes += rec.sizeBytes
                    cal.timeInMillis = rec.timestamp
                    if (cal.get(Calendar.YEAR) == todayYear && cal.get(Calendar.DAY_OF_YEAR) == todayDay) {
                        todayCount++
                    }
                    when (rec.type) {
                        RecordingFile.RecordingType.NORMAL,
                        RecordingFile.RecordingType.PROXIMITY,
                        RecordingFile.RecordingType.OEM_DASHCAM -> dashcamCount++
                        RecordingFile.RecordingType.REPLAY -> replaysCount++
                        RecordingFile.RecordingType.SENTRY -> surveillanceCount++
                    }
                }

                val stats = StorageStats(
                    totalBytes = totalBytes,
                    usedBytes = internalUsed + sdUsed,
                    freeBytes = (internalTotal - internalUsed).coerceAtLeast(0L),
                    totalCount = scanned.size,
                    todayCount = todayCount,
                    dashcamCount = dashcamCount,
                    replaysCount = replaysCount,
                    surveillanceCount = surveillanceCount,
                    internalTotalBytes = internalTotal,
                    internalUsedBytes = internalUsed,
                    sdTotalBytes = sdTotal,
                    sdUsedBytes = sdUsed,
                    formattedSummary = formatSummary(todayCount, scanned.size, totalBytes)
                )

                withContext(Dispatchers.Main) {
                    _uiState.update { current ->
                        val filtered = applyFilters(
                            list = scanned,
                            tab = current.selectedTab,
                            actor = current.selectedActorFilter,
                            severity = current.selectedSeverityFilter,
                            storage = current.selectedStorageFilter,
                            date = current.selectedDateFilter,
                            query = current.searchQuery
                        )
                        val grouped = groupRecordings(filtered)
                        val selected = current.selectedRecording?.let { sel ->
                            filtered.find { it.path == sel.path || it.recordingId == sel.recordingId }
                        } ?: filtered.firstOrNull()

                        current.copy(
                            isLoading = false,
                            isIndexDown = statsPayload?.indexUnavailable == true,
                            allRecordings = scanned,
                            displayedRecordings = filtered,
                            groupedRecordings = grouped,
                            stats = stats,
                            selectedRecording = selected
                        )
                    }
                }
            }
        }
    }

    fun selectTab(tab: RecordingTab) {
        _uiState.update { current ->
            val filtered = applyFilters(
                list = current.allRecordings,
                tab = tab,
                actor = current.selectedActorFilter,
                severity = current.selectedSeverityFilter,
                storage = current.selectedStorageFilter,
                date = current.selectedDateFilter,
                query = current.searchQuery
            )
            val grouped = groupRecordings(filtered)
            val selected = current.selectedRecording?.takeIf { sel ->
                filtered.any { it.path == sel.path || it.recordingId == sel.recordingId }
            } ?: filtered.firstOrNull()

            current.copy(
                selectedTab = tab,
                displayedRecordings = filtered,
                groupedRecordings = grouped,
                selectedRecording = selected
            )
        }
    }

    fun setActorFilter(actor: ActorFilter) {
        _uiState.update { current ->
            val filtered = applyFilters(
                list = current.allRecordings,
                tab = current.selectedTab,
                actor = actor,
                severity = current.selectedSeverityFilter,
                storage = current.selectedStorageFilter,
                date = current.selectedDateFilter,
                query = current.searchQuery
            )
            val grouped = groupRecordings(filtered)
            val selected = current.selectedRecording?.takeIf { sel ->
                filtered.any { it.path == sel.path || it.recordingId == sel.recordingId }
            } ?: filtered.firstOrNull()

            current.copy(
                selectedActorFilter = actor,
                displayedRecordings = filtered,
                groupedRecordings = grouped,
                selectedRecording = selected
            )
        }
    }

    fun setSeverityFilter(severity: SeverityFilter) {
        _uiState.update { current ->
            val filtered = applyFilters(
                list = current.allRecordings,
                tab = current.selectedTab,
                actor = current.selectedActorFilter,
                severity = severity,
                storage = current.selectedStorageFilter,
                date = current.selectedDateFilter,
                query = current.searchQuery
            )
            val grouped = groupRecordings(filtered)
            val selected = current.selectedRecording?.takeIf { sel ->
                filtered.any { it.path == sel.path || it.recordingId == sel.recordingId }
            } ?: filtered.firstOrNull()

            current.copy(
                selectedSeverityFilter = severity,
                displayedRecordings = filtered,
                groupedRecordings = grouped,
                selectedRecording = selected
            )
        }
    }

    fun setStorageFilter(storage: StorageFilter) {
        _uiState.update { current ->
            val filtered = applyFilters(
                list = current.allRecordings,
                tab = current.selectedTab,
                actor = current.selectedActorFilter,
                severity = current.selectedSeverityFilter,
                storage = storage,
                date = current.selectedDateFilter,
                query = current.searchQuery
            )
            val grouped = groupRecordings(filtered)
            val selected = current.selectedRecording?.takeIf { sel ->
                filtered.any { it.path == sel.path || it.recordingId == sel.recordingId }
            } ?: filtered.firstOrNull()

            current.copy(
                selectedStorageFilter = storage,
                displayedRecordings = filtered,
                groupedRecordings = grouped,
                selectedRecording = selected
            )
        }
    }

    fun setDateFilter(date: DateFilter) {
        _uiState.update { current ->
            val filtered = applyFilters(
                list = current.allRecordings,
                tab = current.selectedTab,
                actor = current.selectedActorFilter,
                severity = current.selectedSeverityFilter,
                storage = current.selectedStorageFilter,
                date = date,
                query = current.searchQuery
            )
            val grouped = groupRecordings(filtered)
            val selected = current.selectedRecording?.takeIf { sel ->
                filtered.any { it.path == sel.path || it.recordingId == sel.recordingId }
            } ?: filtered.firstOrNull()

            current.copy(
                selectedDateFilter = date,
                displayedRecordings = filtered,
                groupedRecordings = grouped,
                selectedRecording = selected
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { current ->
            val filtered = applyFilters(
                list = current.allRecordings,
                tab = current.selectedTab,
                actor = current.selectedActorFilter,
                severity = current.selectedSeverityFilter,
                storage = current.selectedStorageFilter,
                date = current.selectedDateFilter,
                query = query
            )
            val grouped = groupRecordings(filtered)
            val selected = current.selectedRecording?.takeIf { sel ->
                filtered.any { it.path == sel.path || it.recordingId == sel.recordingId }
            } ?: filtered.firstOrNull()

            current.copy(
                searchQuery = query,
                displayedRecordings = filtered,
                groupedRecordings = grouped,
                selectedRecording = selected
            )
        }
    }

    fun clearFilters() {
        _uiState.update { current ->
            val filtered = applyFilters(
                list = current.allRecordings,
                tab = current.selectedTab,
                actor = ActorFilter.ALL,
                severity = SeverityFilter.ALL,
                storage = StorageFilter.ALL,
                date = DateFilter.ALL,
                query = ""
            )
            val grouped = groupRecordings(filtered)
            current.copy(
                selectedActorFilter = ActorFilter.ALL,
                selectedSeverityFilter = SeverityFilter.ALL,
                selectedStorageFilter = StorageFilter.ALL,
                selectedDateFilter = DateFilter.ALL,
                searchQuery = "",
                displayedRecordings = filtered,
                groupedRecordings = grouped,
                selectedRecording = filtered.firstOrNull()
            )
        }
    }

    fun selectRecording(recording: RecordingFile?) {
        _uiState.update {
            it.copy(
                selectedRecording = recording,
                isPlaying = false,
                playbackPositionMs = 0L,
                selectedQuadrant = ZoomableVideoView.Quadrant.ALL
            )
        }
    }

    fun toggleSelectionMode() {
        _uiState.update { current ->
            val newMode = !current.isSelectionMode
            current.copy(
                isSelectionMode = newMode,
                selectedClipIds = if (newMode) emptySet() else emptySet()
            )
        }
    }

    fun toggleClipSelection(recordingIdOrPath: String) {
        _uiState.update { current ->
            val updated = current.selectedClipIds.toMutableSet()
            if (updated.contains(recordingIdOrPath)) {
                updated.remove(recordingIdOrPath)
            } else {
                updated.add(recordingIdOrPath)
            }
            current.copy(selectedClipIds = updated)
        }
    }

    fun selectAllClips() {
        _uiState.update { current ->
            val allIds = current.displayedRecordings.map { it.recordingId ?: it.path }.toSet()
            current.copy(selectedClipIds = allIds)
        }
    }

    fun clearClipSelection() {
        _uiState.update { it.copy(selectedClipIds = emptySet()) }
    }

    fun setQuadrant(quadrant: ZoomableVideoView.Quadrant) {
        _uiState.update { it.copy(selectedQuadrant = quadrant) }
    }

    fun toggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
    }

    fun promptDeleteClip(recording: RecordingFile) {
        _uiState.update { it.copy(showDeleteConfirmDialog = true, clipToDelete = recording) }
    }

    fun dismissDeleteClip() {
        _uiState.update { it.copy(showDeleteConfirmDialog = false, clipToDelete = null) }
    }

    fun executeDeleteClip() {
        val target = _uiState.value.clipToDelete ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(showDeleteConfirmDialog = false, clipToDelete = null) }
            withContext(Dispatchers.IO) {
                RecordingScanner.deleteRecording(target)
            }
            loadRecordings()
        }
    }

    fun promptBatchDelete() {
        if (_uiState.value.selectedClipIds.isNotEmpty()) {
            _uiState.update { it.copy(showBatchDeleteConfirmDialog = true) }
        }
    }

    fun dismissBatchDelete() {
        _uiState.update { it.copy(showBatchDeleteConfirmDialog = false) }
    }

    fun executeBatchDelete() {
        val idsToDelete = _uiState.value.selectedClipIds
        if (idsToDelete.isEmpty()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showBatchDeleteConfirmDialog = false,
                    isSelectionMode = false,
                    selectedClipIds = emptySet()
                )
            }
            withContext(Dispatchers.IO) {
                val toDelete = _uiState.value.allRecordings.filter {
                    (it.recordingId ?: it.path) in idsToDelete
                }
                for (rec in toDelete) {
                    RecordingScanner.deleteRecording(rec)
                }
            }
            loadRecordings()
        }
    }

    suspend fun getThumbnail(recording: RecordingFile): Bitmap? = withContext(Dispatchers.IO) {
        val key = recording.recordingId ?: recording.path
        thumbnailCache.get(key)?.let { return@withContext it }

        // 1. Try sidecar hero JPEG
        recording.heroThumbnailFile?.takeIf { it.exists() && it.length() > 0 }?.let { heroFile ->
            try {
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                val bmp = BitmapFactory.decodeFile(heroFile.absolutePath, opts)
                if (bmp != null) {
                    thumbnailCache.put(key, bmp)
                    return@withContext bmp
                }
            } catch (_: Throwable) {}
        }

        // 2. Try MediaMetadataRetriever from file
        if (recording.file.exists() && recording.file.canRead()) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(recording.file.absolutePath)
                val bmp = retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (bmp != null) {
                    val scaled = Bitmap.createScaledBitmap(bmp, 240, 135, true)
                    thumbnailCache.put(key, scaled)
                    return@withContext scaled
                }
            } catch (_: Throwable) {
            } finally {
                try { retriever.release() } catch (_: Throwable) {}
            }
        }
        null
    }

    private fun applyFilters(
        list: List<RecordingFile>,
        tab: RecordingTab,
        actor: ActorFilter,
        severity: SeverityFilter,
        storage: StorageFilter,
        date: DateFilter,
        query: String
    ): List<RecordingFile> {
        val now = Calendar.getInstance()
        val todayYear = now.get(Calendar.YEAR)
        val todayDay = now.get(Calendar.DAY_OF_YEAR)

        val cal = Calendar.getInstance()

        return list.filter { rec ->
            // Tab filter
            val tabMatch = when (tab) {
                RecordingTab.ALL -> true
                RecordingTab.DASHCAM -> rec.type in listOf(
                    RecordingFile.RecordingType.NORMAL,
                    RecordingFile.RecordingType.PROXIMITY,
                    RecordingFile.RecordingType.OEM_DASHCAM
                )
                RecordingTab.REPLAYS -> rec.type == RecordingFile.RecordingType.REPLAY
                RecordingTab.SURVEILLANCE -> rec.type == RecordingFile.RecordingType.SENTRY
            }
            if (!tabMatch) return@filter false

            // Date filter
            if (date != DateFilter.ALL) {
                cal.timeInMillis = rec.timestamp
                val recYear = cal.get(Calendar.YEAR)
                val recDay = cal.get(Calendar.DAY_OF_YEAR)
                when (date) {
                    DateFilter.TODAY -> if (recYear != todayYear || recDay != todayDay) return@filter false
                    DateFilter.YESTERDAY -> if (recYear != todayYear || recDay != todayDay - 1) return@filter false
                    DateFilter.ALL -> Unit
                }
            }

            // Actor filter
            when (actor) {
                ActorFilter.ALL -> Unit
                ActorFilter.PERSON -> if (rec.personCount == 0 && !rec.actorClasses.any { it.contains("person", true) }) return@filter false
                ActorFilter.VEHICLE -> if (rec.vehicleCount == 0 && !rec.actorClasses.any { it.contains("vehicle", true) || it.contains("car", true) }) return@filter false
                ActorFilter.BIKE -> if (rec.bikeCount == 0 && !rec.actorClasses.any { it.contains("bike", true) || it.contains("bicycle", true) }) return@filter false
                ActorFilter.ANIMAL -> if (rec.animalCount == 0 && !rec.actorClasses.any { it.contains("animal", true) || it.contains("dog", true) || it.contains("cat", true) }) return@filter false
            }

            // Severity filter
            when (severity) {
                SeverityFilter.ALL -> Unit
                SeverityFilter.ALERT -> if (!rec.peakSeverity.equals("ALERT", true)) return@filter false
                SeverityFilter.CRITICAL -> if (!rec.peakSeverity.equals("CRITICAL", true)) return@filter false
            }

            // Storage filter
            when (storage) {
                StorageFilter.ALL -> Unit
                StorageFilter.INTERNAL -> if (!rec.storageType.equals("INTERNAL", true)) return@filter false
                StorageFilter.SD_CARD -> if (!rec.storageType.equals("SD_CARD", true)) return@filter false
                StorageFilter.USB -> if (!rec.storageType.equals("USB", true)) return@filter false
            }

            // Query filter
            if (query.isNotBlank()) {
                val q = query.trim().lowercase()
                val nameMatch = rec.name.lowercase().contains(q)
                val placeMatch = rec.placeShortLabel?.lowercase()?.contains(q) == true ||
                        rec.placeMediumLabel?.lowercase()?.contains(q) == true ||
                        rec.placeDisplayName?.lowercase()?.contains(q) == true
                if (!nameMatch && !placeMatch) return@filter false
            }

            true
        }
    }

    private fun groupRecordings(list: List<RecordingFile>): Map<String, List<RecordingFile>> {
        val grouped = LinkedHashMap<String, MutableList<RecordingFile>>()
        val now = Calendar.getInstance()
        val todayYear = now.get(Calendar.YEAR)
        val todayDay = now.get(Calendar.DAY_OF_YEAR)

        val cal = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

        val todayLabel = getApplication<Application>().getString(R.string.recording_lib_date_today)
        val yesterdayLabel = getApplication<Application>().getString(R.string.recording_lib_date_yesterday)

        for (rec in list) {
            val label = when {
                !rec.bucketLabel.isNullOrBlank() -> rec.bucketLabel
                else -> {
                    cal.timeInMillis = rec.timestamp
                    val recYear = cal.get(Calendar.YEAR)
                    val recDay = cal.get(Calendar.DAY_OF_YEAR)
                    when {
                        recYear == todayYear && recDay == todayDay -> todayLabel
                        recYear == todayYear && recDay == todayDay - 1 -> yesterdayLabel
                        else -> dateFormat.format(Date(rec.timestamp))
                    }
                }
            }
            grouped.getOrPut(label) { mutableListOf() }.add(rec)
        }
        return grouped
    }

    private fun formatSummary(today: Int, total: Int, bytes: Long): String {
        val app = getApplication<Application>()
        val formattedSize = when {
            bytes >= 1_000_000_000L -> String.format(Locale.getDefault(), "%.1f GB", bytes / 1_000_000_000.0)
            bytes >= 1_000_000L -> String.format(Locale.getDefault(), "%.1f MB", bytes / 1_000_000.0)
            bytes >= 1_000L -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1_000.0)
            else -> "$bytes B"
        }
        return app.getString(R.string.recordings_summary_format, today, total, formattedSize)
    }

    private fun getInternalStorageSpace(): Pair<Long, Long> {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = stat.totalBytes
            val free = stat.availableBytes
            val used = (total - free).coerceAtLeast(0L)
            Pair(total, used)
        } catch (_: Throwable) {
            Pair(64_000_000_000L, 20_000_000_000L)
        }
    }

    private fun getSdStorageSpace(): Pair<Long, Long> {
        return try {
            val sdDir = File("/storage/sdcard1").takeIf { it.exists() }
                ?: File("/mnt/media_rw").listFiles()?.firstOrNull { it.canRead() }
            if (sdDir != null) {
                val stat = StatFs(sdDir.path)
                val total = stat.totalBytes
                val free = stat.availableBytes
                Pair(total, (total - free).coerceAtLeast(0L))
            } else {
                Pair(0L, 0L)
            }
        } catch (_: Throwable) {
            Pair(0L, 0L)
        }
    }
}
