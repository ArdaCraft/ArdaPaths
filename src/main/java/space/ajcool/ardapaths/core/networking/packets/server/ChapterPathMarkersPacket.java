package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.IRespondablePacket;

import java.util.UUID;

/**
 * Packet sent from client to server to request a complete chapter marker chain.
 *
 * @param requestId        request correlation id
 * @param pathId           path identifier for the requested chapter
 * @param chapterId        chapter identifier for the requested marker chain
 * @param currentPackedPos packed position of the marker currently being edited
 */
public record ChapterPathMarkersPacket(UUID requestId, String pathId, String chapterId,
                                       long currentPackedPos) implements IRespondablePacket<ChapterPathMarkersPacket> {

    /**
     * Network channel used for chapter marker chain requests.
     */
    public static final ResourceLocation CHANNEL = ModConstants.modId("chapter_path_markers");

    /**
     * Custom payload type used for typed Fabric networking.
     */
    public static final CustomPacketPayload.Type<ChapterPathMarkersPacket> TYPE = new CustomPacketPayload.Type<>(CHANNEL);

    /**
     * Creates a chapter marker request before request correlation is assigned.
     *
     * @param pathId           path identifier for the requested chapter
     * @param chapterId        chapter identifier for the requested marker chain
     * @param currentPackedPos packed position of the marker currently being edited
     */
    public ChapterPathMarkersPacket(String pathId, String chapterId, long currentPackedPos) {
        this(IRespondablePacket.UNASSIGNED_REQUEST_ID, pathId, chapterId, currentPackedPos);
    }

    /**
     * Reads a chapter marker list request packet from a network buffer.
     *
     * @param buf packet data buffer
     * @return decoded packet
     */
    public static ChapterPathMarkersPacket read(FriendlyByteBuf buf) {
        return new ChapterPathMarkersPacket(buf.readUUID(), buf.readUtf(), buf.readUtf(), buf.readLong());
    }

    /**
     * Creates a request with the supplied request id.
     *
     * @param requestId request correlation id
     * @return packet carrying the supplied request id
     */
    @Override
    public ChapterPathMarkersPacket withRequestId(UUID requestId) {
        return new ChapterPathMarkersPacket(requestId, pathId, chapterId, currentPackedPos);
    }

    /**
     * Gets the custom payload type for this packet.
     *
     * @return this packet's payload type
     */
    @SuppressWarnings("NullableProblems")
    @Override
    public CustomPacketPayload.Type<ChapterPathMarkersPacket> type() {
        return TYPE;
    }

    @Override
    public FriendlyByteBuf build() {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUUID(requestId);
        buf.writeUtf(pathId);
        buf.writeUtf(chapterId);
        buf.writeLong(currentPackedPos);
        return buf;
    }
}
