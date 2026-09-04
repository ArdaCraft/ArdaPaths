package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

/**
 * Packet sent from client to server to remove a chapter's start position.
 *
 * @param pathId    the ID of the path containing the chapter
 * @param chapterId the ID of the chapter whose start position should be removed
 * @param position  marker position requesting the removal
 */
public record ChapterStartRemovePacket(String pathId, String chapterId, BlockPos position) implements IPacket {

    /**
     * Network channel used for chapter-start removal requests.
     */
    public static final ResourceLocation CHANNEL = ModConstants.modId("chapter_start_remove");

    /**
     * Custom payload type used for typed Fabric networking.
     */
    public static final CustomPacketPayload.Type<ChapterStartRemovePacket> TYPE = new CustomPacketPayload.Type<>(CHANNEL);

    public static ChapterStartRemovePacket read(FriendlyByteBuf buf) {
        final String pathId = buf.readUtf();
        final String chapterId = buf.readUtf();
        final BlockPos position = buf.readBlockPos();
        return new ChapterStartRemovePacket(pathId, chapterId, position);
    }

    /**
     * Gets the custom payload type for this packet.
     *
     * @return this packet's payload type
     */
    @Override
    public CustomPacketPayload.@NotNull Type<ChapterStartRemovePacket> type() {
        return TYPE;
    }

    @Override
    public FriendlyByteBuf build() {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(pathId);
        buf.writeUtf(chapterId);
        buf.writeBlockPos(position);
        return buf;
    }
}
