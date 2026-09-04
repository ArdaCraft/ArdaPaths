package space.ajcool.ardapaths.screens.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Builder;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.screens.GuiTextures;
import space.ajcool.ardapaths.screens.GuiTextures.PanelState;
import space.ajcool.ardapaths.screens.GuiTextures.SliceCap;

import java.util.List;

/**
 * Floating menu used by screens to offer row-scoped contextual actions.
 */
public class ContextMenuWidget extends AbstractWidget {

    /**
     * Height in pixels for each context menu item.
     */
    private static final int ITEM_HEIGHT = 20;

    /**
     * Minimum menu width in pixels.
     */
    private static final int MIN_WIDTH = 96;

    /**
     * Maximum menu width in pixels.
     */
    private static final int MAX_WIDTH = 180;

    /**
     * Menu items displayed from top to bottom.
     */
    private final List<Item> items;

    /**
     * Creates a floating context menu.
     *
     * @param x     the menu x coordinate
     * @param y     the menu y coordinate
     * @param items rows displayed in the menu
     */
    @Builder(builderClassName = "ContextMenuBuilder", builderMethodName = "create", setterPrefix = "set")
    public ContextMenuWidget(int x, int y, List<Item> items) {
        super(x, y, widthFor(items), items.size() * ITEM_HEIGHT, Component.empty());
        this.items = List.copyOf(items);
    }

    /**
     * Computes menu width from the widest item label.
     *
     * @param items menu items to measure
     * @return clamped menu width
     */
    @SuppressWarnings("resource")
    private static int widthFor(List<Item> items) {
        Font textRenderer = Client.mc().font;
        int width = MIN_WIDTH;
        for (Item item : items) {
            width = Math.max(width, textRenderer.width(item.label()) + 16);
        }
        return Math.min(MAX_WIDTH, width);
    }

    /**
     * Renders all menu rows.
     *
     * @param context draw context
     * @param mouseX  current mouse x coordinate
     * @param mouseY  current mouse y coordinate
     * @param delta   partial tick delta
     */
    @SuppressWarnings("resource")
    @Override
    protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        PoseStack matrices = context.pose();
        matrices.pushPose();
        matrices.translate(0, 0, 200);
        for (int index = 0; index < items.size(); index++) {
            Item item = items.get(index);
            int itemY = getY() + index * ITEM_HEIGHT;
            boolean hovered = mouseX >= getX() && mouseX <= getX() + getWidth() && mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT;
            PanelState state = hovered ? PanelState.HOVERED : PanelState.IDLE;
            SliceCap cap = capFor(index, items.size() - 1);
            int color = item.enabled() ? 0xFFFFFF : 0x777777;
            GuiTextures.drawPanelSegment(context, getX(), itemY, getWidth(), ITEM_HEIGHT, state, cap);
            context.drawString(Client.mc().font, item.label(), getX() + 4, itemY + 6, color);
            if (hovered && item.tooltip() != null) {
                setTooltip(Tooltip.create(item.tooltip()));
            }
        }
        matrices.popPose();
    }

    /**
     * Determines which caps a row should draw inside the menu.
     *
     * @param index     row index
     * @param lastIndex final row index
     * @return cap selection for the row
     */
    private SliceCap capFor(int index, int lastIndex) {
        if (lastIndex <= 0) {
            return SliceCap.FULL;
        }
        if (index == 0) {
            return SliceCap.TOP;
        }
        if (index == lastIndex) {
            return SliceCap.BOTTOM;
        }
        return SliceCap.MIDDLE;
    }

    /**
     * Runs the clicked enabled item action.
     *
     * @param mouseX mouse x coordinate
     * @param mouseY mouse y coordinate
     * @param button clicked mouse button
     * @return true when the menu consumed the click
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !clicked(mouseX, mouseY)) return false;
        int index = (int) ((mouseY - getY()) / ITEM_HEIGHT);
        if (index < 0 || index >= items.size()) return true;

        Item item = items.get(index);
        if (item.enabled() && item.action() != null) {
            item.action().run();
        }
        return true;
    }

    /**
     * Supplies no extra narration beyond the row labels.
     *
     * @param builder narration builder
     */
    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
    }

    /**
     * One actionable row in a context menu.
     *
     * @param label   text displayed in the row
     * @param tooltip hover help for the row
     * @param enabled whether the row can run its action
     * @param action  work to run when the row is clicked
     */
    public record Item(Component label, Component tooltip, boolean enabled, Runnable action) {

    }
}
