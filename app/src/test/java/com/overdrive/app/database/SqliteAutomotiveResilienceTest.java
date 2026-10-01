package com.overdrive.app.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Automotive power-cut resilience and high-concurrency stress test suite (Phase 7).
 */
public class SqliteAutomotiveResilienceTest {

    @Test
    public void concurrentMultiThreadedWriterStressTest() throws Exception {
        String dbName = "resilience_stress_" + System.nanoTime();
        try (Connection conn = SqliteDatabaseManager.getJdbcConnection(dbName)) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE stress_log (id INT PRIMARY KEY, thread_id INT, val VARCHAR(64));");
            }
        }

        int threadCount = 8;
        int rowsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    try (Connection conn = SqliteDatabaseManager.getJdbcConnection(dbName)) {
                        for (int r = 0; r < rowsPerThread; r++) {
                            int rowId = threadId * 1000 + r;
                            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO stress_log VALUES (?, ?, ?);")) {
                                ps.setInt(1, rowId);
                                ps.setInt(2, threadId);
                                ps.setString(3, "payload_" + r);
                                ps.executeUpdate();
                                successCount.incrementAndGet();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue("Concurrent stress test timed out", doneLatch.await(15, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(threadCount * rowsPerThread, successCount.get());

        // Verify total committed rows
        try (Connection conn = SqliteDatabaseManager.getJdbcConnection(dbName);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM stress_log;")) {
            assertTrue(rs.next());
            assertEquals(threadCount * rowsPerThread, rs.getInt(1));
        }
    }

    @Test
    public void simulatedCrashAndAtomicRollbackTest() throws Exception {
        String dbName = "resilience_rollback_" + System.nanoTime();
        try (Connection conn = SqliteDatabaseManager.getJdbcConnection(dbName)) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE accounts (id INT PRIMARY KEY, balance REAL);");
                st.execute("INSERT INTO accounts VALUES (1, 100.0);");
            }

            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement("UPDATE accounts SET balance = balance - 50.0 WHERE id = 1;")) {
                ps.executeUpdate();
            }

            // Simulate mid-transaction crash or power cut: rollback without commit
            conn.rollback();
            conn.setAutoCommit(true);

            // Verify account balance remained untouched
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT balance FROM accounts WHERE id = 1;")) {
                assertTrue(rs.next());
                assertEquals(100.0, rs.getDouble("balance"), 0.001);
            }
        }
    }

    @Test
    public void pragmaConfigurationDirectivesTranslatedCorrectly() {
        assertEquals("PRAGMA cache_size = -4000;",
                SqliteJdbcBridge.translateSql("SET CACHE_SIZE 8192"));
        assertEquals("PRAGMA synchronous = NORMAL;",
                SqliteJdbcBridge.translateSql("SET WRITE_DELAY 500"));
    }
}
