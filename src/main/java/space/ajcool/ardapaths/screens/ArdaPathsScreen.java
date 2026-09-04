package space.ajcool.ardapaths.screens;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import space.ajcool.ardapaths.screens.widgets.CheckboxRow;
import space.ajcool.ardapaths.screens.widgets.OverlayRenderer;

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
     * Renders all screen content, then dispatches a single elevated-stratum pass for any widget
     * that needs to draw above everything else (expanded dropdowns, context menus).
     *
     * @param context     draw context for the current frame
     * @param mouseX      current mouse x coordinate
     * @param mouseY      current mouse y coordinate
     * @param partialTick partial tick delta
     */
    @Override
    public final void extractRenderState(@NonNull GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(context, mouseX, mouseY, partialTick);
        extractModContent(context, mouseX, mouseY, partialTick);
        extractOverlayPass(context, mouseX, mouseY, partialTick);
    }

    /**
     * Collects children that implement {@link OverlayRenderer} and have an active overlay, advances
     * the render stratum once, then draws each overlay in insertion order.
     *
     * @param context     draw context
     * @param mouseX      current mouse x
     * @param mouseY      current mouse y
     * @param partialTick partial tick delta
     */
    private void extractOverlayPass(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick) {
        boolean stratumBumped = false;
        for (var child : this.children()) {
            if (child instanceof OverlayRenderer overlay && overlay.hasOverlay()) {
                if (!stratumBumped) {
                    context.nextStratum();
                    stratumBumped = true;
                }
                overlay.extractOverlay(context, mouseX, mouseY, partialTick);
            }
        }
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
    @SuppressWarnings("unused")
    protected void extractModContent(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick) {
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
