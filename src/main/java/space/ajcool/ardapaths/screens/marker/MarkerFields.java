package space.ajcool.ardapaths.screens.marker;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import space.ajcool.ardapaths.core.data.GiveItemAction;
import space.ajcool.ardapaths.core.data.TimeOfDay;
import space.ajcool.ardapaths.core.data.WarpTarget;
import space.ajcool.ardapaths.screens.widgets.InputBoxWidget;
import space.ajcool.ardapaths.screens.widgets.TextValidationError;
import space.ajcool.ardapaths.screens.widgets.TextValidator;

/**
 * Validators and parsing helpers for marker-edit form fields.
 */
public final class MarkerFields {

    /**
     * Prevents construction of the static marker-field helper.
     */
    private MarkerFields() {
    }

    /**
     * Parses a text input, falling back to the last known valid value if the field is invalid.
     *
     * @param input         the input widget to parse
     * @param fallbackValue the value to use when validation fails
     * @return the input text or the fallback value
     */
    public static String parseTextOrFallback(InputBoxWidget input, String fallbackValue) {
        if (input == null) return fallbackValue;
        if (!input.validateText()) return fallbackValue;
        return input.getValue().trim();
    }

    /**
     * Parses an integer input, falling back to the last known valid value if the field is malformed.
     *
     * @param input         the input widget to parse
     * @param fallbackValue the value to use when parsing fails
     * @return the parsed integer or the fallback value
     */
    public static int parseIntegerOrFallback(InputBoxWidget input, int fallbackValue) {
        if (input == null) return fallbackValue;

        try {
            return Integer.parseInt(input.getValue());
        } catch (NumberFormatException e) {
            return fallbackValue;
        }
    }

    /**
     * Parses a time-of-day input, falling back to the last known valid value if the field is malformed.
     *
     * @param input         the input widget to parse
     * @param fallbackValue the value to use when parsing fails
     * @return the parsed time-of-day ticks or the fallback value
     */
    public static int parseTimeOfDayOrFallback(InputBoxWidget input, int fallbackValue) {
        if (input == null) return fallbackValue;

        try {
            return TimeOfDay.parse(input.getValue());
        } catch (TextValidationError e) {
            return fallbackValue;
        }
    }

    /**
     * Parses a transition range input, falling back to the last known valid value if the field is malformed.
     *
     * @param input         the input widget to parse
     * @param fallbackValue the value to use when parsing fails
     * @return parsed transition range or the fallback value
     */
    public static int parseTransitionRangeOrFallback(InputBoxWidget input, int fallbackValue) {
        if (input == null) return fallbackValue;

        try {
            return TimeOfDay.parseTransitionRange(input.getValue());
        } catch (TextValidationError e) {
            return fallbackValue;
        }
    }

    /**
     * Creates an integer validator constrained to the supplied inclusive bounds.
     *
     * @param min the minimum accepted value
     * @param max the maximum accepted value
     * @return validator for bounded integer text
     */
    public static TextValidator rangeValidator(int min, int max) {
        return text -> {
            try {
                int value = Integer.parseInt(text);
                if (value < min || value > max) {
                    throw new TextValidationError(String.format("Must be between %d and %d.", min, max));
                }
            } catch (NumberFormatException e) {
                throw new TextValidationError(Component.translatable("ardapaths.generic.validation.error.integer").getString());
            }
        };
    }

    /**
     * Validates the optional current-marker time.
     *
     * @param text input text to validate
     * @throws TextValidationError when the time is malformed
     */
    public static void validateCurrentTimeOfDay(String text) throws TextValidationError {
        TimeOfDay.parse(text);
    }

    /**
     * Validates the marker time transition mode or fixed transition range.
     *
     * @param text input text to validate
     * @throws TextValidationError when the range text is malformed
     */
    public static void validateTimeTransitionRange(String text) throws TextValidationError {
        TimeOfDay.parseTransitionRange(text);
    }

    /**
     * Validates the optional auto-teleport target shape.
     *
     * @param text input text to validate
     * @throws TextValidationError when the target is neither coordinates nor a single-token warp name
     */
    public static void validateAutoTeleportTarget(String text) throws TextValidationError {
        String value = text.trim();
        if (value.isEmpty() || WarpTarget.isCoordinates(value)) return;
        if (!value.isBlank() && !value.matches(".*\\s+.*")) return;
        throw new TextValidationError(Component.translatable("ardapaths.client.marker.configuration.screens.auto_teleport_target.invalid").getString());
    }

    /**
     * Validates the optional client focus target shape.
     *
     * @param text input text to validate
     * @throws TextValidationError when the target is not blank or coordinate text
     */
    public static void validateLookAt(String text) throws TextValidationError {
        String value = text.trim();
        if (value.isEmpty() || WarpTarget.isCoordinates(value)) return;
        throw new TextValidationError(Component.translatable("ardapaths.client.marker.configuration.screens.look_at.invalid").getString());
    }

    /**
     * Validates that the optional give-item value names a registered item.
     *
     * @param text input text to validate
     * @throws TextValidationError when the item identifier is malformed or unknown
     */
    public static void validateGiveItem(String text) throws TextValidationError {
        String value = text.trim();
        if (value.isEmpty()) return;
        if (GiveItemAction.isClear(value)) return;

        Identifier id = Identifier.tryParse(value);
        if (id != null && BuiltInRegistries.ITEM.containsKey(id)) return;

        throw new TextValidationError(Component.translatable("ardapaths.client.marker.configuration.screens.give_item.invalid").getString());
    }
}
