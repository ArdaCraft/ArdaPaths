package space.ajcool.ardapaths.screens.widgets;

import lombok.Getter;
import lombok.Setter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.screens.GuiTextures;

/**
 * A custom input box widget with text validation support.
 * Validates input as the user types and displays error messages.
 */
@Environment(EnvType.CLIENT)
// Instantiated via screen/builder factory; IntelliJ entry-point analysis can't follow it.
@SuppressWarnings("unused")
public class InputBoxWidget extends AbstractWidget {

    /** Vanilla multiline editor that owns text editing, cursor movement, and wrapping. */
    private final MultiLineEditBox editor;

    /** The text validator that validates input content. */
    private final TextValidator validator;

    /** Whether this input box is enabled for editing. */
    @Getter
    private boolean enabled;

    /** The current validation error message, or null if valid.  */
    private String errorMessage;

    /** Whether the user has triggered validation at least once. */
    private boolean hasValidatedOnce;

    /** The background colour for the input box (-1 for default). */
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
    @lombok.Builder(builderClassName = "InputBoxBuilder", builderMethodName = "create", setterPrefix = "set")
    @SuppressWarnings("resource")
    public InputBoxWidget(int x, int y, int width, int height, Component title, Component placeholder, TextValidator validator, boolean enabled) {
        super(x, y, width, height, title != null ? title : Component.empty());
        this.editor = MultiLineEditBox.builder()
                .setX(x)
                .setY(y)
                .setPlaceholder(placeholder)
                .setShowBackground(false)
                .build(Client.mc().font, width, height, title != null ? title : Component.empty());
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
        this.editor.setTooltip(Tooltip.create(Component.literal("Disabled")));
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
        editor.setFocused(focused);
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
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (!enabled)
            return false;

        int keyCode = event.key();
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            this.setFocused(false);
            return true;
        }
        return editor.keyPressed(event);
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent event) {
        if (!enabled) {
            return false;
        }
        return editor.charTyped(event);
    }

    /**
     * If the widget is disabled, render its background/border (via super.render) then
     * overdraw its text in a light grey colour and show a tooltip when hovered.
     */
    @SuppressWarnings("resource")
    @Override
    public void extractWidgetRenderState(@NonNull GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        editor.setX(getX());
        editor.setY(getY());
        editor.setSize(getWidth(), getHeight());
        context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xFF000000);
        editor.extractRenderState(context, mouseX, mouseY, delta);

        if (!enabled) {
            context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height,
                    GuiTextures.withAlpha(0xFF000000, 179));
            return;
        }

        if (backgroundColor != Integer.MIN_VALUE) {

            // Convert raw text → coloured styled text (you define this)
            Component colored = Component.literal(this.getValue()).withStyle(Style.EMPTY.withColor(backgroundColor));

            // Coordinates for drawing inside the box
            int textX = this.getX() + 4;
            int textY = this.getY() + (this.height - 8) / 2;

            context.text(
                    Client.mc().font,
                    colored,
                    textX,
                    textY,
                    0xFFFFFFFF, // ignored for literal() because color is inside the style
                    false
            );
        }

        if (errorMessage != null && !errorMessage.isEmpty()) {
            int errorX = this.getX();
            int errorY = this.getY() + this.height + 2;
            context.pose().pushMatrix();
            context.pose().scale(0.85f, 0.85f);
            context.text(Client.mc().font, errorMessage, (int) (errorX / 0.85), (int) (errorY / 0.85), 0xFFFF5555);
            context.pose().popMatrix();
        }

        if (isFocused()) {
            int outlineColor = 0xFFBFBFBF;
            int x = this.getX();
            int y = this.getY();
            int w = this.width;
            int h = this.height;
            context.fill(x, y, x + w, y + 1, outlineColor);
            context.fill(x, y + h - 1, x + w, y + h, outlineColor);
            context.fill(x, y, x + 1, y + h, outlineColor);
            context.fill(x + w - 1, y, x + w, y + h, outlineColor);
        }
    }

    /**
     * Prevent mouse clicks if disabled.
     */
    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubled) {
        if (!isEnabled()) {
            return false;
        }
        return editor.mouseClicked(event, doubled);
    }

    /**
     * Getters and setters for enabled state.
     */
    public void enable() {
        this.enabled = true;
        this.setTooltip(null);
        this.editor.setTooltip(null);
    }

    public void reset() {
        setValue("");
        resetValidation();
    }

    /**
     * Sets the editor text and reruns validation after live validation has started.
     *
     * @param text new editor value
     */
    public void setValue(String text) {
        editor.setValue(text);
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

    /**
     * Returns the current editor text.
     *
     * @return text value inside the multiline editor
     */
    public String getValue() {
        return editor.getValue();
    }

    /**
     * Forwards value-change callbacks to the wrapped editor.
     *
     * @param listener callback invoked when the editor value changes
     */
    public void setValueListener(java.util.function.Consumer<String> listener) {
        editor.setValueListener(listener);
    }

    /**
     * Supplies no additional narration beyond the wrapped editor state.
     *
     * @param builder narration builder
     */
    @Override
    protected void updateWidgetNarration(@NonNull NarrationElementOutput builder) {
    }
}
