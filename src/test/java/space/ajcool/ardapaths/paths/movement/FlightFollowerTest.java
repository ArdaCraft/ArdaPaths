package space.ajcool.ardapaths.paths.movement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for vertical auto-walk following while the player is flying.
 */
class FlightFollowerTest {

    /**
     * Verifies players below the trail are raised to marker-block feet height.
     */
    @Test
    void resolveOffsetUsesMarkerFeetHeightWhenBelowTrail() {
        assertEquals(FlightFollower.BELOW_TRAIL_OFFSET, FlightFollower.resolveOffset(63.0D, 64.5D));
    }

    /**
     * Verifies players exactly at the trail node are treated as starting below the held height.
     */
    @Test
    void resolveOffsetUsesMarkerFeetHeightWhenAtTrail() {
        assertEquals(FlightFollower.BELOW_TRAIL_OFFSET, FlightFollower.resolveOffset(64.5D, 64.5D));
    }

    /**
     * Verifies non-positive offsets do not preserve a below-trail starting height.
     */
    @Test
    void resolveOffsetUsesMarkerFeetHeightWhenMarginallyBelowTrail() {
        assertEquals(FlightFollower.BELOW_TRAIL_OFFSET, FlightFollower.resolveOffset(64.499D, 64.5D));
    }

    /**
     * Verifies players above the trail preserve their starting height offset.
     */
    @Test
    void resolveOffsetPreservesPositiveOffsetWhenAboveTrail() {
        assertEquals(15.5D, FlightFollower.resolveOffset(80.0D, 64.5D));
    }

    /**
     * Verifies large upward corrections are capped at the configured speed.
     */
    @Test
    void approachVelocityClampsLargePositiveGap() {
        assertEquals(FlightFollower.MAX_VERTICAL_SPEED, FlightFollower.approachVelocity(64.0D, 80.0D));
    }

    /**
     * Verifies large downward corrections are capped at the configured speed.
     */
    @Test
    void approachVelocityClampsLargeNegativeGap() {
        assertEquals(-FlightFollower.MAX_VERTICAL_SPEED, FlightFollower.approachVelocity(80.0D, 64.0D));
    }

    /**
     * Verifies ordinary small corrections are applied without clamping.
     */
    @Test
    void approachVelocityReturnsRawSubCapGap() {
        assertEquals(0.2D, FlightFollower.approachVelocity(64.0D, 64.2D), 0.0000001D);
    }

    /**
     * Verifies near-arrival corrections stop vertical motion exactly.
     */
    @Test
    void approachVelocityStopsWithinArrivalEpsilon() {
        assertEquals(0.0D, FlightFollower.approachVelocity(64.0D, 64.005D));
    }
}
