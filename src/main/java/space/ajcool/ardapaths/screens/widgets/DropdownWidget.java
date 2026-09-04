package space.ajcool.ardapaths.screens.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.screens.GuiTextures;
import space.ajcool.ardapaths.screens.GuiTextures.PanelState;
import space.ajcool.ardapaths.screens.GuiTextures.SliceCap;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A generic dropdown menu widget for selecting from a list of options.
 * Supports scrolling, custom display functions, and nullable selections.
 *
 * @param <T> the type of options in this dropdown
 */
@SuppressWarnings({"resource", "unused"})
public class DropdownWidget<T> extends AbstractWidget {

    /**
     * The original width when not expanded.
     */
    private final int originalWidth;

    /**
     * The original height when not expanded.
     */
    private final int originalHeight;

    /**
     * Maximum number of options visible at once (for scrolling).
     */
    private final int maxVisibleOptions;

    /**
     * The list of available options to select from.
     */
    @Getter
    @Setter
    private List<T> options;

    /**
     * Function to display options as text.
     */
    @Setter
    private Function<T, Component> optionDisplay;

    /**
     * The currently selected option, or null if none selected.
     */
    @Getter
    @Setter(AccessLevel.NONE)
    @Nullable
    private T selected;

    /**
     * Callback invoked when an option is selected.
     */
    @Setter
    private Consumer<T> onSelect;

    /**
     * Whether null is a valid selection.
     */
    @Setter
    private boolean allowNull;

    /**
     * Whether the dropdown list is currently expanded.
     */
    @Getter
    @Setter
    private boolean expanded;

    /**
     * Current scroll offset in the options list.
     */
    private int scrollOffset = 0;

    /**
     * Constructs a DropdownWidget with the given parameters.
     *
     * @param x                 the x coordinate
     * @param y                 the y coordinate
     * @param width             the width of the dropdown
     * @param height            the height of the dropdown
     * @param title             the label text
     * @param options           the list of available options
     * @param optionDisplay     function to display options as text
     * @param selected          the initially selected option
     * @param onSelect          callback when an option is selected
     * @param allowNull         whether null is a valid selection
     * @param expanded          whether the dropdown starts expanded
     * @param maxVisibleOptions maximum options to show before scrolling
     */
    @Builder(builderClassName = "DropdownBuilder", builderMethodName = "create", setterPrefix = "set")
    public DropdownWidget(
            int x,
            int y,
            int width,
            int height,
            Component title,
            List<T> options,
            Function<T, Component> optionDisplay,
            @Nullable T selected,
            Consumer<T> onSelect,
            boolean allowNull,
            boolean expanded,
            int maxVisibleOptions
    ) {
        super(x, y, width, height, title);
        this.originalWidth = width;
        this.originalHeight = height;
        this.options = options;
        this.optionDisplay = optionDisplay;
        this.selected = selected;
        this.onSelect = onSelect;
        this.allowNull = allowNull;
        this.expanded = expanded;
        this.maxVisibleOptions = maxVisibleOptions;
    }

    @Override
    protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        Component title = this.getMessage();
        Font textRenderer = Client.mc().font;
        int titleY = getY() - (textRenderer.lineHeight / 2) - 8;
        PanelState state = (mouseX >= x && mouseX <= x + originalWidth
                && mouseY >= y && mouseY <= y + originalHeight) ? PanelState.HOVERED : PanelState.SELECTED;
        renderBox(context, x, y, selected, textRenderer, originalWidth, originalHeight, state,
                expanded ? SliceCap.TOP : SliceCap.FULL);

        String arrow = expanded ? "▲" : "▼";
        int arrowX = x + originalWidth - textRenderer.width(arrow) - 4;
        int arrowY = y + (originalHeight - textRenderer.lineHeight) / 2;
        context.drawString(textRenderer, Component.literal(arrow), arrowX, arrowY, 0xFFFFFF);
        context.drawString(textRenderer, title, getX(), titleY, 0xFFFFFF);

