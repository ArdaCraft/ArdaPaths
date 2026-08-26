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
     * @param x        the x coordinate of the label's left edge
     * @param y        the y coordinate of the label and checkbox row
     * @param width    the full component width containing the label, gap, and checkbox square
     * @param height   the component height and square checkbox size
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
        int boxSize = this.height;
        int boxX = x + this.width - boxSize;
        int textY = y + (height - textRenderer.fontHeight) / 2;

        if (!enabled) {
            MatrixStack matrices = context.getMatrices();
            matrices.push();
            matrices.translate(0, 0, 2);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.7f);
            context.fill(boxX, y, boxX + boxSize, y + boxSize, 0xFF48494A);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            matrices.pop();

            context.drawTextWithShadow(textRenderer, text, x, textY, 0xFF48494A);

            return;
        }

        if (this.isHovered()) {
            if (checked) {
                context.drawTexture(TEXTURE, boxX, y, boxSize, boxSize, 20, 20, 20, 20, 64, 64);
            } else {
                context.drawTexture(TEXTURE, boxX, y, boxSize, boxSize, 20, 0, 20, 20, 64, 64);
            }
        } else {
            if (checked) {
                context.drawTexture(TEXTURE, boxX, y, boxSize, boxSize, 0, 20, 20, 20, 64, 64);
            } else {
                context.drawTexture(TEXTURE, boxX, y, boxSize, boxSize, 0, 0, 20, 20, 64, 64);
            }
        }

        context.drawTextWithShadow(textRenderer, text, x, textY, 0xFFFFFF);
    }

    @Override
    public void onPress() {
        if (!enabled) {
            return;
        }

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
