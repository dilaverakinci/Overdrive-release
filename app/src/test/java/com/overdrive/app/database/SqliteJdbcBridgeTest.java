package com.overdrive.app.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class SqliteJdbcBridgeTest {

    @Test
    public void translateSqlTranslatesH2DialectsToSqlite() {
        // IDENTITY PRIMARY KEY
        String sql1 = "CREATE TABLE trips (id IDENTITY PRIMARY KEY, start_time BIGINT);";
        assertEquals("CREATE TABLE trips (id INTEGER PRIMARY KEY AUTOINCREMENT, start_time BIGINT);",
                SqliteJdbcBridge.translateSql(sql1));

        // SET CACHE_SIZE
        assertEquals("PRAGMA cache_size = -4000;", SqliteJdbcBridge.translateSql("SET CACHE_SIZE 8192"));

        // SET WRITE_DELAY
        assertEquals("PRAGMA synchronous = NORMAL;", SqliteJdbcBridge.translateSql("SET WRITE_DELAY 500"));

        // MERGE INTO ... KEY(...) VALUES (...)
        String mergeValues = "MERGE INTO daily_usage (day_key, bytes) KEY(day_key) VALUES (?, ?)";
        assertEquals("INSERT OR REPLACE INTO daily_usage (day_key, bytes) VALUES (?, ?)",
                SqliteJdbcBridge.translateSql(mergeValues));

        // MERGE INTO ... KEY(...) SELECT ...
        String mergeSelect = "MERGE INTO soc_daily (day_epoch, min_soc) KEY(day_epoch) SELECT d, min(s) FROM hist;";
        assertEquals("INSERT OR REPLACE INTO soc_daily (day_epoch, min_soc) SELECT d, min(s) FROM hist;",
                SqliteJdbcBridge.translateSql(mergeSelect));

        // MERGE INTO without column list
        String mergeNoCols = "MERGE INTO meta KEY(key) VALUES ('v1', 'v2')";
        assertEquals("INSERT OR REPLACE INTO meta VALUES ('v1', 'v2')",
                SqliteJdbcBridge.translateSql(mergeNoCols));

        // ALTER TABLE ... ADD COLUMN IF NOT EXISTS
        String alterAdd = "ALTER TABLE trips ADD COLUMN IF NOT EXISTS kwh_start REAL DEFAULT 0";
        assertEquals("ALTER TABLE trips ADD COLUMN kwh_start REAL DEFAULT 0",
                SqliteJdbcBridge.translateSql(alterAdd));
    }

    @Test
    public void getJdbcConnectionProvidesWorkingConnectionInJvmTests() throws Exception {
        try (Connection conn = SqliteDatabaseManager.getJdbcConnection("test_bridge_db")) {
            assertNotNull(conn);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(64));");
                stmt.execute("INSERT INTO test_table VALUES (1, 'Overdrive');");
            }

            try (PreparedStatement pstmt = conn.prepareStatement("SELECT name FROM test_table WHERE id=?")) {
                pstmt.setInt(1, 1);
                try (ResultSet rs = pstmt.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals("Overdrive", rs.getString("name"));
                }
            }
        }
    }
}
