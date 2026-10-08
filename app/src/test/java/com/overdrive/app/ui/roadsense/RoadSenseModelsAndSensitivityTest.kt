package com.overdrive.app.ui.roadsense

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoadSenseModelsAndSensitivityTest {

    @Test
    fun testSensitivityPctToMultAndBack() {
        // 50% must map exactly to 1.0 (default threshold multiplier)
        val defaultMult = RoadSenseSensitivityUtils.pctToMult(50)
        assertEquals(1.0f, defaultMult, 0.001f)
        val defaultPct = RoadSenseSensitivityUtils.multToPct(defaultMult)
        assertEquals(50, defaultPct)

        // 0% (less sensitive) must map to MULT_MAX (1.3)
        val minSensMult = RoadSenseSensitivityUtils.pctToMult(0)
        assertEquals(1.3f, minSensMult, 0.001f)
        val minSensPct = RoadSenseSensitivityUtils.multToPct(minSensMult)
        assertEquals(0, minSensPct)

        // 100% (more sensitive) must map to MULT_MIN (0.7)
        val maxSensMult = RoadSenseSensitivityUtils.pctToMult(100)
        assertEquals(0.7f, maxSensMult, 0.001f)
        val maxSensPct = RoadSenseSensitivityUtils.multToPct(maxSensMult)
        assertEquals(100, maxSensPct)

        // Monotonic check: as pct increases, multiplier decreases (more sensitive = lower threshold)
        var lastMult = 2.0f
        for (p in 0..100 step 5) {
            val mult = RoadSenseSensitivityUtils.pctToMult(p)
            assertTrue(mult <= lastMult)
            lastMult = mult
        }
    }

    @Test
    fun testEnumsFromWire() {
        assertEquals(WarnMode.VISUAL, WarnMode.fromWire("visual"))
        assertEquals(WarnMode.AUDIO, WarnMode.fromWire("audio"))
        assertEquals(WarnMode.BOTH, WarnMode.fromWire("both"))
        assertEquals(WarnMode.BOTH, WarnMode.fromWire("unknown"))

        assertEquals(SoundChannel.NAVIGATION, SoundChannel.fromWire("navigation"))
        assertEquals(SoundChannel.MEDIA, SoundChannel.fromWire("media"))
        assertEquals(SoundChannel.VOICE, SoundChannel.fromWire("voice"))
        assertEquals(SoundChannel.ALARM, SoundChannel.fromWire("alarm"))
        assertEquals(SoundChannel.NAVIGATION, SoundChannel.fromWire("other"))

        assertEquals(BsMergeMode.BOTH, BsMergeMode.fromWire("both"))
        assertEquals(BsMergeMode.SIDE, BsMergeMode.fromWire("side"))
        assertEquals(BsMergeMode.REAR, BsMergeMode.fromWire("rear"))

        assertEquals(BsDisplayTarget.HEAD_UNIT, BsDisplayTarget.fromWire("head_unit"))
        assertEquals(BsDisplayTarget.CLUSTER, BsDisplayTarget.fromWire("cluster"))
    }

    @Test
    fun testInitialUiStateDefaults() {
        val state = RoadSenseUiState()
        assertEquals(RoadSenseTab.GENERAL, state.activeTab)
        assertEquals(false, state.general.enabled)
        assertEquals(1.0f, state.general.detectionSensitivityMult, 0.001f)
        assertEquals(50, state.general.detectionSensitivityPct)
        assertEquals(true, state.general.overlayVisible)

        assertEquals(31, state.map.clusterLayout)
        assertEquals(false, state.map.clusterProjecting)

        assertEquals(true, state.warnings.warnEnabled)
        assertEquals(WarnMode.BOTH, state.warnings.warnMode)
        assertEquals(SoundChannel.NAVIGATION, state.warnings.warnAudioChannel)
        assertEquals(75, state.warnings.warnAudioVolume)
        assertEquals(4, state.warnings.warnLeadSeconds)
        assertEquals(0, state.warnings.warnConfidenceThreshold)

        assertEquals(false, state.blindSpot.enabled)
        assertEquals(BsMergeMode.BOTH, state.blindSpot.mergeMode)
        assertEquals(40, state.blindSpot.sizePct)
        assertEquals(1.66f, state.blindSpot.rearFov, 0.001f)
        assertEquals(1.98f, state.blindSpot.sideFov, 0.001f)
    }
}
