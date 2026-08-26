package space.ajcool.ardapaths.core.networking.handlers.server;

import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import space.ajcool.ardapaths.core.backup.BackupJobRunner;
import space.ajcool.ardapaths.core.consumers.networking.ServerPacketHandler;
import space.ajcool.ardapaths.core.markers.MarkerResolver;
import space.ajcool.ardapaths.core.networking.packets.server.PathMarkerUpdatePacket;

import java.util.Optional;

/**
 * Handles updates to a path marker block's NBT data from the client.
 * Processes incoming {@link PathMarkerUpdatePacket} and applies the updated data to the marker block entity.
 */
@Slf4j(topic = "ardapaths")
public class PathMarkerUpdateHandler extends ServerPacketHandler<PathMarkerUpdatePacket>
{
    public PathMarkerUpdateHandler()
    {
        super("path_marker_update", PathMarkerUpdatePacket::read);
    }

    /**
     * Requires edit permission because marker updates mutate marker NBT.
     *
     * @return true because this packet changes editable marker data
     */
    @Override
    protected boolean requiresEditPermission() {
        return true;
    }

    @Override
    protected void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PathMarkerUpdatePacket packet, PacketSender sender)
    {
        log.debug("Received marker NBT update for {}", packet.position());
        BackupJobRunner.submitMarkerWork(server, gate -> {
            ServerWorld world = gate.call(player::getServerWorld);
            String dimensionId = world.getRegistryKey().getValue().toString();
            MarkerResolver resolver = new MarkerResolver(world, dimensionId);
            BlockPos blockPos = packet.position();
            NbtCompound nbt = packet.data();

            gate.call(() -> {
                Optional<MarkerResolver.ResolvedMarker> resolved = resolver.resolve(blockPos);
                resolved.ifPresent(marker -> {
                    marker.liveMarker().readNbt(nbt);
                    marker.liveMarker().markUpdated();
                });
                return null;
            });
            return null;
        });
    }
}
