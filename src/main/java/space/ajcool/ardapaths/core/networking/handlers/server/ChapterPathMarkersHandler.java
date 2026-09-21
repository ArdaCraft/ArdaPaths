package space.ajcool.ardapaths.core.networking.handlers.server;

import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
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
import space.ajcool.ardapaths.core.data.config.shared.PositionData;
import space.ajcool.ardapaths.core.markers.ChapterStartLocator;
import space.ajcool.ardapaths.core.markers.ChapterStartLocator.AnchorResult;
import space.ajcool.ardapaths.core.markers.MarkerResolver;
import space.ajcool.ardapaths.core.markers.MarkerResolver.ResolvedMarker;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
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

    /** Search radius around a dangling chain end when looking for a detached continuation. */
    private static final int CHAIN_PROBE_RADIUS = 24;

    /** Maximum number of detached segments appended to one chapter chain. */
    private static final int MAX_EXTRA_SEGMENTS = 16;

    /** Maximum number of cross-dimension marker links followed by one list request. */
    private static final int MAX_DIMENSION_JUMPS = 8;

    /**
     * Constructs the handler and its request and response channels.
     */
    public ChapterPathMarkersHandler() {
        super(ChapterPathMarkersPacket.TYPE, ChapterPathMarkersPacket::read, ChapterPathMarkersResponsePacket.TYPE, ChapterPathMarkersResponsePacket::read);
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
    @SuppressWarnings("resource")
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

        String playerDimensionId = player.level().dimension().identifier().toString();
        return ChapterStartLocator.resolveAnchor(server, packet.pathId(), packet.chapterId())
                .thenCompose(anchor -> searchOnWorker(server, packet, anchor, playerDimensionId));
    }

    /**
     * Runs marker search and chain walking on the shared marker worker.
     *
     * @param server server that owns the world state
     * @param packet marker-list request
     * @param anchorResult      resolved anchor state
     * @param playerDimensionId dimension containing the marker currently edited by the player
     * @return future marker list response
     */
    private CompletableFuture<ChapterPathMarkersResponsePacket> searchOnWorker(MinecraftServer server, ChapterPathMarkersPacket packet,
                                                                               AnchorResult anchorResult, String playerDimensionId) {
        return BackupJobRunner.submitMarkerWork(server, gate -> search(packet, anchorResult, server, playerDimensionId, gate));
    }

    /**
     * Searches for the nearest chapter-start marker and builds the ordered response rows.
     *
     * @param packet marker-list request
     * @param anchorResult resolved anchor state
     * @param server            server that owns the world state
     * @param playerDimensionId dimension containing the marker currently edited by the player
     * @param gate              gate for server-thread-only work
     * @return marker list response
     */
    private ChapterPathMarkersResponsePacket search(ChapterPathMarkersPacket packet, AnchorResult anchorResult, MinecraftServer server,
                                                    String playerDimensionId, BackupJobRunner.ServerGate gate) {
        if (anchorResult.warpUnresolvable()) {
            log.warn("Chapter start warp is unresolvable for {}:{}", packet.pathId(), packet.chapterId());
            return response(ChapterMarkersStatus.UNRESOLVABLE_CHAPTER_START, List.of());
        }

        if (anchorResult.anchor().isEmpty()) {
            log.warn("No chapter start anchor configured for {}:{}", packet.pathId(), packet.chapterId());
            return response(ChapterMarkersStatus.NO_CHAPTER_START, List.of());
        }

        ChapterStartLocator.Anchor anchor = anchorResult.anchor().get();
        MarkerResolver resolver = ChapterStartLocator.resolverFor(server, anchor, gate);
        if (resolver == null) {
            return response(ChapterMarkersStatus.INVALID_DATA, List.of());
        }

        Optional<ResolvedMarker> start = ChapterStartLocator.findNearestChapterStart(resolver, anchor.position(), packet.pathId(), packet.chapterId(), gate);
        if (start.isEmpty()) {
            log.warn("No chapter start marker found near {} for {}:{}", anchor.position(), packet.pathId(), packet.chapterId());
            return response(ChapterMarkersStatus.NO_CHAPTER_START, List.of());
        }
        selfHealWarpResolvedStart(server, packet, anchorResult, start.get(), gate);

        Set<VisitKey> visited = new HashSet<>();
        ChainSegment segment;
        List<ChapterMarkerEntry> chapterChain = new ArrayList<>();
        ResolvedMarker head = start.get();
        boolean includeSource = false;
        int dimensionJumps = 0;

        while (true) {
            segment = walkChain(gate, resolver, head, packet.pathId(), packet.chapterId(), includeSource, visited);
            chapterChain.addAll(segment.rows());
            log.debug("Chapter {}:{} segment ended at {} with {} rows", packet.pathId(), packet.chapterId(), segment.danglingEnd(), segment.rows().size());

            for (int extra = 0; extra < MAX_EXTRA_SEGMENTS && segment.danglingEnd() != null && segment.dimensionJump() == null; extra++) {
                Optional<ResolvedMarker> detachedHead = findDetachedChainHead(resolver, segment.danglingEnd(), packet.pathId(), packet.chapterId(), visited, gate);
                if (detachedHead.isEmpty()) {
                    break;
                }

                segment = walkChain(gate, resolver, detachedHead.get(), packet.pathId(), packet.chapterId(), false, visited);
                if (segment.rows().isEmpty()) {
                    break;
                }
                log.debug("Chapter {}:{} segment ended at {} with {} rows", packet.pathId(), packet.chapterId(), segment.danglingEnd(), segment.rows().size());

                chapterChain.add(ChapterMarkerEntry.breakEntry());
                chapterChain.addAll(segment.rows());
            }

            if (segment.dimensionJump() == null) {
                break;
            }

            DimensionJump jump = segment.dimensionJump();
            chapterChain.add(ChapterMarkerEntry.dimensionBreakEntry(jump.dimensionId()));
            if (dimensionJumps++ >= MAX_DIMENSION_JUMPS) {
                log.warn("Chapter {}:{} stopped after too many dimension jumps at {} {}", packet.pathId(), packet.chapterId(), jump.dimensionId(), jump.pos());
                break;
            }

            MarkerResolver jumpResolver = resolverForDimension(server, jump.dimensionId(), jump.pos(), gate);
            if (jumpResolver == null) {
                log.warn("Chapter {}:{} target marker dimension {} is unavailable", packet.pathId(), packet.chapterId(), jump.dimensionId());
                break;
            }

            Optional<ResolvedMarker> exactTarget = gate.call(() -> jumpResolver.resolve(jump.pos()));
            Optional<ResolvedMarker> target = exactTarget.isPresent()
                    ? exactTarget
                    : ChapterStartLocator.findNearestChapterMarker(jumpResolver, jump.pos(), packet.pathId(), packet.chapterId(), gate);
            if (target.isEmpty()) {
                log.warn("Chapter {}:{} target marker {} not found in {}", packet.pathId(), packet.chapterId(), jump.pos(), jump.dimensionId());
                break;
            }

            resolver = jumpResolver;
            head = target.get();
            includeSource = true;
        }

        boolean currentInChapterChain = chapterChain.stream()
                .filter(entry -> !entry.chainBreak())
                .anyMatch(entry -> entry.packedPos() == packet.currentPackedPos() && Objects.equals(entry.dimensionId(), playerDimensionId));
        if (currentInChapterChain) {
            return response(ChapterMarkersStatus.OK, chapterChain);
        }

        MarkerResolver playerResolver = resolverForDimension(server, playerDimensionId, BlockPos.of(packet.currentPackedPos()), gate);
        if (playerResolver == null) {
            return response(ChapterMarkersStatus.INVALID_DATA, List.of());
        }

        Optional<ResolvedMarker> current = gate.call(() -> playerResolver.resolve(BlockPos.of(packet.currentPackedPos())));
        if (current.isEmpty()) {
            return response(ChapterMarkersStatus.INVALID_DATA, List.of());
        }

        List<ChapterMarkerEntry> rows = new ArrayList<>(chapterChain);
        rows.add(ChapterMarkerEntry.breakEntry());
        rows.addAll(walkChain(gate, playerResolver, current.get(), packet.pathId(), packet.chapterId(), true, new HashSet<>()).rows());
        return response(ChapterMarkersStatus.OK_WITH_BREAK, rows);
    }

    /**
     * Finds the nearest unvisited same-chapter marker that starts a detached chain.
     *
     * @param resolver  marker resolver with per-request cache
     * @param end       dangling end of the previously walked chain segment
     * @param pathId    path identifier
     * @param chapterId chapter identifier
     * @param visited   marker positions already listed in this request, keyed by dimension
     * @param gate      gate for server-thread-only work
     * @return nearest detached chain head, or empty when no candidate is found
     */
    private Optional<ResolvedMarker> findDetachedChainHead(MarkerResolver resolver, BlockPos end, String pathId, String chapterId, Set<VisitKey> visited, BackupJobRunner.ServerGate gate) {
        List<ChapterMarkerCandidate> chapterMarkers = ChapterStartLocator.collectMarkersInCube(resolver, end, CHAIN_PROBE_RADIUS, gate).stream()
                .map(marker -> new ChapterMarkerCandidate(marker, gate.call(() -> marker.chapterData(pathId, chapterId))))
                .filter(candidate -> candidate.data() != null)
                .toList();
        Set<VisitKey> targeted = new HashSet<>();

        for (ChapterMarkerCandidate candidate : chapterMarkers) {
            if (candidate.data().getTarget() != null) {
                targeted.add(VisitKey.from(candidate.marker().dimensionId(), candidate.marker().position().offset(candidate.data().getTarget())));
            }
        }

        long visitedCount = chapterMarkers.stream()
                .filter(candidate -> visited.contains(VisitKey.from(candidate.marker())))
                .count();
        long targetedCount = chapterMarkers.stream()
                .filter(candidate -> !visited.contains(VisitKey.from(candidate.marker())))
                .filter(candidate -> targeted.contains(VisitKey.from(candidate.marker())))
                .count();
        Optional<ResolvedMarker> head = chapterMarkers.stream()
                .map(ChapterMarkerCandidate::marker)
                .filter(marker -> !visited.contains(VisitKey.from(marker)))
                .filter(marker -> !targeted.contains(VisitKey.from(marker)))
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
     * @param visited       marker positions already listed by this request, keyed by dimension
     * @return ordered marker rows and a recoverable dangling end, when present
     */
    private ChainSegment walkChain(BackupJobRunner.ServerGate gate, MarkerResolver resolver, ResolvedMarker source, String pathId,
                                   String chapterId, boolean includeSource, Set<VisitKey> visited) {
        List<ChapterMarkerEntry> markers = new ArrayList<>();
        Set<Long> batchChunks = new HashSet<>();
        ResolvedMarker current = source;

        for (int hop = 0; hop <= MAX_HOPS; hop++) {
            long packed = current.position().asLong();
            if (!visited.add(VisitKey.from(current))) {
                return new ChainSegment(markers, null, null);
            }

            ResolvedMarker currentMarker = current;
            PathMarkerBlockEntity.ChapterNbtData data = gate.call(() -> currentMarker.chapterData(pathId, chapterId));
            if (data == null) {
                if (hop == 0 && !includeSource) {
                    return new ChainSegment(markers, null, null);
                }

                markers.add(ChapterMarkerEntry.marker(packed, current.dimensionId(), PathMarkerBlockEntity.ChapterNbtData.empty(chapterId)));
                return new ChainSegment(markers, current.position(), null);
            }

            markers.add(ChapterMarkerEntry.marker(packed, current.dimensionId(), data));
            if (data.hasTargetMarker()) {
                return new ChainSegment(markers, null, new DimensionJump(data.getTargetMarkerDimension(), data.getTargetMarker()));
            }

            if (data.getTarget() == null) {
                return new ChainSegment(markers, current.position(), null);
            }

            BlockPos nextPos = current.position().offset(data.getTarget());
            Optional<ResolvedMarker> next = gate.call(() -> resolver.resolve(nextPos));
            if (next.isEmpty()) {
                return new ChainSegment(markers, current.position(), null);
            }

            current = next.get();
            batchChunks.add(ChunkPos.pack(nextPos));
            if (batchChunks.size() >= MarkerBatching.CHUNKS_PER_BATCH) {
                batchChunks.clear();
                MarkerBatching.paceBetweenBatches(markers.size(), MAX_HOPS);
            }
        }

        return new ChainSegment(markers, null, null);
    }

    /**
     * Creates a marker resolver for a dimension identifier.
     *
     * @param server      server that owns the world state
     * @param dimensionId dimension identifier to resolve
     * @param anchor      position used only to build the resolver anchor
     * @param gate        gate for server-thread-only work
     * @return marker resolver, or null when the dimension is unavailable
     */
    private MarkerResolver resolverForDimension(MinecraftServer server, String dimensionId, BlockPos anchor, BackupJobRunner.ServerGate gate) {
        Identifier id = Identifier.tryParse(dimensionId);
        if (id == null) {
            return null;
        }
        ResourceKey<Level> worldKey = ResourceKey.create(Registries.DIMENSION, id);
        return ChapterStartLocator.resolverFor(server, new ChapterStartLocator.Anchor(worldKey, anchor), gate);
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
     * Persists a warp-resolved chapter start so future requests use coordinates first.
     *
     * @param server       server whose clients should receive config sync
     * @param packet       marker-list request
     * @param anchorResult resolved anchor state
     * @param start        nearest chapter start marker
     * @param gate         gate for server-thread-only config writes
     */
    private void selfHealWarpResolvedStart(MinecraftServer server, ChapterPathMarkersPacket packet, AnchorResult anchorResult, ResolvedMarker start, BackupJobRunner.ServerGate gate) {
        if (anchorResult.fromCoordinates()) return;

        gate.run(() -> {
            ArdaPaths.CONFIG.setChapterStart(packet.pathId(), packet.chapterId(), PositionData.fromBlockPos(start.position()), start.dimensionId());
            ArdaPaths.CONFIG_MANAGER.save();
            PacketRegistry.syncPathDataToClients(server);
        });
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
     * Marker visit identity scoped to a dimension.
     *
     * @param dimensionId dimension identifier containing the marker
     * @param packedPos   packed marker block position
     */
    private record VisitKey(String dimensionId, long packedPos) {

        /**
         * Creates a visit key from a resolved marker.
         *
         * @param marker resolved marker to key
         * @return marker visit key
         */
        private static VisitKey from(ResolvedMarker marker) {
            return from(marker.dimensionId(), marker.position());
        }

        /**
         * Creates a visit key from a dimension and position.
         *
         * @param dimensionId dimension identifier
         * @param pos         marker block position
         * @return marker visit key
         */
        private static VisitKey from(String dimensionId, BlockPos pos) {
            return new VisitKey(dimensionId, pos.asLong());
        }
    }

    /**
     * Cross-dimension continuation target discovered while walking a marker chain.
     *
     * @param dimensionId destination dimension identifier
     * @param pos         absolute destination marker position
     */
    private record DimensionJump(String dimensionId, BlockPos pos) {

    }

    /**
     * Result of walking one marker chain segment.
     * Dangling ends are set for missing targets, unconfigured link-reached ends,
     * and dead target links, but not for cycles or maximum-hop truncation.
     *
     * @param rows          ordered marker rows produced by the walk
     * @param danglingEnd   last marker of the walk when probing may continue, or null otherwise
     * @param dimensionJump cross-dimension continuation target, or null when the walk stayed local
     */
    private record ChainSegment(List<ChapterMarkerEntry> rows, BlockPos danglingEnd, DimensionJump dimensionJump) {

    }
}
