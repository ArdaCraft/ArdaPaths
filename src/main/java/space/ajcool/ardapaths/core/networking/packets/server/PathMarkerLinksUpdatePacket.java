package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

/**
 * Packet sent from client to server to update the path-chapter links stored in a marker's NBT data.
 * @param position the block position of the marker
 * @param data the NBT compound containing updated path and chapter references
 */
public record PathMarkerLinksUpdatePacket(BlockPos position, CompoundTag data) implements IPacket
{
    /**
     * Network channel used for marker path/chapter link updates.
     */
    public static final ResourceLocation CHANNEL = ModConstants.modId("path_marker_links_update");

    @Override
    public FriendlyByteBuf build()
    {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(position);
        buf.writeNbt(data);
        return buf;
    }

    public static PathMarkerLinksUpdatePacket read(FriendlyByteBuf buf)
    {
        final BlockPos position = buf.readBlockPos();
        final CompoundTag data = buf.readNbt();
        return new PathMarkerLinksUpdatePacket(position, data);
    }
}
