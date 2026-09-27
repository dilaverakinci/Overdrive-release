package com.overdrive.app.ui.dashboard

import androidx.compose.ui.unit.dp
import com.overdrive.app.R
import com.overdrive.app.ui.component.OverdriveRailDestination
import com.overdrive.app.ui.component.OverdriveRailSectionGroup
import com.overdrive.app.ui.navigation.NavigationRailCatalog
import com.overdrive.app.ui.theme.OverdriveDimensions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parity and contract verification for Compose Native Dashboard & Navigation Rail models.
 */
class DashboardComposeParityTest {

    @Test
    fun testNavigationRailMetricsParity() {
        val d = OverdriveDimensions.Default
        assertEquals(80.dp, d.railCompactWidth)
        assertEquals(216.dp, d.railExpandedWidth)
        assertEquals(12.dp, d.railItemPillInsetHorizontal)
        assertEquals(4.dp, d.railItemPillInsetVertical)
        assertEquals(8.dp, d.railItemPillRadius)
    }

    @Test
    fun testNavigationRailCatalogParity() {
        val destinations = listOf(
            OverdriveRailDestination(
                key = "dashboard",
                labelRes = R.string.rail_dashboard,
                iconRes = R.drawable.ic_dashboard,
                destinationId = R.id.dashboardFragment
            ),
            OverdriveRailDestination(
                key = NavigationRailCatalog.ASSISTANT,
                labelRes = R.string.rail_assistant,
                iconRes = R.drawable.ic_smart_toy,
                destinationId = R.id.genAiFragment
            ),
            OverdriveRailDestination(
                key = NavigationRailCatalog.RECORDINGS,
                labelRes = R.string.rail_recordings,
                iconRes = R.drawable.ic_recording,
                destinationId = R.id.recordingsFragment,
                ownedDestinationIds = setOf(R.id.videoPlayerFragment)
            )
        )

        assertEquals(3, destinations.size)
        assertEquals("dashboard", destinations[0].key)
        assertTrue(destinations[2].ownedDestinationIds.contains(R.id.videoPlayerFragment))
    }

    @Test
    fun testDashboardRemoteStateModelParity() {
        val remote = DashboardRemoteState(
            isOnline = true,
            statusText = "https://test.zrok.io:8080",
            deviceId = "OD-DEVICE-777",
            activeUrl = "https://test.zrok.io:8080",
            deviceToken = "sec-token-1234",
            isTokenMasked = true,
            isExpanded = false
        )

        assertTrue(remote.isOnline)
        assertEquals("https://test.zrok.io:8080", remote.activeUrl)
        assertEquals("OD-DEVICE-777", remote.deviceId)
        assertTrue(remote.isTokenMasked)
        assertFalse(remote.isExpanded)
    }

    @Test
    fun testDashboardHeroStateModelParity() {
        val hero = DashboardHeroState(
            greeting = "Aracınız Güvende",
            subtitle = "Canlı telemetri güncelleniyor",
            vehicleModel = "BYD SEAL",
            tunnelChipText = "Online",
            isTunnelOnline = true,
            daemonsChipText = "3/3 Servis Aktif",
            areDaemonsRunning = true,
            recordingChipText = "Kayıt Yapılıyor",
            isRecordingActive = true
        )

        assertEquals("BYD SEAL", hero.vehicleModel)
        assertTrue(hero.isTunnelOnline)
        assertTrue(hero.areDaemonsRunning)
        assertTrue(hero.isRecordingActive)
    }

    @Test
    fun testDashboardUiStateStorageCalculations() {
        val storage = DashboardUiState.StorageSummary(
            usedBytes = 40_000_000_000L,
            availableBytes = 60_000_000_000L,
            totalBytes = 100_000_000_000L
        )

        assertEquals(40, storage.usagePercent)
    }
}
