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
 * Packet sent from server to client with the result of a marker bulk-clear request.
 *
 * @param requestId    request correlation id
 * @param status       status code describing the result
 * @param updatedCount number of markers updated by the server
 */
public record MarkerBulkClearResponsePacket(UUID requestId, TimeSpreadStatus status,
                                            int updatedCount) implements IRespondablePacket<MarkerBulkClearResponsePacket> {

    /**
     * Network channel used for marker bulk-clear responses.
     */
    public static final Identifier CHANNEL = ModConstants.modId("marker_bulk_clear_response");

    /**
     * Custom payload type used for typed Fabric networking.
     */
    public static final CustomPacketPayload.Type<MarkerBulkClearResponsePacket> TYPE = new CustomPacketPayload.Type<>(CHANNEL);

    /**
     * Creates a bulk-clear response before request correlation is assigned.
     *
     * @param status       status code describing the result
     * @param updatedCount number of markers updated by the server
     */
    public MarkerBulkClearResponsePacket(TimeSpreadStatus status, int updatedCount) {
        this(IRespondablePacket.UNASSIGNED_REQUEST_ID, status, updatedCount);
    }

    /**
     * Reads a marker bulk-clear response packet from a network buffer.
     *
     * @param buf packet data buffer
     * @return decoded packet
     */
    public static MarkerBulkClearResponsePacket read(FriendlyByteBuf buf) {
        return new MarkerBulkClearResponsePacket(buf.readUUID(), buf.readEnum(TimeSpreadStatus.class), buf.readInt());
    }

    /**
     * Creates a response with the supplied request id.
     *
     * @param requestId request correlation id
     * @return packet carrying the supplied request id
     */
    @Override
    public MarkerBulkClearResponsePacket withRequestId(UUID requestId) {
        return new MarkerBulkClearResponsePacket(requestId, status, updatedCount);
    }

    /**
     * Gets the custom payload type for this packet.
     *
     * @return this packet's payload type
     */
    @Override
    public CustomPacketPayload.@NotNull Type<MarkerBulkClearResponsePacket> type() {
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
