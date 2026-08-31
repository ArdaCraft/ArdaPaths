package space.ajcool.ardapaths.screens.widgets;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * Scrollable list of loaded path markers available from the marker editor.
 */
public class MarkerListWidget extends AbstractSelectionList<MarkerListEntry> {

    /**
     * Create a marker list with fixed-height marker rows.
     *
     * @param client     the Minecraft client
     * @param width      the list width
     * @param height     the screen height
     * @param top        the top list boundary
     * @param bottom     the bottom list boundary
     * @param itemHeight the fixed row height
     */
    public MarkerListWidget(Minecraft client, int width, int height, int top, int bottom, int itemHeight) {
        super(client, width, height, top, bottom, itemHeight);
        setRenderBackground(false);
        setRenderHeader(false, 0);
        setRenderTopAndBottom(false);
    }

    /**
     * Returns the row width with room reserved for the scrollbar.
     *
     * @return the available row width
     */
    @Override
    public int getRowWidth() {
        return this.width - 12;
    }

    /**
     * Renders the marker list, then draws any hovered row tooltip outside of the list clipping bounds.
     *
     * @param context   the draw context
     * @param mouseX    the mouse x coordinate
     * @param mouseY    the mouse y coordinate
     * @param delta     the partial tick
     */
    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        MarkerListEntry hovered = this.getHovered();
        if (hovered != null) {
            hovered.renderTooltip(context, this.minecraft.font, mouseX, mouseY);
        }
    }

    /**
     * Positions the scrollbar at the right edge of the marker column.
     *
     * @return the scrollbar x coordinate
     */
    @Override
    protected int getScrollbarPosition() {
        return this.x0 + this.width - 6;
    }

    /**
     * Replaces the visible marker entries and optionally centers the selected row.
     *
     * @param markers          the entries to display
     * @param scrollToSelected whether to center the row for the marker being edited
     */
    public void setMarkers(List<MarkerListEntry> markers, boolean scrollToSelected) {
        double scrollAmount = this.getScrollAmount();
        this.clearEntries();

        for (MarkerListEntry marker : markers) {
            this.addEntry(marker);
        }

        if (scrollToSelected) {
            MarkerListEntry selected = markers.stream()
                    .filter(MarkerListEntry::isFocused)
                    .findFirst()
                    .orElse(null);

            if (selected != null) {
                setSelected(selected);
                centerScrollOn(selected);
            }
        } else {
            this.setScrollAmount(scrollAmount);
        }
    }

    /**
     * Routes right-clicks through row hit-testing because the vanilla list only selects with left clicks.
     *
     * @param mouseX mouse x coordinate
     * @param mouseY mouse y coordinate
     * @param button clicked mouse button
     * @return true when a row or the list consumes the click
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            MarkerListEntry entry = this.getEntryAtPosition(mouseX, mouseY);
            return entry != null && entry.mouseClicked(mouseX, mouseY, button);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Appends the active marker row narration for accessibility.
     *
     * @param builder the narration builder to populate
     */
    @Override
    public void updateNarration(NarrationElementOutput builder) {
        MarkerListEntry selected = this.getSelected();

        if (selected != null)
            builder.add(NarratedElementType.TITLE, selected.getNarration());
        else if (this.getItemCount() > 0)
            builder.add(NarratedElementType.TITLE, Component.translatable("narration.selection.usage"));

        builder.add(NarratedElementType.USAGE, Component.translatable("narration.component_list.usage"));
    }
}
