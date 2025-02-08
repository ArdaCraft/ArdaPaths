package space.ajcool.ardapaths.trails.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ColorHelper;

import java.util.ArrayList;

public class AnimatedMessage {
    private static final int CHAR_REVEAL_SPEED = 5;
    private static final int FADE_DELAY_OFFSET = 200;
    private static final int FADE_DELAY_FACTOR = 5;
    private static final int FADE_SPEED = 2;
    private static final int MIN_OPACITY = 8;

    private final String message;
    private int timeAlive;
    private boolean showing;
    private boolean done;

    public AnimatedMessage(String message) {
        this.message = message;
        this.timeAlive = 0;
        this.showing = true;
        this.done = false;
    }

    /**
     * Renders the partially revealed (and possibly fading) text onto the screen.
     *
     * @param drawContext The draw context
     * @param tickDelta The partial tick
     */
    public void render(DrawContext drawContext, float tickDelta) {
        if (!showing) return;

        this.timeAlive++;
        var client = MinecraftClient.getInstance();
        var font = client.inGameHud.getTextRenderer();
        var width = client.getWindow().getScaledWidth();
        var height = client.getWindow().getScaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int textLength = message.length() + 1;
        int numChars = Math.max(Math.min(timeAlive / CHAR_REVEAL_SPEED, textLength), 1);

        var splitMessage = message.split("\n");
        int numCharsLeft = numChars;

        var lines = new ArrayList<Text>();
        for (String line : splitMessage) {
            if (line.length() < numCharsLeft) {
                lines.add(Text.literal(line));
                numCharsLeft -= line.length();
            } else {
                var partialLine = Text.empty()
                        .append(Text.literal(line.substring(0, numCharsLeft - 1))) // fully visible
                        .append(Text.literal(line.substring(numCharsLeft - 1, numCharsLeft))
                                .formatted(Formatting.GRAY)); // fade boundary char
                lines.add(partialLine);
                numCharsLeft = 0;
            }
            if (numCharsLeft <= 0) break;
        }

        int opacity = 255;
        int fadeDelay = FADE_DELAY_OFFSET + (textLength * FADE_DELAY_FACTOR);
        if (timeAlive > fadeDelay) {
            opacity = 255 - ((timeAlive - fadeDelay) * FADE_SPEED);
        }

        if (opacity <= MIN_OPACITY) {
            showing = false;
            done = true;
        }

        for (int i = 0; i < lines.size(); i++) {
            drawContext.drawCenteredTextWithShadow(
                    font,
                    lines.get(i),
                    width / 2,
                    (height / 5) + (10 * i),
                    ColorHelper.Argb.getArgb(opacity, 255, 255, 255)
            );
        }

        RenderSystem.disableBlend();
    }

    public boolean isShowing() {
        return showing;
    }

    public boolean isDone() {
        return done;
    }

    public void stop() {
        showing = false;
        done = true;
    }
}
