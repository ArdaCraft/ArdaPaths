package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.IRespondablePacket;

import java.util.UUID;

/**
 * Packet sent from client to server to request one path marker that is not currently loaded on the client.
 *
 * @param requestId request correlation id
 * @param packedPos packed position of the requested path marker
 */
public record PathMarkerRemoteDataPacket(UUID requestId, long packedPos) implements IRespondablePacket<PathMarkerRemoteDataPacket> {
    /**
     * Network channel used for remote path marker data requests.
     */
    public static final ResourceLocation CHANNEL = ModConstants.modId("path_marker_remote_data");

    /**
     * Creates a remote marker request before request correlation is assigned.
     *
     * @param packedPos packed position of the requested path marker
     */
    public PathMarkerRemoteDataPacket(long packedPos) {
        this(IRespondablePacket.UNASSIGNED_REQUEST_ID, packedPos);
    }

    /**
     * Creates a request with the supplied request id.
     *
     * @param requestId request correlation id
     * @return packet carrying the supplied request id
     */
    @Override
    public PathMarkerRemoteDataPacket withRequestId(UUID requestId) {
        return new PathMarkerRemoteDataPacket(requestId, packedPos);
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
        buf.writeLong(packedPos);
        return buf;
    }

    /**
     * Reads a remote path marker data request packet from a network buffer.
     *
     * @param buf packet data buffer
     * @return decoded packet
     */
    public static PathMarkerRemoteDataPacket read(FriendlyByteBuf buf) {
        return new PathMarkerRemoteDataPacket(buf.readUUID(), buf.readLong());
    }
}
