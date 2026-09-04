package space.ajcool.ardapaths.core.networking.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.IRespondablePacket;

import java.util.UUID;
import java.util.function.Function;

/**
 * A packet with no data payload, used for simple request/response patterns.
 * Useful for notifications that don't require any additional information.
 */
public record EmptyPacket(UUID requestId,
                          CustomPacketPayload.Type<EmptyPacket> packetType) implements IRespondablePacket<EmptyPacket> {

    /**
     * Network channel used for path data requests.
     */
    public static final Identifier PATH_DATA_REQUEST_CHANNEL = ModConstants.modId("path_data_request");

    /**
     * Payload type used for path data requests.
     */
    public static final CustomPacketPayload.Type<EmptyPacket> PATH_DATA_REQUEST_TYPE = new CustomPacketPayload.Type<>(PATH_DATA_REQUEST_CHANNEL);

    /**
     * Network channel used for permission check requests.
     */
    public static final Identifier PERMISSION_CHECK_CHANNEL = ModConstants.modId("ardapaths_permission_check_request");

    /**
     * Payload type used for permission check requests.
     */
    public static final CustomPacketPayload.Type<EmptyPacket> PERMISSION_CHECK_TYPE = new CustomPacketPayload.Type<>(PERMISSION_CHECK_CHANNEL);

    /**
     * Network channel used for pathfinder wield requests and responses.
     */
    public static final Identifier WIELD_PATHFINDER_CHANNEL = ModConstants.modId("wield_pathfinder_request_channel");

    /**
     * Payload type used for pathfinder wield requests and responses.
     */
    public static final CustomPacketPayload.Type<EmptyPacket> WIELD_PATHFINDER_TYPE = new CustomPacketPayload.Type<>(WIELD_PATHFINDER_CHANNEL);

    /**
     * Creates an empty packet before request correlation is assigned.
     */
    public EmptyPacket() {
        this(IRespondablePacket.UNASSIGNED_REQUEST_ID, PATH_DATA_REQUEST_TYPE);
    }

    /**
     * Creates an empty packet for the supplied payload type before request correlation is assigned.
     *
     * @param packetType payload type this packet should report
     */
    public EmptyPacket(CustomPacketPayload.Type<EmptyPacket> packetType) {
        this(IRespondablePacket.UNASSIGNED_REQUEST_ID, packetType);
    }

    /**
     * Deserializes an EmptyPacket from a friendly byte buffer using the default request type.
     *
     * @param buf the buffer containing the request id
     * @return a new EmptyPacket instance
     */
    public static EmptyPacket read(FriendlyByteBuf buf) {
        return reader(PATH_DATA_REQUEST_TYPE).apply(buf);
    }

    /**
     * Creates an empty packet reader bound to a specific payload type.
     *
     * @param packetType payload type assigned to decoded packets
     * @return a reader for empty packets of that payload type
     */
    public static Function<FriendlyByteBuf, EmptyPacket> reader(CustomPacketPayload.Type<EmptyPacket> packetType) {
        return buf -> new EmptyPacket(buf.readUUID(), packetType);
    }

    /**
     * Builds the packet into a fresh friendly byte buffer containing only the request id.
     *
     * @return packet data buffer
     */
    @Override
    public FriendlyByteBuf build() {
        FriendlyByteBuf buf = net.fabricmc.fabric.api.networking.v1.FriendlyByteBufs.create();
        buf.writeUUID(requestId);
        return buf;
    }

    /**
     * Creates an empty packet with the supplied request id.
     *
     * @param requestId request correlation id
     * @return packet carrying the supplied request id
     */
    @Override
    public EmptyPacket withRequestId(UUID requestId) {
        return new EmptyPacket(requestId, packetType);
    }

    /**
     * Gets the custom payload type for this packet.
     *
     * @return this packet's payload type
     */
    @Override
    public CustomPacketPayload.@NotNull Type<EmptyPacket> type() {
        return packetType;
    }
}
