package space.ajcool.ardapaths.core.backup;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.scanner.NbtScanQuery;
import net.minecraft.nbt.scanner.SelectiveNbtCollector;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.backup.progress.ProgressReporter;
import space.ajcool.ardapaths.core.conversions.PathMarkerBlockEntityConverter;
import space.ajcool.ardapaths.mc.NbtEncodeable;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.mixin.RegionBasedStorageAccessor;
import space.ajcool.ardapaths.mixin.StorageIoWorkerAccessor;
import space.ajcool.ardapaths.mixin.VersionedChunkStorageAccessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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
     * Scans all server dimensions for path markers and reports region-file progress.
     *
     * @param server   the running server whose save is scanned
     * @param reporter progress reporter for scan phases
     * @return markers and skipped dimensions found during the scan
     */
    public ScanResult scan(MinecraftServer server, ProgressReporter reporter) {
        reporter.phase("scanning");

        List<ScannedMarkerData> markers = new ArrayList<>();
        List<String> skippedDimensions = new ArrayList<>();
        List<RegionScanTarget> regionTargets = collectRegionTargets(server, skippedDimensions);
        int scannedFiles = 0;
        reporter.advance(scannedFiles, regionTargets.size());

        for (RegionScanTarget regionFile : regionTargets) {
            scanRegion(regionFile.world(), regionFile.regionFile(), regionFile.dimensionId(), markers);
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

        return new ScanResult(markers, List.copyOf(skippedDimensions));
    }

    /**
     * Collects region files across every loaded server world.
     *
     * @param server            server whose save directory is scanned
     * @param skippedDimensions dimensions whose region directories could not be scanned
     * @return sorted scan targets
     */
    private List<RegionScanTarget> collectRegionTargets(MinecraftServer server, List<String> skippedDimensions) {
        List<RegionScanTarget> targets = new ArrayList<>();

        for (ServerWorld world : server.getWorlds()) {
            String dimensionId = world.getRegistryKey().getValue().toString();
            Path regionDirectory = regionDirectory(world);

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
            } catch (IOException exception) {
                log.warn("Skipping ArdaPaths marker scan for dimension {} because region directory could not be listed: {}", dimensionId, regionDirectory, exception);
                skippedDimensions.add(dimensionId);
            }
        }

        targets.sort(Comparator
                .comparing(RegionScanTarget::dimensionId)
                .thenComparing(target -> target.regionFile().getFileName().toString()));
        return targets;
    }

    /**
     * Reads the authoritative region directory from vanilla chunk storage.
     *
     * @param world world whose storage directory is needed
     * @return directory containing that world's region files
     */
    @SuppressWarnings("resource")
    private Path regionDirectory(ServerWorld world) {
        var worker = ((VersionedChunkStorageAccessor) world.getChunkManager().threadedAnvilChunkStorage).ardapaths$getWorker();
        var storage = ((StorageIoWorkerAccessor) worker).ardapaths$getStorage();
        return ((RegionBasedStorageAccessor) (Object) storage).ardapaths$getDirectory();
    }

    /**
     * Scans one region coordinate range for persisted path marker block entities with configured path data.
     *
     * @param world       world whose vanilla chunk storage is read
     * @param regionFile  region file path used only for coordinate metadata
     * @param dimensionId dimension identifier for discovered markers
     * @param markers     marker result accumulator
     */
    private void scanRegion(ServerWorld world, Path regionFile, String dimensionId, List<ScannedMarkerData> markers) {
        int[] regionCoordinates = parseRegionCoordinates(regionFile.getFileName().toString());
        if (regionCoordinates == null) return;

        for (int localX = 0; localX < REGION_CHUNK_WIDTH; localX++) {
            for (int localZ = 0; localZ < REGION_CHUNK_WIDTH; localZ++) {
                ChunkPos chunkPos = new ChunkPos(regionCoordinates[0] * REGION_CHUNK_WIDTH + localX, regionCoordinates[1] * REGION_CHUNK_WIDTH + localZ);

                try {
                    NbtCompound chunkNbt = readBlockEntitiesNbt(world, chunkPos);
                    if (chunkNbt != null) scanChunk(chunkNbt, dimensionId, markers);
                } catch (CompletionException exception) {
                    log.warn("Failed to read chunk {} from {}", chunkPos, regionFile, exception);
                }
            }
        }
    }

    /**
     * Reads only the root block entity list from one persisted chunk using vanilla's streaming scanner.
     *
     * @param world    world whose chunk IO worker should perform the read
     * @param chunkPos chunk position to scan
     * @return a partial chunk compound containing block entities, or null when absent or empty
     */
    private NbtCompound readBlockEntitiesNbt(ServerWorld world, ChunkPos chunkPos) {
        SelectiveNbtCollector collector = new SelectiveNbtCollector(new NbtScanQuery(NbtList.TYPE, "block_entities"));
        world.getChunkManager().getChunkIoWorker().scanChunk(chunkPos, collector).join();
        return collector.getRoot() instanceof NbtCompound root ? root : null;
    }

    /**
     * Extracts path marker entries from one chunk NBT compound.
     *
     * @param chunkNbt    chunk NBT read from disk
     * @param dimensionId dimension identifier for discovered markers
     * @param markers     marker result accumulator
     */
    private void scanChunk(NbtCompound chunkNbt, String dimensionId, List<ScannedMarkerData> markers) {
        NbtList blockEntities = chunkNbt.getList("block_entities", NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < blockEntities.size(); i++) {
            NbtCompound blockEntityNbt = blockEntities.getCompound(i);
            if (!PATH_MARKER_BLOCK_ENTITY_ID.equals(blockEntityNbt.getString("id"))) continue;

            NbtCompound converted = PathMarkerBlockEntityConverter.convertNbt(blockEntityNbt.copy());
            NbtCompound pathsNbt = NbtEncodeable.getCompound(converted, "paths");
            Map<String, Map<String, PathMarkerBlockEntity.ChapterNbtData>> pathData = decodePathData(pathsNbt);

            if (pathData.isEmpty()) continue;

            BlockPos position = new BlockPos(blockEntityNbt.getInt("x"), blockEntityNbt.getInt("y"), blockEntityNbt.getInt("z"));
            markers.add(new ScannedMarkerData(dimensionId, position, pathData));
        }
    }

    /**
     * Decodes nested path/chapter marker payloads from NBT.
     *
     * @param pathsNbt marker paths compound
     * @return decoded marker payloads
     */
    private Map<String, Map<String, PathMarkerBlockEntity.ChapterNbtData>> decodePathData(NbtCompound pathsNbt) {
        Map<String, Map<String, PathMarkerBlockEntity.ChapterNbtData>> pathData = new HashMap<>();

        for (String pathId : pathsNbt.getKeys()) {
            NbtCompound pathNbt = NbtEncodeable.getCompound(pathsNbt, pathId);
            Map<String, PathMarkerBlockEntity.ChapterNbtData> chapters = new HashMap<>();

            for (String chapterId : pathNbt.getKeys()) {
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
    private record RegionScanTarget(ServerWorld world, String dimensionId, Path regionFile) {
    }

    /**
     * Result of scanning all loaded dimensions.
     *
     * @param markers           discovered marker payloads
     * @param skippedDimensions dimension ids whose region directories were unreadable
     */
    public record ScanResult(List<ScannedMarkerData> markers, List<String> skippedDimensions) {
    }
}
