package space.ajcool.ardapaths.core.backup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only scanner for Minecraft anvil region files.
 */
public class RegionFileReader {
    /**
     * Width and height of a region in chunks.
     */
    public static final int REGION_CHUNK_WIDTH = 32;

    /**
     * Size of one anvil sector in bytes.
     */
    private static final int SECTOR_BYTES = 4096;

    /**
     * Number of bytes in both region header tables.
     */
    private static final int HEADER_BYTES = SECTOR_BYTES * 2;

    /**
     * Bit marking a chunk whose payload lives in an external {@code .mcc} file.
     */
    private static final int EXTERNAL_STREAM_FLAG = 0x80;

    /**
     * Maximum payload length accepted from a single chunk slot.
     */
    private static final int MAX_CHUNK_PAYLOAD_BYTES = 1024 * 1024 * 16;

    /**
     * UTF-8 marker block entity identifier used for the byte prefilter.
     */
    private final byte[] markerNeedle;

    /**
     * Creates a region reader for a specific marker id.
     *
     * @param markerNeedle UTF-8 bytes to search for in decompressed chunk payloads
     */
    public RegionFileReader(byte[] markerNeedle) {
        this.markerNeedle = markerNeedle.clone();
    }

    /**
     * Reads populated chunk entries from one region file.
     *
     * @param regionFile      region file path
     * @param regionX         region X coordinate
     * @param regionZ         region Z coordinate
     * @param dimensionId     dimension id used for cache lookups
     * @param cache           scan cache consulted before reading chunks
     * @param forceFull       whether cache skipping should be disabled
     * @param decompressor    compression adapter for this Minecraft version
     * @return direct-read entries and scan counters
     * @throws IOException when the region file cannot be opened or its header cannot be read
     */
    @SuppressWarnings("StatementWithEmptyBody")
    public ReadResult read(Path regionFile, int regionX, int regionZ, String dimensionId, ScanCache cache, boolean forceFull, Decompressor decompressor) throws IOException {
        ByteBuffer header = ByteBuffer.allocate(HEADER_BYTES).order(ByteOrder.BIG_ENDIAN);

        try (FileChannel channel = FileChannel.open(regionFile, StandardOpenOption.READ)) {
            while (header.hasRemaining() && channel.read(header) != -1) {
                // Continue until both fixed-size header tables are filled or the file ends.
            }

            // Vanilla leaves empty region files behind and treats a short header as zero-filled, so do the same:
            // missing location entries read as 0 and their slots count as unpopulated.
            if (header.position() == 0) {
                return new ReadResult(List.of(), Stats.empty());
            }

            List<ChunkEntry> entries = new ArrayList<>();
            Stats stats = Stats.empty();

            for (int localX = 0; localX < REGION_CHUNK_WIDTH; localX++) {
                for (int localZ = 0; localZ < REGION_CHUNK_WIDTH; localZ++) {
                    int slot = slot(localX, localZ);
                    int location = header.getInt(slot * Integer.BYTES);
                    if (location == 0) continue;

                    int timestamp = header.getInt(SECTOR_BYTES + slot * Integer.BYTES);
                    int chunkX = regionX * REGION_CHUNK_WIDTH + localX;
                    int chunkZ = regionZ * REGION_CHUNK_WIDTH + localZ;
                    stats = stats.withPopulated();

                    if (!forceFull && cache.shouldSkip(dimensionId, regionFile.getFileName().toString(), slot, timestamp)) {
                        entries.add(ChunkEntry.skipped(chunkX, chunkZ, slot, timestamp));
                        stats = stats.withCacheSkip();
                        continue;
                    }

                    try {
                        ChunkEntry entry = readChunk(channel, regionFile, location, chunkX, chunkZ, slot, timestamp, decompressor);
                        entries.add(entry);
                        stats = entry.fallback() ? stats.withFallback() : entry.hasMarkerBytes() ? stats.withMarkerBytes() : stats.withPrefilterSkip();
                    } catch (IOException | RuntimeException exception) {
                        entries.add(ChunkEntry.fallback(chunkX, chunkZ, slot, timestamp));
                        stats = stats.withFallback();
                    }
                }
            }

            return new ReadResult(entries, stats);
        }
    }

