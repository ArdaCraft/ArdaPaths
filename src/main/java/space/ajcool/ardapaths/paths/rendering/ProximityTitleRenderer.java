package space.ajcool.ardapaths.paths.rendering;

import net.minecraft.client.gui.DrawContext;
import space.ajcool.ardapaths.core.data.config.shared.Color;
import space.ajcool.ardapaths.paths.rendering.objects.AnimatedTitle;

public class ProximityTitleRenderer
{
    private static AnimatedTitle title;

    /**
     * Render the current proximity message.
     *
     * @param context The draw context
     * @param delta   The partial tick
     */
    public static void render(DrawContext context, float delta)
    {
        if (title == null) return;

        title.render(context);
    }

    /**
     * Set the current proximity message. If a message is already set,
     * it will not be overwritten.
     *
     * @param nIndex    The index of the title
     * @param nTitle The new message to render
     * @param nPrimaryColor The color of the chapter
     * @param nSecondaryColor The color of the chapter title
     */
    public static void setTitle(int nIndex, String nTitle, Color nPrimaryColor, Color nSecondaryColor)
    {

        AnimatedTitle newTitle = new AnimatedTitle(nIndex, nTitle, nPrimaryColor, nSecondaryColor);

        if (newTitle.equals(title)) return;

        title = newTitle;
        title.reset();
    }

    /**
     * Clear the current proximity message.
     */
    public static void clearMessage()
    {
        ProximityTitleRenderer.title = null;
    }
}
