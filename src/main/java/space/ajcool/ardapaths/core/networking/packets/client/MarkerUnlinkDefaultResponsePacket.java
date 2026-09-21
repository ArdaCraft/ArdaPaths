package space.ajcool.ardapaths.core.networking.packets.client;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.IRespondablePacket;
import space.ajcool.ardapaths.core.data.TimeSpreadStatus;

import java.util.UUID;

/**
 * Packet sent from server to client with the result of a default-chapter unlink request.
 *
 * @param requestId    request correlation id
 * @param status       status code describing the result
 * @param updatedCount number of markers unlinked by the server
 */
public record MarkerUnlinkDefaultResponsePacket(UUID requestId, TimeSpreadStatus status, int updatedCount) implements IRespondablePacket<MarkerUnlinkDefaultResponsePacket> {
    /**
     * Network channel used for default-chapter unlink responses.
     */
    public static final ResourceLocation CHANNEL = ModConstants.modId("marker_unlink_default_response");

    /**
     * Creates an unlink response before request correlation is assigned.
     *
     * @param status       status code describing the result
     * @param updatedCount number of markers unlinked by the server
     */
    public MarkerUnlinkDefaultResponsePacket(TimeSpreadStatus status, int updatedCount) {
        this(IRespondablePacket.UNASSIGNED_REQUEST_ID, status, updatedCount);
    }

    /**
     * Creates a response with the supplied request id.
     *
     * @param requestId request correlation id
     * @return packet carrying the supplied request id
     */
    @Override
    public MarkerUnlinkDefaultResponsePacket withRequestId(UUID requestId) {
        return new MarkerUnlinkDefaultResponsePacket(requestId, status, updatedCount);
    }

    /**
     * Serializes this response for server-to-client transmission.
     *
     * @return packet data buffer
     */
    @Override
    public FriendlyByteBuf build() {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUUID(requestId);
        buf.writeEnum(status);
        buf.writeInt(updatedCount);
        return buf;
    }

    /**
     * Reads a marker default-chapter unlink response packet from a network buffer.
     *
     * @param buf packet data buffer
     * @return decoded packet
     */
    public static MarkerUnlinkDefaultResponsePacket read(FriendlyByteBuf buf) {
        return new MarkerUnlinkDefaultResponsePacket(buf.readUUID(), buf.readEnum(TimeSpreadStatus.class), buf.readInt());
    }
}
