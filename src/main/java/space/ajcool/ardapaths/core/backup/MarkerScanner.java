package space.ajcool.ardapaths.core.backup;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.backup.progress.ProgressReporter;
import space.ajcool.ardapaths.core.conversions.PathMarkerBlockEntityConverter;
import space.ajcool.ardapaths.mc.NbtEncodeable;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Reads saved chunk data through vanilla storage to discover every persisted path marker.
 */
@Slf4j(topic = "ardapaths")
public class MarkerScanner {
    /**
     * Full block entity identifier stored in chunk NBT for path markers.
     */
    static final String PATH_MARKER_BLOCK_ENTITY_ID = ArdaPaths.MOD_ID + ":path_marker_block_entity";

    /**
     * Number of chunks on one side of a Minecraft region file.
     */
    static final int REGION_CHUNK_WIDTH = 32;

    /**
     * Number of chunk reads submitted before waiting for their results.
     */
    private static final int CHUNK_READ_WINDOW = 256;

    /**
     * Size of the region-file location table in bytes.
     */
    private static final int REGION_LOCATION_TABLE_BYTES = 4096;

    /**
     * Accessor for version-specific chunk storage operations.
     */
    private final ChunkStorageAccess storageAccess;

    /**
     * Creates a scanner with an explicit storage access implementation.
     *
     * @param storageAccess chunk storage operations used during scanning
     */
    MarkerScanner(ChunkStorageAccess storageAccess) {
        this.storageAccess = storageAccess;
    }

    /**
     * Scans all server dimensions for path markers and reports region-file progress.
     *
     * @param server   the running server whose save is scanned
     * @param reporter progress reporter for scan phases
     * @param gate     gate for server-thread-only work
     * @return markers and skipped dimensions found during the scan
     */
    public ScanResult scan(MinecraftServer server, ProgressReporter reporter, BackupJobRunner.ServerGate gate) {
        reporter.phase("scanning");

        List<ScannedMarkerData> markers = new ArrayList<>();
        List<ScannedMarkerData> emptyMarkers = new ArrayList<>();
        List<String> skippedDimensions = new ArrayList<>();
        List<ServerLevel> worlds = gate.call(() -> snapshotWorlds(server));
        List<RegionScanTarget> regionTargets = collectRegionTargets(worlds, skippedDimensions);
        int scannedFiles = 0;
        log.info(
                "ArdaPaths backup scanning {} region files across {} dimensions ({})",
                regionTargets.size(),
                countDistinctDimensions(regionTargets),
                formatRegionCounts(regionTargets)
        );
        reporter.advance(scannedFiles, regionTargets.size());

        for (RegionScanTarget regionFile : regionTargets) {
            scanRegion(regionFile.world(), regionFile.regionFile(), regionFile.dimensionId(), markers, emptyMarkers);
            scannedFiles++;
            reporter.advance(scannedFiles, regionTargets.size());
        }

        markers.sort(Comparator
                .comparing(ScannedMarkerData::dimensionId)
                .thenComparingInt(marker -> marker.position().getX() >> 9)
                .thenComparingInt(marker -> marker.position().getZ() >> 9)
                .thenComparingInt(marker -> marker.position().getX() >> 4)
                .thenComparingInt(marker -> marker.position().getZ() >> 4)
                .thenComparingInt(marker -> marker.position().getY()));

        emptyMarkers.sort(Comparator
                .comparing(ScannedMarkerData::dimensionId)
                .thenComparingInt(marker -> marker.position().getX() >> 9)
                .thenComparingInt(marker -> marker.position().getZ() >> 9)
                .thenComparingInt(marker -> marker.position().getX() >> 4)
                .thenComparingInt(marker -> marker.position().getZ() >> 4)
                .thenComparingInt(marker -> marker.position().getY()));

        if (!emptyMarkers.isEmpty()) {
            log.info("ArdaPaths marker scan found {} path marker(s) with no path data", emptyMarkers.size());
        }

        return new ScanResult(markers, emptyMarkers, List.copyOf(skippedDimensions));
    }

