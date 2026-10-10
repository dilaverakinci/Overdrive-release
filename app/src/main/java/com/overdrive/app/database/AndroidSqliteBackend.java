package com.overdrive.app.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.overdrive.app.logging.DaemonLogger;

import java.io.File;

/**
 * Native Android SQLite engine for production runtime.
 * Uses {@link SQLiteDatabase} built into Android OS with Zero-Context and WAL mode.
 */
public class AndroidSqliteBackend implements SqliteBackend {

    private static final String TAG = "AndroidSqliteBackend";
    private static final DaemonLogger logger = DaemonLogger.getInstance(TAG);

    private final String dbPath;
    private volatile SQLiteDatabase database;

    public AndroidSqliteBackend(String dbPath) {
        this.dbPath = dbPath;
    }

    public SQLiteDatabase getRawDatabase() {
        if (!isOpen() && !open()) {
            throw new IllegalStateException("Android SQLite database is not open: " + dbPath);
        }
        return database;
    }

    @Override
    public synchronized boolean open() {
        if (isOpen()) return true;
        try {
            File dbFile = new File(dbPath);
            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            logger.info("Opening Android SQLite database at: " + dbPath);
            database = SQLiteDatabase.openOrCreateDatabase(dbFile, null);

            // Configure automotive WAL tuning
            database.enableWriteAheadLogging();
            database.execSQL("PRAGMA synchronous = NORMAL;");
            database.execSQL("PRAGMA busy_timeout = 5000;");
            database.execSQL("PRAGMA foreign_keys = ON;");
            database.execSQL("PRAGMA temp_store = MEMORY;");
            database.execSQL("PRAGMA cache_size = -4000;"); // 4MB cache

            return true;
        } catch (Throwable t) {
            logger.error("Failed to open Android SQLite database at " + dbPath + ": " + t.getMessage(), t);
            close();
            return false;
        }
    }

    @Override
    public synchronized boolean isOpen() {
        try {
            return database != null && database.isOpen();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public synchronized void close() {
        SQLiteDatabase db = database;
        database = null;
        if (db != null) {
            try {
                db.close();
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void execSQL(String sql) {
        getRawDatabase().execSQL(sql);
    }

    @Override
    public void execSQL(String sql, Object[] bindArgs) {
        getRawDatabase().execSQL(sql, bindArgs);
    }

    @Override
    public Cursor rawQuery(String sql, String[] selectionArgs) {
        return getRawDatabase().rawQuery(sql, selectionArgs);
    }

    @Override
    public void beginTransaction() {
        getRawDatabase().beginTransaction();
    }

    @Override
    public void setTransactionSuccessful() {
        getRawDatabase().setTransactionSuccessful();
    }

    @Override
    public void endTransaction() {
        getRawDatabase().endTransaction();
    }

    @Override
    public boolean inTransaction() {
        return getRawDatabase().inTransaction();
    }
}
