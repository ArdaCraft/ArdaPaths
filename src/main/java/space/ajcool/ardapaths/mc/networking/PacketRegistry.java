package space.ajcool.ardapaths.mc.networking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import space.ajcool.ardapaths.mc.networking.packets.client.PathDataResponsePacket;
import space.ajcool.ardapaths.mc.networking.packets.server.PathDataRequestPacket;
import space.ajcool.ardapaths.mc.networking.packets.server.PathMarkerUpdatePacket;
import space.ajcool.ardapaths.mc.networking.packets.server.PathPlayerTeleportPacket;
import space.ajcool.ardapaths.utils.McUtils;

public class PacketRegistry {
    /**
     * Client-To-Server
     */
    public static final PathMarkerUpdatePacket PATH_MARKER_UPDATE = register(new PathMarkerUpdatePacket());
    public static final PathPlayerTeleportPacket PATH_PLAYER_TELEPORT = register(new PathPlayerTeleportPacket());
    public static final PathDataRequestPacket PATH_DATA_REQUEST = register(new PathDataRequestPacket());

    /**
     * Server-To-Client
     */
    public static final PathDataResponsePacket PATH_DATA_RESPONSE = register(new PathDataResponsePacket());

    /**
     * Register a client-to-server packet handler.
     *
     * @param handler The handler to register
     */
    private static <T extends ServerPacketHandler> T register(T handler) {
        if (!McUtils.isClient()) {
            ServerPlayNetworking.registerGlobalReceiver(handler.getChannelId(), handler::handle);
        }
        return handler;
    }

    /**
     * Register a server-to-client packet handler.
     *
     * @param handler The handler to register
     */
    private static <T extends ClientPacketHandler> T register(T handler) {
        if (McUtils.isClient()) {
            ClientPlayNetworking.registerGlobalReceiver(handler.getChannelId(), (client, handler1, buf, responseSender) -> handler.handle(buf));
        }
        return handler;
    }

    public static void init() {}
}
