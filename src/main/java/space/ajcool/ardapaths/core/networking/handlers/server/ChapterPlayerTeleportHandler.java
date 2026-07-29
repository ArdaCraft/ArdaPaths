package space.ajcool.ardapaths.core.networking.handlers.server;

import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.consumers.networking.ServerPacketHandler;
import space.ajcool.ardapaths.core.integration.Warps;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterPlayerTeleportPacket;

import java.util.Optional;

/**
 * Handles teleporting a player to a chapter's start position when requested from the client.
 * Uses HuskHomes warp if available and configured, otherwise teleports to the chapter start coordinates directly.
 * Processes incoming {@link ChapterPlayerTeleportPacket} from clients.
 */
@Slf4j(topic = "ardapaths")
public class ChapterPlayerTeleportHandler extends ServerPacketHandler<ChapterPlayerTeleportPacket>
{
    public ChapterPlayerTeleportHandler()
    {
        super("chapter_player_teleport", ChapterPlayerTeleportPacket::read);
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, ChapterPlayerTeleportPacket packet, PacketSender sender)
    {
        server.execute(() ->
        {
            final String pathId = packet.pathId();
            final String chapterId = packet.chapterId();

            final Optional<String> startWarp = ArdaPaths.CONFIG.getChapterStartWarp(pathId, chapterId);
            final Runnable fallback = () -> {
                final BlockPos start = ArdaPaths.CONFIG.getChapterStartCoordinates(pathId, chapterId);

                if (start != null)
                {
                    player.requestTeleport(start.getX() + 0.5, start.getY(), start.getZ() + 0.5);
                }
            };

            if (startWarp.isPresent() && Warps.isAvailable()) {
                log.info("Attempting to warp player {} at {}", player.getUuidAsString(), startWarp.get());
                Warps.warpTo(server, player, startWarp.get(), fallback);
            } else {
                fallback.run();
            }
        });
    }
}
