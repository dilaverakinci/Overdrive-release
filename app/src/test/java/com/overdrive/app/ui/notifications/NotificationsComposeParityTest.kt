package com.overdrive.app.ui.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying state mutations, actions, and safety invariants for Notifications Compose Native.
 */
class NotificationsComposeParityTest {

    @Test
    fun initialState_hasDefaultNotificationsState() {
        val state = NotificationsUiState()
        assertEquals(NotificationsTab.LOG, state.selectedTab)
        assertEquals(0, state.totalCount)
        assertEquals(1, state.currentPage)
        assertEquals(1, state.totalPages)
        assertFalse(state.isLoading)
        assertEquals(NotificationSeverity.ALL, state.filterSeverity)
        assertEquals(0, state.filterDays)
        assertEquals("", state.searchQuery)
        assertFalse(state.isSelectionMode)
        assertTrue(state.selectedIds.isEmpty())
        assertFalse(state.showClearAllDialog)
        assertFalse(state.showDeleteSelectedDialog)
    }

    @Test
    fun tabSelection_switchesToTelegramAndBack() {
        val state = NotificationsUiState()
        val toTelegram = state.copy(selectedTab = NotificationsTab.TELEGRAM)
        assertEquals(NotificationsTab.TELEGRAM, toTelegram.selectedTab)

        val backToLog = toTelegram.copy(selectedTab = NotificationsTab.LOG)
        assertEquals(NotificationsTab.LOG, backToLog.selectedTab)
    }

    @Test
    fun filterSeverity_updatesFilterCorrectly() {
        val state = NotificationsUiState()
        val criticalFilter = state.copy(filterSeverity = NotificationSeverity.CRITICAL)
        assertEquals(NotificationSeverity.CRITICAL, criticalFilter.filterSeverity)
        assertEquals("critical", criticalFilter.filterSeverity.key)

        val warnFilter = state.copy(filterSeverity = NotificationSeverity.WARN)
        assertEquals(NotificationSeverity.WARN, warnFilter.filterSeverity)
        assertEquals("warn", warnFilter.filterSeverity.key)
    }

    @Test
    fun selectionMode_togglesCorrectly() {
        val sampleItems = listOf(
            NotificationLogItem(1L, "Güvenlik Uyarısı", "Hareket tespit edildi", NotificationSeverity.CRITICAL, "surveillance", System.currentTimeMillis()),
            NotificationLogItem(2L, "Şarj Bilgisi", "Şarj tamamlandı", NotificationSeverity.INFO, "charging", System.currentTimeMillis())
        )
        val state = NotificationsUiState(logItems = sampleItems, totalCount = 2)

        val selected = state.copy(isSelectionMode = true, selectedIds = setOf(1L))
        assertTrue(selected.isSelectionMode)
        assertEquals(1, selected.selectedIds.size)
        assertTrue(selected.selectedIds.contains(1L))

        val selectAll = selected.copy(selectedIds = setOf(1L, 2L))
        assertEquals(2, selectAll.selectedIds.size)
    }

    @Test
    fun dialogState_guardsDestructiveModals() {
        val state = NotificationsUiState()
        val showClear = state.copy(showClearAllDialog = true)
        assertTrue(showClear.showClearAllDialog)

        val dismissed = showClear.copy(showClearAllDialog = false)
        assertFalse(dismissed.showClearAllDialog)
    }
}
