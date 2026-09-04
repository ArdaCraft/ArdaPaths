package space.ajcool.ardapaths.screens.marker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the marker editor layout's centering math.
 */
class MarkerEditLayoutTest {

    /**
     * Verifies the marker-list column and the editor panel are centered together as one block
     * when the screen is wide enough to show the marker list.
     */
    @Test
    void markerListAndPanelAreCenteredTogetherWhenRoomExists() {
        MarkerEditLayout layout = MarkerEditLayout.of(1000, 600);

        assertTrue(layout.hasMarkerListRoom());

        int screenCenter = layout.screenWidth() / 2;
        int leftGap = screenCenter - layout.markerListLeft();
        int rightGap = layout.panelRight() - screenCenter;

        assertEquals(leftGap, rightGap);
    }

    /**
     * Verifies the editor panel alone is centered on the screen when the marker-list column
     * does not fit.
     */
    @Test
    void panelIsCenteredOnScreenWhenMarkerListDoesNotFit() {
        MarkerEditLayout layout = MarkerEditLayout.of(400, 600);

        assertFalse(layout.hasMarkerListRoom());
        assertEquals(layout.screenWidth() / 2, layout.centerX());
    }
}
