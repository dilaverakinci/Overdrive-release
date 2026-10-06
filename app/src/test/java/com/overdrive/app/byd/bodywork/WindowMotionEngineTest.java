package com.overdrive.app.byd.bodywork;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WindowMotionEngineTest {

    @Test
    public void feedbackValidationRequiresFourValidPercentages() {
        assertTrue(WindowMotionEngine.hasCompleteSideWindowPositionFeedback(
                new int[] {0, 15, 50, 100}));

        assertFalse(WindowMotionEngine.hasCompleteSideWindowPositionFeedback(null));
        assertFalse(WindowMotionEngine.hasCompleteSideWindowPositionFeedback(
                new int[] {0, 15, 50}));
        assertFalse(WindowMotionEngine.hasCompleteSideWindowPositionFeedback(
                new int[] {0, 15, -1, 100}));
        assertFalse(WindowMotionEngine.hasCompleteSideWindowPositionFeedback(
                new int[] {0, 15, 101, 100}));
    }

    @Test
    public void commandPlanningGeneratesCorrectDirections() {
        assertArrayEquals(
                new int[] {1, 1, 1, 1},
                WindowMotionEngine.planSideWindowCommands(
                        new int[] {0, 0, 0, 0}, 15, 2));

        assertArrayEquals(
                new int[] {0, 2, 1, 2},
                WindowMotionEngine.planSideWindowCommands(
                        new int[] {15, 25, 10, 100}, 15, 2));

        assertNull(WindowMotionEngine.planSideWindowCommands(
                new int[] {15, 25, 10}, 15, 2));
        assertNull(WindowMotionEngine.planSideWindowCommands(
                new int[] {15, 25, 10, 100}, -1, 2));
        assertNull(WindowMotionEngine.planSideWindowCommands(
                new int[] {15, 25, 10, 100}, 105, 2));
        assertNull(WindowMotionEngine.planSideWindowCommands(
                new int[] {15, 25, 10, 100}, 50, -1));
    }

    @Test
    public void commandTowardTargetHandlesToleranceAndBounds() {
        assertEquals(0, WindowMotionEngine.sideWindowCommandTowardTarget(14, 15, 2));
        assertEquals(0, WindowMotionEngine.sideWindowCommandTowardTarget(15, 15, 0));
        assertEquals(1, WindowMotionEngine.sideWindowCommandTowardTarget(10, 15, 2));
        assertEquals(2, WindowMotionEngine.sideWindowCommandTowardTarget(25, 15, 2));

        assertEquals(-1, WindowMotionEngine.sideWindowCommandTowardTarget(-1, 15, 2));
        assertEquals(-1, WindowMotionEngine.sideWindowCommandTowardTarget(50, 105, 2));
        assertEquals(-1, WindowMotionEngine.sideWindowCommandTowardTarget(50, 50, -1));
    }

    @Test
    public void reachedTargetEvaluatesDirectionAndTolerance() {
        assertFalse(WindowMotionEngine.hasReachedSideWindowTarget(12, 15, 1, 2));
        assertTrue(WindowMotionEngine.hasReachedSideWindowTarget(13, 15, 1, 2));
        assertTrue(WindowMotionEngine.hasReachedSideWindowTarget(20, 15, 1, 2));

        assertFalse(WindowMotionEngine.hasReachedSideWindowTarget(18, 15, 2, 2));
        assertTrue(WindowMotionEngine.hasReachedSideWindowTarget(17, 15, 2, 2));
        assertTrue(WindowMotionEngine.hasReachedSideWindowTarget(10, 15, 2, 2));

        // Unknown / non-directional check
        assertTrue(WindowMotionEngine.hasReachedSideWindowTarget(15, 15, 0, 0));
        assertFalse(WindowMotionEngine.hasReachedSideWindowTarget(15, 17, 0, 1));

        // Invalid bounds
        assertFalse(WindowMotionEngine.hasReachedSideWindowTarget(-1, 15, 1, 2));
        assertFalse(WindowMotionEngine.hasReachedSideWindowTarget(15, 101, 1, 2));
        assertFalse(WindowMotionEngine.hasReachedSideWindowTarget(15, 50, 1, -1));
    }
}