    /**
     * Copies the server's currently loaded worlds while running on the server thread.
     *
     * @param server server whose loaded worlds should be scanned
     * @return immutable snapshot of loaded worlds
     */
    private List<ServerLevel> snapshotWorlds(MinecraftServer server) {
        List<ServerLevel> worlds = new ArrayList<>();
        server.getAllLevels().forEach(worlds::add);
        return List.copyOf(worlds);
    }

    /**
     * Collects region files across every loaded server world.
     *
     * @param worlds            server worlds snapshotted on the server thread
     * @param skippedDimensions dimensions whose region directories could not be scanned
     * @return sorted scan targets
     */
    private List<RegionScanTarget> collectRegionTargets(List<ServerLevel> worlds, List<String> skippedDimensions) {
        List<RegionScanTarget> targets = new ArrayList<>();

        for (ServerLevel world : worlds) {
            String dimensionId = world.dimension().location().toString();

            try {
                storageAccess.flushWorker(world);
                Path regionDirectory = storageAccess.regionDirectory(world);
                log.info("Resolved ArdaPaths marker scan region directory for dimension {}: {}", dimensionId, regionDirectory);

                if (!Files.isDirectory(regionDirectory)) {
                    log.warn("Skipping ArdaPaths marker scan for dimension {} because region directory is not readable: {}", dimensionId, regionDirectory);
                    skippedDimensions.add(dimensionId);
                    continue;
                }

                try (var files = Files.list(regionDirectory)) {
                    for (Path regionFile : files
                            .filter(path -> path.getFileName().toString().endsWith(".mca"))
                            .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                            .toList()) {
                        targets.add(new RegionScanTarget(world, dimensionId, regionFile));
                    }
                }
            } catch (IOException exception) {
                log.warn("Skipping ArdaPaths marker scan for dimension {} because region directory could not be listed", dimensionId, exception);
                skippedDimensions.add(dimensionId);
            } catch (RuntimeException exception) {
                log.warn("Skipping ArdaPaths marker scan for dimension {} because chunk storage could not be inspected", dimensionId, exception);
                skippedDimensions.add(dimensionId);
            }
        }

        targets.sort(Comparator
                .comparing(RegionScanTarget::dimensionId)
                .thenComparing(target -> target.regionFile().getFileName().toString()));
        return targets;
    }

    /**
     * Scans one region coordinate range for persisted path marker block entities.
     *
     * @param world        world whose vanilla chunk storage is read
     * @param regionFile   region file path used only for coordinate metadata
     * @param dimensionId  dimension identifier for discovered markers
     * @param markers      marker result accumulator
     * @param emptyMarkers marker accumulator for path markers without path data
     */
    private void scanRegion(ServerLevel world, Path regionFile, String dimensionId, List<ScannedMarkerData> markers, List<ScannedMarkerData> emptyMarkers) {
        int[] regionCoordinates = parseRegionCoordinates(regionFile.getFileName().toString());
        if (regionCoordinates == null) return;

        List<ChunkPos> chunks = populatedChunks(regionFile, regionCoordinates);

        for (int batchStart = 0; batchStart < chunks.size(); batchStart += CHUNK_READ_WINDOW) {
            List<ChunkRead> reads = new ArrayList<>();
            int batchEnd = Math.min(batchStart + CHUNK_READ_WINDOW, chunks.size());

            for (int chunkIndex = batchStart; chunkIndex < batchEnd; chunkIndex++) {
                ChunkPos chunkPos = chunks.get(chunkIndex);
                reads.add(new ChunkRead(chunkPos, storageAccess.scanChunkBlockEntities(world, chunkPos)));
            }

            for (ChunkRead read : reads) {
                try {
                    read.future().join().ifPresent(chunkNbt -> scanChunk(chunkNbt, dimensionId, markers, emptyMarkers));
                } catch (CancellationException | CompletionException exception) {
                    log.warn("Failed to read chunk {} from {}", read.chunkPos(), regionFile, exception);
                }
            }
        }
    }

