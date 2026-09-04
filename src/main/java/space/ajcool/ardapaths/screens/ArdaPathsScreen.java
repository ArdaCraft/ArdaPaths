package space.ajcool.ardapaths.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import space.ajcool.ardapaths.screens.widgets.CheckboxRow;

/**
 * Base class for ArdaPaths screens that centralizes background and content rendering.
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
     * Renders the vanilla screen frame once, then renders direct mod content after the background blur pass.
     * Keeping this sequence final prevents subclasses from drawing text before a later blur pass can affect it.
     *
     * @param context     draw context for the current frame
     * @param mouseX      current mouse x coordinate
     * @param mouseY      current mouse y coordinate
     * @param partialTick partial tick delta
     */
    @Override
    public final void render(GuiGraphics context, int mouseX, int mouseY, float partialTick) {
        super.render(context, mouseX, mouseY, partialTick);
        renderModContent(context, mouseX, mouseY, partialTick);
    }

    /**
     * Renders direct screen content after vanilla has drawn the background, blur pass, and widgets.
     * Subclasses should use this hook for raw text or graphics that are not represented by renderable widgets.
     *
     * @param context     draw context for the current frame
     * @param mouseX      current mouse x coordinate
     * @param mouseY      current mouse y coordinate
     * @param partialTick partial tick delta
     */
    protected void renderModContent(GuiGraphics context, int mouseX, int mouseY, float partialTick) {
    }

    /**
     * Attaches both halves of a checkbox row to this screen.
     *
     * @param row the checkbox row to attach
     * @return the attached row, for callers that need to toggle it later
     */
    protected CheckboxRow addCheckboxRow(CheckboxRow row) {
        this.addRenderableWidget(row.getLabel());
        this.addRenderableWidget(row.getCheckbox());

        return row;
    }
}
