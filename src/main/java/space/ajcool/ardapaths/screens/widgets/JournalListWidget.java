package space.ajcool.ardapaths.screens.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * A custom list widget for displaying journal entries with variable heights.
 */
public class JournalListWidget extends AbstractSelectionList<JournalListEntry> {

    /**
     * Create a new JournalListWidget.
     *
     * @param client     The Minecraft client
     * @param width      The width of the widget
     * @param height     The height of the widget
     * @param top        The top position of the widget
     * @param bottom     The bottom position of the widget
     * @param itemHeight The height of each item (not used for variable height entries)
     */
    public JournalListWidget(Minecraft client, int width, int height, int top, int bottom, int itemHeight) {
        super(client, width, height, top, bottom, itemHeight);
        setRenderBackground(false);
        setRenderHeader(false, 0);
        setRenderTopAndBottom(false);
    }

    /**
     * Get the width available for each row, accounting for padding.
     */
    @Override
    public int getRowWidth() {
        return this.width - 40;
    }

    /**
     * Position the scrollbar on the right side of the list.
     */
    @Override
    protected int getScrollbarPosition() {
        return this.x0 + this.width - 6;
    }

    /**
     * Add a journal entry to the list.
     *
     * @param entry the journal entry to add
     */
    public void addJournalEntry(JournalListEntry entry) {
        this.addEntry(entry);
    }

    /**
     * Moves the list bounds vertically while preserving its height.
     *
     * @param offset the number of pixels to add to the list's top and bottom bounds
     */
    public void offsetY(int offset) {
        this.y0 += offset;
        this.y1 += offset;
    }

    /**
     * Calculate the maximum scroll position based on variable entry heights.
     */
    @Override
    protected int getMaxPosition() {
        int total = 0;
        for (int i = 0; i < this.getItemCount(); i++) {
            total += this.getEntry(i).getHeight(getRowWidth());
        }
        return total + this.headerHeight;
    }

    /** Render the list with variable height entries.
     * @param context The draw context
     * @param mouseX  The mouse x position
     * @param mouseY  The mouse y position
     * @param delta   The delta time
     */
    @Override
    protected void renderList(GuiGraphics context, int mouseX, int mouseY, float delta) {

        int rowLeft = this.getRowLeft();
        int rowWidth = this.getRowWidth();
        int currentY = this.y0 + 4 - (int) this.getScrollAmount();

        for (int i = 0; i < this.getItemCount(); i++) {

            JournalListEntry entry = this.getEntry(i);
            int entryHeight = entry.getHeight(rowWidth);

            if (currentY + entryHeight >= this.y0 && currentY <= this.y1) {
                entry.render(context, i, currentY, rowLeft, rowWidth, entryHeight,
                        mouseX, mouseY, this.isMouseOver(mouseX, mouseY) && this.getEntryAtPosition(mouseX, mouseY) == entry, delta);
            }
            currentY += entryHeight;
        }
    }

    /**
     * Get the entry at the given position, accounting for variable heights.
     * @param x The x position
     * @param y The y position
     * @return  The entry at the given position, or null if none.
     */
    @SuppressWarnings("unused")
    private JournalListEntry getJournalEntryAtPosition(double x, double y) {

        int currentY = this.y0 + 4 - (int) this.getScrollAmount();

        for (int i = 0; i < this.getItemCount(); i++) {
            JournalListEntry entry = this.getEntry(i);
            int entryHeight = entry.getHeight(getRowWidth());
            if (y >= currentY && y < currentY + entryHeight) {
                return entry;
            }
            currentY += entryHeight;
        }
        return null;
    }

    /**
     * Handle mouse clicks to delegate to entries.
     * This is necessary because entries have variable heights.
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     * @param button  The mouse button
     * @return true if the click was handled by an entry, false otherwise.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        }

        JournalListEntry entry = getJournalEntryAtPosition(mouseX, mouseY);
        if (entry != null) {
            if (entry.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Append narration information for accessibility.
     */
    @Override
    public void updateNarration(NarrationElementOutput builder) {

        JournalListEntry selected = this.getSelected();

        if (selected != null)
            builder.add(NarratedElementType.TITLE, selected.getNarration());
        else if (this.getItemCount() > 0)
            builder.add(NarratedElementType.TITLE, Component.translatable("narration.selection.usage"));

        builder.add(NarratedElementType.USAGE, Component.translatable("narration.component_list.usage"));
    }
}
