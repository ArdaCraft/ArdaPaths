package space.ajcool.ardapaths.core.consumers.networking;

import lombok.Getter;
import net.minecraft.resources.ResourceLocation;

/**
 * Abstract base class for packet handlers that provides common channel ID management.
 */
public abstract class PacketHandler implements IPacketHandler {
    /**
     * The network channel identifier for this handler.
     */
    @Getter
    private final ResourceLocation channelId;

    /**
     * Constructs a PacketHandler with the given channel identifier.
     *
     * @param channelId the network channel identifier
     */
    public PacketHandler(final ResourceLocation channelId) {
        this.channelId = channelId;
    }

}
