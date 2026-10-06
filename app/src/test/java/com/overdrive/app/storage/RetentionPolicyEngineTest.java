package com.overdrive.app.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RetentionPolicyEngineTest {

    @Test
    public void categoryPrefixMatchingDetectsPrimaryAndAux() {
        String[] aux = new String[]{"dvr_", "thumb_"};
        assertTrue(RetentionPolicyEngine.nameMatchesCategoryPrefix("cam_front.mp4", "cam_", aux));
        assertTrue(RetentionPolicyEngine.nameMatchesCategoryPrefix("dvr_clip.mp4", "cam_", aux));
        assertTrue(RetentionPolicyEngine.nameMatchesCategoryPrefix("thumb_123.jpg", "cam_", aux));
        assertFalse(RetentionPolicyEngine.nameMatchesCategoryPrefix("other_clip.mp4", "cam_", aux));
    }

    @Test
    public void stemForNameStripsPrimaryExtension() {
        assertEquals("event_2026-10-06", RetentionPolicyEngine.stemForName("event_2026-10-06.mp4", ".mp4"));
        assertEquals("trip_123", RetentionPolicyEngine.stemForName("trip_123.jsonl.gz", ".jsonl.gz"));
        assertEquals("plain", RetentionPolicyEngine.stemForName("plain", ".mp4"));
    }

    @Test
    public void actorMarkerDetectionFindsLastActorIndex() {
        assertEquals(15, RetentionPolicyEngine.lastIndexOfActorMarker("thumb_event_123_a1.jpg", 5));
        assertEquals(-1, RetentionPolicyEngine.lastIndexOfActorMarker("thumb_event_123.jpg", 5));
    }

    @Test
    public void reapDecisionHandlesNormalAndEmergencyStates() {
        long limit = 1000L * 1024 * 1024;
        // < 90% of limit
        assertEquals(RetentionPolicyEngine.REAP_NONE,
                RetentionPolicyEngine.evaluateReapDecision(false, 800L * 1024 * 1024, limit, false));

        // Disk critical overrides all
        assertEquals(RetentionPolicyEngine.REAP_FULL,
                RetentionPolicyEngine.evaluateReapDecision(true, 800L * 1024 * 1024, limit, true));

        // Idle encoder -> full reap
        assertEquals(RetentionPolicyEngine.REAP_FULL,
                RetentionPolicyEngine.evaluateReapDecision(false, 950L * 1024 * 1024, limit, false));

        // Encoder writing, slightly over cap (<= 105%) -> bounded trim
        assertEquals(RetentionPolicyEngine.REAP_BOUNDED,
                RetentionPolicyEngine.evaluateReapDecision(true, 1020L * 1024 * 1024, limit, false));

        // Encoder writing, hard over cap (> 105%) -> full reap
        assertEquals(RetentionPolicyEngine.REAP_FULL,
                RetentionPolicyEngine.evaluateReapDecision(true, 1100L * 1024 * 1024, limit, false));
    }
}
