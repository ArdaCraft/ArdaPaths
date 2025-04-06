package space.ajcool.ardapaths.core.consumers.networking;

import net.minecraft.util.Identifier;
import space.ajcool.ardapaths.ArdaPaths;

public abstract class PacketHandler implements IPacketHandler
{
    private final Identifier channelId;

    public PacketHandler(final String channelId)
    {
        this.channelId = Identifier.of(ArdaPaths.MOD_ID, channelId);
    }

    @Override
    public Identifier getChannelId()
    {
        return channelId;
    }
}
