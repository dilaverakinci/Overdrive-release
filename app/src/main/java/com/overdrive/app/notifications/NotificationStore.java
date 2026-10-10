package com.overdrive.app.notifications;

import android.database.Cursor;

import com.overdrive.app.database.OverdriveSqliteMaster;
import com.overdrive.app.logging.DaemonLogger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Persistent notification log backing Notifications ▸ Log.
 * Migrated to {@link OverdriveSqliteMaster} (Zero-Context SQLite WAL).
 * Eliminates the legacy H2 embedded engine, stale lock files, and socket locks.
 */
public final class NotificationStore {

    private static final String TAG = "NotificationStore";
    private static final DaemonLogger logger = DaemonLogger.getInstance(TAG);

    private static final String TABLE = "notifications";

    private static final long RETENTION_DAYS = 120;
    private static final int MAX_ROWS = 5000;
    private static final int PRUNE_BATCH = 500;
    private static final int PRUNE_MAX_PER_RUN = 20000;
    private static final int MAX_PAGE_SIZE = 200;
    private static final int BULK_DELETE_CHUNK = 500;

    private static final String COLS =
            "id, ts, category, severity, title, body, tag, click_url, data";

    private static volatile NotificationStore instance;
    private static final Object SINGLETON_LOCK = new Object();

    private final OverdriveSqliteMaster master;
    private final Object lock = new Object();
    private volatile boolean isInitialized = false;
    private volatile boolean isRunning = false;
    private ScheduledExecutorService scheduler;

    public NotificationStore() {
        this(OverdriveSqliteMaster.getInstance());
    }

    public NotificationStore(OverdriveSqliteMaster master) {
        this.master = master != null ? master : OverdriveSqliteMaster.getInstance();
    }

    public static NotificationStore getInstance() {
        if (instance == null) {
            synchronized (SINGLETON_LOCK) {
                if (instance == null) instance = new NotificationStore();
            }
        }
        return instance;
    }

    // ==================== LIFECYCLE ====================

    public void init() {
        if (isInitialized) return;
        synchronized (lock) {
            if (isInitialized) return;
            try {
                if (master.open()) {
                    isInitialized = true;
                    isRunning = true;
                    logger.info("NotificationStore initialized via OverdriveSqliteMaster");
                } else {
                    logger.error("Failed to open OverdriveSqliteMaster for NotificationStore");
                }
            } catch (Exception e) {
                logger.error("Failed to initialize NotificationStore: " + e.getMessage(), e);
            }
        }
        if (!isInitialized) return;
        startPruneScheduler();
    }

