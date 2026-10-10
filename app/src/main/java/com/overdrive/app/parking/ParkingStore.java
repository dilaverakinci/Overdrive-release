package com.overdrive.app.parking;

import android.database.Cursor;

import com.overdrive.app.database.OverdriveSqliteMaster;
import com.overdrive.app.logging.DaemonLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * High-performance, zero-context SQLite store for parking sessions and their neighbours.
 *
 * <p>Backed by {@link OverdriveSqliteMaster} in WAL mode. Thread-safe, multi-process safe,
 * with zero H2 memory overhead, no socket locks, and no .lock.db file leakages.
 */
public final class ParkingStore {

    private static final DaemonLogger logger = DaemonLogger.getInstance("ParkingStore");

    public static final String DEFAULT_DB_PATH = OverdriveSqliteMaster.DEFAULT_DB_PATH;

    public static String defaultJdbcUrl() {
        return "jdbc:sqlite:" + DEFAULT_DB_PATH;
    }

    private final OverdriveSqliteMaster master;

    private static final java.util.Map<String, OverdriveSqliteMaster> TEST_MASTERS = new java.util.concurrent.ConcurrentHashMap<>();

    public ParkingStore() {
        this(OverdriveSqliteMaster.getInstance());
    }

    public ParkingStore(OverdriveSqliteMaster master) {
        this.master = master != null ? master : OverdriveSqliteMaster.getInstance();
    }

    /**
     * Backward compatibility constructor for tests or callers that pass a JDBC URL.
     * When ":mem:" is in the URL, creates or reuses an isolated in-memory OverdriveSqliteMaster.
     */
    public ParkingStore(String jdbcUrl) {
        if (jdbcUrl != null && jdbcUrl.contains(":mem:")) {
            this.master = TEST_MASTERS.computeIfAbsent(jdbcUrl, k -> OverdriveSqliteMaster.useInMemoryForTesting());
        } else {
            this.master = OverdriveSqliteMaster.getInstance();
        }
    }

    // ==================== LIFECYCLE ====================

    public synchronized boolean open() {
        return master.open();
    }

    public synchronized void close() {
        // Shared master is managed at the daemon / test harness lifecycle level
    }

    public synchronized boolean isOpen() {
        return master.isOpen();
    }

    // ==================== SESSIONS ====================

