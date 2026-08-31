package space.ajcool.ardapaths.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Base class for ArdaPaths screens that centralizes background rendering.
 */
public abstract class ArdaPathsScreen extends Screen {
    /**
     * Creates a mod screen with the supplied title.
     *
     * @param title screen title used for narration and window metadata
     */
    protected ArdaPathsScreen(Component title) {
        super(title);
    }

    /**
     * Renders the shared mod screen background.
     *
     * @param context draw context for the current frame
     */
    protected void renderModBackground(GuiGraphics context) {
        this.renderBackground(context);
    }
}
