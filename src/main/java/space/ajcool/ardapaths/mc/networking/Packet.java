package space.ajcool.ardapaths.mc.networking;

import net.minecraft.util.Identifier;

public interface Packet {
    /**
     * @return The channel name (Identifier) this handler listens to.
     */
    Identifier getChannelId();
}
