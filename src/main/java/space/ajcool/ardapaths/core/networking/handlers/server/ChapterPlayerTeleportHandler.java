package space.ajcool.ardapaths.core.networking.handlers.server;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.consumers.networking.ServerPacketHandler;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterPlayerTeleportPacket;

public class ChapterPlayerTeleportHandler extends ServerPacketHandler<ChapterPlayerTeleportPacket> {
    public ChapterPlayerTeleportHandler() {
        super("chapter_player_teleport", ChapterPlayerTeleportPacket::read);
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, ChapterPlayerTeleportPacket packet, PacketSender sender) {
        final String pathId = packet.pathId();
        final String chapterId = packet.chapterId();
        final BlockPos start = ArdaPaths.CONFIG.getChapterStart(pathId, chapterId);

        if (start != null) {
            server.execute(() -> player.requestTeleport(start.getX() + 0.5, start.getY(), start.getZ() + 0.5));
        }
    }
}
