package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.FriendlyByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.IRespondablePacket;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Packet sent from client to server to unlink selected markers from the default chapter.
 *
 * @param requestId       request correlation id
 * @param packedPositions packed absolute marker positions selected by the editor
 * @param pathId          path identifier whose default chapter should be unlinked
 */
public record MarkerUnlinkDefaultPacket(UUID requestId, List<Long> packedPositions,
                                        String pathId) implements IRespondablePacket<MarkerUnlinkDefaultPacket> {

    /**
     * Network channel used for marker default-chapter unlink requests.
     */
    public static final Identifier CHANNEL = ModConstants.modId("marker_unlink_default");

    /**
     * Custom payload type used for typed Fabric networking.
     */
    public static final CustomPacketPayload.Type<MarkerUnlinkDefaultPacket> TYPE = new CustomPacketPayload.Type<>(CHANNEL);

    /**
     * Creates an unlink request before request correlation is assigned.
     *
     * @param packedPositions packed absolute marker positions selected by the editor
     * @param pathId          path identifier whose default chapter should be unlinked
     */
    public MarkerUnlinkDefaultPacket(List<Long> packedPositions, String pathId) {
        this(IRespondablePacket.UNASSIGNED_REQUEST_ID, packedPositions, pathId);
    }

    /**
     * Reads a default-chapter unlink request packet from a network buffer.
     *
     * @param buf packet data buffer
     * @return decoded packet
     */
    public static MarkerUnlinkDefaultPacket read(FriendlyByteBuf buf) {
        UUID requestId = buf.readUUID();
        int count = buf.readInt();
        List<Long> packedPositions = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            packedPositions.add(buf.readLong());
        }
        return new MarkerUnlinkDefaultPacket(requestId, packedPositions, buf.readUtf());
    }

    /**
     * Creates a request with the supplied request id.
     *
     * @param requestId request correlation id
     * @return packet carrying the supplied request id
     */
    @Override
    public MarkerUnlinkDefaultPacket withRequestId(UUID requestId) {
        return new MarkerUnlinkDefaultPacket(requestId, packedPositions, pathId);
    }

    /**
     * Gets the custom payload type for this packet.
     *
     * @return this packet's payload type
     */
    @Override
    public CustomPacketPayload.@NotNull Type<MarkerUnlinkDefaultPacket> type() {
        return TYPE;
    }

    @Override
    public FriendlyByteBuf build() {
        FriendlyByteBuf buf = FriendlyByteBufs.create();
        buf.writeUUID(requestId);
        buf.writeInt(packedPositions.size());
        for (Long packedPosition : packedPositions) {
            buf.writeLong(packedPosition);
        }
        buf.writeUtf(pathId);
        return buf;
    }
}
