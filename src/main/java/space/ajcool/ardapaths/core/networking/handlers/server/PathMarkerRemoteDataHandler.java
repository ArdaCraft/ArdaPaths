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
import space.ajcool.ardapaths.core.data.PathMarkerRemoteDataStatus;
import space.ajcool.ardapaths.core.markers.MarkerResolver;
import space.ajcool.ardapaths.core.markers.MarkerResolver.ResolvedMarker;
import space.ajcool.ardapaths.core.networking.packets.client.PathMarkerRemoteDataResponsePacket;
import space.ajcool.ardapaths.core.networking.packets.server.PathMarkerRemoteDataPacket;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Handles server-backed requests for path markers that are not currently loaded on the client.
 */
@Slf4j(topic = "ardapaths")
public class PathMarkerRemoteDataHandler extends RespondablePacketHandler<PathMarkerRemoteDataPacket, PathMarkerRemoteDataResponsePacket> {

    /**
     * Constructs the handler and its request and response channels.
     */
    public PathMarkerRemoteDataHandler() {
        super(PathMarkerRemoteDataPacket.TYPE, PathMarkerRemoteDataPacket::read, PathMarkerRemoteDataResponsePacket.TYPE, PathMarkerRemoteDataResponsePacket::read);
    }

    /**
     * Validates and resolves a full remote path marker NBT payload.
     *
     * @param server  the Minecraft server
     * @param player  the player who sent the request
     * @param handler the network handler
     * @param packet  the deserialized request packet
     * @param sender  the packet sender
     * @return future remote path marker data response
     */
    @Override
    public CompletableFuture<PathMarkerRemoteDataResponsePacket> handleAsync(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, PathMarkerRemoteDataPacket packet, PacketSender sender) {
        if (!PermissionHelper.hasEditPermission(player)) {
            log.warn("Rejected unauthorized packet on {} from {}", getChannelId(), player.getStringUUID());
            return CompletableFuture.completedFuture(response(PathMarkerRemoteDataStatus.UNAUTHORIZED, packet.packedPos(), new CompoundTag()));
        }

        return BackupJobRunner.submitMarkerWork(server, gate -> resolve(player, packet, gate));
    }

    /**
     * Creates a remote path marker data response packet.
     *
     * @param status    response status
     * @param packedPos requested marker position
     * @param data      response marker NBT
     * @return response packet
     */
    private PathMarkerRemoteDataResponsePacket response(PathMarkerRemoteDataStatus status, long packedPos, CompoundTag data) {
        return new PathMarkerRemoteDataResponsePacket(status, packedPos, data);
    }

    /**
     * Resolves one marker through live or persisted chunk state.
     *
     * @param player player whose world contains the requested marker
     * @param packet remote path marker data request
     * @param gate   gate for server-thread-only work
     * @return remote path marker data response
     */
    private PathMarkerRemoteDataResponsePacket resolve(ServerPlayer player, PathMarkerRemoteDataPacket packet, BackupJobRunner.ServerGate gate) {
        ServerLevel world = gate.call(player::level);
        String dimensionId = world.dimension().identifier().toString();
        MarkerResolver resolver = new MarkerResolver(world, dimensionId);
        Optional<ResolvedMarker> marker = gate.call(() -> resolver.resolve(BlockPos.of(packet.packedPos())));
        if (marker.isEmpty()) {
            return response(PathMarkerRemoteDataStatus.NOT_FOUND, packet.packedPos(), new CompoundTag());
        }

        CompoundTag data = gate.call(() -> marker.get().liveMarker().toNbt());
        return response(PathMarkerRemoteDataStatus.OK, packet.packedPos(), data);
    }

    /**
     * Creates a not-found response when asynchronous marker work fails before producing a normal response.
     *
     * @return not-found remote path marker data response packet
     */
    @Override
    protected PathMarkerRemoteDataResponsePacket errorResponse() {
        return response(PathMarkerRemoteDataStatus.NOT_FOUND, 0L, new CompoundTag());
    }
}
