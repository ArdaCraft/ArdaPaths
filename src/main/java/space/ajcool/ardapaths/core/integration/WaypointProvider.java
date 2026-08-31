package space.ajcool.ardapaths.core.integration;

import net.minecraft.world.phys.Vec3;

/**
 * First-party waypoint bridge used by client trail rendering.
 */
public interface WaypointProvider {

    /**
     * Sets or updates the waypoint that points to the next trail node.
     *
     * @param target the next trail node position
     */
    void setNextTrailNode(Vec3 target);

    /**
     * Clears all waypoints owned by ArdaPaths.
     */
    void clearWaypoints();
}
