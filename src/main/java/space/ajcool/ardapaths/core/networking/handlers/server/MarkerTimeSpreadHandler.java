package space.ajcool.ardapaths.core.networking.handlers.server;

import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.ChunkPos;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.PermissionHelper;
import space.ajcool.ardapaths.core.backup.BackupJobRunner;
import space.ajcool.ardapaths.core.backup.MarkerBatching;
import space.ajcool.ardapaths.core.consumers.networking.RespondablePacketHandler;
import space.ajcool.ardapaths.core.data.TimeOfDay;
import space.ajcool.ardapaths.core.data.TimeSpreadStatus;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.markers.MarkerResolver;
import space.ajcool.ardapaths.core.networking.packets.client.MarkerTimeSpreadResponsePacket;
import space.ajcool.ardapaths.core.networking.packets.server.MarkerTimeSpreadPacket;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Handles client requests to compute time-of-day progression across marker chains.
 */
@Slf4j(topic = "ardapaths")
public class MarkerTimeSpreadHandler extends RespondablePacketHandler<MarkerTimeSpreadPacket, MarkerTimeSpreadResponsePacket> {

    /**
     * Maximum number of trail links followed by one spread request.
     */
    private static final int MAX_HOPS = 500;

    /**
     * Maximum target time increment guideline, in wall-clock hours.
     */
    private static final int MAX_HOURS_PER_STEP = 2;

    /**
     * Number of Minecraft daytime ticks represented by one wall-clock hour.
     */
    private static final int TICKS_PER_HOUR = 1000;

    /**
     * Number of Minecraft daytime ticks in one full day.
     */
    private static final int DAY_TICKS = 24_000;

    /**
     * First valid Minecraft daytime tick.
     */
    private static final int MIN_DAYTIME_TICK = 0;

    /**
     * Last valid Minecraft daytime tick.
     */
    private static final int MAX_DAYTIME_TICK = 23_999;

    /**
     * Constructs the handler and its request and response channels.
     */
    public MarkerTimeSpreadHandler() {
        super(MarkerTimeSpreadPacket.TYPE, MarkerTimeSpreadPacket::read, MarkerTimeSpreadResponsePacket.TYPE, MarkerTimeSpreadResponsePacket::read);
    }

    /**
     * Validates, computes, and applies a marker time spread.
     *
     * @param server  the Minecraft server
     * @param player  the player who sent the request
     * @param handler the network handler
     * @param packet  the deserialized request packet
     * @param sender  the packet sender
     * @return status result for the client editor
     */
    @Override
    public CompletableFuture<MarkerTimeSpreadResponsePacket> handleAsync(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, MarkerTimeSpreadPacket packet, PacketSender sender) {
        if (!PermissionHelper.hasEditPermission(player)) {
            log.warn("Rejected unauthorized packet on {} from {}", getChannelId(), player.getStringUUID());
            return CompletableFuture.completedFuture(response(TimeSpreadStatus.UNAUTHORIZED));
        }

        PathData path = ArdaPaths.CONFIG.getPath(packet.pathId());
        if (path == null || path.getChapter(packet.chapterId()) == null ||
                packet.sourcePackedPos() == packet.targetPackedPos()) {
            return CompletableFuture.completedFuture(response(TimeSpreadStatus.INVALID_DATA));
        }

        if (!packet.clear() && (isInvalidTime(packet.sourceTime()) || isInvalidTime(packet.targetTime()))) {
            return CompletableFuture.completedFuture(response(TimeSpreadStatus.INVALID_DATA));
        }

        if (BackupJobRunner.isJobActive()) {
            return CompletableFuture.completedFuture(response(TimeSpreadStatus.BUSY));
        }

        return BackupJobRunner.submitMarkerWork(server, gate -> handleBatched(player, packet, gate));
    }

    /**
     * Creates a response packet with no updated markers or diagnostic position.
     *
     * @param status response status
     * @return response packet
     */
    private MarkerTimeSpreadResponsePacket response(TimeSpreadStatus status) {
        return new MarkerTimeSpreadResponsePacket(status, 0, null);
    }

