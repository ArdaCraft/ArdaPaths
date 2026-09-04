package space.ajcool.ardapaths.core.consumers.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Base interface for packet handlers that process network packets.
 */
public interface IPacketHandler {

    /**
     * Get the channel ID for this packet handler.
     *
     * @return the channel identifier
     */
    @SuppressWarnings("unused")
    Identifier getChannelId();

    /**
     * Gets the registered custom payload type for this handler.
     *
     * @return the custom payload type
     */
    CustomPacketPayload.Type<? extends IPacket> getType();

    /**
     * Gets the stream codec registered for this handler.
     *
     * @return the payload stream codec
     */
    StreamCodec<RegistryFriendlyByteBuf, ? extends IPacket> getCodec();
}
