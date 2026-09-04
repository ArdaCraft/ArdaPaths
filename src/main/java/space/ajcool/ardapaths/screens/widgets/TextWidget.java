package space.ajcool.ardapaths.screens.widgets;

import lombok.Builder;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import space.ajcool.ardapaths.core.Client;

/**
 * A wrapper around Minecraft's TextWidget that provides a convenient setText method.
 * Used for displaying static or dynamic text labels in custom UI screens.
 */
public class TextWidget extends AbstractWidget {

    /** Left-aligned horizontal text placement. */
    private static final int ALIGN_LEFT = 0;

    /** Centered horizontal text placement. */
    private static final int ALIGN_CENTER = 1;

    /** Right-aligned horizontal text placement. */
    private static final int ALIGN_RIGHT = 2;

    /** ARGB text colour used to draw the widget. */
    @Setter
    // Accessed via Lombok-generated accessor; IntelliJ entry-point analysis can't follow it.
    @SuppressWarnings("unused")
    private int color = 0xFFFFFFFF;

    /** Horizontal alignment mode. */
    private int alignment = ALIGN_CENTER;

    /** Whether an overflowing message should scroll horizontally instead of being clipped. */
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
    public TextWidget(int x, int y, int width, int height, Component message, boolean scrolling) {
        super(x, y, width, height, message);
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

    /**
     * Aligns text to the left edge of the widget.
     */
    public void alignLeft() {
        alignment = ALIGN_LEFT;
    }

    /**
     * Aligns text to the right edge of the widget.
     */
    public void alignRight() {
        alignment = ALIGN_RIGHT;
    }

    @SuppressWarnings("resource")
    @Override
    public void extractWidgetRenderState(@NonNull GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        var font = Client.mc().font;
        int textWidth = font.width(getMessage());
        int textX = switch (alignment) {
            case ALIGN_LEFT -> getX();
            case ALIGN_RIGHT -> getX() + getWidth() - textWidth;
            default -> getX() + (getWidth() - textWidth) / 2;
        };
        int textY = getY() + (getHeight() - font.lineHeight) / 2;
        Component message = getMessage();

        if (scrolling && textWidth > getWidth()) {
            int scissorRight = getX() + getWidth();
            context.enableScissor(getX(), getY(), scissorRight, getY() + getHeight());
            context.text(font, font.plainSubstrByWidth(message.getString(), getWidth()), getX(), textY, color);
            context.disableScissor();
            return;
        }

        context.text(font, message, textX, textY, color);
    }

    /**
     * Supplies the widget's message for narration.
     *
     * @param builder narration builder
     */
    @Override
    protected void updateWidgetNarration(@NonNull NarrationElementOutput builder) {
        defaultButtonNarrationText(builder);
    }
}
