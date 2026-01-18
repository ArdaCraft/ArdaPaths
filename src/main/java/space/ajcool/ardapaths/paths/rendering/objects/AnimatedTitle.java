package space.ajcool.ardapaths.paths.rendering.objects;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.data.config.shared.Color;

public class AnimatedTitle
{
    public static final float DEFAULT_CHAPTER_TITLE_DISPLAY_SPEED = 2000;

    private final String title;
    private final Color primaryColor;

    private boolean showing;
    private boolean done;

    private final float fadeDelay;

    private long startTime = -1;

    public AnimatedTitle(String title, Color primaryColor)
    {
        this.fadeDelay = ArdaPathsClient.CONFIG_MANAGER.getConfig().getChapterTitleDisplaySpeed();

        this.title = title;
        this.primaryColor = primaryColor;

        this.showing = true;
        this.done = false;
    }

    /**
     * Renders the partially revealed (and possibly fading) text onto the screen.
     * This renders independently of FPS
     * @param drawContext The draw context
     */
    public void render(DrawContext drawContext)
    {
        if (!showing) return;

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

    private void renderAnimatedTitle(DrawContext drawContext, long elapsedMillis, TextRenderer font, int width, int height) {

        int opacity;

        float fadeInSpeed = 500;
        float fadeOutSpeed = 500;

        float fadeHoldEnd = fadeInSpeed + fadeDelay;

        if (elapsedMillis < fadeInSpeed) {

            opacity = Math.max(Math.round((elapsedMillis / fadeInSpeed) * 255f),10);

        } else if (elapsedMillis < fadeHoldEnd) {

            opacity = 255;

        } else {

            float fadeOutElapsed = elapsedMillis - fadeHoldEnd;
            opacity = 255 - Math.round((fadeOutElapsed / fadeOutSpeed) * 255f);
        }

        var scale = 2.5f;
        int x = (int)(((float) width / 2) / scale);
        int y = 20;

        if (elapsedMillis > (fadeInSpeed + fadeDelay) && opacity <= 15) {
            showing = false;
            done = true;
        }

        drawText(title, primaryColor, drawContext, font, x, y, opacity, scale, -font.fontHeight/2);
    }

    private void drawText(String text, Color color, DrawContext drawContext, TextRenderer font, int x, int y, int opacity, float textScale, int textOffsetY) {

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

    public String getTitle()
    {
        return title;
    }

    public boolean isShowing()
    {
        return showing;
    }

    public boolean isDone()
    {
        return done;
    }

    public void stop()
    {
        showing = false;
        done = true;
    }

    public void reset()
    {
        this.startTime = -1;
        this.showing = true;
        this.done = false;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof AnimatedTitle other)) return super.equals(obj);

        return title.equals(other.title);
    }

    public boolean isFinished() {
        return done && !showing;
    }
}