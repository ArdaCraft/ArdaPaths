package space.ajcool.ardapaths.core.backup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.backup.dto.*;
import space.ajcool.ardapaths.core.backup.progress.ProgressReporter;
import space.ajcool.ardapaths.core.data.BitPacker;
import space.ajcool.ardapaths.core.data.WarpTarget;
import space.ajcool.ardapaths.core.data.config.server.ServerConfig;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.Color;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Coordinates ArdaPaths backup and restore operations.
 */
@Slf4j(topic = "ardapaths")
public class BackupManager {

    /** Current portable backup schema version. */
    private static final int SCHEMA_VERSION = 2;

    /** Number of historical zip backups to keep. */
    private static final int MAX_BACKUP_ZIPS = 5;

    /** Pretty Gson used for stable human-readable export files. */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Data directory containing the latest portable backup. */
    private static final Path DATA_DIR = Path.of("./config/arda-paths/data");

    /** Directory containing rotating zip snapshots of previous data directories. */
    private static final Path BACKUP_DIR = Path.of("./config/arda-paths/data-backups");

    /**
     * Accessor for version-specific chunk storage operations.
     */
    private final ChunkStorageAccess storageAccess;

    /**
     * Scanner used to discover persisted marker block entities.
     */
    private final MarkerScanner markerScanner;

    /**
     * Creates a manager backed by Minecraft 26.1 chunk storage access.
     */
    public BackupManager() {
        this(new Minecraft261ChunkStorageAccess());
    }

    /**
     * Creates a manager with explicit chunk storage access.
     *
     * @param storageAccess version-specific chunk storage operations
     */
    BackupManager(ChunkStorageAccess storageAccess) {
        this.storageAccess = storageAccess;
        this.markerScanner = new MarkerScanner(storageAccess);
    }

    /**
     * Scans and writes the current full ArdaPaths data export with explicit threading control.
     *
     * @param server   server whose config and worlds are exported
     * @param reporter progress reporter for long-running phases
     * @param gate     gate for server-thread-only work
     * @return backup result suitable for command feedback
     * @throws IOException when files cannot be read or written
     */
    public BackupResult runBackup(MinecraftServer server, ProgressReporter reporter, BackupJobRunner.ServerGate gate) throws IOException {
        reporter.phase("saving");
        gate.run(() -> {
            ArdaPaths.CONFIG_MANAGER.flush();
            server.saveEverything(true, true, true);
        });

        MarkerScanner.ScanResult scanResult = markerScanner.scan(server, reporter, gate);
        List<ScannedMarkerData> markers = scanResult.markers();
        reporter.phase("serializing");
        BackupSnapshot snapshot = createSnapshot(markers);
        Optional<ManifestDto> currentManifest = readManifest(DATA_DIR);

        if (currentManifest.isPresent() && Objects.equals(currentManifest.get().files(), snapshot.manifest().files())) {
            return new BackupResult(false, false, snapshot.stats(), null, scanResult.skippedDimensions());
        }

        String zipName = null;
        if (Files.isDirectory(DATA_DIR) && Files.exists(DATA_DIR.resolve("manifest.json"))) {
            reporter.phase("zipping");
            zipName = zipExistingData();
        }

        reporter.phase("writing");
        writeSnapshot(snapshot);
        reporter.phase("rotating");
        rotateBackupZips();

        return new BackupResult(true, zipName != null, snapshot.stats(), zipName, scanResult.skippedDimensions());
    }

    /**
     * Creates in-memory JSON files and manifest for the supplied marker scan.
     *
     * @param markers scanned marker data
     * @return complete export snapshot
     */
    private BackupSnapshot createSnapshot(List<ScannedMarkerData> markers) {
        TreeMap<String, String> files = new TreeMap<>();
        MarkerIndexDto markerIndex = createMarkerIndex(markers);
        files.put("markers.json", GSON.toJson(markerIndex));

        int chapterCount = 0;
        int nodeCount = 0;

        for (PathData pathData : sortedPaths()) {
            PathFileDto pathFile = createPathFile(pathData, markers);
            chapterCount += pathFile.chapters().size();
            nodeCount += pathFile.chapters().stream().mapToInt(chapter -> chapter.nodes().size()).sum();
            files.put("paths/" + safeFileName(pathData.getId()) + ".json", GSON.toJson(pathFile));
        }

        BackupCountsDto counts = new BackupCountsDto(markerIndex.markers().size(), markers.size(), sortedPaths().size(), chapterCount, nodeCount);
        TreeMap<String, String> hashes = hashFiles(files);
        ManifestDto manifest = new ManifestDto(SCHEMA_VERSION, LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME) + "Z", counts, hashes);
        files.put("manifest.json", GSON.toJson(manifest));

