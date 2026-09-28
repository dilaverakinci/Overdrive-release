package com.overdrive.app.ui.parking

data class ParkingUiState(
    val activeTab: ParkingTab = ParkingTab.SESSIONS,
    val filterRange: ParkingFilterRange = ParkingFilterRange.DAYS_30,
    val isRunning: Boolean = false,
    val currentSession: ParkingSessionModel? = null,
    val sessions: List<ParkingSessionModel> = emptyList(),
    val selectedSessionId: String? = null,
    val detailData: ParkingDetailData? = null,
    val config: ParkingConfigModel = ParkingConfigModel(),
    val geocodingEnabled: Boolean = false,
    val geocodingOnline: Boolean = false,
    val isLoading: Boolean = false,
    val isDetailLoading: Boolean = false,
    val lightboxUrl: String? = null,
    val toastMessage: String? = null
)
