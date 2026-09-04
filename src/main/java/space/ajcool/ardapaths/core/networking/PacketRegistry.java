package space.ajcool.ardapaths.core.networking;

import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.Fabric;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;
import space.ajcool.ardapaths.core.consumers.networking.IServerPacketHandler;
import space.ajcool.ardapaths.core.consumers.networking.RespondablePacketHandler;
import space.ajcool.ardapaths.core.data.Json;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.networking.handlers.server.*;
import space.ajcool.ardapaths.core.networking.packets.client.PathDataSyncPacket;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

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
     * @param <T>     the type of server packet handler
     * @param handler the handler to register
     * @return the handler instance
     */
    private static <T extends IServerPacketHandler<?>> T register(T handler) {
        registerServerboundPayload(handler);
        registerServerReceiver(handler);

        if (handler instanceof RespondablePacketHandler<?, ?> responseHandler) {
            registerClientboundResponsePayload(responseHandler);
            if (Fabric.isClient()) {
                registerClientResponseReceiver(responseHandler);
            }
        }
        return handler;
    }

    /**
     * Registers a client-to-server payload type.
     *
     * @param handler handler for the payload
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerServerboundPayload(IServerPacketHandler<?> handler) {
        PayloadTypeRegistry.serverboundPlay().register((CustomPacketPayload.Type) handler.getType(), (StreamCodec) handler.getCodec());
    }

    /**
     * Registers the server receiver for a client-to-server payload.
     *
     * @param handler handler for the payload
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerServerReceiver(IServerPacketHandler<?> handler) {
        ServerPlayNetworking.registerGlobalReceiver((CustomPacketPayload.Type) handler.getType(),
                (packet, context) -> ((IServerPacketHandler) handler).receive((IPacket) packet, context));
    }

    /**
     * Registers a server-to-client response payload type.
     *
     * @param handler respondable handler owning the response channel
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerClientboundResponsePayload(RespondablePacketHandler<?, ?> handler) {
        PayloadTypeRegistry.clientboundPlay().register((CustomPacketPayload.Type) handler.getResponseType(), (StreamCodec) handler.getResponseCodec());
    }

    /**
     * Registers the client receiver for a server-to-client response payload.
     *
     * @param handler respondable handler owning the response channel
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerClientResponseReceiver(RespondablePacketHandler<?, ?> handler) {
        ClientPlayNetworking.registerGlobalReceiver((CustomPacketPayload.Type) handler.getResponseType(),
                (packet, context) -> handler.receive((IPacket) packet, context));
    }

    /**
     * Initializes the packet registry by forcing class loading and handler registration.
     * This method is called during mod initialization.
     */
    public static void init() {
        registerPathDataSyncPayload();
        if (Fabric.isClient()) {
            registerPathDataSyncReceiver();
        }
    }

    /**
     * Registers the server-to-client payload type for server-pushed path data.
     */
    private static void registerPathDataSyncPayload() {
        PayloadTypeRegistry.clientboundPlay().register(PathDataSyncPacket.TYPE, IPacket.codec(PathDataSyncPacket::read));
    }

    /**
     * Registers the client receiver for server-pushed path data.
     */
    @SuppressWarnings("resource")
    private static void registerPathDataSyncReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(PathDataSyncPacket.TYPE, (packet, context) -> context.client().execute(() -> {
            Type listType = new TypeToken<ArrayList<PathData>>() {
            }.getType();

            List<PathData> paths = Json.fromJson(packet.json(), listType);
            if (paths != null) {
                ArdaPathsClient.CONFIG_MANAGER.onPathData(paths);
            }
        }));
    }

    /**
     * Sends the current server path data to every connected client.
     *
     * @param server the Minecraft server instance
     */
    public static void syncPathDataToClients(MinecraftServer server) {
        String json = Json.toJson(ArdaPaths.CONFIG.getPaths());
        PathDataSyncPacket packet = new PathDataSyncPacket(json);

        for (var player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, packet);
        }
    }
}