    /**
     * Checks whether a daytime tick value can be used as an endpoint time.
     *
     * @param time endpoint time from the request
     * @return true when the time is unset or outside the valid day range
     */
    private boolean isInvalidTime(int time) {
        return time < MIN_DAYTIME_TICK || time > MAX_DAYTIME_TICK;
    }

    /**
     * Walks, computes, and applies a marker time spread with paced server-thread work.
     *
     * @param player player whose current world contains the marker chain
     * @param packet deserialized request packet
     * @param gate   gate for server-thread-only work
     * @return status result for the client editor
     */
    private MarkerTimeSpreadResponsePacket handleBatched(ServerPlayer player, MarkerTimeSpreadPacket packet, BackupJobRunner.ServerGate gate) {
        ServerLevel world = gate.call(player::serverLevel);
        String dimensionId = world.dimension().location().toString();
        MarkerResolver resolver = new MarkerResolver(world, dimensionId);
        BlockPos sourcePos = BlockPos.of(packet.sourcePackedPos());
        Optional<BlockPos> source = gate.call(() -> snapshot(resolver, sourcePos, packet.pathId(), packet.chapterId(), true));
        if (source.isEmpty()) {
            return response(TimeSpreadStatus.INVALID_DATA);
        }

        Optional<WalkResult> walkResult = walkChain(gate, resolver, source.get(), BlockPos.of(packet.targetPackedPos()), packet.pathId(), packet.chapterId());
        if (walkResult.isEmpty()) {
            return response(TimeSpreadStatus.INVALID_DATA);
        }

        WalkResult result = walkResult.get();
        if (result.status() != TimeSpreadStatus.OK) {
            return new MarkerTimeSpreadResponsePacket(result.status(), 0, result.lastValidPos());
        }

        SpreadPlan spreadPlan = gate.call(() -> packet.clear()
                ? computeClear(result.markers())
                : computeSpread(result.markers(), packet.sourceTime(), packet.targetTime()));
        ApplyResult applyResult = applySpread(gate, resolver, spreadPlan, packet.pathId(), packet.chapterId());
        return new MarkerTimeSpreadResponsePacket(applyResult.status(), applyResult.updatedCount(), null);
    }

    /**
     * Resolves a marker and snapshots only the data needed outside the current server-thread call.
     *
     * @param resolver       marker resolver
     * @param position       marker position
     * @param pathId         path identifier
     * @param chapterId      chapter identifier
     * @param requireChapter whether the selected chapter data must already exist
     * @return immutable marker position, or empty when the marker or chapter data is missing
     */
    private Optional<BlockPos> snapshot(MarkerResolver resolver, BlockPos position, String pathId, String chapterId, boolean requireChapter) {
        return resolver.resolve(position)
                .map(marker -> {
                    PathMarkerBlockEntity.ChapterNbtData data = marker.chapterData(pathId, chapterId);
                    if (data == null && requireChapter) {
                        return null;
                    }

                    return marker.position();
                });
    }

    /**
     * Walks the selected chapter chain from source to requested target.
     *
     * @param gate      gate for server-thread-only reads
     * @param resolver  request marker resolver
     * @param source    resolved source marker
     * @param targetPos requested target marker position
     * @param pathId    path identifier
     * @param chapterId chapter identifier
     * @return walk outcome, or empty when disk reads fail
     */
    private Optional<WalkResult> walkChain(BackupJobRunner.ServerGate gate, MarkerResolver resolver, BlockPos source, BlockPos targetPos, String pathId, String chapterId) {
        List<BlockPos> markers = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        Set<Long> batchChunks = new HashSet<>();
        BlockPos current = source;

        markers.add(current);
        visited.add(current.asLong());

        for (int hop = 0; hop < MAX_HOPS; hop++) {
            BlockPos currentPos = current;
            PathMarkerBlockEntity.ChapterNbtData data = gate.call(() -> chapterData(resolver, currentPos, pathId, chapterId));
            if (data == null || data.getTarget() == null) {
                return Optional.of(WalkResult.failed(TimeSpreadStatus.CHAIN_ENDED, current));
            }

            BlockPos nextPos = current.offset(data.getTarget());
            boolean isRequestedTarget = nextPos.equals(targetPos);
            Optional<BlockPos> next = gate.call(() -> snapshot(resolver, nextPos, pathId, chapterId, !isRequestedTarget));
            if (next.isEmpty()) {
                return Optional.of(WalkResult.failed(TimeSpreadStatus.CHAIN_BROKEN, current));
            }

            if (!visited.add(nextPos.asLong())) {
                return Optional.of(WalkResult.failed(TimeSpreadStatus.TARGET_TOO_FAR, current));
            }

            markers.add(next.get());
            if (isRequestedTarget) {
                return Optional.of(WalkResult.success(markers));
            }

            current = next.get();
            batchChunks.add(new ChunkPos(nextPos).toLong());
            if (batchChunks.size() >= MarkerBatching.CHUNKS_PER_BATCH) {
                batchChunks.clear();
                MarkerBatching.paceBetweenBatches(markers.size(), MAX_HOPS);
            }
        }

        return Optional.of(WalkResult.failed(TimeSpreadStatus.TARGET_TOO_FAR, current));
    }

