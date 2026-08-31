package space.ajcool.ardapaths.core.networking.handlers.server;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.consumers.networking.ServerPacketHandler;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterDeletePacket;

/**
 * Handles the deletion of a chapter from a path in the server configuration.
 * Processes incoming {@link ChapterDeletePacket} from clients and removes the specified chapter from its path.
 */
public class ChapterDeleteHandler extends ServerPacketHandler<ChapterDeletePacket>
{
    public ChapterDeleteHandler()
    {
        super(ChapterDeletePacket.CHANNEL, ChapterDeletePacket::read);
    }

    /**
     * Requires edit permission because chapter deletion mutates server path config.
     *
     * @return true because this packet changes editable path data
     */
    @Override
    protected boolean requiresEditPermission() {
        return true;
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, ChapterDeletePacket packet, PacketSender sender)
    {
        final String pathId = packet.pathId();
        final String chapterId = packet.chapterId();
        final PathData pathData = ArdaPaths.CONFIG.getPath(pathId);
        if (pathData == null)
        {
            return;
        }

        pathData.removeChapter(chapterId);
        ArdaPaths.CONFIG_MANAGER.save();
    }
}
