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
import space.ajcool.ardapaths.core.backup.BackupJobRunner;
import space.ajcool.ardapaths.core.consumers.networking.ServerPacketHandler;
import space.ajcool.ardapaths.core.data.config.shared.PositionData;
import space.ajcool.ardapaths.core.markers.ChapterStartLocator;
import space.ajcool.ardapaths.core.markers.MarkerResolver;
import space.ajcool.ardapaths.core.markers.MarkerResolver.ResolvedMarker;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterStartUpdatePacket;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handles the update of a chapter start position in the server configuration.
 * Processes incoming {@link ChapterStartUpdatePacket} from clients and persists the new chapter start location.
 */
@Slf4j(topic = "ardapaths")
public class ChapterStartUpdateHandler extends ServerPacketHandler<ChapterStartUpdatePacket> {

    /**
     * Creates the server-side chapter-start update handler.
     */
    public ChapterStartUpdateHandler() {
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

    @SuppressWarnings("resource")
    @Override
    public void handle(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, ChapterStartUpdatePacket packet, PacketSender sender) {
        final String pathId = packet.pathId();
        final String chapterId = packet.chapterId();
        final BlockPos start = packet.position();
        final String playerDimension = player.level().dimension().location().toString();
        final List<String> preferredDimensions = preferredDimensions(pathId, chapterId, playerDimension);

        BackupJobRunner.submitMarkerWork(server, gate -> {
            Optional<ResolvedMarker> located = ChapterStartLocator.locateMarker(server, start, preferredDimensions, gate);
            if (located.isEmpty()) {
                log.warn("Cannot set chapter start for {}:{} at {}; no path marker was found in known dimensions", pathId, chapterId, start);
                return null;
            }

            ResolvedMarker marker = located.get();
            gate.run(() -> applyChapterStart(server, pathId, chapterId, start, marker.dimensionId(), packet.onlyIfUnset()));
            return null;
        });
    }

    /**
     * Builds the dimension search preference for chapter-start marker lookup.
     *
     * @param pathId          path identifier
     * @param chapterId       chapter identifier
     * @param playerDimension dimension containing the player who sent the packet
     * @return preferred dimension identifiers
     */
    private List<String> preferredDimensions(String pathId, String chapterId, String playerDimension) {
        List<String> dimensions = new ArrayList<>();
        dimensions.add(playerDimension);
        if (ArdaPaths.CONFIG.getChapterStartCoordinates(pathId, chapterId) != null) {
            dimensions.add(ArdaPaths.CONFIG.getChapterStartDimension(pathId, chapterId));
        }
        return dimensions;
    }

    /**
     * Writes a located chapter start to config on the server thread.
     *
     * @param server       active server
     * @param pathId       path identifier
     * @param chapterId    chapter identifier
     * @param start        chapter-start position
     * @param dimension    marker-owned dimension identifier
     * @param onlyIfUnset  whether existing coordinates should block the write
     */
    private void applyChapterStart(MinecraftServer server, String pathId, String chapterId, BlockPos start, String dimension, boolean onlyIfUnset) {
        if (!ChapterStartLocator.chapterExists(pathId, chapterId)) return;

        BlockPos previousStart = ArdaPaths.CONFIG.getChapterStartCoordinates(pathId, chapterId);
        String previousDimension = ArdaPaths.CONFIG.getChapterStartDimension(pathId, chapterId);
        if (onlyIfUnset && previousStart != null) return;

        ArdaPaths.CONFIG.setChapterStart(pathId, chapterId, PositionData.fromBlockPos(start), dimension);
        clearPreviousChapterStart(server, pathId, chapterId, previousStart, previousDimension, start, dimension);
        ArdaPaths.CONFIG_MANAGER.save();
        PacketRegistry.syncPathDataToClients(server);
    }

    /**
     * Clears the previous marker's chapter-start flag after a replacement.
     *
     * @param server            active server
     * @param pathId            path identifier
     * @param chapterId         chapter identifier
     * @param previousStart     previous configured start position
     * @param previousDimension previous configured dimension
     * @param newStart          replacement start position
     * @param newDimension      replacement dimension
     */
    private void clearPreviousChapterStart(MinecraftServer server, String pathId, String chapterId, BlockPos previousStart, String previousDimension, BlockPos newStart, String newDimension) {
        if (previousStart == null || previousDimension == null) return;
        if (previousStart.equals(newStart) && previousDimension.equals(newDimension)) return;

        ServerLevel level = resolveDestinationLevel(server, previousDimension);
        if (level == null) return;

        MarkerResolver resolver = new MarkerResolver(level, previousDimension);
        Optional<ResolvedMarker> resolved = resolver.resolve(previousStart);
        if (resolved.isEmpty()) return;

        PathMarkerBlockEntity marker = resolved.get().liveMarker();

        PathMarkerBlockEntity.ChapterNbtData data = marker.getChapterData(pathId, chapterId, false);
        if (data == null) return;

        data.setChapterStart(false);
        data.setDisplayChapterTitleOnTrail(false);
        marker.markUpdated();
    }

    /**
     * Resolves a configured dimension identifier into a loaded server level.
     *
     * @param server      active server
     * @param dimensionId dimension identifier
     * @return loaded server level, or null
     */
    private ServerLevel resolveDestinationLevel(MinecraftServer server, String dimensionId) {
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimensionId));
        return server.getLevel(key);
    }
}
