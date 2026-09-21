package space.ajcool.ardapaths.core.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for marker time activation mode persistence.
 */
class TimeActivationTest {

    /**
     * Verifies the computed sentinel is preserved.
     */
    @Test
    void computedSentinelLoadsAsComputed() {
        assertEquals(TimeActivation.COMPUTED, TimeActivation.fromNbtValue(-1));
    }

    /**
     * Verifies the default marker-range value loads as marker range.
     */
    @Test
    void defaultValueLoadsAsMarkerRange() {
        assertEquals(TimeActivation.MARKER_RANGE, TimeActivation.fromNbtValue(0));
    }

    /**
     * Verifies legacy numeric distances normalize to marker range.
     */
    @Test
    void legacyNumericDistancesLoadAsMarkerRange() {
        assertEquals(TimeActivation.MARKER_RANGE, TimeActivation.fromNbtValue(32));
        assertEquals(TimeActivation.MARKER_RANGE, TimeActivation.fromNbtValue(64));
    }

    /**
     * Verifies activation modes round-trip through their persisted values.
     */
    @Test
    void activationModesRoundTripThroughNbtValues() {
        assertEquals(TimeActivation.COMPUTED, TimeActivation.fromNbtValue(TimeActivation.COMPUTED.toNbtValue()));
        assertEquals(TimeActivation.MARKER_RANGE, TimeActivation.fromNbtValue(TimeActivation.MARKER_RANGE.toNbtValue()));
    }
}
