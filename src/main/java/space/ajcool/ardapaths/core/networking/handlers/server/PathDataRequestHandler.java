package space.ajcool.ardapaths.core.networking.handlers.server;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.consumers.networking.RespondablePacketHandler;
import space.ajcool.ardapaths.core.data.Json;
import space.ajcool.ardapaths.core.networking.packets.EmptyPacket;
import space.ajcool.ardapaths.core.networking.packets.client.PathDataResponsePacket;

/**
 * A packet sent from the client to the server to request path data.
 */
public class PathDataRequestHandler extends RespondablePacketHandler<EmptyPacket, PathDataResponsePacket>
{
    public PathDataRequestHandler()
    {
        super("path_data_request", EmptyPacket::read, "path_data_response", PathDataResponsePacket::read);
    }

    @Override
    public PathDataResponsePacket handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, EmptyPacket packet, PacketSender sender)
    {
        String json = Json.toJson(ArdaPaths.CONFIG.getPaths());
        return new PathDataResponsePacket(json);
    }
}
