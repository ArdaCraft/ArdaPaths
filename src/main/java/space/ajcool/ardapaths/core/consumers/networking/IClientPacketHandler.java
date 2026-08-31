package space.ajcool.ardapaths.core.consumers.networking;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Interface for client-side packet handlers that process packets received from the server.
 */
public interface IClientPacketHandler {
    /**
     * Handle an incoming packet on the client.
     *
     * @param client the Minecraft client instance
     * @param handler the client play network handler
     * @param buf the packet byte buffer
     * @param sender the packet sender
     */
    void handle(Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender sender);
}
