package space.ajcool.ardapaths.paths;

import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.data.config.ClientConfigManager;
import space.ajcool.ardapaths.core.data.config.client.ClientConfig;
import space.ajcool.ardapaths.core.data.config.shared.ChapterData;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterPlayerTeleportPacket;
import space.ajcool.ardapaths.core.networking.packets.server.ChapterUpdatePacket;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.paths.rendering.TrailRenderer;

import java.util.ArrayList;
import java.util.List;

public class Paths {
    private static final ClientConfig config = ArdaPathsClient.CONFIG;
    private static final ClientConfigManager configManager = ArdaPathsClient.CONFIG_MANAGER;
    private static final List<PathMarkerBlockEntity> tickingMarkers = new ArrayList<>();

    public static void setSelectedPath(final String pathId) {

        if (!config.getSelectedPathId().equalsIgnoreCase(pathId)) {
            config.setCurrentChapter("");
        }

        config.setSelectedPath(pathId);
        configManager.save();
    }

    public static void gotoChapter(final String chapterId) {
        gotoChapter(chapterId, true);
    }

    public static void gotoChapter(final String chapterId, boolean teleport)
    {
        config.setCurrentChapter(chapterId);
        configManager.save();

        if (!teleport) return;

        ChapterPlayerTeleportPacket packet = new ChapterPlayerTeleportPacket(config.getSelectedPathId(), chapterId);
        PacketRegistry.CHAPTER_PLAYER_TELEPORT.send(packet);
        TrailRenderer.clearTrails();
    }

    public static void showProximityMessages(final boolean show) {
        config.showProximityMessages(show);
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

    public static void addTickingMarker(PathMarkerBlockEntity marker) {
        tickingMarkers.add(marker);
    }

    public static List<PathMarkerBlockEntity> getTickingMarkers() {
        return List.copyOf(tickingMarkers);
    }

    public static void clearTickingMarkers() {
        tickingMarkers.clear();
    }
}
