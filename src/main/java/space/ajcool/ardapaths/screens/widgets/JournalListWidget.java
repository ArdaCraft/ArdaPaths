package space.ajcool.ardapaths.screens.widgets;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.text.Text;

/**
 * A custom list widget for displaying journal entries with variable heights.
 */
public class JournalListWidget extends EntryListWidget<JournalListEntry> {

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
    public JournalListWidget(MinecraftClient client, int width, int height, int top, int bottom, int itemHeight) {
        super(client, width, height, top, bottom, itemHeight);
        setRenderBackground(false);
        setRenderHeader(false, 0);
        setRenderHorizontalShadows(false);
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
    protected int getScrollbarPositionX() {
        return this.left + this.width - 6;
    }

    /**
     *  Add a journal entry to the list.
     */
    public void addJournalEntry(JournalListEntry entry) {
        this.addEntry(entry);
    }

    /**
     * Calculate the maximum scroll position based on variable entry heights.
     */
    @Override
    protected int getMaxPosition() {
        int total = 0;
        for (int i = 0; i < this.getEntryCount(); i++) {
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
    protected void renderList(DrawContext context, int mouseX, int mouseY, float delta) {

        int rowLeft = this.getRowLeft();
        int rowWidth = this.getRowWidth();
        int currentY = this.top + 4 - (int) this.getScrollAmount();

        for (int i = 0; i < this.getEntryCount(); i++) {

            JournalListEntry entry = this.getEntry(i);
            int entryHeight = entry.getHeight(rowWidth);

            if (currentY + entryHeight >= this.top && currentY <= this.bottom) {
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
    private JournalListEntry getJournalEntryAtPosition(double x, double y) {

        int currentY = this.top + 4 - (int) this.getScrollAmount();

        for (int i = 0; i < this.getEntryCount(); i++) {
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
    public void appendNarrations(NarrationMessageBuilder builder) {

        JournalListEntry selected = this.getSelectedOrNull();

        if (selected != null)
            builder.put(NarrationPart.TITLE, selected.getNarration());
        else if (this.getEntryCount() > 0)
            builder.put(NarrationPart.TITLE, Text.translatable("narration.selection.usage"));

        builder.put(NarrationPart.USAGE, Text.translatable("narration.component_list.usage"));
    }
}