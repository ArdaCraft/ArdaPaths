package space.ajcool.ardapaths.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import space.ajcool.ardapaths.core.ModConstants;

/**
 * GUI texture helpers that isolate volatile draw API signatures and tint behavior.
 */
public final class GuiTextures {

    /**
     * Source width of vanilla button textures.
     */
    private static final int BUTTON_REGION_WIDTH = 200;

    /**
     * Source height of vanilla button textures.
     */
    private static final int BUTTON_REGION_HEIGHT = 20;

    /**
     * Horizontal corner slice width of vanilla button textures.
     */
    private static final int BUTTON_CORNER_WIDTH = 20;

    /**
     * Vertical corner slice height of vanilla button textures.
     */
    private static final int BUTTON_CORNER_HEIGHT = 4;

    /**
     * Local copy of the vanilla widget strip used for panel backgrounds.
     */
    private static final ResourceLocation WIDGETS_TEXTURE = ModConstants.modId("textures/gui/widgets.png");

    /**
     * Prevents construction of this utility class.
     */
    private GuiTextures() {
    }

    /**
     * Draws a GUI atlas sprite to the destination rectangle.
     *
     * @param context draw context
     * @param sprite  sprite identifier
     * @param x       destination x coordinate
     * @param y       destination y coordinate
     * @param width   destination width
     * @param height  destination height
     */
    public static void blitSprite(GuiGraphics context, ResourceLocation sprite, int x, int y, int width, int height) {
        context.blitSprite(sprite, x, y, width, height);
    }

    /**
     * Draws a nine-sliced texture region using a single corner size.
     *
     * @param context      draw context
     * @param texture      texture identifier
     * @param x            destination x coordinate
     * @param y            destination y coordinate
     * @param width        destination width
     * @param height       destination height
     * @param cornerWidth  corner slice width
     * @param cornerHeight corner slice height
     * @param regionWidth  source region width
     * @param regionHeight source region height
     * @param u            source u coordinate
     * @param v            source v coordinate
     */
    public static void blitNineSliced(GuiGraphics context, ResourceLocation texture, int x, int y, int width, int height,
                                      int cornerWidth, int cornerHeight, int regionWidth, int regionHeight,
                                      int u, int v) {
        blitNineSlicedSegment(context, texture, x, y, width, height, cornerWidth, cornerHeight,
                regionWidth, regionHeight, u, v, SliceCap.FULL);
    }

    /**
     * Draws a panel segment from the vanilla button texture using semantic state and cap selection.
     *
     * @param context draw context
     * @param x       destination x coordinate
     * @param y       destination y coordinate
     * @param width   destination width
     * @param height  destination height
     * @param state   visual panel state
     * @param cap     caps to draw for this segment
     */
    public static void drawPanelSegment(GuiGraphics context, int x, int y, int width, int height,
                                        PanelState state, SliceCap cap) {
        if (cap == SliceCap.FULL) {
            blitNineSliced(context, WIDGETS_TEXTURE, x, y, width, height,
                    BUTTON_CORNER_WIDTH, BUTTON_CORNER_HEIGHT, BUTTON_REGION_WIDTH, BUTTON_REGION_HEIGHT,
                    0, state.buttonV);
            return;
        }
        blitNineSlicedSegment(context, WIDGETS_TEXTURE, x, y, width, height,
                BUTTON_CORNER_WIDTH, BUTTON_CORNER_HEIGHT, BUTTON_REGION_WIDTH, BUTTON_REGION_HEIGHT,
                0, state.buttonV, cap);
    }