    public synchronized boolean insertSession(ParkingSession s) {
        if (s == null || s.sessionId == null) return false;
        String sql = "INSERT INTO parking_sessions ("
                + "session_id, started_ms, ended_ms, transition_gen, end_trigger, lat, lng,"
                + " accuracy_m, fix_age_ms, fix_from_cache, gps_quality, place_short, place_display,"
                + " place_source, safe_zone, sentry_state, arrived_snapshot_ms, arrived_snapshot_ok,"
                + " returned_snapshot_ms, returned_snapshot_ok, rectify_strength, signage_json,"
                + " signage_state, notified_started, notified_ended, event_count, neighbour_count,"
                + " created_ms, start_soc_pct, end_soc_pct, charged_while_parked, energy_est_kwh,"
                + " start_remain_kwh, end_remain_kwh) VALUES ("
                + "?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try {
            Object[] args = new Object[]{
                    s.sessionId,
                    s.startedMs,
                    s.endedMs,
                    s.transitionGeneration,
                    s.endTrigger,
                    s.hasFix() ? s.lat : null,
                    s.hasFix() ? s.lng : null,
                    s.accuracyM,
                    s.fixAgeMs,
                    s.fixFromCache ? 1 : 0,
                    s.gpsQuality,
                    clamp(s.placeShort, 128),
                    clamp(s.placeDisplay, 256),
                    clamp(s.placeSource, 32),
                    clamp(s.safeZone, 64),
                    s.sentryState,
                    s.arrivedSnapshotMs,
                    s.arrivedSnapshotOk ? 1 : 0,
                    s.returnedSnapshotMs,
                    s.returnedSnapshotOk ? 1 : 0,
                    s.rectifyStrength,
                    s.signageJson,
                    s.signageState,
                    s.notifiedStarted ? 1 : 0,
                    s.notifiedEnded ? 1 : 0,
                    s.eventCount,
                    s.neighbourCount,
                    s.createdMs,
                    Double.isNaN(s.startSocPercent) ? null : s.startSocPercent,
                    Double.isNaN(s.endSocPercent) ? null : s.endSocPercent,
                    s.chargedWhileParked ? 1 : 0,
                    Double.isNaN(s.energyEstKwh) ? null : s.energyEstKwh,
                    Double.isNaN(s.startRemainKwh) ? null : s.startRemainKwh,
                    Double.isNaN(s.endRemainKwh) ? null : s.endRemainKwh
            };
            master.execSQL(sql, args);
            return true;
        } catch (Throwable t) {
            logger.warn("insertSession failed: " + t.getMessage());
            return false;
        }
    }

    public synchronized boolean updateSession(ParkingSession s) {
        if (s == null || s.sessionId == null) return false;
        String sql = "UPDATE parking_sessions SET started_ms=?, ended_ms=?, transition_gen=?,"
                + " end_trigger=?, lat=?, lng=?, accuracy_m=?, fix_age_ms=?, fix_from_cache=?,"
                + " gps_quality=?, place_short=?, place_display=?, place_source=?, safe_zone=?,"
                + " sentry_state=?, arrived_snapshot_ms=?, arrived_snapshot_ok=?,"
                + " returned_snapshot_ms=?, returned_snapshot_ok=?, rectify_strength=?,"
                + " signage_json=?, signage_state=?, notified_started=?, notified_ended=?,"
                + " event_count=?, neighbour_count=?, created_ms=?, start_soc_pct=?,"
                + " end_soc_pct=?, charged_while_parked=?, energy_est_kwh=?,"
                + " start_remain_kwh=?, end_remain_kwh=? WHERE session_id=?";
        try {
            Object[] args = new Object[]{
                    s.startedMs,
                    s.endedMs,
                    s.transitionGeneration,
                    s.endTrigger,
                    s.hasFix() ? s.lat : null,
                    s.hasFix() ? s.lng : null,
                    s.accuracyM,
                    s.fixAgeMs,
                    s.fixFromCache ? 1 : 0,
                    s.gpsQuality,
                    clamp(s.placeShort, 128),
                    clamp(s.placeDisplay, 256),
                    clamp(s.placeSource, 32),
                    clamp(s.safeZone, 64),
                    s.sentryState,
                    s.arrivedSnapshotMs,
                    s.arrivedSnapshotOk ? 1 : 0,
                    s.returnedSnapshotMs,
                    s.returnedSnapshotOk ? 1 : 0,
                    s.rectifyStrength,
                    s.signageJson,
                    s.signageState,
                    s.notifiedStarted ? 1 : 0,
                    s.notifiedEnded ? 1 : 0,
                    s.eventCount,
                    s.neighbourCount,
                    s.createdMs,
                    Double.isNaN(s.startSocPercent) ? null : s.startSocPercent,
                    Double.isNaN(s.endSocPercent) ? null : s.endSocPercent,
                    s.chargedWhileParked ? 1 : 0,
                    Double.isNaN(s.energyEstKwh) ? null : s.energyEstKwh,
                    Double.isNaN(s.startRemainKwh) ? null : s.startRemainKwh,
                    Double.isNaN(s.endRemainKwh) ? null : s.endRemainKwh,
                    s.sessionId
            };
            return master.executeUpdateDelete(sql, args) > 0;
        } catch (Throwable t) {
            logger.warn("updateSession failed: " + t.getMessage());
            return false;
        }
    }

    public synchronized ParkingSession getSession(String sessionId) {
        if (sessionId == null) return null;
        try (Cursor rs = master.rawQuery(
                "SELECT * FROM parking_sessions WHERE session_id=?", new String[]{sessionId})) {
            if (rs != null && rs.moveToFirst()) {
                return readSession(rs);
            }
        } catch (Throwable t) {
            logger.warn("getSession failed: " + t.getMessage());
        }
        return null;
    }

    /** The most recent session that has not been closed, or null. */
    public synchronized ParkingSession getOpenSession() {
        try (Cursor rs = master.rawQuery(
                "SELECT * FROM parking_sessions WHERE ended_ms <= 0 ORDER BY started_ms DESC LIMIT 1", null)) {
            if (rs != null && rs.moveToFirst()) {
                return readSession(rs);
            }
        } catch (Throwable t) {
            logger.warn("getOpenSession failed: " + t.getMessage());
        }
        return null;
    }

    /** Newest session regardless of state (for /where and the dashboard tile). */
    public synchronized ParkingSession getLatestSession() {
        try (Cursor rs = master.rawQuery(
                "SELECT * FROM parking_sessions ORDER BY started_ms DESC LIMIT 1", null)) {
            if (rs != null && rs.moveToFirst()) {
                return readSession(rs);
            }
        } catch (Throwable t) {
            logger.warn("getLatestSession failed: " + t.getMessage());
        }
        return null;
    }

    /**
     * Sessions overlapping [fromMs, toMs] (either bound may be 0 = unbounded),
     * newest first.
     */
    public synchronized List<ParkingSession> listSessions(long fromMs, long toMs, int limit, int offset) {
        List<ParkingSession> out = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM parking_sessions WHERE 1=1");
        List<String> args = new ArrayList<>();
        if (fromMs > 0) {
            sql.append(" AND (ended_ms <= 0 OR ended_ms >= ?)");
            args.add(String.valueOf(fromMs));
        }
        if (toMs > 0) {
            sql.append(" AND started_ms <= ?");
            args.add(String.valueOf(toMs));
        }
        sql.append(" ORDER BY started_ms DESC LIMIT ? OFFSET ?");
        args.add(String.valueOf(Math.max(1, Math.min(limit, 500))));
        args.add(String.valueOf(Math.max(0, offset)));

        try (Cursor rs = master.rawQuery(sql.toString(), args.toArray(new String[0]))) {
            if (rs != null) {
                while (rs.moveToNext()) {
                    out.add(readSession(rs));
                }
            }
        } catch (Throwable t) {
            logger.warn("listSessions failed: " + t.getMessage());
        }
        return out;
    }

    public synchronized int countSessions() {
        try (Cursor rs = master.rawQuery("SELECT COUNT(*) FROM parking_sessions", null)) {
            if (rs != null && rs.moveToFirst()) {
                return rs.getInt(0);
            }
        } catch (Throwable t) {
            logger.warn("countSessions failed: " + t.getMessage());
        }
        return 0;
    }

    public synchronized boolean deleteSession(String sessionId) {
        if (sessionId == null) return false;
        try {
            master.execSQL("DELETE FROM parking_neighbours WHERE session_id=?", new Object[]{sessionId});
            return master.executeUpdateDelete("DELETE FROM parking_sessions WHERE session_id=?", new Object[]{sessionId}) > 0;
        } catch (Throwable t) {
            logger.warn("deleteSession failed: " + t.getMessage());
            return false;
        }
    }

    /** Ids of closed sessions that started before {@code beforeMs} (retention). */
    public synchronized List<String> listSessionIdsStartedBefore(long beforeMs, int limit) {
        return listSessionIdsStartedBetween(Long.MIN_VALUE, beforeMs, limit);
    }

    /**
     * Closed sessions with {@code floorMs <= started_ms < beforeMs}, oldest
     * first. The floor lets retention skip rows stamped by an unset clock.
     */
    public synchronized List<String> listSessionIdsStartedBetween(long floorMs, long beforeMs, int limit) {
        List<String> out = new ArrayList<>();
        try (Cursor rs = master.rawQuery(
                "SELECT session_id FROM parking_sessions WHERE started_ms >= ? AND started_ms < ?"
                        + " AND ended_ms > 0 ORDER BY started_ms ASC LIMIT ?",
                new String[]{String.valueOf(floorMs), String.valueOf(beforeMs), String.valueOf(Math.max(1, limit))})) {
            if (rs != null) {
                while (rs.moveToNext()) {
                    out.add(rs.getString(0));
                }
            }
        } catch (Throwable t) {
            logger.warn("listSessionIdsStartedBetween failed: " + t.getMessage());
        }
        return out;
    }

    /** Every open row, newest first (normally zero or one; more after a crash). */
    public synchronized List<ParkingSession> listOpenSessions() {
        List<ParkingSession> out = new ArrayList<>();
        try (Cursor rs = master.rawQuery(
                "SELECT * FROM parking_sessions WHERE ended_ms <= 0 ORDER BY started_ms DESC", null)) {
            if (rs != null) {
                while (rs.moveToNext()) {
                    out.add(readSession(rs));
                }
            }
        } catch (Throwable t) {
            logger.warn("listOpenSessions failed: " + t.getMessage());
        }
        return out;
    }

    /** Closed sessions whose signage read is still pending (v2 deferred OCR). */
    public synchronized List<ParkingSession> listSessionsWithSignageState(String state, int limit) {
        List<ParkingSession> out = new ArrayList<>();
        try (Cursor rs = master.rawQuery(
                "SELECT * FROM parking_sessions WHERE signage_state=? AND ended_ms > 0"
                        + " ORDER BY started_ms DESC LIMIT ?",
                new String[]{state, String.valueOf(Math.max(1, limit))})) {
            if (rs != null) {
                while (rs.moveToNext()) {
                    out.add(readSession(rs));
                }
            }
        } catch (Throwable t) {
            logger.warn("listSessionsWithSignageState failed: " + t.getMessage());
        }
        return out;
    }

    private static ParkingSession readSession(Cursor rs) {
        ParkingSession s = new ParkingSession();
        s.sessionId = getString(rs, "session_id");
        s.startedMs = getLong(rs, "started_ms");
        s.endedMs = getLong(rs, "ended_ms");
        s.transitionGeneration = getLong(rs, "transition_gen");
        s.endTrigger = getString(rs, "end_trigger");

        int latIdx = rs.getColumnIndex("lat");
        int lngIdx = rs.getColumnIndex("lng");
        if (latIdx >= 0 && lngIdx >= 0 && !rs.isNull(latIdx) && !rs.isNull(lngIdx)) {
            s.lat = rs.getDouble(latIdx);
            s.lng = rs.getDouble(lngIdx);
        }

        s.accuracyM = getFloat(rs, "accuracy_m");
        s.fixAgeMs = getLong(rs, "fix_age_ms");
        s.fixFromCache = getInt(rs, "fix_from_cache") != 0;
        s.gpsQuality = getString(rs, "gps_quality");
        if (s.gpsQuality == null) s.gpsQuality = ParkingSession.GPS_UNKNOWN;

        s.placeShort = getString(rs, "place_short");
        s.placeDisplay = getString(rs, "place_display");
        s.placeSource = getString(rs, "place_source");
        s.safeZone = getString(rs, "safe_zone");
        s.sentryState = getString(rs, "sentry_state");
        if (s.sentryState == null) s.sentryState = ParkingSession.SENTRY_UNKNOWN;

        s.arrivedSnapshotMs = getLong(rs, "arrived_snapshot_ms");
        s.arrivedSnapshotOk = getInt(rs, "arrived_snapshot_ok") != 0;
        s.returnedSnapshotMs = getLong(rs, "returned_snapshot_ms");
        s.returnedSnapshotOk = getInt(rs, "returned_snapshot_ok") != 0;
        s.rectifyStrength = getInt(rs, "rectify_strength");

        s.signageJson = getString(rs, "signage_json");
        s.signageState = getString(rs, "signage_state");
        if (s.signageState == null) s.signageState = ParkingSession.SIGNAGE_PENDING;

        s.notifiedStarted = getInt(rs, "notified_started") != 0;
        s.notifiedEnded = getInt(rs, "notified_ended") != 0;
        s.eventCount = getInt(rs, "event_count");
        s.neighbourCount = getInt(rs, "neighbour_count");
        s.createdMs = getLong(rs, "created_ms");

        int sSocIdx = rs.getColumnIndex("start_soc_pct");
        if (sSocIdx >= 0 && !rs.isNull(sSocIdx)) s.startSocPercent = rs.getDouble(sSocIdx);
        int eSocIdx = rs.getColumnIndex("end_soc_pct");
        if (eSocIdx >= 0 && !rs.isNull(eSocIdx)) s.endSocPercent = rs.getDouble(eSocIdx);

        s.chargedWhileParked = getInt(rs, "charged_while_parked") != 0;

        int estIdx = rs.getColumnIndex("energy_est_kwh");
        if (estIdx >= 0 && !rs.isNull(estIdx)) s.energyEstKwh = rs.getDouble(estIdx);
        int sRemIdx = rs.getColumnIndex("start_remain_kwh");
        if (sRemIdx >= 0 && !rs.isNull(sRemIdx)) s.startRemainKwh = rs.getDouble(sRemIdx);
        int eRemIdx = rs.getColumnIndex("end_remain_kwh");
        if (eRemIdx >= 0 && !rs.isNull(eRemIdx)) s.endRemainKwh = rs.getDouble(eRemIdx);

        return s;
    }

    // ==================== NEIGHBOURS ====================

    /** Insert or update by (session, key). Returns the row id (0 on failure). */
    public synchronized long upsertNeighbour(ParkingNeighbour n) {
        if (n == null || n.sessionId == null || n.neighbourKey == null) return 0L;
        try {
            ParkingNeighbour existing = findNeighbourByKey(n.sessionId, n.neighbourKey);
            if (existing == null) {
                String sql = "INSERT INTO parking_neighbours (session_id, neighbour_key, side, kind,"
                        + " class_group, status, confirmed, first_seen_ms, arrived_ms, departed_ms,"
                        + " last_seen_ms, cx, cy, w, h, proximity, arrival_event, departure_event,"
                        + " actor_ids, frames_json, updated_ms) VALUES ("
                        + "?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                Object[] args = new Object[]{
                        n.sessionId,
                        n.neighbourKey,
                        n.side,
                        n.kind == null ? ParkingNeighbour.KIND_NEIGHBOUR : n.kind,
                        n.classGroup,
                        n.status,
                        n.confirmed ? 1 : 0,
                        n.firstSeenMs,
                        n.arrivedMs,
                        n.departedMs,
                        n.lastSeenMs,
                        n.cx, n.cy, n.w, n.h,
                        n.proximity,
                        clamp(n.arrivalEvent, 256),
                        clamp(n.departureEvent, 256),
                        clamp(n.actorIds, 256),
                        n.framesJson,
                        n.updatedMs
                };
                long newId = master.executeInsert(sql, args);
                if (newId > 0) {
                    n.id = newId;
                }
            } else {
                n.id = existing.id;
                String sql = "UPDATE parking_neighbours SET side=?, kind=?, class_group=?, status=?,"
                        + " confirmed=?, first_seen_ms=?, arrived_ms=?, departed_ms=?, last_seen_ms=?,"
                        + " cx=?, cy=?, w=?, h=?, proximity=?, arrival_event=?, departure_event=?,"
                        + " actor_ids=?, frames_json=?, updated_ms=? WHERE id=?";
                Object[] args = new Object[]{
                        n.side,
                        n.kind == null ? ParkingNeighbour.KIND_NEIGHBOUR : n.kind,
                        n.classGroup,
                        n.status,
                        n.confirmed ? 1 : 0,
                        n.firstSeenMs,
                        n.arrivedMs,
                        n.departedMs,
                        n.lastSeenMs,
                        n.cx, n.cy, n.w, n.h,
                        n.proximity,
                        clamp(n.arrivalEvent, 256),
                        clamp(n.departureEvent, 256),
                        clamp(n.actorIds, 256),
                        n.framesJson,
                        n.updatedMs,
                        n.id
                };
                master.execSQL(sql, args);
            }
            return n.id;
        } catch (Throwable t) {
            logger.warn("upsertNeighbour failed: " + t.getMessage());
            return 0L;
        }
    }

    public synchronized ParkingNeighbour findNeighbourByKey(String sessionId, String key) {
        try (Cursor rs = master.rawQuery(
                "SELECT * FROM parking_neighbours WHERE session_id=? AND neighbour_key=?",
                new String[]{sessionId, key})) {
            if (rs != null && rs.moveToFirst()) {
                return readNeighbour(rs);
            }
        } catch (Throwable t) {
            logger.warn("findNeighbourByKey failed: " + t.getMessage());
        }
        return null;
    }

    public synchronized List<ParkingNeighbour> listNeighbours(String sessionId) {
        List<ParkingNeighbour> out = new ArrayList<>();
        try (Cursor rs = master.rawQuery(
                "SELECT * FROM parking_neighbours WHERE session_id=?"
                        + " ORDER BY side ASC, first_seen_ms ASC",
                new String[]{sessionId})) {
            if (rs != null) {
                while (rs.moveToNext()) {
                    out.add(readNeighbour(rs));
                }
            }
        } catch (Throwable t) {
            logger.warn("listNeighbours failed: " + t.getMessage());
        }
        return out;
    }

    /** Drop the frame lists of a session whose asset folder was reclaimed by the storage cap. */
    public synchronized int clearNeighbourFrames(String sessionId) {
        if (sessionId == null) return 0;
        try {
            master.execSQL("UPDATE parking_neighbours SET frames_json=NULL WHERE session_id=? AND frames_json IS NOT NULL",
                    new Object[]{sessionId});
            return 1;
        } catch (Throwable t) {
            logger.warn("clearNeighbourFrames failed: " + t.getMessage());
            return 0;
        }
    }

    public synchronized int countNeighbours(String sessionId, boolean confirmedOnly) {
        String sql = "SELECT COUNT(*) FROM parking_neighbours WHERE session_id=? AND kind=?"
                + (confirmedOnly ? " AND confirmed=1" : "");
        try (Cursor rs = master.rawQuery(sql, new String[]{sessionId, ParkingNeighbour.KIND_NEIGHBOUR})) {
            if (rs != null && rs.moveToFirst()) {
                return rs.getInt(0);
            }
        } catch (Throwable t) {
            logger.warn("countNeighbours failed: " + t.getMessage());
        }
        return 0;
    }

    private static ParkingNeighbour readNeighbour(Cursor rs) {
        ParkingNeighbour n = new ParkingNeighbour();
        n.id = getLong(rs, "id");
        n.sessionId = getString(rs, "session_id");
        n.neighbourKey = getString(rs, "neighbour_key");
        n.side = getInt(rs, "side");
        n.kind = getString(rs, "kind");
        n.classGroup = getString(rs, "class_group");
        n.status = getString(rs, "status");
        n.confirmed = getInt(rs, "confirmed") != 0;
        n.firstSeenMs = getLong(rs, "first_seen_ms");
        n.arrivedMs = getLong(rs, "arrived_ms");
        n.departedMs = getLong(rs, "departed_ms");
        n.lastSeenMs = getLong(rs, "last_seen_ms");
        n.cx = getFloat(rs, "cx");
        n.cy = getFloat(rs, "cy");
        n.w = getFloat(rs, "w");
        n.h = getFloat(rs, "h");
        n.proximity = getString(rs, "proximity");
        n.arrivalEvent = getString(rs, "arrival_event");
        n.departureEvent = getString(rs, "departure_event");
        n.actorIds = getString(rs, "actor_ids");
        n.framesJson = getString(rs, "frames_json");
        n.updatedMs = getLong(rs, "updated_ms");
        return n;
    }

    // ==================== HELPERS ====================

    private static String getString(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        return (idx >= 0 && !c.isNull(idx)) ? c.getString(idx) : null;
    }

    private static long getLong(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        return (idx >= 0 && !c.isNull(idx)) ? c.getLong(idx) : 0L;
    }

    private static int getInt(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        return (idx >= 0 && !c.isNull(idx)) ? c.getInt(idx) : 0;
    }

    private static float getFloat(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        return (idx >= 0 && !c.isNull(idx)) ? c.getFloat(idx) : 0f;
    }

    private static String clamp(String v, int max) {
        if (v == null) return null;
        return v.length() <= max ? v : v.substring(0, max);
    }
}
