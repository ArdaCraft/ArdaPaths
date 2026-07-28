package space.ajcool.ardapaths.core.networking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import space.ajcool.ardapaths.core.Fabric;
import space.ajcool.ardapaths.core.consumers.networking.IServerPacketHandler;
import space.ajcool.ardapaths.core.consumers.networking.RespondablePacketHandler;
import space.ajcool.ardapaths.core.networking.handlers.server.*;

/**
 * Central registry for all network packet handlers in ArdaPaths.
 * Handlers are registered statically during class initialization and can be accessed as public constants.
 */
public class PacketRegistry {
    /**
     * Handler for player teleport requests (client to server).
     */
    public static final PlayerTeleportHandler PLAYER_TELEPORT = register(new PlayerTeleportHandler());

    /**
     * Handler for path marker updates (client to server).
     */
    public static final PathMarkerUpdateHandler PATH_MARKER_UPDATE = register(new PathMarkerUpdateHandler());

    /**
     * Handler for path marker links updates (client to server).
     */
    public static final PathMarkerLinksUpdateHandler PATH_MARKER_LINKS_UPDATE = register(new PathMarkerLinksUpdateHandler());

    /**
     * Handler for path data requests (client asks server for current path config).
     */
    public static final PathDataRequestHandler PATH_DATA_REQUEST = register(new PathDataRequestHandler());

    /**
     * Handler for path data update requests (client to server).
     */
    public static final PathDataUpdateRequestHandler PATH_DATA_UPDATE_REQUEST = register(new PathDataUpdateRequestHandler());

    /**
     * Handler for chapter updates (client to server).
     */
    public static final ChapterUpdateHandler CHAPTER_UPDATE = register(new ChapterUpdateHandler());

    /**
     * Handler for chapter deletions (client to server).
     */
    public static final ChapterDeleteHandler CHAPTER_DELETE = register(new ChapterDeleteHandler());

    /**
     * Handler for chapter start position updates (client to server).
     */
    public static final ChapterStartUpdateHandler CHAPTER_START_UPDATE = register(new ChapterStartUpdateHandler());

    /**
     * Handler for chapter start position removals (client to server).
     */
    public static final ChapterStartRemoveHandler CHAPTER_START_REMOVE = register(new ChapterStartRemoveHandler());

    /**
     * Handler for teleporting players to chapter start positions (client to server).
     */
    public static final ChapterPlayerTeleportHandler CHAPTER_PLAYER_TELEPORT = register(new ChapterPlayerTeleportHandler());

    /**
     * Handler for permission checks (client asks server if player has edit permission).
     */
    public static final ArdaPathsPermissionCheckHandler PERMISSION_CHECK = register(new ArdaPathsPermissionCheckHandler());

    /**
     * Handler for wielding the Pathfinder item (client notifies server when picked up).
     */
    public static final WieldPathfinderRequestHandler WIELD_PATHFINDER_REQUEST = register(new WieldPathfinderRequestHandler());

    /**
     * Registers a packet handler on both server and client sides if applicable.
     * For respondable handlers, also registers the response channel on the client.
     *
     * @param handler the handler to register
     * @return the handler instance
     */
    private static <T extends IServerPacketHandler<?>> T register(T handler) {
        ServerPlayNetworking.registerGlobalReceiver(handler.getChannelId(), handler::handle);
        if (Fabric.isClient() && handler instanceof RespondablePacketHandler<?, ?> responseHandler) {
            ClientPlayNetworking.registerGlobalReceiver(responseHandler.getResponseChannelId(), responseHandler::handle);
        }
        return handler;
    }

    /**
     * Initializes the packet registry by forcing class loading and handler registration.
     * This method is called during mod initialization.
     */
    public static void init() {
    }
}
