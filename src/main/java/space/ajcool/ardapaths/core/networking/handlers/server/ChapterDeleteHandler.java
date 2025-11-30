package space.ajcool.ardapaths.core.networking.handlers.server;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.consumers.networking.ServerPacketHandler;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterDeletePacket;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterUpdatePacket;

public class ChapterDeleteHandler extends ServerPacketHandler<ChapterDeletePacket>
{
    public ChapterDeleteHandler()
    {
        super("path_chapter_delete", ChapterDeletePacket::read);
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, ChapterDeletePacket packet, PacketSender sender)
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
