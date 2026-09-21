package space.ajcool.ardapaths.core.networking.handlers.server;

import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.backup.BackupJobRunner;
import space.ajcool.ardapaths.core.consumers.networking.ServerPacketHandler;
import space.ajcool.ardapaths.core.data.WarpTarget;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.data.config.shared.PositionData;
import space.ajcool.ardapaths.core.integration.Warps;
import space.ajcool.ardapaths.core.markers.ChapterStartLocator;
import space.ajcool.ardapaths.core.markers.MarkerResolver;
import space.ajcool.ardapaths.core.markers.MarkerResolver.ResolvedMarker;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterUpdatePacket;

import java.util.Optional;

/**
 * Handles updates to chapter data in the server configuration.
 * Processes incoming {@link ChapterUpdatePacket} from clients and persists chapter metadata.
 */
@Slf4j(topic = "ardapaths")
public class ChapterUpdateHandler extends ServerPacketHandler<ChapterUpdatePacket> {

    /**
     * Creates the server-side chapter update handler.
     */
    public ChapterUpdateHandler() {
        super(ChapterUpdatePacket.CHANNEL, ChapterUpdatePacket::read);
    }

    /**
     * Requires edit permission because chapter updates mutate server path config.
     *
     * @return true because this packet changes editable path data
     */
    @Override
    protected boolean requiresEditPermission() {
        return true;
    }

    @Override
    public void handle(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, ChapterUpdatePacket packet, PacketSender sender) {
        final String pathId = packet.pathId();
        final PathData pathData = ArdaPaths.CONFIG.getPath(pathId);
        if (pathData == null) return;

        final String chapterId = packet.chapterId();
        final String chapterName = packet.chapterName();
        final int chapterIndex = packet.chapterIndex();
        final NormalizedWarp normalized = normalizeWarp(packet.warp(), packet.coordinates());
        final ChapterData chapterData = new ChapterData(chapterId, chapterName, chapterIndex, normalized.warp());
        ChapterData existingChapter = pathData.getChapter(chapterId);
        if (normalized.coordinates() != null) {
            if (!isValidSubmittedDimension(packet.dimension())) {
                log.warn("Rejected chapter update for {}:{} with invalid dimension '{}'", pathId, chapterId, packet.dimension());
                return;
            }

            chapterData.setCoordinates(PositionData.fromBlockPos(normalized.coordinates()));
            //noinspection resource
            chapterData.setDimension(resolveDimension(packet.dimension(), existingChapter, player.level().dimension().location().toString()));
        } else if (existingChapter != null) {
            chapterData.setCoordinates(existingChapter.getCoordinates());
            chapterData.setDimension(existingChapter.getDimension());
        }

        pathData.setChapter(chapterData);
        ArdaPaths.CONFIG_MANAGER.save();
        PacketRegistry.syncPathDataToClients(server);

        if (normalized.coordinates() == null && !normalized.warp().isBlank() && Warps.isAvailable()) {
            resolveWarpCoordinates(server, pathId, chapterId, normalized.warp());
        }
    }

    /**
     * Normalizes coordinate-shaped warp text into the explicit coordinates field.
     *
     * @param warp        submitted warp text
     * @param coordinates submitted explicit coordinates
     * @return normalized warp and coordinate pair
     */
    public static NormalizedWarp normalizeWarp(String warp, BlockPos coordinates) {
        BlockPos parsed = WarpTarget.parseCoordinates(warp);
        if (parsed != null) {
            return new NormalizedWarp("", parsed);
        }

        return new NormalizedWarp(warp == null ? "" : warp.trim(), coordinates);
    }

    /**
     * Resolves the dimension to store for explicit chapter-start coordinates.
     *
     * @param submitted       dimension submitted by the client, or null
     * @param existing        existing chapter data, or null for new chapters
     * @param playerDimension dimension containing the player who submitted the edit
     * @return dimension identifier to store with coordinates
     */
    public static String resolveDimension(@Nullable String submitted, @Nullable ChapterData existing, String playerDimension) {
        if (submitted != null && !submitted.isBlank()) {
            return submitted.trim();
        }

        if (existing != null && existing.getCoordinates() != null) {
            return existing.getDimension();
        }

        return playerDimension;
    }

    /**
     * Checks the optional submitted dimension identifier.
     *
     * @param submitted dimension submitted by the client, or null
     * @return true when the value is blank or a valid identifier
     */
    private boolean isValidSubmittedDimension(@Nullable String submitted) {
        return submitted == null || submitted.isBlank() || ResourceLocation.tryParse(submitted.trim()) != null;
    }

    /**
     * Resolves a warp and persists the nearest chapter marker as coordinate fallback.
     *
     * @param server    server that owns marker worlds
     * @param pathId    path identifier
     * @param chapterId chapter identifier
     * @param warp      warp name to resolve
     */
    private void resolveWarpCoordinates(MinecraftServer server, String pathId, String chapterId, String warp) {
        Warps.resolveWarp(server, warp).thenAccept(destination -> destination.ifPresent(location ->
                BackupJobRunner.submitMarkerWork(server, gate -> {
                    ChapterStartLocator.Anchor anchor = new ChapterStartLocator.Anchor(location.worldKey(), location.position());
                    MarkerResolver resolver = ChapterStartLocator.resolverFor(server, anchor, gate);
                    if (resolver == null) return null;

                    Optional<ResolvedMarker> marker = ChapterStartLocator.findNearestChapterStart(resolver, anchor.position(), pathId, chapterId, gate)
                            .or(() -> ChapterStartLocator.findNearestChapterMarker(resolver, anchor.position(), pathId, chapterId, gate));
                    marker.ifPresent(found -> gate.run(() -> {
                        if (!ChapterStartLocator.chapterExists(pathId, chapterId)) return;
                        ArdaPaths.CONFIG.setChapterStart(pathId, chapterId, PositionData.fromBlockPos(found.position()), found.dimensionId());
                        ArdaPaths.CONFIG_MANAGER.save();
                        PacketRegistry.syncPathDataToClients(server);
                    }));
                    return null;
                })
        ));
    }

    /**
     * Normalized chapter-start target submitted by the client.
     *
     * @param warp        cleaned warp text
     * @param coordinates explicit coordinates, including legacy coordinate-shaped warp text
     */
    public record NormalizedWarp(String warp, BlockPos coordinates) {
    }
}
