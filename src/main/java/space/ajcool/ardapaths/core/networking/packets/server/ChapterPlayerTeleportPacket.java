package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

/**
 * Packet sent from client to server to request teleporting the player to a chapter's start position.
 * @param pathId the ID of the path containing the chapter
 * @param chapterId the ID of the chapter to teleport to
 */
public record ChapterPlayerTeleportPacket(String pathId, String chapterId) implements IPacket
{
    /**
     * Network channel used for chapter-start teleport requests.
     */
    public static final ResourceLocation CHANNEL = ModConstants.modId("chapter_player_teleport");

    @Override
    public FriendlyByteBuf build()
    {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(pathId);
        buf.writeUtf(chapterId);
        return buf;
    }

    public static ChapterPlayerTeleportPacket read(FriendlyByteBuf buf)
    {
        final String pathId = buf.readUtf();
        final String chapterId = buf.readUtf();
        return new ChapterPlayerTeleportPacket(pathId, chapterId);
    }
}
