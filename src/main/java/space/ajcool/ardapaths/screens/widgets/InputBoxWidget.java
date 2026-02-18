package space.ajcool.ardapaths.screens.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import space.ajcool.ardapaths.core.Client;

@Environment(EnvType.CLIENT)
public class InputBoxWidget extends EditBoxWidget
{
    private boolean enabled;
    private final TextValidator validator;
    private String errorMessage;
    private boolean hasValidatedOnce;
    private int backgroundColor = Integer.MIN_VALUE;

    public InputBoxWidget(int x, int y, int width, int height, Text title, Text placeholder, TextValidator validator, boolean enabled)
    {
        super(Client.mc().textRenderer, x, y, width, height, placeholder, title != null ? title : Text.empty());
        this.validator = validator;
        this.errorMessage = null;
        this.hasValidatedOnce = false;
        this.enabled = enabled;
        if (!enabled)
        {
            this.disable();
        }
    }

    /**
     * Override setText so that after the first validation, every edit revalidates.
     */
    @Override
    public void setText(String text)
    {
        super.setText(text);
        if (hasValidatedOnce)
        {
            validateText();
        }
    }

    /**
     * When focus is lost, validate the text and enable live validation.
     */
    @Override
    public void setFocused(boolean focused)
    {
        if (!enabled && focused)
        {
            return;
        }

        if (this.isFocused() && !focused)
        {
            hasValidatedOnce = true;
            validateText();
        }
        super.setFocused(focused);
    }

    /**
     * When the enter key is pressed, unfocus the input box.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (!enabled)
        {
            return false;
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
        {
            this.setFocused(false);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers)
    {
        if (!enabled)
        {
            return false;
        }
        return super.charTyped(chr, modifiers);
    }

    /**
     * If the widget is disabled, render its background/border (via super.render) then
     * overdraw its text in a light gray color and show a tooltip when hovered.
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta)
    {
        super.render(context, mouseX, mouseY, delta);

        if (!enabled)
        {
            MatrixStack matrices = context.getMatrices();
            matrices.push();
            matrices.translate(0, 0, 2);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.7f);
            context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xFF000000);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            matrices.pop();

            return;
        }

        if (backgroundColor != Integer.MIN_VALUE) {

            // Draw colored text on top of the textbox
            String raw = this.getText();

            // Convert raw text → colored styled text (you define this)
            Text colored = Text.literal(this.getText()).fillStyle(Style.EMPTY.withColor(backgroundColor));;

            // Coordinates for drawing inside the box
            int textX = this.getX() + 4;
            int textY = this.getY() + (this.height - 8) / 2;

            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 5); // ensure it's above box text
            context.drawText(
                    Client.mc().textRenderer,
                    colored,
                    textX,
                    textY,
                    0xFFFFFFFF, // ignored for literal() because color is inside the style
                    false
            );
            context.getMatrices().pop();
        }

        if (errorMessage != null && !errorMessage.isEmpty())
        {
            int errorX = this.getX();
            int errorY = this.getY() + this.height + 2;
            context.getMatrices().push();
            context.getMatrices().scale(0.85f, 0.85f, 1.0f);
            context.drawTextWithShadow(Client.mc().textRenderer, errorMessage, (int) (errorX / 0.85), (int) (errorY / 0.85), 0xFFFF5555);
            context.getMatrices().pop();
        }
    }

    /**
     * Prevent mouse clicks if disabled.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (!isEnabled())
        {
            return false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Getters and setters for enabled state.
     */
    public void enable()
    {
        this.enabled = true;
        this.setTooltip(null);
    }

    public void disable()
    {
        this.enabled = false;
        this.setFocused(false);
        this.setTooltip(Tooltip.of(Text.literal("Disabled")));
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * Validates the current text. If the text is invalid, stores the error message.
     */
    public boolean validateText()
    {
        try
        {
            validator.validate(getText());
            errorMessage = null;
            return true;
        }
        catch (TextValidationError e)
        {
            errorMessage = e.getMessage();
            return false;
        }
    }

    public void resetValidation()
    {
        errorMessage = null;
        hasValidatedOnce = false;
    }

    public void reset()
    {
        setText("");
        resetValidation();
    }

    public void reset(String text)
    {
        setText(text);
        resetValidation();
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setTextColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
}