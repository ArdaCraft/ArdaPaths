package space.ajcool.ardapaths.core.consumers.networking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Interface for client-side packet handlers that process packets received from the server.
 */
public interface IClientPacketHandler {

    /**
     * Handle an incoming packet on the client.
     *
     * @param packet  the decoded payload
     * @param context the client networking context
     */
    @SuppressWarnings("unused")
    void receive(IPacket packet, ClientPlayNetworking.Context context);
}
