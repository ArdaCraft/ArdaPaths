package space.ajcool.ardapaths.screens.widgets;

/**
 * A callback interface for validating text.
 */
public interface TextValidator {
    /**
     * Validates the provided text.
     *
     * @param text The text to validate
     * @throws TextValidationError if the text is not valid
     */
    void validate(String text) throws TextValidationError;
}
