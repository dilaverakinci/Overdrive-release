package com.overdrive.app.database;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.overdrive.app.logging.DaemonLogger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance, zero-overhead JDBC-to-SQLite bridge.
 * 
 * Enables complex telemetry, trip, and recording databases (totaling over 20,000 lines
 * of robust JDBC queries) to execute natively on Android's C-based SQLite WAL engine
 * without requiring rewriting of tested query logic or breaking contract tests.
 */
public final class SqliteJdbcBridge {

    private static final String TAG = "SqliteJdbcBridge";
    private static final DaemonLogger logger = DaemonLogger.getInstance(TAG);

    private static final Pattern PATTERN_IDENTITY = Pattern.compile("(?i)\\bIDENTITY\\s+PRIMARY\\s+KEY\\b");
    private static final Pattern PATTERN_MERGE_VALUES = Pattern.compile("(?i)\\bMERGE\\s+INTO\\s+([a-zA-Z0-9_\"]+)\\s*\\(([^)]+)\\)\\s*KEY\\s*\\([^)]+\\)\\s*VALUES");
    private static final Pattern PATTERN_MERGE_SELECT = Pattern.compile("(?i)\\bMERGE\\s+INTO\\s+([a-zA-Z0-9_\"]+)\\s*\\(([^)]+)\\)\\s*KEY\\s*\\([^)]+\\)\\s*SELECT");
    private static final Pattern PATTERN_MERGE_NOCOLS = Pattern.compile("(?i)\\bMERGE\\s+INTO\\s+([a-zA-Z0-9_\"]+)\\s*KEY\\s*\\([^)]+\\)\\s*VALUES");
    private static final Pattern PATTERN_ALTER_ADD_COLUMN = Pattern.compile("(?i)\\bALTER\\s+TABLE\\s+([a-zA-Z0-9_\"]+)\\s+ADD\\s+COLUMN\\s+IF\\s+NOT\\s+EXISTS\\b");

    private SqliteJdbcBridge() {}

    /**
     * Translates H2 SQL dialects to native SQLite syntax.
     */
    public static String translateSql(String sql) {
        if (sql == null) return null;
        String s = sql.trim();

        if (s.regionMatches(true, 0, "SET CACHE_SIZE", 0, 14)) {
            return "PRAGMA cache_size = -4000;";
        }
        if (s.regionMatches(true, 0, "SET WRITE_DELAY", 0, 15)) {
            return "PRAGMA synchronous = NORMAL;";
        }

        s = PATTERN_IDENTITY.matcher(s).replaceAll("INTEGER PRIMARY KEY AUTOINCREMENT");
        s = PATTERN_MERGE_VALUES.matcher(s).replaceAll("INSERT OR REPLACE INTO $1 ($2) VALUES");
        s = PATTERN_MERGE_SELECT.matcher(s).replaceAll("INSERT OR REPLACE INTO $1 ($2) SELECT");
        s = PATTERN_MERGE_NOCOLS.matcher(s).replaceAll("INSERT OR REPLACE INTO $1 VALUES");
        s = PATTERN_ALTER_ADD_COLUMN.matcher(s).replaceAll("ALTER TABLE $1 ADD COLUMN");

        return s;
    }

    /**
     * Wraps a SqliteStorageEngine into a java.sql.Connection proxy.
     */
    public static Connection wrap(SqliteStorageEngine engine) {
        InvocationHandler handler = new ConnectionInvocationHandler(engine);
        return (Connection) Proxy.newProxyInstance(
                SqliteJdbcBridge.class.getClassLoader(),
                new Class<?>[]{ Connection.class },
                handler
        );
    }

    // ==================== CONNECTION HANDLER ====================

    private static class ConnectionInvocationHandler implements InvocationHandler {
        private final SqliteStorageEngine engine;
        private volatile boolean autoCommit = true;

