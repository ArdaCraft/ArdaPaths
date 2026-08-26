package space.ajcool.ardapaths.screens.widgets;

import lombok.Builder;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import space.ajcool.ardapaths.core.Client;

import java.util.List;

/**
 * Floating menu used by screens to offer row-scoped contextual actions.
 */
public class ContextMenuWidget extends ClickableWidget {
    /**
     * Texture resource used by vanilla button-like widgets.
     */
    private static final Identifier WIDGETS_TEXTURE = new Identifier("textures/gui/widgets.png");

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
        super(x, y, widthFor(items), items.size() * ITEM_HEIGHT, Text.empty());
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
        TextRenderer textRenderer = Client.mc().textRenderer;
        int width = MIN_WIDTH;
        for (Item item : items) {
            width = Math.max(width, textRenderer.getWidth(item.label()) + 16);
        }
        return Math.min(MAX_WIDTH, width);
    }

    /**
     * Renders the menu above ordinary screen widgets.
     *
     * @param context draw context
     * @param mouseX  current mouse x coordinate
     * @param mouseY  current mouse y coordinate
     * @param delta   partial tick delta
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(0, 0, 200);
        super.render(context, mouseX, mouseY, delta);
        matrices.pop();
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
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        for (int index = 0; index < items.size(); index++) {
            Item item = items.get(index);
            int itemY = getY() + index * ITEM_HEIGHT;
            boolean hovered = mouseX >= getX() && mouseX <= getX() + getWidth() && mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT;
            int v = hovered ? 86 : 46;
            int color = item.enabled() ? 0xFFFFFF : 0x777777;
            context.drawNineSlicedTexture(WIDGETS_TEXTURE, getX(), itemY, getWidth(), ITEM_HEIGHT, 20, 4, 200, 20, 0, v);
            context.drawTextWithShadow(Client.mc().textRenderer, item.label(), getX() + 4, itemY + 6, color);
            if (hovered && item.tooltip() != null) {
                setTooltip(Tooltip.of(item.tooltip()));
            }
        }
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
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }

    /**
     * One actionable row in a context menu.
     *
     * @param label   text displayed in the row
     * @param tooltip hover help for the row
     * @param enabled whether the row can run its action
     * @param action  work to run when the row is clicked
     */
    public record Item(Text label, Text tooltip, boolean enabled, Runnable action) {
    }
}
