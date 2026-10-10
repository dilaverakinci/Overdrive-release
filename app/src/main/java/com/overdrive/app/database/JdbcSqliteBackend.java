package com.overdrive.app.database;

import android.database.Cursor;

import com.overdrive.app.logging.DaemonLogger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC-backed SQLite engine for host JVM tests (running on Windows/Linux development machines).
 * Uses {@code org.xerial:sqlite-jdbc} to provide full in-memory SQLite support.
 */
public class JdbcSqliteBackend implements SqliteBackend {

    private static final String TAG = "JdbcSqliteBackend";
    private static final DaemonLogger logger = DaemonLogger.getInstance(TAG);

    private final String jdbcUrl;
    private Connection connection;
    private boolean transactionSuccessful = false;
    private boolean inTransaction = false;

    public JdbcSqliteBackend(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public static JdbcSqliteBackend createInMemory() {
        return new JdbcSqliteBackend("jdbc:sqlite::memory:");
    }

    @Override
    public synchronized boolean open() {
        if (isOpen()) return true;
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(jdbcUrl);
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON;");
            }
            return true;
        } catch (Exception e) {
            logger.error("Failed to open JDBC SQLite: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public synchronized boolean isOpen() {
        try {
            return connection != null && !connection.isClosed();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception ignored) {}
            connection = null;
        }
    }

    @Override
    public synchronized void execSQL(String sql) {
        if (!isOpen() && !open()) {
            throw new IllegalStateException("JDBC SQLite is not open");
        }
        try (Statement st = connection.createStatement()) {
            st.execute(sql);
        } catch (Exception e) {
            throw new RuntimeException("execSQL failed: " + sql, e);
        }
    }

    @Override
    public synchronized void execSQL(String sql, Object[] bindArgs) {
        if (!isOpen() && !open()) {
            throw new IllegalStateException("JDBC SQLite is not open");
        }
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (bindArgs != null) {
                for (int i = 0; i < bindArgs.length; i++) {
                    ps.setObject(i + 1, bindArgs[i]);
                }
            }
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("execSQL with bindArgs failed: " + sql, e);
        }
    }

    @Override
    public synchronized Cursor rawQuery(String sql, String[] selectionArgs) {
        if (!isOpen() && !open()) {
            throw new IllegalStateException("JDBC SQLite is not open");
        }
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (selectionArgs != null) {
                for (int i = 0; i < selectionArgs.length; i++) {
                    ps.setString(i + 1, selectionArgs[i]);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();
                String[] colNames = new String[colCount];
                for (int i = 0; i < colCount; i++) {
                    colNames[i] = meta.getColumnLabel(i + 1);
                }
                List<Object[]> rows = new ArrayList<>();
                while (rs.next()) {
                    Object[] row = new Object[colCount];
                    for (int i = 0; i < colCount; i++) {
                        row[i] = rs.getObject(i + 1);
                    }
                    rows.add(row);
                }
                return new SimpleCursor(colNames, rows);
            }
        } catch (Exception e) {
            throw new RuntimeException("rawQuery failed: " + sql, e);
        }
    }

    @Override
    public synchronized void beginTransaction() {
        if (!isOpen() && !open()) {
            throw new IllegalStateException("JDBC SQLite is not open");
        }
        try {
            connection.setAutoCommit(false);
            transactionSuccessful = false;
            inTransaction = true;
        } catch (Exception e) {
            throw new RuntimeException("beginTransaction failed", e);
        }
    }

    @Override
    public synchronized void setTransactionSuccessful() {
        transactionSuccessful = true;
    }

    @Override
    public synchronized void endTransaction() {
        if (!inTransaction) return;
        try {
            if (transactionSuccessful) {
                connection.commit();
            } else {
                connection.rollback();
            }
            connection.setAutoCommit(true);
        } catch (Exception e) {
            throw new RuntimeException("endTransaction failed", e);
        } finally {
            inTransaction = false;
            transactionSuccessful = false;
        }
    }

    @Override
    public synchronized boolean inTransaction() {
        return inTransaction;
    }
}
