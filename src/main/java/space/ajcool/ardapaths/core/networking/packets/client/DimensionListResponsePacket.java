package space.ajcool.ardapaths.core.networking.packets.client;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
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
    public static final ResourceLocation CHANNEL = ModConstants.modId("dimension_list_response");

    /**
     * Creates a dimension list response before request correlation is assigned.
     *
     * @param dimensions server dimension identifiers visible to editors
     */
    public DimensionListResponsePacket(List<String> dimensions) {
        this(IRespondablePacket.UNASSIGNED_REQUEST_ID, dimensions);
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
     * Builds this packet into a buffer for transmission.
     *
     * @return packet data buffer
     */
    @Override
    public FriendlyByteBuf build() {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUUID(requestId);
        buf.writeVarInt(dimensions.size());
        dimensions.forEach(buf::writeUtf);
        return buf;
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
}
