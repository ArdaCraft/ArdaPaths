package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

/**
 * Packet sent from client to server to update a chapter's start position.
 * @param pathId the ID of the path containing the chapter
 * @param chapterId the ID of the chapter to update
 * @param position the new block position for the chapter start
 */
public record ChapterStartUpdatePacket(String pathId, String chapterId, BlockPos position) implements IPacket
{

    @Override
    public PacketByteBuf build()
    {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(pathId);
        buf.writeString(chapterId);
        buf.writeBlockPos(position);
        return buf;
    }

    public static ChapterStartUpdatePacket read(PacketByteBuf buf)
    {
        final String pathId = buf.readString();
        final String chapterId = buf.readString();
        final BlockPos position = buf.readBlockPos();
        return new ChapterStartUpdatePacket(pathId, chapterId, position);
    }
}
