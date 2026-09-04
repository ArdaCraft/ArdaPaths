package space.ajcool.ardapaths.screens.widgets;

import lombok.Builder;
import lombok.Getter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import space.ajcool.ardapaths.core.Client;

import java.util.function.Consumer;

/**
 * Pairs Minecraft's vanilla {@link Checkbox} with a left-aligned label so a toggle row reads
 * "label … box" instead of the vanilla "box label" order.
 * <p>
 * The vanilla checkbox self-sizes around its own message and always draws its box first, so the
 * label is built as a separate {@link TextWidget} and the box is created with an empty message and
 * right-aligned inside the requested row width. Both parts must be attached to the screen; see
 * {@code ArdaPathsScreen#addCheckboxRow}.
 */
public class CheckboxRow {

    /** Horizontal gap kept between the label and the checkbox box. */
    private static final int LABEL_GAP = 4;

    /** Label color used while the row is enabled. */
    private static final int ENABLED_LABEL_COLOR = 0xFFFFFF;

    /** Label color used while the row is disabled. */
    private static final int DISABLED_LABEL_COLOR = 0xFF48494A;

    /** Opacity applied to the checkbox box while the row is disabled. */
    private static final float DISABLED_ALPHA = 0.5F;

    /** The label rendered at the left edge of the row. */
    @Getter
    private final TextWidget label;

    /** The vanilla checkbox rendered at the right edge of the row. */
    @Getter
    private final Checkbox checkbox;

    /**
     * Builds a checkbox row.
     *
     * @param x        the x coordinate of the label's left edge
     * @param y        the y coordinate of the row
     * @param width    the full row width containing the label, gap, and checkbox box
     * @param height   the row height; the box is vertically centered inside it
     * @param text     the label text
     * @param checked  whether the checkbox is initially checked
     * @param enabled  whether the row is interactive
     * @param onChange callback invoked with the new state whenever the checkbox is toggled
     * @param tooltip  optional tooltip shown over both the label and the box, or null for none
     */
    @Builder(builderClassName = "CheckboxRowBuilder", builderMethodName = "create", setterPrefix = "set")
    @SuppressWarnings({"resource", "unused"})
    public CheckboxRow(int x, int y, int width, int height, Component text, boolean checked, boolean enabled,
                       Consumer<Boolean> onChange, Component tooltip) {
        Font font = Client.mc().font;
        int boxSize = Checkbox.getBoxSize(font);

        this.checkbox = Checkbox.builder(Component.empty(), font)
                .pos(x + width - boxSize, y + (height - boxSize) / 2)
                .selected(checked)
                .onValueChange((box, value) -> {
                    if (onChange != null) {
                        onChange.accept(value);
                    }
                })
                .build();

        this.label = TextWidget.create()
                .setX(x)
                .setY(y)
                .setWidth(Math.max(0, width - boxSize - LABEL_GAP))
                .setHeight(height)
                .setMessage(text)
                .setScrolling(true)
                .build();
        this.label.alignLeft();

        if (tooltip != null) {
            Tooltip created = Tooltip.create(tooltip);
            this.label.setTooltip(created);
            this.checkbox.setTooltip(created);
        }

        this.setEnabled(enabled);
    }

    /**
     * Enables or disables the row, dimming the box and greying the label while disabled.
     *
     * @param enabled whether the row should accept interaction
     */
    public void setEnabled(boolean enabled) {
        this.checkbox.active = enabled;
        this.checkbox.setAlpha(enabled ? 1.0F : DISABLED_ALPHA);
        this.label.setColor(enabled ? ENABLED_LABEL_COLOR : DISABLED_LABEL_COLOR);
    }
}
