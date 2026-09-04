package space.ajcool.ardapaths.screens.widgets;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Marks a widget that needs to draw an overlay (e.g. an expanded dropdown list or context menu)
 * above all other screen content.
 * <p>
 * {@link space.ajcool.ardapaths.screens.ArdaPathsScreen} collects widgets implementing this
 * interface and, after all ordinary renderables have been submitted, advances the render stratum
 * once and calls {@link #extractOverlay} on each pending overlay in insertion order.
 */
public interface OverlayRenderer {

    /**
     * Returns whether this widget currently has an overlay to draw.
     *
     * @return true if an overlay pass is needed this frame
     */
    boolean hasOverlay();

    /**
     * Draws the overlay content at the elevated stratum.
     *
     * @param context   draw context
     * @param mouseX    current mouse x
     * @param mouseY    current mouse y
     * @param partialTick partial tick delta
     */
    @SuppressWarnings("unused")
    void extractOverlay(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick);
}
