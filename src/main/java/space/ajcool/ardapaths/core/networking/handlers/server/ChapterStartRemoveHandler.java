package space.ajcool.ardapaths.core.networking.handlers.server;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.consumers.networking.ServerPacketHandler;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterStartRemovePacket;

public class ChapterStartRemoveHandler extends ServerPacketHandler<ChapterStartRemovePacket>
{
    public ChapterStartRemoveHandler()
    {
        super("chapter_start_remove", ChapterStartRemovePacket::read);
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, ChapterStartRemovePacket packet, PacketSender sender)
    {
        final String pathId = packet.pathId();
        final String chapterId = packet.chapterId();
        ArdaPaths.CONFIG.removeChapterStart(pathId, chapterId);
        ArdaPaths.CONFIG_MANAGER.save();
    }
}
