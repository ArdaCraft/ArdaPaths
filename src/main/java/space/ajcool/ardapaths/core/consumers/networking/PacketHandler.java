package space.ajcool.ardapaths.core.consumers.networking;

import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Abstract base class for packet handlers that provides common channel ID management.
 */
public abstract class PacketHandler<T extends IPacket> implements IPacketHandler {

    /**
     * The network channel identifier for this handler.
     */
    @Getter
    // Read by the packet codec at registration time; not accessible from a static entry point.
    @SuppressWarnings("unused")
    private final Identifier channelId;

    /**
     * Custom payload type registered for this handler.
     */
    @Getter
    // Read by the packet codec at registration time; not accessible from a static entry point.
    @SuppressWarnings("unused")
    private final CustomPacketPayload.Type<T> type;

    /**
     * Stream codec registered for this handler.
     */
    @Getter
    // Read by the packet codec at registration time; not accessible from a static entry point.
    @SuppressWarnings("unused")
    private final StreamCodec<RegistryFriendlyByteBuf, T> codec;

    /**
     * Constructs a PacketHandler with the given channel identifier.
     *
     * @param type  the custom payload type
     * @param codec the stream codec for this handler's payload
     */
    public PacketHandler(final CustomPacketPayload.Type<T> type, final StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        this.type = type;
        this.codec = codec;
        this.channelId = type.id();
    }

}
