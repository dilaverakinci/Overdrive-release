package com.overdrive.app.ui.cockpit

import android.app.Application
import com.overdrive.app.ui.dashboard.DashboardStatusResult
import com.overdrive.app.ui.dashboard.DashboardUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CockpitViewModelTest {

    private class FakeApplication : Application()

    private class FakeRepository(app: Application) : CockpitVehicleRepository(app) {
        var statusResult: DashboardStatusResult = DashboardStatusResult.Loading
        var clipCount: Int? = 0

        override suspend fun fetchVehicleStatus(): DashboardStatusResult = statusResult
        override suspend fun fetchTodayClipCount(): Int? = clipCount
    }

    private lateinit var app: FakeApplication
    private lateinit var repo: FakeRepository
    private lateinit var viewModel: CockpitViewModel

    @Before
    fun setup() {
        app = FakeApplication()
        repo = FakeRepository(app)
        viewModel = CockpitViewModel(app, repo)
    }

    @Test
    fun initialStateIsDefaultDashboardUiState() {
        val state = viewModel.uiState.value
        assertTrue(state.vehicle is DashboardUiState.VehicleState.Loading)
        assertTrue(state.recordings is DashboardUiState.RecordingState.Loading)
        assertTrue(state.activity is DashboardUiState.ActivityState.Loading)
        assertFalse(state.remoteExpanded)
    }

    @Test
    fun remoteExpandedUpdatesUiState() {
        viewModel.setRemoteExpanded(true)
        assertTrue(viewModel.uiState.value.remoteExpanded)

        viewModel.setRemoteExpanded(false)
        assertFalse(viewModel.uiState.value.remoteExpanded)
    }

    @Test
    fun updateStorageSummaryUpdatesUiState() {
        val storage = DashboardUiState.StorageSummary(
            usedBytes = 10_000_000L,
            availableBytes = 90_000_000L,
            totalBytes = 100_000_000L
        )
        viewModel.updateStorageSummary(storage)

        val recordingState = viewModel.uiState.value.recordings
        assertTrue(recordingState is DashboardUiState.RecordingState.Ready)
        val ready = recordingState as DashboardUiState.RecordingState.Ready
        assertEquals(storage, ready.storage)
        assertEquals(10, ready.storage?.usagePercent)
    }

    @Test
    fun setActivityRowsUpdatesUiStateWithBounds() {
        val rows = listOf(
            DashboardUiState.ActivityRow("Item 1"),
            DashboardUiState.ActivityRow("Item 2"),
            DashboardUiState.ActivityRow("Item 3"),
            DashboardUiState.ActivityRow("Item 4")
        )
        viewModel.setActivityRows(rows)

        val activityState = viewModel.uiState.value.activity
        assertTrue(activityState is DashboardUiState.ActivityState.Ready)
        val ready = activityState as DashboardUiState.ActivityState.Ready
        assertEquals(3, ready.rows.size)
        assertEquals("Item 1", ready.rows[0].text.toString())
    }
}
