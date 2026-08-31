package space.ajcool.ardapaths.screens.layout;

import net.minecraft.client.gui.components.AbstractWidget;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for screen layout measurement helpers.
 */
class ScreenLayoutTest {
    /**
     * Verifies empty widget groups have no vertical span.
     */
    @Test
    void contentHeightIsZeroForEmptyCollections() {
        assertEquals(0, ScreenLayout.contentHeight(List.of()));
    }

    /**
     * Verifies content height spans from the highest top edge to the lowest bottom edge.
     */
    @Test
    void contentHeightMeasuresWidgetExtents() {
        AbstractWidget first = widgetAt(20, 10);
        AbstractWidget second = widgetAt(5, 8);

        assertEquals(25, ScreenLayout.contentHeight(List.of(first, second)));
    }

    /**
     * Verifies vertical centering returns the offset needed to center the full content span.
     */
    @Test
    void verticalCenterOffsetCentersContentSpan() {
        assertEquals(35, ScreenLayout.verticalCenterOffset(10, 40, 120));
    }

    /**
     * Verifies centering applies the same offset to every widget.
     */
    @Test
    void centerVerticallyMovesWidgetsByComputedOffset() {
        AbstractWidget first = widgetAt(20, 10);
        AbstractWidget second = widgetAt(5, 8);

        ScreenLayout.centerVertically(List.of(first, second), 100);

        verify(first).setY(52);
        verify(second).setY(37);
    }

    /**
     * Creates a mocked clickable widget with stable bounds.
     *
     * @param y top edge
     * @param height widget height
     * @return mocked widget
     */
    private static AbstractWidget widgetAt(int y, int height) {
        AbstractWidget widget = mock(AbstractWidget.class);
        when(widget.getY()).thenReturn(y);
        when(widget.getHeight()).thenReturn(height);
        return widget;
    }
}
