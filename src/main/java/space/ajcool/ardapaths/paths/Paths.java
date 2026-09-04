package space.ajcool.ardapaths.paths;

import net.minecraft.core.BlockPos;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.data.config.ClientConfigManager;
import space.ajcool.ardapaths.core.data.config.client.ClientConfig;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterDeletePacket;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterPlayerTeleportPacket;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterUpdatePacket;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.paths.rendering.EnvironmentController;
import space.ajcool.ardapaths.paths.rendering.TrailRenderer;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client-side façade for modifying path selection state and settings.
 * Provides a single entry point for changing the current path/chapter,
 * updating preferences, and managing marker animations.
 * All changes are automatically persisted to the client config and synchronized with the server.
 */
public class Paths {

    /**
     * The client-side configuration object containing path and player preferences.
     */
    private static final ClientConfig config = ArdaPathsClient.CONFIG;

    /**
     * The configuration manager responsible for saving changes to disk.
     */
    private static final ClientConfigManager configManager = ArdaPathsClient.CONFIG_MANAGER;

    /**
     * Loaded client-side path marker block entities keyed by position.
     */
    private static final Map<BlockPos, PathMarkerBlockEntity> tickingMarkers = new LinkedHashMap<>();

    /**
     * Sets the current selected path, clearing the chapter selection if the path changed.
     * Automatically saves the configuration to disk.
     *
     * @param pathId the ID of the path to select
     */
    public static void setSelectedPath(final String pathId) {

        if (!config.getSelectedPathId().equalsIgnoreCase(pathId)) {
            config.setCurrentChapter("");
        }

        config.setSelectedPath(pathId);
        configManager.save();
    }

    /**
     * Selects a chapter and optionally teleports the player to the chapter start.
     * Automatically saves the configuration to disk and clears active trails.
     *
     * @param chapterId the ID of the chapter to select
     */
    public static void gotoChapter(final String chapterId) {
        gotoChapter(chapterId, true);
    }

    /**
     * Selects a chapter with an option to teleport.
     * Automatically saves the configuration to disk.
     *
     * @param chapterId the ID of the chapter to select
     * @param teleport  whether to teleport the player to the chapter start
     */
    public static void gotoChapter(final String chapterId, boolean teleport) {
        config.setCurrentChapter(chapterId);
        configManager.save();

        if (!teleport) return;

        ChapterPlayerTeleportPacket packet = new ChapterPlayerTeleportPacket(config.getSelectedPathId(), chapterId);
        PacketRegistry.CHAPTER_PLAYER_TELEPORT.send(packet);
        TrailRenderer.clearTrails();
    }

    public static void showChapterTitles(final boolean show) {
        config.setChapterTitles(show);
        configManager.save();
    }

    public static void showProximityMessages(final boolean show) {
        config.setProximityMessages(show);
        configManager.save();
    }

    public static void showTrailWaypoints(final boolean show) {
        config.setTrailWaypoints(show);
        configManager.save();
    }

    /**
     * Sets whether trail markers may dynamically change time and weather and persists the preference.
     *
     * @param use whether dynamic environmental effects should be enabled
     */
    public static void useDynamicEnvironment(final boolean use) {
        config.setDynamicEnvironment(use);
        configManager.save();

        if (!use) {
            EnvironmentController.reset();
        }
    }

    public static void setChapterTitleDisplaySpeed(final Float miliseconds) {
        config.setChapterTitleDisplaySpeed(miliseconds);
        configManager.save();
    }

    public static void setProximityMessagesSpeedMultiplier(final Double factor) {
        config.setProximityTextSpeedMultiplier(factor);
        configManager.save();
    }

    /**
     * Sets the auto-walk speed factor and persists it to the client config.
     *
     * @param factor the movement speed factor to apply
     */
    public static void setAutoWalkSpeedFactor(final Double factor) {
        config.setAutoWalkSpeedFactor(factor);
        configManager.save();
    }

    public static void updateChapter(String pathId, ChapterData chapter) {
        PathData path = config.getPath(pathId);
        if (path != null) {
            path.setChapter(chapter);
            configManager.save();
            ChapterUpdatePacket packet = new ChapterUpdatePacket(pathId, chapter);
            PacketRegistry.CHAPTER_UPDATE.send(packet);
        }
    }

    public static void deleteChapter(String pathId, ChapterData chapter) {
        PathData path = config.getPath(pathId);
        if (path != null) {
            path.removeChapter(chapter.getId());
            configManager.save();
            ChapterDeletePacket packet = new ChapterDeletePacket(pathId, chapter.getId());
            PacketRegistry.CHAPTER_DELETE.send(packet);
        }
    }

    /**
     * Registers a loaded client-side marker for rendering queries.
     *
     * @param marker the marker block entity to register
     */
    public static void addTickingMarker(PathMarkerBlockEntity marker) {
        tickingMarkers.put(marker.getBlockPos().immutable(), marker);
    }

    /**
     * Removes a client-side marker from rendering queries.
     *
     * @param marker the marker block entity to remove
     */
    public static void removeTickingMarker(PathMarkerBlockEntity marker) {
        tickingMarkers.remove(marker.getBlockPos());
    }

    /**
     * Returns the currently loaded client-side markers without copying the backing collection.
     *
     * @return an unmodifiable live view of loaded markers
     */
    public static Collection<PathMarkerBlockEntity> getTickingMarkers() {
        return Collections.unmodifiableCollection(tickingMarkers.values());
    }

    /**
     * Clears all tracked client-side markers, usually because the client left a world.
     */
    public static void clearTickingMarkers() {
        tickingMarkers.clear();
    }
}
