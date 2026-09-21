package space.ajcool.ardapaths.screens.widgets;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.screens.GuiTextures;
import space.ajcool.ardapaths.screens.GuiTextures.PanelState;
import space.ajcool.ardapaths.screens.GuiTextures.SliceCap;

import java.util.ArrayList;
import java.util.List;

/**
 * Single-line text input that offers selectable suggestions above or below the field.
 */
@Environment(EnvType.CLIENT)
public class SuggestionInputWidget extends InputBoxWidget implements OverlayRenderer {

    /**
     * Maximum number of suggestion rows visible before scrolling.
     */
    private final int maxVisibleOptions;

    /**
     * Whether the suggestion list opens above the field instead of below it.
     */
    private final boolean expandUpwards;

    /**
     * Full suggestion candidates available to the input.
     */
    private List<String> suggestions;

    /**
     * Suggestions matching the current input text.
     */
    private List<String> filteredSuggestions;

    /**
     * First visible suggestion index.
     */
    private int scrollOffset;

    /**
     * Selected row index inside the filtered suggestion list.
     */
    private int selectedIndex;

    /**
     * Creates a suggestion-backed input widget for resource identifiers.
     *
     * @param x                 widget x coordinate
     * @param y                 widget y coordinate
     * @param width             widget width
     * @param height            widget height
     * @param placeholder       placeholder text when the input is empty
     * @param suggestions       initial suggestion candidates
     * @param maxVisibleOptions maximum suggestion rows to show
     * @param expandUpwards     true to open the suggestion list above the field
     */
    public SuggestionInputWidget(int x, int y, int width, int height, Component placeholder, List<String> suggestions,
                                 int maxVisibleOptions, boolean expandUpwards) {
        super(x, y, width, height, Component.empty(), placeholder, SuggestionInputWidget::validateResourceLocation, true);
        this.maxVisibleOptions = maxVisibleOptions;
        this.expandUpwards = expandUpwards;
        this.suggestions = new ArrayList<>(suggestions);
        this.filteredSuggestions = List.of();
        this.scrollOffset = 0;
        this.selectedIndex = 0;
        this.setValueListener(_ -> refreshSuggestions());
        refreshSuggestions();
    }

    /**
     * Replaces the available suggestion candidates.
     *
     * @param suggestions new suggestion candidates
     */
    public void setSuggestions(List<String> suggestions) {
        this.suggestions = new ArrayList<>(suggestions);
        refreshSuggestions();
    }

