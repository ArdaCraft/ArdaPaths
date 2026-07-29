package space.ajcool.ardapaths.core.integration;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Client-side facade for optional waypoint providers.
 */
@Environment(EnvType.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Waypoints {

    /**
     * The active waypoint provider, or null when no waypoint integration is available.
     */
    private static volatile @Nullable WaypointProvider provider;

    /**
     * Registers a waypoint provider for trail node markers.
     *
     * @param waypointProvider the provider to route waypoint updates through
     */
    public static void register(WaypointProvider waypointProvider) {
        provider = waypointProvider;
    }

    /**
     * @return true when a waypoint provider has registered itself
     */
    public static boolean isAvailable() {
        return provider != null;
    }

    /**
     * Sets or updates the next trail node waypoint if a provider is available.
     *
     * @param target the next trail node position
     */
    public static void setNextTrailNode(Vec3d target) {
        WaypointProvider waypointProvider = provider;
        if (waypointProvider != null) waypointProvider.setNextTrailNode(target);
    }

    /**
     * Clears all ArdaPaths-owned waypoints if a provider is available.
     */
    public static void clearWaypoints() {
        WaypointProvider waypointProvider = provider;
        if (waypointProvider != null) waypointProvider.clearWaypoints();
    }
}
