package space.ajcool.ardapaths.screens.widgets;

/**
 * An exception thrown when text validation fails.
 */
public class TextValidationError extends Exception {

    /**
     * Constructs a TextValidationError with the given error message.
     *
     * @param message the error message explaining why validation failed
     */
    public TextValidationError(String message) {
        super(message);
    }
}
