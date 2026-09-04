package space.ajcool.ardapaths.screens.marker;

import space.ajcool.ardapaths.screens.MarkerEditScreen;

/**
 * One mountable marker editor tab section.
 */
public interface MarkerEditorTab {

    /**
     * Builds this tab's widgets into the marker edit screen.
     *
     * @param screen marker edit screen that owns the widgets
     * @param layout current marker edit layout
     * @param state  mutable form state to display and edit
     */
    void build(MarkerEditScreen screen, MarkerEditLayout layout, MarkerFormState state);

    /**
     * Copies mounted widget values into a form state.
     *
     * @param state mutable form state to update
     */
    void commitTo(MarkerFormState state);

    /**
     * Validates all mounted inputs owned by this tab.
     *
     * @return true when all mounted inputs are valid
     */
    boolean validate();

    /**
     * Handles mouse release events for widgets that need manual delegation.
     *
     * @param mouseX the mouse x coordinate
     * @param mouseY the mouse y coordinate
     * @param button the mouse button code
     * @return true if the event was handled
     */
    default boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    /**
     * Updates tab-owned widgets that need a per-tick hook.
     */
    @SuppressWarnings("EmptyMethod")
    default void tick() {
    }
}
