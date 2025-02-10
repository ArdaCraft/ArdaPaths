package space.ajcool.ardapaths.screens.widgets.dropdowns;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import space.ajcool.ardapaths.utils.Client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public abstract class DropdownWidget<T> extends ClickableWidget {
    private static final Identifier WIDGETS_TEXTURE = new Identifier("textures/gui/widgets.png");

    private final int maxVisibleItems;
    private final List<T> items;
    private T selected;
    private Consumer<T> itemSelectedListener;
    private boolean hasNullItem = false;
    private boolean expanded = false;
    private int scrollOffset = 0;

    public DropdownWidget(int x, int y, int width, int height, int maxVisibleItems) {
        super(x, y, width, height, Text.empty());
        this.maxVisibleItems = maxVisibleItems;
        this.items = new ArrayList<>();
    }

    public DropdownWidget(int x, int y, int width, int height, int maxVisibleItems, Consumer<T> itemSelectedListener) {
        this(x, y, width, height, maxVisibleItems);
        this.itemSelectedListener = itemSelectedListener;
    }

    /**
     * Get the display text for an item.
     */
    public abstract Text getItemDisplay(@Nullable T item);

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int x = this.getX();
        int y = this.getY();

        if (expanded) {
            MatrixStack matrices = context.getMatrices();
            matrices.push();
            matrices.translate(0, 0, 100);

            int dropdownX = x + 2;
            int dropdownWidth = this.width - 4;
            int visibleCount = Math.min(maxVisibleItems, items.size());
            int itemAreaHeight = visibleCount * this.height + (visibleCount - 1);
            int dropdownY = y + this.height;

            // Render outer border
            int borderColor = 0xFF333333;
            context.fill(dropdownX - 1, dropdownY - 1, dropdownX + dropdownWidth + 1, dropdownY, borderColor);           // Top border
            context.fill(dropdownX - 1, dropdownY + itemAreaHeight, dropdownX + dropdownWidth + 1, dropdownY + itemAreaHeight + 1, borderColor); // Bottom border
            context.fill(dropdownX - 1, dropdownY, dropdownX, dropdownY + itemAreaHeight, borderColor);                     // Left border
            context.fill(dropdownX + dropdownWidth, dropdownY, dropdownX + dropdownWidth + 1, dropdownY + itemAreaHeight, borderColor);   // Right border

            // Compute content area
            int contentWidth = dropdownWidth;
            boolean hasScrollbar = items.size() > maxVisibleItems;
            int scrollbarWidth = hasScrollbar ? 4 : 0;
            if (hasScrollbar) {
                contentWidth -= scrollbarWidth;
            }

            // Render items
            for (int i = 0; i < visibleCount; i++) {
                int itemIndex = i + scrollOffset;
                int itemY = dropdownY + i * (this.height + 1);
                boolean itemHovered = mouseX >= dropdownX && mouseX < dropdownX + contentWidth &&
                        mouseY >= itemY && mouseY < itemY + this.height;
                int itemBg = itemHovered ? 0xFF222222 : 0xFF000000;
                context.fill(dropdownX, itemY, dropdownX + contentWidth, itemY + this.height, itemBg);

                // Render separator
                if (i < visibleCount - 1) {
                    context.fill(dropdownX, itemY + this.height, dropdownX + contentWidth, itemY + this.height + 1, borderColor);
                }

                // Render text
                T item = items.get(itemIndex);
                Text itemText;
                if (item == null) {
                    itemText = Text.literal("None");
                } else {
                    itemText = getItemDisplay(item);
                }
                int textColor = 0xFFFFFFFF;
                if (itemText.getStyle().getColor() != null) {
                    textColor = itemText.getStyle().getColor().getRgb();
                }
                context.drawCenteredTextWithShadow(
                        MinecraftClient.getInstance().textRenderer,
                        itemText,
                        dropdownX + contentWidth / 2,
                        itemY + (this.height - MinecraftClient.getInstance().textRenderer.fontHeight) / 2,
                        textColor
                );
            }

            // Render scrollbar
            if (hasScrollbar) {
                int scrollbarX = dropdownX + dropdownWidth - scrollbarWidth;

                // Render scrollbar track
                int trackColor = 0xFF000000;
                context.fill(scrollbarX, dropdownY, scrollbarX + scrollbarWidth, dropdownY + itemAreaHeight, trackColor);

                // Compute thumb dimensions relative to total items
                float thumbRatio = (float) visibleCount / (float) items.size();
                int thumbHeight = Math.max(8, (int) (itemAreaHeight * thumbRatio));
                int maxScroll = items.size() - visibleCount;
                float scrollPercent = maxScroll == 0 ? 0 : (float) scrollOffset / (float) maxScroll;
                int thumbY = dropdownY + (int) ((itemAreaHeight - thumbHeight) * scrollPercent);

                // Render thumb
                int thumbColor = 0xFFAAAAAA;
                context.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, thumbColor);
            }

            matrices.pop();
        }
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        TextRenderer textRenderer = Client.mc().textRenderer;
        int x = this.getX();
        int y = this.getY();

        // Render button
        RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
        int state = getYImage(this.isHovered());
        context.drawTexture(WIDGETS_TEXTURE, x, y, 0, 46 + state * 20, this.width / 2, this.height);
        context.drawTexture(WIDGETS_TEXTURE, x + this.width / 2, y, 200 - this.width / 2, 46 + state * 20, this.width / 2, this.height);

        // Render selected item text
        Text text = getItemDisplay(selected);
        int color = 0xFFFFFFFF;
        if (text.getStyle().getColor() != null) {
            color = text.getStyle().getColor().getRgb();
        }
        int textX = x + 4;
        int textY = y + (this.height - textRenderer.fontHeight) / 2;
        context.drawTextWithShadow(textRenderer, text, textX, textY, color);

        // Render dropdown arrow
        String arrow = expanded ? "▼" : "▲";
        int arrowWidth = Client.mc().textRenderer.getWidth(arrow);
        int arrowX = x + this.width - arrowWidth - 4;
        int arrowY = y + (this.height - textRenderer.fontHeight) / 2 + 1;
        context.drawTextWithShadow(textRenderer, arrow, arrowX, arrowY, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = this.getX();
        int y = this.getY();

        if (this.isMouseOver(mouseX, mouseY)) {
            expanded = !expanded;
            return true;
        }

        if (expanded) {
            int dropdownX = x + 2;
            int dropdownWidth = this.width - 4;
            int visibleCount = Math.min(maxVisibleItems, items.size());
            int dropdownY = y + this.height;
            boolean hasScrollbar = items.size() > maxVisibleItems;
            int contentWidth = dropdownWidth;
            if (hasScrollbar) {
                contentWidth -= 4;
            }

            for (int i = 0; i < visibleCount; i++) {
                int itemIndex = i + scrollOffset;
                int itemY = dropdownY + i * (this.height + 1);
                if (mouseX >= dropdownX && mouseX < dropdownX + contentWidth &&
                        mouseY >= itemY && mouseY < itemY + this.height) {
                    selected = items.get(itemIndex);
                    if (itemSelectedListener != null) {
                        itemSelectedListener.accept(selected);
                    }
                    expanded = false;
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (expanded && items.size() > maxVisibleItems && (this.isMouseOver(mouseX, mouseY))) {
            scrollOffset -= (int) amount;
            if (scrollOffset < 0) {
                scrollOffset = 0;
            }
            int maxScroll = items.size() - maxVisibleItems;
            if (scrollOffset > maxScroll) {
                scrollOffset = maxScroll;
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {}

    /**
     * Mimics Minecraft’s default ButtonWidget to choose which portion of the widget texture to use.
     *
     * @param hovered Whether the widget is hovered.
     * @return 0 if disabled, 2 if hovered, 1 otherwise.
     */
    private int getYImage(boolean hovered) {
        int i = 1;
        if (!this.active) {
            i = 0;
        } else if (hovered) {
            i = 2;
        }
        return i;
    }

    /**
     * @return The listener for when an item is selected
     */
    public Consumer<T> getItemSelectedListener() {
        return itemSelectedListener;
    }

    /**
     * Set the listener for when an item is selected.
     *
     * @param itemSelectedListener The listener
     */
    public void setItemSelectedListener(Consumer<T> itemSelectedListener) {
        this.itemSelectedListener = itemSelectedListener;
    }

    /**
     * Sets whether the dropdown has a null item at the top.
     *
     * @param hasNullItem Whether the dropdown has a null item
     */
    public void setHasNullItem(boolean hasNullItem) {
        this.hasNullItem = hasNullItem;
        if (hasNullItem && !items.contains(null)) {
            items.add(0, null);
        } else if (!hasNullItem && items.get(0) == null) {
            items.remove(0);
        }
    }

    /**
     * @return The currently selected item
     */
    public @Nullable T getSelected() {
        return selected;
    }

    /**
     * Set the currently selected item.
     *
     * @param selected The item to select
     */
    public void setSelected(T selected) {
        this.selected = selected;
    }

    /**
     * @return The list of items in the dropdown
     */
    public List<T> getOptions() {
        return items;
    }

    /**
     * Set the list of items in the dropdown.
     *
     * @param items The list of items
     */
    public void setOptions(List<T> items) {
        this.items.clear();
        if (hasNullItem) {
            this.items.add(null);
        }
        if (items == null) {
            selected = null;
            return;
        }
        this.items.addAll(items);
        if (!items.contains(selected)) {
            selected = null;
        }
    }
}
