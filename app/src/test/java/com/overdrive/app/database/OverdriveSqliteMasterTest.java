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

    @Test
    public void inMemoryMasterInitializesTablesAndExecutesQueries() {
        OverdriveSqliteMaster master = OverdriveSqliteMaster.useInMemoryForTesting();
        assertTrue("Master should be open", master.isOpen());

        // Test insertion into notifications table
        master.execSQL("INSERT INTO notifications (ts, category, severity, title, body) VALUES (?, ?, ?, ?, ?)",
                new Object[]{123456789L, "system", "info", "Test Title", "Test Body"});

        android.database.Cursor cursor = master.rawQuery("SELECT id, ts, category, severity, title, body FROM notifications WHERE ts = ?",
                new String[]{"123456789"});
        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        org.junit.Assert.assertEquals(123456789L, cursor.getLong(cursor.getColumnIndexOrThrow("ts")));
        org.junit.Assert.assertEquals("system", cursor.getString(cursor.getColumnIndexOrThrow("category")));
        org.junit.Assert.assertEquals("info", cursor.getString(cursor.getColumnIndexOrThrow("severity")));
        org.junit.Assert.assertEquals("Test Title", cursor.getString(cursor.getColumnIndexOrThrow("title")));
        org.junit.Assert.assertEquals("Test Body", cursor.getString(cursor.getColumnIndexOrThrow("body")));
        cursor.close();

        // Test transaction rollback
        master.beginTransaction();
        master.execSQL("INSERT INTO data_usage_daily (day_key, wifi_bytes) VALUES (?, ?)", new Object[]{"2026-10-10", 100L});
        // Not marking transaction successful -> should rollback
        master.endTransaction();

        android.database.Cursor rollCursor = master.rawQuery("SELECT wifi_bytes FROM data_usage_daily WHERE day_key = ?", new String[]{"2026-10-10"});
        org.junit.Assert.assertFalse("Rolled back row must not exist", rollCursor.moveToFirst());
        rollCursor.close();

        master.close();
        org.junit.Assert.assertFalse(master.isOpen());
    }
}
