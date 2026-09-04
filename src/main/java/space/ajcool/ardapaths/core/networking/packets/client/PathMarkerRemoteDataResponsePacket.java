package space.ajcool.ardapaths.core.networking.packets.client;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.IRespondablePacket;
import space.ajcool.ardapaths.core.data.PathMarkerRemoteDataStatus;

import java.util.UUID;

/**
 * Packet sent from server to client with data for one path marker that is not currently loaded on the client.
 *
 * @param requestId request correlation id
 * @param status    status code describing the result
 * @param packedPos packed position of the requested path marker
 * @param data      path marker NBT data when the request succeeds
 */
public record PathMarkerRemoteDataResponsePacket(UUID requestId, PathMarkerRemoteDataStatus status, long packedPos,
                                                 CompoundTag data) implements IRespondablePacket<PathMarkerRemoteDataResponsePacket> {

    /**
     * Network channel used for remote marker data responses.
     */
    public static final ResourceLocation CHANNEL = ModConstants.modId("path_marker_remote_data_response");

    /**
     * Custom payload type used for typed Fabric networking.
     */
    public static final CustomPacketPayload.Type<PathMarkerRemoteDataResponsePacket> TYPE = new CustomPacketPayload.Type<>(CHANNEL);

    /**
     * Creates a remote marker response before request correlation is assigned.
     *
     * @param status    status code describing the result
     * @param packedPos packed position of the requested path marker
     * @param data      path marker NBT data when the request succeeds
     */
    public PathMarkerRemoteDataResponsePacket(PathMarkerRemoteDataStatus status, long packedPos, CompoundTag data) {
        this(IRespondablePacket.UNASSIGNED_REQUEST_ID, status, packedPos, data);
    }

    /**
     * Reads a remote path marker data response packet from a network buffer.
     *
     * @param buf packet data buffer
     * @return decoded packet
     */
    public static PathMarkerRemoteDataResponsePacket read(FriendlyByteBuf buf) {
        return new PathMarkerRemoteDataResponsePacket(
                buf.readUUID(),
                buf.readEnum(PathMarkerRemoteDataStatus.class),
                buf.readLong(),
                buf.readNbt()
        );
    }

    /**
     * Creates a response with the supplied request id.
     *
     * @param requestId request correlation id
     * @return packet carrying the supplied request id
     */
    @Override
    public PathMarkerRemoteDataResponsePacket withRequestId(UUID requestId) {
        return new PathMarkerRemoteDataResponsePacket(requestId, status, packedPos, data);
    }

    /**
     * Gets the custom payload type for this packet.
     *
     * @return this packet's payload type
     */
    @Override
    public CustomPacketPayload.@NotNull Type<PathMarkerRemoteDataResponsePacket> type() {
        return TYPE;
    }

    @Override
    public FriendlyByteBuf build() {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUUID(requestId);
        buf.writeEnum(status);
        buf.writeLong(packedPos);
        buf.writeNbt(data);
        return buf;
    }
}