    /**
     * Computes updates that remove marker time data from a resolved marker chain.
     *
     * @param markers complete marker chain from source to target
     * @return clear plan for every marker in the chain
     */
    private SpreadPlan computeClear(List<BlockPos> markers) {
        List<MarkerUpdate> updates = new ArrayList<>();

        for (BlockPos marker : markers) {
            updates.add(new MarkerUpdate(
                    marker,
                    TimeOfDay.UNSET,
                    TimeOfDay.DEFAULT_TRANSITION_RANGE
            ));
        }

        return new SpreadPlan(updates, false);
    }

    /**
     * Computes endpoint times and transition ranges for a resolved marker chain.
     *
     * @param markers    complete marker chain from source to target
     * @param sourceTime requested source endpoint time
     * @param targetTime requested target endpoint time
     * @return computed spread plan
     */
    private SpreadPlan computeSpread(List<BlockPos> markers, int sourceTime, int targetTime) {
        int gaps = markers.size() - 1;
        int delta = Math.floorMod(targetTime - sourceTime, DAY_TICKS);
        double[] distances = cumulativeDistances(markers);
        double totalDistance = distances[distances.length - 1];
        boolean stepExceeded = maxTimeStep(delta, distances, totalDistance, gaps) > MAX_HOURS_PER_STEP * TICKS_PER_HOUR;
        List<MarkerUpdate> updates = new ArrayList<>();

        for (int index = 0; index < markers.size(); index++) {
            double weight = totalDistance <= 0.0D ? index / (double) gaps : distances[index] / totalDistance;
            int time = TimeOfDay.snap(Math.floorMod(sourceTime + (int) Math.round(delta * weight), DAY_TICKS));
            updates.add(new MarkerUpdate(markers.get(index), time, TimeOfDay.COMPUTED_TRANSITION_RANGE));
        }

        return new SpreadPlan(updates, stepExceeded);
    }

    /**
     * Applies a spread plan to live markers in chunk-bounded server-thread batches.
     *
     * @param gate      gate for server-thread-only writes
     * @param resolver  request marker resolver
     * @param plan      computed spread plan
     * @param pathId    path identifier
     * @param chapterId chapter identifier
     * @return final apply result
     */
    private ApplyResult applySpread(BackupJobRunner.ServerGate gate, MarkerResolver resolver, SpreadPlan plan, String pathId, String chapterId) {
        List<MarkerUpdate> updates = plan.markers();
        int applied = 0;

        for (int batchStart = 0; batchStart < updates.size(); ) {
            int toIndex = MarkerBatching.findChunkBoundedBatchEnd(
                    updates,
                    batchStart,
                    ignored -> "",
                    update -> new ChunkPos(update.position()).toLong()
            );
            int fromIndex = batchStart;
            applied += gate.call(() -> applySpreadBatch(resolver, updates.subList(fromIndex, toIndex), pathId, chapterId));
            batchStart = toIndex;
            MarkerBatching.paceBetweenBatches(batchStart, updates.size());
        }

        return new ApplyResult(plan.stepExceeded() ? TimeSpreadStatus.OK_STEP_EXCEEDED : TimeSpreadStatus.OK, applied);
    }

