package space.ajcool.ardapaths.mc.networking;

import net.minecraft.util.Identifier;
import space.ajcool.ardapaths.ArdaPaths;

public abstract class ClientPacket implements ClientPacketHandler {
    private final Identifier CHANNEL_ID;

    public ClientPacket(final String channel) {
        CHANNEL_ID = Identifier.of(ArdaPaths.MOD_ID, channel);
    }

    @Override
    public Identifier getChannelId() {
        return CHANNEL_ID;
    }
}
