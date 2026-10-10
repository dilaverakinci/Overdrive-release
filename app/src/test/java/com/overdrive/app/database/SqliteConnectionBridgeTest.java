package com.overdrive.app.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

public class SqliteConnectionBridgeTest {

    private OverdriveSqliteMaster master;
    private Connection connection;

    @Before
    public void setUp() {
        master = OverdriveSqliteMaster.useInMemoryForTesting();
        connection = master.asJdbcConnection();
    }

    @After
    public void tearDown() {
        if (connection != null) {
            try { connection.close(); } catch (Exception ignored) {}
        }
        if (master != null) {
            master.close();
        }
    }

    @Test
    public void basicCrudAndTypedGettersWork() throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE test_users ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "name TEXT, "
                    + "age INT, "
                    + "balance REAL, "
                    + "is_active INT, "
                    + "notes TEXT);");
        }

        // Insert using PreparedStatement
        String insertSql = "INSERT INTO test_users (name, age, balance, is_active, notes) VALUES (?, ?, ?, ?, ?)";
        long generatedId;
        try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
            ps.setString(1, "Alice");
            ps.setInt(2, 30);
            ps.setDouble(3, 125.50);
            ps.setBoolean(4, true);
            ps.setNull(5, java.sql.Types.VARCHAR);
            int count = ps.executeUpdate();
            assertEquals(1, count);

            try (ResultSet keys = ps.getGeneratedKeys()) {
                assertTrue(keys.next());
                generatedId = keys.getLong(1);
                assertTrue(generatedId > 0);
            }
        }

        // Query using PreparedStatement and check typed getters by column name and index
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM test_users WHERE id = ?")) {
            ps.setLong(1, generatedId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(generatedId, rs.getLong("id"));
                assertEquals(generatedId, rs.getLong(1));

                assertEquals("Alice", rs.getString("NAME")); // case-insensitive
                assertEquals(30, rs.getInt("age"));
                assertEquals(125.50, rs.getDouble("balance"), 0.001);
                assertTrue(rs.getBoolean("is_active"));

                // Null handling & wasNull
                assertEquals(null, rs.getString("notes"));
                assertTrue(rs.wasNull());

                // Metadata
                ResultSetMetaData meta = rs.getMetaData();
                assertEquals(6, meta.getColumnCount());
                assertEquals("id", meta.getColumnName(1).toLowerCase());

                assertFalse(rs.next());
            }
        }
    }

    @Test
    public void batchExecutionWorks() throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE test_batch (id INTEGER PRIMARY KEY AUTOINCREMENT, val TEXT);");
        }

        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO test_batch (val) VALUES (?)")) {
            ps.setString(1, "item1");
            ps.addBatch();
            ps.setString(1, "item2");
            ps.addBatch();
            ps.setString(1, "item3");
            ps.addBatch();

            int[] results = ps.executeBatch();
            assertEquals(3, results.length);
        }

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM test_batch")) {
            assertTrue(rs.next());
            assertEquals(3, rs.getInt(1));
        }
    }

    @Test
    public void transactionCommitAndRollbackWork() throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE test_tx (id INT PRIMARY KEY, val TEXT);");
        }

        connection.setAutoCommit(false);
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO test_tx (id, val) VALUES (?, ?)")) {
            ps.setInt(1, 1);
            ps.setString(2, "committed");
            ps.executeUpdate();
        }
        connection.commit();

        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO test_tx (id, val) VALUES (?, ?)")) {
            ps.setInt(1, 2);
            ps.setString(2, "rolled_back");
            ps.executeUpdate();
        }
        connection.rollback();
        connection.setAutoCommit(true);

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM test_tx")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    public void databaseMetaDataDetectsTablesAndColumns() throws Exception {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE test_meta_table (col_a TEXT, col_b INT);");
        }

        DatabaseMetaData meta = connection.getMetaData();
        assertNotNull(meta);

        // Test table detection (case-insensitive)
        try (ResultSet tables = meta.getTables(null, null, "TEST_META_TABLE", null)) {
            assertTrue(tables.next());
            assertEquals("test_meta_table", tables.getString("TABLE_NAME").toLowerCase());
            assertFalse(tables.next());
        }

        // Test column detection (case-insensitive)
        try (ResultSet cols = meta.getColumns(null, null, "TEST_META_TABLE", "COL_A")) {
            assertTrue(cols.next());
            assertEquals("col_a", cols.getString("COLUMN_NAME").toLowerCase());
            assertFalse(cols.next());
        }

        // Test non-existent column
        try (ResultSet cols = meta.getColumns(null, null, "TEST_META_TABLE", "NON_EXISTENT")) {
            assertFalse(cols.next());
        }
    }

    @Test
    public void sqlTranslationsWork() throws Exception {
        // 1. IDENTITY PRIMARY KEY translation
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE test_identity (id IDENTITY PRIMARY KEY, name VARCHAR(64));");
        }

        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO test_identity (name) VALUES (?)")) {
            ps.setString(1, "identity_test");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                assertTrue(keys.next());
                assertTrue(keys.getLong(1) >= 1);
            }
        }

        // 2. ALTER TABLE ADD COLUMN IF NOT EXISTS translation
        try (Statement st = connection.createStatement()) {
            st.execute("ALTER TABLE test_identity ADD COLUMN IF NOT EXISTS extra_col VARCHAR(32);");
            // Run twice to ensure idempotency
            st.execute("ALTER TABLE test_identity ADD COLUMN IF NOT EXISTS extra_col VARCHAR(32);");
        }

        // 3. MERGE INTO translation
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE test_merge (k TEXT PRIMARY KEY, v TEXT);");
            st.execute("MERGE INTO test_merge KEY(k) VALUES ('key1', 'val1');");
            st.execute("MERGE INTO test_merge KEY(k) VALUES ('key1', 'val2');");
        }

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT v FROM test_merge WHERE k='key1'")) {
            assertTrue(rs.next());
            assertEquals("val2", rs.getString(1));
        }
    }
}
