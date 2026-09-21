package space.ajcool.ardapaths.paths.rendering;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import space.ajcool.ardapaths.core.data.TimeActivation;
import space.ajcool.ardapaths.core.data.TimeOfDay;
import space.ajcool.ardapaths.screens.widgets.TextValidationError;

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
        assertEquals(-5.0D, projection.overshoot());
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
        assertEquals(0.0D, projection.overshoot());
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
        assertEquals(5.0D, projection.overshoot());
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
        assertEquals(0.0D, projection.overshoot());
    }

    /**
     * Verifies in-progress computed segments are active time sources.
     */
    @Test
    void computedSegmentActiveMidSegment() {
        assertTrue(TimeSourceRules.isComputedSegmentActive(3000, 6000, TimeActivation.COMPUTED, 0.0D, 3.0D, 3.0D));
    }

    /**
     * Verifies completed computed segments remain active inside the end marker arrival range.
     */
    @Test
    void computedSegmentActiveAtEnd() {
        assertTrue(TimeSourceRules.isComputedSegmentActive(3000, 6000, TimeActivation.COMPUTED, 0.0D, 3.0D, 3.0D));
    }

    /**
     * Verifies computed segments remain active shortly before the start marker.
     */
    @Test
    void computedSegmentActiveBeforeStartArrivalRange() {
        assertTrue(TimeSourceRules.isComputedSegmentActive(3000, 6000, TimeActivation.COMPUTED, -2.5D, 3.0D, 3.0D));
    }

    /**
     * Verifies computed segments stop before the start marker arrival range.
     */
    @Test
    void computedSegmentInactiveBeforeStartArrivalRange() {
        assertFalse(TimeSourceRules.isComputedSegmentActive(3000, 6000, TimeActivation.COMPUTED, -3.5D, 3.0D, 3.0D));
    }

    /**
     * Verifies computed segments remain active shortly past the end marker.
     */
    @Test
    void computedSegmentActivePastEndArrivalRange() {
        assertTrue(TimeSourceRules.isComputedSegmentActive(3000, 6000, TimeActivation.COMPUTED, 2.5D, 3.0D, 3.0D));
    }

    /**
     * Verifies computed segments stop past the end marker arrival range.
     */
    @Test
    void computedSegmentInactivePastEndArrivalRange() {
        assertFalse(TimeSourceRules.isComputedSegmentActive(3000, 6000, TimeActivation.COMPUTED, 3.5D, 3.0D, 3.0D));
    }

    /**
     * Verifies segments without a start-marker time are not active time sources.
     */
    @Test
    void computedSegmentInactiveWithoutStartTime() {
        assertFalse(TimeSourceRules.isComputedSegmentActive(TimeOfDay.UNSET, 6000, TimeActivation.COMPUTED, 0.0D, 3.0D, 3.0D));
    }

    /**
     * Verifies segments without an end-marker time are not active time sources.
     */
    @Test
    void computedSegmentInactiveWithoutEndTime() {
        assertFalse(TimeSourceRules.isComputedSegmentActive(3000, TimeOfDay.UNSET, TimeActivation.COMPUTED, 0.0D, 3.0D, 3.0D));
    }

    /**
     * Verifies marker-range activation is handled by radial source selection instead.
     */
    @Test
    void computedSegmentInactiveForMarkerRangeActivation() {
        assertFalse(TimeSourceRules.isComputedSegmentActive(3000, 6000, TimeActivation.MARKER_RANGE, 0.0D, 3.0D, 3.0D));
    }

    /**
     * Verifies a trail element tied for nearest distance can drive segment time.
     */
    @Test
    void nearestTrailElementAcceptsEqualDistance() {
        assertTrue(TimeSourceRules.isNearestTrailElement(4.0D, 4.0D));
    }

    /**
     * Verifies a trail element outside the comparison tolerance cannot drive segment time.
     */
    @Test
    void nearestTrailElementRejectsFartherDistance() {
        assertFalse(TimeSourceRules.isNearestTrailElement(4.0001D, 4.0D));
    }

    /**
     * Verifies tiny floating-point differences do not break nearest-trail ties.
     */
    @Test
    void nearestTrailElementAcceptsDistanceWithinEpsilon() {
        assertTrue(TimeSourceRules.isNearestTrailElement(4.0000005D, 4.0D));
    }

    /**
     * Verifies activation ranges are floored for time arrivals.
     */
    @Test
    void arrivalRangeHasMinimumBand() {
        assertEquals(3.0D, TimeSourceRules.arrivalRange(0));
        assertEquals(8.0D, TimeSourceRules.arrivalRange(8));
    }

    /**
     * Verifies segment interpolation can span multiple authored days.
     *
     * @throws TextValidationError when the fixture date is malformed
     */
    @Test
    void segmentTimeCrossesMultiDaySpan() throws TextValidationError {
        long start = TimeOfDay.parse("19/12/1989 06:00");
        long end = TimeOfDay.parse("25/12/1989 12:00");

        assertEquals(TimeOfDay.parse("22/12/1989 09:00"), TimeSourceRules.segmentTime(start, end, 0.5D));
    }

    /**
     * Verifies segment interpolation can move backwards on the authored timeline.
     *
     * @throws TextValidationError when the fixture date is malformed
     */
    @Test
    void segmentTimeSupportsBackwardsSpan() throws TextValidationError {
        long start = TimeOfDay.parse("25/12/1989 12:00");
        long end = TimeOfDay.parse("19/12/1989 06:00");

        assertEquals(TimeOfDay.parse("22/12/1989 09:00"), TimeSourceRules.segmentTime(start, end, 0.5D));
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
