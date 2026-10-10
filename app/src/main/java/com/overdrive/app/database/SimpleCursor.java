package com.overdrive.app.database;

import android.content.ContentResolver;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;

import java.util.Collections;
import java.util.List;

/**
 * Lightweight in-memory implementation of {@link Cursor}.
 * Enables SQLite query results to be read uniformly in host JVM unit tests
 * without depending on Android framework bytecode.
 */
public class SimpleCursor implements Cursor {

    private final String[] columnNames;
    private final List<Object[]> rows;
    private int position = -1;
    private boolean closed = false;

    public SimpleCursor(String[] columnNames, List<Object[]> rows) {
        this.columnNames = columnNames != null ? columnNames : new String[0];
        this.rows = rows != null ? rows : Collections.emptyList();
    }

    @Override
    public int getCount() {
        return rows.size();
    }

    @Override
    public int getPosition() {
        return position;
    }

    @Override
    public boolean move(int offset) {
        return moveToPosition(position + offset);
    }

    @Override
    public boolean moveToPosition(int targetPosition) {
        if (targetPosition >= 0 && targetPosition < rows.size()) {
            position = targetPosition;
            return true;
        }
        if (targetPosition < 0) {
            position = -1;
        } else if (targetPosition >= rows.size()) {
            position = rows.size();
        }
        return false;
    }

    @Override
    public boolean moveToFirst() {
        return moveToPosition(0);
    }

    @Override
    public boolean moveToLast() {
        return moveToPosition(rows.size() - 1);
    }

    @Override
    public boolean moveToNext() {
        return moveToPosition(position + 1);
    }

    @Override
    public boolean moveToPrevious() {
        return moveToPosition(position - 1);
    }

    @Override
    public boolean isFirst() {
        return position == 0 && !rows.isEmpty();
    }

    @Override
    public boolean isLast() {
        return position == rows.size() - 1 && !rows.isEmpty();
    }

    @Override
    public boolean isBeforeFirst() {
        return rows.isEmpty() || position < 0;
    }

    @Override
    public boolean isAfterLast() {
        return rows.isEmpty() || position >= rows.size();
    }

    @Override
    public int getColumnIndex(String columnName) {
        if (columnName == null) return -1;
        for (int i = 0; i < columnNames.length; i++) {
            if (columnName.equalsIgnoreCase(columnNames[i])) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
        int idx = getColumnIndex(columnName);
        if (idx < 0) {
            throw new IllegalArgumentException("Column not found: " + columnName);
        }
        return idx;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnNames[columnIndex];
    }

    @Override
    public String[] getColumnNames() {
        return columnNames;
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    private Object get(int columnIndex) {
        if (position < 0 || position >= rows.size()) {
            throw new IllegalStateException("Cursor position out of bounds: " + position);
        }
        return rows.get(position)[columnIndex];
    }

    @Override
    public String getString(int columnIndex) {
        Object val = get(columnIndex);
        return val != null ? String.valueOf(val) : null;
    }

    @Override
    public short getShort(int columnIndex) {
        Number n = (Number) get(columnIndex);
        return n != null ? n.shortValue() : 0;
    }

    @Override
    public int getInt(int columnIndex) {
        Object val = get(columnIndex);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) {
            try { return Integer.parseInt((String) val); } catch (Exception ignored) {}
        }
        return 0;
    }

    @Override
    public long getLong(int columnIndex) {
        Object val = get(columnIndex);
        if (val instanceof Number) return ((Number) val).longValue();
        if (val instanceof String) {
            try { return Long.parseLong((String) val); } catch (Exception ignored) {}
        }
        return 0L;
    }

    @Override
    public float getFloat(int columnIndex) {
        Object val = get(columnIndex);
        if (val instanceof Number) return ((Number) val).floatValue();
        if (val instanceof String) {
            try { return Float.parseFloat((String) val); } catch (Exception ignored) {}
        }
        return 0f;
    }

    @Override
    public double getDouble(int columnIndex) {
        Object val = get(columnIndex);
        if (val instanceof Number) return ((Number) val).doubleValue();
        if (val instanceof String) {
            try { return Double.parseDouble((String) val); } catch (Exception ignored) {}
        }
        return 0.0;
    }

    @Override
    public byte[] getBlob(int columnIndex) {
        Object val = get(columnIndex);
        if (val instanceof byte[]) return (byte[]) val;
        return null;
    }

    @Override
    public boolean isNull(int columnIndex) {
        return get(columnIndex) == null;
    }

    @Override
    public int getType(int columnIndex) {
        Object val = get(columnIndex);
        if (val == null) return FIELD_TYPE_NULL;
        if (val instanceof byte[]) return FIELD_TYPE_BLOB;
        if (val instanceof Float || val instanceof Double) return FIELD_TYPE_FLOAT;
        if (val instanceof Number) return FIELD_TYPE_INTEGER;
        return FIELD_TYPE_STRING;
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {}
    @Override public void deactivate() {}
    @Override public boolean requery() { return false; }
    @Override public void registerContentObserver(ContentObserver observer) {}
    @Override public void unregisterContentObserver(ContentObserver observer) {}
    @Override public void registerDataSetObserver(DataSetObserver observer) {}
    @Override public void unregisterDataSetObserver(DataSetObserver observer) {}
    @Override public void setNotificationUri(ContentResolver cr, Uri uri) {}
    @Override public Uri getNotificationUri() { return null; }
    @Override public boolean getWantsAllOnMoveCalls() { return false; }
    @Override public void setExtras(Bundle extras) {}
    @Override public Bundle getExtras() { return null; }
    @Override public Bundle respond(Bundle extras) { return null; }
}
