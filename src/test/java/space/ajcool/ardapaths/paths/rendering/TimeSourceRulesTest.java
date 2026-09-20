package space.ajcool.ardapaths.paths.rendering;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import space.ajcool.ardapaths.core.data.TimeOfDay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for pure marker time-source selection rules.
 */
class TimeSourceRulesTest {
    /**
     * Verifies segment projections clamp before the start marker and measure distance to that endpoint.
     */
    @Test
    void projectOntoSegmentClampsBeforeStart() {
        TimeSourceRules.SegmentProjection projection = TimeSourceRules.projectOntoSegment(
                new Vec3(-4.5D, 0.5D, 0.5D),
                pos(0, 0, 0),
                pos(10, 0, 0));

        assertEquals(0.0D, projection.progress());
        assertEquals(25.0D, projection.distanceSquared());
    }

    /**
     * Verifies segment projections report midpoint progress and perpendicular distance.
     */
    @Test
    void projectOntoSegmentReportsMidSegmentDistance() {
        TimeSourceRules.SegmentProjection projection = TimeSourceRules.projectOntoSegment(
                new Vec3(5.5D, 3.5D, 0.5D),
                pos(0, 0, 0),
                pos(10, 0, 0));

        assertEquals(0.5D, projection.progress());
        assertEquals(9.0D, projection.distanceSquared());
    }

    /**
     * Verifies segment projections clamp past the end marker and measure distance to that endpoint.
     */
    @Test
    void projectOntoSegmentClampsPastEnd() {
        TimeSourceRules.SegmentProjection projection = TimeSourceRules.projectOntoSegment(
                new Vec3(15.5D, 0.5D, 0.5D),
                pos(0, 0, 0),
                pos(10, 0, 0));

        assertEquals(1.0D, projection.progress());
        assertEquals(25.0D, projection.distanceSquared());
    }

    /**
     * Verifies zero-length segments project to their single marker center.
     */
    @Test
    void projectOntoSegmentHandlesDegenerateSegment() {
        TimeSourceRules.SegmentProjection projection = TimeSourceRules.projectOntoSegment(
                new Vec3(2.5D, 7.5D, 4.5D),
                pos(2, 3, 4),
                pos(2, 3, 4));

        assertEquals(0.0D, projection.progress());
        assertEquals(16.0D, projection.distanceSquared());
    }

    /**
     * Verifies in-progress computed segments are eligible time sources.
     */
    @Test
    void computedSegmentEligibleMidSegment() {
        assertTrue(TimeSourceRules.isComputedSegmentEligible(6000, TimeOfDay.COMPUTED_TRANSITION_RANGE, 0.5D));
    }

    /**
     * Verifies completed computed segments stop reasserting their endpoint time.
     */
    @Test
    void computedSegmentIneligibleAtEnd() {
        assertFalse(TimeSourceRules.isComputedSegmentEligible(6000, TimeOfDay.COMPUTED_TRANSITION_RANGE, 1.0D));
    }

    /**
     * Verifies segments without an end-marker time are not eligible time sources.
     */
    @Test
    void computedSegmentIneligibleWithoutEndTime() {
        assertFalse(TimeSourceRules.isComputedSegmentEligible(TimeOfDay.UNSET, TimeOfDay.COMPUTED_TRANSITION_RANGE, 0.5D));
    }

    /**
     * Verifies numeric transition ranges are handled by radial source selection instead.
     */
    @Test
    void computedSegmentIneligibleForNumericTransitionRange() {
        assertFalse(TimeSourceRules.isComputedSegmentEligible(6000, 12, 0.5D));
    }

    /**
     * Creates a block position for projection tests.
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param z z coordinate
     * @return block position
     */
    private static BlockPos pos(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }
}
