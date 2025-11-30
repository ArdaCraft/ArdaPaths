package space.ajcool.ardapaths.core.networking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import space.ajcool.ardapaths.core.Fabric;
import space.ajcool.ardapaths.core.consumers.networking.IServerPacketHandler;
import space.ajcool.ardapaths.core.consumers.networking.RespondablePacketHandler;
import space.ajcool.ardapaths.core.networking.handlers.server.*;

public class PacketRegistry
{
    /**
     * Client-To-Server
     */
    public static final PlayerTeleportHandler PLAYER_TELEPORT = register(new PlayerTeleportHandler());
    public static final PathMarkerUpdateHandler PATH_MARKER_UPDATE = register(new PathMarkerUpdateHandler());
    public static final PathDataRequestHandler PATH_DATA_REQUEST = register(new PathDataRequestHandler());
    public static final ChapterUpdateHandler CHAPTER_UPDATE = register(new ChapterUpdateHandler());
    public static final ChapterDeleteHandler CHAPTER_DELETE = register(new ChapterDeleteHandler());
    public static final ChapterStartUpdateHandler CHAPTER_START_UPDATE = register(new ChapterStartUpdateHandler());
    public static final ChapterStartRemoveHandler CHAPTER_START_REMOVE = register(new ChapterStartRemoveHandler());
    public static final ChapterPlayerTeleportHandler CHAPTER_PLAYER_TELEPORT = register(new ChapterPlayerTeleportHandler());

    /**
     * Register a client-to-server packet handler.
     *
     * @param handler The handler to register
     */
    private static <T extends IServerPacketHandler<?>> T register(T handler)
    {
        ServerPlayNetworking.registerGlobalReceiver(handler.getChannelId(), handler::handle);
        if (Fabric.isClient() && handler instanceof RespondablePacketHandler<?, ?> responseHandler)
        {
            ClientPlayNetworking.registerGlobalReceiver(responseHandler.getResponseChannelId(), responseHandler::handle);
        }
        return handler;
    }

    public static void init()
    {
    }
}
