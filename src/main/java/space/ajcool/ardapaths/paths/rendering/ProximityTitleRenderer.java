package space.ajcool.ardapaths.paths.rendering;

import net.minecraft.client.gui.DrawContext;
import space.ajcool.ardapaths.core.data.config.shared.Color;
import space.ajcool.ardapaths.paths.rendering.objects.AnimatedTitle;

import java.util.ArrayDeque;
import java.util.Queue;

public class ProximityTitleRenderer
{
    private static final Queue<AnimatedTitle> titleDeque = new ArrayDeque<>();

    private static AnimatedTitle currentDisplayedTitle;

    /**
     * Render the current proximity message.
     *
     * @param context The draw context
     * @param delta   The partial tick
     */
    public static void render(DrawContext context, float delta)
    {
        if (currentDisplayedTitle ==  null || currentDisplayedTitle.isFinished()) {

            if (titleDeque.isEmpty()) return;

            currentDisplayedTitle = titleDeque.poll();

            if (currentDisplayedTitle == null) return;
        }

        currentDisplayedTitle.render(context);
    }

    /**
     * Set the current proximity message. If a message is already set,
     * it will not be overwritten.
     *
     * @param nTitle The new message to render
     * @param nPrimaryColor The color of the chapter
     */
    public static void setTitle(String nTitle, Color nPrimaryColor)
    {

        AnimatedTitle newTitle = new AnimatedTitle(nTitle, nPrimaryColor);

        if (titleDeque.contains(newTitle)) return;
        if (currentDisplayedTitle != null && currentDisplayedTitle.equals(newTitle)) return;
        newTitle.reset();
        titleDeque.add(newTitle);
    }

    /**
     * Clear the current proximity message.
     */
    public static void clearMessage()
    {
        currentDisplayedTitle = null;
        titleDeque.clear();
    }
}