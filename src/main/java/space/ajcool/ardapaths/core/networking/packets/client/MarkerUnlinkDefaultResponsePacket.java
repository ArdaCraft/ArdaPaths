package space.ajcool.ardapaths.core.networking.packets.client;

import net.fabricmc.fabric.api.networking.v1.FriendlyByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
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
public record MarkerUnlinkDefaultResponsePacket(UUID requestId, TimeSpreadStatus status,
                                                int updatedCount) implements IRespondablePacket<MarkerUnlinkDefaultResponsePacket> {

    /**
     * Network channel used for default-chapter unlink responses.
     */
    public static final Identifier CHANNEL = ModConstants.modId("marker_unlink_default_response");

    /**
     * Custom payload type used for typed Fabric networking.
     */
    public static final CustomPacketPayload.Type<MarkerUnlinkDefaultResponsePacket> TYPE = new CustomPacketPayload.Type<>(CHANNEL);

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
     * Reads a default-chapter unlink response packet from a network buffer.
     *
     * @param buf packet data buffer
     * @return decoded packet
     */
    public static MarkerUnlinkDefaultResponsePacket read(FriendlyByteBuf buf) {
        return new MarkerUnlinkDefaultResponsePacket(buf.readUUID(), buf.readEnum(TimeSpreadStatus.class), buf.readInt());
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
     * Gets the custom payload type for this packet.
     *
     * @return this packet's payload type
     */
    @Override
    public CustomPacketPayload.@NotNull Type<MarkerUnlinkDefaultResponsePacket> type() {
        return TYPE;
    }

    @Override
    public FriendlyByteBuf build() {
        FriendlyByteBuf buf = FriendlyByteBufs.create();
        buf.writeUUID(requestId);
        buf.writeEnum(status);
        buf.writeInt(updatedCount);
        return buf;
    }
}
