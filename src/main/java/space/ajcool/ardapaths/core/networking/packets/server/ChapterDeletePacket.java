package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

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
    /**
     * Network channel used for chapter deletion requests.
     */
    public static final ResourceLocation CHANNEL = ModConstants.modId("path_chapter_delete");

    @Override
    public FriendlyByteBuf build()
    {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(pathId);
        buf.writeUtf(chapterId);
        return buf;
    }

    public static ChapterDeletePacket read(FriendlyByteBuf buf)
    {
        final String pathId = buf.readUtf();
        final String chapterId = buf.readUtf();
        return new ChapterDeletePacket(pathId, chapterId);
    }
}
