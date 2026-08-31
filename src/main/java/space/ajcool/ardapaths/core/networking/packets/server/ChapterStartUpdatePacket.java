package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

/**
 * Packet sent from client to server to update a chapter's start position.
 * @param pathId the ID of the path containing the chapter
 * @param chapterId the ID of the chapter to update
 * @param position the new block position for the chapter start
 */
public record ChapterStartUpdatePacket(String pathId, String chapterId, BlockPos position) implements IPacket
{
    /**
     * Network channel used for chapter-start position updates.
     */
    public static final ResourceLocation CHANNEL = ModConstants.modId("path_chapter_start_update");

    @Override
    public FriendlyByteBuf build()
    {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(pathId);
        buf.writeUtf(chapterId);
        buf.writeBlockPos(position);
        return buf;
    }

    public static ChapterStartUpdatePacket read(FriendlyByteBuf buf)
    {
        final String pathId = buf.readUtf();
        final String chapterId = buf.readUtf();
        final BlockPos position = buf.readBlockPos();
        return new ChapterStartUpdatePacket(pathId, chapterId, position);
    }
}
