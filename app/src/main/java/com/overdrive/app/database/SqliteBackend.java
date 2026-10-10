package com.overdrive.app.database;

import android.database.Cursor;

/**
 * Pluggable backend for {@link OverdriveSqliteMaster}.
 * Allows seamless execution on native Android (via {@code android.database.sqlite.SQLiteDatabase})
 * and on host JVM unit tests (via SQLite JDBC in-memory).
 */
public interface SqliteBackend {
    boolean open();
    boolean isOpen();
    void close();
    void execSQL(String sql);
    void execSQL(String sql, Object[] bindArgs);
    Cursor rawQuery(String sql, String[] selectionArgs);
    void beginTransaction();
    void setTransactionSuccessful();
    void endTransaction();
    boolean inTransaction();
}
