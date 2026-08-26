package space.ajcool.ardapaths.core.networking;

import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.Fabric;
import space.ajcool.ardapaths.core.consumers.networking.IServerPacketHandler;
import space.ajcool.ardapaths.core.consumers.networking.RespondablePacketHandler;
import space.ajcool.ardapaths.core.data.Json;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.networking.handlers.server.*;
import space.ajcool.ardapaths.core.networking.packets.client.PathDataResponsePacket;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Central registry for all network packet handlers in ArdaPaths.
 * Handlers are registered statically during class initialization and can be accessed as public constants.
 */
public class PacketRegistry {
    /**
     * Server-to-client channel used to push path data after server-side restore.
     */
    private static final Identifier PATH_DATA_SYNC_CHANNEL = new Identifier(ArdaPaths.MOD_ID, "path_data_sync");

    /**
     * Handler for player teleport requests (client to server).
     */
    public static final PlayerTeleportHandler PLAYER_TELEPORT = register(new PlayerTeleportHandler());

    /**
     * Handler for path marker updates (client to server).
     */
    public static final PathMarkerUpdateHandler PATH_MARKER_UPDATE = register(new PathMarkerUpdateHandler());

    /**
     * Handler for server-computed marker time progression requests.
     */
    public static final MarkerTimeSpreadHandler MARKER_TIME_SPREAD = register(new MarkerTimeSpreadHandler());

    /**
     * Handler for clearing environment data across selected chapter markers.
     */
    public static final MarkerBulkClearHandler MARKER_BULK_CLEAR = register(new MarkerBulkClearHandler());

    /**
     * Handler for server-resolved chapter marker list requests.
     */
    public static final ChapterPathMarkersHandler CHAPTER_PATH_MARKERS = register(new ChapterPathMarkersHandler());

    /**
     * Handler for path marker data resolved server-side for markers that are not loaded on the client.
     */
    public static final PathMarkerRemoteDataHandler PATH_MARKER_REMOTE_DATA = register(new PathMarkerRemoteDataHandler());

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
     * Handler for server-executed marker actions triggered during trail traversal.
     */
    public static final MarkerActionTriggerHandler MARKER_ACTION_TRIGGER = register(new MarkerActionTriggerHandler());

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
     * @param <T> the type of server packet handler
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
        if (Fabric.isClient()) {
            registerPathDataSyncClient();
        }
    }

    /**
     * Sends the current server path data to every connected client.
     *
     * @param server the Minecraft server instance
     */
    public static void syncPathDataToClients(MinecraftServer server) {
        String json = Json.toJson(ArdaPaths.CONFIG.getPaths());
        PathDataResponsePacket packet = new PathDataResponsePacket(json);

        for (var player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, PATH_DATA_SYNC_CHANNEL, packet.build());
        }
    }

    /**
     * Registers the client receiver for server-pushed path data.
     */
    private static void registerPathDataSyncClient() {
        ClientPlayNetworking.registerGlobalReceiver(PATH_DATA_SYNC_CHANNEL, (client, handler, buf, sender) -> {
            PathDataResponsePacket packet = PathDataResponsePacket.read(buf);
            client.execute(() -> {
                Type listType = new TypeToken<ArrayList<PathData>>() {
                }.getType();

                List<PathData> paths = Json.fromJson(packet.json(), listType);
                if (paths != null) {
                    ArdaPathsClient.CONFIG_MANAGER.onPathData(paths);
                }
            });
        });
    }
}
