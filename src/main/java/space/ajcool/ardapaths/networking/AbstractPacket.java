package space.ajcool.ardapaths.networking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.utils.McUtils;

public abstract class AbstractPacket implements ServerPacketHandler {
    private final Identifier CHANNEL_ID;

    public AbstractPacket(final String channel) {
        CHANNEL_ID = Identifier.of(ArdaPaths.MOD_ID, channel);
    }

    @Override
    public Identifier getChannelId() {
        return CHANNEL_ID;
    }

    /**
     * Send a packet to the server through the set channel.
     *
     * @param buf The packet to send
     */
    protected void sendToServer(PacketByteBuf buf) {
        if (McUtils.isClient()) {
            ClientPlayNetworking.send(CHANNEL_ID, buf);
        }
    }
}
