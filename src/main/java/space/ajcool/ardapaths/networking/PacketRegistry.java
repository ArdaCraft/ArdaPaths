package space.ajcool.ardapaths.networking;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import space.ajcool.ardapaths.networking.packets.PathMarkerUpdatePacket;
import space.ajcool.ardapaths.networking.packets.PathPlayerTeleportPacket;

public class PacketRegistry {
    public static final PathMarkerUpdatePacket PATH_MARKER_UPDATE = register(new PathMarkerUpdatePacket());
    public static final PathPlayerTeleportPacket PATH_PLAYER_TELEPORT = register(new PathPlayerTeleportPacket());

    /**
     * Register a packet handler.
     *
     * @param handler The handler to register
     */
    private static <T extends ServerPacketHandler> T register(T handler) {
        ServerPlayNetworking.registerGlobalReceiver(handler.getChannelId(), handler::handle);
        return handler;
    }

    public static void init() {}
}
