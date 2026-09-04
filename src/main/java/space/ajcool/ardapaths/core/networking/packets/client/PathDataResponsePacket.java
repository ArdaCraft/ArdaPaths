package space.ajcool.ardapaths.core.networking.packets.client;

import net.fabricmc.fabric.api.networking.v1.FriendlyByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.IRespondablePacket;

import java.util.UUID;

/**
 * Packet sent from server to client containing the complete path data as JSON.
 * Response to a path data request, transmitted to update or initialize the client's path configuration.
 *
 * @param requestId request correlation id
 * @param json      the complete path configuration serialized as JSON
 */
public record PathDataResponsePacket(UUID requestId,
                                     String json) implements IRespondablePacket<PathDataResponsePacket> {

    /**
     * Network channel used for path data responses and server-pushed path sync.
     */
    public static final Identifier CHANNEL = ModConstants.modId("path_data_response");

    /**
     * Custom payload type used for typed Fabric networking.
     */
    public static final CustomPacketPayload.Type<PathDataResponsePacket> TYPE = new CustomPacketPayload.Type<>(CHANNEL);

    /**
     * Creates a path data response before request correlation is assigned.
     *
     * @param json serialized path data
     */
    public PathDataResponsePacket(String json) {
        this(IRespondablePacket.UNASSIGNED_REQUEST_ID, json);
    }

    public static PathDataResponsePacket read(FriendlyByteBuf buf) {
        final UUID requestId = buf.readUUID();
        final String json = buf.readUtf();
        return new PathDataResponsePacket(requestId, json);
    }

    /**
     * Creates a response with the supplied request id.
     *
     * @param requestId request correlation id
     * @return packet carrying the supplied request id
     */
    @Override
    public PathDataResponsePacket withRequestId(UUID requestId) {
        return new PathDataResponsePacket(requestId, json);
    }

    /**
     * Gets the custom payload type for this packet.
     *
     * @return this packet's payload type
     */
    @Override
    public CustomPacketPayload.@NotNull Type<PathDataResponsePacket> type() {
        return TYPE;
    }

    @Override
    public FriendlyByteBuf build() {
        FriendlyByteBuf buf = FriendlyByteBufs.create();
        buf.writeUUID(requestId);
        buf.writeUtf(json);
        return buf;
    }
}