    /**
     * Determines which chunk slots in a region file contain persisted chunk data.
     *
     * @param regionFile        region file whose location table is read
     * @param regionCoordinates parsed region coordinates
     * @return populated chunk positions in the scanner's stable traversal order
     */
    private List<ChunkPos> populatedChunks(Path regionFile, int[] regionCoordinates) {
        ByteBuffer header = ByteBuffer.allocate(REGION_LOCATION_TABLE_BYTES).order(ByteOrder.BIG_ENDIAN);

        try (SeekableByteChannel channel = Files.newByteChannel(regionFile, StandardOpenOption.READ)) {
            //noinspection StatementWithEmptyBody
            while (header.hasRemaining() && channel.read(header) != -1) {
                // Continue until the fixed-size location table is filled or the file ends.
            }
        } catch (IOException exception) {
            log.warn("Failed to read region header {}; scanning all chunk slots", regionFile, exception);
            return allRegionChunks(regionCoordinates);
        }

        if (header.position() < REGION_LOCATION_TABLE_BYTES) {
            log.warn("Region header {} ended after {} bytes; scanning all chunk slots", regionFile, header.position());
            return allRegionChunks(regionCoordinates);
        }

        header.flip();
        List<ChunkPos> chunks = new ArrayList<>();

        for (int localX = 0; localX < REGION_CHUNK_WIDTH; localX++) {
            for (int localZ = 0; localZ < REGION_CHUNK_WIDTH; localZ++) {
                int locationTableIndex = localX + localZ * REGION_CHUNK_WIDTH;
                int offsetAndSectorCount = header.getInt(locationTableIndex * Integer.BYTES);

                if (offsetAndSectorCount != 0) {
                    chunks.add(chunkPosition(regionCoordinates, localX, localZ));
                }
            }
        }

        return chunks;
    }

    /**
     * Builds every chunk position in a region when header prefiltering is unavailable.
     *
     * @param regionCoordinates parsed region coordinates
     * @return all chunk positions in the scanner's stable traversal order
     */
    private List<ChunkPos> allRegionChunks(int[] regionCoordinates) {
        List<ChunkPos> chunks = new ArrayList<>(REGION_CHUNK_WIDTH * REGION_CHUNK_WIDTH);

        for (int localX = 0; localX < REGION_CHUNK_WIDTH; localX++) {
            for (int localZ = 0; localZ < REGION_CHUNK_WIDTH; localZ++) {
                chunks.add(chunkPosition(regionCoordinates, localX, localZ));
            }
        }

        return chunks;
    }

    /**
     * Converts a region-local chunk coordinate to a world chunk position.
     *
     * @param regionCoordinates parsed region coordinates
     * @param localX            local chunk X coordinate inside the region
     * @param localZ            local chunk Z coordinate inside the region
     * @return absolute world chunk position
     */
    private ChunkPos chunkPosition(int[] regionCoordinates, int localX, int localZ) {
        return new ChunkPos(regionCoordinates[0] * REGION_CHUNK_WIDTH + localX, regionCoordinates[1] * REGION_CHUNK_WIDTH + localZ);
    }

    /**
     * Counts dimensions represented in a sorted region scan plan.
     *
     * @param regionTargets region scan plan
     * @return number of dimensions with region files
     */
    private int countDistinctDimensions(List<RegionScanTarget> regionTargets) {
        return (int) regionTargets.stream().map(RegionScanTarget::dimensionId).distinct().count();
    }

    /**
     * Formats per-dimension region-file counts for scan-plan logging.
     *
     * @param regionTargets region scan plan
     * @return comma-separated dimension counts
     */
    private String formatRegionCounts(List<RegionScanTarget> regionTargets) {
        if (regionTargets.isEmpty()) {
            return "none";
        }

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (RegionScanTarget target : regionTargets) {
            counts.merge(target.dimensionId(), 1, Integer::sum);
        }

        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            parts.add(entry.getKey() + "=" + entry.getValue());
        }

