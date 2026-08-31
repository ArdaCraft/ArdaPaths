package space.ajcool.ardapaths.core.networking.packets;

import space.ajcool.ardapaths.core.consumers.networking.IRespondablePacket;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

/**
 * A packet with no data payload, used for simple request/response patterns.
 * Useful for notifications that don't require any additional information.
 */
public record EmptyPacket(UUID requestId) implements IRespondablePacket<EmptyPacket> {
    /**
     * Creates an empty packet before request correlation is assigned.
     */
    public EmptyPacket() {
        this(IRespondablePacket.UNASSIGNED_REQUEST_ID);
    }

    /**
     * Deserializes an EmptyPacket from a PacketByteBuf.
     *
     * @param buf the buffer containing the request id
     * @return a new EmptyPacket instance
     */
    public static EmptyPacket read(FriendlyByteBuf buf) {
        return new EmptyPacket(buf.readUUID());
    }

    /**
     * Builds the packet into a fresh PacketByteBuf containing only the request id.
     *
     * @return packet data buffer
     */
    @Override
    public FriendlyByteBuf build() {
        FriendlyByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
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
        return new EmptyPacket(requestId);
    }
}
