package space.ajcool.ardapaths.paths.rendering;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import space.ajcool.ardapaths.core.data.TimeOfDay;

/**
 * Pure rules used to select and evaluate marker-authored time sources.
 */
final class TimeSourceRules {
    /**
     * Prevents construction of this utility class.
     */
    private TimeSourceRules() {
    }

    /**
     * Projected player position on a marker segment.
     *
     * @param progress clamped progress from start to end
     * @param distanceSquared squared distance from the player to the projected point
     */
    record SegmentProjection(double progress, double distanceSquared) {}

    /**
     * Projects a player position onto a marker-to-marker segment.
     *
     * @param playerPos precise player position
     * @param startPos segment start marker position
     * @param endPos segment end marker position
     * @return clamped projection progress and squared distance to the segment
     */
    static SegmentProjection projectOntoSegment(Vec3 playerPos, BlockPos startPos, BlockPos endPos) {
        Vec3 start = Vec3.atCenterOf(startPos);
        Vec3 end = Vec3.atCenterOf(endPos);
        Vec3 segment = end.subtract(start);
        double lengthSquared = segment.lengthSqr();
        if (lengthSquared <= 0.0D) {
            double distanceSquared = playerPos.distanceToSqr(start);
            return new SegmentProjection(0.0D, distanceSquared);
        }

        double progress = Mth.clamp(playerPos.subtract(start).dot(segment) / lengthSquared, 0.0D, 1.0D);
        Vec3 projected = start.add(segment.scale(progress));
        return new SegmentProjection(progress, playerPos.distanceToSqr(projected));
    }

    /**
     * Determines whether a segment's end marker can currently drive computed time interpolation.
     *
     * @param endTimeOfDay segment end marker time
     * @param endTransitionRange segment end marker transition range
     * @param progress clamped player progress along the segment
     * @return true when the segment is computed, time-capable, and not already completed
     */
    static boolean isComputedSegmentEligible(int endTimeOfDay, int endTransitionRange, double progress) {
        return endTimeOfDay != TimeOfDay.UNSET
                && TimeOfDay.isComputed(endTransitionRange)
                && progress < 1.0D;
    }
}
