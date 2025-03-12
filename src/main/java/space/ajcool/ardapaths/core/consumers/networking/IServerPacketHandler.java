package space.ajcool.ardapaths.core.consumers.networking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

public interface IServerPacketHandler<T extends IPacket> extends IPacketHandler {
    /**
     * Send a packet to the server.
     *
     * @param packet The packet to send
     */
    default void send(final T packet) {
        ClientPlayNetworking.send(getChannelId(), packet.build());
    }

    /**
     * Handle an incoming packet on the server.
     */
    void handle(MinecraftServer server,
                ServerPlayerEntity player,
                ServerPlayNetworkHandler handler,
                PacketByteBuf buf,
                PacketSender sender);
}
