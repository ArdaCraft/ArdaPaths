package space.ajcool.ardapaths.core.networking.handlers.server;

import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
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
        super(PathMarkerUpdatePacket.CHANNEL, PathMarkerUpdatePacket::read);
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
    protected void handle(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, PathMarkerUpdatePacket packet, PacketSender sender)
    {
        log.debug("Received marker NBT update for {}", packet.position());
        BackupJobRunner.submitMarkerWork(server, gate -> {
            ServerLevel world = gate.call(player::serverLevel);
            String dimensionId = world.dimension().location().toString();
            MarkerResolver resolver = new MarkerResolver(world, dimensionId);
            BlockPos blockPos = packet.position();
            CompoundTag nbt = packet.data();

            gate.call(() -> {
                Optional<MarkerResolver.ResolvedMarker> resolved = resolver.resolve(blockPos);
                resolved.ifPresent(marker -> {
                    marker.liveMarker().loadValidated(nbt);
                    marker.liveMarker().markUpdated();
                });
                return null;
            });
            return null;
        });
    }
}
