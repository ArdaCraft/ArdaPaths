package space.ajcool.ardapaths.screens.marker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests activation-range conversion helpers used by the marker editor slider.
 */
class MarkerFieldsActivationRangeTest {

    /**
     * Verifies normalized slider positions map across the full activation-range span.
     */
    @Test
    void sliderValueMapsToActivationRange() {
        assertEquals(0, MarkerFields.sliderToActivationRange(0.0));
        assertEquals(50, MarkerFields.sliderToActivationRange(0.5));
        assertEquals(100, MarkerFields.sliderToActivationRange(1.0));
        assertEquals(25, MarkerFields.sliderToActivationRange(0.25));
    }

    /**
     * Verifies slider positions outside the track clamp to supported activation ranges.
     */
    @Test
    void sliderValueClampsToActivationRangeBounds() {
        assertEquals(0, MarkerFields.sliderToActivationRange(-0.5));
        assertEquals(100, MarkerFields.sliderToActivationRange(1.5));
    }

    /**
     * Verifies an activation range can be rendered by the slider and saved unchanged.
     */
    @Test
    void activationRangeRoundTripsThroughSliderValue() {
        assertEquals(75, MarkerFields.sliderToActivationRange(MarkerFields.activationRangeToSlider(75)));
    }

    /**
     * Verifies legacy stored values above the slider maximum stay inside the visible track.
     */
    @Test
    void storedActivationRangeClampsToSliderBounds() {
        assertEquals(1.0, MarkerFields.activationRangeToSlider(250));
    }
}