        ConnectionInvocationHandler(SqliteStorageEngine engine) {
            this.engine = engine;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            SQLiteDatabase db = engine.getDatabase();

            switch (name) {
                case "createStatement":
                    return createStatementProxy((Connection) proxy, engine, db);

                case "prepareStatement": {
                    String sql = (String) args[0];
                    return createPreparedStatementProxy((Connection) proxy, engine, db, sql);
                }

                case "setAutoCommit": {
                    boolean newAutoCommit = (Boolean) args[0];
                    if (this.autoCommit && !newAutoCommit) {
                        if (!db.inTransaction()) {
                            db.beginTransaction();
                        }
                    } else if (!this.autoCommit && newAutoCommit) {
                        if (db.inTransaction()) {
                            db.setTransactionSuccessful();
                            db.endTransaction();
                        }
                    }
                    this.autoCommit = newAutoCommit;
                    return null;
                }

                case "getAutoCommit":
                    return autoCommit;

                case "commit":
                    if (db.inTransaction()) {
                        db.setTransactionSuccessful();
                        db.endTransaction();
                        if (!autoCommit) {
                            db.beginTransaction();
                        }
                    }
                    return null;

                case "rollback":
                    if (db.inTransaction()) {
                        db.endTransaction();
                        if (!autoCommit) {
                            db.beginTransaction();
                        }
                    }
                    return null;

                case "close":
                    // Connection lifetime managed by SqliteDatabaseManager
                    return null;

                case "isClosed":
                    return db == null || !db.isOpen();

                case "isValid":
                    return db != null && db.isOpen();

                case "getMetaData":
                    return createDatabaseMetaDataProxy((Connection) proxy, db);

                case "equals":
                    return proxy == args[0];

                case "hashCode":
                    return System.identityHashCode(proxy);

                case "toString":
                    return "SqliteJdbcConnection[" + engine.getPath() + "]";

                default:
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) return false;
                    if (returnType == int.class) return 0;
                    if (returnType == long.class) return 0L;
                    return null;
            }
        }
    }

    // ==================== STATEMENT / PREPARED STATEMENT ====================

    private static Statement createStatementProxy(Connection connection, SqliteStorageEngine engine, SQLiteDatabase db) {
        StatementInvocationHandler handler = new StatementInvocationHandler(connection, engine, db, null);
        return (Statement) Proxy.newProxyInstance(
                SqliteJdbcBridge.class.getClassLoader(),
                new Class<?>[]{ Statement.class },
                handler
        );
    }

    private static PreparedStatement createPreparedStatementProxy(Connection connection, SqliteStorageEngine engine, SQLiteDatabase db, String sql) {
        StatementInvocationHandler handler = new StatementInvocationHandler(connection, engine, db, sql);
        return (PreparedStatement) Proxy.newProxyInstance(
                SqliteJdbcBridge.class.getClassLoader(),
                new Class<?>[]{ PreparedStatement.class },
                handler
        );
    }

    private static class StatementInvocationHandler implements InvocationHandler {
        private final Connection connection;
        private final SqliteStorageEngine engine;
        private final SQLiteDatabase db;
        private final String originalSql;
        private final String translatedSql;

        private final Map<Integer, Object> params = new ConcurrentHashMap<>();
        private final List<Object[]> batchArgs = Collections.synchronizedList(new ArrayList<>());
        private volatile ResultSet currentResultSet;
        private volatile int updateCount = -1;
        private volatile long lastInsertRowId = -1;
        private volatile boolean closed = false;

        StatementInvocationHandler(Connection connection, SqliteStorageEngine engine, SQLiteDatabase db, String sql) {
            this.connection = connection;
            this.engine = engine;
            this.db = db;
            this.originalSql = sql;
            this.translatedSql = translateSql(sql);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();

            // Parameter setters for PreparedStatement
            if (name.startsWith("set") && args != null && args.length >= 2 && args[0] instanceof Integer) {
                int index = (Integer) args[0];
                Object val = args[1];
                if ("setBoolean".equals(name)) {
                    params.put(index, Boolean.TRUE.equals(val) ? 1L : 0L);
                } else if ("setNull".equals(name)) {
                    params.put(index, null);
                } else {
                    params.put(index, val);
                }
                return null;
            }

            switch (name) {
                case "clearParameters":
                    params.clear();
                    return null;

                case "executeQuery": {
                    String querySql = (args != null && args.length > 0 && args[0] instanceof String)
                            ? translateSql((String) args[0])
                            : this.translatedSql;
                    return executeQueryInternal(querySql);
                }

                case "executeUpdate": {
                    String execSql = (args != null && args.length > 0 && args[0] instanceof String)
                            ? translateSql((String) args[0])
                            : this.translatedSql;
                    return executeUpdateInternal(execSql);
                }

                case "execute": {
                    String execSql = (args != null && args.length > 0 && args[0] instanceof String)
                            ? translateSql((String) args[0])
                            : this.translatedSql;
                    if (execSql.trim().toUpperCase(Locale.US).startsWith("SELECT")) {
                        currentResultSet = executeQueryInternal(execSql);
                        updateCount = -1;
                        return true;
                    } else {
                        updateCount = executeUpdateInternal(execSql);
                        currentResultSet = null;
                        return false;
                    }
                }

                case "getResultSet":
                    return currentResultSet;

                case "getUpdateCount":
                    return updateCount;

                case "getGeneratedKeys":
                    return createGeneratedKeysResultSet(lastInsertRowId);

                case "addBatch":
                    if (args != null && args.length > 0 && args[0] instanceof String) {
                        batchArgs.add(new Object[]{ translateSql((String) args[0]) });
                    } else {
                        batchArgs.add(buildBindArgs());
                    }
                    return null;

                case "executeBatch": {
                    int[] results = new int[batchArgs.size()];
                    boolean inOurTx = !db.inTransaction();
                    if (inOurTx) db.beginTransaction();
                    try {
                        for (int i = 0; i < batchArgs.size(); i++) {
                            Object[] bArgs = batchArgs.get(i);
                            try {
                                if (bArgs.length > 0) {
                                    db.execSQL(translatedSql, bArgs);
                                } else {
                                    db.execSQL(translatedSql);
                                }
                                results[i] = 1;
                            } catch (Exception e) {
                                results[i] = Statement.EXECUTE_FAILED;
                            }
                        }
                        if (inOurTx) db.setTransactionSuccessful();
                    } finally {
                        if (inOurTx) db.endTransaction();
                        batchArgs.clear();
                    }
                    return results;
                }

                case "clearBatch":
                    batchArgs.clear();
                    return null;

                case "close":
                    closed = true;
                    if (currentResultSet != null) {
                        try { currentResultSet.close(); } catch (Exception ignored) {}
                        currentResultSet = null;
                    }
                    return null;

                case "isClosed":
                    return closed;

                case "getConnection":
                    return connection;

                case "equals":
                    return proxy == args[0];

                case "hashCode":
                    return System.identityHashCode(proxy);

                default:
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) return false;
                    if (returnType == int.class) return 0;
                    if (returnType == long.class) return 0L;
                    return null;
            }
        }

        private Object[] buildBindArgs() {
            int maxIdx = 0;
            for (Integer k : params.keySet()) {
                if (k > maxIdx) maxIdx = k;
            }
            Object[] bindArgs = new Object[maxIdx];
            for (int i = 1; i <= maxIdx; i++) {
                bindArgs[i - 1] = params.get(i);
            }
            return bindArgs;
        }

        private String[] buildSelectionArgs() {
            int maxIdx = 0;
            for (Integer k : params.keySet()) {
                if (k > maxIdx) maxIdx = k;
            }
            if (maxIdx == 0) return null;
            String[] selectionArgs = new String[maxIdx];
            for (int i = 1; i <= maxIdx; i++) {
                Object val = params.get(i);
                selectionArgs[i - 1] = (val == null) ? null : String.valueOf(val);
            }
            return selectionArgs;
        }

        private ResultSet executeQueryInternal(String sql) {
            String[] selectionArgs = buildSelectionArgs();
            Cursor cursor = db.rawQuery(sql, selectionArgs);
            currentResultSet = createResultSetProxy(cursor);
            return currentResultSet;
        }

        private int executeUpdateInternal(String sql) throws SQLException {
            if (sql != null && sql.trim().toUpperCase(Locale.US).startsWith("PRAGMA")) {
                try (Cursor c = db.rawQuery(sql, null)) {
                    if (c != null) c.moveToFirst();
                } catch (Exception ignored) {}
                return 0;
            }
            Object[] bindArgs = buildBindArgs();
            try {
                if (bindArgs.length > 0) {
                    db.execSQL(sql, bindArgs);
                } else {
                    db.execSQL(sql);
                }
            } catch (SQLiteException e) {
                // If it's ALTER TABLE ADD COLUMN and column already exists, ignore (IF NOT EXISTS semantics)
                if (sql.toUpperCase(Locale.US).contains("ADD COLUMN") &&
                        e.getMessage() != null && e.getMessage().contains("duplicate column")) {
                    return 0;
                }
                throw new SQLException("SQLite execSQL failed: " + e.getMessage(), e);
            }

            int changes = 0;
            try (Cursor c = db.rawQuery("SELECT changes()", null)) {
                if (c.moveToFirst()) {
                    changes = c.getInt(0);
                }
            } catch (Exception ignored) {}

            try (Cursor c = db.rawQuery("SELECT last_insert_rowid()", null)) {
                if (c.moveToFirst()) {
                    lastInsertRowId = c.getLong(0);
                }
            } catch (Exception ignored) {}

            updateCount = changes;
            return changes;
        }
    }

    // ==================== RESULT SET ====================

    private static ResultSet createResultSetProxy(Cursor cursor) {
        ResultSetInvocationHandler handler = new ResultSetInvocationHandler(cursor);
        return (ResultSet) Proxy.newProxyInstance(
                SqliteJdbcBridge.class.getClassLoader(),
                new Class<?>[]{ ResultSet.class },
                handler
        );
    }

    private static ResultSet createGeneratedKeysResultSet(long rowId) {
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"ID"});
        if (rowId >= 0) {
            matrixCursor.addRow(new Object[]{ rowId });
        }
        return createResultSetProxy(matrixCursor);
    }

    private static class ResultSetInvocationHandler implements InvocationHandler {
        private final Cursor cursor;
        private volatile boolean wasNull = false;

        ResultSetInvocationHandler(Cursor cursor) {
            this.cursor = cursor;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();

            switch (name) {
                case "next":
                    return cursor != null && cursor.moveToNext();

                case "close":
                    if (cursor != null && !cursor.isClosed()) {
                        cursor.close();
                    }
                    return null;

                case "isClosed":
                    return cursor == null || cursor.isClosed();

                case "wasNull":
                    return wasNull;

                case "getString": {
                    int idx = resolveColumnIndex(args[0]);
                    if (idx < 0 || cursor.isNull(idx)) {
                        wasNull = true;
                        return null;
                    }
                    wasNull = false;
                    return cursor.getString(idx);
                }

                case "getInt": {
                    int idx = resolveColumnIndex(args[0]);
                    if (idx < 0 || cursor.isNull(idx)) {
                        wasNull = true;
                        return 0;
                    }
                    wasNull = false;
                    return cursor.getInt(idx);
                }

                case "getLong": {
                    int idx = resolveColumnIndex(args[0]);
                    if (idx < 0 || cursor.isNull(idx)) {
                        wasNull = true;
                        return 0L;
                    }
                    wasNull = false;
                    return cursor.getLong(idx);
                }

                case "getDouble": {
                    int idx = resolveColumnIndex(args[0]);
                    if (idx < 0 || cursor.isNull(idx)) {
                        wasNull = true;
                        return 0.0;
                    }
                    wasNull = false;
                    return cursor.getDouble(idx);
                }

                case "getFloat": {
                    int idx = resolveColumnIndex(args[0]);
                    if (idx < 0 || cursor.isNull(idx)) {
                        wasNull = true;
                        return 0.0f;
                    }
                    wasNull = false;
                    return cursor.getFloat(idx);
                }

                case "getBoolean": {
                    int idx = resolveColumnIndex(args[0]);
                    if (idx < 0 || cursor.isNull(idx)) {
                        wasNull = true;
                        return false;
                    }
                    wasNull = false;
                    return cursor.getInt(idx) != 0;
                }

                case "getBytes": {
                    int idx = resolveColumnIndex(args[0]);
                    if (idx < 0 || cursor.isNull(idx)) {
                        wasNull = true;
                        return null;
                    }
                    wasNull = false;
                    return cursor.getBlob(idx);
                }

                case "getObject": {
                    int idx = resolveColumnIndex(args[0]);
                    if (idx < 0 || cursor.isNull(idx)) {
                        wasNull = true;
                        return null;
                    }
                    wasNull = false;
                    return cursor.getString(idx);
                }

                case "findColumn": {
                    String colName = (String) args[0];
                    int idx = resolveColumnIndex(colName);
                    if (idx < 0) throw new SQLException("Column not found: " + colName);
                    return idx + 1; // 1-based index
                }

                case "getMetaData":
                    return createResultSetMetaDataProxy(cursor);

                case "equals":
                    return proxy == args[0];

                case "hashCode":
                    return System.identityHashCode(proxy);

                default:
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) return false;
                    if (returnType == int.class) return 0;
                    if (returnType == long.class) return 0L;
                    return null;
            }
        }

        private int resolveColumnIndex(Object colArg) {
            if (colArg instanceof Integer) {
                return ((Integer) colArg) - 1; // 1-indexed to 0-indexed
            }
            if (colArg instanceof String) {
                String name = (String) colArg;
                int idx = cursor.getColumnIndex(name);
                if (idx < 0) {
                    idx = cursor.getColumnIndex(name.toLowerCase(Locale.US));
                }
                if (idx < 0) {
                    idx = cursor.getColumnIndex(name.toUpperCase(Locale.US));
                }
                return idx;
            }
            return -1;
        }
    }

    // ==================== RESULT SET METADATA ====================

    private static ResultSetMetaData createResultSetMetaDataProxy(Cursor cursor) {
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            switch (name) {
                case "getColumnCount":
                    return cursor != null ? cursor.getColumnCount() : 0;
                case "getColumnName":
                case "getColumnLabel": {
                    int col = (Integer) args[0];
                    return cursor != null ? cursor.getColumnName(col - 1) : "";
                }
                case "getColumnType":
                    return Types.VARCHAR;
                case "getColumnTypeName":
                    return "VARCHAR";
                default:
                    return null;
            }
        };
        return (ResultSetMetaData) Proxy.newProxyInstance(
                SqliteJdbcBridge.class.getClassLoader(),
                new Class<?>[]{ ResultSetMetaData.class },
                handler
        );
    }

    // ==================== DATABASE METADATA ====================

    private static DatabaseMetaData createDatabaseMetaDataProxy(Connection connection, SQLiteDatabase db) {
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            switch (name) {
                case "getTables": {
                    String tableNamePattern = (args != null && args.length > 2 && args[2] != null) ? (String) args[2] : "%";
                    Cursor c = db.rawQuery(
                            "SELECT name AS TABLE_NAME FROM sqlite_master WHERE type='table' AND name LIKE ? COLLATE NOCASE",
                            new String[]{ tableNamePattern }
                    );
                    return createResultSetProxy(c);
                }

                case "getColumns": {
                    String table = (args != null && args.length > 2 && args[2] != null) ? (String) args[2] : "";
                    String colPattern = (args != null && args.length > 3 && args[3] != null) ? (String) args[3] : "%";
                    // PRAGMA table_info returns columns: cid, name, type, notnull, dflt_value, pk
                    Cursor c = db.rawQuery("PRAGMA table_info('" + table.replace("'", "''") + "')", null);
                    MatrixCursor result = new MatrixCursor(new String[]{"COLUMN_NAME", "TYPE_NAME"});
                    while (c.moveToNext()) {
                        String colName = c.getString(1); // column name
                        String typeName = c.getString(2); // type
                        if ("%".equals(colPattern) || colName.equalsIgnoreCase(colPattern)) {
                            result.addRow(new Object[]{ colName, typeName });
                        }
                    }
                    c.close();
                    return createResultSetProxy(result);
                }

                case "getDatabaseProductName":
                    return "SQLite";

                case "getDatabaseProductVersion":
                    return "3.x";

                case "getDriverName":
                    return "SqliteJdbcBridge";

                case "getDriverVersion":
                    return "1.0";

                case "getConnection":
                    return connection;

                default:
                    return null;
            }
        };
        return (DatabaseMetaData) Proxy.newProxyInstance(
                SqliteJdbcBridge.class.getClassLoader(),
                new Class<?>[]{ DatabaseMetaData.class },
                handler
        );
    }
}
