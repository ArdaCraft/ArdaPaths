package space.ajcool.ardapaths.core.networking.handlers.server;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.consumers.networking.ServerPacketHandler;
import space.ajcool.ardapaths.core.data.config.shared.PositionData;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterStartRemovePacket;

/**
 * Handles the removal of a chapter start position from the server configuration.
 * Processes incoming {@link ChapterStartRemovePacket} from clients and updates the server-side config.
 */
public class ChapterStartRemoveHandler extends ServerPacketHandler<ChapterStartRemovePacket> {

    public ChapterStartRemoveHandler() {
        super(ChapterStartRemovePacket.TYPE, ChapterStartRemovePacket::read);
    }

    /**
     * Requires edit permission because chapter start removal mutates server path config.
     *
     * @return true because this packet changes editable path data
     */
    @Override
    protected boolean requiresEditPermission() {
        return true;
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, ChapterStartRemovePacket packet, PacketSender sender) {
        final String pathId = packet.pathId();
        final String chapterId = packet.chapterId();
        PositionData requestedPosition = PositionData.fromBlockPos(packet.position());
        if (ArdaPaths.CONFIG.removeChapterStart(pathId, chapterId, requestedPosition)) {
            ArdaPaths.CONFIG_MANAGER.save();
        }
    }
}
