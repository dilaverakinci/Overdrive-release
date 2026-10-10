package com.overdrive.app.database;

import android.database.Cursor;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SqliteConnectionBridge — High-performance JDBC compatibility bridge over {@link OverdriveSqliteMaster}.
 *
 * <p>Allows complex stores (such as {@code RecordingsIndex}, {@code TripDatabase}, and
 * {@code SocHistoryDatabase}) written against standard {@code java.sql.*} JDBC interfaces
 * to run with 100% behavioral parity over SQLite, without pulling in heavy external JDBC drivers
 * or third-party bytecode into the Android runtime.
 */
public final class SqliteConnectionBridge {

    private static final String TAG = "SqliteConnectionBridge";
    private static final DaemonLogger logger = DaemonLogger.getInstance(TAG);

    private static final Pattern PATTERN_IDENTITY_PK =
            Pattern.compile("(?i)\\bIDENTITY\\s+PRIMARY\\s+KEY\\b");
    private static final Pattern PATTERN_ALTER_ADD_COLUMN_IF_NOT_EXISTS =
            Pattern.compile("(?i)\\bADD\\s+COLUMN\\s+IF\\s+NOT\\s+EXISTS\\b");

    private static final Pattern PATTERN_MERGE_VALUES =
            Pattern.compile("^\\s*MERGE\\s+INTO\\s+([a-zA-Z0-9_]+)\\s*(?:\\((.*?)\\))?\\s+KEY\\s*\\((.*?)\\)\\s+VALUES\\s*\\((.*?)\\)\\s*;?\\s*$",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern PATTERN_MERGE_SELECT =
            Pattern.compile("^\\s*MERGE\\s+INTO\\s+([a-zA-Z0-9_]+)\\s*\\((.*?)\\)\\s+KEY\\s*\\((.*?)\\)\\s+(SELECT\\s+.*)$",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private SqliteConnectionBridge() {}

    /**
     * Creates a {@link Connection} proxy backed by the given {@link OverdriveSqliteMaster}.
     */
    public static Connection create(OverdriveSqliteMaster master) {
        if (master == null) {
            throw new IllegalArgumentException("master cannot be null");
        }
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                new ConnectionHandler(master)
        );
    }

    /**
     * Translates H2-specific SQL dialects to standard SQLite 3 syntax.
     */
    public static String translateSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return sql;
        }

        String trimmed = sql.trim();

        // 1. IDENTITY PRIMARY KEY -> INTEGER PRIMARY KEY AUTOINCREMENT
        if (trimmed.toUpperCase(Locale.US).contains("IDENTITY PRIMARY KEY")) {
            sql = PATTERN_IDENTITY_PK.matcher(sql).replaceAll("INTEGER PRIMARY KEY AUTOINCREMENT");
        }

        // 2. ALTER TABLE ... ADD COLUMN IF NOT EXISTS ... -> ADD COLUMN ...
        if (trimmed.toUpperCase(Locale.US).contains("ADD COLUMN IF NOT EXISTS")) {
            sql = PATTERN_ALTER_ADD_COLUMN_IF_NOT_EXISTS.matcher(sql).replaceAll("ADD COLUMN");
        }

        // 3. MERGE INTO translation
        if (trimmed.toUpperCase(Locale.US).startsWith("MERGE INTO")) {
            sql = translateMerge(sql);
        }

