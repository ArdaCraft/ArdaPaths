package space.ajcool.ardapaths.core.integration;

import net.minecraft.util.math.Vec3d;

/**
 * First-party waypoint bridge used by client trail rendering.
 */
public interface WaypointProvider {

    /**
     * Sets or updates the waypoint that points to the next trail node.
     *
     * @param target the next trail node position
     */
    void setNextTrailNode(Vec3d target);

    /**
     * Clears all waypoints owned by ArdaPaths.
     */
    void clearWaypoints();
}
