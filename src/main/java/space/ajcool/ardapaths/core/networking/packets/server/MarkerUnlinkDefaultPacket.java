package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
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
public record MarkerUnlinkDefaultPacket(UUID requestId, List<Long> packedPositions, String pathId) implements IRespondablePacket<MarkerUnlinkDefaultPacket> {
    /**
     * Network channel used for marker default-chapter unlink requests.
     */
    public static final ResourceLocation CHANNEL = ModConstants.modId("marker_unlink_default");

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
     * Serializes this request for client-to-server transmission.
     *
     * @return packet data buffer
     */
    @Override
    public FriendlyByteBuf build() {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUUID(requestId);
        buf.writeInt(packedPositions.size());
        for (Long packedPosition : packedPositions) {
            buf.writeLong(packedPosition);
        }
        buf.writeUtf(pathId);
        return buf;
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
}