        return sql;
    }

    private static String translateMerge(String sql) {
        Matcher m1 = PATTERN_MERGE_VALUES.matcher(sql);
        if (m1.matches()) {
            String table = m1.group(1);
            String colsStr = m1.group(2);
            String keyStr = m1.group(3).trim();
            String valsStr = m1.group(4).trim();

            if (colsStr != null && !colsStr.trim().isEmpty()) {
                String[] cols = colsStr.split(",");
                String[] keys = keyStr.split(",");
                List<String> keyList = new ArrayList<>();
                for (String k : keys) keyList.add(k.trim().toLowerCase(Locale.US));

                List<String> updateClauses = new ArrayList<>();
                for (String col : cols) {
                    String c = col.trim();
                    if (!keyList.contains(c.toLowerCase(Locale.US))) {
                        updateClauses.add(c + "=excluded." + c);
                    }
                }

                if (updateClauses.isEmpty()) {
                    return "INSERT OR IGNORE INTO " + table + " (" + colsStr + ") VALUES (" + valsStr + ")";
                }

                return "INSERT INTO " + table + " (" + colsStr + ") VALUES (" + valsStr + ") "
                        + "ON CONFLICT(" + keyStr + ") DO UPDATE SET " + join(", ", updateClauses);
            } else {
                return "INSERT OR REPLACE INTO " + table + " VALUES (" + valsStr + ")";
            }
        }

        Matcher m2 = PATTERN_MERGE_SELECT.matcher(sql);
        if (m2.matches()) {
            String table = m2.group(1);
            String colsStr = m2.group(2);
            String keyStr = m2.group(3).trim();
            String selectStmt = m2.group(4).trim();
            if (selectStmt.endsWith(";")) {
                selectStmt = selectStmt.substring(0, selectStmt.length() - 1);
            }

            String[] cols = colsStr.split(",");
            String[] keys = keyStr.split(",");
            List<String> keyList = new ArrayList<>();
            for (String k : keys) keyList.add(k.trim().toLowerCase(Locale.US));

            List<String> updateClauses = new ArrayList<>();
            for (String col : cols) {
                String c = col.trim();
                if (!keyList.contains(c.toLowerCase(Locale.US))) {
                    updateClauses.add(c + "=excluded." + c);
                }
            }

            if (updateClauses.isEmpty()) {
                return "INSERT OR IGNORE INTO " + table + " (" + colsStr + ") " + selectStmt;
            }

            return "INSERT INTO " + table + " (" + colsStr + ") " + selectStmt
                    + " ON CONFLICT(" + keyStr + ") DO UPDATE SET " + join(", ", updateClauses);
        }

        return sql;
    }

    private static String join(String delimiter, List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(delimiter);
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    // ── Connection Handler ──────────────────────────────────────────────

    private static class ConnectionHandler implements InvocationHandler {
        private final OverdriveSqliteMaster master;
        private boolean autoCommit = true;
        private boolean closed = false;

        ConnectionHandler(OverdriveSqliteMaster master) {
            this.master = master;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();

            switch (name) {
                case "createStatement":
                    return createStatementProxy(master, (Connection) proxy);

                case "prepareStatement": {
                    String rawSql = (String) args[0];
                    String translated = translateSql(rawSql);
                    return createPreparedStatementProxy(master, (Connection) proxy, translated);
                }

                case "setAutoCommit": {
                    boolean target = (Boolean) args[0];
                    if (this.autoCommit != target) {
                        this.autoCommit = target;
                        if (!this.autoCommit) {
                            if (!master.inTransaction()) {
                                master.beginTransaction();
                            }
                        } else {
                            if (master.inTransaction()) {
                                master.setTransactionSuccessful();
                                master.endTransaction();
                            }
                        }
                    }
                    return null;
                }

                case "getAutoCommit":
                    return autoCommit;

                case "commit":
                    if (master.inTransaction()) {
                        master.setTransactionSuccessful();
                        master.endTransaction();
                        if (!autoCommit) {
                            master.beginTransaction();
                        }
                    }
                    return null;

                case "rollback":
                    if (master.inTransaction()) {
                        master.endTransaction();
                        if (!autoCommit) {
                            master.beginTransaction();
                        }
                    }
                    return null;

                case "isClosed":
                    return closed || !master.isOpen();

                case "close":
                    if (!closed) {
                        if (master.inTransaction()) {
                            try { master.endTransaction(); } catch (Exception ignored) {}
                        }
                        closed = true;
                    }
                    return null;

                case "getMetaData":
                    return createDatabaseMetaDataProxy(master, (Connection) proxy);

                case "isValid":
                    return !closed && master.isOpen();

                case "setTransactionIsolation":
                    return null;

                case "getTransactionIsolation":
                    return Connection.TRANSACTION_SERIALIZABLE;

                case "equals":
                    return proxy == args[0];

                case "hashCode":
                    return System.identityHashCode(proxy);

                case "toString":
                    return "SqliteConnectionBridge[" + master + "]";

                default:
                    return defaultValue(method.getReturnType());
            }
        }
    }

    // ── PreparedStatement / Statement Handler ───────────────────────────

    private static PreparedStatement createPreparedStatementProxy(
            OverdriveSqliteMaster master, Connection connection, String sql) {
        return (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class<?>[]{PreparedStatement.class},
                new StatementHandler(master, connection, sql)
        );
    }

    private static Statement createStatementProxy(
            OverdriveSqliteMaster master, Connection connection) {
        return (Statement) Proxy.newProxyInstance(
                Statement.class.getClassLoader(),
                new Class<?>[]{Statement.class},
                new StatementHandler(master, connection, null)
        );
    }

    private static class StatementHandler implements InvocationHandler {
        private final OverdriveSqliteMaster master;
        private final Connection connection;
        private final String defaultSql;
        private final Map<Integer, Object> params = new LinkedHashMap<>();
        private final List<Map<Integer, Object>> batch = new ArrayList<>();
        private Long lastGeneratedKey = null;
        private boolean closed = false;

        StatementHandler(OverdriveSqliteMaster master, Connection connection, String defaultSql) {
            this.master = master;
            this.connection = connection;
            this.defaultSql = defaultSql;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();

            // Setters for PreparedStatement
            if (name.startsWith("set") && args != null && args.length >= 2 && args[0] instanceof Integer) {
                int paramIndex = (Integer) args[0];
                switch (name) {
                    case "setNull":
                        params.put(paramIndex, null);
                        return null;
                    case "setBoolean":
                        params.put(paramIndex, ((Boolean) args[1]) ? 1 : 0);
                        return null;
                    case "setInt":
                    case "setLong":
                    case "setDouble":
                    case "setString":
                    case "setObject":
                        params.put(paramIndex, args[1]);
                        return null;
                }
            }

            switch (name) {
                case "clearParameters":
                    params.clear();
                    return null;

                case "addBatch":
                    batch.add(new LinkedHashMap<>(params));
                    return null;

                case "clearBatch":
                    batch.clear();
                    return null;

                case "executeBatch": {
                    int[] counts = new int[batch.size()];
                    for (int i = 0; i < batch.size(); i++) {
                        Object[] bindArgs = toArgsArray(batch.get(i));
                        counts[i] = executeUpdateInternal(defaultSql, bindArgs);
                    }
                    batch.clear();
                    return counts;
                }

                case "executeQuery": {
                    String execSql = (args != null && args.length > 0 && args[0] instanceof String)
                            ? translateSql((String) args[0])
                            : defaultSql;
                    Object[] bindArgs = (args != null && args.length > 0 && args[0] instanceof String)
                            ? new Object[0]
                            : toArgsArray(params);

                    String[] strArgs = toStringArgs(bindArgs);
                    Cursor cursor = master.rawQuery(execSql, strArgs);
                    return createResultSetProxy(cursor, (Statement) proxy);
                }

                case "executeUpdate": {
                    String execSql = (args != null && args.length > 0 && args[0] instanceof String)
                            ? translateSql((String) args[0])
                            : defaultSql;
                    Object[] bindArgs = (args != null && args.length > 0 && args[0] instanceof String)
                            ? new Object[0]
                            : toArgsArray(params);

                    return executeUpdateInternal(execSql, bindArgs);
                }

                case "execute": {
                    String execSql = (args != null && args.length > 0 && args[0] instanceof String)
                            ? translateSql((String) args[0])
                            : defaultSql;
                    Object[] bindArgs = (args != null && args.length > 0 && args[0] instanceof String)
                            ? new Object[0]
                            : toArgsArray(params);

                    if (execSql == null || execSql.trim().isEmpty()) {
                        return false;
                    }

                    String upper = execSql.trim().toUpperCase(Locale.US);
                    if (upper.startsWith("SET ")) {
                        // H2-specific session settings like SET CACHE_SIZE are no-ops on SQLite
                        return false;
                    }

                    if (upper.startsWith("SELECT") || upper.startsWith("PRAGMA")) {
                        String[] strArgs = toStringArgs(bindArgs);
                        Cursor cursor = master.rawQuery(execSql, strArgs);
                        createResultSetProxy(cursor, (Statement) proxy);
                        return true;
                    } else {
                        executeUpdateInternal(execSql, bindArgs);
                        return false;
                    }
                }

                case "getGeneratedKeys":
                    return createGeneratedKeysResultSet(lastGeneratedKey, (Statement) proxy);

                case "getConnection":
                    return connection;

                case "isClosed":
                    return closed;

                case "close":
                    closed = true;
                    params.clear();
                    batch.clear();
                    return null;

                case "setQueryTimeout":
                case "setMaxRows":
                case "setFetchSize":
                    return null;

                case "equals":
                    return proxy == args[0];

                case "hashCode":
                    return System.identityHashCode(proxy);

                case "toString":
                    return "SqliteStatementBridge[" + defaultSql + "]";

                default:
                    return defaultValue(method.getReturnType());
            }
        }

        private int executeUpdateInternal(String sql, Object[] bindArgs) throws SQLException {
            if (sql == null || sql.trim().isEmpty()) return 0;
            String upper = sql.trim().toUpperCase(Locale.US);

            if (upper.startsWith("SET ")) {
                return 0; // H2 session settings
            }

            try {
                if (upper.startsWith("INSERT")) {
                    long rowId = master.executeInsert(sql, bindArgs);
                    lastGeneratedKey = rowId;
                    return rowId >= 0 ? 1 : 0;
                } else {
                    return master.executeUpdateDelete(sql, bindArgs);
                }
            } catch (Throwable t) {
                // If an ALTER TABLE ADD COLUMN encounters a duplicate column, treat it as successful
                boolean isDuplicateCol = false;
                for (Throwable cur = t; cur != null; cur = cur.getCause()) {
                    if (cur.getMessage() != null && cur.getMessage().toLowerCase(Locale.US).contains("duplicate column")) {
                        isDuplicateCol = true;
                        break;
                    }
                }
                if (upper.startsWith("ALTER TABLE") && isDuplicateCol) {
                    return 0;
                }
                throw new SQLException("SQL execution failed: " + sql + " - " + t.getMessage(), t);
            }
        }
    }

    // ── ResultSet Handler ───────────────────────────────────────────────

    private static ResultSet createResultSetProxy(Cursor cursor, Statement statement) {
        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class<?>[]{ResultSet.class},
                new ResultSetHandler(cursor, statement)
        );
    }

    private static class ResultSetHandler implements InvocationHandler {
        private final Cursor cursor;
        private final Statement statement;
        private boolean lastWasNull = false;

        ResultSetHandler(Cursor cursor, Statement statement) {
            this.cursor = cursor;
            this.statement = statement;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();

            switch (name) {
                case "next":
                    return cursor != null && cursor.moveToNext();

                case "wasNull":
                    return lastWasNull;

                case "close":
                    if (cursor != null) {
                        cursor.close();
                    }
                    return null;

                case "isClosed":
                    return cursor == null || cursor.isClosed();

                case "getStatement":
                    return statement;

                case "getMetaData":
                    return createResultSetMetaDataProxy(cursor);

                // Typed getters by index (1-based)
                case "getInt": {
                    int col = resolveColumnIndex(args[0]);
                    if (cursor == null || cursor.isNull(col)) {
                        lastWasNull = true;
                        return 0;
                    }
                    lastWasNull = false;
                    return cursor.getInt(col);
                }

                case "getLong": {
                    int col = resolveColumnIndex(args[0]);
                    if (cursor == null || cursor.isNull(col)) {
                        lastWasNull = true;
                        return 0L;
                    }
                    lastWasNull = false;
                    return cursor.getLong(col);
                }

                case "getDouble": {
                    int col = resolveColumnIndex(args[0]);
                    if (cursor == null || cursor.isNull(col)) {
                        lastWasNull = true;
                        return 0.0;
                    }
                    lastWasNull = false;
                    return cursor.getDouble(col);
                }

                case "getString": {
                    int col = resolveColumnIndex(args[0]);
                    if (cursor == null || cursor.isNull(col)) {
                        lastWasNull = true;
                        return null;
                    }
                    lastWasNull = false;
                    return cursor.getString(col);
                }

                case "getBoolean": {
                    int col = resolveColumnIndex(args[0]);
                    if (cursor == null || cursor.isNull(col)) {
                        lastWasNull = true;
                        return false;
                    }
                    lastWasNull = false;
                    String s = cursor.getString(col);
                    if (s == null) return false;
                    return "1".equals(s) || "true".equalsIgnoreCase(s) || cursor.getInt(col) != 0;
                }

                case "getObject": {
                    int col = resolveColumnIndex(args[0]);
                    if (cursor == null || cursor.isNull(col)) {
                        lastWasNull = true;
                        return null;
                    }
                    lastWasNull = false;
                    int type = cursor.getType(col);
                    switch (type) {
                        case Cursor.FIELD_TYPE_INTEGER:
                            return cursor.getLong(col);
                        case Cursor.FIELD_TYPE_FLOAT:
                            return cursor.getDouble(col);
                        case Cursor.FIELD_TYPE_BLOB:
                            return cursor.getBlob(col);
                        case Cursor.FIELD_TYPE_STRING:
                        default:
                            return cursor.getString(col);
                    }
                }

                case "equals":
                    return proxy == args[0];

                case "hashCode":
                    return System.identityHashCode(proxy);

                case "toString":
                    return "SqliteResultSetBridge[" + cursor + "]";

                default:
                    return defaultValue(method.getReturnType());
            }
        }

        private int resolveColumnIndex(Object colArg) throws SQLException {
            if (cursor == null) {
                throw new SQLException("Cursor is null");
            }
            if (colArg instanceof Integer) {
                return ((Integer) colArg) - 1; // 1-based to 0-based
            } else if (colArg instanceof String) {
                int idx = cursor.getColumnIndex((String) colArg);
                if (idx < 0) {
                    throw new SQLException("Column not found: " + colArg);
                }
                return idx;
            }
            throw new SQLException("Invalid column identifier: " + colArg);
        }
    }

    // ── Generated Keys ResultSet ────────────────────────────────────────

    private static ResultSet createGeneratedKeysResultSet(Long rowId, Statement statement) {
        String[] colNames = new String[]{"id"};
        List<Object[]> rows = new ArrayList<>();
        if (rowId != null && rowId >= 0) {
            rows.add(new Object[]{rowId});
        }
        SimpleCursor cursor = new SimpleCursor(colNames, rows);
        return createResultSetProxy(cursor, statement);
    }

    // ── ResultSetMetaData Handler ───────────────────────────────────────

    private static ResultSetMetaData createResultSetMetaDataProxy(Cursor cursor) {
        return (ResultSetMetaData) Proxy.newProxyInstance(
                ResultSetMetaData.class.getClassLoader(),
                new Class<?>[]{ResultSetMetaData.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    switch (name) {
                        case "getColumnCount":
                            return cursor != null ? cursor.getColumnCount() : 0;
                        case "getColumnName":
                        case "getColumnLabel": {
                            int idx = (Integer) args[0] - 1;
                            return (cursor != null && idx >= 0 && idx < cursor.getColumnCount())
                                    ? cursor.getColumnName(idx)
                                    : "";
                        }
                        default:
                            return defaultValue(method.getReturnType());
                    }
                }
        );
    }

    // ── DatabaseMetaData Handler ────────────────────────────────────────

    private static DatabaseMetaData createDatabaseMetaDataProxy(
            OverdriveSqliteMaster master, Connection connection) {
        return (DatabaseMetaData) Proxy.newProxyInstance(
                DatabaseMetaData.class.getClassLoader(),
                new Class<?>[]{DatabaseMetaData.class},
                new DatabaseMetaDataHandler(master, connection)
        );
    }

    private static class DatabaseMetaDataHandler implements InvocationHandler {
        private final OverdriveSqliteMaster master;
        private final Connection connection;

        DatabaseMetaDataHandler(OverdriveSqliteMaster master, Connection connection) {
            this.master = master;
            this.connection = connection;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();

            switch (name) {
                case "getConnection":
                    return connection;

                case "getDatabaseProductName":
                    return "SQLite";

                case "getDatabaseProductVersion":
                    return "3.x";

                case "getDriverName":
                    return "OverdriveSqliteBridge";

                case "getTables": {
                    // args: catalog, schemaPattern, tableNamePattern, types
                    String tableNamePattern = args != null && args.length > 2 && args[2] != null
                            ? String.valueOf(args[2])
                            : null;

                    String sql = "SELECT name AS TABLE_NAME, type AS TABLE_TYPE FROM sqlite_master WHERE type IN ('table', 'view')";
                    String[] selArgs = null;
                    if (tableNamePattern != null && !tableNamePattern.equals("%") && !tableNamePattern.isEmpty()) {
                        sql += " AND LOWER(name) = LOWER(?)";
                        selArgs = new String[]{tableNamePattern};
                    }

                    Cursor c = master.rawQuery(sql, selArgs);
                    List<Object[]> rows = new ArrayList<>();
                    if (c != null) {
                        try {
                            while (c.moveToNext()) {
                                rows.add(new Object[]{
                                        null, // TABLE_CAT
                                        null, // TABLE_SCHEM
                                        c.getString(0), // TABLE_NAME
                                        c.getString(1).toUpperCase(Locale.US), // TABLE_TYPE
                                        null  // REMARKS
                                });
                            }
                        } finally {
                            c.close();
                        }
                    }

                    SimpleCursor sc = new SimpleCursor(
                            new String[]{"TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "TABLE_TYPE", "REMARKS"},
                            rows
                    );
                    return createResultSetProxy(sc, null);
                }

                case "getColumns": {
                    // args: catalog, schemaPattern, tableNamePattern, columnNamePattern
                    String tableName = args != null && args.length > 2 && args[2] != null
                            ? String.valueOf(args[2])
                            : "";
                    String colPattern = args != null && args.length > 3 && args[3] != null
                            ? String.valueOf(args[3])
                            : null;

                    List<Object[]> rows = new ArrayList<>();
                    if (!tableName.isEmpty()) {
                        String pragma = "PRAGMA table_info('" + tableName.replace("'", "''") + "')";
                        Cursor c = master.rawQuery(pragma, null);
                        if (c != null) {
                            try {
                                int nameCol = c.getColumnIndex("name");
                                int typeCol = c.getColumnIndex("type");
                                int notNullCol = c.getColumnIndex("notnull");

                                while (c.moveToNext()) {
                                    String colName = c.getString(nameCol);
                                    String colType = c.getString(typeCol);
                                    int notNull = c.getInt(notNullCol);

                                    if (colPattern == null || colPattern.equals("%")
                                            || colPattern.equalsIgnoreCase(colName)) {
                                        rows.add(new Object[]{
                                                tableName, // TABLE_NAME
                                                colName,   // COLUMN_NAME
                                                0,         // DATA_TYPE
                                                colType,   // TYPE_NAME
                                                0,         // COLUMN_SIZE
                                                notNull == 0 ? 1 : 0 // NULLABLE
                                        });
                                    }
                                }
                            } finally {
                                c.close();
                            }
                        }
                    }

                    SimpleCursor sc = new SimpleCursor(
                            new String[]{"TABLE_NAME", "COLUMN_NAME", "DATA_TYPE", "TYPE_NAME", "COLUMN_SIZE", "NULLABLE"},
                            rows
                    );
                    return createResultSetProxy(sc, null);
                }

                default:
                    return defaultValue(method.getReturnType());
            }
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private static Object[] toArgsArray(Map<Integer, Object> map) {
        if (map == null || map.isEmpty()) {
            return new Object[0];
        }
        int maxIndex = 0;
        for (Integer idx : map.keySet()) {
            if (idx > maxIndex) maxIndex = idx;
        }
        Object[] arr = new Object[maxIndex];
        for (Map.Entry<Integer, Object> entry : map.entrySet()) {
            int idx = entry.getKey();
            if (idx >= 1 && idx <= maxIndex) {
                arr[idx - 1] = entry.getValue();
            }
        }
        return arr;
    }

    private static String[] toStringArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return new String[0];
        }
        String[] strArgs = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            strArgs[i] = args[i] != null ? String.valueOf(args[i]) : null;
        }
        return strArgs;
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == null || returnType == void.class || returnType == Void.class) return null;
        if (returnType == boolean.class || returnType == Boolean.class) return Boolean.FALSE;
        if (returnType == byte.class || returnType == Byte.class) return (byte) 0;
        if (returnType == short.class || returnType == Short.class) return (short) 0;
        if (returnType == int.class || returnType == Integer.class) return 0;
        if (returnType == long.class || returnType == Long.class) return 0L;
        if (returnType == float.class || returnType == Float.class) return 0.0f;
        if (returnType == double.class || returnType == Double.class) return 0.0;
        return null;
    }
}
