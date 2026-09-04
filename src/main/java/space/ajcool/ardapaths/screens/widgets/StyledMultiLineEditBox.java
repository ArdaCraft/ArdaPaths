package space.ajcool.ardapaths.screens.widgets;

import lombok.Builder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import space.ajcool.ardapaths.core.Client;

import java.util.function.Consumer;

/**
 * Styled wrapper around Minecraft's multi-line edit box.
 */
public class StyledMultiLineEditBox extends AbstractWidget {

    /** Opaque black input background colour. */
    private static final int BACKGROUND_COLOR = 0xFF000000;

    /** One-pixel focused outline colour. */
    private static final int FOCUS_OUTLINE_COLOR = 0xFFBFBFBF;

    /** Vanilla editor that owns multi-line text behavior. */
    private final MultiLineEditBox editor;

    /**
     * Creates a styled multi-line edit box with the given parameters.
     *
     * @param x           the x coordinate
     * @param y           the y coordinate
     * @param width       the width of the input box
     * @param height      the height of the input box
     * @param title       the narration title for the input box
     * @param placeholder the placeholder text when empty
     */
    @Builder(builderClassName = "StyledMultiLineEditBoxBuilder", builderMethodName = "create", setterPrefix = "set")
    @SuppressWarnings("resource")
    public StyledMultiLineEditBox(int x, int y, int width, int height, Component title, Component placeholder) {
        super(x, y, width, height, title != null ? title : Component.empty());
        this.editor = MultiLineEditBox.builder()
                .setX(x)
                .setY(y)
                .setPlaceholder(placeholder)
                .setShowBackground(false)
                .build(Client.mc().font, width, height, title != null ? title : Component.empty());
    }

    /**
     * Forwards character-limit configuration to the wrapped editor.
     *
     * @param limit maximum number of characters accepted by the editor
     */
    public void setCharacterLimit(int limit) {
        editor.setCharacterLimit(limit);
    }

    /**
     * Forwards value-change callbacks to the wrapped editor.
     *
     * @param listener callback invoked when the text changes
     */
    public void setValueListener(Consumer<String> listener) {
        editor.setValueListener(listener);
    }

    /**
     * Updates the wrapped editor text.
     *
     * @param value new editor value
     */
    public void setValue(String value) {
        editor.setValue(value);
    }

    /**
     * Draws the custom input background, editor contents, and focus outline.
     *
     * @param context draw context for the current frame
     * @param mouseX  current mouse x coordinate
     * @param mouseY  current mouse y coordinate
     * @param delta   partial tick delta
     */
    @Override
    protected void extractWidgetRenderState(@NonNull GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        editor.setX(getX());
        editor.setY(getY());
        editor.setSize(getWidth(), getHeight());
        context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, BACKGROUND_COLOR);
        editor.extractRenderState(context, mouseX, mouseY, delta);

        if (isFocused()) {
            int x = this.getX();
            int y = this.getY();
            int w = this.width;
            int h = this.height;
            context.fill(x, y, x + w, y + 1, FOCUS_OUTLINE_COLOR);
            context.fill(x, y + h - 1, x + w, y + h, FOCUS_OUTLINE_COLOR);
            context.fill(x, y, x + 1, y + h, FOCUS_OUTLINE_COLOR);
            context.fill(x + w - 1, y, x + w, y + h, FOCUS_OUTLINE_COLOR);
        }
    }

    /**
     * Keeps wrapped editor focus in sync with the screen child focus.
     *
     * @param focused whether this widget is focused
     */
    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        editor.setFocused(focused);
    }

    /**
     * Forwards mouse clicks to the wrapped editor and syncs focus.
     *
     * @param event   clicked mouse button event
     * @param doubled whether the click is a double click
     * @return true when the wrapped editor handled the click
     */
    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubled) {
        boolean handled = editor.mouseClicked(event, doubled);
        this.setFocused(handled);
        return handled;
    }

    /**
     * Forwards mouse releases to the wrapped editor.
     *
     * @param event released mouse button event
     * @return true when the wrapped editor handled the release
     */
    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        return editor.mouseReleased(event);
    }

    /**
     * Forwards key presses to the wrapped editor.
     *
     * @param event pressed key event
     * @return true when the wrapped editor handled the key press
     */
    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        return editor.keyPressed(event);
    }

    /**
     * Forwards typed characters to the wrapped editor.
     *
     * @param event typed character event
     * @return true when the wrapped editor handled the character
     */
    @Override
    public boolean charTyped(@NonNull CharacterEvent event) {
        return editor.charTyped(event);
    }

    /**
     * Supplies the wrapped editor narration.
     *
     * @param builder narration builder
     */
    @Override
    protected void updateWidgetNarration(@NonNull NarrationElementOutput builder) {
        editor.updateWidgetNarration(builder);
    }
}
