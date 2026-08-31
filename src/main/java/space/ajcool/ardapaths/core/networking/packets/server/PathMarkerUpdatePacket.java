package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

/**
 * Packet sent from client to server to update a path marker block's NBT data.
 * @param position the block position of the marker to update
 * @param data the NBT compound containing the updated marker configuration
 */
public record PathMarkerUpdatePacket(BlockPos position, CompoundTag data) implements IPacket
{
    /**
     * Network channel used for marker NBT updates.
     */
    public static final ResourceLocation CHANNEL = ModConstants.modId("path_marker_update");

    @Override
    public FriendlyByteBuf build()
    {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(position);
        buf.writeNbt(data);
        return buf;
    }

    public static PathMarkerUpdatePacket read(FriendlyByteBuf buf)
    {
        final BlockPos position = buf.readBlockPos();
        final CompoundTag data = buf.readNbt();
        return new PathMarkerUpdatePacket(position, data);
    }
}
