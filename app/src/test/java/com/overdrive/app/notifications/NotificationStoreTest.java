package com.overdrive.app.notifications;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.overdrive.app.database.OverdriveSqliteMaster;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NotificationStoreTest {

    private OverdriveSqliteMaster master;
    private NotificationStore store;

    @Before
    public void setUp() {
        master = OverdriveSqliteMaster.useInMemoryForTesting();
        store = new NotificationStore(master);
        store.init();
    }

    @After
    public void tearDown() {
        store.stop();
        master.close();
    }

    @Test
    public void testInsertAndListWithCount() throws Exception {
        NotificationEvent ev1 = new NotificationEvent(
                "sentry",
                NotificationEvent.Severity.WARN,
                "Motion Detected",
                "Front camera detected pedestrian",
                "sentry_front",
                "http://localhost:8080/events/1",
                new JSONObject().put("camera", "front")
        );
        store.insert(ev1);

        Thread.sleep(10); // ensure distinct timestamp

        NotificationEvent ev2 = new NotificationEvent(
                "charging",
                NotificationEvent.Severity.INFO,
                "Charging Started",
                "Plugged in at 11 kW",
                "charging_start",
                null,
                null
        );
        store.insert(ev2, "http://localhost:8080/charging");

        long now = System.currentTimeMillis();
        NotificationStore.Page page = store.listWithCount(0L, now + 10000L, null, null, 10, 0);
        assertEquals(2, page.total);
        assertEquals(2, page.items.length());

        // Newest first -> ev2 at index 0
        JSONObject item0 = page.items.getJSONObject(0);
        assertEquals("charging", item0.getString("category"));
        assertEquals("http://localhost:8080/charging", item0.getString("url"));

        JSONObject item1 = page.items.getJSONObject(1);
        assertEquals("sentry", item1.getString("category"));
        assertEquals("warn", item1.getString("severity"));
        assertNotNull(item1.getJSONObject("data"));
        assertEquals("front", item1.getJSONObject("data").getString("camera"));
    }

    @Test
    public void testFilteringByCategoryAndSeverity() throws Exception {
        store.insert(new NotificationEvent("sentry.motion", NotificationEvent.Severity.WARN, "T1", "B1", null, null, null));
        Thread.sleep(5);
        store.insert(new NotificationEvent("sentry.impact", NotificationEvent.Severity.CRITICAL, "T2", "B2", null, null, null));
        Thread.sleep(5);
        store.insert(new NotificationEvent("system.update", NotificationEvent.Severity.INFO, "T3", "B3", null, null, null));

        long now = System.currentTimeMillis();

        // Filter by category prefix "sentry"
        NotificationStore.Page sentryPage = store.listWithCount(0L, now + 10000L, "sentry", null, 10, 0);
        assertEquals(2, sentryPage.total);

        // Filter by severity "critical"
        NotificationStore.Page critPage = store.listWithCount(0L, now + 10000L, null, "critical", 10, 0);
        assertEquals(1, critPage.total);
        assertEquals("T2", critPage.items.getJSONObject(0).getString("title"));
    }

    @Test
    public void testDeleteAndClear() throws Exception {
        store.insert(new NotificationEvent("test", NotificationEvent.Severity.INFO, "A", "Body", null, null, null));
        store.insert(new NotificationEvent("test", NotificationEvent.Severity.INFO, "B", "Body", null, null, null));

        long now = System.currentTimeMillis();
        NotificationStore.Page page = store.listWithCount(0L, now + 10000L, null, null, 10, 0);
        assertEquals(2, page.total);
        long idToDelete = page.items.getJSONObject(0).getLong("id");

        assertTrue(store.deleteById(idToDelete));
        assertEquals(1, store.listWithCount(0L, now + 10000L, null, null, 10, 0).total);

        int cleared = store.clear(0L, now + 10000L);
        assertEquals(1, cleared);
        assertEquals(0, store.listWithCount(0L, now + 10000L, null, null, 10, 0).total);
    }
}
