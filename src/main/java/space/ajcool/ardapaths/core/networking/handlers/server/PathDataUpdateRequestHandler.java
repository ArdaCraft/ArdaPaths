package space.ajcool.ardapaths.core.networking.handlers.server;

import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.consumers.networking.ServerPacketHandler;
import space.ajcool.ardapaths.core.data.config.shared.Color;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.networking.packets.server.PathDataUpdatePacket;

/**
 * A packet sent from the client to the server to request path data.
 */
@Slf4j(topic = "ardapaths")
public class PathDataUpdateRequestHandler extends ServerPacketHandler<PathDataUpdatePacket>
{
    public PathDataUpdateRequestHandler()
    {
        super(PathDataUpdatePacket.CHANNEL, PathDataUpdatePacket::read);
    }

    /**
     * Requires edit permission because path data updates mutate server path config.
     *
     * @return true because this packet changes editable path data
     */
    @Override
    protected boolean requiresEditPermission() {
        return true;
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, PathDataUpdatePacket packet, PacketSender sender)
    {
        final String pathId = packet.id();
        final String name = packet.name();
        final int primaryColor = packet.primaryColor();
        final int secondaryColor = packet.secondaryColor();
        final int tertiaryColor = packet.tertiaryColor();

        PathData pathData = ArdaPaths.CONFIG.getPath(pathId);

        if (pathData != null) {
            log.info("Updating path data for path ID: {}, name {}", pathId, name);

            pathData.setName(name);
            pathData.setPrimaryColor(Color.fromHex(primaryColor));
            pathData.setSecondaryColor(Color.fromHex(secondaryColor));
            pathData.setTertiaryColor(Color.fromHex(tertiaryColor));

            ArdaPaths.CONFIG_MANAGER.save();
        } else {
            log.warn("No path found with ID: {}", pathId);
        }
    }
}
