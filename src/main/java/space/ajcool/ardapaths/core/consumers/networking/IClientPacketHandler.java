package space.ajcool.ardapaths.core.consumers.networking;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;

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
    void handle(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender);
}
