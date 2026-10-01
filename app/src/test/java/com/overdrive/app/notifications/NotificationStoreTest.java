package com.overdrive.app.notifications;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.junit.Test;

/**
 * Unit tests verifying lifecycle, invariants, and data contracts for NotificationStore.
 */
public class NotificationStoreTest {

    @Test
    public void getInstance_returnsNonNullSingleton() {
        NotificationStore store = NotificationStore.getInstance();
        assertNotNull(store);
        NotificationStore store2 = NotificationStore.getInstance();
        assertEquals(store, store2);
    }

    @Test
    public void pageModel_constructsCorrectly() {
        JSONArray items = new JSONArray();
        NotificationStore.Page page = new NotificationStore.Page(items, 42L);
        assertNotNull(page.items);
        assertEquals(42L, page.total);
    }

    @Test
    public void uninitializedStore_failsGracefullyWithoutExceptions() {
        NotificationStore store = NotificationStore.getInstance();
        // Safe to call query on uninitialized/stopped store without throwing
        NotificationStore.Page page = store.listWithCount(0, System.currentTimeMillis(), null, null, 10, 0);
        assertNotNull(page);
        assertNotNull(page.items);
        assertEquals(0, page.total);

        // Safe to call delete
        assertFalse(store.deleteById(999L));
        assertEquals(0, store.deleteBulk(new long[]{1L, 2L}));
        assertEquals(0, store.clear(0, 100));
    }
}
