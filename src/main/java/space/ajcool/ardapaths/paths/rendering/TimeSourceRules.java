package space.ajcool.ardapaths.paths.rendering;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import space.ajcool.ardapaths.core.data.TimeActivation;
import space.ajcool.ardapaths.core.data.TimeOfDay;

/**
 * Pure rules used to select and evaluate marker-authored time sources.
 */
final class TimeSourceRules {
    /**
     * Minimum arrival band for marker-authored time, in blocks.
     */
    private static final double MIN_ARRIVAL_RANGE = 3.0D;

    /**
     * Small tolerance for comparing projected trail distances.
     */
    private static final double NEAREST_TRAIL_EPSILON = 1.0E-6D;

    /**
     * Prevents construction of this utility class.
     */
    private TimeSourceRules() {
    }

    /**
     * Projected player position on a marker segment.
     *
     * @param progress        clamped progress from start to end
     * @param distanceSquared squared distance from the player to the projected point
     * @param overshoot       signed blocks outside the segment axis, or zero while between the endpoints
     */
    record SegmentProjection(double progress, double distanceSquared, double overshoot) {}

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
            return new SegmentProjection(0.0D, distanceSquared, 0.0D);
        }

        double rawProgress = playerPos.subtract(start).dot(segment) / lengthSquared;
        double progress = Mth.clamp(rawProgress, 0.0D, 1.0D);
        Vec3 projected = start.add(segment.scale(progress));
        double overshoot = 0.0D;
        double length = Math.sqrt(lengthSquared);
        if (rawProgress < 0.0D) {
            overshoot = rawProgress * length;
        } else if (rawProgress > 1.0D) {
            overshoot = (rawProgress - 1.0D) * length;
        }

        return new SegmentProjection(progress, playerPos.distanceToSqr(projected), overshoot);
    }

    /**
     * Determines whether a segment's end marker can currently drive computed time interpolation.
     *
     * @param startTimeOfDay    segment start marker time
     * @param endTimeOfDay      segment end marker time
     * @param endActivation     segment end marker time activation mode
     * @param overshoot         signed blocks outside the segment axis
     * @param startArrivalRange allowed distance before the start marker
     * @param endArrivalRange   allowed distance past the end marker
     * @return true when the segment is computed, fully timed, and within its arrival band
     */
    static boolean isComputedSegmentActive(long startTimeOfDay,
                                           long endTimeOfDay,
                                           TimeActivation endActivation,
                                           double overshoot,
                                           double startArrivalRange,
                                           double endArrivalRange) {
        return startTimeOfDay != TimeOfDay.UNSET
                && endTimeOfDay != TimeOfDay.UNSET
                && endActivation == TimeActivation.COMPUTED
                && (overshoot >= 0.0D ? overshoot <= endArrivalRange : -overshoot <= startArrivalRange);
    }

    /**
     * Determines whether a segment is the nearest collected trail element to the player.
     *
     * <p>A computed segment drives time only while it is the part of the collected trail nearest to the player. This
     * prevents another leg of the trail from claiming the player just because the player's position falls inside that
     * segment's axial range.</p>
     *
     * @param candidateDistanceSquared squared distance from the player to the candidate segment
     * @param nearestTrailDistanceSquared squared distance from the player to the nearest collected trail element
     * @return true when the candidate segment is nearest, allowing a small floating-point tolerance
     */
    static boolean isNearestTrailElement(double candidateDistanceSquared, double nearestTrailDistanceSquared) {
        return candidateDistanceSquared <= nearestTrailDistanceSquared + NEAREST_TRAIL_EPSILON;
    }

    /**
     * Computes the arrival band used by marker time activation.
     *
     * @param activationRange marker activation range in blocks
     * @return activation range with the minimum usable time band applied
     */
    static double arrivalRange(int activationRange) {
        return Math.max(MIN_ARRIVAL_RANGE, activationRange);
    }

    /**
     * Interpolates from the start time to the target time along the signed absolute timeline.
     *
     * @param startTicks  starting absolute ticks
     * @param targetTicks target absolute ticks
     * @param progress    segment progress in the range {@code [0, 1]}
     * @return interpolated absolute ticks
     */
    static long segmentTime(long startTicks, long targetTicks, double progress) {
        return startTicks + Math.round((targetTicks - startTicks) * Mth.clamp(progress, 0.0D, 1.0D));
    }
}
