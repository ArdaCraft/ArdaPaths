package space.ajcool.ardapaths.core.networking.handlers.server;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.consumers.networking.ServerPacketHandler;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterUpdatePacket;

/**
 * Handles updates to chapter data in the server configuration.
 * Processes incoming {@link ChapterUpdatePacket} from clients and persists changes to chapter name, date, index, and warp settings.
 */
public class ChapterUpdateHandler extends ServerPacketHandler<ChapterUpdatePacket>
{
    public ChapterUpdateHandler()
    {
        super(ChapterUpdatePacket.CHANNEL, ChapterUpdatePacket::read);
    }

    /**
     * Requires edit permission because chapter updates mutate server path config.
     *
     * @return true because this packet changes editable path data
     */
    @Override
    protected boolean requiresEditPermission() {
        return true;
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, ChapterUpdatePacket packet, PacketSender sender)
    {
        final String pathId = packet.pathId();
        final PathData pathData = ArdaPaths.CONFIG.getPath(pathId);
        if (pathData == null)
        {
            return;
        }

        final String chapterId = packet.chapterId();
        final String chapterName = packet.chapterName();
        final String chapterDate = packet.chapterDate();
        final int chapterIndex = packet.chapterIndex();
        final String warp = packet.warp();
        final ChapterData chapterData = new ChapterData(chapterId, chapterName, chapterDate, chapterIndex,warp);

        pathData.setChapter(chapterData);
        ArdaPaths.CONFIG_MANAGER.save();
    }
}