        return String.join(", ", parts);
    }

    /**
     * Extracts path marker entries from one chunk NBT compound.
     *
     * @param chunkNbt     chunk NBT read from disk
     * @param dimensionId  dimension identifier for discovered markers
     * @param markers      marker result accumulator
     * @param emptyMarkers marker accumulator for path markers without path data
     */
    void scanChunk(CompoundTag chunkNbt, String dimensionId, List<ScannedMarkerData> markers, List<ScannedMarkerData> emptyMarkers) {
        ListTag blockEntities = chunkNbt.getList("block_entities", Tag.TAG_COMPOUND);

        for (int i = 0; i < blockEntities.size(); i++) {
            CompoundTag blockEntityNbt = blockEntities.getCompound(i);
            if (!PATH_MARKER_BLOCK_ENTITY_ID.equals(blockEntityNbt.getString("id"))) continue;

            CompoundTag converted = PathMarkerBlockEntityConverter.convertNbt(blockEntityNbt.copy());
            CompoundTag pathsNbt = NbtEncodeable.getCompound(converted, "paths");
            Map<String, Map<String, PathMarkerBlockEntity.ChapterNbtData>> pathData = decodePathData(pathsNbt);

            BlockPos position = new BlockPos(blockEntityNbt.getInt("x"), blockEntityNbt.getInt("y"), blockEntityNbt.getInt("z"));
            if (pathData.isEmpty()) {
                emptyMarkers.add(new ScannedMarkerData(dimensionId, position, pathData));
                continue;
            }

            markers.add(new ScannedMarkerData(dimensionId, position, pathData));
        }
    }

    /**
     * Decodes nested path/chapter marker payloads from NBT.
     *
     * @param pathsNbt marker paths compound
     * @return decoded marker payloads
     */
    private Map<String, Map<String, PathMarkerBlockEntity.ChapterNbtData>> decodePathData(CompoundTag pathsNbt) {
        Map<String, Map<String, PathMarkerBlockEntity.ChapterNbtData>> pathData = new HashMap<>();

        for (String pathId : pathsNbt.getAllKeys()) {
            CompoundTag pathNbt = NbtEncodeable.getCompound(pathsNbt, pathId);
            Map<String, PathMarkerBlockEntity.ChapterNbtData> chapters = new HashMap<>();

            for (String chapterId : pathNbt.getAllKeys()) {
                chapters.put(chapterId, PathMarkerBlockEntity.ChapterNbtData.fromNbt(NbtEncodeable.getCompound(pathNbt, chapterId)));
            }

            if (!chapters.isEmpty()) pathData.put(pathId, chapters);
        }

        return pathData;
    }

    /**
     * Parses region X/Z coordinates from a file name like {@code r.0.-1.mca}.
     *
     * @param fileName region file name
     * @return two-element [regionX, regionZ], or null when the file name is invalid
     */
    static int[] parseRegionCoordinates(String fileName) {
        String[] parts = fileName.split("\\.");
        if (parts.length != 4 || !"r".equals(parts[0]) || !"mca".equals(parts[3])) return null;

        try {
            return new int[]{Integer.parseInt(parts[1]), Integer.parseInt(parts[2])};
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * Region coordinate file and dimension metadata to scan.
     *
     * @param world       world whose vanilla chunk storage is read
     * @param dimensionId dimension identifier
     * @param regionFile  region file path used only for coordinate metadata
     */
    private record RegionScanTarget(ServerLevel world, String dimensionId, Path regionFile) {
    }

    /**
     * Submitted chunk read and the chunk position it belongs to.
     *
     * @param chunkPos chunk position being read
     * @param future   pending block-entity-only chunk scan
     */
    private record ChunkRead(ChunkPos chunkPos, CompletableFuture<Optional<CompoundTag>> future) {
    }

    /**
     * Result of scanning all loaded dimensions.
     *
     * @param markers           discovered markers with path data
     * @param emptyMarkers      discovered path markers without path data
     * @param skippedDimensions dimension ids whose region directories were unreadable
     */
    public record ScanResult(List<ScannedMarkerData> markers, List<ScannedMarkerData> emptyMarkers, List<String> skippedDimensions) {
    }
}
