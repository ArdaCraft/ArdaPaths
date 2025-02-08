package space.ajcool.ardapaths.networking.packets;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.networking.AbstractPacket;

public class PathMarkerUpdatePacket extends AbstractPacket {
    public PathMarkerUpdatePacket() {
        super("path_marker_update");
    }

    /**
     * Create a new {@link PathMarkerUpdatePacket} packet.
     *
     * @param blockPos The block position
     * @param nbt The NBT data
     */
    public PacketByteBuf create(BlockPos blockPos, NbtCompound nbt) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(blockPos);
        buf.writeNbt(nbt);
        return buf;
    }

    /**
     * Send a {@link PathMarkerUpdatePacket} packet to the server.
     *
     * @param blockPos The block position
     * @param nbt The NBT data
     */
    public void sendToServer(BlockPos blockPos, NbtCompound nbt) {
        super.sendToServer(create(blockPos, nbt));
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        BlockPos blockPos = buf.readBlockPos();
        NbtCompound nbt = buf.readNbt();

        server.execute(() -> {
            var blockEntity = player.getWorld().getBlockEntity(blockPos);

            if (blockEntity instanceof PathMarkerBlockEntity pathMarkerBlockEntity)
            {
                pathMarkerBlockEntity.readNbt(nbt);
                pathMarkerBlockEntity.markUpdated();
            }
        });
    }
}
