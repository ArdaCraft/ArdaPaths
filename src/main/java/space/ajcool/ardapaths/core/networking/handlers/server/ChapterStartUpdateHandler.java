package space.ajcool.ardapaths.core.networking.handlers.server;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.consumers.networking.ServerPacketHandler;
import space.ajcool.ardapaths.core.data.config.server.PositionData;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterStartUpdatePacket;

/**
 * Handles the update of a chapter start position in the server configuration.
 * Processes incoming {@link ChapterStartUpdatePacket} from clients and persists the new chapter start location.
 */
public class ChapterStartUpdateHandler extends ServerPacketHandler<ChapterStartUpdatePacket>
{
    public ChapterStartUpdateHandler()
    {
        super(ChapterStartUpdatePacket.CHANNEL, ChapterStartUpdatePacket::read);
    }

    /**
     * Requires edit permission because chapter start updates mutate server path config.
     *
     * @return true because this packet changes editable path data
     */
    @Override
    protected boolean requiresEditPermission() {
        return true;
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, ChapterStartUpdatePacket packet, PacketSender sender)
    {
        final String pathId = packet.pathId();
        final String chapterId = packet.chapterId();
        final BlockPos start = packet.position();
        ArdaPaths.CONFIG.setChapterStart(pathId, chapterId, PositionData.fromBlockPos(start));
        ArdaPaths.CONFIG_MANAGER.save();
    }
}
