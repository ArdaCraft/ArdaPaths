package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.FriendlyByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

/**
 * Packet sent from client to server to request deletion of a chapter from a path.
 *
 * @param pathId    the ID of the path containing the chapter
 * @param chapterId the ID of the chapter to delete
 */
public record ChapterDeletePacket(
        String pathId,
        String chapterId
) implements IPacket {

    /**
     * Network channel used for chapter deletion requests.
     */
    public static final Identifier CHANNEL = ModConstants.modId("path_chapter_delete");

    /**
     * Custom payload type used for typed Fabric networking.
     */
    public static final CustomPacketPayload.Type<ChapterDeletePacket> TYPE = new CustomPacketPayload.Type<>(CHANNEL);

    public static ChapterDeletePacket read(FriendlyByteBuf buf) {
        final String pathId = buf.readUtf();
        final String chapterId = buf.readUtf();
        return new ChapterDeletePacket(pathId, chapterId);
    }

    /**
     * Gets the custom payload type for this packet.
     *
     * @return this packet's payload type
     */
    @Override
    public CustomPacketPayload.@NotNull Type<ChapterDeletePacket> type() {
        return TYPE;
    }

    @Override
    public FriendlyByteBuf build() {
        FriendlyByteBuf buf = FriendlyByteBufs.create();
        buf.writeUtf(pathId);
        buf.writeUtf(chapterId);
        return buf;
    }
}
