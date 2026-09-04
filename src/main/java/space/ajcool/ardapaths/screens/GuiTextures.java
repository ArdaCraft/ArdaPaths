package space.ajcool.ardapaths.screens;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * GUI texture helpers that isolate volatile draw API signatures and tint behavior.
 */
public final class GuiTextures {

    /**
     * Extra pixels used to clip unwanted vanilla button caps fully outside a panel segment.
     */
    private static final int PANEL_CAP_INSET = 4;

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
    public static void blitSprite(GuiGraphicsExtractor context, Identifier sprite, int x, int y, int width, int height) {
        context.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height);
    }

    /**
     * Draws a panel segment from vanilla button sprites, clipping away caps for stacked rows.
     *
     * @param context draw context
     * @param x       destination x coordinate
     * @param y       destination y coordinate
     * @param width   destination width
     * @param height  destination height
     * @param state   visual panel state
     * @param cap     caps to draw for this segment
     */
    public static void drawPanelSegment(GuiGraphicsExtractor context, int x, int y, int width, int height,
                                        PanelState state, SliceCap cap) {
        switch (cap) {
            case FULL -> blitSprite(context, state.sprite(), x, y, width, height);
            case TOP -> drawClippedPanelSegment(context, state, x, y, width, height, y, height + PANEL_CAP_INSET);
            case BOTTOM -> drawClippedPanelSegment(context, state, x, y, width, height, y - PANEL_CAP_INSET,
                    height + PANEL_CAP_INSET);
            case MIDDLE -> drawClippedPanelSegment(context, state, x, y, width, height, y - PANEL_CAP_INSET,
                    height + (PANEL_CAP_INSET * 2));
            default -> throw new IllegalStateException("Unexpected panel cap: " + cap);
        }
    }

    /**
     * Draws an oversized vanilla button sprite through a scissor box to hide unneeded caps.
     *
     * @param context    draw context
     * @param state      visual panel state
     * @param x          destination x coordinate
     * @param y          destination y coordinate
     * @param width      destination width
     * @param height     destination height
     * @param textureY   y coordinate for the oversized sprite
     * @param textureHgt height for the oversized sprite
     */
    private static void drawClippedPanelSegment(GuiGraphicsExtractor context, PanelState state, int x, int y,
                                                int width, int height, int textureY, int textureHgt) {
        context.enableScissor(x, y, x + width, y + height);
        blitSprite(context, state.sprite(), x, textureY, width, textureHgt);
        context.disableScissor();
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
     * Visual state for panel-like widget backgrounds drawn from vanilla button sprites.
     */
    public enum PanelState {
        /**
         * Normal inactive panel state.
         */
        IDLE(Identifier.withDefaultNamespace("widget/button_disabled")),

        /**
         * Selected panel state.
         */
        SELECTED(Identifier.withDefaultNamespace("widget/button")),

        /**
         * Hovered panel state.
         */
        HOVERED(Identifier.withDefaultNamespace("widget/button_highlighted"));

        /**
         * Vanilla GUI sprite for this state.
         */
        private final Identifier sprite;

        /**
         * Creates a panel state mapped to a vanilla button sprite.
         *
         * @param sprite vanilla GUI sprite for this state
         */
        PanelState(Identifier sprite) {
            this.sprite = sprite;
        }

        /**
         * Resolves the vanilla atlas sprite for this panel state.
         *
         * @return GUI atlas sprite identifier
         */
        private Identifier sprite() {
            return sprite;
        }
    }
}