    /**
     * Reads and prefilters one populated chunk slot.
     *
     * @param channel      open region file channel
     * @param regionFile   region file path
     * @param location     packed anvil location table entry
     * @param chunkX       absolute chunk X coordinate
     * @param chunkZ       absolute chunk Z coordinate
     * @param slot         local region slot index
     * @param timestamp    anvil timestamp table value
     * @param decompressor compression adapter
     * @return direct chunk entry or fallback request
     * @throws IOException when the chunk payload cannot be read
     */
    @SuppressWarnings("StatementWithEmptyBody")
    private ChunkEntry readChunk(FileChannel channel, Path regionFile, int location, int chunkX, int chunkZ, int slot, int timestamp, Decompressor decompressor) throws IOException {
        int sectorOffset = location >>> 8;
        int sectorCount = location & 0xFF;
        if (sectorOffset < 2 || sectorCount <= 0) {
            return ChunkEntry.fallback(chunkX, chunkZ, slot, timestamp);
        }

        ByteBuffer chunkHeader = ByteBuffer.allocate(Integer.BYTES + 1).order(ByteOrder.BIG_ENDIAN);
        channel.position((long) sectorOffset * SECTOR_BYTES);
        while (chunkHeader.hasRemaining() && channel.read(chunkHeader) != -1) {
            // Continue until length and compression byte are available.
        }
        if (chunkHeader.hasRemaining()) {
            return ChunkEntry.fallback(chunkX, chunkZ, slot, timestamp);
        }

        chunkHeader.flip();
        int length = chunkHeader.getInt();
        int compression = Byte.toUnsignedInt(chunkHeader.get());
        if (length <= 1 || length > MAX_CHUNK_PAYLOAD_BYTES || length > sectorCount * SECTOR_BYTES) {
            return ChunkEntry.fallback(chunkX, chunkZ, slot, timestamp);
        }

        boolean external = (compression & EXTERNAL_STREAM_FLAG) != 0;
        int compressionType = compression & ~EXTERNAL_STREAM_FLAG;
        byte[] payload = external
                ? readExternalPayload(regionFile, chunkX, chunkZ)
                : readInternalPayload(channel, length - 1);

        try (InputStream raw = new ByteArrayInputStream(payload);
             InputStream decompressed = decompressor.decompress(compressionType, raw)) {
            if (decompressed == null) {
                return ChunkEntry.fallback(chunkX, chunkZ, slot, timestamp);
            }

            byte[] inflated = readAll(decompressed);
            boolean hasMarkerBytes = contains(inflated, markerNeedle);
            return new ChunkEntry(chunkX, chunkZ, slot, timestamp, hasMarkerBytes, hasMarkerBytes ? inflated : null, false, false);
        }
    }

    /**
     * Reads an internal chunk payload following the anvil compression byte.
     *
     * @param channel open region file channel
     * @param length  payload length
     * @return raw compressed payload
     * @throws IOException when the payload cannot be read
     */
    @SuppressWarnings("StatementWithEmptyBody")
    private byte[] readInternalPayload(FileChannel channel, int length) throws IOException {
        ByteBuffer payload = ByteBuffer.allocate(length);
        while (payload.hasRemaining() && channel.read(payload) != -1) {
            // Continue until the compressed chunk payload is filled.
        }
        if (payload.hasRemaining()) {
            throw new IOException("Chunk payload ended after " + payload.position() + " bytes");
        }
        return payload.array();
    }

    /**
     * Reads a chunk payload stored in an external {@code .mcc} file.
     *
     * @param regionFile region file owning the external payload
     * @param chunkX     absolute chunk X coordinate
     * @param chunkZ     absolute chunk Z coordinate
     * @return raw compressed payload
     * @throws IOException when the payload cannot be read
     */
    private byte[] readExternalPayload(Path regionFile, int chunkX, int chunkZ) throws IOException {
        Path externalFile = regionFile.resolveSibling("c." + chunkX + "." + chunkZ + ".mcc");
        return Files.readAllBytes(externalFile);
    }

