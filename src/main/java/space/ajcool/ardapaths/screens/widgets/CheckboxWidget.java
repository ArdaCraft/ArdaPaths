package space.ajcool.ardapaths.screens.widgets;

import lombok.Builder;
import lombok.Setter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.screens.GuiTextures;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Consumer;

/**
 * A custom checkbox widget for UI screens.
 * Displays a checkbox with a label and calls an onChange callback when toggled.
 */
public class CheckboxWidget extends AbstractButton {
    /**
     * The texture resource for the checkbox graphics.
     */
    private static final ResourceLocation TEXTURE = ModConstants.modId("textures/gui/checkbox.png");

    /**
     * The label text displayed next to the checkbox.
     */
    private final Component text;

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
    public CheckboxWidget(int x, int y, int width, int height, Component text, boolean checked, boolean enabled, Consumer<Boolean> onChange) {
        super(x, y, width, height, null);
        this.text = text;
        this.checked = checked;
        this.onChange = onChange;
        this.enabled = enabled;
    }

    @SuppressWarnings("resource")
    @Override
    protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        int x = this.getX();
        int y = this.getY();
        Font textRenderer = Client.mc().font;
        int boxSize = this.height;
        int boxX = x + this.width - boxSize;
        int textY = y + (height - textRenderer.lineHeight) / 2;

        if (!enabled) {
            PoseStack matrices = context.pose();
            matrices.pushPose();
            matrices.translate(0, 0, 2);
            context.fill(boxX, y, boxX + boxSize, y + boxSize, GuiTextures.withAlpha(0xFF48494A, 179));
            matrices.popPose();

            context.drawString(textRenderer, text, x, textY, 0xFF48494A);

            return;
        }

        if (this.isHovered()) {
            if (checked) {
                GuiTextures.blit(context, TEXTURE, boxX, y, boxSize, boxSize, 20, 20, 20, 20, 64, 64);
            } else {
                GuiTextures.blit(context, TEXTURE, boxX, y, boxSize, boxSize, 20, 0, 20, 20, 64, 64);
            }
        } else {
            if (checked) {
                GuiTextures.blit(context, TEXTURE, boxX, y, boxSize, boxSize, 0, 20, 20, 20, 64, 64);
            } else {
                GuiTextures.blit(context, TEXTURE, boxX, y, boxSize, boxSize, 0, 0, 20, 20, 64, 64);
            }
        }

        context.drawString(textRenderer, text, x, textY, 0xFFFFFF);
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
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        this.defaultButtonNarrationText(builder);
    }
}
