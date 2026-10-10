package com.overdrive.app.storage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class StorageHealthCheckTest {

    @Test
    public void canWriteToDirectoryDetectsWritableDir() throws Exception {
        Path tempDir = Files.createTempDirectory("writable-test");
        assertTrue(StorageHealthCheck.canWriteToDirectory(tempDir.toFile()));
    }

    @Test
    public void canWriteToDirectoryFailsForNullOrNonexistent() {
        assertFalse(StorageHealthCheck.canWriteToDirectory(null));
        assertFalse(StorageHealthCheck.canWriteToDirectory(new File("/nonexistent_drive_xyz/invalid/dir")));
    }

    @Test
    public void isVolumeAccessibleChecksExistenceAndReadability() throws Exception {
        Path tempDir = Files.createTempDirectory("access-test");
        assertTrue(StorageHealthCheck.isVolumeAccessible(tempDir.toString()));
        assertFalse(StorageHealthCheck.isVolumeAccessible(null));
        assertFalse(StorageHealthCheck.isVolumeAccessible("/nonexistent_drive_xyz/path"));
    }
}
