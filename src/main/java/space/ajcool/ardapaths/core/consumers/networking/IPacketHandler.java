package space.ajcool.ardapaths.core.consumers.networking;

import net.minecraft.util.Identifier;

/**
 * Base interface for packet handlers that process network packets.
 */
public interface IPacketHandler {
    /**
     * Get the channel ID for this packet handler.
     *
     * @return the channel identifier
     */
    Identifier getChannelId();
}
