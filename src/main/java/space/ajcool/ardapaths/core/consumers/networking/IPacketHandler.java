package space.ajcool.ardapaths.core.consumers.networking;

import net.minecraft.resources.ResourceLocation;

/**
 * Base interface for packet handlers that process network packets.
 */
public interface IPacketHandler {
    /**
     * Get the channel ID for this packet handler.
     *
     * @return the channel identifier
     */
    ResourceLocation getChannelId();
}