    /**
     * Accepts keyboard navigation and completion for the suggestion popup.
     *
     * @param event keyboard event
     * @return true when the key was handled
     */
    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (hasVisibleSuggestions()) {
            if (event.key() == GLFW.GLFW_KEY_DOWN) {
                moveSelection(1);
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_UP) {
                moveSelection(-1);
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_TAB || event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
                acceptSelectedSuggestion();
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                setFocused(false);
                return true;
            }
        }
        return super.keyPressed(event);
    }

    /**
     * Closes suggestions when the input loses focus.
     *
     * @param focused true when the widget is focused
     */
    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            filteredSuggestions = List.of();
            scrollOffset = 0;
            selectedIndex = 0;
        } else {
            refreshSuggestions();
        }
    }

    /**
     * Handles clicks on visible suggestion rows before falling back to text input behavior.
     *
     * @param event   mouse button event
     * @param doubled whether the click is a double click
     * @return true when the click was handled
     */
    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubled) {
        int clickedIndex = suggestionIndexAt(event.x(), event.y());
        if (clickedIndex >= 0) {
            selectedIndex = clickedIndex;
            acceptSelectedSuggestion();
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    /**
     * Scrolls the suggestion popup when it has more rows than can be displayed.
     *
     * @param mouseX           mouse x coordinate
     * @param mouseY           mouse y coordinate
     * @param horizontalAmount horizontal scroll wheel amount
     * @param verticalAmount   vertical scroll wheel amount
     * @return true when the scroll changed the popup
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (hasVisibleSuggestions() && filteredSuggestions.size() > maxVisibleOptions) {
            scrollOffset -= (int) verticalAmount;
            scrollOffset = Math.max(0, Math.min(scrollOffset, filteredSuggestions.size() - maxVisibleOptions));
            selectedIndex = Math.max(scrollOffset, Math.min(selectedIndex, scrollOffset + maxVisibleOptions - 1));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    /**
     * Reports whether the suggestion popup needs an overlay pass.
     *
     * @return true when suggestions should be drawn
     */
    @Override
    public boolean hasOverlay() {
        return hasVisibleSuggestions();
    }

    /**
     * Draws visible suggestions above the normal widget stratum.
     *
     * @param context     draw context
     * @param mouseX      current mouse x
     * @param mouseY      current mouse y
     * @param partialTick partial tick delta
     */
    @Override
    public void extractOverlay(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick) {
        int visibleCount = visibleSuggestionCount();
        int lastVisibleIndex = visibleCount - 1;
        for (int i = 0; i < visibleCount; i++) {
            int suggestionIndex = scrollOffset + i;
            String suggestion = filteredSuggestions.get(suggestionIndex);
            int rowY = rowY(i);
            boolean hovered = mouseX >= getX() && mouseX <= getX() + width && mouseY >= rowY && mouseY <= rowY + height;
            boolean selected = suggestionIndex == selectedIndex;
            renderSuggestion(context, suggestion, rowY, hovered, selected, capFor(i, lastVisibleIndex));
        }
    }

    /**
     * Resets text and suggestion state.
     */
    @Override
    public void reset() {
        super.reset();
        refreshSuggestions();
    }

    /**
     * Validates dimension identifiers entered into the field.
     *
     * @param text current input text
     * @throws TextValidationError when the text is not blank and not a resource location
     */
    private static void validateResourceLocation(String text) throws TextValidationError {
        if (text == null || text.isBlank()) {
            return;
        }
        if (Identifier.tryParse(text.trim()) == null) {
            throw new TextValidationError(Component.translatable(
                    "ardapaths.client.chapter.configuration.screens.dimension_invalid",
                    "namespace:path"
            ).getString());
        }
    }

    /**
     * Recomputes the visible suggestions for the current input text.
     */
    private void refreshSuggestions() {
        if (!isFocused()) {
            return;
        }
        String value = getValue();
        if (SuggestionTextMatcher.containsExactIgnoreCase(suggestions, value)) {
            filteredSuggestions = List.of();
        } else {
            filteredSuggestions = SuggestionTextMatcher.filterContains(suggestions, value);
        }
        selectedIndex = filteredSuggestions.isEmpty() ? 0 : Math.min(selectedIndex, filteredSuggestions.size() - 1);
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, filteredSuggestions.size() - maxVisibleOptions)));
        updateUnknownDimensionTooltip();
    }

    /**
     * Updates the tooltip for parseable dimensions absent from the suggestion list.
     */
    private void updateUnknownDimensionTooltip() {
        String value = getValue();
        if (value.isBlank() || Identifier.tryParse(value.trim()) == null || SuggestionTextMatcher.containsExactIgnoreCase(suggestions, value)) {
            setTooltip(null);
            return;
        }
        setTooltip(Tooltip.create(Component.translatable("ardapaths.client.chapter.configuration.screens.dimension_not_loaded")));
    }

    /**
     * Checks whether the popup currently has rows to render.
     *
     * @return true when suggestions should be shown
     */
    private boolean hasVisibleSuggestions() {
        return isFocused() && !filteredSuggestions.isEmpty();
    }

    /**
     * Counts the visible rows in the popup.
     *
     * @return visible suggestion count
     */
    private int visibleSuggestionCount() {
        return Math.min(filteredSuggestions.size() - scrollOffset, maxVisibleOptions);
    }

    /**
     * Moves the selected suggestion row.
     *
     * @param delta signed row delta
     */
    private void moveSelection(int delta) {
        selectedIndex = Math.max(0, Math.min(selectedIndex + delta, filteredSuggestions.size() - 1));
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + maxVisibleOptions) {
            scrollOffset = selectedIndex - maxVisibleOptions + 1;
        }
    }

    /**
     * Accepts the selected suggestion as the input value.
     */
    private void acceptSelectedSuggestion() {
        if (filteredSuggestions.isEmpty()) {
            return;
        }
        setValue(filteredSuggestions.get(selectedIndex));
        setFocused(false);
        validateText();
    }

    /**
     * Finds the suggestion row under a mouse position.
     *
     * @param mouseX mouse x coordinate
     * @param mouseY mouse y coordinate
     * @return filtered suggestion index, or -1 when no row is under the mouse
     */
    private int suggestionIndexAt(double mouseX, double mouseY) {
        if (!hasVisibleSuggestions()) {
            return -1;
        }
        int count = visibleSuggestionCount();
        int listTop = expandUpwards ? getY() - count * height : getY() + height;
        int listBottom = listTop + count * height;
        if (mouseX < getX() || mouseX > getX() + width || mouseY < listTop || mouseY >= listBottom) {
            return -1;
        }
        int visibleIndex = expandUpwards
                ? (int) ((getY() - 1 - mouseY) / height)
                : (int) ((mouseY - listTop) / height);
        int index = scrollOffset + visibleIndex;
        return index < filteredSuggestions.size() ? index : -1;
    }

    /**
     * Computes the y coordinate of a visible suggestion row.
     *
     * @param visibleIndex row index inside the visible window
     * @return row y coordinate; upward lists place row 0 directly above the field
     */
    private int rowY(int visibleIndex) {
        return expandUpwards ? getY() - (visibleIndex + 1) * height : getY() + height + visibleIndex * height;
    }

    /**
     * Renders one suggestion row.
     *
     * @param context    drawing context
     * @param suggestion suggestion text
     * @param y          row y coordinate
     * @param hovered    true when the row is hovered
     * @param selected   true when the row is selected by keyboard
     * @param cap        panel cap for the row position
     */
    @SuppressWarnings("resource")
    private void renderSuggestion(GuiGraphicsExtractor context, String suggestion, int y, boolean hovered, boolean selected, SliceCap cap) {
        Font font = Client.mc().font;
        PanelState state = PanelState.IDLE;
        if (hovered) {
            state = PanelState.HOVERED;
        } else if (selected) {
            state = PanelState.SELECTED;
        }
        GuiTextures.drawPanelSegment(context, getX(), y, width, height, state, cap);
        context.text(font, font.plainSubstrByWidth(suggestion, width - 8), getX() + 4, y + (height - font.lineHeight) / 2, 0xFFFFFFFF);
    }

    /**
     * Determines the panel cap for a visible row.
     *
     * @param visibleIndex     row index inside the visible window
     * @param lastVisibleIndex final row index inside the visible window
     * @return cap selection for the row
     */
    private SliceCap capFor(int visibleIndex, int lastVisibleIndex) {
        if (lastVisibleIndex <= 0) {
            return SliceCap.FULL;
        }
        if (visibleIndex == 0) {
            return expandUpwards ? SliceCap.BOTTOM : SliceCap.TOP;
        }
        if (visibleIndex == lastVisibleIndex) {
            return expandUpwards ? SliceCap.TOP : SliceCap.BOTTOM;
        }
        return SliceCap.MIDDLE;
    }
}
