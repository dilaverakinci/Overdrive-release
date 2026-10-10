package com.overdrive.app.monitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.overdrive.app.database.OverdriveSqliteMaster;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DataUsageMonitorTest {

    private OverdriveSqliteMaster master;
    private DataUsageMonitor monitor;

    @Before
    public void setUp() {
        master = OverdriveSqliteMaster.useInMemoryForTesting();
        monitor = new DataUsageMonitor(master);
    }

    @After
    public void tearDown() {
        monitor.shutdown();
        master.close();
    }

    @Test
    public void testSamplingAndUpsertDeltas() throws Exception {
        long t0 = System.currentTimeMillis(); // baseline timestamp

        // Tick 1: Initial baseline seed (e.g. WiFi=1000, Mobile=2000, Other=0, App=500, System=2500)
        monitor.recordSample(new long[]{1000L, 2000L, 0L, 500L, 2500L}, t0);

        // Verify initial state is recorded and day usage is 0 (since it was a seed)
        JSONObject usage = monitor.getUsage(30);
        System.out.println("DEBUG usage: " + usage);
        assertNotNull(usage);
        JSONArray days = usage.optJSONArray("days");
        assertNotNull("days array should not be null, usage was: " + usage, days);
        assertEquals(0, days.length());

        // Tick 2: 2 minutes later (+500 WiFi, +1000 Mobile, +300 App, +1200 System)
        long t1 = t0 + 120_000L;
        monitor.recordSample(new long[]{1500L, 3000L, 0L, 800L, 3700L}, t1);

        usage = monitor.getUsage(30);
        days = usage.getJSONArray("days");
        assertEquals(1, days.length());

        JSONObject today = days.getJSONObject(0);
        assertEquals(500L, today.getLong("wifi"));
        assertEquals(1000L, today.getLong("mobile"));
        assertEquals(300L, today.getLong("app"));
        assertEquals(1200L, today.getLong("system"));
        assertEquals(1500L, today.getLong("total"));

        assertEquals(500L, usage.getLong("totalWifi"));
        assertEquals(1000L, usage.getLong("totalMobile"));
        assertEquals(1500L, usage.getLong("total"));

        // Tick 3: Another tick accumulating into the SAME day
        long t2 = t1 + 120_000L;
        monitor.recordSample(new long[]{1700L, 3200L, 0L, 900L, 4000L}, t2);

        usage = monitor.getUsage(30);
        days = usage.getJSONArray("days");
        assertEquals(1, days.length());

        today = days.getJSONObject(0);
        // 500 + 200 = 700 WiFi, 1000 + 200 = 1200 Mobile
        assertEquals(700L, today.getLong("wifi"));
        assertEquals(1200L, today.getLong("mobile"));
        assertEquals(1900L, today.getLong("total"));

        // Test history reset
        assertTrue(monitor.resetHistory());
        usage = monitor.getUsage(30);
        assertEquals(0, usage.getJSONArray("days").length());
        assertEquals(0L, usage.getLong("total"));
    }
}
