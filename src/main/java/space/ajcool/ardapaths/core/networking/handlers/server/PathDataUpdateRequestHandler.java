package space.ajcool.ardapaths.core.networking.handlers.server;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.consumers.networking.ServerPacketHandler;
import space.ajcool.ardapaths.core.data.config.shared.Color;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.networking.packets.server.PathDataUpdatePacket;

/**
 * A packet sent from the client to the server to request path data.
 */
public class PathDataUpdateRequestHandler extends ServerPacketHandler<PathDataUpdatePacket>
{
    public PathDataUpdateRequestHandler()
    {
        super("path_data_update_request", PathDataUpdatePacket::read);
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PathDataUpdatePacket packet, PacketSender sender)
    {
        final String pathId = packet.id();
        final String name = packet.name();
        final int primaryColor = packet.primaryColor();
        final int secondaryColor = packet.secondaryColor();
        final int tertiaryColor = packet.tertiaryColor();

        PathData pathData = ArdaPaths.CONFIG.getPath(pathId);

        if (pathData != null) {
            ArdaPaths.LOGGER.info("Updating path data for path ID: {}, name {}", pathId, name);

            pathData.setName(name);
            pathData.setPrimaryColor(Color.fromHex(primaryColor));
            pathData.setSecondaryColor(Color.fromHex(secondaryColor));
            pathData.setTertiaryColor(Color.fromHex(tertiaryColor));

            ArdaPaths.CONFIG_MANAGER.save();
        } else {
            ArdaPaths.LOGGER.warn("No path found with ID: {}", pathId);
        }
    }
}
