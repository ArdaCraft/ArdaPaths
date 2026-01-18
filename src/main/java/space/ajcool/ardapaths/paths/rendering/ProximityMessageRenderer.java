package space.ajcool.ardapaths.paths.rendering;

import net.minecraft.client.gui.DrawContext;
import space.ajcool.ardapaths.paths.rendering.objects.AnimatedMessage;

import java.util.ArrayDeque;
import java.util.Queue;

public class ProximityMessageRenderer
{
    private static final Queue<AnimatedMessage> messageDeque = new ArrayDeque<>();

    private static AnimatedMessage currentDisplayedMessage;

    /**
     * Render the current proximity message.
     *
     * @param context The draw context
     * @param delta   The partial tick
     */
    public static void render(DrawContext context, float delta)
    {
        if (currentDisplayedMessage == null || currentDisplayedMessage.isFinished()) {

            if (messageDeque.isEmpty()) return;

            currentDisplayedMessage = messageDeque.poll();

            if (currentDisplayedMessage == null) return;
        };

        currentDisplayedMessage.render(context, delta);
    }

    /**
     * Set the current proximity message. If a message is already set,
     * it will not be overwritten.
     *
     * @param newMessage The new message to render
     */
    public static void setMessage(String newMessage)
    {
        var newAnimatedMessage = new AnimatedMessage(newMessage);

        if (messageDeque.contains(newAnimatedMessage)) return;
        if (currentDisplayedMessage != null && currentDisplayedMessage.equals(newAnimatedMessage)) return;

        messageDeque.add(newAnimatedMessage);
    }

    public static void setMessage(AnimatedMessage newMessage)
    {
        if (messageDeque.contains(newMessage)) return;
        if (currentDisplayedMessage != null && currentDisplayedMessage.equals(newMessage)) return;

        newMessage.reset();
        messageDeque.add(newMessage);
    }

    /**
     * Clear the current proximity message.
     */
    public static void clearMessage()
    {
        currentDisplayedMessage = null;
        messageDeque.clear();
    }
}