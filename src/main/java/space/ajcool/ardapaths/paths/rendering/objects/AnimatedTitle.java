package space.ajcool.ardapaths.paths.rendering.objects;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.consumers.ArdaRegionsState;
import space.ajcool.ardapaths.core.data.config.shared.Color;
import space.ajcool.ardapaths.paths.rendering.TextRenderable;

/**
 * An animated title that fades in, holds, and then fades out over time.
 * <p>
 * This class handles rendering of scaled title text with a three-phase animation:
 * fade in, hold at full opacity, and fade out. The title is displayed centered
 * on screen with customizable color and configurable display duration.
 */
public class AnimatedTitle extends TextRenderable {

    /**
     * Default duration in milliseconds to display the title at full opacity
     */
    public static final float DEFAULT_CHAPTER_TITLE_DISPLAY_SPEED = 2000;

    /**
     * Vertical offset from the top of the screen to render the title
     */
    public static final int TITLE_Y_OFFSET = 20;

    /**
     * The title text to display
     */
    private final String title;

    /**
     * The RGB color used to render the title
     */
    private final Color primaryColor;

    /**
     * Duration in milliseconds to hold the title at full opacity
     */
    private final float fadeDelay;

    /**
     * Creates an animated title with the specified text and color.
     * <p>
     * The display duration is retrieved from the current configuration.
     *
     * @param title        the text to display
     * @param primaryColor the color to render the title in
     */
    public AnimatedTitle(String title, Color primaryColor) {
        super();

        this.fadeDelay = ArdaPathsClient.CONFIG_MANAGER.getConfig().getChapterTitleDisplaySpeed();

        this.title = title;
        this.primaryColor = primaryColor;
    }

    /**
     * Renders the animated title with fade-in, hold, and fade-out effects.
     * <p>
     * Animation timing is independent of FPS. The title goes through three phases:
     * <ul>
     *     <li>Fade in (500ms): opacity increases from 10 to 255</li>
     *     <li>Hold (configurable): remains at full opacity</li>
     *     <li>Fade out (500ms): opacity decreases to 0</li>
     * </ul>
     *
     * @param drawContext the drawing context used for rendering
     */
    @Override
    public void render(DrawContext drawContext) {
        if (!showing) return;

        // Initialize start time on first render
        if (startTime == -1) {
            startTime = System.currentTimeMillis();
        }

        long elapsedMillis = System.currentTimeMillis() - startTime;

        var client = MinecraftClient.getInstance();
        var font = client.inGameHud.getTextRenderer();
        var width = client.getWindow().getScaledWidth();
        var height = client.getWindow().getScaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        renderAnimatedTitle(drawContext, elapsedMillis, font, width, height);

        RenderSystem.disableBlend();
    }

    /**
     * Internal rendering logic that calculates opacity based on animation phase.
     * <p>
     * The title animates through three phases with the following timing:
     * <ul>
     *     <li>0-500ms: fade in from 10 to 255 opacity</li>
     *     <li>500ms-(500+fadeDelay)ms: hold at 255 opacity</li>
     *     <li>(500+fadeDelay)ms onward: fade out from 255 to 0 opacity</li>
     * </ul>
     * Once opacity drops below 15, the title is marked as done.
     *
     * @param drawContext   the drawing context
     * @param elapsedMillis time elapsed since animation start
     * @param font          the text renderer to use
     * @param width         screen width in scaled pixels
     * @param height        screen height in scaled pixels
     */
    private void renderAnimatedTitle(DrawContext drawContext, long elapsedMillis, TextRenderer font, int width, int height) {

        int opacity;

        float fadeInSpeed = 500;
        float fadeOutSpeed = 500;

        float fadeHoldEnd = fadeInSpeed + fadeDelay;

        // Phase 1: Fade in
        if (elapsedMillis < fadeInSpeed) {

            opacity = Math.max(Math.round((elapsedMillis / fadeInSpeed) * 255f), 10);

        // Phase 2: Hold at full opacity
        } else if (elapsedMillis < fadeHoldEnd) {

            opacity = 255;

        // Phase 3: Fade out
        } else {

            float fadeOutElapsed = elapsedMillis - fadeHoldEnd;
            opacity = 255 - Math.round((fadeOutElapsed / fadeOutSpeed) * 255f);
        }

        var scale = 2.5f;
        int x = (int) (((float) width / 2) / scale);

        // Mark as done when fade out completes
        if (elapsedMillis > (fadeInSpeed + fadeDelay) && opacity <= 15) {
            showing = false;
            done = true;
        }

        int y = TITLE_Y_OFFSET;

        if (ArdaRegionsState.isDisplaying()) y = (int) ((height / 2f) / scale);

        drawTitleText(title, primaryColor, drawContext, font, x, y, opacity, scale, -font.fontHeight / 2);
    }

    /**
     * Renders the title text with the specified transformations and opacity.
     * <p>
     * The text is scaled up and centered horizontally on screen with a shadow effect.
     *
     * @param text        the text to render
     * @param color       the RGB color to use
     * @param drawContext the drawing context
     * @param font        the text renderer
     * @param x           the x position (pre-scaled)
     * @param y           the y position (pre-scaled)
     * @param opacity     the opacity value (0-255)
     * @param textScale   the scale factor to apply
     * @param textOffsetY vertical offset to apply after scaling
     */
    private void drawTitleText(String text, Color color, DrawContext drawContext, TextRenderer font, int x, int y, int opacity, float textScale, int textOffsetY) {

        MatrixStack matrices = drawContext.getMatrices();
        matrices.push();

        matrices.scale(textScale, textScale, 1f);

        drawContext.drawCenteredTextWithShadow(
                font,
                text,
                x,
                y + textOffsetY,
                ColorHelper.Argb.getArgb(opacity, color.r, color.g, color.b));

        matrices.pop();
    }

    /**
     * Checks if the animation has completed and is no longer visible.
     *
     * @return true if the title has finished animating and fading out
     */
    public boolean isFinished() {
        return done && !showing;
    }

    /**
     * @return the title text content
     */
    public String getTitle() {
        return title;
    }

    /**
     * Compares this title with another object for equality based on title text.
     *
     * @param obj the object to compare
     * @return true if obj is an AnimatedTitle with the same title text
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AnimatedTitle other)) return super.equals(obj);

        return title.equals(other.title);
    }
}