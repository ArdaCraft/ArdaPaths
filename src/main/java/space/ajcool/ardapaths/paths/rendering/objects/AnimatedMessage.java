package space.ajcool.ardapaths.paths.rendering.objects;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ColorHelper;
import org.jetbrains.annotations.NotNull;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.core.data.BitPacker;
import space.ajcool.ardapaths.mc.blocks.entities.PathMarkerBlockEntity;
import space.ajcool.ardapaths.paths.rendering.ProximityRenderer;
import space.ajcool.ardapaths.paths.rendering.TextRenderable;

import java.util.ArrayList;

/**
 * An animated text message that reveals characters progressively and fades out over time.
 * <p>
 * This class handles the rendering of multi-line text with customizable reveal and fade animations.
 * Characters appear one by one, and after full reveal, the message gradually fades to transparency.
 */
public class AnimatedMessage extends TextRenderable {

    /**
     * Default speed multiplier for proximity-based text animation
     */
    public static final double DEFAULT_PROXIMITY_TEXT_SPEED_MULTIPLIER = 0.125d;

    /**
     * The text content to display
     */
    private final String message;

    /**
     * Milliseconds per character reveal (0 for instant reveal)
     */
    private final int charRevealSpeed;

    /**
     * Base delay before fade starts (milliseconds)
     */
    private final int fadeDelayOffset;

    /**
     * Additional fade delay per character (milliseconds)
     */
    private final int fadeDelayFactor;

    /**
     * Milliseconds per opacity point decrease during fade
     */
    private final int fadeSpeed;

    /**
     * Minimum opacity value (0-255) before message is considered done
     */
    private final int minOpacity;

    /**
     * Speed multiplier for proximity-triggered animations
     */
    private final double proximitySpeedMultiplier;

    /**
     * Creates an animated message with default animation parameters.
     *
     * @param message the text to display
     */
    public AnimatedMessage(String message) {
        this(message, 5, 100, 5, 2, 8);
    }

    /**
     * Creates an animated message with custom animation parameters.
     *
     * @param message         the text to display
     * @param charRevealSpeed milliseconds per character reveal (0 for instant)
     * @param fadeDelayOffset base delay before fade begins (milliseconds)
     * @param fadeDelayFactor additional fade delay per character (milliseconds)
     * @param fadeSpeed       milliseconds per opacity point decrease
     * @param minOpacity      minimum opacity before marking as done (0-255)
     */
    public AnimatedMessage(String message, int charRevealSpeed, int fadeDelayOffset, int fadeDelayFactor, int fadeSpeed, int minOpacity) {
        super();

        this.proximitySpeedMultiplier = ArdaPathsClient.CONFIG_MANAGER.getConfig().getProximityTextSpeedMultiplier();
        this.message = message;

        this.charRevealSpeed = charRevealSpeed;
        this.fadeDelayOffset = fadeDelayOffset;
        this.fadeDelayFactor = fadeDelayFactor;
        this.fadeSpeed = fadeSpeed;
        this.minOpacity = minOpacity;
    }

    /**
     * Creates an {@link AnimatedMessage} by unpacking message-related data from the given chapter data.
     * <p>
     * The method retrieves a packed long value that encodes five integers using bitwise operations,
     * unpacks them using {@link BitPacker#unpackFive(long)}, and uses them to construct a new
     * {@code AnimatedMessage} instance along with the proximity message.
     *
     * @param currentChapterData the chapter data containing the packed message data and proximity message
     * @return a fully constructed {@link AnimatedMessage} based on the chapter data
     */
    public static @NotNull AnimatedMessage getAnimatedMessage(PathMarkerBlockEntity.ChapterNbtData currentChapterData) {
        var packedMessageData = currentChapterData.getPackedMessageData();
        var unpackedMessageData = BitPacker.unpackFive(packedMessageData);

        if (packedMessageData == 0) return new AnimatedMessage(currentChapterData.getProximityMessage());

        return new AnimatedMessage(
                currentChapterData.getProximityMessage(),
                unpackedMessageData[0],
                unpackedMessageData[1],
                unpackedMessageData[2],
                unpackedMessageData[3],
                unpackedMessageData[4]
        );
    }

