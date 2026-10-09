package com.overdrive.app.charging;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.overdrive.app.logging.DaemonLogger;
import com.overdrive.app.surveillance.SafeLocationManager;
import com.overdrive.app.util.DaemonStorage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * High-performance, zero-context SQLite reader for EV Charging Station databases.
 *
 * <p>Supports querying 20,000+ national EV stations (e.g. turkey_ev_stations.db)
 * with sub-5ms indexed spatial lookups, automatic asset extraction, and seamless
 * integration with {@link TariffManager} and {@link com.overdrive.app.geo.GeocodingResolver}.
 */
public final class EvStationDatabase {

    private static final String TAG = "EvStationDatabase";
    private static final DaemonLogger logger = DaemonLogger.getInstance(TAG);

    private static final String DB_FILENAME = "turkey_ev_stations.db";
    public static final String DEFAULT_DB_PATH = DaemonStorage.rebase("/data/local/tmp/" + DB_FILENAME);

    private static volatile EvStationDatabase instance;
    private static final Object INSTANCE_LOCK = new Object();

    private final String dbPath;
    private volatile SQLiteDatabase database;
    private volatile boolean isInitialized = false;

    public static class Station {
        public final String id;
        public final String operator;
        public final String name;
        public final String city;
        public final String district;
        public final String address;
        public final double latitude;
        public final double longitude;
        public final String chargingType;
        public final double maxPowerKw;
        public final int socketCount;
        public final double acPrice;
        public final double dcPrice;
        public final String logoUrl;
        public final String connectorsJson;
        public double distanceMeters;

