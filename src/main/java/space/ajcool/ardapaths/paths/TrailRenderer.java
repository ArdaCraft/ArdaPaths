package space.ajcool.ardapaths.paths;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.config.shared.PathData;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.mc.items.ModItems;
import space.ajcool.ardapaths.paths.rendering.AnimatedTrail;
import space.ajcool.ardapaths.utils.McUtils;

import java.util.ArrayList;
import java.util.List;

public class TrailRenderer {
    private static final double DESIRED_SPACING = 15.0;
    private static final List<AnimatedTrail> trails = new ArrayList<>();

    /**
     * Render all registered trails.
     *
     * @param level The client world
     */
    public static void render(ClientWorld level) {
        ClientPlayerEntity player = McUtils.player();
        if (player == null) return;

        if (player.isHolding(ModItems.PATH_MARKER) || player.isHolding(ModItems.PATH_REVEALER)) {
            boolean foundMessage = false;
            boolean onlyRenderChapter = ArdaPathsClient.CONFIG.onlyRenderChapter();
            String selectedPathId = ArdaPathsClient.CONFIG.getSelectedPathId();
            String currentChapterId = ArdaPathsClient.CONFIG.getCurrentChapterId();

            for (PathMarkerBlockEntity marker : Paths.getTickingMarkers()) {
                BlockPos markerPos = marker.getPos();
                BlockPos playerPos = player.getBlockPos();
                PathMarkerBlockEntity.NbtData data = marker.getNbt(selectedPathId);

                String chapterId = data.getChapterId();
                int activationRange = data.getActivationRange();
                boolean withinDistance = playerPos.isWithinDistance(markerPos, activationRange);

                if (withinDistance && data.isChapterStart()) {
                    ArdaPathsClient.CONFIG.setCurrentChapter(data.getChapterId());
                    ArdaPathsClient.CONFIG_MANAGER.save();
                }

                if (onlyRenderChapter && (!chapterId.isEmpty() && !chapterId.equalsIgnoreCase(currentChapterId))) {
                    removeTrail(markerPos);
                    continue;
                }

                if (playerPos.isWithinDistance(markerPos, 100)) {
                    Vec3d markerCenter = marker.getCenterPos();
                    List<AnimatedTrail> markerTrails = trails.stream()
                            .filter(trail -> trail.getStart().equals(marker.getPos()))
                            .toList();

                    double minTraveled = Double.MAX_VALUE;
                    for (AnimatedTrail t : markerTrails) {
                        double traveled = markerCenter.distanceTo(t.getCurrentPos());
                        if (traveled < minTraveled) {
                            minTraveled = traveled;
                        }
                    }

                    if (markerTrails.isEmpty() || minTraveled >= DESIRED_SPACING) {
                        PathData pathData = ArdaPathsClient.CONFIG.getSelectedPath();
                        if (pathData == null) continue;
                        marker.createTrail(pathData.getId(), pathData.getColor());
                    }
                } else {
                    removeTrail(markerPos);
                }

                if (withinDistance && !data.getProximityMessage().isEmpty()) {
                    ProximityMessageRenderer.setMessage(data.getProximityMessage());
                    foundMessage = true;
                }
            }

            if (!foundMessage) {
                ProximityMessageRenderer.clearMessage();
            }

            for (AnimatedTrail trail : List.copyOf(trails)) {
                if (trail.isAtEnd()) {
                    removeTrail(trail);
                } else {
                    trail.render(level);
                }
            }
        } else {
            clearTrails();
            ProximityMessageRenderer.clearMessage();
        }
    }

    /**
     * Register a new trail to render.
     *
     * @param trail The trail to render
     */
    public static void registerTrail(AnimatedTrail trail) {
        trails.add(trail);
    }

    /**
     * Remove a trail from the list of trails to render.
     *
     * @param start The starting position of the trail to remove
     */
    public static void removeTrail(BlockPos start) {
        trails.removeIf(trail -> trail.getStart().equals(start));
    }

    /**
     * Remove a trail from the list of trails to render.
     *
     * @param trail The trail to remove
     */
    public static void removeTrail(AnimatedTrail trail) {
        trails.remove(trail);
    }

    /**
     * Clear all registered trails.
     */
    public static void clearTrails() {
        trails.clear();
    }
}
