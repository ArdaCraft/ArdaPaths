package space.ajcool.ardapaths.screens.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.lwjgl.glfw.GLFW;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.screens.GuiTextures;

/**
 * A custom input box widget with text validation support.
 * Validates input as the user types and displays error messages.
 */
@Environment(EnvType.CLIENT)
public class InputBoxWidget extends MultiLineEditBox {
    /**
     * The text validator that validates input content.
     */
    private final TextValidator validator;

    /**
     * Whether this input box is enabled for editing.
     */
    @Getter
    private boolean enabled;

    /**
     * The current validation error message, or null if valid.
     */
    private String errorMessage;

    /**
     * Whether the user has triggered validation at least once.
     */
    private boolean hasValidatedOnce;

    /**
     * The background colour for the input box (-1 for default).
     */
    @Getter
    @Setter
    private int backgroundColor = Integer.MIN_VALUE;

    /**
     * Constructs an InputBoxWidget with the given parameters.
     *
     * @param x           the x coordinate
     * @param y           the y coordinate
     * @param width       the width of the input box
     * @param height      the height of the input box
     * @param title       the title/label text
     * @param placeholder the placeholder text when empty
     * @param validator   the text validator to use
     * @param enabled     whether the input box is enabled
     */
    @Builder(builderClassName = "InputBoxBuilder", builderMethodName = "create", setterPrefix = "set")
    @SuppressWarnings("resource")
    public InputBoxWidget(int x, int y, int width, int height, Component title, Component placeholder, TextValidator validator, boolean enabled) {
        super(Client.mc().font, x, y, width, height, placeholder, title != null ? title : Component.empty());
        this.validator = validator;
        this.errorMessage = null;
        this.hasValidatedOnce = false;
        this.enabled = enabled;
        if (!enabled) {
            this.disable();
        }
    }

    public void disable() {
        this.enabled = false;
        this.setFocused(false);
        this.setTooltip(Tooltip.create(Component.literal("Disabled")));
    }

    /**
     * When focus is lost, validate the text and enable live validation.
     */
    @Override
    public void setFocused(boolean focused) {
        if (!enabled && focused) {
            return;
        }

        if (this.isFocused() && !focused) {
            hasValidatedOnce = true;
            validateText();
        }
        super.setFocused(focused);
    }

    /**
     * Validates the current text. If the text is invalid, stores the error message.
     *
     * @return true if validation succeeds, false if validation fails
     */
    public boolean validateText() {

        if (validator == null) return true;

        try {
            validator.validate(getValue());
            errorMessage = null;
            return true;
        } catch (TextValidationError e) {
            errorMessage = e.getMessage();
            return false;
        }
    }

    /**
     * When the enter key is pressed, unfocus the input box.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!enabled) {
            return false;
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            this.setFocused(false);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!enabled) {
            return false;
        }
        return super.charTyped(chr, modifiers);
    }

    /**
     * If the widget is disabled, render its background/border (via super.render) then
     * overdraw its text in a light grey colour and show a tooltip when hovered.
     */
    @SuppressWarnings("resource")
    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        if (!enabled) {
            PoseStack matrices = context.pose();
            matrices.pushPose();
            matrices.translate(0, 0, 2);
            context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height,
                    GuiTextures.withAlpha(0xFF000000, 179));
            matrices.popPose();

            return;
        }

        if (backgroundColor != Integer.MIN_VALUE) {

            // Convert raw text → coloured styled text (you define this)
            Component colored = Component.literal(this.getValue()).withStyle(Style.EMPTY.withColor(backgroundColor));

            // Coordinates for drawing inside the box
            int textX = this.getX() + 4;
            int textY = this.getY() + (this.height - 8) / 2;

            context.pose().pushPose();
            context.pose().translate(0, 0, 5); // ensure it's above box text
            context.drawString(
                    Client.mc().font,
                    colored,
                    textX,
                    textY,
                    0xFFFFFFFF, // ignored for literal() because color is inside the style
                    false
            );
            context.pose().popPose();
        }

        if (errorMessage != null && !errorMessage.isEmpty()) {
            int errorX = this.getX();
            int errorY = this.getY() + this.height + 2;
            context.pose().pushPose();
            context.pose().scale(0.85f, 0.85f, 1.0f);
            context.drawString(Client.mc().font, errorMessage, (int) (errorX / 0.85), (int) (errorY / 0.85), 0xFFFF5555);
            context.pose().popPose();
        }
    }

    /**
     * Prevent mouse clicks if disabled.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isEnabled()) {
            return false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Getters and setters for enabled state.
     */
    public void enable() {
        this.enabled = true;
        this.setTooltip(null);
    }

    public void reset() {
        setValue("");
        resetValidation();
    }

    /**
     * Override setText so that after the first validation, every edit revalidates.
     */
    @Override
    public void setValue(String text) {
        super.setValue(text);
        if (hasValidatedOnce) {
            validateText();
        }
    }

    public void resetValidation() {
        errorMessage = null;
        hasValidatedOnce = false;
    }

    public void reset(String text) {
        setValue(text);
        resetValidation();
    }
}
