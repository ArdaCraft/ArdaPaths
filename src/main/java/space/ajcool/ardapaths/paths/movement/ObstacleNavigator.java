package space.ajcool.ardapaths.paths.movement;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Obstacle-aware steering helper for auto-walk movement.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ObstacleNavigator {

    /** Small distance used to detect exhausted movement targets. */
    private static final double MIN_SEGMENT_LENGTH = 0.0001D;

    /** Vanilla jump impulse used when auto-walk meets a one-block obstacle. */
    private static final double AUTO_JUMP_VELOCITY = 0.42D;

    /** Sustained upward velocity used to lift the player over obstacles too tall for a normal jump impulse. */
    private static final double FLOAT_CLIMB_VELOCITY = 0.18D;

    /** Extra feet clearance required before auto-walk stops lifting over a tall obstacle. */
    private static final double CLEARANCE_MARGIN = 0.15D;

    /** Tallest obstacle treated as a normal single-jump obstruction. */
    private static final double JUMP_MAX_STEP = 1.0D;

    /** Distance in front of the player checked for jumpable obstacles. */
    private static final double OBSTACLE_PROBE_DISTANCE = 1.0D;

    /** Vertical range above the player's feet sampled when looking for a forward obstacle. */
    private static final double OBSTACLE_PROBE_HEIGHT = 2.0D;

    /** Maximum lateral distance auto-walk may leave the trail centreline to steer around an obstacle. */
    private static final double MAX_SIDESTEP_DISTANCE = 4.0D;

    /** Lateral spacing between candidate sidestep corridors. */
    private static final double SIDESTEP_PROBE_STEP = 0.5D;

    /** Vertical depth below the target feet position used to detect walkable support on uneven ground. */
    private static final double GROUND_SUPPORT_PROBE_DEPTH = 1.1D;

    /** Forward distance used to place a detour waypoint beyond a narrow obstacle. */
    private static final double DETOUR_FORWARD_CLEARANCE = 2.5D;

    /** Horizontal distance from the active detour target that counts as reaching it. */
    private static final double DETOUR_ARRIVAL_DISTANCE = 0.35D;

    /** Horizontal inset applied to ground probes so adjacent wall faces do not count as floor support. */
    private static final double GROUND_SUPPORT_PROBE_INSET = 0.05D;

    /** Ordered lateral directions tested for each sidestep distance. */
    private static final int[] SIDESTEP_SIDES = {1, -1};

    /** Feet Y target for the current float-over lift, or negative infinity when no lift is active. */
    private static double floatOverTargetY = Double.NEGATIVE_INFINITY;

    /** World-space waypoint being pursued while auto-walk steers around an obstacle. */
    @Nullable
    private static Vec3 detourTarget = null;

    /**
     * Chooses the vertical velocity needed to handle an obstacle ahead while preserving ordinary stepping.
     *
     * @param player     player being driven
     * @param directionX normalized horizontal x movement direction
     * @param directionZ normalized horizontal z movement direction
     * @param currentY   player's current vertical velocity
     * @return vertical velocity to apply this tick
     */
    static double verticalVelocity(LocalPlayer player, double directionX, double directionZ, double currentY) {
        if (shouldKeepFloating(player)) return FLOAT_CLIMB_VELOCITY;
        if (!player.onGround() && floatOverTargetY == Double.NEGATIVE_INFINITY) return currentY;

        double obstacleTop = forwardObstacleTop(player, directionX, directionZ);
        if (obstacleTop == Double.NEGATIVE_INFINITY) {
            return currentY;
        }

        double obstacleHeight = obstacleTop - player.getY();
        if (obstacleHeight <= player.maxUpStep()) {
            return currentY;
        }

        AABB forwardBox = forwardProbeBox(player, directionX, directionZ);
        if (canJumpOver(player, directionX, directionZ, obstacleTop)) {
            return AUTO_JUMP_VELOCITY;
        }

        double targetY = obstacleTop + CLEARANCE_MARGIN;
        if (hasClearanceAtFeetY(player, forwardBox, targetY)) {
            floatOverTargetY = Math.max(floatOverTargetY, targetY);
        }

        return shouldKeepFloating(player) ? FLOAT_CLIMB_VELOCITY : currentY;
    }

    /**
     * Checks whether a float-over lift should keep applying upward velocity.
     *
     * @param player player being lifted
     * @return true when the player's feet have not reached the active lift target
     */
    private static boolean shouldKeepFloating(LocalPlayer player) {
        if (floatOverTargetY == Double.NEGATIVE_INFINITY) return false;
        if (player.getY() < floatOverTargetY) return true;

        floatOverTargetY = Double.NEGATIVE_INFINITY;
        return false;
    }

    /**
     * Measures the highest collision surface in the forward movement probe.
     *
     * @param player     player being driven
     * @param directionX normalized horizontal x movement direction
     * @param directionZ normalized horizontal z movement direction
     * @return maximum obstacle top Y, or negative infinity when no obstacle is ahead
     */
    @SuppressWarnings("resource")
    private static double forwardObstacleTop(LocalPlayer player, double directionX, double directionZ) {
        AABB collisionProbe = lowerForwardProbeBox(player, directionX, directionZ);
        double obstacleTop = Double.NEGATIVE_INFINITY;
        for (VoxelShape shape : player.level().getBlockCollisions(player, collisionProbe)) {
            if (shape.isEmpty()) continue;

            obstacleTop = Math.max(obstacleTop, shape.bounds().maxY);
        }

        return obstacleTop;
    }

    /**
     * Builds the full-height forward probe box at the player's current position.
     *
     * @param player     player being driven
     * @param directionX normalized horizontal x movement direction
     * @param directionZ normalized horizontal z movement direction
     * @return player collision box shifted forward by the obstacle probe distance
     */
    private static AABB forwardProbeBox(LocalPlayer player, double directionX, double directionZ) {
        return player.getBoundingBox().move(directionX * OBSTACLE_PROBE_DISTANCE, 0.0D, directionZ * OBSTACLE_PROBE_DISTANCE);
    }

    /**
     * Checks whether a blocking obstacle can be cleared by a normal jump this tick.
     *
     * @param player      player being driven
     * @param directionX  normalized horizontal x movement direction
     * @param directionZ  normalized horizontal z movement direction
     * @param obstacleTop measured top of the obstacle ahead
     * @return true when auto-walk should keep the trail direction and issue a jump
     */
    private static boolean canJumpOver(LocalPlayer player, double directionX, double directionZ, double obstacleTop) {
        return player.onGround()
                && obstacleTop - player.getY() <= JUMP_MAX_STEP
                && hasClearanceAtFeetY(player, forwardProbeBox(player, directionX, directionZ), player.getY() + 1.0D);
    }

    /**
     * Checks whether the player's collision box would fit with feet at a target height.
     *
     * @param player    player being driven
     * @param targetBox target player collision box at the player's current feet height
     * @param feetY     target world Y for the player's feet
     * @return true when the target player volume is empty
     */
    @SuppressWarnings("resource")
    private static boolean hasClearanceAtFeetY(LocalPlayer player, AABB targetBox, double feetY) {
        return player.level().noCollision(player, targetBox.move(0.0D, feetY - player.getY(), 0.0D));
    }

    /**
     * Builds the lower forward probe area used to measure nearby obstacle heights.
     *
     * @param player     player being driven
     * @param directionX normalized horizontal x movement direction
     * @param directionZ normalized horizontal z movement direction
     * @return forward probe box clipped to the lower obstacle-sampling range
     */
    private static AABB lowerForwardProbeBox(LocalPlayer player, double directionX, double directionZ) {
        AABB forwardBox = forwardProbeBox(player, directionX, directionZ);
        return new AABB(
                forwardBox.minX,
                forwardBox.minY,
                forwardBox.minZ,
                forwardBox.maxX,
                Math.min(forwardBox.maxY, player.getY() + OBSTACLE_PROBE_HEIGHT),
                forwardBox.maxZ
        );
    }

    /**
     * Chooses the horizontal movement direction, including any committed obstacle detour.
     *
     * @param player      player being driven
     * @param trailCenter nearest point on the trail centreline
     * @param directionX  normalized horizontal x movement direction
     * @param directionZ  normalized horizontal z movement direction
     * @return normalized horizontal direction for this tick
     */
    static Vec3 horizontalDirection(LocalPlayer player, Vec3 trailCenter, double directionX, double directionZ) {
        Vec3 trailDirection = new Vec3(directionX, 0.0D, directionZ);
        if (floatOverTargetY != Double.NEGATIVE_INFINITY) return trailDirection;

        if (detourTarget != null) {
            Vec3 detourDirection = directionToTarget(player.position(), detourTarget);
            if (detourDirection != null && !hasArrivedAtDetour(player.position(), detourTarget) && !isBlockingObstacle(player, forwardObstacleTop(player, detourDirection.x, detourDirection.z))) {
                return detourDirection;
            }

            detourTarget = null;
        }

        double obstacleTop = forwardObstacleTop(player, directionX, directionZ);
        if (player.onGround() && isBlockingObstacle(player, obstacleTop)) {
            if (!canJumpOver(player, directionX, directionZ, obstacleTop)) {
                detourTarget = findDetourTarget(player, trailCenter, directionX, directionZ);
                if (detourTarget != null) {
                    Vec3 detourDirection = directionToTarget(player.position(), detourTarget);
                    if (detourDirection != null) return detourDirection;
                }
            }
        }

        return trailDirection;
    }

    /**
     * Builds a normalized horizontal direction from a position to a target.
     *
     * @param position current position
     * @param target   target position
     * @return normalized horizontal direction, or null when the target is too close
     */
    @Nullable
    private static Vec3 directionToTarget(Vec3 position, Vec3 target) {
        double deltaX = target.x - position.x;
        double deltaZ = target.z - position.z;
        double length = Math.hypot(deltaX, deltaZ);
        if (length < MIN_SEGMENT_LENGTH) return null;

        return new Vec3(deltaX / length, 0.0D, deltaZ / length);
    }

    /**
     * Checks whether the player has reached a committed detour waypoint.
     *
     * @param position player's current position
     * @param target   detour waypoint
     * @return true when the player is horizontally close enough to the target
     */
    private static boolean hasArrivedAtDetour(Vec3 position, Vec3 target) {
        double distanceX = target.x - position.x;
        double distanceZ = target.z - position.z;
        return Math.hypot(distanceX, distanceZ) < DETOUR_ARRIVAL_DISTANCE;
    }

    /**
     * Checks whether an obstacle top is high enough to require detouring or lifting.
     *
     * @param player      player being driven
     * @param obstacleTop measured top of the obstacle ahead
     * @return true when the obstacle is taller than the player's step height
     */
    private static boolean isBlockingObstacle(LocalPlayer player, double obstacleTop) {
        return obstacleTop != Double.NEGATIVE_INFINITY && obstacleTop - player.getY() > player.maxUpStep();
    }

    /**
     * Finds the nearest usable sidestep waypoint around a blocking obstacle.
     *
     * @param player      player being driven
     * @param trailCenter nearest point on the trail centreline
     * @param directionX  normalized horizontal x movement direction
     * @param directionZ  normalized horizontal z movement direction
     * @return detour waypoint to pursue, or null when no usable corridor is available
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    @Nullable
    private static Vec3 findDetourTarget(LocalPlayer player, Vec3 trailCenter, double directionX, double directionZ) {
        double perpendicularX = -directionZ;
        double perpendicularZ = directionX;
        double currentTrailOffset = ((player.getX() - trailCenter.x) * perpendicularX) + ((player.getZ() - trailCenter.z) * perpendicularZ);

        for (double offset = SIDESTEP_PROBE_STEP; offset <= MAX_SIDESTEP_DISTANCE + 0.000001D; offset += SIDESTEP_PROBE_STEP) {
            for (int side : SIDESTEP_SIDES) {
                double lateralOffset = offset * side;
                if (Math.abs(currentTrailOffset + lateralOffset) > MAX_SIDESTEP_DISTANCE) continue;

                AABB approachBox = sidestepProbeBox(player, directionX, directionZ, perpendicularX, perpendicularZ, lateralOffset, DETOUR_FORWARD_CLEARANCE / 2.0D);
                AABB waypointBox = sidestepProbeBox(player, directionX, directionZ, perpendicularX, perpendicularZ, lateralOffset, DETOUR_FORWARD_CLEARANCE);
                if (!hasUsableDetourCorridor(player, approachBox) || !hasUsableDetourCorridor(player, waypointBox))
                    continue;

                return player.position().add(
                        (directionX * DETOUR_FORWARD_CLEARANCE) + (perpendicularX * lateralOffset),
                        0.0D,
                        (directionZ * DETOUR_FORWARD_CLEARANCE) + (perpendicularZ * lateralOffset)
                );
            }
        }

        return null;
    }

    /**
     * Builds a forward-and-lateral probe box for a possible obstacle detour.
     *
     * @param player         player being driven
     * @param directionX     normalized horizontal x movement direction
     * @param directionZ     normalized horizontal z movement direction
     * @param perpendicularX normalized horizontal x axis perpendicular to movement
     * @param perpendicularZ normalized horizontal z axis perpendicular to movement
     * @param lateralOffset  signed lateral offset from the trail direction
     * @param forwardOffset  forward distance from the player's current position
     * @return player collision box shifted into the candidate sidestep corridor
     */
    private static AABB sidestepProbeBox(LocalPlayer player, double directionX, double directionZ, double perpendicularX, double perpendicularZ, double lateralOffset, double forwardOffset) {
        return player.getBoundingBox().move(
                (directionX * forwardOffset) + (perpendicularX * lateralOffset),
                0.0D,
                (directionZ * forwardOffset) + (perpendicularZ * lateralOffset)
        );
    }

    /**
     * Checks whether a candidate sidestep corridor has both player clearance and walkable support.
     *
     * @param player      player being driven
     * @param corridorBox candidate sidestep collision box
     * @return true when the corridor can hold the player on nearby terrain height
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean hasUsableDetourCorridor(LocalPlayer player, AABB corridorBox) {
        double feetY = player.getY();
        double stepUpFeetY = feetY + player.maxUpStep();

        return (hasClearanceAtFeetY(player, corridorBox, feetY) && hasGroundSupportAtFeetY(player, corridorBox, feetY))
                || (hasClearanceAtFeetY(player, corridorBox, stepUpFeetY) && hasGroundSupportAtFeetY(player, corridorBox, stepUpFeetY));
    }

    /**
     * Checks whether the candidate sidestep corridor has solid ground below the target feet height.
     *
     * @param player      player being driven
     * @param corridorBox candidate sidestep collision box
     * @param feetY       target world Y for the player's feet
     * @return true when a collision surface supports the target standing area
     */
    @SuppressWarnings("resource")
    private static boolean hasGroundSupportAtFeetY(LocalPlayer player, AABB corridorBox, double feetY) {
        AABB supportBox = new AABB(
                corridorBox.minX + GROUND_SUPPORT_PROBE_INSET,
                feetY - GROUND_SUPPORT_PROBE_DEPTH,
                corridorBox.minZ + GROUND_SUPPORT_PROBE_INSET,
                corridorBox.maxX - GROUND_SUPPORT_PROBE_INSET,
                feetY,
                corridorBox.maxZ - GROUND_SUPPORT_PROBE_INSET
        );
        for (VoxelShape shape : player.level().getBlockCollisions(player, supportBox)) {
            if (!shape.isEmpty()) return true;
        }

        return false;
    }

    /**
     * Clears obstacle navigation state for a fresh auto-walk engagement.
     */
    static void reset() {
        floatOverTargetY = Double.NEGATIVE_INFINITY;
        detourTarget = null;
    }
}
