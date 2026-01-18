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

import java.util.ArrayList;

public class AnimatedMessage
{
    public static final double DEFAULT_PROXIMITY_TEXT_SPEED_MULTIPLIER = 0.125d;

    private final String message;

    private final int charRevealSpeed;
    private final int fadeDelayOffset;
    private final int fadeDelayFactor;
    private final int fadeSpeed;
    private final int minOpacity;

    private final double proximitySpeedMultiplier;
    private boolean showing;
    private boolean done;

    private long startTime = -1;

    public AnimatedMessage(String message)
    {
        this(message, 5, 100, 5, 2, 8);
    }

    public AnimatedMessage(String message, int charRevealSpeed, int fadeDelayOffset, int fadeDelayFactor, int fadeSpeed, int minOpacity)
    {
        this.proximitySpeedMultiplier = ArdaPathsClient.CONFIG_MANAGER.getConfig().getProximityTextSpeedMultiplier();
        this.message = message;

        this.charRevealSpeed = charRevealSpeed;
        this.fadeDelayOffset = fadeDelayOffset;
        this.fadeDelayFactor = fadeDelayFactor;
        this.fadeSpeed = fadeSpeed;
        this.minOpacity = minOpacity;

        this.showing = true;
        this.done = false;
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
    public static @NotNull AnimatedMessage getAnimatedMessage(PathMarkerBlockEntity.ChapterNbtData currentChapterData)
    {
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
     * Renders the partially revealed (and possibly fading) text onto the screen.
     * This renders independently of FPS
     * @param drawContext The draw context
     * @param tickDelta   The partial tick
     */
    public void render(DrawContext drawContext, float tickDelta)
    {
        if (!showing) return;

        if (startTime == -1) {
            startTime = System.currentTimeMillis();
        }

        long elapsedMillis = (long)((System.currentTimeMillis() - startTime) * proximitySpeedMultiplier);

        var client = MinecraftClient.getInstance();
        var font = client.inGameHud.getTextRenderer();
        var width = client.getWindow().getScaledWidth();
        var height = client.getWindow().getScaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        renderAnimatedMessage(drawContext, elapsedMillis, font, width, height);

        RenderSystem.disableBlend();
    }

    private void renderAnimatedMessage(DrawContext drawContext, long elapsedMillis, TextRenderer font, int width, int height) {

        int textLength = message.length() + 1;
        int numChars = charRevealSpeed == 0 ? textLength : Math.max(Math.min((int) (elapsedMillis / charRevealSpeed), textLength), 1);

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
        int fadeDelay = fadeDelayOffset + (textLength * fadeDelayFactor);
        if (elapsedMillis > fadeDelay) {
            opacity = 255 - (int) ((elapsedMillis - fadeDelay) / fadeSpeed);
        }

        if (opacity <= minOpacity) {
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
    }

    public String getMessage ()
    {
        return message;
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
        if (!(obj instanceof AnimatedMessage other)) return super.equals(obj);

        return message.equals(other.message);
    }

    public boolean isFinished() {

        return done && !showing;
    }
}