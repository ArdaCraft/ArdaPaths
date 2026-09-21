package space.ajcool.ardapaths.core.backup;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.backup.progress.ProgressReporter;
import space.ajcool.ardapaths.core.conversions.PathMarkerBlockEntityConverter;
import space.ajcool.ardapaths.mc.NbtEncodeable;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

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
    static final int REGION_CHUNK_WIDTH = RegionFileReader.REGION_CHUNK_WIDTH;

    /**
     * Maximum number of region files scanned at the same time.
     */
    private static final int MAX_SCAN_THREADS = 4;

    /**
     * Direct region-file reader with marker byte prefiltering.
     */
    private static final RegionFileReader REGION_READER = new RegionFileReader(PATH_MARKER_BLOCK_ENTITY_ID.getBytes(StandardCharsets.UTF_8));

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
        return scan(server, reporter, gate, false);
    }

    /**
     * Scans all server dimensions for path markers and reports region-file progress.
     *
     * @param server    the running server whose save is scanned
     * @param reporter  progress reporter for scan phases
     * @param gate      gate for server-thread-only work
     * @param forceFull whether the incremental scan cache should be ignored
     * @return markers and skipped dimensions found during the scan
     */
    public ScanResult scan(MinecraftServer server, ProgressReporter reporter, BackupJobRunner.ServerGate gate, boolean forceFull) {
        reporter.phase("scanning");

        List<ScannedMarkerData> markers = new ArrayList<>();
        List<ScannedMarkerData> emptyMarkers = new ArrayList<>();
        List<String> skippedDimensions = new ArrayList<>();
        List<ServerLevel> worlds = gate.call(() -> snapshotWorlds(server));
        List<RegionScanTarget> regionTargets = collectRegionTargets(worlds, skippedDimensions);
        long scanStartEpochSeconds = Instant.now().getEpochSecond();
        ScanCache scanCache = forceFull ? ScanCache.empty(scanStartEpochSeconds) : ScanCache.load(ScanCache.DEFAULT_PATH, scanStartEpochSeconds);
        int scannedFiles = 0;
        RegionFileReader.Stats stats = RegionFileReader.Stats.empty();
        log.info(
                "ArdaPaths backup scanning {} region files across {} dimensions ({}){}",
                regionTargets.size(),
                countDistinctDimensions(regionTargets),
                formatRegionCounts(regionTargets),
                forceFull ? " with full scan" : ""
        );
        reporter.advance(scannedFiles, regionTargets.size());

        ExecutorService executor = Executors.newFixedThreadPool(scanThreadCount(), new ScanThreadFactory());
        CompletionService<RegionScanResult> completions = new ExecutorCompletionService<>(executor);

        try {
            for (RegionScanTarget regionFile : regionTargets) {
                completions.submit(() -> scanRegion(regionFile, scanCache, forceFull));
            }

            for (int completed = 0; completed < regionTargets.size(); completed++) {
                RegionScanResult result = completions.take().get();
                markers.addAll(result.markers());
                emptyMarkers.addAll(result.emptyMarkers());
                stats = stats.plus(result.stats());
                scannedFiles++;
                reporter.advance(scannedFiles, regionTargets.size());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CancellationException("ArdaPaths marker scan interrupted");
        } catch (ExecutionException exception) {
            throw new CompletionException(exception.getCause());
        } finally {
            executor.shutdownNow();
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
        log.info(
                "ArdaPaths marker scan read {} populated chunks: {} cache skips, {} byte-prefilter skips, {} marker-byte hits, {} vanilla fallbacks",
                stats.populatedChunks(),
                stats.cacheSkipped(),
                stats.prefilterSkipped(),
                stats.markerByteHits(),
                stats.fallbacks()
        );

        try {
            scanCache.write(ScanCache.DEFAULT_PATH);
        } catch (IOException exception) {
            log.warn("Failed to write ArdaPaths marker scan cache", exception);
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
     * @param target    region scan target
     * @param scanCache cache used for skip decisions and updated observations
     * @param forceFull whether cache skipping should be ignored
     * @return scan result for one region
     */
    private RegionScanResult scanRegion(RegionScanTarget target, ScanCache scanCache, boolean forceFull) {
        int[] regionCoordinates = parseRegionCoordinates(target.regionFile().getFileName().toString());
        if (regionCoordinates == null) return RegionScanResult.empty();

        try {
            RegionFileReader.ReadResult readResult = REGION_READER.read(
                    target.regionFile(),
                    regionCoordinates[0],
                    regionCoordinates[1],
                    target.dimensionId(),
                    scanCache,
                    forceFull,
                    storageAccess::decompress
            );
            return scanRegionEntries(target, readResult, scanCache);
        } catch (IOException exception) {
            log.warn("Failed to read region {}; scanning all chunk slots through vanilla storage", target.regionFile(), exception);
            return scanRegionFallback(target, regionCoordinates, scanCache);
        }
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
     * Scans direct region-reader entries and falls back per chunk as needed.
     *
     * @param target     region scan target
     * @param readResult direct region read result
     * @param scanCache  cache updated with observed chunks
     * @return scan result for one region
     */
    private RegionScanResult scanRegionEntries(RegionScanTarget target, RegionFileReader.ReadResult readResult, ScanCache scanCache) {
        List<ScannedMarkerData> markers = new ArrayList<>();
        List<ScannedMarkerData> emptyMarkers = new ArrayList<>();

        for (RegionFileReader.ChunkEntry entry : readResult.entries()) {
            if (entry.skipped()) {
                continue;
            }

            boolean hadMarker = false;

            if (entry.fallback()) {
                hadMarker = scanFallbackChunk(target, entry.chunkX(), entry.chunkZ(), markers, emptyMarkers);
            } else if (entry.hasMarkerBytes()) {
                hadMarker = scanPrefilteredChunk(target, entry, markers, emptyMarkers);
            }

            scanCache.record(target.dimensionId(), target.regionFile().getFileName().toString(), entry.slot(), entry.timestamp(), hadMarker);
        }

        return new RegionScanResult(markers, emptyMarkers, readResult.stats());
    }

    /**
     * Parses and scans a direct-read chunk whose bytes matched the marker id.
     *
     * @param target       region scan target
     * @param entry        direct chunk entry
     * @param markers      marker accumulator
     * @param emptyMarkers empty-marker accumulator
     * @return true when a path marker was found
     */
    private boolean scanPrefilteredChunk(RegionScanTarget target, RegionFileReader.ChunkEntry entry, List<ScannedMarkerData> markers, List<ScannedMarkerData> emptyMarkers) {
        int markerCount = markers.size();
        int emptyMarkerCount = emptyMarkers.size();

        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(entry.payload()))) {
            storageAccess.parseBlockEntities(input).ifPresent(chunkNbt -> scanChunk(chunkNbt, target.dimensionId(), markers, emptyMarkers));
        } catch (IOException | RuntimeException exception) {
            log.warn("Failed to parse path-marker candidate chunk {},{} from {}; using vanilla fallback", entry.chunkX(), entry.chunkZ(), target.regionFile(), exception);
            return scanFallbackChunk(target, entry.chunkX(), entry.chunkZ(), markers, emptyMarkers);
        }

        return markers.size() > markerCount || emptyMarkers.size() > emptyMarkerCount;
    }

    /**
     * Scans all slots in a region through the vanilla storage fallback.
     *
     * @param target            region scan target
     * @param regionCoordinates parsed region coordinates
     * @param scanCache         cache updated with observed chunks
     * @return scan result for the fallback region
     */
    private RegionScanResult scanRegionFallback(RegionScanTarget target, int[] regionCoordinates, ScanCache scanCache) {
        List<ScannedMarkerData> markers = new ArrayList<>();
        List<ScannedMarkerData> emptyMarkers = new ArrayList<>();
        RegionFileReader.Stats stats = RegionFileReader.Stats.empty();

        for (ChunkPos chunkPos : allRegionChunks(regionCoordinates)) {
            int markerCount = markers.size();
            int emptyMarkerCount = emptyMarkers.size();
            scanFallbackChunk(target, chunkPos.x, chunkPos.z, markers, emptyMarkers);
            boolean hadMarker = markers.size() > markerCount || emptyMarkers.size() > emptyMarkerCount;
            int localX = Math.floorMod(chunkPos.x, REGION_CHUNK_WIDTH);
            int localZ = Math.floorMod(chunkPos.z, REGION_CHUNK_WIDTH);
            scanCache.record(target.dimensionId(), target.regionFile().getFileName().toString(), RegionFileReader.slot(localX, localZ), 0, hadMarker);
            stats = stats.withPopulated().withFallback();
        }

        return new RegionScanResult(markers, emptyMarkers, stats);
    }

    /**
     * Scans one chunk through vanilla chunk storage.
     *
     * @param target       region scan target
     * @param chunkX       chunk X coordinate
     * @param chunkZ       chunk Z coordinate
     * @param markers      marker accumulator
     * @param emptyMarkers empty-marker accumulator
     * @return true when a path marker was found
     */
    private boolean scanFallbackChunk(RegionScanTarget target, int chunkX, int chunkZ, List<ScannedMarkerData> markers, List<ScannedMarkerData> emptyMarkers) {
        int markerCount = markers.size();
        int emptyMarkerCount = emptyMarkers.size();
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);

        try {
            storageAccess.scanChunkBlockEntities(target.world(), chunkPos).join()
                    .ifPresent(chunkNbt -> scanChunk(chunkNbt, target.dimensionId(), markers, emptyMarkers));
        } catch (CancellationException | CompletionException exception) {
            log.warn("Failed to read chunk {} from {}", chunkPos, target.regionFile(), exception);
        }

        return markers.size() > markerCount || emptyMarkers.size() > emptyMarkerCount;
    }

    /**
     * Determines how many region scanner threads to use.
     *
     * @return bounded scan thread count
     */
    private int scanThreadCount() {
        return Math.max(1, Math.min(MAX_SCAN_THREADS, Runtime.getRuntime().availableProcessors() / 2));
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
     * Result of scanning one region file.
     *
     * @param markers      discovered populated markers
     * @param emptyMarkers discovered markers without path data
     * @param stats        scan counters for the region
     */
    private record RegionScanResult(List<ScannedMarkerData> markers, List<ScannedMarkerData> emptyMarkers, RegionFileReader.Stats stats) {
        /**
         * Creates an empty region scan result.
         *
         * @return empty result
         */
        private static RegionScanResult empty() {
            return new RegionScanResult(List.of(), List.of(), RegionFileReader.Stats.empty());
        }
    }

    /**
     * Thread factory for bounded direct region scans.
     */
    private static class ScanThreadFactory implements ThreadFactory {
        /**
         * Next worker number.
         */
        private int nextWorkerId = 1;

        /**
         * Creates a daemon scanner thread.
         *
         * @param runnable region scan task
         * @return daemon scanner thread
         */
        @Override
        public Thread newThread(@NotNull Runnable runnable) {
            Thread thread = new Thread(runnable, "ardapaths-backup-scan-" + nextWorkerId++);
            thread.setDaemon(true);
            return thread;
        }
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
