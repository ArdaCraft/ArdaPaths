package space.ajcool.ardapaths.core.markers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.backup.BackupJobRunner;
import space.ajcool.ardapaths.core.backup.MarkerBatching;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.integration.Warps;
import space.ajcool.ardapaths.core.markers.MarkerResolver.ResolvedMarker;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Shared server-side helpers for resolving chapter starts and nearby chapter marker candidates.
 */
public final class ChapterStartLocator {

    /** Search radius around a configured chapter-start anchor. */
    public static final int START_SEARCH_RADIUS = 12;

    /**
     * Hidden constructor for static utility methods.
     */
    private ChapterStartLocator() {
    }

    /**
     * Resolves the configured chapter-start anchor using coordinates before optional warp services.
     *
     * @param server    server that owns destination worlds
     * @param pathId    path identifier
     * @param chapterId chapter identifier
     * @return future anchor result
     */
    public static CompletableFuture<AnchorResult> resolveAnchor(MinecraftServer server, String pathId, String chapterId) {
        Optional<Anchor> coordinateAnchor = coordinateAnchor(pathId, chapterId);
        if (coordinateAnchor.isPresent()) {
            return CompletableFuture.completedFuture(new AnchorResult(coordinateAnchor, true, false));
        }

        Optional<String> startWarp = ArdaPaths.CONFIG.getChapterStartWarp(pathId, chapterId);
        if (startWarp.isEmpty()) {
            return CompletableFuture.completedFuture(new AnchorResult(Optional.empty(), false, false));
        }

        if (!Warps.isAvailable()) {
            return CompletableFuture.completedFuture(new AnchorResult(Optional.empty(), false, true));
        }

        return Warps.resolveWarp(server, startWarp.get()).thenApply(warp -> warp
                .map(location -> new AnchorResult(Optional.of(new Anchor(location.worldKey(), location.position())), false, false))
                .orElseGet(() -> new AnchorResult(Optional.empty(), false, true)));
    }

