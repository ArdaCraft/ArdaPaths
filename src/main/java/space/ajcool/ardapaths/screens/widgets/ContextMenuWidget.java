package space.ajcool.ardapaths.screens.widgets;

import lombok.Builder;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.screens.GuiTextures;
import space.ajcool.ardapaths.screens.GuiTextures.PanelState;
import space.ajcool.ardapaths.screens.GuiTextures.SliceCap;

import java.util.List;

/**
 * Floating menu used by screens to offer row-scoped contextual actions.
 */
// Instantiated via screen/builder factory; IntelliJ entry-point analysis can't follow it.
@SuppressWarnings("unused")
public class ContextMenuWidget extends AbstractWidget implements OverlayRenderer {

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
    // Instantiated via screen/builder factory; IntelliJ entry-point analysis can't follow it.
    @SuppressWarnings("unused")
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
     * Does not draw the menu rows itself — drawing is deferred to {@link #extractOverlay} so the
     * menu always appears above all other screen content.
     *
     * @param context draw context
     * @param mouseX  current mouse x coordinate
     * @param mouseY  current mouse y coordinate
     * @param delta   partial tick delta
     */
    @Override
    protected void extractWidgetRenderState(@NonNull GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Rows drawn in extractOverlay
    }

    @Override
    public boolean hasOverlay() {
        return visible;
    }

    /**
     * Draws all menu rows at the overlay stratum, above all other screen content.
     *
     * @param context     draw context
     * @param mouseX      current mouse x
     * @param mouseY      current mouse y
     * @param partialTick partial tick delta
     */
    @SuppressWarnings("resource")
    @Override
    public void extractOverlay(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick) {
        for (int index = 0; index < items.size(); index++) {
            Item item = items.get(index);
            int itemY = getY() + index * ITEM_HEIGHT;
            boolean hovered = mouseX >= getX() && mouseX <= getX() + getWidth() && mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT;
            PanelState state = hovered ? PanelState.HOVERED : PanelState.IDLE;
            SliceCap cap = capFor(index, items.size() - 1);
            int color = item.enabled() ? 0xFFFFFFFF : 0xFF777777;
            GuiTextures.drawPanelSegment(context, getX(), itemY, getWidth(), ITEM_HEIGHT, state, cap);
            context.text(Client.mc().font, item.label(), getX() + 4, itemY + 6, color);
            if (hovered && item.tooltip() != null) {
                setTooltip(Tooltip.create(item.tooltip()));
            }
        }
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
     * @param event   clicked mouse button event
     * @param doubled whether this click is a double-click
     * @return true when the menu consumed the click
     */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button != 0 || !isMouseOver(mouseX, mouseY)) return false;
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
    protected void updateWidgetNarration(@NonNull NarrationElementOutput builder) {
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
