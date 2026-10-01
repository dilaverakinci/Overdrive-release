package com.overdrive.app.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.overdrive.app.logging.DaemonLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * High-performance, robust SQLite storage engine designed for automotive stability.
 * 
 * Key Architectural Features:
 * 1. Zero-Context Independence: Operates cleanly in both standard Android App (UID 10xxx)
 *    and background daemon (UID 2000 / app_process) environments.
 * 2. Write-Ahead Logging (WAL): Enabled via PRAGMA journal_mode=WAL. Prevents corruption
 *    during abrupt 12V / ignition power cutoffs and allows non-blocking concurrent reads.
 * 3. eMMC Flash Protection: Uses PRAGMA synchronous=NORMAL and PRAGMA temp_store=MEMORY
 *    to minimize physical writes and prevent vehicle flash storage wear-out.
 * 4. Cross-UID Permissions: Automatically sets POSIX read/write permissions for shared
 *    /data/local/tmp/ operation across daemon and UI processes.
 * 5. Thread-safe: Uses ReentrantReadWriteLock to coordinate concurrent multi-thread access.
 */
public class SqliteStorageEngine implements AutoCloseable {

    private static final String TAG = "SqliteStorageEngine";
    private static final DaemonLogger logger = DaemonLogger.getInstance(TAG);

    @FunctionalInterface
    public interface RowMapper<T> {
        T mapRow(Cursor cursor) throws Exception;
    }

    @FunctionalInterface
    public interface TransactionCallback<T> {
        T execute(SQLiteDatabase db) throws Exception;
    }

    private final File dbFile;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private volatile SQLiteDatabase database;

    /**
     * Checks whether we are executing in a real Android runtime (app or daemon) vs host JVM unit test.
     */
    public static boolean isAndroidRuntime() {
        try {
            Class.forName("android.database.sqlite.SQLiteDatabase");
            return System.getProperty("java.vendor", "").contains("Android")
                    || System.getProperty("java.vm.vendor", "").contains("The Android Project")
                    || new File("/system/build.prop").exists();
        } catch (Throwable t) {
            return false;
        }
    }

    public SqliteStorageEngine(String dbPath) {
        this.dbFile = new File(dbPath);
        initDatabase();
    }

    public SqliteStorageEngine(File dbFile) {
        this.dbFile = dbFile;
        initDatabase();
    }

