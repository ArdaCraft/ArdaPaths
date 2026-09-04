package space.ajcool.ardapaths.screens.widgets;

import lombok.Builder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import space.ajcool.ardapaths.core.Client;

/**
 * A wrapper around Minecraft's TextWidget that provides a convenient setText method.
 * Used for displaying static or dynamic text labels in custom UI screens.
 */
public class TextWidget extends net.minecraft.client.gui.components.StringWidget {

    /**
     * Whether an overflowing message should scroll horizontally instead of being clipped.
     */
    private final boolean scrolling;

    /**
     * Constructs a non-scrolling TextWidget with the given parameters.
     *
     * @param x       the x coordinate
     * @param y       the y coordinate
     * @param width   the width of the widget
     * @param height  the height of the widget
     * @param message the text message to display
     */
    public TextWidget(int x, int y, int width, int height, Component message) {
        this(x, y, width, height, message, false);
    }

    /**
     * Constructs a TextWidget with the given parameters.
     *
     * @param x         the x coordinate
     * @param y         the y coordinate
     * @param width     the width of the widget
     * @param height    the height of the widget
     * @param message   the text message to display
     * @param scrolling whether an overflowing message should scroll instead of being clipped
     */
    @Builder(builderClassName = "TextBuilder", builderMethodName = "create", setterPrefix = "set")
    @SuppressWarnings("resource")
    public TextWidget(int x, int y, int width, int height, Component message, boolean scrolling) {
        super(x, y, width, height, message, Client.mc().font);
        this.scrolling = scrolling;
    }

    /**
     * Sets the text displayed by this widget.
     *
     * @param message the new text message
     */
    public void setText(Component message) {
        this.setMessage(message);
    }

    @SuppressWarnings("resource")
    @Override
    public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (scrolling && Client.mc().font.width(getMessage()) > getWidth()) {
            this.renderScrollingString(context, Client.mc().font, 2, getColor());
            return;
        }

        super.renderWidget(context, mouseX, mouseY, delta);
    }
}
