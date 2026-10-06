package com.overdrive.app.database;

import com.overdrive.app.logging.DaemonLogger;
import com.overdrive.app.util.DaemonStorage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * H2ToSqliteMigrator — Automated, resilient one-shot data migration from legacy H2
 * databases to {@link OverdriveSqliteMaster}.
 *
 * <p>Safeguards:
 * <ul>
 *   <li>Runs asynchronously in background to ensure zero startup latency.</li>
 *   <li>Checks for the existence of legacy {@code .mv.db} files before doing any work.</li>
 *   <li>Transfers rows inside batch transactions.</li>
 *   <li>Renames migrated H2 files to {@code .mv.db.migrated} so migration runs strictly once.</li>
 *   <li>Catches all errors gracefully without crashing the master database or daemon.</li>
 * </ul>
 */
public final class H2ToSqliteMigrator {

    private static final String TAG = "H2ToSqliteMigrator";
    private static final DaemonLogger logger = DaemonLogger.getInstance(TAG);

    private static final ExecutorService MIGRATION_EXECUTOR =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "OverdriveH2MigrationWorker");
                t.setDaemon(true);
                return t;
            });

    private static final String[] LEGACY_H2_BASES = {
        "/data/local/tmp/overdrive_parking_h2",
        "/data/local/tmp/overdrive_notif_h2",
        "/data/local/tmp/overdrive_data_usage_h2",
        "/data/local/tmp/overdrive_recordings_h2",
        "/data/local/tmp/overdrive_trips_h2",
        "/data/local/tmp/overdrive_soc_h2"
    };

    private H2ToSqliteMigrator() {}

    public static void checkAndMigrateAsync(OverdriveSqliteMaster master) {
        MIGRATION_EXECUTOR.submit(() -> {
            try {
                performMigration(master);
            } catch (Throwable t) {
                logger.error("H2 to SQLite migration encounter error: " + t.getMessage(), t);
            }
        });
    }

    private static void performMigration(OverdriveSqliteMaster master) {
        boolean hasLegacyFiles = false;
        for (String base : LEGACY_H2_BASES) {
            String path = DaemonStorage.rebase(base);
            File mvFile = new File(path + ".mv.db");
            if (mvFile.exists()) {
                hasLegacyFiles = true;
                migrateH2Store(master, path, mvFile);
            }
        }
        if (!hasLegacyFiles) {
            logger.info("No legacy H2 databases detected. Migration not required.");
        }
    }

    private static void migrateH2Store(OverdriveSqliteMaster master, String dbBasePath, File mvFile) {
        logger.info("Migrating legacy H2 store: " + mvFile.getName() + " (" + (mvFile.length() / 1024) + " KB)...");

        // Load H2 driver
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            logger.warn("H2 Driver not found on classpath; skipping migration for " + mvFile.getName());
            return;
        }

        String jdbcUrl = "jdbc:h2:file:" + dbBasePath + ";FILE_LOCK=SOCKET;TRACE_LEVEL_FILE=0;DB_CLOSE_ON_EXIT=FALSE";
        try (Connection h2Conn = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            try (Statement st = h2Conn.createStatement()) {
                // Discover all user tables
                try (ResultSet tablesRs = st.executeQuery(
                        "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
                        "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE'")) {

                    while (tablesRs.next()) {
                        String tableName = tablesRs.getString("TABLE_NAME").toLowerCase();
                        migrateTable(master, h2Conn, tableName);
                    }
                }
            }

            // Close connection cleanly
            h2Conn.close();

            // Rename .mv.db to .mv.db.migrated
            File migratedMarker = new File(mvFile.getAbsolutePath() + ".migrated");
            if (mvFile.renameTo(migratedMarker)) {
                logger.info("Successfully archived " + mvFile.getName() + " -> " + migratedMarker.getName());
            }

            // Also clean up any orphan .lock.db or .trace.db
            File lockFile = new File(dbBasePath + ".lock.db");
            if (lockFile.exists()) lockFile.delete();
            File traceFile = new File(dbBasePath + ".trace.db");
            if (traceFile.exists()) traceFile.delete();

        } catch (Throwable t) {
            logger.error("Failed to migrate H2 store at " + dbBasePath + ": " + t.getMessage(), t);
        }
    }

    private static void migrateTable(OverdriveSqliteMaster master, Connection h2Conn, String tableName) {
        String selectSql = "SELECT * FROM " + tableName;
        try (Statement st = h2Conn.createStatement();
             ResultSet rs = st.executeQuery(selectSql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            if (columnCount == 0) return;

            StringBuilder insertSql = new StringBuilder("INSERT OR IGNORE INTO ")
                    .append(tableName)
                    .append(" (");
            StringBuilder placeholders = new StringBuilder();

            for (int i = 1; i <= columnCount; i++) {
                if (i > 1) {
                    insertSql.append(", ");
                    placeholders.append(", ");
                }
                insertSql.append(meta.getColumnName(i));
                placeholders.append("?");
            }
            insertSql.append(") VALUES (").append(placeholders).append(")");

            master.beginTransaction();
            int rowCount = 0;
            try {
                android.database.sqlite.SQLiteStatement stmt =
                        master.compileStatement(insertSql.toString());

                while (rs.next()) {
                    stmt.clearBindings();
                    for (int i = 1; i <= columnCount; i++) {
                        Object val = rs.getObject(i);
                        if (val == null) {
                            stmt.bindNull(i);
                        } else if (val instanceof Number) {
                            if (val instanceof Double || val instanceof Float) {
                                stmt.bindDouble(i, ((Number) val).doubleValue());
                            } else {
                                stmt.bindLong(i, ((Number) val).longValue());
                            }
                        } else if (val instanceof Boolean) {
                            stmt.bindLong(i, ((Boolean) val) ? 1L : 0L);
                        } else if (val instanceof byte[]) {
                            stmt.bindBlob(i, (byte[]) val);
                        } else {
                            stmt.bindString(i, val.toString());
                        }
                    }
                    stmt.executeInsert();
                    rowCount++;
                }
                master.setTransactionSuccessful();
                logger.info("Migrated " + rowCount + " rows for table [" + tableName + "] into SQLite.");
            } finally {
                master.endTransaction();
            }

        } catch (Throwable t) {
            logger.warn("Could not migrate table " + tableName + ": " + t.getMessage());
        }
    }
}
