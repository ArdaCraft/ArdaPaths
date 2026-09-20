package space.ajcool.ardapaths.paths.movement;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/**
 * Vertical trail-following helper used while auto-walk controls a flying player.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class FlightFollower {

    /** Feet offset used when a flying player starts at or below the trail node height. */
    static final double BELOW_TRAIL_OFFSET = -0.5D;

    /** Maximum upward or downward speed applied while flying toward the held trail height. */
    static final double MAX_VERTICAL_SPEED = 0.35D;

    /** Maximum height error treated as already arriving at the held trail height. */
    static final double ARRIVAL_EPSILON = 0.01D;

    /** Vertical offset from the active trail node, or NaN when flight following is inactive. */
    private static double trailOffset = Double.NaN;

    /**
     * Checks whether the player is using normal creative or spectator flight.
     *
     * @param player the player being driven by auto-walk
     * @return true when flight following should control vertical velocity
     */
    static boolean isFlying(LocalPlayer player) {
        return player.getAbilities().flying;
    }

    /**
     * Captures or clears the flight-following offset for the current tick.
     *
     * @param player the player being driven by auto-walk
     * @param trailY nearest trail node height for this tick
     */
    static void update(LocalPlayer player, double trailY) {
        if (!isFlying(player)) {
            reset();
            return;
        }

        if (Double.isNaN(trailOffset)) {
            trailOffset = resolveOffset(player.getY(), trailY);
        }
    }

    /**
     * Chooses the vertical velocity needed to approach the held trail-relative flight height.
     *
     * @param player the player being driven by auto-walk
     * @param trailY nearest trail node height for this tick
     * @return vertical velocity to apply this tick
     */
    static double verticalVelocity(LocalPlayer player, double trailY) {
        if (Double.isNaN(trailOffset)) {
            trailOffset = resolveOffset(player.getY(), trailY);
        }

        return approachVelocity(player.getY(), trailY + trailOffset);
    }

    /**
     * Resolves the held offset from the trail when flight following begins.
     *
     * @param playerY current player feet Y
     * @param trailY  nearest trail node Y
     * @return positive starting offset, or the marker-block feet offset when at or below the trail
     */
    static double resolveOffset(double playerY, double trailY) {
        double offset = playerY - trailY;
        return offset > 0.0D ? offset : BELOW_TRAIL_OFFSET;
    }

    /**
     * Chooses a bounded velocity that approaches the target height.
     *
     * @param currentY current player feet Y
     * @param targetY  target player feet Y
     * @return vertical velocity for the next tick
     */
    static double approachVelocity(double currentY, double targetY) {
        double delta = targetY - currentY;
        if (Math.abs(delta) < ARRIVAL_EPSILON) return 0.0D;

        return Mth.clamp(delta, -MAX_VERTICAL_SPEED, MAX_VERTICAL_SPEED);
    }

    /**
     * Clears the remembered flight offset for the next flight-following engagement.
     */
    static void reset() {
        trailOffset = Double.NaN;
    }
}
