package com.overdrive.app.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for Issue #321: Park Standby mode in LocationSidecarService.
 * Validates debounce thresholds, standby polling interval, and state management.
 */
public class LocationSidecarServiceStandbyTest {

    @Test
    public void parkStabilizationThresholdIsFifteenSeconds() {
        assertEquals("PARK_STABILIZATION_MS must be 15,000 ms (15s) to avoid flickering at short stops",
                15_000L, LocationSidecarService.PARK_STABILIZATION_MS);
    }

    @Test
    public void standbyPollIntervalIsFiveSeconds() {
        assertEquals("STANDBY_POLL_INTERVAL_MS must be 5,000 ms to conserve CPU/HAL while keeping coordinates cached",
                5_000L, LocationSidecarService.STANDBY_POLL_INTERVAL_MS);
    }

    @Test
    public void defaultStandbyStateIsFalse() {
        LocationSidecarService service = new LocationSidecarService();
        assertFalse("New service must not be in park standby by default", service.isParkStandby());
        assertFalse("GPS provider should not be marked registered before start", service.isGpsProviderRegistered());
        assertFalse("Network provider should not be marked registered before start", service.isNetworkProviderRegistered());
    }

    @Test
    public void testStandbyStateToggle() {
        LocationSidecarService service = new LocationSidecarService();
        service.setParkStandbyForTesting(true);
        assertTrue(service.isParkStandby());

        service.setParkStandbyForTesting(false);
        assertFalse(service.isParkStandby());
    }
}
