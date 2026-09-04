package space.ajcool.ardapaths.core.networking.handlers.server;

import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.Level;
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
public class ChapterPlayerTeleportHandler extends ServerPacketHandler<ChapterPlayerTeleportPacket> {

    public ChapterPlayerTeleportHandler() {
        super(ChapterPlayerTeleportPacket.TYPE, ChapterPlayerTeleportPacket::read);
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, ChapterPlayerTeleportPacket packet, PacketSender sender) {
        final String pathId = packet.pathId();
        final String chapterId = packet.chapterId();

        final Optional<String> startWarp = ArdaPaths.CONFIG.getChapterStartWarp(pathId, chapterId);
        final Runnable fallback = () -> {
            final BlockPos start = ArdaPaths.CONFIG.getChapterStartCoordinates(pathId, chapterId);
            final String dimensionId = ArdaPaths.CONFIG.getChapterStartDimension(pathId, chapterId);

            if (start != null) {
                ServerLevel destinationLevel = resolveDestinationLevel(server, dimensionId);
                if (destinationLevel == null) {
                    log.warn("Cannot teleport player {} to chapter start in unloaded dimension {}", player.getStringUUID(), dimensionId);
                    return;
                }
                player.teleportTo(destinationLevel, start.getX() + 0.5, start.getY(), start.getZ() + 0.5, player.getYRot(), player.getXRot());
            }
        };

        if (startWarp.isPresent() && Warps.isAvailable()) {
            log.info("Attempting to warp player {} at {}", player.getStringUUID(), startWarp.get());
            Warps.warpTo(server, player, startWarp.get(), fallback);
        } else {
            fallback.run();
        }
    }

    /**
     * Resolves the destination dimension for a coordinate chapter-start fallback.
     *
     * @param server      active Minecraft server
     * @param dimensionId configured dimension identifier
     * @return destination level, or null when no matching level is loaded
     */
    private ServerLevel resolveDestinationLevel(MinecraftServer server, String dimensionId) {
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimensionId));
        return server.getLevel(key);
    }
}
