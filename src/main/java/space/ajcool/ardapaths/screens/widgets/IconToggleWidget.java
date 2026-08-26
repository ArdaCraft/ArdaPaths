package space.ajcool.ardapaths.screens.widgets;

import lombok.Builder;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

/**
 * Square icon button that toggles between active and inactive states.
 */
public class IconToggleWidget extends PressableWidget {
    /**
     * Texture displayed while the toggle is active.
     */
    private final Identifier activeTexture;

    /**
     * Texture displayed while the toggle is inactive.
     */
    private final Identifier inactiveTexture;

    /**
     * Whether the toggle is currently active.
     */
    private boolean active;

    /**
     * Callback invoked when the active state changes.
     */
    private final Consumer<Boolean> onChange;

    /**
     * Whether the toggle can currently be interacted with.
     */
    @Setter
    private boolean enabled;

    /**
     * Constructs an icon toggle widget.
     *
     * @param x               the x coordinate of the toggle
     * @param y               the y coordinate of the toggle
     * @param width           the width of the toggle
     * @param height          the height of the toggle
     * @param activeTexture   the texture displayed when active
     * @param inactiveTexture the texture displayed when inactive
     * @param active          whether the toggle starts active
     * @param enabled         whether the toggle can be interacted with
     * @param onChange        callback invoked when the active state changes
     */
    @Builder(builderClassName = "IconToggleBuilder", builderMethodName = "create", setterPrefix = "set")
    public IconToggleWidget(int x, int y, int width, int height, Identifier activeTexture, Identifier inactiveTexture,
                            boolean active, boolean enabled, Consumer<Boolean> onChange) {
        super(x, y, width, height, Text.empty());
        this.activeTexture = activeTexture;
        this.inactiveTexture = inactiveTexture;
        this.active = active;
        this.enabled = enabled;
        this.onChange = onChange;
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = this.getX();
        int y = this.getY();
        int size = Math.min(this.width, this.height);

        if (enabled && active) {
            context.fill(x, y, x + this.width, y + this.height, 0x66336699);
        } else if (enabled && this.isHovered()) {
            context.fill(x, y, x + this.width, y + this.height, 0x33FFFFFF);
        }

        Identifier texture = active ? activeTexture : inactiveTexture;
        context.drawTexture(texture, x, y, 0, 0, size, size, size, size);
    }

    @Override
    public void onPress() {
        if (!enabled) {
            return;
        }

        active = !active;
        if (onChange != null) {
            onChange.accept(active);
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }
}