    /**
     * Resolves the coordinate chapter-start anchor from server config.
     *
     * @param pathId    path identifier
     * @param chapterId chapter identifier
     * @return configured coordinate anchor, or empty
     */
    private static Optional<Anchor> coordinateAnchor(String pathId, String chapterId) {
        BlockPos start = ArdaPaths.CONFIG.getChapterStartCoordinates(pathId, chapterId);
        if (start == null) return Optional.empty();

        String dimensionId = ArdaPaths.CONFIG.getChapterStartDimension(pathId, chapterId);
        if (dimensionId == null) return Optional.empty();

        ResourceKey<Level> worldKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimensionId));
        return Optional.of(new Anchor(worldKey, start));
    }

    /**
     * Finds the nearest marker flagged as a chapter start within the default chapter-start radius.
     *
     * @param resolver  marker resolver with per-request cache
     * @param anchor    configured chapter-start anchor
     * @param pathId    path identifier
     * @param chapterId chapter identifier
     * @param gate      gate for server-thread-only work
     * @return nearest chapter-start marker, or empty when absent
     */
    public static Optional<ResolvedMarker> findNearestChapterStart(MarkerResolver resolver, BlockPos anchor, String pathId, String chapterId, BackupJobRunner.ServerGate gate) {
        return collectMarkersInCube(resolver, anchor, START_SEARCH_RADIUS, gate).stream()
                .filter(marker -> {
                    PathMarkerBlockEntity.ChapterNbtData data = gate.call(() -> marker.chapterData(pathId, chapterId));
                    return data != null && data.isChapterStart();
                })
                .min(nearestTo(anchor));
    }

    /**
     * Finds the nearest marker with any data for the requested path chapter.
     *
     * @param resolver  marker resolver with per-request cache
     * @param anchor    configured chapter-start anchor
     * @param pathId    path identifier
     * @param chapterId chapter identifier
     * @param gate      gate for server-thread-only work
     * @return nearest marker with chapter data, or empty when absent
     */
    public static Optional<ResolvedMarker> findNearestChapterMarker(MarkerResolver resolver, BlockPos anchor, String pathId, String chapterId, BackupJobRunner.ServerGate gate) {
        return collectMarkersInCube(resolver, anchor, START_SEARCH_RADIUS, gate).stream()
                .filter(marker -> gate.call(() -> marker.chapterData(pathId, chapterId)) != null)
                .min(nearestTo(anchor));
    }

    /**
     * Collects all marker candidates inside a cube while scanning the intersecting chunks.
     *
     * @param resolver marker resolver with per-request cache
     * @param centre   centre of the search cube
     * @param radius   inclusive block radius on each axis
     * @param gate     gate for server-thread-only work
     * @return resolved markers inside the search cube
     */
    public static List<ResolvedMarker> collectMarkersInCube(MarkerResolver resolver, BlockPos centre, int radius, BackupJobRunner.ServerGate gate) {
        ChunkPos minChunk = new ChunkPos(centre.offset(-radius, 0, -radius));
        ChunkPos maxChunk = new ChunkPos(centre.offset(radius, 0, radius));
        List<ResolvedMarker> markers = new ArrayList<>();
        int inspectedChunks = 0;

        for (int chunkX = minChunk.x; chunkX <= maxChunk.x; chunkX++) {
            for (int chunkZ = minChunk.z; chunkZ <= maxChunk.z; chunkZ++) {
                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                for (ResolvedMarker marker : readChunkCandidates(resolver, chunkPos, gate)) {
                    if (withinCube(centre, marker.position(), radius)) {
                        markers.add(marker);
                    }
                }

                inspectedChunks++;
                if (inspectedChunks % MarkerBatching.CHUNKS_PER_BATCH == 0) {
                    MarkerBatching.paceBetweenBatches(inspectedChunks, Integer.MAX_VALUE);
                }
            }
        }

        return markers;
    }

    /**
     * Creates a marker resolver for an anchor world.
     *
     * @param server active server
     * @param anchor chapter-start anchor
     * @param gate   gate for server-thread-only world reads
     * @return marker resolver, or null when the world cannot be resolved
     */
    public static @Nullable MarkerResolver resolverFor(MinecraftServer server, Anchor anchor, BackupJobRunner.ServerGate gate) {
        ServerLevel world = gate.call(() -> server.getLevel(anchor.worldKey()));
        if (world == null) return null;

        String dimensionId = world.dimension().location().toString();
        return new MarkerResolver(world, dimensionId);
    }

    /**
     * Locates a marker by position across preferred dimensions first, then every other loaded server level.
     *
     * @param server              active server
     * @param pos                 marker block position
     * @param preferredDimensions dimension identifiers to try before all others
     * @param gate                gate for server-thread-only marker resolution
     * @return resolved marker and its owning dimension, or empty when absent
     */
    public static Optional<ResolvedMarker> locateMarker(MinecraftServer server, BlockPos pos, List<String> preferredDimensions, BackupJobRunner.ServerGate gate) {
        Set<String> tried = new HashSet<>();
        for (String dimensionId : preferredDimensions) {
            if (dimensionId == null || dimensionId.isBlank() || !tried.add(dimensionId)) continue;

            Optional<ResolvedMarker> marker = locateMarkerInDimension(server, pos, dimensionId, gate);
            if (marker.isPresent()) return marker;
        }

        List<ServerLevel> levels = gate.call(() -> {
            List<ServerLevel> snapshot = new ArrayList<>();
            server.getAllLevels().forEach(snapshot::add);
            return snapshot;
        });
        for (ServerLevel level : levels) {
            String dimensionId = level.dimension().location().toString();
            if (!tried.add(dimensionId)) continue;

            Optional<ResolvedMarker> marker = locateMarkerInLevel(level, pos, dimensionId, gate);
            if (marker.isPresent()) return marker;
        }

        return Optional.empty();
    }

    /**
     * Locates a marker by position in one configured dimension.
     *
     * @param server      active server
     * @param pos         marker block position
     * @param dimensionId dimension identifier
     * @param gate        gate for server-thread-only marker resolution
     * @return resolved marker, or empty
     */
    private static Optional<ResolvedMarker> locateMarkerInDimension(MinecraftServer server, BlockPos pos, String dimensionId, BackupJobRunner.ServerGate gate) {
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimensionId));
        ServerLevel level = gate.call(() -> server.getLevel(key));
        if (level == null) return Optional.empty();

        return locateMarkerInLevel(level, pos, dimensionId, gate);
    }

    /**
     * Locates a marker by position in one server level.
     *
     * @param level       server level to search
     * @param pos         marker block position
     * @param dimensionId dimension identifier
     * @param gate        gate for server-thread-only marker resolution
     * @return resolved marker, or empty
     */
    private static Optional<ResolvedMarker> locateMarkerInLevel(ServerLevel level, BlockPos pos, String dimensionId, BackupJobRunner.ServerGate gate) {
        MarkerResolver resolver = new MarkerResolver(level, dimensionId);
        return gate.call(() -> resolver.resolve(pos));
    }

    /**
     * Reads marker candidates from a loaded or existing persisted chunk.
     *
     * @param resolver marker resolver with per-request cache
     * @param chunkPos chunk position to inspect
     * @param gate     gate for server-thread-only work
     * @return resolved markers present in the chunk
     */
    private static List<ResolvedMarker> readChunkCandidates(MarkerResolver resolver, ChunkPos chunkPos, BackupJobRunner.ServerGate gate) {
        return gate.call(() -> resolver.resolveChunkMarkers(chunkPos));
    }

    /**
     * Checks whether a marker is inside an inclusive cubic search radius.
     *
     * @param centre centre of the search cube
     * @param marker marker position to test
     * @param radius inclusive block radius on each axis
     * @return true when the marker is inside the search cube
     */
    private static boolean withinCube(BlockPos centre, BlockPos marker, int radius) {
        return Math.abs(centre.getX() - marker.getX()) <= radius
                && Math.abs(centre.getY() - marker.getY()) <= radius
                && Math.abs(centre.getZ() - marker.getZ()) <= radius;
    }

    /**
     * Creates the deterministic nearest-marker comparator for one anchor.
     *
     * @param anchor position to compare distances against
     * @return comparator ordered by distance then packed position
     */
    private static Comparator<ResolvedMarker> nearestTo(BlockPos anchor) {
        return Comparator
                .comparingDouble((ResolvedMarker marker) -> marker.position().distSqr(anchor))
                .thenComparingLong(marker -> marker.position().asLong());
    }

    /**
     * Returns whether the config currently has a chapter to update.
     *
     * @param pathId    path identifier
     * @param chapterId chapter identifier
     * @return true when both path and chapter exist
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean chapterExists(String pathId, String chapterId) {
        PathData path = ArdaPaths.CONFIG.getPath(pathId);
        ChapterData chapter = path == null ? null : path.getChapter(chapterId);
        return chapter != null;
    }

    /**
     * Search anchor resolved from server chapter configuration.
     *
     * @param worldKey world containing the search anchor
     * @param position configured anchor position
     */
    public record Anchor(ResourceKey<Level> worldKey, BlockPos position) {
    }

    /**
     * Result of resolving a chapter-start anchor.
     *
     * @param anchor             resolved anchor, when available
     * @param fromCoordinates    whether the anchor came from configured coordinates
     * @param warpUnresolvable   whether a configured warp could not be resolved
     */
    public record AnchorResult(Optional<Anchor> anchor, boolean fromCoordinates, boolean warpUnresolvable) {
    }
}
