package com.overdrive.app.parking;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.overdrive.app.database.SqliteDatabaseManager;
import com.overdrive.app.database.SqliteStorageEngine;
import com.overdrive.app.logging.DaemonLogger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Durable store for parking sessions and their neighbours.
 *
 * <p>Modernized to support {@link SqliteStorageEngine} with Write-Ahead Logging (WAL)
 * mode in production environments, while maintaining transparent JDBC in-memory fallback
 * for JVM tests.
 */
public final class ParkingStore {

    private static final DaemonLogger logger = DaemonLogger.getInstance("ParkingStore");

    public static final String DEFAULT_DB_PATH = "/data/local/tmp/overdrive_parking.db";

    public static String defaultJdbcUrl() {
        return "jdbc:h2:file:" + DEFAULT_DB_PATH
                + ";FILE_LOCK=SOCKET;TRACE_LEVEL_FILE=0;DB_CLOSE_ON_EXIT=FALSE"
                + ";AUTO_COMPACT_FILL_RATE=50";
    }

    private final String jdbcUrl;
    private volatile Connection connection;
    private volatile SqliteStorageEngine sqliteEngine;

    public ParkingStore() { this(defaultJdbcUrl()); }

    public ParkingStore(String jdbcUrl) { this.jdbcUrl = jdbcUrl; }

    // ==================== LIFECYCLE ====================