    /**
     * Draws a nine-sliced texture segment while allowing the top or bottom cap to be omitted.
     *
     * @param context      draw context
     * @param texture      texture identifier
     * @param x            destination x coordinate
     * @param y            destination y coordinate
     * @param width        destination width
     * @param height       destination height
     * @param cornerWidth  corner slice width
     * @param cornerHeight corner slice height
     * @param regionWidth  source region width
     * @param regionHeight source region height
     * @param u            source u coordinate
     * @param v            source v coordinate
     * @param cap          caps to draw for this segment
     */
    @SuppressWarnings("SameParameterValue")
    private static void blitNineSlicedSegment(GuiGraphics context, ResourceLocation texture,
                                              int x, int y, int width, int height,
                                              int cornerWidth, int cornerHeight,
                                              int regionWidth, int regionHeight,
                                              int u, int v, SliceCap cap) {
        boolean top = cap == SliceCap.TOP || cap == SliceCap.FULL;
        boolean bottom = cap == SliceCap.BOTTOM || cap == SliceCap.FULL;
        int topHeight = top ? cornerHeight : 0;
        int bottomHeight = bottom ? cornerHeight : 0;
        int bodyHeight = height - topHeight - bottomHeight;
        int centerWidth = width - 2 * cornerWidth;
        int sourceCenterWidth = regionWidth - 2 * cornerWidth;
        int sourceBodyHeight = regionHeight - 2 * cornerHeight;
        int rightU = u + regionWidth - cornerWidth;
        int rightX = x + width - cornerWidth;
        int centerX = x + cornerWidth;
        int centerU = u + cornerWidth;

        if (topHeight > 0) {
            context.blit(texture, x, y, u, v, cornerWidth, cornerHeight);
            blitRepeating(context, texture, centerX, y, centerWidth, cornerHeight,
                    centerU, v, sourceCenterWidth, cornerHeight);
            context.blit(texture, rightX, y, rightU, v, cornerWidth, cornerHeight);
        }

        if (bodyHeight > 0) {
            int bodyY = y + topHeight;
            int bodyV = v + cornerHeight;
            blitRepeating(context, texture, x, bodyY, cornerWidth, bodyHeight,
                    u, bodyV, cornerWidth, sourceBodyHeight);
            blitRepeating(context, texture, centerX, bodyY, centerWidth, bodyHeight,
                    centerU, bodyV, sourceCenterWidth, sourceBodyHeight);
            blitRepeating(context, texture, rightX, bodyY, cornerWidth, bodyHeight,
                    rightU, bodyV, cornerWidth, sourceBodyHeight);
        }

        if (bottomHeight > 0) {
            int bottomY = y + height - cornerHeight;
            int bottomV = v + regionHeight - cornerHeight;
            context.blit(texture, x, bottomY, u, bottomV, cornerWidth, cornerHeight);
            blitRepeating(context, texture, centerX, bottomY, centerWidth, cornerHeight,
                    centerU, bottomV, sourceCenterWidth, cornerHeight);
            context.blit(texture, rightX, bottomY, rightU, bottomV, cornerWidth, cornerHeight);
        }
    }

    /**
     * Tiles a source texture region across a destination rectangle.
     *
     * @param context      draw context
     * @param texture      texture identifier
     * @param x            destination x coordinate
     * @param y            destination y coordinate
     * @param width        destination width
     * @param height       destination height
     * @param u            source u coordinate
     * @param v            source v coordinate
     * @param sourceWidth  source tile width
     * @param sourceHeight source tile height
     */
    private static void blitRepeating(GuiGraphics context, ResourceLocation texture, int x, int y, int width, int height,
                                      int u, int v, int sourceWidth, int sourceHeight) {
        for (int offsetY = 0; offsetY < height; offsetY += sourceHeight) {
            int tileHeight = Math.min(sourceHeight, height - offsetY);
            for (int offsetX = 0; offsetX < width; offsetX += sourceWidth) {
                int tileWidth = Math.min(sourceWidth, width - offsetX);
                context.blit(texture, x + offsetX, y + offsetY, u, v, tileWidth, tileHeight);
            }
        }
    }

    /**
     * Creates an ARGB color from integer channel values.
     *
     * @param alpha alpha channel from 0 to 255
     * @param red   red channel from 0 to 255
     * @param green green channel from 0 to 255
     * @param blue  blue channel from 0 to 255
     * @return packed ARGB color
     */
    public static int argb(int alpha, int red, int green, int blue) {
        return (component(alpha) << 24)
                | (component(red) << 16)
                | (component(green) << 8)
                | component(blue);
    }

    /**
     * Converts an integer channel to the valid byte range.
     *
     * @param value channel value
     * @return clamped channel value
     */
    private static int component(int value) {
        return Math.max(0, Math.min(255, value));
    }

    /**
     * Replaces the alpha channel of an ARGB color.
     *
     * @param argb  original packed ARGB color
     * @param alpha alpha channel from 0 to 255
     * @return packed ARGB color with the supplied alpha
     */
    public static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | (component(alpha) << 24);
    }

    /**
     * Which horizontal caps of a nine-slice segment should be rendered.
     */
    public enum SliceCap {
        /**
         * Draws both the top and bottom caps.
         */
        FULL,

        /**
         * Draws only the top cap.
         */
        TOP,

        /**
         * Draws no horizontal caps.
         */
        MIDDLE,

        /**
         * Draws only the bottom cap.
         */
        BOTTOM
    }

    /**
     * Visual state for panel-like widget backgrounds.
     */
    public enum PanelState {
        /**
         * Normal inactive panel state.
         */
        IDLE(46),

        /**
         * Selected panel state.
         */
        SELECTED(66),

        /**
         * Hovered panel state.
         */
        HOVERED(86);

        /**
         * Source v coordinate for the 1.20.1 vanilla button texture strip.
         */
        private final int buttonV;

        /**
         * Creates a panel state mapped to a vanilla button texture strip.
         *
         * @param buttonV source v coordinate for the backing button strip
         */
        PanelState(int buttonV) {
            this.buttonV = buttonV;
        }
    }
}
