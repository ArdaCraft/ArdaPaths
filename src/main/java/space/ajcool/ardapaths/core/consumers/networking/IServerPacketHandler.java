package space.ajcool.ardapaths.core.consumers.networking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/**
 * Interface for server-side packet handlers that process packets received from clients.
 * Provides default implementation for sending packets to the server.
 *
 * @param <T> the type of packet this handler processes
 */
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
     *
     * @param server the Minecraft server instance
     * @param player the player sending the packet
     * @param handler the server play network handler
     * @param buf the packet byte buffer
     * @param sender the packet sender
     */
    void handle(MinecraftServer server,
                ServerPlayer player,
                ServerGamePacketListenerImpl handler,
                FriendlyByteBuf buf,
                PacketSender sender);
}
