package space.ajcool.ardapaths.core.networking.packets.client;

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
 * Packet sent from server to client with dimension identifiers available on the server.
 *
 * @param requestId  request correlation id
 * @param dimensions server dimension identifiers visible to editors
 */
public record DimensionListResponsePacket(UUID requestId, List<String> dimensions) implements IRespondablePacket<DimensionListResponsePacket> {

    /**
     * Network channel used for dimension list responses.
     */
    public static final Identifier CHANNEL = ModConstants.modId("dimension_list_response");

    /**
     * Custom payload type used for typed Fabric networking.
     */
    public static final CustomPacketPayload.Type<DimensionListResponsePacket> TYPE = new CustomPacketPayload.Type<>(CHANNEL);

    /**
     * Creates a dimension list response before request correlation is assigned.
     *
     * @param dimensions server dimension identifiers visible to editors
     */
    public DimensionListResponsePacket(List<String> dimensions) {
        this(IRespondablePacket.UNASSIGNED_REQUEST_ID, dimensions);
    }

    /**
     * Deserializes a dimension list response from a packet buffer.
     *
     * @param buf packet data buffer
     * @return decoded dimension list response
     */
    public static DimensionListResponsePacket read(FriendlyByteBuf buf) {
        UUID requestId = buf.readUUID();
        int count = buf.readVarInt();
        List<String> dimensions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            dimensions.add(buf.readUtf());
        }
        return new DimensionListResponsePacket(requestId, dimensions);
    }

    /**
     * Creates a dimension list response with a different request id.
     *
     * @param requestId request correlation id
     * @return packet carrying the supplied request id
     */
    @Override
    public DimensionListResponsePacket withRequestId(UUID requestId) {
        return new DimensionListResponsePacket(requestId, dimensions);
    }

    /**
     * Gets the custom payload type for this packet.
     *
     * @return this packet's payload type
     */
    @Override
    public CustomPacketPayload.@NotNull Type<DimensionListResponsePacket> type() {
        return TYPE;
    }

    /**
     * Builds this packet into a buffer for transmission.
     *
     * @return packet data buffer
     */
    @Override
    public FriendlyByteBuf build() {
        FriendlyByteBuf buf = FriendlyByteBufs.create();
        buf.writeUUID(requestId);
        buf.writeVarInt(dimensions.size());
        dimensions.forEach(buf::writeUtf);
        return buf;
    }
}
