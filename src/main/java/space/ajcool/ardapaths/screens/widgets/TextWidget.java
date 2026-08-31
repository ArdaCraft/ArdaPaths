package space.ajcool.ardapaths.screens.widgets;

import lombok.Builder;
import net.minecraft.network.chat.Component;
import space.ajcool.ardapaths.core.Client;

/**
 * A wrapper around Minecraft's TextWidget that provides a convenient setText method.
 * Used for displaying static or dynamic text labels in custom UI screens.
 */
public class TextWidget extends net.minecraft.client.gui.components.StringWidget {
    /**
     * Constructs a TextWidget with the given parameters.
     *
     * @param x       the x coordinate
     * @param y       the y coordinate
     * @param width   the width of the widget
     * @param height  the height of the widget
     * @param message the text message to display
     */
    @Builder(builderClassName = "TextBuilder", builderMethodName = "create", setterPrefix = "set")
    @SuppressWarnings("resource")
    public TextWidget(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message, Client.mc().font);
    }

    /**
     * Sets the text displayed by this widget.
     *
     * @param message the new text message
     */
    public void setText(Component message) {
        this.setMessage(message);
    }
}
