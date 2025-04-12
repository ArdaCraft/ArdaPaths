package space.ajcool.ardapaths.paths.rendering;

import net.minecraft.client.gui.DrawContext;
import space.ajcool.ardapaths.paths.rendering.objects.AnimatedMessage;

public class ProximityMessageRenderer
{
    private static AnimatedMessage message;

    /**
     * Render the current proximity message.
     *
     * @param context The draw context
     * @param delta   The partial tick
     */
    public static void render(DrawContext context, float delta)
    {
        if (message == null) return;

        message.render(context, delta);
    }

    /**
     * Set the current proximity message. If a message is already set,
     * it will not be overwritten.
     *
     * @param newMessage The new message to render
     */
    public static void setMessage(String newMessage)
    {
        if (message != null && message.getMessage().equals(newMessage)) return;

        message = new AnimatedMessage(newMessage);
    }

    /**
     * Clear the current proximity message.
     */
    public static void clearMessage()
    {
        ProximityMessageRenderer.message = null;
    }
}
