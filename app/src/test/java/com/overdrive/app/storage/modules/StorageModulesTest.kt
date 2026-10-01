package com.overdrive.app.storage.modules

import com.overdrive.app.storage.StorageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Unit test suite verifying Phase 4 Storage Modules de-composition and StorageManager Facade.
 */
class StorageModulesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testVolumeMountDetectorMountClassification() {
        val detector = VolumeMountDetector()

        // External mount points
        assertTrue(detector.isExternalStorageMount("/storage/1234-ABCD"))
        assertTrue(detector.isExternalStorageMount("/storage/E4A1-19F2"))
        assertTrue(detector.isExternalStorageMount("/mnt/media_rw/usb_disk"))

        // Internal / system mount points
        assertFalse(detector.isExternalStorageMount("/storage/emulated/0"))
        assertFalse(detector.isExternalStorageMount("/storage/self/primary"))
        assertFalse(detector.isExternalStorageMount("/system"))
        assertFalse(detector.isExternalStorageMount(null))
    }

    @Test
    fun testVolumeMountDetectorNullSafety() {
        val detector = VolumeMountDetector()
        assertEquals(0L, detector.getAvailableBytes(null))
        assertEquals(0L, detector.getTotalBytes(null))
        assertFalse(detector.isWritable(null))

        val nonExistent = File("/path/to/definitely/nonexistent/storage/disk")
        assertEquals(0L, detector.getAvailableBytes(nonExistent))
        assertEquals(0L, detector.getTotalBytes(nonExistent))
    }

    @Test
    fun testCircularStorageCleanerLockedFileIdentification() {
        val cleaner = CircularStorageCleaner()

        assertTrue(cleaner.isLockedFile(File("/recordings/trip_event_emergency.mp4")))
        assertTrue(cleaner.isLockedFile(File("/recordings/front_locked_2026.mp4")))
        assertTrue(cleaner.isLockedFile(File("/recordings/sentry_event_collision.ts")))
        assertTrue(cleaner.isLockedFile(File("/recordings/recording_active.tmp")))

        // Standard circular files are not locked
        assertFalse(cleaner.isLockedFile(File("/recordings/trip_normal_001.mp4")))
        assertFalse(cleaner.isLockedFile(File("/recordings/dashcam_20260901_120000.mp4")))
        assertFalse(cleaner.isLockedFile(null))
    }

    @Test
    fun testCircularStorageCleanerPrunesOldestUnlockedFiles() {
        val cleaner = CircularStorageCleaner()
        val dir = tempFolder.newFolder("recordings")

        // Create 3 files:
        // f1: oldest unlocked (1000 bytes)
        val f1 = File(dir, "video_oldest.mp4")
        f1.writeBytes(ByteArray(1000))
        f1.setLastModified(1000000L)

        // f2: locked file (500 bytes)
        val f2 = File(dir, "video_locked_incident.mp4")
        f2.writeBytes(ByteArray(500))
        f2.setLastModified(2000000L)

        // f3: newest unlocked (1000 bytes)
        val f3 = File(dir, "video_newest.mp4")
        f3.writeBytes(ByteArray(1000))
        f3.setLastModified(3000000L)

        assertEquals(2500L, cleaner.calculateDirectoryBytes(dir))

        // Target quota: 1600 bytes. It should delete f1 (1000 bytes), keep f2 (locked) and f3
        val result = cleaner.cleanToQuota(dir, 1600L)
        assertEquals(1, result.deletedCount)
        assertEquals(1000L, result.freedBytes)
        assertTrue(result.targetReached)

        assertFalse(f1.exists())
        assertTrue(f2.exists())
        assertTrue(f3.exists())
        assertEquals(1500L, cleaner.calculateDirectoryBytes(dir))
    }

    @Test
    fun testStorageTelemetryReconcilerIdentifiesMissingFiles() {
        val reconciler = StorageTelemetryReconciler()
        val dir = tempFolder.newFolder("storage_check")

        val existingFile = File(dir, "saved_trip.mp4")
        existingFile.writeBytes(ByteArray(100))

        val registeredEntries = setOf("saved_trip.mp4", "deleted_ghost_trip.mp4", "old_recording.mp4")
        val orphaned = reconciler.findOrphanedRecords(dir, registeredEntries)

        assertEquals(2, orphaned.size)
        assertTrue(orphaned.contains("deleted_ghost_trip.mp4"))
        assertTrue(orphaned.contains("old_recording.mp4"))
        assertFalse(orphaned.contains("saved_trip.mp4"))
    }

    @Test
    fun testStorageManagerFacadeExposesModules() {
        assertNotNull(StorageManager.getVolumeMountDetector())
        assertNotNull(StorageManager.getCircularStorageCleaner())
        assertNotNull(StorageManager.getStorageTelemetryReconciler())
    }
}