    /**
     * Reads current chapter data from a freshly resolved marker.
     *
     * @param resolver  marker resolver
     * @param position  marker position
     * @param pathId    path identifier
     * @param chapterId chapter identifier
     * @return chapter data, or null when the marker no longer resolves
     */
    private PathMarkerBlockEntity.ChapterNbtData chapterData(MarkerResolver resolver, BlockPos position, String pathId, String chapterId) {
        return resolver.resolve(position)
                .map(marker -> marker.chapterData(pathId, chapterId))
                .orElse(null);
    }

    /**
     * Computes cumulative distances along a marker chain.
     *
     * @param markers complete marker chain from source to target
     * @return cumulative block distances at each marker index
     */
    private double[] cumulativeDistances(List<BlockPos> markers) {
        double[] distances = new double[markers.size()];

        for (int index = 1; index < markers.size(); index++) {
            distances[index] = distances[index - 1] + Math.sqrt(markers.get(index - 1).distSqr(markers.get(index)));
        }

        return distances;
    }

    /**
     * Computes the largest authored time step in the spread plan.
     *
     * @param delta         forward time delta from source to target
     * @param distances     cumulative block distances at each marker index
     * @param totalDistance total chain distance
     * @param gaps          number of marker gaps
     * @return largest time increment between consecutive markers
     */
    private double maxTimeStep(int delta, double[] distances, double totalDistance, int gaps) {
        if (totalDistance <= 0.0D) {
            return delta / (double) gaps;
        }

        double maxStep = 0.0D;
        for (int index = 1; index < distances.length; index++) {
            double gapDistance = distances[index] - distances[index - 1];
            maxStep = Math.max(maxStep, delta * gapDistance / totalDistance);
        }

        return maxStep;
    }

    /**
     * Applies one spread update batch on the server thread.
     *
     * @param resolver  request marker resolver
     * @param updates   marker updates in the current batch
     * @param pathId    path identifier
     * @param chapterId chapter identifier
     * @return number of markers successfully updated
     */
    private int applySpreadBatch(MarkerResolver resolver, List<MarkerUpdate> updates, String pathId, String chapterId) {
        int applied = 0;

        for (MarkerUpdate update : updates) {
            Optional<MarkerResolver.ResolvedMarker> marker = resolver.resolve(update.position());
            if (marker.isEmpty()) {
                continue;
            }

            marker.get().apply(update.timeOfDay(), update.timeTransitionRange(), pathId, chapterId);
            applied++;
        }

        return applied;
    }

    /**
     * Creates an error response when asynchronous marker work fails before producing a normal response.
     *
     * @return invalid-data response packet
     */
    @Override
    protected MarkerTimeSpreadResponsePacket errorResponse() {
        return response(TimeSpreadStatus.INVALID_DATA);
    }

    /**
     * Result of walking the marker chain.
     *
     * @param status       status code for the walk
     * @param markers      resolved markers in chain order
     * @param lastValidPos last valid marker for diagnostics
     */
    private record WalkResult(TimeSpreadStatus status, List<BlockPos> markers, BlockPos lastValidPos) {

        /**
         * Creates a successful walk result.
         *
         * @param markers resolved markers in chain order
         * @return successful walk result
         */
        private static WalkResult success(List<BlockPos> markers) {
            return new WalkResult(TimeSpreadStatus.OK, markers, null);
        }

        /**
         * Creates a failed walk result.
         *
         * @param status       failure status
         * @param lastValidPos last valid marker for diagnostics
         * @return failed walk result
         */
        private static WalkResult failed(TimeSpreadStatus status, BlockPos lastValidPos) {
            return new WalkResult(status, List.of(), lastValidPos);
        }
    }

    /**
     * Computed marker updates and warning state.
     *
     * @param markers      marker updates in chain order
     * @param stepExceeded whether the max step guideline was exceeded
     */
    private record SpreadPlan(List<MarkerUpdate> markers, boolean stepExceeded) {

    }

    /**
     * One computed marker update.
     *
     * @param position            marker position to update
     * @param timeOfDay           computed marker time
     * @param timeTransitionRange computed transition range
     */
    private record MarkerUpdate(BlockPos position, int timeOfDay, int timeTransitionRange) {

    }

    /**
     * Result of applying computed marker updates.
     *
     * @param status       status to send to the client
     * @param updatedCount number of markers successfully updated
     */
    private record ApplyResult(TimeSpreadStatus status, int updatedCount) {

    }

}