    public synchronized boolean open() {
        if (isOpen()) return true;

        // Use high-performance SQLite WAL engine when running in Android environment
        if (SqliteStorageEngine.isAndroidRuntime() && (jdbcUrl == null || jdbcUrl.equals(defaultJdbcUrl()))) {
            try {
                sqliteEngine = SqliteDatabaseManager.getDatabase(SqliteDatabaseManager.DB_PARKING);
                createSqliteTables();
                logger.info("ParkingStore opened via native SQLite WAL engine: " + sqliteEngine.getPath());
                return true;
            } catch (Exception e) {
                logger.error("ParkingStore SQLite open failed: " + e.getMessage(), e);
                return false;
            }
        }

        // Host JVM / unit test JDBC fallback (e.g. jdbc:h2:mem:)
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            logger.error("H2 driver not found", e);
            return false;
        }
        try {
            connection = DriverManager.getConnection(jdbcUrl, "sa", "");
            try (Statement st = connection.createStatement()) {
                st.execute("SET CACHE_SIZE 2048");
            }
            createJdbcTables();
            return true;
        } catch (Exception e) {
            logger.error("Parking store open failed: " + e.getMessage());
            closeQuietly();
            return false;
        }
    }

    public synchronized void close() {
        if (sqliteEngine != null) {
            SqliteDatabaseManager.closeDatabase(SqliteDatabaseManager.DB_PARKING);
            sqliteEngine = null;
        }
        closeQuietly();
    }

    public synchronized boolean isOpen() {
        if (sqliteEngine != null && sqliteEngine.isOpen()) return true;
        try {
            return connection != null && !connection.isClosed();
        } catch (Exception e) {
            return false;
        }
    }

    private void closeQuietly() {
        Connection c = connection;
        connection = null;
        if (c != null) {
            try { c.close(); } catch (Exception ignored) {}
        }
    }

    private Connection conn() throws Exception {
        Connection c = connection;
        if (c == null || c.isClosed()) {
            throw new IllegalStateException("parking store not open");
        }
        return c;
    }

    private void createSqliteTables() {
        sqliteEngine.execSQL("CREATE TABLE IF NOT EXISTS parking_sessions ("
                + "session_id TEXT PRIMARY KEY,"
                + "started_ms INTEGER NOT NULL,"
                + "ended_ms INTEGER DEFAULT 0,"
                + "transition_gen INTEGER DEFAULT 0,"
                + "end_trigger TEXT,"
                + "lat REAL,"
                + "lng REAL,"
                + "accuracy_m REAL DEFAULT 0,"
                + "fix_age_ms INTEGER DEFAULT -1,"
                + "fix_from_cache INTEGER DEFAULT 0,"
                + "gps_quality TEXT,"
                + "place_short TEXT,"
                + "place_display TEXT,"
                + "place_source TEXT,"
                + "safe_zone TEXT,"
                + "sentry_state TEXT,"
                + "arrived_snapshot_ms INTEGER DEFAULT 0,"
                + "arrived_snapshot_ok INTEGER DEFAULT 0,"
                + "returned_snapshot_ms INTEGER DEFAULT 0,"
                + "returned_snapshot_ok INTEGER DEFAULT 0,"
                + "rectify_strength INTEGER DEFAULT 0,"
                + "signage_json TEXT,"
                + "signage_state TEXT,"
                + "notified_started INTEGER DEFAULT 0,"
                + "notified_ended INTEGER DEFAULT 0,"
                + "event_count INTEGER DEFAULT 0,"
                + "neighbour_count INTEGER DEFAULT 0,"
                + "created_ms INTEGER DEFAULT 0,"
                + "start_soc_pct REAL,"
                + "end_soc_pct REAL,"
                + "charged_while_parked INTEGER DEFAULT 0,"
                + "energy_est_kwh REAL,"
                + "start_remain_kwh REAL,"
                + "end_remain_kwh REAL"
                + ")");
        sqliteEngine.execSQL("CREATE INDEX IF NOT EXISTS idx_parking_sessions_started ON parking_sessions(started_ms DESC)");
        sqliteEngine.execSQL("CREATE INDEX IF NOT EXISTS idx_parking_sessions_ended ON parking_sessions(ended_ms)");
        sqliteEngine.execSQL("CREATE TABLE IF NOT EXISTS parking_neighbours ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "session_id TEXT NOT NULL,"
                + "neighbour_key TEXT,"
                + "side INTEGER NOT NULL,"
                + "kind TEXT NOT NULL,"
                + "class_group TEXT,"
                + "status TEXT,"
                + "confirmed INTEGER DEFAULT 0,"
                + "first_seen_ms INTEGER DEFAULT 0,"
                + "arrived_ms INTEGER DEFAULT 0,"
                + "departed_ms INTEGER DEFAULT 0,"
                + "last_seen_ms INTEGER DEFAULT 0,"
                + "cx REAL DEFAULT 0, cy REAL DEFAULT 0, w REAL DEFAULT 0, h REAL DEFAULT 0,"
                + "proximity TEXT,"
                + "arrival_event TEXT,"
                + "departure_event TEXT,"
                + "actor_ids TEXT,"
                + "frames_json TEXT,"
                + "updated_ms INTEGER DEFAULT 0"
                + ")");
        sqliteEngine.execSQL("CREATE INDEX IF NOT EXISTS idx_parking_neighbours_session ON parking_neighbours(session_id)");
        sqliteEngine.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_parking_neighbours_key ON parking_neighbours(session_id, neighbour_key)");
    }

    private void createJdbcTables() throws Exception {
        try (Statement st = conn().createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS parking_sessions ("
                    + "session_id VARCHAR(48) PRIMARY KEY,"
                    + "started_ms BIGINT NOT NULL,"
                    + "ended_ms BIGINT DEFAULT 0,"
                    + "transition_gen BIGINT DEFAULT 0,"
                    + "end_trigger VARCHAR(16),"
                    + "lat DOUBLE,"
                    + "lng DOUBLE,"
                    + "accuracy_m REAL DEFAULT 0,"
                    + "fix_age_ms BIGINT DEFAULT -1,"
                    + "fix_from_cache BOOLEAN DEFAULT FALSE,"
                    + "gps_quality VARCHAR(16),"
                    + "place_short VARCHAR(128),"
                    + "place_display VARCHAR(256),"
                    + "place_source VARCHAR(32),"
                    + "safe_zone VARCHAR(64),"
                    + "sentry_state VARCHAR(32),"
                    + "arrived_snapshot_ms BIGINT DEFAULT 0,"
                    + "arrived_snapshot_ok BOOLEAN DEFAULT FALSE,"
                    + "returned_snapshot_ms BIGINT DEFAULT 0,"
                    + "returned_snapshot_ok BOOLEAN DEFAULT FALSE,"
                    + "rectify_strength INT DEFAULT 0,"
                    + "signage_json CLOB,"
                    + "signage_state VARCHAR(16),"
                    + "notified_started BOOLEAN DEFAULT FALSE,"
                    + "notified_ended BOOLEAN DEFAULT FALSE,"
                    + "event_count INT DEFAULT 0,"
                    + "neighbour_count INT DEFAULT 0,"
                    + "created_ms BIGINT DEFAULT 0,"
                    + "start_soc_pct DOUBLE,"
                    + "end_soc_pct DOUBLE,"
                    + "charged_while_parked BOOLEAN DEFAULT FALSE,"
                    + "energy_est_kwh DOUBLE,"
                    + "start_remain_kwh DOUBLE,"
                    + "end_remain_kwh DOUBLE"
                    + ")");
            st.execute("ALTER TABLE parking_sessions ADD COLUMN IF NOT EXISTS start_soc_pct DOUBLE");
            st.execute("ALTER TABLE parking_sessions ADD COLUMN IF NOT EXISTS end_soc_pct DOUBLE");
            st.execute("ALTER TABLE parking_sessions ADD COLUMN IF NOT EXISTS charged_while_parked BOOLEAN DEFAULT FALSE");
            st.execute("ALTER TABLE parking_sessions ADD COLUMN IF NOT EXISTS energy_est_kwh DOUBLE");
            st.execute("ALTER TABLE parking_sessions ADD COLUMN IF NOT EXISTS start_remain_kwh DOUBLE");
            st.execute("ALTER TABLE parking_sessions ADD COLUMN IF NOT EXISTS end_remain_kwh DOUBLE");
            st.execute("CREATE INDEX IF NOT EXISTS idx_parking_sessions_started ON parking_sessions(started_ms DESC)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_parking_sessions_ended ON parking_sessions(ended_ms)");

            st.execute("CREATE TABLE IF NOT EXISTS parking_neighbours ("
                    + "id IDENTITY PRIMARY KEY,"
                    + "session_id VARCHAR(48) NOT NULL,"
                    + "neighbour_key VARCHAR(64),"
                    + "side INT NOT NULL,"
                    + "kind VARCHAR(16) NOT NULL,"
                    + "class_group VARCHAR(16),"
                    + "status VARCHAR(24),"
                    + "confirmed BOOLEAN DEFAULT FALSE,"
                    + "first_seen_ms BIGINT DEFAULT 0,"
                    + "arrived_ms BIGINT DEFAULT 0,"
                    + "departed_ms BIGINT DEFAULT 0,"
                    + "last_seen_ms BIGINT DEFAULT 0,"
                    + "cx REAL DEFAULT 0, cy REAL DEFAULT 0, w REAL DEFAULT 0, h REAL DEFAULT 0,"
                    + "proximity VARCHAR(16),"
                    + "arrival_event VARCHAR(256),"
                    + "departure_event VARCHAR(256),"
                    + "actor_ids VARCHAR(256),"
                    + "frames_json CLOB,"
                    + "updated_ms BIGINT DEFAULT 0"
                    + ")");
            st.execute("CREATE INDEX IF NOT EXISTS idx_parking_neighbours_session ON parking_neighbours(session_id)");
            st.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_parking_neighbours_key ON parking_neighbours(session_id, neighbour_key)");
        }
    }

    // ==================== SESSIONS ====================

    public synchronized boolean insertSession(ParkingSession s) {
        if (sqliteEngine != null) {
            ContentValues cv = sessionToContentValues(s);
            long id = sqliteEngine.insertWithOnConflict("parking_sessions", cv, SQLiteDatabase.CONFLICT_FAIL);
            return id >= 0;
        }

        String sql = "INSERT INTO parking_sessions (session_id, started_ms, ended_ms,"
                + " transition_gen, end_trigger, lat, lng, accuracy_m, fix_age_ms,"
                + " fix_from_cache, gps_quality, place_short, place_display, place_source,"
                + " safe_zone, sentry_state, arrived_snapshot_ms, arrived_snapshot_ok,"
                + " returned_snapshot_ms, returned_snapshot_ok, rectify_strength,"
                + " signage_json, signage_state, notified_started, notified_ended,"
                + " event_count, neighbour_count, created_ms, start_soc_pct,"
                + " end_soc_pct, charged_while_parked, energy_est_kwh,"
                + " start_remain_kwh, end_remain_kwh) VALUES ("
                + "?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            bindSession(ps, s, 1);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            logger.warn("insertSession failed: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean updateSession(ParkingSession s) {
        if (sqliteEngine != null) {
            ContentValues cv = sessionToContentValues(s);
            return sqliteEngine.update("parking_sessions", cv, "session_id=?", new String[]{s.sessionId}) > 0;
        }

        String sql = "UPDATE parking_sessions SET started_ms=?, ended_ms=?, transition_gen=?,"
                + " end_trigger=?, lat=?, lng=?, accuracy_m=?, fix_age_ms=?, fix_from_cache=?,"
                + " gps_quality=?, place_short=?, place_display=?, place_source=?, safe_zone=?,"
                + " sentry_state=?, arrived_snapshot_ms=?, arrived_snapshot_ok=?,"
                + " returned_snapshot_ms=?, returned_snapshot_ok=?, rectify_strength=?,"
                + " signage_json=?, signage_state=?, notified_started=?, notified_ended=?,"
                + " event_count=?, neighbour_count=?, created_ms=?, start_soc_pct=?,"
                + " end_soc_pct=?, charged_while_parked=?, energy_est_kwh=?,"
                + " start_remain_kwh=?, end_remain_kwh=? WHERE session_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            int i = bindSessionBody(ps, s, 1);
            ps.setString(i, s.sessionId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            logger.warn("updateSession failed: " + e.getMessage());
            return false;
        }
    }

    private static ContentValues sessionToContentValues(ParkingSession s) {
        ContentValues cv = new ContentValues();
        cv.put("session_id", s.sessionId);
        cv.put("started_ms", s.startedMs);
        cv.put("ended_ms", s.endedMs);
        cv.put("transition_gen", s.transitionGeneration);
        cv.put("end_trigger", s.endTrigger);
        if (s.hasFix()) {
            cv.put("lat", s.lat);
            cv.put("lng", s.lng);
        } else {
            cv.putNull("lat");
            cv.putNull("lng");
        }
        cv.put("accuracy_m", s.accuracyM);
        cv.put("fix_age_ms", s.fixAgeMs);
        cv.put("fix_from_cache", s.fixFromCache ? 1 : 0);
        cv.put("gps_quality", s.gpsQuality);
        cv.put("place_short", clamp(s.placeShort, 128));
        cv.put("place_display", clamp(s.placeDisplay, 256));
        cv.put("place_source", clamp(s.placeSource, 32));
        cv.put("safe_zone", clamp(s.safeZone, 64));
        cv.put("sentry_state", s.sentryState);
        cv.put("arrived_snapshot_ms", s.arrivedSnapshotMs);
        cv.put("arrived_snapshot_ok", s.arrivedSnapshotOk ? 1 : 0);
        cv.put("returned_snapshot_ms", s.returnedSnapshotMs);
        cv.put("returned_snapshot_ok", s.returnedSnapshotOk ? 1 : 0);
        cv.put("rectify_strength", s.rectifyStrength);
        cv.put("signage_json", s.signageJson);
        cv.put("signage_state", s.signageState);
        cv.put("notified_started", s.notifiedStarted ? 1 : 0);
        cv.put("notified_ended", s.notifiedEnded ? 1 : 0);
        cv.put("event_count", s.eventCount);
        cv.put("neighbour_count", s.neighbourCount);
        cv.put("created_ms", s.createdMs);
        if (!Double.isNaN(s.startSocPercent)) cv.put("start_soc_pct", s.startSocPercent); else cv.putNull("start_soc_pct");
        if (!Double.isNaN(s.endSocPercent)) cv.put("end_soc_pct", s.endSocPercent); else cv.putNull("end_soc_pct");
        cv.put("charged_while_parked", s.chargedWhileParked ? 1 : 0);
        if (!Double.isNaN(s.energyEstKwh)) cv.put("energy_est_kwh", s.energyEstKwh); else cv.putNull("energy_est_kwh");
        if (!Double.isNaN(s.startRemainKwh)) cv.put("start_remain_kwh", s.startRemainKwh); else cv.putNull("start_remain_kwh");
        if (!Double.isNaN(s.endRemainKwh)) cv.put("end_remain_kwh", s.endRemainKwh); else cv.putNull("end_remain_kwh");
        return cv;
    }

    private static int bindSession(PreparedStatement ps, ParkingSession s, int i) throws Exception {
        ps.setString(i++, s.sessionId);
        return bindSessionBody(ps, s, i);
    }

    private static int bindSessionBody(PreparedStatement ps, ParkingSession s, int i) throws Exception {
        ps.setLong(i++, s.startedMs);
        ps.setLong(i++, s.endedMs);
        ps.setLong(i++, s.transitionGeneration);
        setStr(ps, i++, s.endTrigger);
        if (s.hasFix()) { ps.setDouble(i++, s.lat); ps.setDouble(i++, s.lng); }
        else { ps.setNull(i++, java.sql.Types.DOUBLE); ps.setNull(i++, java.sql.Types.DOUBLE); }
        ps.setFloat(i++, s.accuracyM);
        ps.setLong(i++, s.fixAgeMs);
        ps.setBoolean(i++, s.fixFromCache);
        setStr(ps, i++, s.gpsQuality);
        setStr(ps, i++, clamp(s.placeShort, 128));
        setStr(ps, i++, clamp(s.placeDisplay, 256));
        setStr(ps, i++, clamp(s.placeSource, 32));
        setStr(ps, i++, clamp(s.safeZone, 64));
        setStr(ps, i++, s.sentryState);
        ps.setLong(i++, s.arrivedSnapshotMs);
        ps.setBoolean(i++, s.arrivedSnapshotOk);
        ps.setLong(i++, s.returnedSnapshotMs);
        ps.setBoolean(i++, s.returnedSnapshotOk);
        ps.setInt(i++, s.rectifyStrength);
        setStr(ps, i++, s.signageJson);
        setStr(ps, i++, s.signageState);
        ps.setBoolean(i++, s.notifiedStarted);
        ps.setBoolean(i++, s.notifiedEnded);
        ps.setInt(i++, s.eventCount);
        ps.setInt(i++, s.neighbourCount);
        ps.setLong(i++, s.createdMs);
        setNullableDouble(ps, i++, s.startSocPercent);
        setNullableDouble(ps, i++, s.endSocPercent);
        ps.setBoolean(i++, s.chargedWhileParked);
        setNullableDouble(ps, i++, s.energyEstKwh);
        setNullableDouble(ps, i++, s.startRemainKwh);
        setNullableDouble(ps, i++, s.endRemainKwh);
        return i;
    }

    public synchronized ParkingSession getSession(String sessionId) {
        if (sessionId == null) return null;
        if (sqliteEngine != null) {
            return sqliteEngine.queryOne("SELECT * FROM parking_sessions WHERE session_id=?",
                    new String[]{sessionId}, ParkingStore::readSessionFromCursor);
        }

        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM parking_sessions WHERE session_id=?")) {
            ps.setString(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? readSession(rs) : null;
            }
        } catch (Exception e) {
            logger.warn("getSession failed: " + e.getMessage());
            return null;
        }
    }

    /** The most recent session that has not been closed, or null. */
    public synchronized ParkingSession getOpenSession() {
        if (sqliteEngine != null) {
            return sqliteEngine.queryOne("SELECT * FROM parking_sessions WHERE ended_ms <= 0 ORDER BY started_ms DESC LIMIT 1",
                    null, ParkingStore::readSessionFromCursor);
        }

        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM parking_sessions WHERE ended_ms <= 0"
                        + " ORDER BY started_ms DESC LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? readSession(rs) : null;
        } catch (Exception e) {
            logger.warn("getOpenSession failed: " + e.getMessage());
            return null;
        }
    }

    /** Newest session regardless of state (for /where and the dashboard tile). */
    public synchronized ParkingSession getLatestSession() {
        if (sqliteEngine != null) {
            return sqliteEngine.queryOne("SELECT * FROM parking_sessions ORDER BY started_ms DESC LIMIT 1",
                    null, ParkingStore::readSessionFromCursor);
        }

        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM parking_sessions ORDER BY started_ms DESC LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? readSession(rs) : null;
        } catch (Exception e) {
            logger.warn("getLatestSession failed: " + e.getMessage());
            return null;
        }
    }

    public synchronized List<ParkingSession> listSessions(long fromMs, long toMs, int limit, int offset) {
        if (sqliteEngine != null) {
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
            sql.append(" ORDER BY started_ms DESC LIMIT ").append(Math.max(1, Math.min(limit, 500)))
               .append(" OFFSET ").append(Math.max(0, offset));
            return sqliteEngine.query(sql.toString(), args.toArray(new String[0]), ParkingStore::readSessionFromCursor);
        }

        List<ParkingSession> out = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM parking_sessions WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (fromMs > 0) {
            sql.append(" AND (ended_ms <= 0 OR ended_ms >= ?)");
            args.add(fromMs);
        }
        if (toMs > 0) {
            sql.append(" AND started_ms <= ?");
            args.add(toMs);
        }
        sql.append(" ORDER BY started_ms DESC LIMIT ? OFFSET ?");
        try (PreparedStatement ps = conn().prepareStatement(sql.toString())) {
            int i = 1;
            for (Object a : args) ps.setLong(i++, (Long) a);
            ps.setInt(i++, Math.max(1, Math.min(limit, 500)));
            ps.setInt(i, Math.max(0, offset));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(readSession(rs));
            }
        } catch (Exception e) {
            logger.warn("listSessions failed: " + e.getMessage());
        }
        return out;
    }

    public synchronized int countSessions() {
        if (sqliteEngine != null) {
            return (int) sqliteEngine.queryLong("SELECT COUNT(*) FROM parking_sessions", null, 0L);
        }

        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM parking_sessions")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public synchronized boolean deleteSession(String sessionId) {
        if (sessionId == null) return false;
        if (sqliteEngine != null) {
            sqliteEngine.delete("parking_neighbours", "session_id=?", new String[]{sessionId});
            return sqliteEngine.delete("parking_sessions", "session_id=?", new String[]{sessionId}) > 0;
        }

        try {
            try (PreparedStatement ps = conn().prepareStatement(
                    "DELETE FROM parking_neighbours WHERE session_id=?")) {
                ps.setString(1, sessionId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn().prepareStatement(
                    "DELETE FROM parking_sessions WHERE session_id=?")) {
                ps.setString(1, sessionId);
                return ps.executeUpdate() > 0;
            }
        } catch (Exception e) {
            logger.warn("deleteSession failed: " + e.getMessage());
            return false;
        }
    }

    public synchronized List<String> listSessionIdsStartedBefore(long beforeMs, int limit) {
        return listSessionIdsStartedBetween(Long.MIN_VALUE, beforeMs, limit);
    }

    public synchronized List<String> listSessionIdsStartedBetween(long floorMs, long beforeMs, int limit) {
        if (sqliteEngine != null) {
            return sqliteEngine.query(
                "SELECT session_id FROM parking_sessions WHERE started_ms >= ? AND started_ms < ?"
                        + " AND ended_ms > 0 ORDER BY started_ms ASC LIMIT " + Math.max(1, limit),
                new String[]{String.valueOf(floorMs), String.valueOf(beforeMs)},
                c -> c.getString(0)
            );
        }

        List<String> out = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT session_id FROM parking_sessions WHERE started_ms >= ? AND started_ms < ?"
                        + " AND ended_ms > 0 ORDER BY started_ms ASC LIMIT ?")) {
            ps.setLong(1, floorMs);
            ps.setLong(2, beforeMs);
            ps.setInt(3, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getString(1));
            }
        } catch (Exception e) {
            logger.warn("listSessionIdsStartedBetween failed: " + e.getMessage());
        }
        return out;
    }

    public synchronized List<ParkingSession> listOpenSessions() {
        if (sqliteEngine != null) {
            return sqliteEngine.query("SELECT * FROM parking_sessions WHERE ended_ms <= 0 ORDER BY started_ms DESC",
                    null, ParkingStore::readSessionFromCursor);
        }

        List<ParkingSession> out = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM parking_sessions WHERE ended_ms <= 0 ORDER BY started_ms DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(readSession(rs));
        } catch (Exception e) {
            logger.warn("listOpenSessions failed: " + e.getMessage());
        }
        return out;
    }

    public synchronized List<ParkingSession> listSessionsWithSignageState(String state, int limit) {
        if (sqliteEngine != null) {
            return sqliteEngine.query("SELECT * FROM parking_sessions WHERE signage_state=? AND ended_ms > 0"
                    + " ORDER BY started_ms DESC LIMIT " + Math.max(1, limit),
                    new String[]{state}, ParkingStore::readSessionFromCursor);
        }

        List<ParkingSession> out = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM parking_sessions WHERE signage_state=? AND ended_ms > 0"
                        + " ORDER BY started_ms DESC LIMIT ?")) {
            ps.setString(1, state);
            ps.setInt(2, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(readSession(rs));
            }
        } catch (Exception e) {
            logger.warn("listSessionsWithSignageState failed: " + e.getMessage());
        }
        return out;
    }

    private static ParkingSession readSessionFromCursor(Cursor c) {
        ParkingSession s = new ParkingSession();
        s.sessionId = c.getString(c.getColumnIndexOrThrow("session_id"));
        s.startedMs = c.getLong(c.getColumnIndexOrThrow("started_ms"));
        s.endedMs = c.getLong(c.getColumnIndexOrThrow("ended_ms"));
        s.transitionGeneration = c.getLong(c.getColumnIndexOrThrow("transition_gen"));
        int endTrigIdx = c.getColumnIndex("end_trigger");
        if (endTrigIdx >= 0 && !c.isNull(endTrigIdx)) s.endTrigger = c.getString(endTrigIdx);
        int latIdx = c.getColumnIndex("lat");
        int lngIdx = c.getColumnIndex("lng");
        if (latIdx >= 0 && lngIdx >= 0 && !c.isNull(latIdx) && !c.isNull(lngIdx)) {
            s.lat = c.getDouble(latIdx);
            s.lng = c.getDouble(lngIdx);
        }
        int accIdx = c.getColumnIndex("accuracy_m");
        if (accIdx >= 0 && !c.isNull(accIdx)) s.accuracyM = c.getFloat(accIdx);
        int ageIdx = c.getColumnIndex("fix_age_ms");
        if (ageIdx >= 0 && !c.isNull(ageIdx)) s.fixAgeMs = c.getLong(ageIdx);
        int cacheIdx = c.getColumnIndex("fix_from_cache");
        if (cacheIdx >= 0 && !c.isNull(cacheIdx)) s.fixFromCache = c.getInt(cacheIdx) == 1;
        int gpsIdx = c.getColumnIndex("gps_quality");
        s.gpsQuality = (gpsIdx >= 0 && !c.isNull(gpsIdx)) ? c.getString(gpsIdx) : ParkingSession.GPS_UNKNOWN;
        int pShortIdx = c.getColumnIndex("place_short");
        if (pShortIdx >= 0 && !c.isNull(pShortIdx)) s.placeShort = c.getString(pShortIdx);
        int pDispIdx = c.getColumnIndex("place_display");
        if (pDispIdx >= 0 && !c.isNull(pDispIdx)) s.placeDisplay = c.getString(pDispIdx);
        int pSrcIdx = c.getColumnIndex("place_source");
        if (pSrcIdx >= 0 && !c.isNull(pSrcIdx)) s.placeSource = c.getString(pSrcIdx);
        int zoneIdx = c.getColumnIndex("safe_zone");
        if (zoneIdx >= 0 && !c.isNull(zoneIdx)) s.safeZone = c.getString(zoneIdx);
        int sentryIdx = c.getColumnIndex("sentry_state");
        s.sentryState = (sentryIdx >= 0 && !c.isNull(sentryIdx)) ? c.getString(sentryIdx) : ParkingSession.SENTRY_UNKNOWN;
        int arrMsIdx = c.getColumnIndex("arrived_snapshot_ms");
        if (arrMsIdx >= 0 && !c.isNull(arrMsIdx)) s.arrivedSnapshotMs = c.getLong(arrMsIdx);
        int arrOkIdx = c.getColumnIndex("arrived_snapshot_ok");
        if (arrOkIdx >= 0 && !c.isNull(arrOkIdx)) s.arrivedSnapshotOk = c.getInt(arrOkIdx) == 1;
        int retMsIdx = c.getColumnIndex("returned_snapshot_ms");
        if (retMsIdx >= 0 && !c.isNull(retMsIdx)) s.returnedSnapshotMs = c.getLong(retMsIdx);
        int retOkIdx = c.getColumnIndex("returned_snapshot_ok");
        if (retOkIdx >= 0 && !c.isNull(retOkIdx)) s.returnedSnapshotOk = c.getInt(retOkIdx) == 1;
        int rectIdx = c.getColumnIndex("rectify_strength");
        if (rectIdx >= 0 && !c.isNull(rectIdx)) s.rectifyStrength = c.getInt(rectIdx);
        int signJIdx = c.getColumnIndex("signage_json");
        if (signJIdx >= 0 && !c.isNull(signJIdx)) s.signageJson = c.getString(signJIdx);
        int signSIdx = c.getColumnIndex("signage_state");
        s.signageState = (signSIdx >= 0 && !c.isNull(signSIdx)) ? c.getString(signSIdx) : ParkingSession.SIGNAGE_PENDING;
        int nStartIdx = c.getColumnIndex("notified_started");
        if (nStartIdx >= 0 && !c.isNull(nStartIdx)) s.notifiedStarted = c.getInt(nStartIdx) == 1;
        int nEndIdx = c.getColumnIndex("notified_ended");
        if (nEndIdx >= 0 && !c.isNull(nEndIdx)) s.notifiedEnded = c.getInt(nEndIdx) == 1;
        int evCntIdx = c.getColumnIndex("event_count");
        if (evCntIdx >= 0 && !c.isNull(evCntIdx)) s.eventCount = c.getInt(evCntIdx);
        int nbCntIdx = c.getColumnIndex("neighbour_count");
        if (nbCntIdx >= 0 && !c.isNull(nbCntIdx)) s.neighbourCount = c.getInt(nbCntIdx);
        int crMsIdx = c.getColumnIndex("created_ms");
        if (crMsIdx >= 0 && !c.isNull(crMsIdx)) s.createdMs = c.getLong(crMsIdx);
        int sSocIdx = c.getColumnIndex("start_soc_pct");
        if (sSocIdx >= 0 && !c.isNull(sSocIdx)) s.startSocPercent = c.getDouble(sSocIdx);
        int eSocIdx = c.getColumnIndex("end_soc_pct");
        if (eSocIdx >= 0 && !c.isNull(eSocIdx)) s.endSocPercent = c.getDouble(eSocIdx);
        int chgIdx = c.getColumnIndex("charged_while_parked");
        if (chgIdx >= 0 && !c.isNull(chgIdx)) s.chargedWhileParked = c.getInt(chgIdx) == 1;
        int nrgIdx = c.getColumnIndex("energy_est_kwh");
        if (nrgIdx >= 0 && !c.isNull(nrgIdx)) s.energyEstKwh = c.getDouble(nrgIdx);
        int sRemIdx = c.getColumnIndex("start_remain_kwh");
        if (sRemIdx >= 0 && !c.isNull(sRemIdx)) s.startRemainKwh = c.getDouble(sRemIdx);
        int eRemIdx = c.getColumnIndex("end_remain_kwh");
        if (eRemIdx >= 0 && !c.isNull(eRemIdx)) s.endRemainKwh = c.getDouble(eRemIdx);
        return s;
    }

    private static ParkingSession readSession(ResultSet rs) throws Exception {
        ParkingSession s = new ParkingSession();
        s.sessionId = rs.getString("session_id");
        s.startedMs = rs.getLong("started_ms");
        s.endedMs = rs.getLong("ended_ms");
        s.transitionGeneration = rs.getLong("transition_gen");
        s.endTrigger = rs.getString("end_trigger");
        double lat = rs.getDouble("lat");
        boolean latNull = rs.wasNull();
        double lng = rs.getDouble("lng");
        boolean lngNull = rs.wasNull();
        if (!latNull && !lngNull) { s.lat = lat; s.lng = lng; }
        s.accuracyM = rs.getFloat("accuracy_m");
        s.fixAgeMs = rs.getLong("fix_age_ms");
        s.fixFromCache = rs.getBoolean("fix_from_cache");
        s.gpsQuality = rs.getString("gps_quality");
        if (s.gpsQuality == null) s.gpsQuality = ParkingSession.GPS_UNKNOWN;
        s.placeShort = rs.getString("place_short");
        s.placeDisplay = rs.getString("place_display");
        s.placeSource = rs.getString("place_source");
        s.safeZone = rs.getString("safe_zone");
        s.sentryState = rs.getString("sentry_state");
        if (s.sentryState == null) s.sentryState = ParkingSession.SENTRY_UNKNOWN;
        s.arrivedSnapshotMs = rs.getLong("arrived_snapshot_ms");
        s.arrivedSnapshotOk = rs.getBoolean("arrived_snapshot_ok");
        s.returnedSnapshotMs = rs.getLong("returned_snapshot_ms");
        s.returnedSnapshotOk = rs.getBoolean("returned_snapshot_ok");
        s.rectifyStrength = rs.getInt("rectify_strength");
        s.signageJson = rs.getString("signage_json");
        s.signageState = rs.getString("signage_state");
        if (s.signageState == null) s.signageState = ParkingSession.SIGNAGE_PENDING;
        s.notifiedStarted = rs.getBoolean("notified_started");
        s.notifiedEnded = rs.getBoolean("notified_ended");
        s.eventCount = rs.getInt("event_count");
        s.neighbourCount = rs.getInt("neighbour_count");
        s.createdMs = rs.getLong("created_ms");
        double startSoc = rs.getDouble("start_soc_pct");
        if (!rs.wasNull()) s.startSocPercent = startSoc;
        double endSoc = rs.getDouble("end_soc_pct");
        if (!rs.wasNull()) s.endSocPercent = endSoc;
        s.chargedWhileParked = rs.getBoolean("charged_while_parked");
        double estKwh = rs.getDouble("energy_est_kwh");
        if (!rs.wasNull()) s.energyEstKwh = estKwh;
        double startKwh = rs.getDouble("start_remain_kwh");
        if (!rs.wasNull()) s.startRemainKwh = startKwh;
        double endKwh = rs.getDouble("end_remain_kwh");
        if (!rs.wasNull()) s.endRemainKwh = endKwh;
        return s;
    }

    // ==================== NEIGHBOURS ====================

    public synchronized long upsertNeighbour(ParkingNeighbour n) {
        if (n == null || n.sessionId == null || n.neighbourKey == null) return 0L;
        if (sqliteEngine != null) {
            ParkingNeighbour existing = findNeighbourByKey(n.sessionId, n.neighbourKey);
            ContentValues cv = neighbourToContentValues(n);
            if (existing == null) {
                long rowId = sqliteEngine.insert("parking_neighbours", cv);
                if (rowId > 0) n.id = rowId;
                return rowId;
            } else {
                n.id = existing.id;
                sqliteEngine.update("parking_neighbours", cv, "id=?", new String[]{String.valueOf(n.id)});
                return n.id;
            }
        }

        try {
            ParkingNeighbour existing = findNeighbourByKey(n.sessionId, n.neighbourKey);
            if (existing == null) {
                String sql = "INSERT INTO parking_neighbours (session_id, neighbour_key, side, kind,"
                        + " class_group, status, confirmed, first_seen_ms, arrived_ms, departed_ms,"
                        + " last_seen_ms, cx, cy, w, h, proximity, arrival_event, departure_event,"
                        + " actor_ids, frames_json, updated_ms) VALUES ("
                        + "?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                try (PreparedStatement ps = conn().prepareStatement(sql,
                        Statement.RETURN_GENERATED_KEYS)) {
                    bindNeighbour(ps, n, 1, true);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) n.id = keys.getLong(1);
                    }
                }
            } else {
                n.id = existing.id;
                String sql = "UPDATE parking_neighbours SET side=?, kind=?, class_group=?, status=?,"
                        + " confirmed=?, first_seen_ms=?, arrived_ms=?, departed_ms=?, last_seen_ms=?,"
                        + " cx=?, cy=?, w=?, h=?, proximity=?, arrival_event=?, departure_event=?,"
                        + " actor_ids=?, frames_json=?, updated_ms=? WHERE id=?";
                try (PreparedStatement ps = conn().prepareStatement(sql)) {
                    int i = bindNeighbour(ps, n, 1, false);
                    ps.setLong(i, n.id);
                    ps.executeUpdate();
                }
            }
            return n.id;
        } catch (Exception e) {
            logger.warn("upsertNeighbour failed: " + e.getMessage());
            return 0L;
        }
    }

    private static ContentValues neighbourToContentValues(ParkingNeighbour n) {
        ContentValues cv = new ContentValues();
        cv.put("session_id", n.sessionId);
        cv.put("neighbour_key", n.neighbourKey);
        cv.put("side", n.side);
        cv.put("kind", n.kind == null ? ParkingNeighbour.KIND_NEIGHBOUR : n.kind);
        cv.put("class_group", n.classGroup);
        cv.put("status", n.status);
        cv.put("confirmed", n.confirmed ? 1 : 0);
        cv.put("first_seen_ms", n.firstSeenMs);
        cv.put("arrived_ms", n.arrivedMs);
        cv.put("departed_ms", n.departedMs);
        cv.put("last_seen_ms", n.lastSeenMs);
        cv.put("cx", n.cx);
        cv.put("cy", n.cy);
        cv.put("w", n.w);
        cv.put("h", n.h);
        cv.put("proximity", n.proximity);
        cv.put("arrival_event", clamp(n.arrivalEvent, 256));
        cv.put("departure_event", clamp(n.departureEvent, 256));
        cv.put("actor_ids", clamp(n.actorIds, 256));
        cv.put("frames_json", n.framesJson);
        cv.put("updated_ms", n.updatedMs);
        return cv;
    }

    private static int bindNeighbour(PreparedStatement ps, ParkingNeighbour n, int i,
                                     boolean includeIdentity) throws Exception {
        if (includeIdentity) {
            ps.setString(i++, n.sessionId);
            ps.setString(i++, n.neighbourKey);
        }
        ps.setInt(i++, n.side);
        ps.setString(i++, n.kind == null ? ParkingNeighbour.KIND_NEIGHBOUR : n.kind);
        setStr(ps, i++, n.classGroup);
        setStr(ps, i++, n.status);
        ps.setBoolean(i++, n.confirmed);
        ps.setLong(i++, n.firstSeenMs);
        ps.setLong(i++, n.arrivedMs);
        ps.setLong(i++, n.departedMs);
        ps.setLong(i++, n.lastSeenMs);
        ps.setFloat(i++, n.cx); ps.setFloat(i++, n.cy); ps.setFloat(i++, n.w); ps.setFloat(i++, n.h);
        setStr(ps, i++, n.proximity);
        setStr(ps, i++, clamp(n.arrivalEvent, 256));
        setStr(ps, i++, clamp(n.departureEvent, 256));
        setStr(ps, i++, clamp(n.actorIds, 256));
        setStr(ps, i++, n.framesJson);
        ps.setLong(i++, n.updatedMs);
        return i;
    }

    public synchronized ParkingNeighbour findNeighbourByKey(String sessionId, String key) {
        if (sqliteEngine != null) {
            return sqliteEngine.queryOne("SELECT * FROM parking_neighbours WHERE session_id=? AND neighbour_key=?",
                    new String[]{sessionId, key}, ParkingStore::readNeighbourFromCursor);
        }

        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM parking_neighbours WHERE session_id=? AND neighbour_key=?")) {
            ps.setString(1, sessionId);
            ps.setString(2, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? readNeighbour(rs) : null;
            }
        } catch (Exception e) {
            logger.warn("findNeighbourByKey failed: " + e.getMessage());
            return null;
        }
    }

    public synchronized List<ParkingNeighbour> listNeighbours(String sessionId) {
        if (sqliteEngine != null) {
            return sqliteEngine.query("SELECT * FROM parking_neighbours WHERE session_id=? ORDER BY side ASC, first_seen_ms ASC",
                    new String[]{sessionId}, ParkingStore::readNeighbourFromCursor);
        }

        List<ParkingNeighbour> out = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM parking_neighbours WHERE session_id=?"
                        + " ORDER BY side ASC, first_seen_ms ASC")) {
            ps.setString(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(readNeighbour(rs));
            }
        } catch (Exception e) {
            logger.warn("listNeighbours failed: " + e.getMessage());
        }
        return out;
    }

    public synchronized int clearNeighbourFrames(String sessionId) {
        if (sessionId == null) return 0;
        if (sqliteEngine != null) {
            ContentValues cv = new ContentValues();
            cv.putNull("frames_json");
            return sqliteEngine.update("parking_neighbours", cv, "session_id=? AND frames_json IS NOT NULL", new String[]{sessionId});
        }

        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE parking_neighbours SET frames_json=NULL WHERE session_id=? AND frames_json IS NOT NULL")) {
            ps.setString(1, sessionId);
            return ps.executeUpdate();
        } catch (Exception e) {
            logger.warn("clearNeighbourFrames failed: " + e.getMessage());
            return 0;
        }
    }

    public synchronized int countNeighbours(String sessionId, boolean confirmedOnly) {
        if (sqliteEngine != null) {
            String sql = "SELECT COUNT(*) FROM parking_neighbours WHERE session_id=? AND kind='neighbour'"
                    + (confirmedOnly ? " AND confirmed=1" : "");
            return (int) sqliteEngine.queryLong(sql, new String[]{sessionId}, 0L);
        }

        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT COUNT(*) FROM parking_neighbours WHERE session_id=? AND kind=?"
                        + (confirmedOnly ? " AND confirmed=TRUE" : ""))) {
            ps.setString(1, sessionId);
            ps.setString(2, ParkingNeighbour.KIND_NEIGHBOUR);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    private static ParkingNeighbour readNeighbourFromCursor(Cursor c) {
        ParkingNeighbour n = new ParkingNeighbour();
        n.id = c.getLong(c.getColumnIndexOrThrow("id"));
        n.sessionId = c.getString(c.getColumnIndexOrThrow("session_id"));
        int keyIdx = c.getColumnIndex("neighbour_key");
        if (keyIdx >= 0 && !c.isNull(keyIdx)) n.neighbourKey = c.getString(keyIdx);
        n.side = c.getInt(c.getColumnIndexOrThrow("side"));
        int kindIdx = c.getColumnIndex("kind");
        n.kind = (kindIdx >= 0 && !c.isNull(kindIdx)) ? c.getString(kindIdx) : ParkingNeighbour.KIND_NEIGHBOUR;
        int grpIdx = c.getColumnIndex("class_group");
        if (grpIdx >= 0 && !c.isNull(grpIdx)) n.classGroup = c.getString(grpIdx);
        int statIdx = c.getColumnIndex("status");
        if (statIdx >= 0 && !c.isNull(statIdx)) n.status = c.getString(statIdx);
        int confIdx = c.getColumnIndex("confirmed");
        if (confIdx >= 0 && !c.isNull(confIdx)) n.confirmed = c.getInt(confIdx) == 1;
        int fsIdx = c.getColumnIndex("first_seen_ms");
        if (fsIdx >= 0 && !c.isNull(fsIdx)) n.firstSeenMs = c.getLong(fsIdx);
        int arrIdx = c.getColumnIndex("arrived_ms");
        if (arrIdx >= 0 && !c.isNull(arrIdx)) n.arrivedMs = c.getLong(arrIdx);
        int depIdx = c.getColumnIndex("departed_ms");
        if (depIdx >= 0 && !c.isNull(depIdx)) n.departedMs = c.getLong(depIdx);
        int lsIdx = c.getColumnIndex("last_seen_ms");
        if (lsIdx >= 0 && !c.isNull(lsIdx)) n.lastSeenMs = c.getLong(lsIdx);
        int cxIdx = c.getColumnIndex("cx"); if (cxIdx >= 0 && !c.isNull(cxIdx)) n.cx = c.getFloat(cxIdx);
        int cyIdx = c.getColumnIndex("cy"); if (cyIdx >= 0 && !c.isNull(cyIdx)) n.cy = c.getFloat(cyIdx);
        int wIdx = c.getColumnIndex("w"); if (wIdx >= 0 && !c.isNull(wIdx)) n.w = c.getFloat(wIdx);
        int hIdx = c.getColumnIndex("h"); if (hIdx >= 0 && !c.isNull(hIdx)) n.h = c.getFloat(hIdx);
        int proxIdx = c.getColumnIndex("proximity");
        if (proxIdx >= 0 && !c.isNull(proxIdx)) n.proximity = c.getString(proxIdx);
        int aeIdx = c.getColumnIndex("arrival_event");
        if (aeIdx >= 0 && !c.isNull(aeIdx)) n.arrivalEvent = c.getString(aeIdx);
        int deIdx = c.getColumnIndex("departure_event");
        if (deIdx >= 0 && !c.isNull(deIdx)) n.departureEvent = c.getString(deIdx);
        int actIdx = c.getColumnIndex("actor_ids");
        if (actIdx >= 0 && !c.isNull(actIdx)) n.actorIds = c.getString(actIdx);
        int frIdx = c.getColumnIndex("frames_json");
        if (frIdx >= 0 && !c.isNull(frIdx)) n.framesJson = c.getString(frIdx);
        int upIdx = c.getColumnIndex("updated_ms");
        if (upIdx >= 0 && !c.isNull(upIdx)) n.updatedMs = c.getLong(upIdx);
        return n;
    }

    private static ParkingNeighbour readNeighbour(ResultSet rs) throws Exception {
        ParkingNeighbour n = new ParkingNeighbour();
        n.id = rs.getLong("id");
        n.sessionId = rs.getString("session_id");
        n.neighbourKey = rs.getString("neighbour_key");
        n.side = rs.getInt("side");
        n.kind = rs.getString("kind");
        n.classGroup = rs.getString("class_group");
        n.status = rs.getString("status");
        n.confirmed = rs.getBoolean("confirmed");
        n.firstSeenMs = rs.getLong("first_seen_ms");
        n.arrivedMs = rs.getLong("arrived_ms");
        n.departedMs = rs.getLong("departed_ms");
        n.lastSeenMs = rs.getLong("last_seen_ms");
        n.cx = rs.getFloat("cx"); n.cy = rs.getFloat("cy");
        n.w = rs.getFloat("w"); n.h = rs.getFloat("h");
        n.proximity = rs.getString("proximity");
        n.arrivalEvent = rs.getString("arrival_event");
        n.departureEvent = rs.getString("departure_event");
        n.actorIds = rs.getString("actor_ids");
        n.framesJson = rs.getString("frames_json");
        n.updatedMs = rs.getLong("updated_ms");
        return n;
    }

    // ==================== HELPERS ====================

    private static void setStr(PreparedStatement ps, int idx, String v) throws Exception {
        if (v == null) ps.setNull(idx, java.sql.Types.VARCHAR);
        else ps.setString(idx, v);
    }

    private static void setNullableDouble(PreparedStatement ps, int idx, double v) throws Exception {
        if (Double.isNaN(v)) ps.setNull(idx, java.sql.Types.DOUBLE);
        else ps.setDouble(idx, v);
    }

    private static String clamp(String v, int max) {
        if (v == null) return null;
        return v.length() <= max ? v : v.substring(0, max);
    }
}
