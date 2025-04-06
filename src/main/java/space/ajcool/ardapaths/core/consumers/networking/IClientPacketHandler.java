package space.ajcool.ardapaths.core.consumers.networking;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;

public interface IClientPacketHandler
{
    /**
     * Handle an incoming packet on the client.
     */
    void handle(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender);
}
