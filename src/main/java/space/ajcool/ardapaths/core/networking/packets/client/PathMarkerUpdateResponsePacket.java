package space.ajcool.ardapaths.core.networking.packets.client;

import net.fabricmc.fabric.api.networking.v1.FriendlyByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.IRespondablePacket;
import space.ajcool.ardapaths.core.data.PathMarkerUpdateStatus;

import java.util.UUID;

/**
 * Packet sent from server to client with the result of a path marker save.
 *
 * @param requestId request correlation id
 * @param status    status code describing the save result
 * @param packedPos packed position of the updated path marker
 */
public record PathMarkerUpdateResponsePacket(UUID requestId,
                                             PathMarkerUpdateStatus status,
                                             long packedPos) implements IRespondablePacket<PathMarkerUpdateResponsePacket> {

    /** Network channel used for marker update responses. */
    public static final Identifier CHANNEL = ModConstants.modId("path_marker_update_response");

    /** Custom payload type used for typed Fabric networking. */
    public static final CustomPacketPayload.Type<PathMarkerUpdateResponsePacket> TYPE = new CustomPacketPayload.Type<>(CHANNEL);

    /**
     * Creates a marker update response before request correlation is assigned.
     *
     * @param status    status code describing the save result
     * @param packedPos packed position of the updated path marker
     */
    public PathMarkerUpdateResponsePacket(PathMarkerUpdateStatus status, long packedPos) {
        this(IRespondablePacket.UNASSIGNED_REQUEST_ID, status, packedPos);
    }

    /**
     * Reads a marker update response packet from a network buffer.
     *
     * @param buf packet data buffer
     * @return decoded packet
     */
    public static PathMarkerUpdateResponsePacket read(FriendlyByteBuf buf) {
        return new PathMarkerUpdateResponsePacket(
                buf.readUUID(),
                buf.readEnum(PathMarkerUpdateStatus.class),
                buf.readLong()
        );
    }

    /**
     * Creates a response with the supplied request id.
     *
     * @param requestId request correlation id
     * @return packet carrying the supplied request id
     */
    @Override
    public PathMarkerUpdateResponsePacket withRequestId(UUID requestId) {
        return new PathMarkerUpdateResponsePacket(requestId, status, packedPos);
    }

    /**
     * Gets the custom payload type for this packet.
     *
     * @return this packet's payload type
     */
    @Override
    public CustomPacketPayload.@NotNull Type<PathMarkerUpdateResponsePacket> type() {
        return TYPE;
    }

    @Override
    public FriendlyByteBuf build() {
        FriendlyByteBuf buf = FriendlyByteBufs.create();
        buf.writeUUID(requestId);
        buf.writeEnum(status);
        buf.writeLong(packedPos);
        return buf;
    }
}
