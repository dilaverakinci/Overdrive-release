package com.overdrive.app.database;

import com.overdrive.app.logging.DaemonLogger;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Automatically discovers and migrates legacy H2 database files (.mv.db) to
 * the new high-performance SQLite WAL storage engine.
 * 
 * Features:
 * 1. Zero-startup latency: Runs asynchronously in a background worker thread.
 * 2. Sentinel marker: Checks /data/local/tmp/.overdrive_h2_migration_done to bail
 *    in 0ms on all subsequent app/daemon launches.
 * 3. Atomic rename: Marks migrated legacy files with .migrated extension so they
 *    are never reprocessed and can be safely reclaimed.
 * 4. Stale lock purge: Deletes orphaned .lock.db and .trace.db files left by previous H2 crashes.
 */
public final class LegacyH2Migrator {

    private static final String TAG = "LegacyH2Migrator";
    private static final DaemonLogger logger = DaemonLogger.getInstance(TAG);

    private static final String BASE_DIR = "/data/local/tmp";
    private static final File SENTINEL_FILE = new File(BASE_DIR, ".overdrive_h2_migration_done");

    private static final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "LegacyH2MigratorWorker");
        t.setDaemon(true);
        return t;
    });

    private LegacyH2Migrator() {}

    /**
     * Triggers asynchronous migration of legacy H2 databases if present.
     */
    public static void migrateAllAsync() {
        if (SENTINEL_FILE.exists()) {
            return;
        }

        executor.execute(() -> {
            try {
                performMigration();
            } catch (Throwable t) {
                logger.error("Legacy H2 migration encountered error: " + t.getMessage(), t);
            }
        });
    }

    /**
     * Executes legacy database migration synchronously.
     */
    public static void performMigration() {
        if (SENTINEL_FILE.exists()) {
            return;
        }

        logger.info("Checking for legacy H2 database files in " + BASE_DIR);

        // Check if H2 driver is available
        boolean hasH2Driver;
        try {
            Class.forName("org.h2.Driver");
            hasH2Driver = true;
        } catch (Throwable t) {
            hasH2Driver = false;
        }

        String[] targetDatabases = new String[]{
                "overdrive_notifications_h2",
                "overdrive_parking_h2",
                "overdrive_trips_h2",
                "overdrive_soc_h2",
                "overdrive_recordings_h2",
                "overdrive_datausage_h2"
        };

        boolean foundAny = false;
        for (String dbPrefix : targetDatabases) {
            File h2File = new File(BASE_DIR, dbPrefix + ".mv.db");
            if (h2File.exists() && h2File.length() > 0) {
                foundAny = true;
                if (hasH2Driver) {
                    migrateDatabase(dbPrefix, h2File);
                } else {
                    logger.warn("Found legacy database " + h2File.getName() + " but H2 driver is not present; skipping row transfer.");
                    h2File.renameTo(new File(BASE_DIR, dbPrefix + ".mv.db.unmigrated"));
                }
            }

            // Cleanup stale lock and trace files
            File lockFile = new File(BASE_DIR, dbPrefix + ".lock.db");
            if (lockFile.exists()) lockFile.delete();
            File traceFile = new File(BASE_DIR, dbPrefix + ".trace.db");
            if (traceFile.exists()) traceFile.delete();
        }

        try {
            SENTINEL_FILE.createNewFile();
            SENTINEL_FILE.setReadable(true, false);
            SENTINEL_FILE.setWritable(true, false);
            logger.info("Legacy H2 migration complete. Sentinel created at " + SENTINEL_FILE.getAbsolutePath());
        } catch (Exception e) {
            logger.warn("Could not create migration sentinel: " + e.getMessage());
        }
    }

    private static void migrateDatabase(String dbPrefix, File h2File) {
        String h2Url = "jdbc:h2:file:" + new File(BASE_DIR, dbPrefix).getAbsolutePath()
                + ";FILE_LOCK=SOCKET;TRACE_LEVEL_FILE=0;DB_CLOSE_ON_EXIT=FALSE";
        logger.info("Migrating legacy H2 database: " + h2File.getName());

        String targetDbName;
        if (dbPrefix.contains("notifications")) targetDbName = SqliteDatabaseManager.DB_NOTIFICATIONS;
        else if (dbPrefix.contains("parking")) targetDbName = SqliteDatabaseManager.DB_PARKING;
        else if (dbPrefix.contains("trips")) targetDbName = SqliteDatabaseManager.DB_TRIPS;
        else if (dbPrefix.contains("soc")) targetDbName = SqliteDatabaseManager.DB_SOC;
        else if (dbPrefix.contains("recordings")) targetDbName = SqliteDatabaseManager.DB_RECORDINGS;
        else if (dbPrefix.contains("datausage")) targetDbName = SqliteDatabaseManager.DB_DATA_USAGE;
        else targetDbName = dbPrefix;

        try (Connection h2Conn = DriverManager.getConnection(h2Url, "sa", "");
             Connection sqliteConn = SqliteDatabaseManager.getJdbcConnection(targetDbName)) {

            DatabaseMetaData meta = h2Conn.getMetaData();
            List<String> tableNames = new ArrayList<>();
            try (ResultSet rs = meta.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String table = rs.getString("TABLE_NAME");
                    tableNames.add(table);
                }
            }

            for (String table : tableNames) {
                migrateTable(h2Conn, sqliteConn, table);
            }

            logger.info("Successfully migrated all tables from legacy " + h2File.getName() + " to SQLite");
        } catch (Exception e) {
            logger.error("Migration failed for " + h2File.getName() + ": " + e.getMessage(), e);
        } finally {
            // Rename to .migrated so it is never processed again
            File migratedFile = new File(BASE_DIR, dbPrefix + ".mv.db.migrated");
            if (h2File.exists()) {
                if (migratedFile.exists()) migratedFile.delete();
                h2File.renameTo(migratedFile);
            }
        }
    }

    private static void migrateTable(Connection h2Conn, Connection sqliteConn, String tableName) {
        try {
            // Read columns
            List<String> columns = new ArrayList<>();
            DatabaseMetaData meta = h2Conn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, tableName, "%")) {
                while (rs.next()) {
                    columns.add(rs.getString("COLUMN_NAME"));
                }
            }

            if (columns.isEmpty()) return;

            StringBuilder insertSql = new StringBuilder();
            insertSql.append("INSERT OR IGNORE INTO \"").append(tableName).append("\" (");
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) {
                    insertSql.append(", ");
                    placeholders.append(", ");
                }
                insertSql.append("\"").append(columns.get(i)).append("\"");
                placeholders.append("?");
            }
            insertSql.append(") VALUES (").append(placeholders).append(")");

            int migratedRows = 0;
            try (Statement selectStmt = h2Conn.createStatement();
                 ResultSet rs = selectStmt.executeQuery("SELECT * FROM \"" + tableName + "\"");
                 PreparedStatement insertStmt = sqliteConn.prepareStatement(insertSql.toString())) {

                sqliteConn.setAutoCommit(false);
                while (rs.next()) {
                    for (int i = 0; i < columns.size(); i++) {
                        insertStmt.setObject(i + 1, rs.getObject(i + 1));
                    }
                    insertStmt.addBatch();
                    migratedRows++;
                    if (migratedRows % 500 == 0) {
                        insertStmt.executeBatch();
                    }
                }
                insertStmt.executeBatch();
                sqliteConn.commit();
                sqliteConn.setAutoCommit(true);
            }

            logger.info("Table " + tableName + " migrated (" + migratedRows + " rows)");
        } catch (Exception e) {
            logger.warn("Could not migrate table " + tableName + ": " + e.getMessage());
        }
    }
}
