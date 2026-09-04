package space.ajcool.ardapaths.core.consumers.networking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

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
        ClientPlayNetworking.send(packet);
    }

    /**
     * Handle an incoming packet on the server.
     *
     * @param packet  the decoded payload
     * @param context the server networking context
     */
    void receive(T packet, ServerPlayNetworking.Context context);
}
