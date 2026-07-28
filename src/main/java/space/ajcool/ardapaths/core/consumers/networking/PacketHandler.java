package space.ajcool.ardapaths.core.consumers.networking;

import lombok.Getter;
import net.minecraft.util.Identifier;
import space.ajcool.ardapaths.ArdaPaths;

/**
 * Abstract base class for packet handlers that provides common channel ID management.
 */
public abstract class PacketHandler implements IPacketHandler {
    /**
     * The network channel identifier for this handler.
     */
    @Getter
    private final Identifier channelId;

    /**
     * Constructs a PacketHandler with the given channel name.
     * The channel ID is constructed as "ardapaths:&lt;channelId&gt;".
     *
     * @param channelId the local channel name
     */
    public PacketHandler(final String channelId) {
        this.channelId = Identifier.of(ArdaPaths.MOD_ID, channelId);
    }

}
