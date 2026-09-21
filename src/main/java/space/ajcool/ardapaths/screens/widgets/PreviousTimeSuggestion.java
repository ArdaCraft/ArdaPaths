package space.ajcool.ardapaths.screens.widgets;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import space.ajcool.ardapaths.core.data.TimeOfDay;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Focus-gated button that applies the nearest previous marker time to a date-time input.
 */
public class PreviousTimeSuggestion {
    /** Date-time input that receives the suggested previous marker time. */
    private final InputBoxWidget target;

    /** Button shown below the input while a suggestion is available. */
    private final Button button;

    /** Supplies the nearest previous configured marker time, or null when none exists. */
    private final Supplier<Long> previousTimeSupplier;

    /**
     * Creates a previous-time suggestion helper.
     *
     * @param target               date-time input that receives the suggestion
     * @param button               button shown while the target input is focused
     * @param previousTimeSupplier supplier for the current previous marker time
     */
    private PreviousTimeSuggestion(InputBoxWidget target, Button button, Supplier<Long> previousTimeSupplier) {
        this.target = target;
        this.button = button;
        this.previousTimeSupplier = previousTimeSupplier;
    }

    /**
     * Builds, registers, and returns a previous-time suggestion helper.
     *
     * @param target               date-time input that receives the suggestion
     * @param previousTimeSupplier supplier for the current previous marker time
     * @param x                    button x coordinate
     * @param y                    button y coordinate
     * @param width                button width
     * @param height               button height
     * @param register             hook that adds the button to its owning screen
     * @return helper that controls the registered button
     */
    public static PreviousTimeSuggestion create(InputBoxWidget target,
                                                Supplier<Long> previousTimeSupplier,
                                                int x,
                                                int y,
                                                int width,
                                                int height,
                                                Function<Button, Button> register) {
        PreviousTimeSuggestion[] suggestion = new PreviousTimeSuggestion[1];
        Button button = Button.builder(Component.empty(), ignored -> suggestion[0].apply())
                .bounds(x, y, width, height)
                .build();
        button.visible = false;
        suggestion[0] = new PreviousTimeSuggestion(target, register.apply(button), previousTimeSupplier);
        return suggestion[0];
    }

    /**
     * Applies the current suggested time to the target input.
     */
    private void apply() {
        Long previous = previousTimeSupplier.get();
        if (previous == null) {
            return;
        }

        target.setValue(TimeOfDay.format(previous));
        target.validateText();
    }

    /**
     * Updates the suggestion button visibility and displayed formatted time.
     */
    public void tick() {
        Long previous = previousTimeSupplier.get();
        button.visible = previous != null && target.isFocused();
        if (previous != null) {
            button.setMessage(Component.literal(TimeOfDay.format(previous)));
        }
    }
}
