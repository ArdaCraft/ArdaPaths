package space.ajcool.ardapaths.mc.networking;

import net.minecraft.network.PacketByteBuf;

public interface ClientPacketHandler extends Packet {
    /**
     * Handle an incoming packet on the client.
     * Called from the global receiver in the PacketRegistry.
     */
    void handle(PacketByteBuf buf);
}
