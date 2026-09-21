package space.ajcool.ardapaths.core.backup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the incremental marker scan cache.
 */
class ScanCacheTest {
    /**
     * Verifies persisted empty chunks can be skipped after a later scan loads the cache.
     *
     * @param temporaryDirectory test-owned directory
     * @throws IOException when the cache cannot be written
     */
    @Test
    void roundTripPreservesSkipRules(@TempDir Path temporaryDirectory) throws IOException {
        Path cacheFile = temporaryDirectory.resolve("scan-cache.bin");
        ScanCache cache = ScanCache.empty(100L);
        cache.record("minecraft:overworld", "r.0.0.mca", 1, 50, false);
        cache.record("minecraft:overworld", "r.0.0.mca", 2, 50, true);
        cache.write(cacheFile);

        ScanCache loaded = ScanCache.load(cacheFile, 200L);

        assertTrue(loaded.shouldSkip("minecraft:overworld", "r.0.0.mca", 1, 50));
        assertFalse(loaded.shouldSkip("minecraft:overworld", "r.0.0.mca", 1, 51));
        assertFalse(loaded.shouldSkip("minecraft:overworld", "r.0.0.mca", 2, 50));
        assertFalse(loaded.shouldSkip("minecraft:the_nether", "r.0.0.mca", 1, 50));
    }

    /**
     * Verifies same-second rewrites are not skipped.
     *
     * @param temporaryDirectory test-owned directory
     * @throws IOException when the cache cannot be written
     */
    @Test
    void sameSecondTimestampIsReadAgain(@TempDir Path temporaryDirectory) throws IOException {
        Path cacheFile = temporaryDirectory.resolve("scan-cache.bin");
        ScanCache cache = ScanCache.empty(100L);
        cache.record("minecraft:overworld", "r.0.0.mca", 1, 100, false);
        cache.write(cacheFile);

        ScanCache loaded = ScanCache.load(cacheFile, 200L);

        assertFalse(loaded.shouldSkip("minecraft:overworld", "r.0.0.mca", 1, 100));
    }

    /**
     * Verifies a corrupt cache behaves like an empty cache.
     *
     * @param temporaryDirectory test-owned directory
     * @throws IOException when the corrupt cache cannot be written
     */
    @Test
    void corruptCacheFallsBackToFullScan(@TempDir Path temporaryDirectory) throws IOException {
        Path cacheFile = temporaryDirectory.resolve("scan-cache.bin");
        Files.write(cacheFile, new byte[]{1, 2, 3});

        ScanCache loaded = ScanCache.load(cacheFile, 200L);

        assertFalse(loaded.shouldSkip("minecraft:overworld", "r.0.0.mca", 1, 50));
    }
}
