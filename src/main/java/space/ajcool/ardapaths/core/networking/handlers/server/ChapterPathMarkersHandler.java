package space.ajcool.ardapaths.core.networking.handlers.server;

import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.PermissionHelper;
import space.ajcool.ardapaths.core.backup.BackupJobRunner;
import space.ajcool.ardapaths.core.backup.MarkerBatching;
import space.ajcool.ardapaths.core.consumers.networking.RespondablePacketHandler;
import space.ajcool.ardapaths.core.data.ChapterMarkerEntry;
import space.ajcool.ardapaths.core.data.ChapterMarkersStatus;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.integration.Warps;
import space.ajcool.ardapaths.core.markers.MarkerResolver;
import space.ajcool.ardapaths.core.markers.MarkerResolver.ResolvedMarker;
import space.ajcool.ardapaths.core.networking.packets.client.ChapterPathMarkersResponsePacket;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterPathMarkersPacket;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Handles server-backed marker list requests for the marker editor.
 */
@Slf4j(topic = "ardapaths")
public class ChapterPathMarkersHandler extends RespondablePacketHandler<ChapterPathMarkersPacket, ChapterPathMarkersResponsePacket> {

    /** Maximum number of chapter links followed by one list request. */
    private static final int MAX_HOPS = 500;

    /** Search radius around a configured chapter-start anchor. */
    private static final int START_SEARCH_RADIUS = 12;

    /** Search radius around a dangling chain end when looking for a detached continuation. */
    private static final int CHAIN_PROBE_RADIUS = 24;

    /** Maximum number of detached segments appended to one chapter chain. */
    private static final int MAX_EXTRA_SEGMENTS = 16;

    /**
     * Constructs the handler and its request and response channels.
     */
    public ChapterPathMarkersHandler() {
        super(ChapterPathMarkersPacket.CHANNEL, ChapterPathMarkersPacket::read, ChapterPathMarkersResponsePacket.CHANNEL, ChapterPathMarkersResponsePacket::read);
    }

    /**
     * Validates and resolves a full chapter marker list.
     *
     * @param server  the Minecraft server
     * @param player  the player who sent the request
     * @param handler the network handler
     * @param packet  the deserialized request packet
     * @param sender  the packet sender
     * @return future marker list response
     */
    @Override
    public CompletableFuture<ChapterPathMarkersResponsePacket> handleAsync(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, ChapterPathMarkersPacket packet, PacketSender sender) {
        if (!PermissionHelper.hasEditPermission(player)) {
            log.warn("Rejected unauthorized packet on {} from {}", getChannelId(), player.getStringUUID());
            return CompletableFuture.completedFuture(response(ChapterMarkersStatus.UNAUTHORIZED, List.of()));
        }

        PathData path = ArdaPaths.CONFIG.getPath(packet.pathId());
        if (path == null || path.getChapter(packet.chapterId()) == null) {
            return CompletableFuture.completedFuture(response(ChapterMarkersStatus.INVALID_DATA, List.of()));
        }

        return resolveAnchor(server, player, packet)
                .thenCompose(anchor -> searchOnWorker(server, packet, anchor));
    }

    /**
     * Resolves the configured chapter-start anchor using warp first, then coordinates.
     *
     * @param server server that owns the destination world
     * @param player player whose world provides coordinate fallback context
     * @param packet marker-list request
     * @return future optional anchor
     */
    @SuppressWarnings("resource")
    private CompletableFuture<Optional<Anchor>> resolveAnchor(MinecraftServer server, ServerPlayer player, ChapterPathMarkersPacket packet) {
        Optional<String> startWarp = ArdaPaths.CONFIG.getChapterStartWarp(packet.pathId(), packet.chapterId());
        ResourceKey<Level> fallbackWorldKey = player.serverLevel().dimension();
        if (startWarp.isPresent() && Warps.isAvailable()) {
            return Warps.resolveWarp(server, startWarp.get()).thenApply(warp -> warp
                    .map(location -> new Anchor(location.worldKey(), location.position()))
                    .or(() -> coordinateAnchor(fallbackWorldKey, packet)));
        }

        return CompletableFuture.completedFuture(coordinateAnchor(fallbackWorldKey, packet));
    }

