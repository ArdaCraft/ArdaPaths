package space.ajcool.ardapaths.screens.widgets;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class InputBoxWidget extends EditBoxWidget {
    private final TextValidator validator;
    private String errorMessage;
    private boolean hasValidatedOnce;

    public InputBoxWidget(TextRenderer textRenderer, int x, int y, int width, int height, Text placeholder) {
        this(textRenderer, x, y, width, height, placeholder, text -> {});
    }

    public InputBoxWidget(TextRenderer textRenderer, int x, int y, int width, int height, Text placeholder, TextValidator validator) {
        super(textRenderer, x, y, width, height, placeholder, Text.empty());
        this.validator = validator;
        this.errorMessage = null;
        this.hasValidatedOnce = false;
    }

    /**
     * Validates the current text. If the text is invalid, stores the error message.
     */
    public boolean validateText() {
        try {
            validator.validate(getText());
            errorMessage = null;
            return true;
        } catch (TextValidationError e) {
            errorMessage = e.getMessage();
            return false;
        }
    }

    /**
     * Override setText so that after the first validation, every edit revalidates.
     */
    @Override
    public void setText(String text) {
        super.setText(text);
        if (hasValidatedOnce) {
            validateText();
        }
    }

    /**
     * When focus is lost, validate the text and enable live validation.
     */
    @Override
    public void setFocused(boolean focused) {
        if (!focused) {
            hasValidatedOnce = true;
            validateText();
        }
        super.setFocused(focused);
    }

    /**
     * When the enter key is pressed, unfocus the input box.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            this.setFocused(false);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Render the input box and, if present, the error message.
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        if (errorMessage != null && !errorMessage.isEmpty()) {
            int errorX = this.getX();
            int errorY = this.getY() + this.height + 2;
            context.getMatrices().push();
            context.getMatrices().scale(0.85f, 0.85f, 1.0f);
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, errorMessage, (int) (errorX / 0.85), (int) (errorY / 0.85), 0xFFFF5555);
            context.getMatrices().pop();
        }
    }
}