        public Station(String id, String operator, String name, String city, String district,
                       String address, double latitude, double longitude, String chargingType,
                       double maxPowerKw, int socketCount, double acPrice, double dcPrice,
                       String logoUrl, String connectorsJson, double distanceMeters) {
            this.id = id != null ? id : "";
            this.operator = operator != null ? operator : "";
            this.name = name != null ? name : "";
            this.city = city != null ? city : "";
            this.district = district != null ? district : "";
            this.address = address != null ? address : "";
            this.latitude = latitude;
            this.longitude = longitude;
            this.chargingType = chargingType != null ? chargingType : "AC";
            this.maxPowerKw = maxPowerKw;
            this.socketCount = socketCount;
            this.acPrice = acPrice;
            this.dcPrice = dcPrice;
            this.logoUrl = logoUrl != null ? logoUrl : "";
            this.connectorsJson = connectorsJson != null ? connectorsJson : "[]";
            this.distanceMeters = distanceMeters;
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("id", id);
                json.put("operator", operator);
                json.put("name", name);
                json.put("city", city);
                json.put("district", district);
                json.put("address", address);
                json.put("latitude", latitude);
                json.put("longitude", longitude);
                json.put("chargingType", chargingType);
                json.put("maxPowerKw", maxPowerKw);
                json.put("socketCount", socketCount);
                json.put("acPrice", acPrice);
                json.put("dcPrice", dcPrice);
                json.put("logoUrl", logoUrl);
                json.put("distanceMeters", Math.round(distanceMeters));
                try {
                    json.put("connectors", new JSONArray(connectorsJson));
                } catch (Exception e) {
                    json.put("connectors", new JSONArray());
                }
            } catch (Exception ignored) {}
            return json;
        }
    }

    private EvStationDatabase(String dbPath) {
        this.dbPath = dbPath;
    }

    public static EvStationDatabase getInstance() {
        return getInstance(DEFAULT_DB_PATH);
    }

    public static EvStationDatabase getInstance(String dbPath) {
        EvStationDatabase inst = instance;
        if (inst == null) {
            synchronized (INSTANCE_LOCK) {
                inst = instance;
                if (inst == null) {
                    inst = new EvStationDatabase(dbPath);
                    instance = inst;
                }
            }
        }
        return inst;
    }

    /**
     * Copy database asset to target path if not present or corrupt.
     */
    public static void copyFromAssetsIfMissing(Context context) {
        if (context == null) return;
        try {
            File target = new File(DEFAULT_DB_PATH);
            if (target.exists() && target.length() > 1_000_000) {
                // Already extracted
                return;
            }
            File parent = target.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            String assetPath = "databases/" + DB_FILENAME;
            try (InputStream in = context.getAssets().open(assetPath);
                 OutputStream out = new FileOutputStream(target)) {
                byte[] buf = new byte[65536];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.flush();
                target.setReadable(true, false);
                logger.info("Extracted EV station database from assets to: " + DEFAULT_DB_PATH);
            }
        } catch (Exception e) {
            logger.debug("Notice: EV station asset extract skipped: " + e.getMessage());
        }
    }

    public synchronized boolean open() {
        if (isInitialized && database != null && database.isOpen()) {
            return true;
        }
        try {
            File file = new File(dbPath);
            if (!file.exists() || !file.canRead()) {
                // Secondary check for app databases path
                File appDb = new File("/data/data/com.overdrive.app/databases/" + DB_FILENAME);
                if (appDb.exists() && appDb.canRead()) {
                    file = appDb;
                } else {
                    return false;
                }
            }

            database = SQLiteDatabase.openDatabase(
                    file.getAbsolutePath(),
                    null,
                    SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS
            );
            isInitialized = true;
            logger.info("EV station database opened (Read-Only) at: " + file.getAbsolutePath());
            return true;
        } catch (Exception e) {
            logger.debug("Could not open EV station database: " + e.getMessage());
            isInitialized = false;
            return false;
        }
    }

    public boolean isAvailable() {
        return open();
    }

    public int getStationCount() {
        if (!open()) return 0;
        try (Cursor c = database.rawQuery("SELECT count(*) FROM stations;", null)) {
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
        } catch (Exception e) {
            logger.debug("Error getting station count: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Finds the nearest EV charging station within maxDistanceMeters using
     * indexed spatial bounding box + exact haversine calculation.
     */
    public Station findNearestStation(double lat, double lng, double maxDistanceMeters) {
        if (lat == 0 && lng == 0) return null;
        if (!open()) return null;

        // 1 deg lat is ~111,000 meters
        double deltaLat = maxDistanceMeters / 111000.0;
        double radLat = Math.toRadians(lat);
        double deltaLng = maxDistanceMeters / (111000.0 * Math.max(0.1, Math.cos(radLat)));

        double minLat = lat - deltaLat;
        double maxLat = lat + deltaLat;
        double minLng = lng - deltaLng;
        double maxLng = lng + deltaLng;

        String query = "SELECT id, operator, name, city, district, address, latitude, longitude, "
                + "charging_type, max_power_kw, socket_count, ac_price, dc_price, logo_url, connectors_json "
                + "FROM stations WHERE latitude BETWEEN ? AND ? AND longitude BETWEEN ? AND ?;";

        Station nearest = null;
        double minDistance = Double.MAX_VALUE;

        try (Cursor c = database.rawQuery(query, new String[] {
                String.valueOf(minLat), String.valueOf(maxLat),
                String.valueOf(minLng), String.valueOf(maxLng)
        })) {
            while (c.moveToNext()) {
                double sLat = c.getDouble(6);
                double sLng = c.getDouble(7);
                double dist = SafeLocationManager.haversine(lat, lng, sLat, sLng);
                if (dist <= maxDistanceMeters && dist < minDistance) {
                    minDistance = dist;
                    nearest = new Station(
                            c.getString(0), c.getString(1), c.getString(2), c.getString(3),
                            c.getString(4), c.getString(5), sLat, sLng, c.getString(8),
                            c.getDouble(9), c.getInt(10), c.getDouble(11), c.getDouble(12),
                            c.getString(13), c.getString(14), dist
                    );
                }
            }
        } catch (Exception e) {
            logger.debug("Error finding nearest station: " + e.getMessage());
        }

        return nearest;
    }

    /**
     * Finds nearby charging stations within a given radius, sorted by distance.
     */
    public List<Station> findNearbyStations(double lat, double lng, double radiusMeters, int limit) {
        if (lat == 0 && lng == 0) return Collections.emptyList();
        if (!open()) return Collections.emptyList();

        double deltaLat = radiusMeters / 111000.0;
        double radLat = Math.toRadians(lat);
        double deltaLng = radiusMeters / (111000.0 * Math.max(0.1, Math.cos(radLat)));

        double minLat = lat - deltaLat;
        double maxLat = lat + deltaLat;
        double minLng = lng - deltaLng;
        double maxLng = lng + deltaLng;

        String query = "SELECT id, operator, name, city, district, address, latitude, longitude, "
                + "charging_type, max_power_kw, socket_count, ac_price, dc_price, logo_url, connectors_json "
                + "FROM stations WHERE latitude BETWEEN ? AND ? AND longitude BETWEEN ? AND ?;";

        List<Station> results = new ArrayList<>();
        try (Cursor c = database.rawQuery(query, new String[] {
                String.valueOf(minLat), String.valueOf(maxLat),
                String.valueOf(minLng), String.valueOf(maxLng)
        })) {
            while (c.moveToNext()) {
                double sLat = c.getDouble(6);
                double sLng = c.getDouble(7);
                double dist = SafeLocationManager.haversine(lat, lng, sLat, sLng);
                if (dist <= radiusMeters) {
                    results.add(new Station(
                            c.getString(0), c.getString(1), c.getString(2), c.getString(3),
                            c.getString(4), c.getString(5), sLat, sLng, c.getString(8),
                            c.getDouble(9), c.getInt(10), c.getDouble(11), c.getDouble(12),
                            c.getString(13), c.getString(14), dist
                    ));
                }
            }
        } catch (Exception e) {
            logger.debug("Error finding nearby stations: " + e.getMessage());
        }

        Collections.sort(results, new Comparator<Station>() {
            @Override
            public int compare(Station o1, Station o2) {
                return Double.compare(o1.distanceMeters, o2.distanceMeters);
            }
        });

        if (limit > 0 && results.size() > limit) {
            return results.subList(0, limit);
        }
        return results;
    }
}