    /**
     * Resolves the coordinate fallback anchor in the player's current world.
     *
     * @param worldKey world that owns coordinate chapter starts
     * @param packet marker-list request
     * @return optional coordinate anchor
     */
    private Optional<Anchor> coordinateAnchor(ResourceKey<Level> worldKey, ChapterPathMarkersPacket packet) {
        BlockPos start = ArdaPaths.CONFIG.getChapterStartCoordinates(packet.pathId(), packet.chapterId());
        if (start == null) return Optional.empty();
        return Optional.of(new Anchor(worldKey, start));
    }

    /**
     * Runs marker search and chain walking on the shared marker worker.
     *
     * @param server server that owns the world state
     * @param packet marker-list request
     * @param anchor optional configured chapter-start anchor
     * @return future marker list response
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private CompletableFuture<ChapterPathMarkersResponsePacket> searchOnWorker(MinecraftServer server, ChapterPathMarkersPacket packet, Optional<Anchor> anchor) {
        return BackupJobRunner.submitMarkerWork(server, gate -> search(packet, anchor, server, gate));
    }

    /**
     * Searches for the nearest chapter-start marker and builds the ordered response rows.
     *
     * @param packet marker-list request
     * @param anchor optional configured chapter-start anchor
     * @param server server that owns the world state
     * @param gate   gate for server-thread-only work
     * @return marker list response
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private ChapterPathMarkersResponsePacket search(ChapterPathMarkersPacket packet, Optional<Anchor> anchor, MinecraftServer server, BackupJobRunner.ServerGate gate) {
        if (anchor.isEmpty()) {
            log.warn("No chapter start anchor configured for {}:{}", packet.pathId(), packet.chapterId());
            return response(ChapterMarkersStatus.NO_CHAPTER_START, List.of());
        }

        ServerLevel world = gate.call(() -> server.getLevel(anchor.get().worldKey()));
        if (world == null) {
            return response(ChapterMarkersStatus.INVALID_DATA, List.of());
        }

        String dimensionId = world.dimension().location().toString();
        MarkerResolver resolver = new MarkerResolver(world, dimensionId);
        Optional<ResolvedMarker> start = findNearestChapterStart(resolver, anchor.get().position(), packet.pathId(), packet.chapterId(), gate);
        if (start.isEmpty()) {
            log.warn("No chapter start marker found near {} for {}:{}", anchor.get().position(), packet.pathId(), packet.chapterId());
            return response(ChapterMarkersStatus.NO_CHAPTER_START, List.of());
        }

        Set<Long> visited = new HashSet<>();
        ChainSegment segment = walkChain(gate, resolver, start.get(), packet.pathId(), packet.chapterId(), false, visited);
        List<ChapterMarkerEntry> chapterChain = new ArrayList<>(segment.rows());
        log.debug("Chapter {}:{} segment ended at {} with {} rows", packet.pathId(), packet.chapterId(), segment.danglingEnd(), segment.rows().size());

        for (int extra = 0; extra < MAX_EXTRA_SEGMENTS && segment.danglingEnd() != null; extra++) {
            Optional<ResolvedMarker> head = findDetachedChainHead(resolver, segment.danglingEnd(), packet.pathId(), packet.chapterId(), visited, gate);
            if (head.isEmpty()) {
                break;
            }

            segment = walkChain(gate, resolver, head.get(), packet.pathId(), packet.chapterId(), false, visited);
            if (segment.rows().isEmpty()) {
                break;
            }
            log.debug("Chapter {}:{} segment ended at {} with {} rows", packet.pathId(), packet.chapterId(), segment.danglingEnd(), segment.rows().size());

            chapterChain.add(ChapterMarkerEntry.breakEntry());
            chapterChain.addAll(segment.rows());
        }

        boolean currentInChapterChain = chapterChain.stream()
                .filter(entry -> !entry.chainBreak())
                .anyMatch(entry -> entry.packedPos() == packet.currentPackedPos());
        if (currentInChapterChain) {
            return response(ChapterMarkersStatus.OK, chapterChain);
        }

        Optional<ResolvedMarker> current = gate.call(() -> resolver.resolve(BlockPos.of(packet.currentPackedPos())));
        if (current.isEmpty()) {
            return response(ChapterMarkersStatus.INVALID_DATA, List.of());
        }

        List<ChapterMarkerEntry> rows = new ArrayList<>(chapterChain);
        rows.add(ChapterMarkerEntry.breakEntry());
        rows.addAll(walkChain(gate, resolver, current.get(), packet.pathId(), packet.chapterId(), true, new HashSet<>()).rows());
        return response(ChapterMarkersStatus.OK_WITH_BREAK, rows);
    }

    /**
     * Finds the nearest marker flagged as a chapter start within the configured radius.
     *
     * @param resolver marker resolver with per-request cache
     * @param anchor   configured chapter-start anchor
     * @param pathId   path identifier
     * @param chapterId chapter identifier
     * @param gate     gate for server-thread-only work
     * @return nearest chapter-start marker, or empty when absent
     */
    private Optional<ResolvedMarker> findNearestChapterStart(MarkerResolver resolver, BlockPos anchor, String pathId, String chapterId, BackupJobRunner.ServerGate gate) {
        return collectMarkersInCube(resolver, anchor, START_SEARCH_RADIUS, gate).stream()
                .filter(marker -> {
                    PathMarkerBlockEntity.ChapterNbtData data = gate.call(() -> marker.chapterData(pathId, chapterId));
                    return data != null && data.isChapterStart();
                })
                .min(Comparator
                        .comparingDouble((ResolvedMarker marker) -> marker.position().distSqr(anchor))
                        .thenComparingLong(marker -> marker.position().asLong()));
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
    private List<ResolvedMarker> collectMarkersInCube(MarkerResolver resolver, BlockPos centre, int radius, BackupJobRunner.ServerGate gate) {
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
     * Finds the nearest unvisited same-chapter marker that starts a detached chain.
     *
     * @param resolver  marker resolver with per-request cache
     * @param end       dangling end of the previously walked chain segment
     * @param pathId    path identifier
     * @param chapterId chapter identifier
     * @param visited   packed marker positions already listed in this request
     * @param gate      gate for server-thread-only work
     * @return nearest detached chain head, or empty when no candidate is found
     */
    private Optional<ResolvedMarker> findDetachedChainHead(MarkerResolver resolver, BlockPos end, String pathId, String chapterId, Set<Long> visited, BackupJobRunner.ServerGate gate) {
        List<ChapterMarkerCandidate> chapterMarkers = collectMarkersInCube(resolver, end, CHAIN_PROBE_RADIUS, gate).stream()
                .map(marker -> new ChapterMarkerCandidate(marker, gate.call(() -> marker.chapterData(pathId, chapterId))))
                .filter(candidate -> candidate.data() != null)
                .toList();
        Set<Long> targeted = new HashSet<>();

        for (ChapterMarkerCandidate candidate : chapterMarkers) {
            if (candidate.data().getTarget() != null) {
                targeted.add(candidate.marker().position().offset(candidate.data().getTarget()).asLong());
            }
        }

        long visitedCount = chapterMarkers.stream()
                .filter(candidate -> visited.contains(candidate.marker().position().asLong()))
                .count();
        long targetedCount = chapterMarkers.stream()
                .filter(candidate -> !visited.contains(candidate.marker().position().asLong()))
                .filter(candidate -> targeted.contains(candidate.marker().position().asLong()))
                .count();
        Optional<ResolvedMarker> head = chapterMarkers.stream()
                .map(ChapterMarkerCandidate::marker)
                .filter(marker -> !visited.contains(marker.position().asLong()))
                .filter(marker -> !targeted.contains(marker.position().asLong()))
                .min(Comparator
                        .comparingDouble((ResolvedMarker marker) -> marker.position().distSqr(end))
                        .thenComparingLong(marker -> marker.position().asLong()));
        log.debug(
                "Chapter {}:{} detached probe at {} found {} same-chapter markers, excluded {} visited and {} targeted, chose {}",
                pathId,
                chapterId,
                end,
                chapterMarkers.size(),
                visitedCount,
                targetedCount,
                head.map(ResolvedMarker::position).orElse(null)
        );
        return head;
    }

    /**
     * Reads marker candidates from a loaded or existing persisted chunk.
     *
     * @param resolver marker resolver with per-request cache
     * @param chunkPos chunk position to inspect
     * @param gate     gate for server-thread-only work
     * @return resolved markers present in the chunk
     */
    private List<ResolvedMarker> readChunkCandidates(MarkerResolver resolver, ChunkPos chunkPos, BackupJobRunner.ServerGate gate) {
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
    private boolean withinCube(BlockPos centre, BlockPos marker, int radius) {
        return Math.abs(centre.getX() - marker.getX()) <= radius
                && Math.abs(centre.getY() - marker.getY()) <= radius
                && Math.abs(centre.getZ() - marker.getZ()) <= radius;
    }

    /**
     * Walks a marker chain segment forward and converts each new marker to a response row.
     * A marker reached through an explicit link is treated as a terminal chapter row even
     * when its empty chapter data was not persisted.
     *
     * @param gate          gate for server-thread-only work
     * @param resolver      marker resolver with per-request cache
     * @param source        first marker in the chain
     * @param pathId        path identifier
     * @param chapterId     chapter identifier
     * @param includeSource whether to include the source even when it lacks chapter data
     * @param visited       packed marker positions already listed by this request
     * @return ordered marker rows and a recoverable dangling end, when present
     */
    private ChainSegment walkChain(BackupJobRunner.ServerGate gate, MarkerResolver resolver, ResolvedMarker source, String pathId, String chapterId, boolean includeSource, Set<Long> visited) {
        List<ChapterMarkerEntry> markers = new ArrayList<>();
        Set<Long> batchChunks = new HashSet<>();
        ResolvedMarker current = source;

        for (int hop = 0; hop <= MAX_HOPS; hop++) {
            long packed = current.position().asLong();
            if (!visited.add(packed)) {
                return new ChainSegment(markers, null);
            }

            ResolvedMarker currentMarker = current;
            PathMarkerBlockEntity.ChapterNbtData data = gate.call(() -> currentMarker.chapterData(pathId, chapterId));
            if (data == null) {
                if (hop == 0 && !includeSource) {
                    return new ChainSegment(markers, null);
                }

                markers.add(ChapterMarkerEntry.marker(packed, PathMarkerBlockEntity.ChapterNbtData.empty(chapterId)));
                return new ChainSegment(markers, current.position());
            }

            markers.add(ChapterMarkerEntry.marker(packed, data));
            if (data.getTarget() == null) {
                return new ChainSegment(markers, current.position());
            }

            BlockPos nextPos = current.position().offset(data.getTarget());
            Optional<ResolvedMarker> next = gate.call(() -> resolver.resolve(nextPos));
            if (next.isEmpty()) {
                return new ChainSegment(markers, current.position());
            }

            current = next.get();
            batchChunks.add(new ChunkPos(nextPos).toLong());
            if (batchChunks.size() >= MarkerBatching.CHUNKS_PER_BATCH) {
                batchChunks.clear();
                MarkerBatching.paceBetweenBatches(markers.size(), MAX_HOPS);
            }
        }

        return new ChainSegment(markers, null);
    }

    /**
     * Creates a chapter marker response packet.
     *
     * @param status  response status
     * @param markers response marker rows
     * @return response packet
     */
    private ChapterPathMarkersResponsePacket response(ChapterMarkersStatus status, List<ChapterMarkerEntry> markers) {
        return new ChapterPathMarkersResponsePacket(status, markers);
    }

    /**
     * Search anchor resolved from server chapter configuration.
     *
     * @param worldKey world containing the search anchor
     * @param position configured anchor position
     */
    private record Anchor(ResourceKey<Level> worldKey, BlockPos position) {
    }

    /**
     * Same-chapter marker discovered during a detached-chain probe.
     *
     * @param marker resolved marker block entity
     * @param data   chapter data for the requested path and chapter
     */
    private record ChapterMarkerCandidate(ResolvedMarker marker, PathMarkerBlockEntity.ChapterNbtData data) {
    }

    /**
     * Result of walking one marker chain segment.
     * Dangling ends are set for missing targets, unconfigured link-reached ends,
     * and dead target links, but not for cycles or maximum-hop truncation.
     *
     * @param rows        ordered marker rows produced by the walk
     * @param danglingEnd last marker of the walk when probing may continue, or null otherwise
     */
    private record ChainSegment(List<ChapterMarkerEntry> rows, BlockPos danglingEnd) {
    }
}