        // If expanded, draw the dropdown list.
        if (expanded) {
            PoseStack matrices = context.pose();
            matrices.pushPose();
            matrices.translate(0, 0, 100); // Raise the dropdown above other elements.

            int baseY = getY() + originalHeight;

            // Build a combined list of items. When allowNull is true we add a null entry
            // (which we later display as "None").
            List<T> allItems = new ArrayList<>();
            if (allowNull) {
                allItems.add(null);
            }
            allItems.addAll(options);

            int totalItems = allItems.size();
            int visibleCount = Math.min(totalItems, maxVisibleOptions);
            int lastVisibleIndex = Math.min(visibleCount, totalItems - scrollOffset) - 1;
            this.height = originalHeight + visibleCount * originalHeight;

            // Render each visible option, starting from scrollOffset.
            for (int i = 0; i < visibleCount; i++) {
                int actualIndex = scrollOffset + i;
                if (actualIndex >= totalItems) break;
                T item = allItems.get(actualIndex);
                int itemY = baseY + i * originalHeight;
                boolean hovered = mouseX >= x && mouseX <= x + originalWidth &&
                        mouseY >= itemY && mouseY <= itemY + originalHeight;
                boolean isSelected = (item == null && selected == null)
                        || (item != null && item.equals(selected));
                renderItem(context, x, itemY, item, isSelected, hovered, capFor(i, lastVisibleIndex));
            }
            matrices.popPose();
        }
    }

    /**
     * Renders a box with text. If item is null, "None" is displayed.
     *
     * @param context      the drawing context
     * @param x            the x coordinate
     * @param y            the y coordinate
     * @param item         the item to render
     * @param textRenderer the text renderer for drawing text
     * @param width        the width of the box
     * @param height       the height of the box
     * @param state        visual panel state
     * @param cap          caps to draw for this box
     */
    private void renderBox(GuiGraphics context, int x, int y, T item, Font textRenderer,
                           int width, int height, PanelState state, SliceCap cap) {
        GuiTextures.drawPanelSegment(context, x, y, width, height, state, cap);
        Component display = (item == null) ? Component.literal("None") : optionDisplay.apply(item);
        int textX = x + 4;
        int textY = y + (height - textRenderer.lineHeight) / 2;
        context.drawString(textRenderer, display, textX, textY, 0xFFFFFF);
    }

    /**
     * Renders an individual option in the dropdown.
     *
     * @param context  the drawing context
     * @param x        the x coordinate
     * @param y        the y coordinate
     * @param item     the item to render
     * @param selected whether the item is selected
     * @param hovered  whether the item is hovered
     * @param cap      caps to draw for this row's list position
     */
    private void renderItem(GuiGraphics context, int x, int y, T item, boolean selected, boolean hovered,
                            SliceCap cap) {
        Font textRenderer = Client.mc().font;
        int width = getWidth();
        PanelState state = PanelState.IDLE;
        if (hovered) {
            state = PanelState.HOVERED;
        } else if (selected) {
            state = PanelState.SELECTED;
        }
        renderBox(context, x, y, item, textRenderer, width, originalHeight, state, cap);
    }

    /**
     * Determines which caps a row should draw inside the visible list window.
     *
     * @param visibleIndex     row index inside the currently visible window
     * @param lastVisibleIndex last row index inside the currently visible window
     * @return cap selection for the row
     */
    private SliceCap capFor(int visibleIndex, int lastVisibleIndex) {
        if (lastVisibleIndex <= 0) {
            return SliceCap.FULL;
        }
        if (visibleIndex == 0) {
            return SliceCap.TOP;
        }
        if (visibleIndex == lastVisibleIndex) {
            return SliceCap.BOTTOM;
        }
        return SliceCap.MIDDLE;
    }

    /**
     * Toggles expansion. When expanded, clicking inside the list selects an item;
     * clicking outside collapses the list.
     */
    @Override
    public void onClick(double mouseX, double mouseY) {
        if (!expanded) {
            expanded = true;
            scrollOffset = 0;
            List<T> allItems = new ArrayList<>();
            if (allowNull) {
                allItems.add(null);
            }
            allItems.addAll(options);
            int totalItems = allItems.size();
            int visibleCount = Math.min(totalItems, maxVisibleOptions);
            this.height = originalHeight + visibleCount * originalHeight;
        } else {
            int dropdownTop = getY() + originalHeight;
            List<T> allItems = new ArrayList<>();
            if (allowNull) {
                allItems.add(null);
            }
            allItems.addAll(options);
            int totalItems = allItems.size();
            int visibleCount = Math.min(totalItems, maxVisibleOptions);
            int dropdownBottom = dropdownTop + visibleCount * originalHeight;

            // If the click is outside the dropdown list, simply collapse
            if (mouseY < dropdownTop || mouseY > dropdownBottom) {
                expanded = false;
                this.height = originalHeight;
                return;
            }

            // Determine which visible item was clicked
            int clickedIndex = (int) ((mouseY - dropdownTop) / originalHeight);
            int actualIndex = scrollOffset + clickedIndex;
            if (actualIndex < totalItems) {
                T item = allItems.get(actualIndex);
                selected = item;
                if (onSelect != null) {
                    onSelect.accept(item);
                }
            }
            expanded = false;
            this.height = originalHeight;
        }
        super.onClick(mouseX, mouseY);
    }

    /**
     * When the dropdown is expanded and there are more items than can be shown,
     * the user can scroll using the mouse wheel.
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (expanded) {
            List<T> allItems = new ArrayList<>();
            if (allowNull) {
                allItems.add(null);
            }
            allItems.addAll(options);
            int totalItems = allItems.size();
            if (totalItems > maxVisibleOptions) {
                // Adjust scrollOffset
                scrollOffset -= (int) verticalAmount;
                scrollOffset = Math.max(0, scrollOffset);
                scrollOffset = Math.min(scrollOffset, totalItems - maxVisibleOptions);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        defaultButtonNarrationText(builder);
    }

    /**
     * Sets the selected option after validating that it is available.
     * This stays hand-written because invalid values must clear the selection.
     *
     * @param selected the selected option, or null when no option is selected
     */
    public void setSelected(@Nullable T selected) {
        if (!options.contains(selected)) {
            this.selected = null;
        } else {
            this.selected = selected;
        }
    }

    /**
     * Fluent builder for {@link DropdownWidget}; see {@code create()}.
     *
     * @param <T> the type of options in this dropdown
     */
    public static class DropdownBuilder<T> {

        /**
         * Default options used when no option list is configured.
         */
        @SuppressWarnings("FieldMayBeFinal")
        private List<T> options = List.of();

        /**
         * Default option text renderer used when no display function is configured.
         */
        @SuppressWarnings("FieldMayBeFinal")
        private Function<T, Component> optionDisplay = item -> Component.empty();

        /**
         * Default maximum visible option count before scrolling is required.
         */
        @SuppressWarnings("FieldMayBeFinal")
        private int maxVisibleOptions = 8;

        /**
         * Sets both widget coordinates at once.
         *
         * @param x the x coordinate
         * @param y the y coordinate
         * @return this builder
         */
        public DropdownBuilder<T> setPosition(int x, int y) {
            return this.setX(x).setY(y);
        }

        /**
         * Sets both widget dimensions at once.
         *
         * @param width  the widget width
         * @param height the widget height
         * @return this builder
         */
        public DropdownBuilder<T> setSize(int width, int height) {
            return this.setWidth(width).setHeight(height);
        }
    }
}
