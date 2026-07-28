package space.ajcool.ardapaths.screens.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Builder;
import lombok.Setter;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import space.ajcool.ardapaths.core.Client;

import java.util.function.Consumer;

/**
 * A custom checkbox widget for UI screens.
 * Displays a checkbox with a label and calls an onChange callback when toggled.
 */
public class CheckboxWidget extends PressableWidget {
    /**
     * The texture resource for the checkbox graphics.
     */
    private static final Identifier TEXTURE = new Identifier("textures/gui/checkbox.png");

    /**
     * The label text displayed next to the checkbox.
     */
    private final Text text;

    /**
     * Whether the checkbox is currently checked.
     */
    private boolean checked;

    /**
     * Whether the checkbox is enabled and can be interacted with.
     */
    @Setter
    private boolean enabled;

    /**
     * Callback function invoked when the checkbox state changes.
     */
    private final Consumer<Boolean> onChange;

    /**
     * Constructs a CheckboxWidget with the given parameters.
     *
     * @param x        the x coordinate
     * @param y        the y coordinate
     * @param width    the width of the checkbox
     * @param height   the height of the checkbox
     * @param text     the label text
     * @param checked  whether the checkbox is initially checked
     * @param enabled  whether the checkbox is enabled
     * @param onChange callback function when the state changes
     */
    @Builder(builderClassName = "CheckboxBuilder", builderMethodName = "create", setterPrefix = "set")
    public CheckboxWidget(int x, int y, int width, int height, Text text, boolean checked, boolean enabled, Consumer<Boolean> onChange) {
        super(x, y, width, height, null);
        this.text = text;
        this.checked = checked;
        this.onChange = onChange;
        this.enabled = enabled;
    }

    @SuppressWarnings("resource")
    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = this.getX();
        int y = this.getY();
        TextRenderer textRenderer = Client.mc().textRenderer;

        if (!enabled) {
            MatrixStack matrices = context.getMatrices();
            matrices.push();
            matrices.translate(0, 0, 2);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.7f);
            context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xFF48494A);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            matrices.pop();

            int textX = x - width - textRenderer.getWidth(text) + 10;
            int textY = y + (height - textRenderer.fontHeight) / 2;
            context.drawTextWithShadow(textRenderer, text, textX, textY, 0xFF48494A);

            return;
        }

        if (this.isHovered()) {
            if (checked) {
                context.drawTexture(TEXTURE, x, y, width, height, 20, 20, 20, 20, 64, 64);
            } else {
                context.drawTexture(TEXTURE, x, y, width, height, 20, 0, 20, 20, 64, 64);
            }
        } else {
            if (checked) {
                context.drawTexture(TEXTURE, x, y, width, height, 0, 20, 20, 20, 64, 64);
            } else {
                context.drawTexture(TEXTURE, x, y, width, height, 0, 0, 20, 20, 64, 64);
            }
        }

        int textX = x - width - textRenderer.getWidth(text) + 10;
        int textY = y + (height - textRenderer.fontHeight) / 2;
        context.drawTextWithShadow(textRenderer, text, textX, textY, 0xFFFFFF);
    }

    @Override
    public void onPress() {
        checked = !checked;
        if (onChange != null) {
            onChange.accept(checked);
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }
}