    /**
     * Reads a decompressed stream into memory.
     *
     * @param input decompressed chunk input
     * @return chunk bytes
     * @throws IOException when the stream cannot be read
     */
    private byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
            if (output.size() > MAX_CHUNK_PAYLOAD_BYTES) {
                throw new IOException("Inflated chunk payload exceeded " + MAX_CHUNK_PAYLOAD_BYTES + " bytes");
            }
        }
        return output.toByteArray();
    }

    /**
     * Searches a byte array for a smaller byte sequence.
     *
     * @param haystack bytes to search
     * @param needle   bytes to find
     * @return true when the needle appears in the haystack
     */
    private boolean contains(byte[] haystack, byte[] needle) {
        if (needle.length == 0 || haystack.length < needle.length) {
            return false;
        }

        for (int start = 0; start <= haystack.length - needle.length; start++) {
            int index = 0;
            while (index < needle.length && haystack[start + index] == needle[index]) {
                index++;
            }
            if (index == needle.length) {
                return true;
            }
        }

        return false;
    }

    /**
     * Converts local chunk coordinates to an anvil slot index.
     *
     * @param localX local chunk X coordinate
     * @param localZ local chunk Z coordinate
     * @return location table slot
     */
    public static int slot(int localX, int localZ) {
        return localX + localZ * REGION_CHUNK_WIDTH;
    }

    /**
     * Decompresses a raw chunk payload for a Minecraft version.
     */
    @FunctionalInterface
    public interface Decompressor {
        /**
         * Wraps a raw chunk payload in a decompression stream.
         *
         * @param compressionType anvil compression type
         * @param raw             raw payload stream
         * @return decompressed payload stream, or null to request vanilla fallback
         * @throws IOException when the stream cannot be opened
         */
        InputStream decompress(int compressionType, InputStream raw) throws IOException;
    }

    /**
     * Result of reading one region file.
     *
     * @param entries chunk entries discovered from the region header
     * @param stats   region scan counters
     */
    public record ReadResult(List<ChunkEntry> entries, Stats stats) {
    }

    /**
     * Direct-read state for one chunk slot.
     *
     * @param chunkX         absolute chunk X coordinate
     * @param chunkZ         absolute chunk Z coordinate
     * @param slot           local region slot
     * @param timestamp      anvil timestamp table value
     * @param hasMarkerBytes whether the byte prefilter matched the marker id
     * @param payload        decompressed chunk payload when the prefilter matched
     * @param fallback       whether vanilla chunk storage should read this chunk
     * @param skipped        whether the cache skipped this chunk
     */
    public record ChunkEntry(int chunkX, int chunkZ, int slot, int timestamp, boolean hasMarkerBytes, byte[] payload, boolean fallback, boolean skipped) {
        /**
         * Creates a fallback entry for a chunk slot.
         *
         * @param chunkX    absolute chunk X coordinate
         * @param chunkZ    absolute chunk Z coordinate
         * @param slot      local region slot
         * @param timestamp anvil timestamp table value
         * @return fallback chunk entry
         */
        public static ChunkEntry fallback(int chunkX, int chunkZ, int slot, int timestamp) {
            return new ChunkEntry(chunkX, chunkZ, slot, timestamp, false, null, true, false);
        }

        /**
         * Creates a cache-skipped entry for a chunk slot.
         *
         * @param chunkX    absolute chunk X coordinate
         * @param chunkZ    absolute chunk Z coordinate
         * @param slot      local region slot
         * @param timestamp anvil timestamp table value
         * @return skipped chunk entry
         */
        public static ChunkEntry skipped(int chunkX, int chunkZ, int slot, int timestamp) {
            return new ChunkEntry(chunkX, chunkZ, slot, timestamp, false, null, false, true);
        }
    }

    /**
     * Region scan counters used for backup diagnostics.
     *
     * @param populatedChunks chunks with region header entries
     * @param cacheSkipped    chunks skipped by the incremental cache
     * @param prefilterSkipped chunks skipped after marker-id byte prefilter misses
     * @param markerByteHits  chunks whose inflated bytes contained the marker id
     * @param fallbacks       chunks delegated to vanilla storage fallback
     */
    public record Stats(int populatedChunks, int cacheSkipped, int prefilterSkipped, int markerByteHits, int fallbacks) {
        /**
         * Creates an empty counter set.
         *
         * @return empty stats
         */
        public static Stats empty() {
            return new Stats(0, 0, 0, 0, 0);
        }

        /**
         * Adds one populated chunk.
         *
         * @return updated stats
         */
        public Stats withPopulated() {
            return new Stats(populatedChunks + 1, cacheSkipped, prefilterSkipped, markerByteHits, fallbacks);
        }

        /**
         * Adds one cache skip.
         *
         * @return updated stats
         */
        public Stats withCacheSkip() {
            return new Stats(populatedChunks, cacheSkipped + 1, prefilterSkipped, markerByteHits, fallbacks);
        }

        /**
         * Adds one byte-prefilter skip.
         *
         * @return updated stats
         */
        public Stats withPrefilterSkip() {
            return new Stats(populatedChunks, cacheSkipped, prefilterSkipped + 1, markerByteHits, fallbacks);
        }

        /**
         * Adds one marker byte hit.
         *
         * @return updated stats
         */
        public Stats withMarkerBytes() {
            return new Stats(populatedChunks, cacheSkipped, prefilterSkipped, markerByteHits + 1, fallbacks);
        }

        /**
         * Adds one vanilla fallback.
         *
         * @return updated stats
         */
        public Stats withFallback() {
            return new Stats(populatedChunks, cacheSkipped, prefilterSkipped, markerByteHits, fallbacks + 1);
        }

        /**
         * Combines two counter sets.
         *
         * @param other counters to add
         * @return combined stats
         */
        public Stats plus(Stats other) {
            return new Stats(
                    populatedChunks + other.populatedChunks,
                    cacheSkipped + other.cacheSkipped,
                    prefilterSkipped + other.prefilterSkipped,
                    markerByteHits + other.markerByteHits,
                    fallbacks + other.fallbacks
            );
        }
    }
}
