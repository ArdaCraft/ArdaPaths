package space.ajcool.ardapaths.core.networking.packets.client;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.IRespondablePacket;

import java.util.UUID;

/**
 * Packet sent from server to client containing the complete path data as JSON.
 * Response to a path data request, transmitted to update or initialize the client's path configuration.
 * @param requestId request correlation id
 * @param json the complete path configuration serialized as JSON
 */
public record PathDataResponsePacket(UUID requestId, String json) implements IRespondablePacket<PathDataResponsePacket>
{
    /**
     * Network channel used for path data responses and server-pushed path sync.
     */
    public static final ResourceLocation CHANNEL = ModConstants.modId("path_data_response");

    /**
     * Creates a path data response before request correlation is assigned.
     *
     * @param json serialized path data
     */
    public PathDataResponsePacket(String json) {
        this(IRespondablePacket.UNASSIGNED_REQUEST_ID, json);
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

    @Override
    public FriendlyByteBuf build()
    {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUUID(requestId);
        buf.writeUtf(json);
        return buf;
    }

    public static PathDataResponsePacket read(FriendlyByteBuf buf)
    {
        final UUID requestId = buf.readUUID();
        final String json = buf.readUtf();
        return new PathDataResponsePacket(requestId, json);
    }
}
