package com.overdrive.app.updater;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AppUpdaterVersionComparisonTest {

    @Test
    public void legacy51xUpgradesToBetterOverdriveVersions() {
        // Upstream 51.8 to BetterOverdrive 1.0.x and 10.x
        assertTrue("51.8 should upgrade to 1.0.106", AppUpdater.isNewerVersion("51.8", "1.0.106"));
        assertTrue("51.8 should upgrade to 10.8", AppUpdater.isNewerVersion("51.8", "10.8"));
        assertTrue("51.0 should upgrade to 10.8", AppUpdater.isNewerVersion("51.0", "10.8"));

        // Reject downgrade from BetterOverdrive back to 51.x
        assertFalse("10.8 should not downgrade to 51.8", AppUpdater.isNewerVersion("10.8", "51.8"));
        assertFalse("1.0.106 should not downgrade to 51.8", AppUpdater.isNewerVersion("1.0.106", "51.8"));
    }

    @Test
    public void standardSemverComparisonWorksBetweenBetterOverdriveVersions() {
        // Upgrade from 1.0.x to 10.x
        assertTrue("1.0.106 should upgrade to 10.8", AppUpdater.isNewerVersion("1.0.106", "10.8"));

        // Incremental upgrades
        assertTrue("10.8 should upgrade to 10.9", AppUpdater.isNewerVersion("10.8", "10.9"));
        assertTrue("10.9 should upgrade to 11.0", AppUpdater.isNewerVersion("10.9", "11.0"));

        // Same or older versions
        assertFalse("10.8 is not newer than 10.8", AppUpdater.isNewerVersion("10.8", "10.8"));
        assertFalse("10.8 is not newer than 10.9", AppUpdater.isNewerVersion("10.9", "10.8"));
    }
}
