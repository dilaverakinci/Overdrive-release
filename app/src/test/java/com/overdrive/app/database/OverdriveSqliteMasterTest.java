package com.overdrive.app.database;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OverdriveSqliteMasterTest {

    @Test
    public void masterDbPathIsConfiguredUnderDataLocalTmp() {
        assertNotNull(OverdriveSqliteMaster.DEFAULT_DB_PATH);
        assertTrue("Master SQLite DB should be stored in /data/local/tmp",
                OverdriveSqliteMaster.DEFAULT_DB_PATH.contains("overdrive_master.db"));
    }

    @Test
    public void singletonInstanceIsAvailable() {
        OverdriveSqliteMaster instance = OverdriveSqliteMaster.getInstance();
        assertNotNull("OverdriveSqliteMaster singleton must not be null", instance);
    }
}
