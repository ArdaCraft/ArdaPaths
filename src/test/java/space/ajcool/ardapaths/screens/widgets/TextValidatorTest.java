package space.ajcool.ardapaths.screens.widgets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the text validation callback contract.
 */
class TextValidatorTest {
    /**
     * Verifies validator callbacks can accept valid text without throwing.
     */
    @Test
    void validatorAcceptsValidText() {
        TextValidator validator = text -> {
            if (text.isBlank()) {
                throw new TextValidationError("blank");
            }
        };

        assertDoesNotThrow(() -> validator.validate("frodo"));
    }

    /**
     * Verifies validator callbacks communicate invalid text through TextValidationError.
     */
    @Test
    void validatorRejectsInvalidText() {
        TextValidator validator = text -> {
            if (text.isBlank()) {
                throw new TextValidationError("blank");
            }
        };

        assertThrows(TextValidationError.class, () -> validator.validate(" "));
    }
}
