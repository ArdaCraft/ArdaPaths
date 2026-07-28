package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

/**
 * Packet sent from client to server to remove a chapter's start position.
 * @param pathId the ID of the path containing the chapter
 * @param chapterId the ID of the chapter whose start position should be removed
 */
public record ChapterStartRemovePacket(String pathId, String chapterId) implements IPacket
{

    @Override
    public PacketByteBuf build()
    {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(pathId);
        buf.writeString(chapterId);
        return buf;
    }

    public static ChapterStartRemovePacket read(PacketByteBuf buf)
    {
        final String pathId = buf.readString();
        final String chapterId = buf.readString();
        return new ChapterStartRemovePacket(pathId, chapterId);
    }
}
