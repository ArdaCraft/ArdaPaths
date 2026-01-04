package space.ajcool.ardapaths.screens.widgets;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.Collections;
import java.util.List;

/**
 * A custom list entry for displaying journal entries with variable heights.
 */
public class JournalListEntry extends AlwaysSelectedEntryListWidget.Entry<JournalListEntry> {

    private final Text heading;
    private final Text description;
    private final ButtonWidget teleportButton;
    private final MinecraftClient client;
    private List<OrderedText> wrappedDescription;
    private int cachedWidth = -1;
    private int calculatedHeight;

    /**
     * Create a new JournalListEntry.
     *
     * @param heading        The heading of the journal entry
     * @param description The description text of the journal entry
     * @param buttonText  The text for the teleport button (empty for no button)
     * @param onPress     The action to perform when the button is pressed
     */
    public JournalListEntry(Text heading, Text description, Text buttonText, ButtonWidget.PressAction onPress) {

        this.client = MinecraftClient.getInstance();
        this.heading = heading;
        this.description = description;

        if (!buttonText.equals(Text.empty())) {

            this.teleportButton = ButtonWidget.builder(buttonText, onPress)
                    .dimensions(0, 0, 60, 20)
                    .build();
        } else {
            this.teleportButton = null;
        }

        // default minimum
        this.calculatedHeight = 32;
    }

    /**
     * Get the height of this entry based on the given width.
     *
     * @param entryWidth The width available for the entry
     * @return The calculated height of the entry
     */
    public int getHeight(int entryWidth) {

        if (entryWidth != cachedWidth) {
            cachedWidth = entryWidth;
            recalculateHeight(entryWidth);
        }
        return calculatedHeight;
    }

    /**
     * Recalculate the height of the entry based on the given width.
     *
     * @param entryWidth The width available for the entry
     */
    private void recalculateHeight(int entryWidth) {
        TextRenderer textRenderer = client.textRenderer;
        int textWidth = entryWidth - 80; // leave space for button

        if (description != null) {
            wrappedDescription = textRenderer.wrapLines(description, textWidth);
        } else {
            wrappedDescription = Collections.emptyList();
        }

        int lineHeight = textRenderer.fontHeight + 2;
        int typeHeight = 14;
        int descriptionHeight = wrappedDescription.size() * lineHeight;
        int padding = 8;

        calculatedHeight = Math.max(32, typeHeight + descriptionHeight + padding);
    }

    /**
     * Render the journal entry.
     *
     * @param context     The draw context
     * @param index       The index of the entry
     * @param y           The y position to render at
     * @param x           The x position to render at
     * @param entryWidth  The width of the entry
     * @param entryHeight The height of the entry
     * @param mouseX      The x position of the mouse
     * @param mouseY      The y position of the mouse
     * @param hovered     Whether the entry is hovered
     * @param tickDelta   The partial tick
     */
    @Override
    public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                       int mouseX, int mouseY, boolean hovered, float tickDelta) {
        TextRenderer textRenderer = client.textRenderer;

        // Recalculate if width changed
        getHeight(entryWidth);

        // Draw type
        context.drawText(textRenderer, heading, x + 5, y + 2, 0xFFFFFF, false);

        // Draw wrapped description
        if (wrappedDescription != null && !wrappedDescription.isEmpty()) {
            int lineY = y + 14;
            int lineHeight = textRenderer.fontHeight + 2;
            for (OrderedText line : wrappedDescription) {
                context.drawText(textRenderer, line, x + 5, lineY, 0xAAAAAA, false);
                lineY += lineHeight;
            }
        }

        if (teleportButton != null) {

            teleportButton.setX(x + entryWidth - 65);
            teleportButton.setY(y + 2);
            teleportButton.render(context, mouseX, mouseY, tickDelta);
        }
    }

    /**
     * Handle mouse clicks for the entry.
     *
     * @param mouseX The x position of the mouse
     * @param mouseY The y position of the mouse
     * @param button  The mouse button
     * @return true if the click was handled, false otherwise
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if (this.teleportButton.isMouseOver(mouseX, mouseY)) {
            return this.teleportButton.mouseClicked(mouseX, mouseY, button);
        }
        return false;
    }

    /**
     * Get the narration text for the entry.
     *
     * @return The narration text
     */
    @Override
    public Text getNarration() {
        return Text.literal(heading.getString());
    }
}