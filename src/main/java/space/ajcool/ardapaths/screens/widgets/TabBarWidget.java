package space.ajcool.ardapaths.screens.widgets;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.screens.GuiTextures;

import java.util.List;
import java.util.function.IntConsumer;

/**
 * A compact tab selector for switching between fixed content panels in configuration screens.
 */
public class TabBarWidget extends AbstractWidget {

    /**
     * Inner padding applied between the tab content panel edges and its content.
     */
    public static final int CONTENT_PADDING = 5;

    /**
     * Labels displayed for each selectable tab.
     */
    private final List<Component> tabs;

    /**
     * Callback invoked when the selected tab changes.
     */
    private final IntConsumer onSelect;

    /**
     * Height of the content panel drawn under the tab body.
     */
    private final int contentHeight;

    /**
     * ARGB color used for the tab content background panel.
     */
    private final int contentBackgroundColor;

    /**
     * Index of the currently selected tab.
     */
    @Getter
    @Setter
    private int selectedIndex;

    /**
     * Constructs a tab bar with the configured tabs and selection callback.
     *
     * @param x                      the x coordinate
     * @param y                      the y coordinate
     * @param width                  the total tab bar width
     * @param height                 the tab bar height
     * @param tabs                   labels for the available tabs
     * @param selectedIndex          the initially selected tab index
     * @param onSelect               callback invoked when a different tab is selected
     * @param contentHeight          height of the tab content background, or zero to draw none
     * @param contentBackgroundColor ARGB color for the tab content background, or zero for the default
     */
    @Builder(builderClassName = "TabBarBuilder", builderMethodName = "create", setterPrefix = "set")
    public TabBarWidget(int x, int y, int width, int height, List<Component> tabs, int selectedIndex, IntConsumer onSelect,
                        int contentHeight, int contentBackgroundColor) {
        super(x, y, width, height, Component.empty());
        this.tabs = tabs;
        this.selectedIndex = selectedIndex;
        this.onSelect = onSelect;
        this.contentHeight = contentHeight;
        this.contentBackgroundColor = contentBackgroundColor == 0 ? 0x80000000 : contentBackgroundColor;
    }

    /**
     * Renders the tab cells, labels, hover state, and selected-tab accent.
     *
     * @param context the draw context for rendering
     * @param mouseX  the current mouse x coordinate
     * @param mouseY  the current mouse y coordinate
     * @param delta   the partial tick delta
     */
    @SuppressWarnings("resource")
    @Override
    protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (tabs.isEmpty()) return;

        Font textRenderer = Client.mc().font;
        int tabWidth = getWidth() / tabs.size();
        int x = getX();

        if (contentHeight > 0) {
            context.fill(getX(), getY() + getHeight(), getX() + getWidth(), getY() + getHeight() + contentHeight,
                    contentBackgroundColor);
        }

        for (int index = 0; index < tabs.size(); index++) {
            int cellWidth = index == tabs.size() - 1 ? getWidth() - tabWidth * index : tabWidth;
            int cellX = x + tabWidth * index;
            boolean hovered = mouseX >= cellX && mouseX <= cellX + cellWidth &&
                    mouseY >= getY() && mouseY <= getY() + getHeight();
            boolean selected = index == selectedIndex;
            Component tab = tabs.get(index);
            int textX = cellX + (cellWidth - textRenderer.width(tab)) / 2;
            int textY = getY() + (getHeight() - textRenderer.lineHeight) / 2;
            int color = selected ? 0xFFFFFFFF : 0xFFA0A0A0;

            GuiTextures.PanelState state = hovered || selected ? GuiTextures.PanelState.HOVERED : GuiTextures.PanelState.IDLE;
            GuiTextures.drawPanelSegment(context, cellX, getY(), cellWidth, getHeight(), state, GuiTextures.SliceCap.FULL);
            context.drawString(textRenderer, tab, textX, textY, color);
        }
    }

    /**
     * Selects the tab under the cursor and notifies listeners when the selected index changes.
     *
     * @param mouseX the mouse x coordinate
     * @param mouseY the mouse y coordinate
     */
    @Override
    public void onClick(double mouseX, double mouseY) {
        if (tabs.isEmpty()) return;

        int index = (int) ((mouseX - getX()) / (getWidth() / (double) tabs.size()));
        index = Math.max(0, Math.min(tabs.size() - 1, index));

        if (index != selectedIndex) {
            selectedIndex = index;
            onSelect.accept(index);
        }

        super.onClick(mouseX, mouseY);
    }

    /**
     * Appends the default narration metadata for this clickable tab bar.
     *
     * @param builder the narration builder receiving widget narration
     */
    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        defaultButtonNarrationText(builder);
    }
}
