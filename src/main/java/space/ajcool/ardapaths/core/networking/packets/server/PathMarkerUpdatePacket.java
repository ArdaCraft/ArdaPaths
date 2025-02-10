package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

public record PathMarkerUpdatePacket(BlockPos position, NbtCompound data) implements IPacket {

    @Override
    public PacketByteBuf build() {
        return PacketByteBufs.create()
                .writeBlockPos(position)
                .writeNbt(data);
    }

    public static PathMarkerUpdatePacket read(PacketByteBuf buf) {
        final BlockPos position = buf.readBlockPos();
        final NbtCompound data = buf.readNbt();
        return new PathMarkerUpdatePacket(position, data);
    }
}
