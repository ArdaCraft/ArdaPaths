package space.ajcool.ardapaths.core.backup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for direct anvil region reads used by marker scans.
 */
class RegionFileReaderTest {
    /**
     * Marker id bytes searched by the region prefilter.
     */
    private static final byte[] MARKER_ID = MarkerScanner.PATH_MARKER_BLOCK_ENTITY_ID.getBytes(java.nio.charset.StandardCharsets.UTF_8);

    /**
     * Verifies compression handling, byte prefilter hits, external payloads, and fallback entries.
     *
     * @param temporaryDirectory test-owned directory
     * @throws IOException when synthetic region files cannot be written
     */
    @Test
    void readReportsPrefilterHitsAndFallbacks(@TempDir Path temporaryDirectory) throws IOException {
        Path regionFile = temporaryDirectory.resolve("r.0.0.mca");
        RegionBuilder builder = new RegionBuilder(regionFile);
        builder.chunk(0, 0, 10, 2, zlib(bytes("ordinary chunk")), false);
        builder.chunk(1, 0, 11, 1, gzip(bytes("contains " + MarkerScanner.PATH_MARKER_BLOCK_ENTITY_ID)), false);
        builder.chunk(2, 0, 12, 3, bytes("plain " + MarkerScanner.PATH_MARKER_BLOCK_ENTITY_ID), false);
        builder.chunk(3, 0, 13, 2, zlib(bytes("external " + MarkerScanner.PATH_MARKER_BLOCK_ENTITY_ID)), true);
        builder.chunk(4, 0, 14, 99, bytes("unknown"), false);
        builder.write();

        RegionFileReader reader = new RegionFileReader(MARKER_ID);
        RegionFileReader.ReadResult result = reader.read(
                regionFile,
                0,
                0,
                "minecraft:overworld",
                ScanCache.emptyNow(),
                false,
                new Minecraft261ChunkStorageAccess()::decompress
        );

        List<RegionFileReader.ChunkEntry> entries = result.entries();
        assertEquals(5, entries.size());
        assertFalse(entries.get(0).hasMarkerBytes());
        assertNull(entries.get(0).payload());
        assertTrue(entries.get(1).hasMarkerBytes());
        assertTrue(entries.get(2).hasMarkerBytes());
        assertTrue(entries.get(3).hasMarkerBytes());
        assertTrue(entries.get(4).fallback());
        assertEquals(5, result.stats().populatedChunks());
        assertEquals(1, result.stats().prefilterSkipped());
        assertEquals(3, result.stats().markerByteHits());
        assertEquals(1, result.stats().fallbacks());
    }

    /**
     * Verifies that empty region files, which vanilla leaves behind routinely, read as regions with no chunks.
     *
     * @param temporaryDirectory test-owned directory
     * @throws IOException when the synthetic region file cannot be written
     */
    @Test
    void readTreatsEmptyRegionFileAsEmpty(@TempDir Path temporaryDirectory) throws IOException {
        Path regionFile = temporaryDirectory.resolve("r.0.0.mca");
        Files.write(regionFile, new byte[0]);

        RegionFileReader.ReadResult result = read(regionFile);

        assertTrue(result.entries().isEmpty());
        assertEquals(RegionFileReader.Stats.empty(), result.stats());
    }

    /**
     * Verifies that a truncated header is zero-filled like vanilla, keeping the location entries that were present.
     *
     * @param temporaryDirectory test-owned directory
     * @throws IOException when the synthetic region file cannot be written
     */
    @Test
    void readZeroFillsTruncatedHeader(@TempDir Path temporaryDirectory) throws IOException {
        Path fullRegion = temporaryDirectory.resolve("full.mca");
        RegionBuilder builder = new RegionBuilder(fullRegion);
        builder.chunk(0, 0, 10, 2, zlib(bytes("ordinary chunk")), false);
        builder.write();

        Path regionFile = temporaryDirectory.resolve("r.0.0.mca");
        Files.write(regionFile, java.util.Arrays.copyOf(Files.readAllBytes(fullRegion), 100));

        RegionFileReader.ReadResult result = read(regionFile);

        assertEquals(1, result.entries().size());
        assertEquals(1, result.stats().populatedChunks());
        assertEquals(1, result.stats().fallbacks());
    }

    /**
     * Reads a synthetic region at region coordinates 0,0 with an empty cache.
     *
     * @param regionFile region file to read
     * @return read result
     * @throws IOException when the region cannot be read
     */
    private static RegionFileReader.ReadResult read(Path regionFile) throws IOException {
        return new RegionFileReader(MARKER_ID).read(
                regionFile,
                0,
                0,
                "minecraft:overworld",
                ScanCache.emptyNow(),
                false,
                new Minecraft261ChunkStorageAccess()::decompress
        );
    }

    /**
     * Converts text to bytes.
     *
     * @param text text to encode
     * @return UTF-8 bytes
     */
    private static byte[] bytes(String text) {
        return text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Compresses bytes with zlib.
     *
     * @param input raw payload
     * @return compressed payload
     * @throws IOException when compression fails
     */
    private static byte[] zlib(byte[] input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflater = new DeflaterOutputStream(output)) {
            deflater.write(input);
        }
        return output.toByteArray();
    }

    /**
     * Compresses bytes with gzip.
     *
     * @param input raw payload
     * @return compressed payload
     * @throws IOException when compression fails
     */
    private static byte[] gzip(byte[] input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(input);
        }
        return output.toByteArray();
    }

    /**
     * Small writer for synthetic region files.
     *
     */
    private static class RegionBuilder {
        /**
         * Mutable region bytes, including the header.
         */
        private static final int REGION_SIZE = 4096 * 16;

        /**
         * Target region file.
         */
        private final Path regionFile;

        /**
         * Region file bytes.
         */
        private final ByteBuffer buffer = ByteBuffer.allocate(REGION_SIZE).order(ByteOrder.BIG_ENDIAN);

        /**
         * Creates a synthetic region writer.
         *
         * @param regionFile target region file
         */
        RegionBuilder(Path regionFile) {
            this.regionFile = regionFile;
        }

        /**
         * Adds a chunk to the synthetic region file.
         *
         * @param localX          local chunk X
         * @param localZ          local chunk Z
         * @param timestamp       anvil timestamp
         * @param compressionType compression type
         * @param payload         compressed payload
         * @param external        whether payload should be written to an external file
         * @throws IOException when an external payload cannot be written
         */
        @SuppressWarnings("SameParameterValue")
        void chunk(int localX, int localZ, int timestamp, int compressionType, byte[] payload, boolean external) throws IOException {
            int slot = RegionFileReader.slot(localX, localZ);
            int sector = 2 + slot;
            buffer.putInt(slot * Integer.BYTES, (sector << 8) | 1);
            buffer.putInt(4096 + slot * Integer.BYTES, timestamp);
            buffer.putInt(sector * 4096, external ? 2 : payload.length + 1);
            buffer.put(sector * 4096 + Integer.BYTES, (byte) (external ? compressionType | 0x80 : compressionType));
            if (external) {
                Files.write(regionFile.resolveSibling("c." + localX + "." + localZ + ".mcc"), payload);
            } else {
                buffer.position(sector * 4096 + Integer.BYTES + 1);
                buffer.put(payload);
            }
        }

        /**
         * Writes region bytes to disk.
         *
         * @throws IOException when the region cannot be written
         */
        void write() throws IOException {
            Files.write(regionFile, buffer.array());
        }
    }
}
