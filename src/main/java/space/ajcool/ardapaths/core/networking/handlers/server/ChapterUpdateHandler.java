package space.ajcool.ardapaths.core.networking.handlers.server;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.consumers.networking.ServerPacketHandler;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterUpdatePacket;

public class ChapterUpdateHandler extends ServerPacketHandler<ChapterUpdatePacket> {
    public ChapterUpdateHandler() {
        super("path_chapter_update", ChapterUpdatePacket::read);
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, ChapterUpdatePacket packet, PacketSender sender) {
        final String pathId = packet.pathId();
        final PathData pathData = ArdaPaths.CONFIG.getPath(pathId);
        if (pathData == null) {
            return;
        }

        final String chapterId = packet.chapterId();
        final String chapterName = packet.chapterName();
        final String chapterDate = packet.chapterDate();
        final ChapterData chapterData = new ChapterData(chapterId, chapterName, chapterDate);
        pathData.setChapter(chapterData);
        ArdaPaths.CONFIG_MANAGER.save();
    }
}
