package space.ajcool.ardapaths.core.backup;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Version-independent cache for chunks that previously had no path markers.
 */
public class ScanCache {
    /**
     * Current binary cache format version.
     */
    private static final int VERSION = 1;

    /**
     * Cache file path used by backup scans.
     */
    public static final Path DEFAULT_PATH = Path.of("./config/arda-paths/scan-cache.bin");

    /**
     * Last successful scan start time recorded in the loaded cache.
     */
    private final long loadedLastScanStartEpochSeconds;

    /**
     * Scan start time that will be written if this cache is saved.
     */
    private final long nextLastScanStartEpochSeconds;

    /**
     * Cached chunk metadata by dimension, region file, and slot.
     */
    private final Map<String, Map<String, Map<Integer, Entry>>> dimensions;

    /**
     * Creates a scan cache instance.
     *
     * @param loadedLastScanStartEpochSeconds last scan start from disk
     * @param nextLastScanStartEpochSeconds   scan start to persist on save
     * @param dimensions                      cached entries
     */
    private ScanCache(long loadedLastScanStartEpochSeconds, long nextLastScanStartEpochSeconds, Map<String, Map<String, Map<Integer, Entry>>> dimensions) {
        this.loadedLastScanStartEpochSeconds = loadedLastScanStartEpochSeconds;
        this.nextLastScanStartEpochSeconds = nextLastScanStartEpochSeconds;
        this.dimensions = dimensions;
    }

    /**
     * Loads a cache file, returning an empty cache when the file is absent or unusable.
     *
     * @param path cache file path
     * @param scanStartEpochSeconds current scan start time
     * @return loaded or empty cache
     */
    public static ScanCache load(Path path, long scanStartEpochSeconds) {
        if (!Files.isRegularFile(path)) {
            return empty(scanStartEpochSeconds);
        }

        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            int version = input.readInt();
            if (version != VERSION) {
                return empty(scanStartEpochSeconds);
            }

            long lastScanStartEpochSeconds = input.readLong();
            Map<String, Map<String, Map<Integer, Entry>>> dimensions = new HashMap<>();
            int dimensionCount = input.readInt();

            for (int dimensionIndex = 0; dimensionIndex < dimensionCount; dimensionIndex++) {
                String dimensionId = input.readUTF();
                int regionCount = input.readInt();
                Map<String, Map<Integer, Entry>> regions = new HashMap<>();

                for (int regionIndex = 0; regionIndex < regionCount; regionIndex++) {
                    String regionFile = input.readUTF();
                    int entryCount = input.readInt();
                    Map<Integer, Entry> entries = new HashMap<>();

                    for (int entryIndex = 0; entryIndex < entryCount; entryIndex++) {
                        int slot = input.readInt();
                        int timestamp = input.readInt();
                        boolean hadMarker = input.readBoolean();
                        entries.put(slot, new Entry(timestamp, hadMarker));
                    }

                    regions.put(regionFile, entries);
                }

                dimensions.put(dimensionId, regions);
            }

            return new ScanCache(lastScanStartEpochSeconds, scanStartEpochSeconds, dimensions);
        } catch (IOException | RuntimeException exception) {
            return empty(scanStartEpochSeconds);
        }
    }

    /**
     * Creates an empty cache for a scan.
     *
     * @param scanStartEpochSeconds current scan start time
     * @return empty cache
     */
    public static ScanCache empty(long scanStartEpochSeconds) {
        return new ScanCache(0L, scanStartEpochSeconds, new HashMap<>());
    }

    /**
     * Creates an empty cache using the current epoch second.
     *
     * @return empty cache
     */
    public static ScanCache emptyNow() {
        return empty(Instant.now().getEpochSecond());
    }

    /**
     * Determines whether a chunk can be skipped safely.
     *
     * @param dimensionId dimension id
     * @param regionFile  region file name
     * @param slot        local region slot
     * @param timestamp   current anvil timestamp
     * @return true when a previously empty unchanged chunk is older than the loaded scan start
     */
    public synchronized boolean shouldSkip(String dimensionId, String regionFile, int slot, int timestamp) {
        Entry entry = entry(dimensionId, regionFile, slot);
        return entry != null
                && entry.timestamp() == timestamp
                && !entry.hadMarker()
                && Integer.toUnsignedLong(timestamp) < loadedLastScanStartEpochSeconds;
    }

    /**
     * Records this scan's observed marker status for a chunk.
     *
     * @param dimensionId dimension id
     * @param regionFile  region file name
     * @param slot        local region slot
     * @param timestamp   current anvil timestamp
     * @param hadMarker   whether the chunk contained a path marker
     */
    public synchronized void record(String dimensionId, String regionFile, int slot, int timestamp, boolean hadMarker) {
        dimensions
                .computeIfAbsent(dimensionId, ignored -> new HashMap<>())
                .computeIfAbsent(regionFile, ignored -> new HashMap<>())
                .put(slot, new Entry(timestamp, hadMarker));
    }

    /**
     * Writes the cache atomically.
     *
     * @param path cache file path
     * @throws IOException when the cache cannot be written
     */
    public synchronized void write(Path path) throws IOException {
        Files.createDirectories(path.toAbsolutePath().normalize().getParent());
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");

        try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temporary)))) {
            output.writeInt(VERSION);
            output.writeLong(nextLastScanStartEpochSeconds);
            output.writeInt(dimensions.size());

            for (Map.Entry<String, Map<String, Map<Integer, Entry>>> dimension : dimensions.entrySet()) {
                output.writeUTF(dimension.getKey());
                output.writeInt(dimension.getValue().size());

                for (Map.Entry<String, Map<Integer, Entry>> region : dimension.getValue().entrySet()) {
                    output.writeUTF(region.getKey());
                    output.writeInt(region.getValue().size());

                    for (Map.Entry<Integer, Entry> chunk : region.getValue().entrySet()) {
                        output.writeInt(chunk.getKey());
                        output.writeInt(chunk.getValue().timestamp());
                        output.writeBoolean(chunk.getValue().hadMarker());
                    }
                }
            }
        }

        try {
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Finds a cached chunk entry.
     *
     * @param dimensionId dimension id
     * @param regionFile  region file name
     * @param slot        local region slot
     * @return cached entry, or null
     */
    private Entry entry(String dimensionId, String regionFile, int slot) {
        Map<String, Map<Integer, Entry>> regions = dimensions.get(dimensionId);
        if (regions == null) {
            return null;
        }

        Map<Integer, Entry> entries = regions.get(regionFile);
        return entries == null ? null : entries.get(slot);
    }

    /**
     * Cached state for one chunk slot.
     *
     * @param timestamp anvil timestamp table value
     * @param hadMarker whether the chunk contained a path marker
     */
    public record Entry(int timestamp, boolean hadMarker) {
    }
}
