package space.ajcool.ardapaths.screens.widgets;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.core.Client;

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
public class DropdownWidget<T> extends ClickableWidget {
    /**
     * The texture resource for Minecraft UI widgets.
     */
    private static final Identifier WIDGETS_TEXTURE = new Identifier("textures/gui/widgets.png");

    /**
     * The original width when not expanded.
     */
    @SuppressWarnings("FieldMayBeFinal")
    private int originalWidth;

    /**
     * The original height when not expanded.
     */
    @SuppressWarnings("FieldMayBeFinal")
    private int originalHeight;

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
    private Function<T, Text> optionDisplay;

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
     * Maximum number of options visible at once (for scrolling).
     */
    @SuppressWarnings("FieldMayBeFinal")
    private int maxVisibleOptions;

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
            Text title,
            List<T> options,
            Function<T, Text> optionDisplay,
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // First, draw the button itself.
        super.render(context, mouseX, mouseY, delta);
        Text title = this.getMessage();
        if (title != null) {
            TextRenderer textRenderer = Client.mc().textRenderer;
            int titleY = getY() - (textRenderer.fontHeight / 2) - 8;
            context.drawTextWithShadow(textRenderer, title, getX(), titleY, 0xFFFFFF);
        }

        // If expanded, draw the dropdown list.
        if (expanded) {
            MatrixStack matrices = context.getMatrices();
            matrices.push();
            matrices.translate(0, 0, 100); // Raise the dropdown above other elements.

            int x = getX();
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
            this.height = originalHeight + visibleCount * originalHeight;

            // Render each visible option, starting from scrollOffset.
            for (int i = 0; i < visibleCount; i++) {
                int actualIndex = scrollOffset + i;
                if (actualIndex >= totalItems) break;
                T item = allItems.get(actualIndex);
                int y = baseY + i * originalHeight;
                boolean hovered = mouseX >= x && mouseX <= x + originalWidth &&
                        mouseY >= y && mouseY <= y + originalHeight;
                boolean isSelected = (item == null && selected == null)
                        || (item != null && item.equals(selected));
                renderItem(context, x, y, item, isSelected, hovered);
            }
            matrices.pop();
        }
    }

    /**
     * Renders an individual option in the dropdown.
     *
     * @param context the drawing context
     * @param x the x coordinate
     * @param y the y coordinate
     * @param item the item to render
     * @param selected whether the item is selected
     * @param hovered whether the item is hovered
     */
    private void renderItem(DrawContext context, int x, int y, T item, boolean selected, boolean hovered) {
        TextRenderer textRenderer = Client.mc().textRenderer;
        int width = getWidth();

        int v = 46;
        if (hovered) {
            v += 40;
        } else if (selected) {
            v += 20;
        }
        renderBox(context, x, y, item, textRenderer, width, originalHeight, v);
    }

    /**
     * Renders a box with text. If item is null, "None" is displayed.
     *
     * @param context the drawing context
     * @param x the x coordinate
     * @param y the y coordinate
     * @param item the item to render
     * @param textRenderer the text renderer for drawing text
     * @param width the width of the box
     * @param height the height of the box
     * @param v the v-coordinate for texture mapping
     */
    private void renderBox(DrawContext context, int x, int y, T item, TextRenderer textRenderer,
                           int width, int height, int v) {
        context.drawNineSlicedTexture(WIDGETS_TEXTURE, x, y, width, height, 20, 4, 200, 20, 0, v);
        Text display = (item == null) ? Text.literal("None") : optionDisplay.apply(item);
        int textX = x + 4;
        int textY = y + (height - textRenderer.fontHeight) / 2;
        context.drawTextWithShadow(textRenderer, display, textX, textY, 0xFFFFFF);
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        TextRenderer textRenderer = Client.mc().textRenderer;
        int x = getX();
        int y = getY();

        int vScale = (mouseX >= x && mouseX <= x + originalWidth &&
                mouseY >= y && mouseY <= y + originalHeight) ? 2 : 1;
        int v = 46 + (vScale * 20);
        renderBox(context, x, y, selected, textRenderer, originalWidth, originalHeight, v);

        String arrow = expanded ? "▲" : "▼";
        int arrowX = x + originalWidth - textRenderer.getWidth(arrow) - 4;
        int arrowY = y + (originalHeight - textRenderer.fontHeight) / 2;
        context.drawTextWithShadow(textRenderer, Text.literal(arrow), arrowX, arrowY, 0xFFFFFF);
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
                    System.out.println("Accepting");
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
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (expanded) {
            List<T> allItems = new ArrayList<>();
            if (allowNull) {
                allItems.add(null);
            }
            allItems.addAll(options);
            int totalItems = allItems.size();
            if (totalItems > maxVisibleOptions) {
                // Adjust scrollOffset
                scrollOffset -= (int) amount;
                scrollOffset = Math.max(0, scrollOffset);
                scrollOffset = Math.min(scrollOffset, totalItems - maxVisibleOptions);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
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
        private Function<T, Text> optionDisplay = item -> Text.empty();

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
