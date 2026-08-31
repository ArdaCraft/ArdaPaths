package space.ajcool.ardapaths.core.networking.handlers.server;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.RespondablePacketHandler;
import space.ajcool.ardapaths.core.data.Json;
import space.ajcool.ardapaths.core.networking.packets.EmptyPacket;
import space.ajcool.ardapaths.core.networking.packets.client.PathDataResponsePacket;

/**
 * A packet sent from the client to the server to request path data.
 */
public class PathDataRequestHandler extends RespondablePacketHandler<EmptyPacket, PathDataResponsePacket>
{
    /**
     * Channel identifier for client path data requests.
     */
    private static final ResourceLocation REQUEST_CHANNEL = ModConstants.modId("path_data_request");

    /**
     * Creates the path data request handler.
     */
    public PathDataRequestHandler()
    {
        super(REQUEST_CHANNEL, EmptyPacket::read, PathDataResponsePacket.CHANNEL, PathDataResponsePacket::read);
    }

    @Override
    public PathDataResponsePacket handle(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, EmptyPacket packet, PacketSender sender)
    {
        String json = Json.toJson(ArdaPaths.CONFIG.getPaths());
        return new PathDataResponsePacket(json);
    }
}
