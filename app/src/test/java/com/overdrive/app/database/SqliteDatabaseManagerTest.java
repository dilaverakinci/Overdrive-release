package com.overdrive.app.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Unit tests verifying well-known names, paths, and registry invariants for SQLite WAL storage.
 */
public class SqliteDatabaseManagerTest {

    @Test
    public void wellKnownDatabaseNames_areDistinctAndValid() {
        assertEquals("overdrive_notifications", SqliteDatabaseManager.DB_NOTIFICATIONS);
        assertEquals("overdrive_parking", SqliteDatabaseManager.DB_PARKING);
        assertEquals("overdrive_soc", SqliteDatabaseManager.DB_SOC);
        assertEquals("overdrive_trips", SqliteDatabaseManager.DB_TRIPS);
        assertEquals("overdrive_recordings", SqliteDatabaseManager.DB_RECORDINGS);
        assertEquals("overdrive_data_usage", SqliteDatabaseManager.DB_DATA_USAGE);
    }

    @Test
    public void closeAll_succeedsEvenWhenRegistryIsEmpty() {
        // Safe to invoke at daemon startup or teardown with zero engines
        SqliteDatabaseManager.closeAll();
    }
}
