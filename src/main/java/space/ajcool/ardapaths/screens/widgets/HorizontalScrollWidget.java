package space.ajcool.ardapaths.screens.widgets;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.List;

public class HorizontalScrollWidget extends ClickableWidget {
    private final int maxWidth;
    private final int maxHeight;
    private final List<HorizontalScrollChild> children;

    // Calculated full content size (before scrolling/clipping)
    private int contentWidth;
    private int contentHeight;
    // The current horizontal scroll offset (in pixels)
    private int scrollX = 0;
    // Whether a horizontal scrollbar is needed.
    private boolean hasScrollbar = false;
    // Height reserved for the scrollbar, if present.
    private final int SCROLLBAR_HEIGHT = 12;

    /**
     * Constructs a horizontal scroll container.
     *
     * @param x         The x-position on screen.
     * @param y         The y-position on screen.
     * @param maxWidth  The maximum allowed width of the widget.
     * @param maxHeight The maximum allowed height of the widget.
     * @param children  Varargs of child elements implementing HorizontalScrollChild.
     */
    public HorizontalScrollWidget(int x, int y, int maxWidth, int maxHeight, HorizontalScrollChild... children) {
        // Start with the maximum sizes; we adjust the effective drawing area below.
        super(x, y, maxWidth, maxHeight, Text.empty());
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.children = Arrays.asList(children);
        calculateContentBounds();

        // Cap the widget's dimensions to the maximum allowed.
        int finalWidth = Math.min(contentWidth, maxWidth);
        int finalHeight = Math.min(contentHeight, maxHeight);
        // If a scrollbar is needed, the available height for children is reduced.
        if (hasScrollbar) {
            // We still set the overall widget height as finalHeight,
            // but the children drawing area is (finalHeight - SCROLLBAR_HEIGHT).
            // (finalHeight is already capped to maxHeight.)
        }
        this.width = finalWidth;
        this.height = finalHeight;
    }

    public HorizontalScrollWidget(int x, int y, int maxWidth, int maxHeight, List<HorizontalScrollChild> children) {
        this(x, y, maxWidth, maxHeight, children.toArray(new HorizontalScrollChild[0]));
    }

    /**
     * Calculates the bounding rectangle of all child elements.
     * Also determines if a scrollbar is required.
     */
    private void calculateContentBounds() {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (HorizontalScrollChild child : children) {
            int cx = child.getX();
            int cy = child.getY();
            int cwidth = child.getWidth();
            int cheight = child.getHeight();
            if (cx < minX) {
                minX = cx;
            }
            if (cy < minY) {
                minY = cy;
            }
            if (cx + cwidth > maxX) {
                maxX = cx + cwidth;
            }
            if (cy + cheight > maxY) {
                maxY = cy + cheight;
            }
        }
        contentWidth = maxX - minX;
        contentHeight = maxY - minY;
        // If the full content width exceeds the maximum allowed width, we need a scrollbar.
        hasScrollbar = (contentWidth > maxWidth);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = this.getX();
        int y = this.getY();

        // Draw the widget's background (for visual clarity; adjust color as needed)
        context.fill(x, y, x + this.width, y + this.height, 0xFF777777);

        // Determine the area in which child elements are drawn.
        int contentAreaHeight = hasScrollbar ? this.height - SCROLLBAR_HEIGHT : this.height;

        // Draw each child if its (scrolled) position is within the visible area.
        for (HorizontalScrollChild child : children) {
            // Calculate the child's screen X position, offset by scrollX.
            int childScreenX = x + child.getX() - scrollX;
            int childScreenY = y + child.getY();
            // Perform a simple bounds check before drawing.
            if (childScreenX + child.getWidth() > x &&
                    childScreenX < x + this.width &&
                    childScreenY + child.getHeight() > y &&
                    childScreenY < y + contentAreaHeight) {
                // Save the current transform.
                context.getMatrices().push();
                // Translate so the child can draw relative to its own origin.
                context.getMatrices().translate(childScreenX, childScreenY, 0);
                child.render(context, mouseX, mouseY, delta);
                context.getMatrices().pop();
            }
        }

        // If the content is wider than the widget, draw a horizontal scrollbar.
        if (hasScrollbar) {
            int sbX = x;
            int sbY = y + contentAreaHeight; // Scrollbar at the bottom of content area.
            int sbWidth = this.width;
            int sbHeight = SCROLLBAR_HEIGHT;
            // Draw the scrollbar track in light gray.
            context.fill(sbX, sbY, sbX + sbWidth, sbY + sbHeight, 0xFFCCCCCC);

            // Compute the thumb (handle) width based on the ratio of visible width to content width.
            float visibleRatio = (float)this.width / (float)contentWidth;
            int thumbWidth = (int)(sbWidth * visibleRatio);
            thumbWidth = Math.max(thumbWidth, 10); // Enforce a minimum thumb width.
            int maxScroll = contentWidth - this.width;
            float scrollPercent = maxScroll == 0 ? 0 : (float) scrollX / (float) maxScroll;
            int thumbX = sbX + (int)((sbWidth - thumbWidth) * scrollPercent);
            // Draw the thumb in a darker gray.
            context.fill(thumbX, sbY, thumbX + thumbWidth, sbY + sbHeight, 0xFF888888);
        }
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {}

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        // Only scroll if the mouse is over the widget and if a scrollbar is active.
        if (isMouseOver(mouseX, mouseY) && hasScrollbar) {
            int maxScroll = contentWidth - this.width;
            // Adjust scrollX by an increment (20 pixels per scroll notch; adjust as desired).
            scrollX -= (int)(amount * 20);
            if (scrollX < 0) {
                scrollX = 0;
            }
            if (scrollX > maxScroll) {
                scrollX = maxScroll;
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    // You could add additional mouse handling here if you want to support dragging the scrollbar thumb.
}

