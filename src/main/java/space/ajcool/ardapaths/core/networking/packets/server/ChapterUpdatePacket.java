package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.FriendlyByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;

/**
 * Packet sent from client to server to update a chapter's metadata and configuration.
 *
 * @param pathId       the ID of the path containing the chapter
 * @param chapterId    the ID of the chapter to update
 * @param chapterName  the new chapter name
 * @param chapterDate  the new chapter date
 * @param chapterIndex the chapter's position index in the path
 * @param warp         the warp location for "Return to Chapter Start" functionality
 */
public record ChapterUpdatePacket(
        String pathId,
        String chapterId,
        String chapterName,
        String chapterDate,
        int chapterIndex,
        String warp
) implements IPacket {

    /**
     * Network channel used for chapter metadata updates.
     */
    public static final Identifier CHANNEL = ModConstants.modId("path_chapter_update");

    /**
     * Custom payload type used for typed Fabric networking.
     */
    public static final CustomPacketPayload.Type<ChapterUpdatePacket> TYPE = new CustomPacketPayload.Type<>(CHANNEL);

    public ChapterUpdatePacket(String pathId, ChapterData chapter) {
        this(pathId, chapter.getId(), chapter.getName(), chapter.getDate(), chapter.getIndex(), chapter.getWarp());
    }

    public static ChapterUpdatePacket read(FriendlyByteBuf buf) {
        final String pathId = buf.readUtf();
        final String chapterId = buf.readUtf();
        final String chapterName = buf.readUtf();
        final String chapterDate = buf.readUtf();
        final int chapterIndex = buf.readInt();
        final String warp = buf.readUtf();
        return new ChapterUpdatePacket(pathId, chapterId, chapterName, chapterDate, chapterIndex, warp);
    }

    /**
     * Gets the custom payload type for this packet.
     *
     * @return this packet's payload type
     */
    @Override
    public CustomPacketPayload.@NotNull Type<ChapterUpdatePacket> type() {
        return TYPE;
    }

    @Override
    public FriendlyByteBuf build() {
        FriendlyByteBuf buf = FriendlyByteBufs.create();
        buf.writeUtf(pathId);
        buf.writeUtf(chapterId);
        buf.writeUtf(chapterName);
        buf.writeUtf(chapterDate);
        buf.writeInt(chapterIndex);
        buf.writeUtf(warp);
        return buf;
    }
}
