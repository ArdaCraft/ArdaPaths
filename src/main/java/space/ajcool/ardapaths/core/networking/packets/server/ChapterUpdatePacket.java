package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.FriendlyByteBufs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;

/**
 * Packet sent from client to server to update a chapter's metadata and configuration.
 *
 * @param pathId       the ID of the path containing the chapter
 * @param chapterId    the ID of the chapter to update
 * @param chapterName  the new chapter name
 * @param chapterIndex the chapter's position index in the path
 * @param warp         the warp location for "Return to Chapter Start" functionality
 * @param coordinates  explicit chapter-start coordinates, or null to preserve/derive them
 * @param dimension    dimension for explicit chapter-start coordinates, or null when coordinates are absent
 */
public record ChapterUpdatePacket(
        String pathId,
        String chapterId,
        String chapterName,
        int chapterIndex,
        String warp,
        @Nullable BlockPos coordinates,
        @Nullable String dimension
) implements IPacket {

    /**
     * Network channel used for chapter metadata updates.
     */
    public static final Identifier CHANNEL = ModConstants.modId("path_chapter_update");

    /**
     * Custom payload type used for typed Fabric networking.
     */
    public static final CustomPacketPayload.Type<ChapterUpdatePacket> TYPE = new CustomPacketPayload.Type<>(CHANNEL);

    /**
     * Creates an update packet from an in-memory chapter definition.
     *
     * @param pathId  path that owns the chapter
     * @param chapter chapter definition to send
     */
    public ChapterUpdatePacket(String pathId, ChapterData chapter) {
        this(pathId, chapter.getId(), chapter.getName(), chapter.getIndex(), chapter.getWarp(),
                chapter.getCoordinates() == null ? null : chapter.getCoordinates().toBlockPos(),
                chapter.getCoordinates() == null ? null : chapter.getDimension());
    }

    /**
     * Creates an update packet without explicit coordinates.
     *
     * @param pathId       path that owns the chapter
     * @param chapterId    chapter identifier
     * @param chapterName  display name
     * @param chapterIndex order index
     * @param warp         optional warp name
     */
    public ChapterUpdatePacket(String pathId, String chapterId, String chapterName, int chapterIndex, String warp) {
        this(pathId, chapterId, chapterName, chapterIndex, warp, null, null);
    }

    /**
     * Reads a chapter update packet from a byte buffer.
     *
     * @param buf buffer containing encoded packet fields
     * @return decoded chapter update packet
     */
    public static ChapterUpdatePacket read(FriendlyByteBuf buf) {
        final String pathId = buf.readUtf();
        final String chapterId = buf.readUtf();
        final String chapterName = buf.readUtf();
        final int chapterIndex = buf.readInt();
        final String warp = buf.readUtf();
        final boolean hasCoordinates = buf.readBoolean();
        final BlockPos coordinates = hasCoordinates ? buf.readBlockPos() : null;
        final String dimension = hasCoordinates ? buf.readUtf() : null;
        return new ChapterUpdatePacket(pathId, chapterId, chapterName, chapterIndex, warp, coordinates, dimension);
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
        buf.writeInt(chapterIndex);
        buf.writeUtf(warp);
        buf.writeBoolean(coordinates != null);
        if (coordinates != null) {
            buf.writeBlockPos(coordinates);
            buf.writeUtf(dimension == null ? "" : dimension);
        }
        return buf;
    }
}