    private void startPruneScheduler() {
        ScheduledExecutorService sched;
        synchronized (lock) {
            if (scheduler != null || !isRunning) return;
            sched = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "NotifStorePrune");
                t.setDaemon(true);
                return t;
            });
            scheduler = sched;
        }
        try {
            sched.scheduleAtFixedRate(() -> {
                try {
                    cleanupOldData();
                } catch (Throwable t) {
                    logger.error("NotificationStore prune task error: " + t.getMessage(),
                            t instanceof Exception ? (Exception) t : new Exception(t));
                }
            }, 0, 6, TimeUnit.HOURS);
        } catch (java.util.concurrent.RejectedExecutionException rex) {
            logger.debug("NotificationStore prune scheduler rejected (shutting down)");
        }
    }

    public void stop() {
        isRunning = false;
        ScheduledExecutorService sched;
        synchronized (lock) {
            sched = scheduler;
            scheduler = null;
        }
        if (sched != null) {
            sched.shutdownNow();
        }
        synchronized (lock) {
            isInitialized = false;
        }
    }

    // ==================== WRITE ====================

    public void insert(NotificationEvent event) {
        insert(event, null);
    }

    public void insert(NotificationEvent event, String resolvedUrl) {
        if (event == null) return;
        synchronized (lock) {
            if (!isInitialized) {
                init();
                if (!isInitialized) return;
            }
            try {
                String url = event.clickUrl;
                if (url == null && event.data != null) url = event.data.optString("url", null);
                if (url == null) url = resolvedUrl;
                String dataStr = event.data != null ? event.data.toString() : null;

                master.execSQL(
                        "INSERT INTO " + TABLE + " (ts, category, severity, title, body, tag, click_url, data) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        new Object[]{
                                event.timestamp,
                                event.category,
                                event.severity != null ? event.severity.name().toLowerCase(Locale.US) : "info",
                                clip(event.title, 512),
                                event.body,
                                event.tag,
                                url,
                                dataStr
                        }
                );
            } catch (Exception e) {
                logger.error("NotificationStore.insert failed: " + e.getMessage(), e);
            }
        }
    }

    // ==================== READ ====================

    public static final class Page {
        public final JSONArray items;
        public final long total;
        public Page(JSONArray items, long total) { this.items = items; this.total = total; }
    }

    public Page listWithCount(long fromMs, long toMs, String categoryPrefix, String severity,
                              int limit, int offset) {
        JSONArray out = new JSONArray();
        long total = 0;
        if (limit < 1) limit = 1;
        if (limit > MAX_PAGE_SIZE) limit = MAX_PAGE_SIZE;
        if (offset < 0) offset = 0;
        boolean hasPrefix = categoryPrefix != null && !categoryPrefix.isEmpty();
        boolean hasSev = severity != null && !severity.isEmpty();
        String likeArg = hasPrefix ? escapeLike(categoryPrefix) + "%" : null;
        String sevArg = hasSev ? severity.toLowerCase(Locale.US) : null;

        StringBuilder where = new StringBuilder(" WHERE ts >= ? AND ts <= ?");
        List<String> argsList = new ArrayList<>();
        argsList.add(String.valueOf(fromMs));
        argsList.add(String.valueOf(toMs));

        if (hasPrefix) {
            where.append(" AND category LIKE ?");
            argsList.add(likeArg);
        }
        if (hasSev) {
            where.append(" AND severity = ?");
            argsList.add(sevArg);
        }

        synchronized (lock) {
            if (!isInitialized) {
                init();
                if (!isInitialized) return new Page(new JSONArray(), 0);
            }
            try {
                // Count query
                String countSql = "SELECT COUNT(*) FROM " + TABLE + where;
                try (Cursor countCursor = master.rawQuery(countSql, argsList.toArray(new String[0]))) {
                    if (countCursor != null && countCursor.moveToFirst()) {
                        total = countCursor.getLong(0);
                    }
                }

                // Data query
                String dataSql = "SELECT " + COLS + " FROM " + TABLE + where + " ORDER BY ts DESC LIMIT ? OFFSET ?";
                List<String> queryArgs = new ArrayList<>(argsList);
                queryArgs.add(String.valueOf(limit));
                queryArgs.add(String.valueOf(offset));

                try (Cursor cursor = master.rawQuery(dataSql, queryArgs.toArray(new String[0]))) {
                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            out.put(cursorToJson(cursor));
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("NotificationStore.listWithCount failed: " + e.getMessage(), e);
                return new Page(new JSONArray(), 0);
            }
        }
        return new Page(out, total);
    }

    private JSONObject cursorToJson(Cursor cursor) {
        JSONObject o = new JSONObject();
        try {
            o.put("id", cursor.getLong(cursor.getColumnIndexOrThrow("id")));
            o.put("ts", cursor.getLong(cursor.getColumnIndexOrThrow("ts")));
            o.put("category", cursor.getString(cursor.getColumnIndexOrThrow("category")));
            o.put("severity", cursor.getString(cursor.getColumnIndexOrThrow("severity")));
            o.put("title", cursor.getString(cursor.getColumnIndexOrThrow("title")));
            String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
            o.put("body", body == null ? "" : body);
            String tag = cursor.getString(cursor.getColumnIndexOrThrow("tag"));
            if (tag != null) o.put("tag", tag);
            String url = cursor.getString(cursor.getColumnIndexOrThrow("click_url"));
            if (url != null) o.put("url", url);
            String data = cursor.getString(cursor.getColumnIndexOrThrow("data"));
            if (data != null && !data.isEmpty()) {
                try { o.put("data", new JSONObject(data)); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return o;
    }

    // ==================== DELETE ====================

    public boolean deleteById(long id) {
        synchronized (lock) {
            if (!isInitialized) {
                init();
                if (!isInitialized) return false;
            }
            try {
                master.execSQL("DELETE FROM " + TABLE + " WHERE id = ?", new Object[]{id});
                return true;
            } catch (Exception e) {
                logger.error("NotificationStore.deleteById failed: " + e.getMessage(), e);
                return false;
            }
        }
    }

    public int deleteBulk(long[] ids) {
        if (ids == null || ids.length == 0) return 0;
        synchronized (lock) {
            if (!isInitialized) {
                init();
                if (!isInitialized) return 0;
            }
            int removed = 0;
            try {
                for (int start = 0; start < ids.length; start += BULK_DELETE_CHUNK) {
                    int end = Math.min(start + BULK_DELETE_CHUNK, ids.length);
                    int n = end - start;
                    StringBuilder in = new StringBuilder();
                    Object[] chunkArgs = new Object[n];
                    for (int i = 0; i < n; i++) {
                        in.append(i == 0 ? "?" : ",?");
                        chunkArgs[i] = ids[start + i];
                    }
                    master.execSQL("DELETE FROM " + TABLE + " WHERE id IN (" + in + ")", chunkArgs);
                    removed += n;
                }
            } catch (Exception e) {
                logger.error("NotificationStore.deleteBulk failed: " + e.getMessage(), e);
            }
            return removed;
        }
    }

    public int clear(long fromMs, long toMs) {
        synchronized (lock) {
            if (!isInitialized) {
                init();
                if (!isInitialized) return 0;
            }
            try {
                int before = 0;
                try (Cursor c = master.rawQuery("SELECT COUNT(*) FROM " + TABLE + " WHERE ts >= ? AND ts <= ?",
                        new String[]{String.valueOf(fromMs), String.valueOf(toMs)})) {
                    if (c != null && c.moveToFirst()) before = c.getInt(0);
                }
                master.execSQL("DELETE FROM " + TABLE + " WHERE ts >= ? AND ts <= ?",
                        new Object[]{fromMs, toMs});
                logger.info("NotificationStore.clear removed " + before + " rows");
                return before;
            } catch (Exception e) {
                logger.error("NotificationStore.clear failed: " + e.getMessage(), e);
                return 0;
            }
        }
    }

    // ==================== RETENTION ====================

    public void cleanupOldData() {
        synchronized (lock) {
            if (!isInitialized) {
                init();
                if (!isInitialized) return;
            }
            try {
                long cutoff = System.currentTimeMillis() - (RETENTION_DAYS * 24L * 60 * 60 * 1000L);
                master.execSQL("DELETE FROM " + TABLE + " WHERE ts < ?", new Object[]{cutoff});

                long total = 0;
                try (Cursor c = master.rawQuery("SELECT COUNT(*) FROM " + TABLE, null)) {
                    if (c != null && c.moveToFirst()) total = c.getLong(0);
                }

                if (total > MAX_ROWS) {
                    long toTrim = Math.min(total - MAX_ROWS, PRUNE_MAX_PER_RUN);
                    master.execSQL("DELETE FROM " + TABLE + " WHERE id IN (SELECT id FROM " + TABLE + " ORDER BY ts ASC LIMIT ?)",
                            new Object[]{toTrim});
                    logger.info("NotificationStore cap: trimmed " + toTrim + " oldest rows (was " + total + ", cap " + MAX_ROWS + ")");
                }
            } catch (Exception e) {
                logger.error("NotificationStore.cleanupOldData failed: " + e.getMessage(), e);
            }
        }
    }

    private static String clip(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        int end = max;
        if (Character.isHighSurrogate(s.charAt(end - 1))) end--;
        return s.substring(0, end);
    }

    private static String escapeLike(String s) {
        if (s == null) return null;
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
