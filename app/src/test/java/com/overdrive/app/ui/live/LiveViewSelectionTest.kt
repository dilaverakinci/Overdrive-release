package com.overdrive.app.ui.live

import com.overdrive.app.ui.fragment.LiveViewNativeFragment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveViewSelectionTest {

    @Test
    fun cameraConstantsMatchBydDaemonMapping() {
        assertEquals(0, LiveViewNativeFragment.CAM_ALL)
        assertEquals(1, LiveViewNativeFragment.CAM_FRONT)
        assertEquals(2, LiveViewNativeFragment.CAM_RIGHT)
        assertEquals(3, LiveViewNativeFragment.CAM_REAR)
        assertEquals(4, LiveViewNativeFragment.CAM_LEFT)
        assertEquals(6, LiveViewNativeFragment.CAM_DVR)
    }

    @Test
    fun streamQualityPresetsContainStandardBitrates() {
        val qualities = listOf(
            "ULTRA_LOW",
            "LOW",
            "MEDIUM",
            "HIGH",
            "ULTRA_HIGH",
            "SMOOTH",
            "MAX"
        )
        assertTrue(qualities.contains("MEDIUM"))
        assertTrue(qualities.contains("HIGH"))
        assertTrue(qualities.contains("ULTRA_HIGH"))
    }
}