    /**
     * Renders the animated message with progressive character reveal and fade effects.
     * <p>
     * Animation timing is independent of FPS and adjusted by the proximity speed multiplier.
     * Characters reveal sequentially, with the boundary character shown in gray. Once fully
     * revealed, the entire message fades to transparency.
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

        // Calculate elapsed time with proximity speed multiplier applied
        long elapsedMillis = (long) ((System.currentTimeMillis() - startTime) * proximitySpeedMultiplier);

        var client = MinecraftClient.getInstance();
        var font = client.inGameHud.getTextRenderer();
        var width = client.getWindow().getScaledWidth();
        var height = client.getWindow().getScaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        renderAnimatedMessage(drawContext, elapsedMillis, font, width, height);

        RenderSystem.disableBlend();
    }

    /**
     * Internal rendering logic that handles character reveal, multi-line splitting, and fade calculation.
     *
     * @param drawContext   the drawing context
     * @param elapsedMillis time elapsed since animation start (with speed multiplier applied)
     * @param font          the text renderer to use
     * @param width         screen width in scaled pixels
     * @param height        screen height in scaled pixels
     */
    private void renderAnimatedMessage(DrawContext drawContext, long elapsedMillis, TextRenderer font, int width, int height) {

        // Calculate number of characters to reveal
        int textLength = message.length() + 1;
        int numChars = charRevealSpeed == 0 ? textLength : Math.max(Math.min((int) (elapsedMillis / charRevealSpeed), textLength), 1);

        // Split message into lines and build partially revealed text
        var splitMessage = message.split("\n");
        int numCharsLeft = numChars;

        var lines = new ArrayList<Text>();
        for (String line : splitMessage) {
            if (line.length() < numCharsLeft) {
                // Entire line is revealed
                lines.add(Text.literal(line));
                numCharsLeft -= line.length();
            } else {
                // Partial line with gray boundary character
                var partialLine = Text.empty()
                        .append(Text.literal(line.substring(0, numCharsLeft - 1))) // fully visible
                        .append(Text.literal(line.substring(numCharsLeft - 1, numCharsLeft))
                                .formatted(Formatting.GRAY)); // fade boundary char
                lines.add(partialLine);
                numCharsLeft = 0;
            }
            if (numCharsLeft <= 0) break;
        }

        // Calculate fade opacity
        int opacity = 255;
        int fadeDelay = fadeDelayOffset + (textLength * fadeDelayFactor);
        if (elapsedMillis > fadeDelay) {
            opacity = 255 - (int) ((elapsedMillis - fadeDelay) / fadeSpeed);
        }

        // Mark as done when opacity drops below minimum threshold
        if (opacity <= minOpacity) {
            showing = false;
            done = true;
        }

        // Adjust for title offset if a title is being displayed
        var titleOffset = ProximityRenderer.isDisplayingTitle() ? AnimatedTitle.TITLE_Y_OFFSET : 0;

        // Render each line centered on screen
        for (int i = 0; i < lines.size(); i++) {

            int y = ((height - titleOffset)/ 5) + (10 * i) + titleOffset;

            drawContext.drawCenteredTextWithShadow(
                    font,
                    lines.get(i),
                    width / 2,
                    y,
                    ColorHelper.Argb.getArgb(opacity, 255, 255, 255)
            );
        }
    }

    /**
     * Checks if the animation has completed and is no longer visible.
     *
     * @return true if the message has finished animating and fading out
     */
    @Override
    public boolean isFinished() {

        return done && !showing;
    }

    /**
     * @return the text content of this message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Compares this message with another object for equality based on message content.
     *
     * @param obj the object to compare
     * @return true if obj is an AnimatedMessage with the same message text
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AnimatedMessage other)) return super.equals(obj);

        return message.equals(other.message);
    }
}