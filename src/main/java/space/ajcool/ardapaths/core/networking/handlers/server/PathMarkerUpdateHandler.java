package space.ajcool.ardapaths.core.networking.handlers.server;

import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import space.ajcool.ardapaths.core.PermissionHelper;
import space.ajcool.ardapaths.core.backup.BackupJobRunner;
import space.ajcool.ardapaths.core.consumers.networking.RespondablePacketHandler;
import space.ajcool.ardapaths.core.data.PathMarkerUpdateStatus;
import space.ajcool.ardapaths.core.markers.MarkerResolver;
import space.ajcool.ardapaths.core.markers.MarkerResolver.ResolvedMarker;
import space.ajcool.ardapaths.core.networking.packets.client.PathMarkerUpdateResponsePacket;
import space.ajcool.ardapaths.core.networking.packets.server.PathMarkerUpdatePacket;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Handles updates to a path marker block's NBT data from the client.
 * Processes incoming {@link PathMarkerUpdatePacket} and applies the updated data to the marker block entity.
 */
@Slf4j(topic = "ardapaths")
public class PathMarkerUpdateHandler extends RespondablePacketHandler<PathMarkerUpdatePacket, PathMarkerUpdateResponsePacket> {

    /**
     * Constructs the handler and its request and response channels.
     */
    public PathMarkerUpdateHandler() {
        super(PathMarkerUpdatePacket.TYPE, PathMarkerUpdatePacket::read, PathMarkerUpdateResponsePacket.TYPE, PathMarkerUpdateResponsePacket::read);
    }

    /**
     * Validates and applies a path marker update payload.
     *
     * @param server  the Minecraft server
     * @param player  the player who sent the request
     * @param handler the network handler
     * @param packet  the deserialized update packet
     * @param sender  the packet sender
     * @return future marker update response
     */
    @Override
    public CompletableFuture<PathMarkerUpdateResponsePacket> handleAsync(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, PathMarkerUpdatePacket packet, PacketSender sender) {
        if (!PermissionHelper.hasEditPermission(player)) {
            log.warn("Rejected unauthorized packet on {} from {}", getChannelId(), player.getStringUUID());
            return CompletableFuture.completedFuture(response(PathMarkerUpdateStatus.UNAUTHORIZED, packet.position().asLong()));
        }

        log.debug("Received marker NBT update for {}", packet.position());
        return BackupJobRunner.submitMarkerWork(server, gate -> update(player, packet, gate));
    }

    /**
     * Creates a marker update response packet.
     *
     * @param status    response status
     * @param packedPos requested marker position
     * @return response packet
     */
    private PathMarkerUpdateResponsePacket response(PathMarkerUpdateStatus status, long packedPos) {
        return new PathMarkerUpdateResponsePacket(status, packedPos);
    }

    /**
     * Resolves one marker and applies the updated marker NBT when present.
     *
     * @param player player whose world contains the marker
     * @param packet marker update request
     * @param gate   gate for server-thread-only work
     * @return marker update response
     */
    private PathMarkerUpdateResponsePacket update(ServerPlayer player, PathMarkerUpdatePacket packet, BackupJobRunner.ServerGate gate) {
        ServerLevel world = gate.call(player::level);
        String dimensionId = world.dimension().identifier().toString();
        MarkerResolver resolver = new MarkerResolver(world, dimensionId);
        BlockPos blockPos = packet.position();
        CompoundTag nbt = packet.data();

        Optional<ResolvedMarker> resolved = gate.call(() -> resolver.resolve(blockPos));
        if (resolved.isEmpty()) {
            return response(PathMarkerUpdateStatus.NOT_FOUND, blockPos.asLong());
        }

        gate.call(() -> {
            resolved.get().liveMarker().loadValidated(nbt);
            resolved.get().liveMarker().markUpdated();
            return null;
        });
        return response(PathMarkerUpdateStatus.OK, blockPos.asLong());
    }

    /**
     * Creates a not-found response when asynchronous marker work fails before producing a normal response.
     *
     * @return not-found marker update response packet
     */
    @Override
    protected PathMarkerUpdateResponsePacket errorResponse() {
        return response(PathMarkerUpdateStatus.NOT_FOUND, 0L);
    }
}
