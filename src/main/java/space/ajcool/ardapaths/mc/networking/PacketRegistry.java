package space.ajcool.ardapaths.mc.networking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import space.ajcool.ardapaths.mc.networking.packets.server.ChapterPlayerTeleportPacket;
import space.ajcool.ardapaths.mc.networking.packets.server.ChapterStartUpdatePacket;
import space.ajcool.ardapaths.mc.networking.packets.server.ChapterUpdatePacket;
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
    public static final ChapterUpdatePacket CHAPTER_UPDATE = register(new ChapterUpdatePacket());
    public static final ChapterStartUpdatePacket CHAPTER_START_UPDATE = register(new ChapterStartUpdatePacket());
    public static final ChapterPlayerTeleportPacket CHAPTER_PLAYER_TELEPORT = register(new ChapterPlayerTeleportPacket());

    /**
     * Register a client-to-server packet handler.
     *
     * @param handler The handler to register
     */
    private static <T extends ServerPacketHandler> T register(T handler) {
        ServerPlayNetworking.registerGlobalReceiver(handler.getChannelId(), handler::handle);
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
