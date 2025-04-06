package space.ajcool.ardapaths.core.consumers.networking;

import net.minecraft.util.Identifier;

public interface IPacketHandler
{
    /**
     * Get the channel ID for this packet handler.
     */
    Identifier getChannelId();
}
