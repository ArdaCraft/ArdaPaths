package space.ajcool.ardapaths.screens.layout;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;

import java.util.Collection;
import java.util.List;

/**
 * Stateless helpers for measuring and repositioning screen widget groups.
 */
public final class ScreenLayout {
    /**
     * Prevents construction of this utility class.
     */
    private ScreenLayout()
    {
    }

    /**
     * Measures the vertical span occupied by a collection of widgets.
     *
     * @param widgets the widgets to measure
     * @return the distance between the highest top edge and lowest bottom edge, or zero when empty
     */
    public static int contentHeight(Collection<? extends ClickableWidget> widgets)
    {
        if (widgets.isEmpty()) {
            return 0;
        }

        int minTop = Integer.MAX_VALUE;
        int maxBottom = Integer.MIN_VALUE;
        for (ClickableWidget widget : widgets) {
            minTop = Math.min(minTop, widget.getY());
            maxBottom = Math.max(maxBottom, widget.getY() + widget.getHeight());
        }

        return maxBottom - minTop;
    }

    /**
     * Calculates the vertical offset needed to center a bounded content block.
     *
     * @param minTop       the top edge of the content block
     * @param maxBottom    the bottom edge of the content block
     * @param screenHeight the height of the containing screen
     * @return the offset to add to each y coordinate in the content block
     */
    public static int verticalCenterOffset(int minTop, int maxBottom, int screenHeight)
    {
        int contentHeight = maxBottom - minTop;
        return (screenHeight - contentHeight) / 2 - minTop;
    }

    /**
     * Vertically centers a collection of widgets inside a screen height.
     *
     * @param widgets      the widgets to reposition
     * @param screenHeight the height of the containing screen
     */
    public static void centerVertically(Collection<? extends ClickableWidget> widgets, int screenHeight)
    {
        if (widgets.isEmpty()) {
            return;
        }

        int minTop = Integer.MAX_VALUE;
        for (ClickableWidget widget : widgets) {
            minTop = Math.min(minTop, widget.getY());
        }

        int offset = verticalCenterOffset(minTop, minTop + contentHeight(widgets), screenHeight);
        for (ClickableWidget widget : widgets) {
            widget.setY(widget.getY() + offset);
        }
    }

    /**
     * Vertically centers all clickable widgets currently attached to a screen.
     *
     * @param screen the screen whose clickable widgets should be repositioned
     */
    public static void centerVertically(Screen screen)
    {
        List<ClickableWidget> widgets = screen.children().stream()
                .filter(ClickableWidget.class::isInstance)
                .map(ClickableWidget.class::cast)
                .toList();

        centerVertically(widgets, screen.height);
    }
}
