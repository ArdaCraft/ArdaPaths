package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

/**
 * Packet sent from client to server to update the path-chapter links stored in a marker's NBT data.
 * @param position the block position of the marker
 * @param data the NBT compound containing updated path and chapter references
 */
public record PathMarkerLinksUpdatePacket(BlockPos position, NbtCompound data) implements IPacket
{

    @Override
    public PacketByteBuf build()
    {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(position);
        buf.writeNbt(data);
        return buf;
    }

    public static PathMarkerLinksUpdatePacket read(PacketByteBuf buf)
    {
        final BlockPos position = buf.readBlockPos();
        final NbtCompound data = buf.readNbt();
        return new PathMarkerLinksUpdatePacket(position, data);
    }
}
