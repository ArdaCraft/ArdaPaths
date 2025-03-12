package space.ajcool.ardapaths.core.networking.handlers.server;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.consumers.networking.ServerPacketHandler;
import space.ajcool.ardapaths.core.data.config.server.PositionData;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterStartUpdatePacket;

public class ChapterStartUpdateHandler extends ServerPacketHandler<ChapterStartUpdatePacket> {
    public ChapterStartUpdateHandler() {
        super("path_chapter_start_update", ChapterStartUpdatePacket::read);
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, ChapterStartUpdatePacket packet, PacketSender sender) {
        final String pathId = packet.pathId();
        final String chapterId = packet.chapterId();
        final BlockPos start = packet.position();
        ArdaPaths.CONFIG.setChapterStart(pathId, chapterId, PositionData.fromBlockPos(start));
        ArdaPaths.CONFIG_MANAGER.save();
    }
}
