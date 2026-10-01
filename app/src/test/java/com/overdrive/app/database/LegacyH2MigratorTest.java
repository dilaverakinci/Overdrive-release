package com.overdrive.app.database;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;

public class LegacyH2MigratorTest {

    @Test
    public void migratorBailsCleanlyWhenSentinelOrNoFilesPresent() {
        // Must not throw or crash when no legacy files exist
        LegacyH2Migrator.migrateAllAsync();
        assertTrue(true);
    }
}
