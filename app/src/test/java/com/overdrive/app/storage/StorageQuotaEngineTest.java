package com.overdrive.app.storage;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StorageQuotaEngineTest {

    @Test
    public void formatSizeBytesFormatsCorrectUnits() {
        assertEquals("500 B", StorageQuotaEngine.formatSize(500));
        assertEquals("1.5 KB", StorageQuotaEngine.formatSize(1500));
        assertEquals("10.0 MB", StorageQuotaEngine.formatSize(10_000_000));
        assertEquals("2.5 GB", StorageQuotaEngine.formatSize(2_500_000_000L));
    }

    @Test
    public void computeVolumeCeilingHonorsHeadroomAndMinimum() {
        long totalBytes = 1000L * 1024 * 1024; // 1000 MB
        long ceiling = StorageQuotaEngine.computeVolumeCeilingMb(totalBytes, 256, 100);
        assertEquals(744, ceiling); // 1000 - 256 = 744

        long smallBytes = 200L * 1024 * 1024; // 200 MB
        long smallCeiling = StorageQuotaEngine.computeVolumeCeilingMb(smallBytes, 256, 100);
        assertEquals(100, smallCeiling); // Floored at min 100MB
    }

    @Test
    public void reclampTargetShrinksDownOrHolds() {
        assertEquals(500, StorageQuotaEngine.reclampTargetMb(500, 1000, 100)); // shrinks down to ceiling
        assertEquals(300, StorageQuotaEngine.reclampTargetMb(500, 300, 100));  // holds lower limit
        assertEquals(100, StorageQuotaEngine.reclampTargetMb(50, 200, 100));   // clamped to min
    }
}
