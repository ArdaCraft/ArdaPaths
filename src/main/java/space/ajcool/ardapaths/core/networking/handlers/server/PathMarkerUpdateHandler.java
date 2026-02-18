package space.ajcool.ardapaths.core.networking.handlers.server;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.consumers.networking.ServerPacketHandler;
import space.ajcool.ardapaths.core.networking.packets.server.PathMarkerUpdatePacket;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;

public class PathMarkerUpdateHandler extends ServerPacketHandler<PathMarkerUpdatePacket>
{
    public PathMarkerUpdateHandler()
    {
        super("path_marker_update", PathMarkerUpdatePacket::read);
    }

    @Override
    protected void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PathMarkerUpdatePacket packet, PacketSender sender)
    {
        BlockPos blockPos = packet.position();
        NbtCompound nbt = packet.data();
        ArdaPaths.LOGGER.info("Received NBT : [{}]", nbt.toString());
        server.execute(() ->
        {
            BlockEntity blockEntity = player.getWorld().getBlockEntity(blockPos);

            if (blockEntity instanceof PathMarkerBlockEntity marker)
            {
                marker.readNbt(nbt);
                marker.markUpdated();
            }
        });
    }
}