        return new BackupSnapshot(files, manifest, statsFromCounts(counts));
    }

    /**
     * Reads the current manifest when a data directory exists.
     *
     * @param directory backup data directory
     * @return parsed manifest, or empty when absent/invalid
     */
    @SuppressWarnings("SameParameterValue")
    private Optional<ManifestDto> readManifest(Path directory) {
        Path manifestPath = directory.resolve("manifest.json");
        if (!Files.isRegularFile(manifestPath)) return Optional.empty();

        try {
            return Optional.ofNullable(GSON.fromJson(Files.readString(manifestPath), ManifestDto.class));
        } catch (IOException | JsonParseException exception) {
            log.warn("Failed to read existing ArdaPaths backup manifest", exception);
            return Optional.empty();
        }
    }

    /**
     * Creates a zip of the existing live data directory.
     *
     * @return created zip file name
     * @throws IOException when the zip cannot be written
     */
    private String zipExistingData() throws IOException {
        Files.createDirectories(BACKUP_DIR);
        String zipName = "ardapath-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".zip";
        Path zipPath = BACKUP_DIR.resolve(zipName);

        try (OutputStream outputStream = Files.newOutputStream(zipPath);
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            try (var files = Files.walk(DATA_DIR)) {
                for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                    String relativePath = DATA_DIR.relativize(file).toString().replace('\\', '/');
                    zipOutputStream.putNextEntry(new ZipEntry(relativePath));
                    Files.copy(file, zipOutputStream);
                    zipOutputStream.closeEntry();
                }
            }
        }

        return zipName;
    }

    /**
     * Writes a backup snapshot to the live data directory.
     *
     * @param snapshot snapshot to write
     * @throws IOException when a file cannot be written
     */
    private void writeSnapshot(BackupSnapshot snapshot) throws IOException {
        Files.createDirectories(DATA_DIR);
        Files.createDirectories(DATA_DIR.resolve("paths"));

        Set<Path> expectedFiles = new HashSet<>();
        for (Map.Entry<String, String> fileEntry : snapshot.files().entrySet()) {
            Path file = DATA_DIR.resolve(fileEntry.getKey());
            expectedFiles.add(file.normalize());
            atomicWriteString(file, fileEntry.getValue());
        }

        deleteStaleFiles(DATA_DIR, expectedFiles);
    }

    /**
     * Deletes historical backup zips beyond the retention limit.
     *
     * @throws IOException when an old zip cannot be deleted
     */
    private void rotateBackupZips() throws IOException {
        if (!Files.isDirectory(BACKUP_DIR)) return;

        try (var files = Files.list(BACKUP_DIR)) {
            List<Path> zips = files
                    .filter(path -> path.getFileName().toString().endsWith(".zip"))
                    .sorted(Comparator.comparing(Path::getFileName).reversed())
                    .toList();

            for (int i = MAX_BACKUP_ZIPS; i < zips.size(); i++) {
                Files.deleteIfExists(zips.get(i));
            }
        }
    }

    /**
     * Creates a marker position index grouped by dimension.
     *
     * @param markers scanned marker data
     * @return marker index DTO
     */
    private MarkerIndexDto createMarkerIndex(List<ScannedMarkerData> markers) {
        Map<String, Map<String, int[]>> byDimension = new TreeMap<>();

        for (ScannedMarkerData marker : markers) {
            BlockPos position = marker.position();
            byDimension
                    .computeIfAbsent(marker.dimensionId(), ignored -> new TreeMap<>())
                    .put(Long.toString(position.asLong()), new int[]{position.getX(), position.getY(), position.getZ()});
        }

        return new MarkerIndexDto(byDimension);
    }

    /**
     * Gets paths in deterministic export order.
     *
     * @return sorted path definitions
     */
    private List<PathData> sortedPaths() {
        return ArdaPaths.CONFIG.getPaths().stream()
                .sorted(Comparator.comparing(PathData::getId))
                .toList();
    }

    /**
     * Creates one path export file from config and scanned marker data.
     *
     * @param pathData source path definition
     * @param markers  scanned marker data
     * @return path file DTO
     */
    private PathFileDto createPathFile(PathData pathData, List<ScannedMarkerData> markers) {
        List<PathChapterDto> chapters = sortedChapters(pathData).stream()
                .map(chapter -> createChapter(pathData, chapter, markers))
                .toList();
        PathDiagnosticsDto diagnostics = createDiagnostics(chapters);

        return new PathFileDto(
                pathData.getId(),
                pathData.getName(),
                new PathColorDto(toRgb(pathData.getPrimaryColor()), toRgb(pathData.getSecondaryColor()), toRgb(pathData.getTertiaryColor())),
                chapters,
                diagnostics
        );
    }

    /**
     * Creates a conservative file name for a path identifier.
     *
     * @param pathId path identifier
     * @return path file name stem
     */
    private String safeFileName(String pathId) {
        String safe = pathId.replaceAll("[^A-Za-z0-9._-]", "_");
        return safe.isBlank() ? "path" : safe;
    }

    /**
     * Computes hashes for serialized files.
     *
     * @param files relative file path to content map
     * @return relative file path to SHA-256 hash map
     */
    private TreeMap<String, String> hashFiles(Map<String, String> files) {
        TreeMap<String, String> hashes = new TreeMap<>();
        for (Map.Entry<String, String> fileEntry : files.entrySet()) {
            hashes.put(fileEntry.getKey(), sha256(fileEntry.getValue()));
        }
        return hashes;
    }

    /**
     * Converts manifest counts into command-facing stats.
     *
     * @param counts manifest counts
     * @return backup stats
     */
    private BackupStats statsFromCounts(BackupCountsDto counts) {
        return new BackupStats(counts.dimensions(), counts.markers(), counts.paths(), counts.chapters(), counts.nodes());
    }

    /**
     * Writes one file with a temporary sibling and atomic replacement when supported.
     *
     * @param file    target file
     * @param content UTF-8 content
     * @throws IOException when the file cannot be written
     */
    private void atomicWriteString(Path file, String content) throws IOException {
        Path parent = file.getParent();
        if (parent != null) Files.createDirectories(parent);

        Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tempFile, content, StandardCharsets.UTF_8);

        try {
            Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Removes files from a backup directory that are not part of the current snapshot.
     *
     * @param directory     live data directory
     * @param expectedFiles normalized files that should remain
     * @throws IOException when a stale file cannot be removed
     */
    private void deleteStaleFiles(Path directory, Set<Path> expectedFiles) throws IOException {
        if (!Files.isDirectory(directory)) return;

        try (var files = Files.walk(directory)) {
            for (Path file : files.filter(Files::isRegularFile).sorted(Comparator.reverseOrder()).toList()) {
                if (!expectedFiles.contains(file.normalize())) {
                    Files.deleteIfExists(file);
                }
            }
        }
    }

    /**
     * Gets chapters in deterministic export order.
     *
     * @param pathData path containing chapters
     * @return sorted chapter definitions
     */
    private List<ChapterData> sortedChapters(PathData pathData) {
        return pathData.getChapters().stream()
                .sorted(Comparator.comparingInt(ChapterData::getIndex).thenComparing(ChapterData::getId))
                .toList();
    }

    /**
     * Creates one exported chapter and its marker nodes.
     *
     * @param pathData source path definition
     * @param chapter  source chapter definition
     * @param markers  scanned marker data
     * @return chapter DTO
     */
    private PathChapterDto createChapter(PathData pathData, ChapterData chapter, List<ScannedMarkerData> markers) {
        List<PathNodeDto> nodes = markers.stream()
                .map(marker -> createNode(pathData.getId(), chapter.getId(), marker))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparingLong(PathNodeDto::pos))
                .toList();
        return new PathChapterDto(
                chapter.getId(),
                chapter.getName(),
                chapter.getDate(),
                chapter.getIndex(),
                chapter.getWarp(),
                chapter.getCoordinates(),
                chapter.getCoordinates() == null ? null : chapter.getDimension(),
                nodes
        );
    }

    /**
     * Builds informational graph diagnostics for a path file.
     *
     * @param chapters exported chapters
     * @return diagnostics DTO
     */
    private PathDiagnosticsDto createDiagnostics(List<PathChapterDto> chapters) {
        Set<Long> allPositions = new HashSet<>();
        Set<Long> allIncoming = new HashSet<>();
        List<Long> danglingNext = new ArrayList<>();
        List<Long> orphans = new ArrayList<>();
        List<List<Long>> cycles = new ArrayList<>();
        Map<String, List<Long>> multiRoot = new TreeMap<>();

        for (PathChapterDto chapter : chapters) {
            Set<Long> chapterPositions = new HashSet<>();
            Set<Long> chapterIncoming = new HashSet<>();

            for (PathNodeDto node : chapter.nodes()) {
                allPositions.add(node.pos());
                chapterPositions.add(node.pos());
                if (node.next() != null) {
                    chapterIncoming.add(node.next());
                    allIncoming.add(node.next());
                    if (!chapterPositions.contains(node.next()) && chapter.nodes().stream().noneMatch(candidate -> candidate.pos() == node.next())) {
                        danglingNext.add(node.next());
                    }
                }
            }

            List<Long> roots = chapter.nodes().stream()
                    .map(PathNodeDto::pos)
                    .filter(pos -> !chapterIncoming.contains(pos))
                    .sorted()
                    .toList();
            if (roots.size() > 1) multiRoot.put(chapter.id(), roots);
            cycles.addAll(findCycles(chapter.nodes()));
        }

        for (Long position : allPositions.stream().sorted().toList()) {
            if (!allIncoming.contains(position)) orphans.add(position);
        }

        danglingNext.sort(Long::compareTo);
        return new PathDiagnosticsDto(orphans, danglingNext.stream().distinct().toList(), cycles, multiRoot);
    }

    /**
     * Converts a config colour to RGB array form.
     *
     * @param color config colour
     * @return [r, g, b] array
     */
    private int[] toRgb(Color color) {
        return new int[]{color.r, color.g, color.b};
    }

    /**
     * Computes a SHA-256 hash for text.
     *
     * @param content content to hash as UTF-8
     * @return lowercase hex digest
     */
    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte hashByte : hash) {
                builder.append(String.format("%02x", hashByte));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    /**
     * Creates an exported node for a marker when it has data for the requested path chapter.
     *
     * @param pathId    path identifier
     * @param chapterId chapter identifier
     * @param marker    scanned marker
     * @return exported node, or empty when the marker is unrelated
     */
    Optional<PathNodeDto> createNode(String pathId, String chapterId, ScannedMarkerData marker) {
        Map<String, PathMarkerBlockEntity.ChapterNbtData> chapters = marker.pathData().get(pathId);
        if (chapters == null) return Optional.empty();

        PathMarkerBlockEntity.ChapterNbtData chapterNbtData = chapters.get(chapterId);
        if (chapterNbtData == null) return Optional.empty();

        BlockPos target = chapterNbtData.getTarget();
        Long next = target == null ? null : marker.position().offset(target).asLong();
        long packedMessageData = chapterNbtData.getPackedMessageData();

        return Optional.of(new PathNodeDto(
                marker.dimensionId(),
                marker.position().asLong(),
                next,
                chapterNbtData.isChapterStart(),
                chapterNbtData.isDisplayChapterTitleOnTrail(),
                chapterNbtData.isDisplayAboveBlocks(),
                chapterNbtData.getWeather(),
                chapterNbtData.getTimeOfDay(),
                chapterNbtData.getTimeTransitionRange(),
                chapterNbtData.getAutoTeleportTarget(),
                WarpTarget.formatCoordinates(chapterNbtData.getLookAt()),
                chapterNbtData.getGiveItem(),
                chapterNbtData.getProximityMessage(),
                chapterNbtData.getActivationRange(),
                new NodeAnimDto(packedMessageData, BitPacker.unpackFive(packedMessageData))
        ));
    }

    /**
     * Detects simple next-link cycles in one chapter.
     *
     * @param nodes chapter nodes
     * @return cycle position lists
     */
    private List<List<Long>> findCycles(List<PathNodeDto> nodes) {
        Map<Long, Long> nextByPosition = new HashMap<>();
        for (PathNodeDto node : nodes) {
            if (node.next() != null) nextByPosition.put(node.pos(), node.next());
        }

        Set<Long> reported = new HashSet<>();
        List<List<Long>> cycles = new ArrayList<>();

        for (Long start : nextByPosition.keySet()) {
            Set<Long> seen = new HashSet<>();
            ArrayDeque<Long> chain = new ArrayDeque<>();
            Long current = start;

            while (current != null && nextByPosition.containsKey(current)) {
                if (!seen.add(current)) {
                    if (reported.add(current)) cycles.add(new ArrayList<>(chain));
                    break;
                }
                chain.add(current);
                current = nextByPosition.get(current);
            }
        }

        return cycles;
    }

    /**
     * Restores the latest data directory or a named historical zip backup with explicit threading control.
     *
     * @param server   server whose config and worlds are updated
     * @param zipName  optional zip file name inside {@code data-backups}
     * @param hard     whether markers absent from the backup should be deleted
     * @param reporter progress reporter for long-running phases
     * @param gate     gate for server-thread-only work
     * @return restore result suitable for command feedback
     * @throws IOException when backup files cannot be read or applied
     */
    public RestoreResult runRestore(MinecraftServer server, @Nullable String zipName, boolean hard, ProgressReporter reporter, BackupJobRunner.ServerGate gate) throws IOException {
        Path restoreDirectory = DATA_DIR;
        Path temporaryDirectory = null;
        String source = "data";

        if (zipName != null && !zipName.isBlank()) {
            reporter.phase("reading");
            Path zipPath = BACKUP_DIR.resolve(zipName).normalize();
            if (!zipPath.startsWith(BACKUP_DIR) || !Files.isRegularFile(zipPath)) {
                throw new IOException("Backup zip not found: " + zipName);
            }

            temporaryDirectory = Files.createTempDirectory("ardapaths-restore-");
            unzip(zipPath, temporaryDirectory);
            restoreDirectory = temporaryDirectory;
            source = zipName;
        }

        try {
            reporter.phase("reading");
            RestorableBackup backup = readBackup(restoreDirectory);
            reporter.phase("verifying");
            verifyManifest(restoreDirectory, backup.manifest());
            reporter.phase("config");
            gate.run(() -> restoreServerConfig(backup.paths()));

            reporter.phase("saving");
            gate.run(() -> server.saveEverything(true, true, true));
            reporter.phase("planning");
            List<PlannedMarker> plannedMarkers = new MarkerRestorer().plan(backup.markerIndex(), backup.paths());
            RestoreApplyResult applyResult = applyMarkersInBatches(server, plannedMarkers, reporter, gate);
            int deletedMarkers = hard ? deleteStaleMarkers(server, backup.markerIndex(), reporter, gate) : 0;

            reporter.phase("saving");
            gate.run(() -> {
                server.saveEverything(true, true, true);
                PacketRegistry.syncPathDataToClients(server);
            });
            if (temporaryDirectory != null) {
                reporter.phase("writing");
                copyBackupDirectory(temporaryDirectory, DATA_DIR);
            }

            logSkippedMarkers(applyResult.missingChunks(), applyResult.missingMarkers(), applyResult.conflicts(), applyResult.failed());
            return new RestoreResult(source, statsFromCounts(backup.manifest().counts()), applyResult.placed(), hard, deletedMarkers, applyResult.skipped(), applyResult.missingChunks().size(), applyResult.conflicts());
        } finally {
            if (temporaryDirectory != null) {
                deleteRecursively(temporaryDirectory);
            }
        }
    }

    /**
     * Extracts a backup zip into a temporary directory.
     *
     * @param zipPath         zip file to extract
     * @param targetDirectory temporary extraction directory
     * @throws IOException when the zip cannot be extracted
     */
    private void unzip(Path zipPath, Path targetDirectory) throws IOException {
        try (InputStream inputStream = Files.newInputStream(zipPath);
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path target = targetDirectory.resolve(entry.getName()).normalize();
                if (!target.startsWith(targetDirectory)) {
                    throw new IOException("Backup zip contains an unsafe entry: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Path parent = target.getParent();
                    if (parent != null) Files.createDirectories(parent);
                    Files.copy(zipInputStream, target, StandardCopyOption.REPLACE_EXISTING);
                }
                zipInputStream.closeEntry();
            }
        }
    }

    /**
     * Reads a backup directory from disk.
     *
     * @param directory backup directory
     * @return restorable backup data
     * @throws IOException when required files cannot be read
     */
    private RestorableBackup readBackup(Path directory) throws IOException {
        ManifestDto manifest = GSON.fromJson(Files.readString(directory.resolve("manifest.json")), ManifestDto.class);
        MarkerIndexDto markerIndex = GSON.fromJson(Files.readString(directory.resolve("markers.json")), MarkerIndexDto.class);
        List<PathFileDto> paths = new ArrayList<>();
        Path pathsDirectory = directory.resolve("paths");

        if (manifest == null || markerIndex == null || markerIndex.markers() == null) {
            throw new IOException("Backup is missing required manifest or marker index");
        }

        if (Files.isDirectory(pathsDirectory)) {
            try (var files = Files.list(pathsDirectory)) {
                for (Path pathFile : files
                        .filter(path -> path.getFileName().toString().endsWith(".json"))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .toList()) {
                    paths.add(GSON.fromJson(Files.readString(pathFile), PathFileDto.class));
                }
            }
        }

        return new RestorableBackup(manifest, markerIndex, paths);
    }

    /**
     * Verifies manifest hashes for all files in a backup directory.
     *
     * @param directory backup directory to verify
     * @param manifest  manifest containing expected hashes
     * @throws IOException when a hash is missing or mismatched
     */
    private void verifyManifest(Path directory, ManifestDto manifest) throws IOException {
        if (manifest.schemaVersion() != SCHEMA_VERSION) {
            throw new IOException("Unsupported ArdaPaths backup schema version: " + manifest.schemaVersion());
        }

        Path normalizedDirectory = directory.normalize();
        for (Map.Entry<String, String> hashEntry : manifest.files().entrySet()) {
            Path file = normalizedDirectory.resolve(hashEntry.getKey()).normalize();
            if (!file.startsWith(normalizedDirectory) || !Files.isRegularFile(file)) {
                throw new IOException("Backup file listed in manifest is missing: " + hashEntry.getKey());
            }

            String actualHash = sha256(Files.readString(file));
            if (!hashEntry.getValue().equals(actualHash)) {
                throw new IOException("Backup hash mismatch for " + hashEntry.getKey());
            }
        }
    }

    /**
     * Rebuilds and persists the authoritative server config from path files.
     *
     * @param paths exported path files
     */
    private void restoreServerConfig(List<PathFileDto> paths) {
        ServerConfig serverConfig = new ServerConfig();

        for (PathFileDto pathFile : paths) {
            PathData pathData = new PathData()
                    .setId(pathFile.id())
                    .setName(pathFile.name())
                    .setPrimaryColor(fromRgb(pathFile.colors().primary()))
                    .setSecondaryColor(fromRgb(pathFile.colors().secondary()))
                    .setTertiaryColor(fromRgb(pathFile.colors().tertiary()));

            for (PathChapterDto chapterFile : pathFile.chapters()) {
                ChapterData chapter = new ChapterData(chapterFile.id(), chapterFile.name(), chapterFile.date(), chapterFile.index(), chapterFile.warp());
                chapter.setCoordinates(chapterFile.coordinates());
                chapter.setDimension(chapterFile.dimension());
                pathData.setChapter(chapter);
            }

            serverConfig.addPath(pathData);
        }

        ArdaPaths.CONFIG_MANAGER.setConfig(serverConfig);
        ArdaPaths.CONFIG = serverConfig;
        ArdaPaths.CONFIG_MANAGER.save();
        ArdaPaths.CONFIG_MANAGER.flush();
    }

    /**
     * Applies planned markers in bounded server-thread batches.
     *
     * @param server         target server
     * @param plannedMarkers marker payloads to apply
     * @param reporter       progress reporter
     * @param gate           gate for server-thread-only work
     * @return marker restore counts
     */
    private RestoreApplyResult applyMarkersInBatches(MinecraftServer server, List<PlannedMarker> plannedMarkers, ProgressReporter reporter, BackupJobRunner.ServerGate gate) {
        reporter.phase("placing");
        RestoreApplyResult result = RestoreApplyResult.empty();

        for (int batchStart = 0; batchStart < plannedMarkers.size(); ) {
            int toIndex = MarkerBatching.findChunkBoundedBatchEnd(
                    plannedMarkers,
                    batchStart,
                    PlannedMarker::dimensionId,
                    marker -> ChunkPos.pack(BlockPos.of(marker.packedPos()))
            );
            int fromIndex = batchStart;
            result = result.plus(gate.call(() -> applyMarkerBatch(server, plannedMarkers.subList(fromIndex, toIndex))));
            reporter.advance(toIndex, plannedMarkers.size());
            batchStart = toIndex;
            MarkerBatching.paceBetweenBatches(batchStart, plannedMarkers.size());
        }

        return result;
    }

    /**
     * Scans the live world and deletes markers absent from the backup index.
     *
     * @param server      target server
     * @param markerIndex backup marker index
     * @param reporter    progress reporter
     * @param gate        gate for server-thread-only work
     * @return number of stale markers deleted
     */
    private int deleteStaleMarkers(MinecraftServer server, MarkerIndexDto markerIndex, ProgressReporter reporter, BackupJobRunner.ServerGate gate) {
        reporter.phase("saving");
        gate.run(() -> server.saveEverything(true, true, true));

        MarkerScanner.ScanResult scanResult = markerScanner.scan(server, reporter, gate);
        List<ScannedMarkerData> currentMarkers = new ArrayList<>(scanResult.markers());
        currentMarkers.addAll(scanResult.emptyMarkers());
        Set<String> backupLocations = markerLocations(markerIndex);
        List<ScannedMarkerData> staleMarkers = currentMarkers.stream()
                .filter(marker -> !backupLocations.contains(markerLocation(marker.dimensionId(), marker.position().asLong())))
                .sorted(Comparator
                        .comparing(ScannedMarkerData::dimensionId)
                        .thenComparingLong(marker -> ChunkPos.pack(marker.position()))
                        .thenComparingInt(marker -> marker.position().getY()))
                .toList();

        reporter.phase("deleting");
        // NOTE: Very large hard deletes may need an offline block-palette editing pass later.
        int deleted = 0;

        for (int batchStart = 0; batchStart < staleMarkers.size(); ) {
            int toIndex = MarkerBatching.findChunkBoundedBatchEnd(
                    staleMarkers,
                    batchStart,
                    ScannedMarkerData::dimensionId,
                    marker -> ChunkPos.pack(marker.position())
            );
            int fromIndex = batchStart;
            deleted += gate.call(() -> deleteMarkerBatch(server, staleMarkers.subList(fromIndex, toIndex)));
            reporter.advance(toIndex, staleMarkers.size());
            batchStart = toIndex;
            MarkerBatching.paceBetweenBatches(batchStart, staleMarkers.size());
        }

        return deleted;
    }

    /**
     * Copies a verified backup directory into the live data directory.
     *
     * @param sourceDirectory backup directory to copy
     * @param targetDirectory live data directory
     * @throws IOException when files cannot be copied
     */
    @SuppressWarnings("SameParameterValue")
    private void copyBackupDirectory(Path sourceDirectory, Path targetDirectory) throws IOException {
        Files.createDirectories(targetDirectory);
        Set<Path> expectedFiles = new HashSet<>();

        try (var files = Files.walk(sourceDirectory)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                Path relativePath = sourceDirectory.relativize(file);
                Path targetFile = targetDirectory.resolve(relativePath);
                expectedFiles.add(targetFile.normalize());
                atomicWriteString(targetFile, Files.readString(file));
            }
        }

        deleteStaleFiles(targetDirectory, expectedFiles);
    }

    /**
     * Logs a concise summary of markers skipped because their target chunks or blocks are unsafe.
     *
     * @param missingChunks  chunks absent from the target world data
     * @param skippedMarkers marker payloads skipped because their chunks are missing
     * @param conflicts      marker payloads skipped because another block occupied the target
     * @param failed         marker payloads skipped because placement failed
     */
    private void logSkippedMarkers(List<ChunkKey> missingChunks, int skippedMarkers, int conflicts, int failed) {
        if (missingChunks.isEmpty() && skippedMarkers == 0 && conflicts == 0 && failed == 0) return;

        List<String> formattedChunks = missingChunks.stream()
                .sorted(Comparator
                        .comparing(ChunkKey::dimensionId)
                        .thenComparingInt(key -> ChunkPos.unpack(key.chunkPos()).x())
                        .thenComparingInt(key -> ChunkPos.unpack(key.chunkPos()).z()))
                .map(key -> {
                    ChunkPos chunkPos = ChunkPos.unpack(key.chunkPos());
                    return key.dimensionId() + " [" + chunkPos.x() + ", " + chunkPos.z() + "]";
                })
                .toList();
        String chunks = String.join(", ", formattedChunks.stream().limit(10).toList());
        if (formattedChunks.size() > 10) {
            chunks += ", and " + (formattedChunks.size() - 10) + " other chunks, please review world data before running the restoration process";
        }

        if (!missingChunks.isEmpty()) {
            log.warn("ArdaPaths restore skipped {} markers in {} chunk(s) missing from the world: {}", skippedMarkers, missingChunks.size(), chunks);
        }
        if (conflicts > 0) {
            log.warn("ArdaPaths restore skipped {} marker(s) because a non-marker block occupied the target position", conflicts);
        }
        if (failed > 0) {
            log.warn("ArdaPaths restore skipped {} marker(s) because marker placement did not produce a valid marker block entity", failed);
        }
    }

    /**
     * Deletes a directory tree used for temporary restore extraction.
     *
     * @param directory directory to delete
     * @throws IOException when a file cannot be deleted
     */
    private void deleteRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) return;

        try (var files = Files.walk(directory)) {
            for (Path path : files.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    /**
     * Converts exported RGB array form into a config colour.
     *
     * @param rgb [r, g, b] array
     * @return config colour
     */
    private Color fromRgb(int[] rgb) {
        if (rgb == null || rgb.length < 3) return new Color(255, 255, 255);
        return new Color(rgb[0], rgb[1], rgb[2]);
    }

    /**
     * Applies a marker batch on the server thread.
     *
     * @param server target server
     * @param batch  marker batch
     * @return marker restore counts for the batch
     */
    private RestoreApplyResult applyMarkerBatch(MinecraftServer server, List<PlannedMarker> batch) {
        RestoreApplyResult result = RestoreApplyResult.empty();

        for (PlannedMarker marker : batch) {
            ServerLevel world = getWorld(server, marker.dimensionId());
            if (world == null) {
                result = result.withFailed();
                continue;
            }

            BlockPos position = BlockPos.of(marker.packedPos());
            ChunkPos chunkPos = ChunkPos.containing(position);
            if (!chunkExists(world, chunkPos)) {
                result = result.missing(new ChunkKey(marker.dimensionId(), chunkPos.pack()));
                continue;
            }

            world.getChunk(chunkPos.x(), chunkPos.z(), ChunkStatus.FULL, true);

            switch (MarkerRestorer.apply(world, position, marker.pathsNbt())) {
                case PLACED -> result = result.withPlaced();
                case CONFLICT -> result = result.conflict();
                case FAILED -> result = result.withFailed();
            }
        }

        return result;
    }

    /**
     * Builds comparable marker location keys from a marker index.
     *
     * @param markerIndex marker index to inspect
     * @return dimension and packed-position keys
     */
    private Set<String> markerLocations(MarkerIndexDto markerIndex) {
        Set<String> locations = new HashSet<>();

        for (Map.Entry<String, Map<String, int[]>> dimensionEntry : markerIndex.markers().entrySet()) {
            for (String packedPosition : dimensionEntry.getValue().keySet()) {
                locations.add(markerLocation(dimensionEntry.getKey(), Long.parseLong(packedPosition)));
            }
        }

        return locations;
    }

    /**
     * Builds a stable comparable marker location key.
     *
     * @param dimensionId dimension identifier
     * @param packedPos   packed block position
     * @return comparable marker location key
     */
    private String markerLocation(String dimensionId, long packedPos) {
        return dimensionId + "\u0000" + packedPos;
    }

    /**
     * Deletes a stale marker batch on the server thread.
     *
     * @param server target server
     * @param batch  stale marker batch
     * @return number of markers deleted
     */
    private int deleteMarkerBatch(MinecraftServer server, List<ScannedMarkerData> batch) {
        int deleted = 0;

        for (ScannedMarkerData marker : batch) {
            ServerLevel world = getWorld(server, marker.dimensionId());
            if (world == null) continue;

            if (MarkerRestorer.delete(world, marker.position())) {
                deleted++;
            }
        }

        return deleted;
    }

    /**
     * Finds a loaded server world by dimension identifier.
     *
     * @param server      server containing worlds
     * @param dimensionId dimension identifier
     * @return matching server world, or null when unavailable
     */
    private @Nullable ServerLevel getWorld(MinecraftServer server, String dimensionId) {
        for (ServerLevel world : server.getAllLevels()) {
            if (world.dimension().identifier().toString().equals(dimensionId)) {
                return world;
            }
        }

        return null;
    }

    /**
     * Checks whether a chunk has persisted NBT without creating terrain.
     *
     * @param world    world whose chunk storage is probed
     * @param chunkPos chunk position to inspect
     * @return true when the chunk already exists in vanilla storage
     */
    private boolean chunkExists(ServerLevel world, ChunkPos chunkPos) {
        if (storageAccess.isChunkLoaded(world, chunkPos)) {
            return true;
        }

        try {
            return storageAccess.readChunkNbt(world, chunkPos).isPresent();
        } catch (CompletionException exception) {
            log.warn("Failed to probe ArdaPaths restore chunk {}", chunkPos, exception);
            return false;
        }
    }

    /**
     * Lists available backup zip names for command suggestions.
     *
     * @return sorted zip file names from newest to oldest
     */
    public List<String> listBackupZipNames() {
        if (!Files.isDirectory(BACKUP_DIR)) return List.of();

        try (var files = Files.list(BACKUP_DIR)) {
            return files
                    .filter(path -> path.getFileName().toString().endsWith(".zip"))
                    .sorted(Comparator.comparing(Path::getFileName).reversed())
                    .map(path -> path.getFileName().toString())
                    .toList();
        } catch (IOException exception) {
            log.warn("Failed to list ArdaPaths backup zips", exception);
            return List.of();
        }
    }

    /**
     * Complete serialized backup snapshot.
     *
     * @param files    relative file path to JSON content
     * @param manifest generated manifest
     * @param stats    export stats
     */
    private record BackupSnapshot(Map<String, String> files, ManifestDto manifest, BackupStats stats) {

    }

    /**
     * Parsed backup input ready for restore.
     *
     * @param manifest    parsed manifest
     * @param markerIndex parsed marker index
     * @param paths       parsed path files
     */
    private record RestorableBackup(ManifestDto manifest, MarkerIndexDto markerIndex, List<PathFileDto> paths) {

    }

    /**
     * Accumulated marker placement counts from restore batches.
     *
     * @param placed         marker payloads applied to path marker block entities
     * @param missingChunks  target chunks absent from the world
     * @param missingMarkers marker payloads skipped because their chunks were absent
     * @param conflicts      marker payloads skipped because another block occupied the target
     * @param failed         marker payloads skipped because placement did not produce a marker entity
     */
    private record RestoreApplyResult(int placed, List<ChunkKey> missingChunks, int missingMarkers, int conflicts,
                                      int failed) {

        /**
         * Creates an empty restore-apply result.
         *
         * @return zero-count result
         */
        private static RestoreApplyResult empty() {
            return new RestoreApplyResult(0, List.of(), 0, 0, 0);
        }

        /**
         * Adds one successfully placed marker.
         *
         * @return updated result
         */
        private RestoreApplyResult withPlaced() {
            return new RestoreApplyResult(placed + 1, missingChunks, missingMarkers, conflicts, failed);
        }

        /**
         * Adds one missing-chunk skip.
         *
         * @param chunkKey skipped target chunk
         * @return updated result
         */
        private RestoreApplyResult missing(ChunkKey chunkKey) {
            List<ChunkKey> chunks = new ArrayList<>(missingChunks);
            if (!chunks.contains(chunkKey)) {
                chunks.add(chunkKey);
            }
            return new RestoreApplyResult(placed, List.copyOf(chunks), missingMarkers + 1, conflicts, failed);
        }

        /**
         * Adds one occupied-block conflict.
         *
         * @return updated result
         */
        private RestoreApplyResult conflict() {
            return new RestoreApplyResult(placed, missingChunks, missingMarkers, conflicts + 1, failed);
        }

        /**
         * Adds one generic placement failure.
         *
         * @return updated result
         */
        private RestoreApplyResult withFailed() {
            return new RestoreApplyResult(placed, missingChunks, missingMarkers, conflicts, failed + 1);
        }

        /**
         * Combines two restore-apply results.
         *
         * @param other result to add
         * @return combined result
         */
        private RestoreApplyResult plus(RestoreApplyResult other) {
            List<ChunkKey> chunks = new ArrayList<>(missingChunks);
            for (ChunkKey chunk : other.missingChunks()) {
                if (!chunks.contains(chunk)) {
                    chunks.add(chunk);
                }
            }
            return new RestoreApplyResult(
                    placed + other.placed(),
                    List.copyOf(chunks),
                    missingMarkers + other.missingMarkers(),
                    conflicts + other.conflicts(),
                    failed + other.failed()
            );
        }

        /**
         * Counts every skipped marker payload.
         *
         * @return total skipped markers
         */
        private int skipped() {
            return missingMarkers + conflicts + failed;
        }
    }
}
