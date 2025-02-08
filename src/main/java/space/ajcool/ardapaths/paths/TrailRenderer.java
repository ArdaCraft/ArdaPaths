package space.ajcool.ardapaths.paths;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.mc.items.ModItems;
import space.ajcool.ardapaths.screen.PathSelectionScreen;
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
            for (PathMarkerBlockEntity marker : Paths.getTickingMarkers()) {
                BlockPos markerPos = marker.getPos();
                BlockPos playerPos = player.getBlockPos();
                if (playerPos.isWithinDistance(markerPos, 100)) {
                    Vec3d markerCenter = marker.position();
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
                        marker.createTrail(PathSelectionScreen.selectedPathId);
                    }
                } else {
                    removeTrail(markerPos);
                }

                if (marker.data().hasProximityMessage() && playerPos.isWithinDistance(markerPos, marker.data().getActivationRange())) {
                    ProximityMessageRenderer.setMessage(marker.data().getProximityMessage());
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
