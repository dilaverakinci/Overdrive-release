package com.overdrive.app.database;

import com.overdrive.app.logging.DaemonLogger;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry and lifecycle manager for all OverDrive SQLite WAL storage engines.
 */
public final class SqliteDatabaseManager {

    private static final String TAG = "SqliteDatabaseManager";
    private static final DaemonLogger logger = DaemonLogger.getInstance(TAG);

    private static final String DEFAULT_BASE_DIR = "/data/local/tmp";
    private static final Map<String, SqliteStorageEngine> engines = new ConcurrentHashMap<>();

    // Well-known database names
    public static final String DB_NOTIFICATIONS = "overdrive_notifications";
    public static final String DB_PARKING = "overdrive_parking";
    public static final String DB_SOC = "overdrive_soc";
    public static final String DB_TRIPS = "overdrive_trips";
    public static final String DB_RECORDINGS = "overdrive_recordings";
    public static final String DB_DATA_USAGE = "overdrive_data_usage";

    private SqliteDatabaseManager() {}

    /**
     * Obtains or creates a SqliteStorageEngine for the specified database name in /data/local/tmp/.
     */
    public static SqliteStorageEngine getDatabase(String name) {
        return getDatabase(DEFAULT_BASE_DIR, name);
    }

    /**
     * Obtains or creates a SqliteStorageEngine for the specified directory and database name.
     */
    public static SqliteStorageEngine getDatabase(String baseDir, String name) {
        String key = new File(baseDir, name + ".db").getAbsolutePath();
        return engines.computeIfAbsent(key, k -> {
            logger.info("Opening SQLite WAL database: " + k);
            return new SqliteStorageEngine(k);
        });
    }

    /**
     * Obtains a JDBC Connection. On Android devices, returns a zero-overhead
     * native SQLite WAL connection via SqliteJdbcBridge. On host JVM test runners,
     * returns an in-memory test connection.
     */
    public static Connection getJdbcConnection(String name) {
        if (SqliteStorageEngine.isAndroidRuntime()) {
            SqliteStorageEngine engine = getDatabase(name);
            return SqliteJdbcBridge.wrap(engine);
        } else {
            try {
                Class.forName("org.h2.Driver");
                return DriverManager.getConnection("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1", "sa", "");
            } catch (Exception e) {
                logger.error("Failed to establish in-memory test JDBC connection for " + name, e);
                throw new RuntimeException("Test JDBC connection failed for " + name, e);
            }
        }
    }

    /**
     * Closes and unregisters a specific database engine.
     */
    public static void closeDatabase(String name) {
        String key = new File(DEFAULT_BASE_DIR, name + ".db").getAbsolutePath();
        SqliteStorageEngine engine = engines.remove(key);
        if (engine != null) {
            engine.close();
        }
    }

    /**
     * Closes all active SQLite database engines cleanly. Called on daemon shutdown.
     */
    public static void closeAll() {
        logger.info("Closing all SQLite database engines (" + engines.size() + " active)");
        for (Map.Entry<String, SqliteStorageEngine> entry : engines.entrySet()) {
            try {
                entry.getValue().close();
            } catch (Exception e) {
                logger.error("Error closing database: " + entry.getKey(), e);
            }
        }
        engines.clear();
    }
}
