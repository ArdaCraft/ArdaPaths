package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.PathData;

/**
 * Packet sent from client to server to request deletion of a chapter from a path.
 * @param pathId the ID of the path containing the chapter
 * @param chapterId the ID of the chapter to delete
 */
public record ChapterDeletePacket(
        String pathId,
        String chapterId
) implements IPacket
{

    @Override
    public PacketByteBuf build()
    {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(pathId);
        buf.writeString(chapterId);
        return buf;
    }

    public static ChapterDeletePacket read(PacketByteBuf buf)
    {
        final String pathId = buf.readString();
        final String chapterId = buf.readString();
        return new ChapterDeletePacket(pathId, chapterId);
    }
}