    /**
     * Initializes the SQLite database and configures automotive WAL settings.
     */
    private synchronized void initDatabase() {
        if (database != null && database.isOpen()) {
            return;
        }

        try {
            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
                ensureWorldPermissions(parentDir);
            }

            database = SQLiteDatabase.openOrCreateDatabase(dbFile, null);

            // Configure optimal automotive WAL parameters
            database.enableWriteAheadLogging();
            database.execSQL("PRAGMA synchronous = NORMAL;");
            database.execSQL("PRAGMA temp_store = MEMORY;");
            database.execSQL("PRAGMA cache_size = -4000;"); // ~4MB RAM cache
            database.execSQL("PRAGMA busy_timeout = 5000;"); // 5s retry on contention

            ensureWorldPermissions(dbFile);
            ensureAuxiliaryFilesPermissions();

            logger.info("SqliteStorageEngine initialized successfully at: " + dbFile.getAbsolutePath() + " (WAL enabled)");
        } catch (Exception e) {
            logger.error("Failed to initialize SqliteStorageEngine at " + dbFile.getAbsolutePath(), e);
            throw new RuntimeException("SQLite initialization failed for: " + dbFile.getAbsolutePath(), e);
        }
    }

    /**
     * Sets world read/write permissions to allow access across UID 2000 (daemon) and UID 10xxx (app).
     */
    private void ensureWorldPermissions(File file) {
        if (file != null && file.exists()) {
            try {
                file.setReadable(true, false);
                file.setWritable(true, false);
                file.setExecutable(file.isDirectory(), false);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Checks and sets permissions on SQLite WAL and SHM auxiliary files if they exist.
     */
    private void ensureAuxiliaryFilesPermissions() {
        File wal = new File(dbFile.getAbsolutePath() + "-wal");
        File shm = new File(dbFile.getAbsolutePath() + "-shm");
        if (wal.exists()) ensureWorldPermissions(wal);
        if (shm.exists()) ensureWorldPermissions(shm);
    }

    private void checkOpen() {
        if (database == null || !database.isOpen()) {
            synchronized (this) {
                if (database == null || !database.isOpen()) {
                    initDatabase();
                }
            }
        }
    }

    public SQLiteDatabase getRawDatabase() {
        checkOpen();
        return database;
    }

    public String getPath() {
        return dbFile.getAbsolutePath();
    }

    public boolean isOpen() {
        return database != null && database.isOpen();
    }

    // ==================== EXECUTION HELPERS ====================

    /**
     * Executes a non-query DDL or DML statement under a write lock.
     */
    public void execSQL(String sql) {
        rwLock.writeLock().lock();
        try {
            checkOpen();
            database.execSQL(sql);
            ensureAuxiliaryFilesPermissions();
        } catch (SQLiteException e) {
            logger.error("execSQL failed: " + sql, e);
            throw e;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Executes a parameterized statement under a write lock.
     */
    public void execSQL(String sql, Object[] bindArgs) {
        rwLock.writeLock().lock();
        try {
            checkOpen();
            database.execSQL(sql, bindArgs);
            ensureAuxiliaryFilesPermissions();
        } catch (SQLiteException e) {
            logger.error("execSQL with args failed: " + sql, e);
            throw e;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Inserts a row into the specified table.
     */
    public long insert(String table, ContentValues values) {
        rwLock.writeLock().lock();
        try {
            checkOpen();
            long rowId = database.insert(table, null, values);
            ensureAuxiliaryFilesPermissions();
            return rowId;
        } catch (SQLiteException e) {
            logger.error("insert failed on table: " + table, e);
            throw e;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Inserts or replaces a row into the specified table.
     */
    public long insertWithOnConflict(String table, ContentValues values, int conflictAlgorithm) {
        rwLock.writeLock().lock();
        try {
            checkOpen();
            long rowId = database.insertWithOnConflict(table, null, values, conflictAlgorithm);
            ensureAuxiliaryFilesPermissions();
            return rowId;
        } catch (SQLiteException e) {
            logger.error("insertWithOnConflict failed on table: " + table, e);
            throw e;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Updates rows in the specified table.
     */
    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        rwLock.writeLock().lock();
        try {
            checkOpen();
            int count = database.update(table, values, whereClause, whereArgs);
            ensureAuxiliaryFilesPermissions();
            return count;
        } catch (SQLiteException e) {
            logger.error("update failed on table: " + table, e);
            throw e;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Deletes rows from the specified table.
     */
    public int delete(String table, String whereClause, String[] whereArgs) {
        rwLock.writeLock().lock();
        try {
            checkOpen();
            int count = database.delete(table, whereClause, whereArgs);
            ensureAuxiliaryFilesPermissions();
            return count;
        } catch (SQLiteException e) {
            logger.error("delete failed on table: " + table, e);
            throw e;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    // ==================== QUERY HELPERS ====================

    /**
     * Executes a raw query with a read lock and maps each row.
     */
    public <T> List<T> query(String sql, String[] selectionArgs, RowMapper<T> mapper) {
        rwLock.readLock().lock();
        Cursor cursor = null;
        try {
            checkOpen();
            cursor = database.rawQuery(sql, selectionArgs);
            List<T> results = new ArrayList<>();
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    results.add(mapper.mapRow(cursor));
                } while (cursor.moveToNext());
            }
            return results;
        } catch (Exception e) {
            logger.error("query failed: " + sql, e);
            throw new RuntimeException("Query execution failed: " + sql, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            rwLock.readLock().unlock();
        }
    }

    /**
     * Executes a query expecting a single row (or null if none found).
     */
    public <T> T queryOne(String sql, String[] selectionArgs, RowMapper<T> mapper) {
        rwLock.readLock().lock();
        Cursor cursor = null;
        try {
            checkOpen();
            cursor = database.rawQuery(sql, selectionArgs);
            if (cursor != null && cursor.moveToFirst()) {
                return mapper.mapRow(cursor);
            }
            return null;
        } catch (Exception e) {
            logger.error("queryOne failed: " + sql, e);
            throw new RuntimeException("QueryOne execution failed: " + sql, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            rwLock.readLock().unlock();
        }
    }

    /**
     * Returns a count or long scalar result from a query.
     */
    public long queryLong(String sql, String[] selectionArgs, long defaultValue) {
        rwLock.readLock().lock();
        Cursor cursor = null;
        try {
            checkOpen();
            cursor = database.rawQuery(sql, selectionArgs);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
            return defaultValue;
        } catch (Exception e) {
            logger.error("queryLong failed: " + sql, e);
            return defaultValue;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            rwLock.readLock().unlock();
        }
    }

    // ==================== TRANSACTIONS ====================

    /**
     * Executes a callback within a managed database transaction.
     */
    public <T> T inTransaction(TransactionCallback<T> callback) {
        rwLock.writeLock().lock();
        checkOpen();
        database.beginTransaction();
        try {
            T result = callback.execute(database);
            database.setTransactionSuccessful();
            return result;
        } catch (Exception e) {
            logger.error("Transaction failed and rolled back", e);
            throw new RuntimeException("Transaction rolled back", e);
        } finally {
            try {
                database.endTransaction();
                ensureAuxiliaryFilesPermissions();
            } finally {
                rwLock.writeLock().unlock();
            }
        }
    }

    /**
     * Executes a runnable within a managed database transaction.
     */
    public void inTransaction(Runnable runnable) {
        inTransaction(db -> {
            runnable.run();
            return null;
        });
    }

    // ==================== LIFECYCLE ====================

    @Override
    public synchronized void close() {
        rwLock.writeLock().lock();
        try {
            if (database != null && database.isOpen()) {
                database.close();
                database = null;
                logger.info("SqliteStorageEngine closed for: " + dbFile.getAbsolutePath());
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
