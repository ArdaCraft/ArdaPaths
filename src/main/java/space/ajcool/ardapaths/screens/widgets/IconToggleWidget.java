package space.ajcool.ardapaths.screens.widgets;

import lombok.Builder;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import space.ajcool.ardapaths.screens.GuiTextures;

import java.util.function.Consumer;

/**
 * Square icon button that toggles between active and inactive states.
 */
// Instantiated via screen/builder factory; IntelliJ entry-point analysis can't follow it.
@SuppressWarnings("unused")
public class IconToggleWidget extends AbstractButton {

    /**
     * Texture displayed while the toggle is active.
     */
    private final Identifier activeTexture;

    /**
     * Texture displayed while the toggle is inactive.
     */
    private final Identifier inactiveTexture;

    /**
     * Callback invoked when the active state changes.
     */
    private final Consumer<Boolean> onChange;

    /**
     * Whether the toggle is currently active.
     */
    private boolean active;

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
    // Instantiated via screen/builder factory; IntelliJ entry-point analysis can't follow it.
    @SuppressWarnings("unused")
    public IconToggleWidget(int x, int y, int width, int height, Identifier activeTexture, Identifier inactiveTexture,
                            boolean active, boolean enabled, Consumer<Boolean> onChange) {
        super(x, y, width, height, Component.empty());
        this.activeTexture = activeTexture;
        this.inactiveTexture = inactiveTexture;
        this.active = active;
        this.enabled = enabled;
        this.onChange = onChange;
    }

    @Override
    protected void extractContents(@NonNull GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int x = this.getX();
        int y = this.getY();
        int size = Math.min(this.width, this.height);

        if (enabled && active) {
            context.fill(x, y, x + this.width, y + this.height, 0x66336699);
        } else if (enabled && this.isHovered()) {
            context.fill(x, y, x + this.width, y + this.height, 0x33FFFFFF);
        }

        Identifier texture = active ? activeTexture : inactiveTexture;
        GuiTextures.blitSprite(context, texture, x, y, size, size);
    }

    @Override
    public void onPress(@NonNull InputWithModifiers input) {
        if (!enabled) {
            return;
        }

        active = !active;
        if (onChange != null) {
            onChange.accept(active);
        }
    }

    @Override
    protected void updateWidgetNarration(@NonNull NarrationElementOutput builder) {
        this.defaultButtonNarrationText(builder);
    }
}
