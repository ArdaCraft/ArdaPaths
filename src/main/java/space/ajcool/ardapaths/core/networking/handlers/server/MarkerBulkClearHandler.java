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
import space.ajcool.ardapaths.core.data.TimeSpreadStatus;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.markers.MarkerResolver;
import space.ajcool.ardapaths.core.markers.MarkerResolver.ResolvedMarker;
import space.ajcool.ardapaths.core.networking.packets.client.MarkerBulkClearResponsePacket;
import space.ajcool.ardapaths.core.networking.packets.server.MarkerBulkClearPacket;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Handles client requests to clear time and weather data from an arbitrary marker selection.
 */
@Slf4j(topic = "ardapaths")
public class MarkerBulkClearHandler extends RespondablePacketHandler<MarkerBulkClearPacket, MarkerBulkClearResponsePacket> {

    /**
     * Maximum number of markers accepted in one bulk-clear request.
     */
    private static final int MAX_MARKERS = 500;

    /**
     * Constructs the handler and its request and response channels.
     */
    public MarkerBulkClearHandler() {
        super(MarkerBulkClearPacket.TYPE, MarkerBulkClearPacket::read, MarkerBulkClearResponsePacket.TYPE, MarkerBulkClearResponsePacket::read);
    }

    /**
     * Validates and applies a marker bulk-clear request.
     *
     * @param server  the Minecraft server
     * @param player  the player who sent the request
     * @param handler the network handler
     * @param packet  the deserialized request packet
     * @param sender  the packet sender
     * @return status result for the client editor
     */
    @Override
    public CompletableFuture<MarkerBulkClearResponsePacket> handleAsync(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, MarkerBulkClearPacket packet, PacketSender sender) {
        if (!PermissionHelper.hasEditPermission(player)) {
            log.warn("Rejected unauthorized packet on {} from {}", getChannelId(), player.getStringUUID());
            return CompletableFuture.completedFuture(response(TimeSpreadStatus.UNAUTHORIZED, 0));
        }

        PathData path = ArdaPaths.CONFIG.getPath(packet.pathId());
        if (path == null || path.getChapter(packet.chapterId()) == null || packet.packedPositions().isEmpty() ||
                packet.packedPositions().size() > MAX_MARKERS || (!packet.clearTime() && !packet.clearWeather())) {
            return CompletableFuture.completedFuture(response(TimeSpreadStatus.INVALID_DATA, 0));
        }

        Set<Long> seen = new HashSet<>();
        List<Long> positions = new ArrayList<>();

        for (Long packedPosition : packet.packedPositions()) {
            if (!seen.add(packedPosition)) continue;
            positions.add(packedPosition);
        }

        if (BackupJobRunner.isJobActive()) {
            return CompletableFuture.completedFuture(response(TimeSpreadStatus.BUSY, 0));
        }

        return BackupJobRunner.submitMarkerWork(server, gate -> applyClear(player, positions, packet.pathId(), packet.chapterId(), packet.clearTime(), packet.clearWeather(), gate));
    }

    /**
     * Creates a response packet.
     *
     * @param status       response status
     * @param updatedCount number of updated markers
     * @return response packet
     */
    private MarkerBulkClearResponsePacket response(TimeSpreadStatus status, int updatedCount) {
        return new MarkerBulkClearResponsePacket(status, updatedCount);
    }

    /**
     * Applies a clear request in chunk-bounded server-thread batches.
     *
     * @param player       player whose current world contains the selection
     * @param positions    deduplicated marker positions
     * @param pathId       path identifier
     * @param chapterId    chapter identifier
     * @param clearTime    whether marker time fields should be cleared
     * @param clearWeather whether marker weather fields should be cleared
     * @param gate         gate for server-thread-only work
     * @return final response packet
     */
    private MarkerBulkClearResponsePacket applyClear(ServerPlayer player, List<Long> positions, String pathId, String chapterId, boolean clearTime, boolean clearWeather, BackupJobRunner.ServerGate gate) {
        ServerLevel world = gate.call(player::level);
        String dimensionId = world.dimension().identifier().toString();
        MarkerResolver resolver = new MarkerResolver(world, dimensionId);
        int updated = 0;

        for (int batchStart = 0; batchStart < positions.size(); ) {
            int toIndex = MarkerBatching.findChunkBoundedBatchEnd(
                    positions,
                    batchStart,
                    ignored -> dimensionId,
                    packedPosition -> ChunkPos.pack(BlockPos.of(packedPosition))
            );
            int fromIndex = batchStart;
            BatchClearResult result = gate.call(() -> applyClearBatch(resolver, positions.subList(fromIndex, toIndex), pathId, chapterId, clearTime, clearWeather));
            if (!result.ok()) {
                return response(TimeSpreadStatus.INVALID_DATA, 0);
            }

            updated += result.updatedCount();
            batchStart = toIndex;
            MarkerBatching.paceBetweenBatches(batchStart, positions.size());
        }

        return response(TimeSpreadStatus.OK, updated);
    }

    /**
     * Applies one clear batch on the server thread.
     *
     * @param resolver     request marker resolver
     * @param positions    marker positions in the current batch
     * @param pathId       path identifier
     * @param chapterId    chapter identifier
     * @param clearTime    whether marker time fields should be cleared
     * @param clearWeather whether marker weather fields should be cleared
     * @return batch result
     */
    private BatchClearResult applyClearBatch(MarkerResolver resolver, List<Long> positions, String pathId, String chapterId, boolean clearTime, boolean clearWeather) {
        int updated = 0;

        for (Long packedPosition : positions) {
            Optional<ResolvedMarker> marker = resolver.resolve(BlockPos.of(packedPosition));
            if (marker.isEmpty()) {
                return new BatchClearResult(false, updated);
            }

            marker.get().clear(clearTime, clearWeather, pathId, chapterId);
            updated++;
        }

        return new BatchClearResult(true, updated);
    }

    /**
     * Creates an error response when asynchronous marker work fails before producing a normal response.
     *
     * @return invalid-data response packet
     */
    @Override
    protected MarkerBulkClearResponsePacket errorResponse() {
        return response(TimeSpreadStatus.INVALID_DATA, 0);
    }

    /**
     * Result of applying one clear batch.
     *
     * @param ok           whether every marker in the batch resolved
     * @param updatedCount number of markers updated in the batch
     */
    private record BatchClearResult(boolean ok, int updatedCount) {

    }
}
